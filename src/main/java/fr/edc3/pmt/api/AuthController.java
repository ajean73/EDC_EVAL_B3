package fr.edc3.pmt.api;

import fr.edc3.pmt.api.dto.AuthDtos;
import fr.edc3.pmt.domain.service.AuthService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Inscription et connexion des comptes utilisateurs")
public class AuthController {

    // Ce contrôleur expose uniquement des opérations d'identité (inscription et connexion).

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
        @Operation(
            summary = "Inscrire un utilisateur",
            description = "Crée un compte utilisateur après validation de l'unicité de l'email et du nom d'utilisateur.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Informations d'inscription.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthDtos.RegisterRequest.class),
                    examples = @ExampleObject(
                        name = "Exemple inscription",
                        value = """
                            {
                              \"username\": \"alice\",
                              \"email\": \"alice@acme.io\",
                              \"password\": \"MotDePasse123!\"
                            }
                            """
                    )
                )
            )
        )
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AuthDtos.AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Conflit métier (email/username déjà utilisé).",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "422", description = "Erreur de validation des données.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
        })
    public AuthDtos.AccountResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
        @Operation(
            summary = "Connecter un utilisateur",
            description = "Authentifie un utilisateur et retourne les informations de session.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Identifiants de connexion.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AuthDtos.LoginRequest.class),
                    examples = @ExampleObject(
                        name = "Exemple connexion",
                        value = """
                            {
                              \"email\": \"alice@acme.io\",
                              \"password\": \"MotDePasse123!\"
                            }
                            """
                    )
                )
            )
        )
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AuthDtos.LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Identifiants invalides.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "422", description = "Erreur de validation des données.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
        })
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }
}
