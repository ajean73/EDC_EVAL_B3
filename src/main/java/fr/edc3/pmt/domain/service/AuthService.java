package fr.edc3.pmt.domain.service;

import fr.edc3.pmt.api.dto.AuthDtos;
import fr.edc3.pmt.domain.model.Account;
import fr.edc3.pmt.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final ApiException apiException = new ApiException("");

    @Transactional
    public AuthDtos.AccountResponse register(AuthDtos.RegisterRequest request) {
        // Unicité contrôlée en amont pour retourner une erreur métier explicite.
        if (accountRepository.existsByEmail(request.email())) {
            throw new ApiException("Email already in use");
        }
        if (accountRepository.existsByUsername(request.username())) {
            throw new ApiException("Username already in use");
        }

        // Le mot de passe n'est jamais persisté en clair.
        Account account = Account.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        Account saved = accountRepository.save(account);
        return new AuthDtos.AccountResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        Account account = accountRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException("Invalid credentials"));

        // Message volontairement générique pour ne pas révéler si l'email existe.
        boolean ok = passwordEncoder.matches(request.password(), account.getPasswordHash());
        if (!ok) {
            throw new ApiException("Invalid credentials");
        }

        return new AuthDtos.LoginResponse(account.getId(), account.getUsername(), account.getEmail());
    }
}
