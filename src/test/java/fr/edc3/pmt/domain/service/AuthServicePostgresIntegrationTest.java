package fr.edc3.pmt.domain.service;

import fr.edc3.pmt.PmtApplication;
import fr.edc3.pmt.api.dto.AuthDtos;
import fr.edc3.pmt.domain.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        classes = PmtApplication.class,
        properties = {
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.jpa.open-in-view=false",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
        }
)
class AuthServicePostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountRepository accountRepository;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @BeforeEach
        void setupSchema() {
                jdbcTemplate.execute("""
                        CREATE TABLE IF NOT EXISTS accounts (
                                id BIGSERIAL PRIMARY KEY,
                                username VARCHAR(50) NOT NULL UNIQUE,
                                email VARCHAR(255) NOT NULL UNIQUE,
                                password_hash VARCHAR(255) NOT NULL,
                                created_at TIMESTAMP NOT NULL,
                                updated_at TIMESTAMP NOT NULL
                        )
                        """);
                jdbcTemplate.execute("TRUNCATE TABLE accounts RESTART IDENTITY");
        }

    @Test
    void register_and_login_shouldWorkAgainstPostgresContainer() {
        AuthDtos.RegisterRequest registerRequest = new AuthDtos.RegisterRequest(
                "postgres-user",
                "postgres-user@pmt.local",
                "StrongPass123"
        );

        AuthDtos.AccountResponse registered = authService.register(registerRequest);

        assertNotNull(registered.id());
        assertEquals("postgres-user", registered.username());
        assertEquals(1, accountRepository.findAll().size());

        AuthDtos.LoginResponse login = authService.login(
                new AuthDtos.LoginRequest("postgres-user@pmt.local", "StrongPass123")
        );

        assertEquals(registered.id(), login.accountId());
        assertEquals("postgres-user", login.username());
    }

    @Test
    void register_shouldRejectDuplicateEmail_onRealPostgres() {
        AuthDtos.RegisterRequest first = new AuthDtos.RegisterRequest(
                "existing",
                "duplicate@pmt.local",
                "StrongPass123"
        );
        authService.register(first);

        AuthDtos.RegisterRequest duplicate = new AuthDtos.RegisterRequest(
                "another-user",
                "duplicate@pmt.local",
                "StrongPass123"
        );

        ApiException ex = assertThrows(ApiException.class, () -> authService.register(duplicate));
        assertEquals("Email already in use", ex.getMessage());
    }
}
