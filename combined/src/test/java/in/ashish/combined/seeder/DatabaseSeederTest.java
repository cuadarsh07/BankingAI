package in.ashish.combined.seeder;

import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.Transaction;
import in.ashish.combined.entity.TransactionType;
import in.ashish.combined.entity.User;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.TransactionRepository;
import in.ashish.combined.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class DatabaseSeederTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DatabaseSeeder databaseSeeder;

    @Test
    @DisplayName("Verify DatabaseSeeder provisions 4 personas and over 1,000 mathematically accurate transactions")
    void testSeedingGeneratesAccurateData() {
        // Assert users seeded
        List<User> users = userRepository.findAll();
        assertThat(users).hasSize(4);

        for (User user : users) {
            assertThat(passwordEncoder.matches("Password123", user.getPassword())).isTrue();
            assertThat(user.getAccount()).isNotNull();
        }

        // Assert accounts seeded
        List<Account> accounts = accountRepository.findAll();
        assertThat(accounts).hasSize(4);

        // Assert total transactions > 1,000
        List<Transaction> allTransactions = transactionRepository.findAll();
        assertThat(allTransactions.size()).isGreaterThan(1000);

        // Verify Persona 1: Alice (Everyday Spender)
        User aliceUser = userRepository.findByUsername("alice@example.com").orElseThrow();
        Account aliceAccount = aliceUser.getAccount();
        List<Transaction> aliceTxns = allTransactions.stream()
                .filter(t -> t.getAccount().getId().equals(aliceAccount.getId()))
                .sorted(Comparator.comparing(Transaction::getCreatedAt))
                .toList();

        // ~600 transactions
        assertThat(aliceTxns.size()).isBetween(580, 620);

        // Check Alice transaction types: Deposits (salary) and Withdrawals (coffee & groceries)
        long aliceSalaryCount = aliceTxns.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .count();
        assertThat(aliceSalaryCount).isBetween(5L, 8L);

        // Verify coffee (~8 AM) and groceries (~5 PM)
        long coffeeCount = aliceTxns.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL && (t.getCreatedAt().getHour() == 7 || t.getCreatedAt().getHour() == 8))
                .count();
        assertThat(coffeeCount).isGreaterThan(500);

        long groceryCount = aliceTxns.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL && (t.getCreatedAt().getHour() == 16 || t.getCreatedAt().getHour() == 17))
                .count();
        assertThat(groceryCount).isBetween(20L, 35L);

        // Verify Persona 2: Bob (Business Owner)
        User bobUser = userRepository.findByUsername("bob@example.com").orElseThrow();
        Account bobAccount = bobUser.getAccount();
        List<Transaction> bobTxns = allTransactions.stream()
                .filter(t -> t.getAccount().getId().equals(bobAccount.getId()))
                .sorted(Comparator.comparing(Transaction::getCreatedAt))
                .toList();

        // ~150 transactions
        assertThat(bobTxns.size()).isBetween(140, 165);

        // Massive client deposits every 14 days
        long bobDeposits = bobTxns.stream()
                .filter(t -> t.getType() == TransactionType.DEPOSIT)
                .count();
        assertThat(bobDeposits).isBetween(12L, 15L);

        // Large vendor transfers every 3 days
        long bobTransfers = bobTxns.stream()
                .filter(t -> t.getType() == TransactionType.TRANSFER_OUT)
                .count();
        assertThat(bobTransfers).isBetween(130L, 150L);

        // Verify Persona 3: Charlie (Strict Saver)
        User charlieUser = userRepository.findByUsername("charlie@example.com").orElseThrow();
        Account charlieAccount = charlieUser.getAccount();
        List<Transaction> charlieTxns = allTransactions.stream()
                .filter(t -> t.getAccount().getId().equals(charlieAccount.getId()))
                .sorted(Comparator.comparing(Transaction::getCreatedAt))
                .toList();

        // ~50 transactions
        assertThat(charlieTxns.size()).isBetween(45, 55);

        // Charlie has ONLY deposits, NO withdrawals
        long charlieWithdrawals = charlieTxns.stream()
                .filter(t -> t.getType() == TransactionType.WITHDRAWAL || t.getType() == TransactionType.TRANSFER_OUT)
                .count();
        assertThat(charlieWithdrawals).isEqualTo(0L);

        // Verify Persona 4: Diana (Night Owl)
        User dianaUser = userRepository.findByUsername("diana@example.com").orElseThrow();
        Account dianaAccount = dianaUser.getAccount();
        List<Transaction> dianaTxns = allTransactions.stream()
                .filter(t -> t.getAccount().getId().equals(dianaAccount.getId()))
                .sorted(Comparator.comparing(Transaction::getCreatedAt))
                .toList();

        // ~200 transactions
        assertThat(dianaTxns.size()).isBetween(190, 215);

        // All Diana withdrawals strictly between 1 AM and 4 AM
        for (Transaction txn : dianaTxns) {
            assertThat(txn.getType()).isEqualTo(TransactionType.WITHDRAWAL);
            LocalTime time = txn.getCreatedAt().toLocalTime();
            assertThat(time.getHour()).isGreaterThanOrEqualTo(1).isLessThan(4);
        }

        // Mathematical Accuracy Verification across all accounts
        verifyMathematicalAccuracy(aliceAccount, aliceTxns, new BigDecimal("2500.00"));
        verifyMathematicalAccuracy(bobAccount, bobTxns, new BigDecimal("50000.00"));
        verifyMathematicalAccuracy(charlieAccount, charlieTxns, new BigDecimal("1000.00"));
        verifyMathematicalAccuracy(dianaAccount, dianaTxns, new BigDecimal("25000.00"));
    }

    private void verifyMathematicalAccuracy(Account account, List<Transaction> txns, BigDecimal startingBalance) {
        BigDecimal balance = startingBalance.setScale(2, RoundingMode.HALF_UP);
        for (Transaction txn : txns) {
            if (txn.getType() == TransactionType.DEPOSIT || txn.getType() == TransactionType.TRANSFER_IN) {
                balance = balance.add(txn.getAmount()).setScale(2, RoundingMode.HALF_UP);
            } else {
                balance = balance.subtract(txn.getAmount()).setScale(2, RoundingMode.HALF_UP);
            }
            assertThat(txn.getBalanceAfter())
                    .as("Transaction reference %s balanceAfter mismatch", txn.getReferenceNumber())
                    .isEqualByComparingTo(balance);
        }

        // Account final balance must match the last transaction's balanceAfter
        Account reloaded = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo(balance);
    }

    @Test
    @DisplayName("Safety Guard: run() does not re-seed when userRepository.count() > 0")
    void testSafetyGuardPreventsDuplicateSeeding() {
        long initialUserCount = userRepository.count();
        long initialTxnCount = transactionRepository.count();

        assertThat(initialUserCount).isGreaterThan(0);

        // Execute runner again
        databaseSeeder.run();

        assertThat(userRepository.count()).isEqualTo(initialUserCount);
        assertThat(transactionRepository.count()).isEqualTo(initialTxnCount);
    }
}

