package com.gd.demo.axondemo.cli;

import com.gd.demo.axondemo.account.command.*;
import com.gd.demo.axondemo.account.projection.BankAccountView;
import com.gd.demo.axondemo.account.projection.TransactionEntry;
import com.gd.demo.axondemo.account.query.FindAccountQuery;
import com.gd.demo.axondemo.account.query.FindAllAccountsQuery;
import com.gd.demo.axondemo.account.query.FindTransactionHistoryQuery;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

@Component
public class BankCli implements CommandLineRunner {

	private final CommandGateway commandGateway;
	private final QueryGateway queryGateway;

	public BankCli(CommandGateway commandGateway, QueryGateway queryGateway) {
		this.commandGateway = commandGateway;
		this.queryGateway = queryGateway;
	}

	@Override
	public void run(String... args) {
		System.out.println();
		System.out.println("╔══════════════════════════════════╗");
		System.out.println("║   Axon Framework 4 — Bank Demo   ║");
		System.out.println("╚══════════════════════════════════╝");
		System.out.println("H2 Console : http://localhost:8080/h2-console");
		System.out.println("JDBC URL   : jdbc:h2:mem:axondb  |  User: sa  |  Password: (empty)");
		System.out.println();
		printHelp();

		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print("> ");
			String line = scanner.nextLine().trim();
			if (line.isEmpty())
				continue;

			String[] parts = line.split("\\s+", 2);
			String command = parts[0];
			String commandArgs = parts.length > 1 ? parts[1] : "";
			try {
				switch (command) {
					case "create" -> handleCreate(commandArgs);
					case "deposit" -> handleDeposit(commandArgs);
					case "withdraw" -> handleWithdraw(commandArgs);
					case "transfer" -> handleTransfer(commandArgs);
					case "update" -> handleUpdateOwner(commandArgs);
					case "block" -> handleBlock(commandArgs);
					case "balance" -> handleBalance(commandArgs);
					case "list" -> handleList();
					case "history" -> handleHistory(commandArgs);
					case "help" -> printHelp();
					case "exit" -> {
						System.out.println("Bye!");
						System.exit(0);
					}
					default -> System.out.println("Unknown command. Type 'help' for available commands.");
				}
			} catch (Exception e) {
				System.out.println("Error: " + rootCause(e).getMessage());
			}
		}
	}

	private void handleCreate(String args) {
		String[] parts = args.split("\\s+", 3);
		if (parts.length < 3) {
			System.out.println("Usage: create <id> <owner> <initialBalance>");
			return;
		}
		String id = parts[0];
		String owner = parts[1];
		BigDecimal balance = new BigDecimal(parts[2]);
		commandGateway.sendAndWait(new CreateAccountCommand(id, owner, balance));
		System.out.printf("✓ Account '%s' created for %s with balance %s%n", id, owner, balance);
	}

	private void handleDeposit(String args) {
		String[] parts = args.split("\\s+", 3);
		if (parts.length < 2) {
			System.out.println("Usage: deposit <id> <amount> [message]");
			return;
		}
		String accountId = parts[0];
		BigDecimal amount = new BigDecimal(parts[1]);
		String message = parts.length > 2 ? parts[2] : "";
		commandGateway.sendAndWait(new DepositMoneyCommand(UUID.randomUUID(), accountId, amount, message, null));
		System.out.printf("✓ Deposited %s into account '%s'%n", amount, accountId);
	}

	private void handleWithdraw(String args) {
		ParsedArgs p = ParsedArgs.of(args);
		if (p.positional.size() < 2) {
			System.out.println("Usage: withdraw <id> <amount> [--version=X] [--message=<text>]");
			return;
		}
		String accountId = p.positional.get(0);
		BigDecimal amount = new BigDecimal(p.positional.get(1));
		long version = p.hasFlag("version") ? Long.parseLong(p.flag("version")) : currentVersion(accountId);
		String message = p.flag("message");
		commandGateway.sendAndWait(new WithdrawMoneyCommand(UUID.randomUUID(), accountId, amount, message != null ? message : "", version));
		System.out.printf("✓ Withdrew %s from account '%s' (used version %d)%n", amount, accountId, version);
	}

	private void handleTransfer(String args) {
		ParsedArgs p = ParsedArgs.of(args);
		if (p.positional.size() < 3) {
			System.out.println("Usage: transfer <fromId> <toId> <amount> [--version=X] [--message=<text>]");
			return;
		}
		String accountId = p.positional.get(0);
		String targetAccountId = p.positional.get(1);
		BigDecimal amount = new BigDecimal(p.positional.get(2));
		long version = p.hasFlag("version") ? Long.parseLong(p.flag("version")) : currentVersion(accountId);
		String message = p.flag("message");
		UUID transferId = UUID.randomUUID();
		commandGateway.sendAndWait(new TransferMoneyCommand(transferId, accountId, amount, targetAccountId, message != null ? message : "", version));
		System.out.printf("✓ Transfer initiated: %s from '%s' to '%s' (id: %s, used version %d)%n",
				amount, accountId, targetAccountId, transferId, version);
		System.out.println("  (watch for [SAGA] messages — compensation fires if target is blocked)");
	}

	private void handleUpdateOwner(String args) {
		String[] parts = args.split("\\s+", 3);
		if (parts.length < 3) {
			System.out.println("Usage: updateowner <id> <version> <newOwner>");
			return;
		}
		String accountId = parts[0];
		Long version = Long.parseLong(parts[1]);
		String newOwner = parts[2];
		commandGateway.sendAndWait(new UpdateOwnerCommand(accountId, newOwner, version));
		System.out.printf("✓ Owner of account '%s' updated to '%s'%n", accountId, newOwner);
	}

	private void handleBlock(String args) {
		String[] parts = args.split("\\s+", 1);
		if (parts.length < 1 || parts[0].isBlank()) {
			System.out.println("Usage: block <id>");
			return;
		}
		String accountId = parts[0];
		commandGateway.sendAndWait(new BlockAccountCommand(accountId));
		System.out.printf("✓ Account '%s' has been blocked%n", accountId);
	}

	private void handleBalance(String args) {
		String[] parts = args.split("\\s+", 1);
		if (parts.length < 1 || parts[0].isBlank()) {
			System.out.println("Usage: balance <id>");
			return;
		}

		String accountId = parts[0];
		BankAccountView account = queryGateway
				.query(new FindAccountQuery(accountId), BankAccountView.class)
				.join();
		if (account == null) {
			System.out.printf("Account '%s' not found.%n", accountId);
		} else {
			String status = account.isBlocked() ? " [BLOCKED]" : "";
			System.out.printf("Account %-10s | Owner: %-15s | Balance: %-10s | Version: %d%s%n",
					account.getAccountId(), account.getOwner(), account.getBalance(),
					account.getVersion(), status);
		}
	}

	private void handleList() {
		List<BankAccountView> accounts = queryGateway
				.query(new FindAllAccountsQuery(), ResponseTypes.multipleInstancesOf(BankAccountView.class))
				.join();
		if (accounts.isEmpty()) {
			System.out.println("No accounts found.");
			return;
		}
		System.out.println("┌─────────────┬──────────────────┬─────────────────┬─────────┬──────────┐");
		System.out.println("│ Account ID  │ Owner            │ Balance         │ Version │ Status   │");
		System.out.println("├─────────────┼──────────────────┼─────────────────┼─────────┼──────────┤");
		for (BankAccountView a : accounts) {
			System.out.printf("│ %-11s │ %-16s │ %-15s │ %-7d │ %-8s │%n",
					a.getAccountId(), a.getOwner(), a.getBalance(), a.getVersion(),
					a.isBlocked() ? "BLOCKED" : "active");
		}
		System.out.println("└─────────────┴──────────────────┴─────────────────┴─────────┴──────────┘");
	}

	private void handleHistory(String args) {
		String[] parts = args.split("\\s+", 1);
		if (parts.length < 1 || parts[0].isBlank()) {
			System.out.println("Usage: history <id>");
			return;
		}
		String accountId = parts[0];
		List<TransactionEntry> entries = queryGateway
				.query(new FindTransactionHistoryQuery(accountId),
						ResponseTypes.multipleInstancesOf(TransactionEntry.class))
				.join();
		if (entries.isEmpty()) {
			System.out.printf("No transaction history found for account '%s'.%n", accountId);
			return;
		}
		System.out.printf("Transaction history for account '%s':%n", accountId);
		System.out.println("┌────┬─────────────────┬────────────────┬────────────────┬────────────────┬──────────────────────┬──────────────────────┐");
		System.out.println("│ Sq │ Type            │ Amount         │ Before         │ After          │ Counterpart          │ Message              │");
		System.out.println("├────┼─────────────────┼────────────────┼────────────────┼────────────────┼──────────────────────┼──────────────────────┤");
		for (TransactionEntry e : entries) {
			System.out.printf("│ %-2d │ %-15s │ %-14s │ %-14s │ %-14s │ %-20s │ %-20s │%n",
					e.getSequenceNumber(),
					e.getType(),
					e.getAmount() != null ? e.getAmount() : "-",
					e.getBalanceBefore() != null ? e.getBalanceBefore() : "-",
					e.getBalanceAfter() != null ? e.getBalanceAfter() : "-",
					e.getCounterpartAccountId() != null ? e.getCounterpartAccountId() : "-",
					e.getMessage() != null ? e.getMessage() : "-");
		}
		System.out.println("└────┴─────────────────┴────────────────┴────────────────┴────────────────┴──────────────────────┴──────────────────────┘");
	}

	private void printHelp() {
		System.out.println("Commands:");
		System.out.println("  create <id> <owner> <initialBalance>      						Create a new bank account");
		System.out.println("  deposit <id> <amount> [--message=<text>]                  		Deposit money");
		System.out.println("  withdraw <id> <amount> [--version=X] [--message=<text>]   		Withdraw money (version defaults to current)");
		System.out.println("  transfer <fromId> <toId> <amt> [--version=X] [--message=<text>]	Transfer money via Saga (version defaults to current)");
		System.out.println("  updateowner <id> <version> <newOwner>                  			Update account owner (conflict-checked)");
		System.out.println("  block <id>                                						Block an account");
		System.out.println("  balance <id>                              						Show account details + current version");
		System.out.println("  list                                      						List all accounts with versions");
		System.out.println("  history <id>                              						Show transaction history for an account");
		System.out.println("  help                                      						Show this help");
		System.out.println("  exit                                      						Quit");
		System.out.println();
	}

	private long currentVersion(String accountId) {
		BankAccountView account = queryGateway
				.query(new FindAccountQuery(accountId), BankAccountView.class)
				.join();
		if (account == null) {
			throw new IllegalStateException("Account '" + accountId + "' not found");
		}
		return account.getVersion();
	}

	/** Splits a raw arg string into positional args and --key=value flags. */
	private static class ParsedArgs {
		final java.util.List<String> positional = new java.util.ArrayList<>();
		final java.util.Map<String, String> flags = new java.util.HashMap<>();

		static ParsedArgs of(String args) {
			ParsedArgs p = new ParsedArgs();
			if (args == null || args.isBlank()) return p;
			for (String token : args.trim().split("\\s+")) {
				if (token.startsWith("--") && token.contains("=")) {
					int eq = token.indexOf('=');
					p.flags.put(token.substring(2, eq), token.substring(eq + 1));
				} else {
					p.positional.add(token);
				}
			}
			return p;
		}

		boolean hasFlag(String key) { return flags.containsKey(key); }
		String flag(String key) { return flags.get(key); }
	}

	private Throwable rootCause(Throwable t) {
		while (t.getCause() != null)
			t = t.getCause();
		return t;
	}
}
