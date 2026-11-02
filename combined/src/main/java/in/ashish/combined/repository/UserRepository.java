package in.ashish.combined.repository;

import in.ashish.combined.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User persistence and lookups.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.account WHERE u.username = :username")
    Optional<User> findByUsernameWithAccount(@Param("username") String username);

    boolean existsByUsername(String username);
}
