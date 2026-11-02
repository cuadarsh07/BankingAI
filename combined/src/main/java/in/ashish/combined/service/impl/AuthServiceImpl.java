package in.ashish.combined.service.impl;

import in.ashish.combined.dto.AuthRequest;
import in.ashish.combined.dto.AuthResponse;
import in.ashish.combined.dto.RegisterRequest;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.AccountStatus;
import in.ashish.combined.entity.Role;
import in.ashish.combined.entity.User;
import in.ashish.combined.exception.DuplicateResourceException;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.UserRepository;
import in.ashish.combined.security.JwtService;
import in.ashish.combined.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Service Implementation for user registration, authentication, and token generation.
 *
 * Interview Concepts:
 * - Password Hashing: Plaintext passwords are NEVER stored in the database.
 *   BCrypt adds unique salts and runs multiple hashing rounds.
 * - Automatic Account Association: On registering a customer, a bank account is provisioned
 *   and bound to the user record atomically within a @Transactional boundary.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedUsername = request.getUsername().trim().toLowerCase();
        log.info("Processing user registration for username: {}", normalizedUsername);

        if (userRepository.existsByUsername(normalizedUsername)) {
            log.error("Registration failed: Username '{}' already taken", normalizedUsername);
            throw new DuplicateResourceException(String.format("Username '%s' is already registered", normalizedUsername));
        }

        Role role = request.getRole() != null ? request.getRole() : Role.ROLE_CUSTOMER;
        Account linkedAccount = null;

        // If registering a CUSTOMER, provision a new bank account
        if (role == Role.ROLE_CUSTOMER) {
            String generatedAccountNumber = generateUniqueAccountNumber();
            BigDecimal initialBalance = request.getInitialDeposit() != null ? request.getInitialDeposit() : BigDecimal.ZERO;

            Account account = Account.builder()
                    .accountNumber(generatedAccountNumber)
                    .accountHolderName(request.getFullName().trim())
                    .email(normalizedUsername)
                    .balance(initialBalance)
                    .status(AccountStatus.ACTIVE)
                    .build();

            linkedAccount = accountRepository.save(account);
            log.info("Provisioned new bank account {} for user {}", generatedAccountNumber, normalizedUsername);
        }

        User user = User.builder()
                .username(normalizedUsername)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(role)
                .account(linkedAccount)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        Long accountId = linkedAccount != null ? linkedAccount.getId() : null;
        String accountNumber = linkedAccount != null ? linkedAccount.getAccountNumber() : null;

        String token = jwtService.generateToken(savedUser.getUsername(), savedUser.getRole().name(), accountId);
        log.info("User {} registered successfully with role {}", savedUser.getUsername(), savedUser.getRole());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(savedUser.getUsername())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole().name())
                .accountId(accountId)
                .accountNumber(accountNumber)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        String normalizedUsername = request.getUsername().trim().toLowerCase();
        log.info("Processing login attempt for username: {}", normalizedUsername);

        User user = userRepository.findByUsername(normalizedUsername)
                .or(() -> userRepository.findByUsername(normalizedUsername + "@example.com"))
                .orElseThrow(() -> {
                    log.warn("Login failed: Username '{}' not found", normalizedUsername);
                    return new BadCredentialsException("Invalid username or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: Incorrect password for username '{}'", normalizedUsername);
            throw new BadCredentialsException("Invalid username or password");
        }

        Long accountId = user.getAccount() != null ? user.getAccount().getId() : null;
        String accountNumber = user.getAccount() != null ? user.getAccount().getAccountNumber() : null;

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name(), accountId);
        log.info("User {} logged in successfully", user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .accountId(accountId)
                .accountNumber(accountNumber)
                .build();
    }

    private String generateUniqueAccountNumber() {
        Random random = new Random();
        String accNum;
        do {
            accNum = String.valueOf(1000000000L + (long) (random.nextDouble() * 8999999999L));
        } while (accountRepository.existsByAccountNumber(accNum));
        return accNum;
    }
}
