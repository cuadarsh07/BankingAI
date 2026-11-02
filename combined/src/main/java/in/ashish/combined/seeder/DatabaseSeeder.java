package in.ashish.combined.seeder;

import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.AccountStatus;
import in.ashish.combined.entity.Role;
import in.ashish.combined.entity.Transaction;
import in.ashish.combined.entity.TransactionType;
import in.ashish.combined.entity.User;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.TransactionRepository;
import in.ashish.combined.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * DatabaseSeeder populates the development database with baseline historical financial data.
 *
 * It generates 4 distinct behavioral personas with over 1,000 mathematically accurate
 * transactions spanning the last 6 months to train and baseline the AI/LLM fraud detection engine:
 * 1. Alice (Everyday Spender): ~600 transactions (daily coffee ~8 AM, weekly groceries ~5 PM, monthly salary).
 * 2. Bob (Business Owner): ~150 transactions (massive client deposits every 14 days, large vendor transfers every 3 days).
 * 3. Charlie (Strict Saver): ~50 transactions (only weekly deposits, zero withdrawals).
 * 4. Diana (Night Owl): ~200 transactions (random withdrawals strictly between 1 AM and 4 AM).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "postgres", "prod", "default"})
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        String encodedPassword = passwordEncoder.encode("Password123");

        // 1. Ensure System Administrator accounts always exist
        if (!userRepository.existsByUsername("admin@example.com")) {
            User adminUser = User.builder()
                    .username("admin@example.com")
                    .password(encodedPassword)
                    .fullName("Marcus Vance (Security Lead)")
                    .role(Role.ROLE_ADMIN)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(adminUser);
            log.info("Default Admin account seeded: admin@example.com / Password123");
        }

        if (!userRepository.existsByUsername("admin")) {
            User adminShort = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Marcus Vance (Security Lead)")
                    .role(Role.ROLE_ADMIN)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRepository.save(adminShort);
            log.info("Default Admin shortcut account seeded: admin / admin123");
        }

        // Safety Guard: Check if the customer personas have already been seeded
        if (userRepository.count() > 2 && userRepository.existsByUsername("alice@example.com")) {
            log.info("Database personas already seeded. Skipping...");
            return;
        }

        log.info("Starting historical transaction data seeding for AI fraud detection baseline...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sixMonthsAgo = now.minusMonths(6);

        // Seed 4 distinct financial personas
        seedAlice(sixMonthsAgo, now, encodedPassword);
        seedBob(sixMonthsAgo, now, encodedPassword);
        seedCharlie(sixMonthsAgo, now, encodedPassword);
        seedDiana(sixMonthsAgo, now, encodedPassword);

        long totalTxns = transactionRepository.count();
        log.info("Seeding completed successfully! Total historical transactions created: {}", totalTxns);
    }

    /**
     * Persona 1 (Alice - Everyday Spender): ~600 transactions.
     * Characteristics:
     * - Daily coffee withdrawals around 8 AM (~3 daily coffee purchases).
     * - Weekly grocery withdrawals around 5 PM (~17:00).
     * - Monthly salary direct deposits ($5,000.00).
     */
    private void seedAlice(LocalDateTime sixMonthsAgo, LocalDateTime now, String encodedPassword) {
        BigDecimal initialBalance = new BigDecimal("2500.00").setScale(2, RoundingMode.HALF_UP);

        Account account = Account.builder()
                .accountNumber("1000000001")
                .accountHolderName("Alice Spender")
                .email("alice@example.com")
                .balance(initialBalance)
                .status(AccountStatus.ACTIVE)
                .createdAt(sixMonthsAgo)
                .updatedAt(now)
                .build();
        account = accountRepository.save(account);

        User user = User.builder()
                .username("alice@example.com")
                .password(encodedPassword)
                .fullName("Alice Spender")
                .role(Role.ROLE_CUSTOMER)
                .account(account)
                .createdAt(sixMonthsAgo)
                .build();
        userRepository.save(user);

        int totalDays = (int) Math.max(180, ChronoUnit.DAYS.between(sixMonthsAgo.toLocalDate(), now.toLocalDate()));
        Random random = new Random(101);
        List<Transaction> transactions = new ArrayList<>();

        String[] coffeeDescriptions = {
                "Morning Coffee - Espresso Bar",
                "Artisan Cafe - Drip Coffee & Croissant",
                "Morning Latte - Starbucks",
                "Specialty Coffee Roasters",
                "Mid-Morning Americano"
        };

        String[] groceryDescriptions = {
                "Weekly Groceries - Whole Foods Market",
                "Supermarket Purchase - Trader Joe's",
                "Weekly Fresh Groceries - Local Market"
        };

        for (int d = 0; d < totalDays; d++) {
            LocalDate date = sixMonthsAgo.toLocalDate().plusDays(d);

            // 1. Monthly salary deposit every 30 days
            if (d % 30 == 0) {
                transactions.add(Transaction.builder()
                        .referenceNumber(generateRefNumber("SAL"))
                        .account(account)
                        .type(TransactionType.DEPOSIT)
                        .amount(new BigDecimal("5000.00"))
                        .balanceAfter(BigDecimal.ZERO) // Calculated below
                        .description("Monthly Salary Payroll Deposit")
                        .createdAt(LocalDateTime.of(date, LocalTime.of(9, 0, 0)))
                        .build());
            }

            // 2. Daily coffee withdrawals (~8 AM)
            // 3 coffees daily, plus an occasional extra coffee (~570 total coffee transactions)
            int coffeeCount = (d % 6 == 0) ? 4 : 3;
            for (int c = 0; c < coffeeCount; c++) {
                // Around 8 AM: between 07:45 and 08:45
                int minuteOffset = 45 + (c * 18) + random.nextInt(8);
                int hour = 7 + (minuteOffset / 60);
                int minute = minuteOffset % 60;
                LocalTime coffeeTime = LocalTime.of(hour, minute, random.nextInt(60));

                double coffeeAmount = 3.50 + (random.nextDouble() * 3.25);
                BigDecimal amount = BigDecimal.valueOf(coffeeAmount).setScale(2, RoundingMode.HALF_UP);

                transactions.add(Transaction.builder()
                        .referenceNumber(generateRefNumber("COF"))
                        .account(account)
                        .type(TransactionType.WITHDRAWAL)
                        .amount(amount)
                        .balanceAfter(BigDecimal.ZERO)
                        .description(coffeeDescriptions[random.nextInt(coffeeDescriptions.length)])
                        .createdAt(LocalDateTime.of(date, coffeeTime))
                        .build());
            }

            // 3. Weekly groceries (~5 PM)
            if (d % 7 == 3) {
                // ~5 PM: between 16:50 and 17:25
                int minute = random.nextInt(35);
                int hour = (minute < 10) ? 16 : 17;
                LocalTime groceryTime = LocalTime.of(17, random.nextInt(30), random.nextInt(60));

                double groceryAmount = 65.00 + (random.nextDouble() * 90.00);
                BigDecimal amount = BigDecimal.valueOf(groceryAmount).setScale(2, RoundingMode.HALF_UP);

                transactions.add(Transaction.builder()
                        .referenceNumber(generateRefNumber("GRO"))
                        .account(account)
                        .type(TransactionType.WITHDRAWAL)
                        .amount(amount)
                        .balanceAfter(BigDecimal.ZERO)
                        .description(groceryDescriptions[random.nextInt(groceryDescriptions.length)])
                        .createdAt(LocalDateTime.of(date, groceryTime))
                        .build());
            }
        }

        applyTransactionsAndSave(account, initialBalance, transactions);
        log.info("Persona 1 (Alice - Everyday Spender) seeded with {} transactions", transactions.size());
    }

    /**
     * Persona 2 (Bob - Business Owner): ~150 transactions.
     * Characteristics:
     * - Massive client deposits every 14 days ($25,000 - $65,000).
     * - Large vendor transfers every 3 days ($1,500 - $5,500).
     */
    private void seedBob(LocalDateTime sixMonthsAgo, LocalDateTime now, String encodedPassword) {
        BigDecimal initialBalance = new BigDecimal("50000.00").setScale(2, RoundingMode.HALF_UP);

        Account account = Account.builder()
                .accountNumber("1000000002")
                .accountHolderName("Bob Business")
                .email("bob@example.com")
                .balance(initialBalance)
                .status(AccountStatus.ACTIVE)
                .createdAt(sixMonthsAgo)
                .updatedAt(now)
                .build();
        account = accountRepository.save(account);

        User user = User.builder()
                .username("bob@example.com")
                .password(encodedPassword)
                .fullName("Bob Business")
                .role(Role.ROLE_CUSTOMER)
                .account(account)
                .createdAt(sixMonthsAgo)
                .build();
        userRepository.save(user);

        int totalDays = (int) Math.max(180, ChronoUnit.DAYS.between(sixMonthsAgo.toLocalDate(), now.toLocalDate()));
        Random random = new Random(202);
        List<Transaction> transactions = new ArrayList<>();

        String[] vendorDescriptions = {
                "Vendor Transfer - Supply Chain Logistics Ltd",
                "Large Vendor Transfer - Cloud Infrastructure & Hosting",
                "Vendor Payment - Raw Materials & Manufacturing",
                "Vendor Transfer - Office Lease & Commercial Facilities",
                "Vendor Wire - Software License & Enterprise Tools"
        };

        for (int d = 0; d < totalDays; d++) {
            LocalDate date = sixMonthsAgo.toLocalDate().plusDays(d);

            // 1. Massive client deposits every 14 days
            if (d % 14 == 0) {
                double clientAmount = 30000.0 + (random.nextInt(350) * 100.0);
                BigDecimal amount = BigDecimal.valueOf(clientAmount).setScale(2, RoundingMode.HALF_UP);

                transactions.add(Transaction.builder()
                        .referenceNumber(generateRefNumber("CLT"))
                        .account(account)
                        .type(TransactionType.DEPOSIT)
                        .amount(amount)
                        .balanceAfter(BigDecimal.ZERO)
                        .description("Massive Client Wire Deposit - Invoice #" + (1000 + d))
                        .createdAt(LocalDateTime.of(date, LocalTime.of(10, 30, random.nextInt(60))))
                        .build());
            }

            // 2. Large vendor transfers every 3 days (~140 transfers across 60 cycles)
            if (d % 3 == 0 && d > 0) {
                int transfersToday = (d % 9 == 0) ? 3 : 2;
                LocalTime[] transferTimes = {
                        LocalTime.of(11, 15, random.nextInt(60)),
                        LocalTime.of(14, 30, random.nextInt(60)),
                        LocalTime.of(16, 45, random.nextInt(60))
                };

                for (int t = 0; t < transfersToday; t++) {
                    double vendorAmount = 1500.0 + (random.nextInt(350) * 10.0);
                    BigDecimal amount = BigDecimal.valueOf(vendorAmount).setScale(2, RoundingMode.HALF_UP);

                    transactions.add(Transaction.builder()
                            .referenceNumber(generateRefNumber("VND"))
                            .account(account)
                            .type(TransactionType.TRANSFER_OUT)
                            .amount(amount)
                            .balanceAfter(BigDecimal.ZERO)
                            .description(vendorDescriptions[random.nextInt(vendorDescriptions.length)])
                            .createdAt(LocalDateTime.of(date, transferTimes[t]))
                            .build());
                }
            }
        }

        applyTransactionsAndSave(account, initialBalance, transactions);
        log.info("Persona 2 (Bob - Business Owner) seeded with {} transactions", transactions.size());
    }

    /**
     * Persona 3 (Charlie - Strict Saver): ~50 transactions.
     * Characteristics:
     * - Only weekly deposits ($150 - $550).
     * - Zero withdrawals.
     */
    private void seedCharlie(LocalDateTime sixMonthsAgo, LocalDateTime now, String encodedPassword) {
        BigDecimal initialBalance = new BigDecimal("1000.00").setScale(2, RoundingMode.HALF_UP);

        Account account = Account.builder()
                .accountNumber("1000000003")
                .accountHolderName("Charlie Saver")
                .email("charlie@example.com")
                .balance(initialBalance)
                .status(AccountStatus.ACTIVE)
                .createdAt(sixMonthsAgo)
                .updatedAt(now)
                .build();
        account = accountRepository.save(account);

        User user = User.builder()
                .username("charlie@example.com")
                .password(encodedPassword)
                .fullName("Charlie Saver")
                .role(Role.ROLE_CUSTOMER)
                .account(account)
                .createdAt(sixMonthsAgo)
                .build();
        userRepository.save(user);

        int totalDays = (int) Math.max(180, ChronoUnit.DAYS.between(sixMonthsAgo.toLocalDate(), now.toLocalDate()));
        Random random = new Random(303);
        List<Transaction> transactions = new ArrayList<>();

        String[] savingsDescriptions = {
                "Weekly Automated Savings Deposit",
                "High-Yield Savings Contribution",
                "Weekly Emergency Fund Allocation",
                "Automated Thrift & Investment Deposit"
        };

        // Exactly 52 weekly deposits spanning the 6-month period (~26 weeks * 2 deposits/week)
        int targetDeposits = 52;
        for (int i = 0; i < targetDeposits; i++) {
            int dayOffset = (i * totalDays) / targetDeposits;
            LocalDate date = sixMonthsAgo.toLocalDate().plusDays(dayOffset);
            LocalTime depositTime = LocalTime.of(12, 15 + random.nextInt(30), random.nextInt(60));

            double depositAmount = 150.0 + (random.nextInt(400));
            BigDecimal amount = BigDecimal.valueOf(depositAmount).setScale(2, RoundingMode.HALF_UP);

            transactions.add(Transaction.builder()
                    .referenceNumber(generateRefNumber("SAV"))
                    .account(account)
                    .type(TransactionType.DEPOSIT)
                    .amount(amount)
                    .balanceAfter(BigDecimal.ZERO)
                    .description(savingsDescriptions[random.nextInt(savingsDescriptions.length)])
                    .createdAt(LocalDateTime.of(date, depositTime))
                    .build());
        }

        applyTransactionsAndSave(account, initialBalance, transactions);
        log.info("Persona 3 (Charlie - Strict Saver) seeded with {} transactions (0 withdrawals)", transactions.size());
    }

    /**
     * Persona 4 (Diana - Night Owl): ~200 transactions.
     * Characteristics:
     * - Random withdrawals happening strictly between 1 AM and 4 AM (01:00:00 to 03:59:59).
     * - Sustained by initial account balance, zero daytime spending.
     */
    private void seedDiana(LocalDateTime sixMonthsAgo, LocalDateTime now, String encodedPassword) {
        BigDecimal initialBalance = new BigDecimal("25000.00").setScale(2, RoundingMode.HALF_UP);

        Account account = Account.builder()
                .accountNumber("1000000004")
                .accountHolderName("Diana Nightowl")
                .email("diana@example.com")
                .balance(initialBalance)
                .status(AccountStatus.ACTIVE)
                .createdAt(sixMonthsAgo)
                .updatedAt(now)
                .build();
        account = accountRepository.save(account);

        User user = User.builder()
                .username("diana@example.com")
                .password(encodedPassword)
                .fullName("Diana Nightowl")
                .role(Role.ROLE_CUSTOMER)
                .account(account)
                .createdAt(sixMonthsAgo)
                .build();
        userRepository.save(user);

        int totalDays = (int) Math.max(180, ChronoUnit.DAYS.between(sixMonthsAgo.toLocalDate(), now.toLocalDate()));
        Random random = new Random(404);
        List<Transaction> transactions = new ArrayList<>();

        String[] nightOwlDescriptions = {
                "Late Night Online Shopping - Amazon",
                "Late Night Food Delivery - DoorDash",
                "Night Gaming In-App Purchase - Steam",
                "Midnight Digital Media Subscription",
                "Late Night Rideshare Fare - Uber",
                "24-Hour Convenience Store & Mart",
                "Late Night Digital Entertainment"
        };

        // 205 withdrawals strictly between 1 AM and 4 AM
        int targetWithdrawals = 205;
        for (int i = 0; i < targetWithdrawals; i++) {
            int dayOffset = (i * totalDays) / targetWithdrawals;
            LocalDate date = sixMonthsAgo.toLocalDate().plusDays(dayOffset);

            // Strictly between 1 AM and 4 AM: hour is 1, 2, or 3
            int hour = 1 + random.nextInt(3);
            int minute = random.nextInt(60);
            int second = random.nextInt(60);
            LocalTime nightTime = LocalTime.of(hour, minute, second);

            double withdrawalAmount = 15.0 + (random.nextDouble() * 85.0);
            BigDecimal amount = BigDecimal.valueOf(withdrawalAmount).setScale(2, RoundingMode.HALF_UP);

            transactions.add(Transaction.builder()
                    .referenceNumber(generateRefNumber("NGH"))
                    .account(account)
                    .type(TransactionType.WITHDRAWAL)
                    .amount(amount)
                    .balanceAfter(BigDecimal.ZERO)
                    .description(nightOwlDescriptions[random.nextInt(nightOwlDescriptions.length)])
                    .createdAt(LocalDateTime.of(date, nightTime))
                    .build());
        }

        applyTransactionsAndSave(account, initialBalance, transactions);
        log.info("Persona 4 (Diana - Night Owl) seeded with {} late-night transactions (all between 1 AM and 4 AM)", transactions.size());
    }

    /**
     * Staggers and chronologically sorts transactions, calculates exact BigDecimal running balances,
     * updates balanceAfter for every transaction, persists all transactions, and updates the Account balance.
     */
    private void applyTransactionsAndSave(Account account, BigDecimal startingBalance, List<Transaction> transactions) {
        // Chronologically order transactions by createdAt
        transactions.sort(Comparator.comparing(Transaction::getCreatedAt));

        BigDecimal currentBalance = startingBalance;
        for (Transaction txn : transactions) {
            if (txn.getType() == TransactionType.DEPOSIT || txn.getType() == TransactionType.TRANSFER_IN) {
                currentBalance = currentBalance.add(txn.getAmount()).setScale(2, RoundingMode.HALF_UP);
            } else if (txn.getType() == TransactionType.WITHDRAWAL || txn.getType() == TransactionType.TRANSFER_OUT) {
                currentBalance = currentBalance.subtract(txn.getAmount()).setScale(2, RoundingMode.HALF_UP);
            }
            txn.setBalanceAfter(currentBalance);
        }

        // Save transactions in batch
        transactionRepository.saveAll(transactions);

        // Update and save final calculated balance back to the Account entity
        account.setBalance(currentBalance);
        accountRepository.save(account);
    }

    /**
     * Generates a unique reference number of length <= 36 (spec requirement).
     */
    private String generateRefNumber(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}

