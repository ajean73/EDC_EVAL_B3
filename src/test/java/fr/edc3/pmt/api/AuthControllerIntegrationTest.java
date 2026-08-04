package fr.edc3.pmt.api;

import fr.edc3.pmt.api.dto.AuthDtos;
import fr.edc3.pmt.domain.service.ApiException;
import fr.edc3.pmt.domain.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerIntegrationTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void register_shouldDelegateToService() {
        AuthDtos.RegisterRequest request = new AuthDtos.RegisterRequest("alice", "alice@pmt.local", "StrongPass123");
        AuthDtos.AccountResponse expected = new AuthDtos.AccountResponse(3L, "alice", "alice@pmt.local", LocalDateTime.now());
        when(authService.register(request)).thenReturn(expected);

        AuthDtos.AccountResponse actual = authController.register(request);

        assertEquals(expected, actual);
    }

    @Test
    void login_shouldDelegateToService() {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("alice@pmt.local", "StrongPass123");
        AuthDtos.LoginResponse expected = new AuthDtos.LoginResponse(3L, "alice", "alice@pmt.local");
        when(authService.login(request)).thenReturn(expected);

        AuthDtos.LoginResponse actual = authController.login(request);

        assertEquals(expected, actual);
    }

    @Test
    void login_shouldPropagateBusinessError() {
        AuthDtos.LoginRequest request = new AuthDtos.LoginRequest("alice@pmt.local", "bad");
        when(authService.login(request)).thenThrow(new ApiException("Invalid credentials"));

        ApiException ex = assertThrows(ApiException.class, () -> authController.login(request));

        assertEquals("Invalid credentials", ex.getMessage());
    }
}
