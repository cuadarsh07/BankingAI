package in.ashish.combined.repository;

import in.ashish.combined.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Account persistence operations.
 *
 * Interview Concepts:
 * - Spring Data JPA generates the proxy implementation at runtime based on method names.
 * - findByIdWithLock: Uses Pessimistic Write Locking (SELECT ... FOR UPDATE).
 *   Prevents concurrency anomalies (Lost Updates, Race Conditions) by locking the
 *   row in the database until the transaction commits or rolls back.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}
