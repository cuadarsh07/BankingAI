package in.ashish.combined.security;

import in.ashish.combined.entity.Role;
import in.ashish.combined.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Service providing fine-grained account authorization logic.
 *
 * Interview Concepts:
 * - Why Authentication != Authorization:
 *     * Authentication answers: "Who are you?" (verified via password / valid JWT).
 *     * Authorization answers: "Are you allowed to perform THIS action on THIS resource?"
 * - In banking, simply being an authenticated customer does NOT grant permission to view
 *   or withdraw from other customers' bank accounts!
 * - This service enforces resource ownership: Customers can only access their own linked account,
 *   while Administrators (ROLE_ADMIN) have system-wide oversight.
 */
@Slf4j
@Service("accountSecurityService")
public class AccountSecurityService {

    /**
     * Check if the authenticated user owns the given account or has admin privileges.
     */
    public boolean canAccessAccount(Long accountId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Admins can access all accounts
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));
        if (isAdmin) {
            return true;
        }

        // For customers, verify ownership
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            boolean isOwner = user.getAccount() != null && user.getAccount().getId().equals(accountId);
            if (!isOwner) {
                log.warn("Access denied: User '{}' attempted to access account ID {} which they do not own",
                        user.getUsername(), accountId);
            }
            return isOwner;
        }

        return false;
    }
}
