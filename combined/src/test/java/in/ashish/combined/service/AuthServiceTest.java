package in.ashish.combined.service;

import in.ashish.combined.dto.AuthRequest;
import in.ashish.combined.dto.AuthResponse;
import in.ashish.combined.dto.RegisterRequest;
import in.ashish.combined.entity.Account;
import in.ashish.combined.entity.Role;
import in.ashish.combined.entity.User;
import in.ashish.combined.exception.DuplicateResourceException;
import in.ashish.combined.repository.AccountRepository;
import in.ashish.combined.repository.UserRepository;
import in.ashish.combined.security.JwtService;
import in.ashish.combined.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private AuthRequest authRequest;
    private User sampleUser;
    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .username("customer@example.com")
                .password("Secret@123")
                .fullName("Customer User")
                .role(Role.ROLE_CUSTOMER)
                .initialDeposit(new BigDecimal("1000.00"))
                .build();

        authRequest = AuthRequest.builder()
                .username("customer@example.com")
                .password("Secret@123")
                .build();

        sampleAccount = Account.builder()
                .id(1L)
                .accountNumber("1234567890")
                .build();

        sampleUser = User.builder()
                .id(1L)
                .username("customer@example.com")
                .password("encoded_secret_hash")
                .fullName("Customer User")
                .role(Role.ROLE_CUSTOMER)
                .account(sampleAccount)
                .build();
    }

    @Test
    @DisplayName("Should successfully register a new customer and return JWT token")
    void register_Customer_Success() {
        when(userRepository.existsByUsername("customer@example.com")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);
        when(passwordEncoder.encode("Secret@123")).thenReturn("encoded_secret_hash");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateToken("customer@example.com", "ROLE_CUSTOMER", 1L)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("customer@example.com", response.getUsername());
        assertEquals("ROLE_CUSTOMER", response.getRole());
        assertEquals(1L, response.getAccountId());

        verify(userRepository, times(1)).save(any(User.class));
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when username is already taken")
    void register_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByUsername("customer@example.com")).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> authService.register(registerRequest)
        );

        assertTrue(exception.getMessage().contains("already registered"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully authenticate valid credentials and return JWT token")
    void login_Success() {
        when(userRepository.findByUsername("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Secret@123", "encoded_secret_hash")).thenReturn(true);
        when(jwtService.generateToken("customer@example.com", "ROLE_CUSTOMER", 1L)).thenReturn("mock.jwt.token");

        AuthResponse response = authService.login(authRequest);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("customer@example.com", response.getUsername());
    }

    @Test
    @DisplayName("Should throw BadCredentialsException when password does not match")
    void login_WrongPassword_ThrowsException() {
        when(userRepository.findByUsername("customer@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Secret@123", "encoded_secret_hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(authRequest));
        verify(jwtService, never()).generateToken(any(), any(), any());
    }
}
