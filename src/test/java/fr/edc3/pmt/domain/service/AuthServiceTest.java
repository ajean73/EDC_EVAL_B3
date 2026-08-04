package fr.edc3.pmt.domain.service;

import fr.edc3.pmt.api.dto.AuthDtos;
import fr.edc3.pmt.domain.model.Account;
import fr.edc3.pmt.domain.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AuthService authService;

    private AuthDtos.RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new AuthDtos.RegisterRequest("alice", "alice@pmt.local", "StrongPass123");
    }

    @Test
    void register_shouldCreateAccount_whenInputIsValid() {
        when(accountRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(accountRepository.existsByUsername(registerRequest.username())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(42L);
            return account;
        });

        AuthDtos.AccountResponse response = authService.register(registerRequest);

        assertEquals(42L, response.id());
        assertEquals("alice", response.username());
        assertEquals("alice@pmt.local", response.email());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void register_shouldFail_whenEmailAlreadyExists() {
        when(accountRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));

        assertEquals("Email already in use", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void register_shouldFail_whenUsernameAlreadyExists() {
        when(accountRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(accountRepository.existsByUsername(registerRequest.username())).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(registerRequest));

        assertEquals("Username already in use", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void login_shouldReturnUser_whenCredentialsAreValid() {
        String rawPassword = "demo-pass-123";
        String encodedPassword = new BCryptPasswordEncoder().encode(rawPassword);
        Account account = Account.builder()
                .id(7L)
                .username("john")
                .email("john@pmt.local")
            .passwordHash(encodedPassword)
                .build();
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("john@pmt.local", rawPassword);

        when(accountRepository.findByEmail(request.email())).thenReturn(Optional.of(account));

        AuthDtos.LoginResponse response = authService.login(request);

        assertEquals(7L, response.accountId());
        assertEquals("john", response.username());
    }

    @Test
    void login_shouldFail_whenEmailNotFound() {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("unknown@pmt.local", "pass");
        when(accountRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void login_shouldFail_whenPasswordDoesNotMatch() {
        Account account = Account.builder()
                .id(9L)
                .username("john")
                .email("john@pmt.local")
                .passwordHash("$2a$10$S7VfW2v7mgTA0Ie4j9hwbuodjY2x6E4fSW3dD8e6TqcHnNf4sfq5W")
                .build();
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("john@pmt.local", "bad-pass");

        when(accountRepository.findByEmail(request.email())).thenReturn(Optional.of(account));

        ApiException ex = assertThrows(ApiException.class, () -> authService.login(request));

        assertEquals("Invalid credentials", ex.getMessage());
    }
}
