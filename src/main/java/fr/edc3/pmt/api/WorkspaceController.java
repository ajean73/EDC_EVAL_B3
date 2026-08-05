package fr.edc3.pmt.api;

import fr.edc3.pmt.api.dto.WorkspaceDtos;
import fr.edc3.pmt.domain.service.WorkspaceService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspaces", description = "Gestion des workspaces, membres et invitations")
public class WorkspaceController {

    // Les vérifications d'autorisations métier sont centralisées dans la couche service.

    private final WorkspaceService workspaceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
        @Operation(
            summary = "Créer un workspace",
            description = "Crée un nouveau workspace et rattache automatiquement le propriétaire avec le rôle ADMIN.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Données de création du workspace.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = WorkspaceDtos.CreateWorkspaceRequest.class),
                    examples = @ExampleObject(
                        name = "Exemple création workspace",
                        value = """
                            {
                              \"name\": \"Projet Atlas\",
                              \"description\": \"Pilotage du produit Atlas\",
                              \"startDate\": \"2026-08-05\",
                              \"ownerAccountId\": 1
                            }
                            """
                    )
                )
            )
        )
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Workspace créé.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WorkspaceDtos.WorkspaceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Règle métier invalide.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "422", description = "Erreur de validation.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
        })
    public WorkspaceDtos.WorkspaceResponse create(@Valid @RequestBody WorkspaceDtos.CreateWorkspaceRequest request) {
        return workspaceService.createWorkspace(request);
    }

    @GetMapping
        @Operation(summary = "Lister les workspaces accessibles", description = "Retourne les workspaces auxquels le compte acteur appartient.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des workspaces.",
                content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètre invalide.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
        })
    public List<WorkspaceDtos.WorkspaceResponse> findAll(@RequestParam Long actorAccountId) {
        return workspaceService.findAll(actorAccountId);
    }

    @GetMapping("/{workspaceId}")
        @Operation(summary = "Récupérer un workspace", description = "Retourne le détail d'un workspace par identifiant.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Détail du workspace.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WorkspaceDtos.WorkspaceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Workspace introuvable.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
        })
    public WorkspaceDtos.WorkspaceResponse findById(@PathVariable Long workspaceId) {
        return workspaceService.findById(workspaceId);
    }

    @GetMapping("/{workspaceId}/members")
        @Operation(summary = "Lister les membres", description = "Retourne la liste des membres d'un workspace avec leur rôle.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des membres.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Workspace introuvable.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
        })
    public List<WorkspaceDtos.TeamMemberResponse> members(@PathVariable Long workspaceId) {
        return workspaceService.findMembers(workspaceId);
    }

    @PostMapping("/{workspaceId}/members")
    @ResponseStatus(HttpStatus.CREATED)
        @Operation(
            summary = "Ajouter un membre",
            description = "Ajoute un compte au workspace avec le rôle indiqué (action réservée aux admins).",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                description = "Compte cible, rôle et acteur qui réalise l'action.",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = WorkspaceDtos.AddMemberRequest.class),
                    examples = @ExampleObject(
                        name = "Exemple ajout membre",
                        value = """
                            {
                              \"accountId\": 2,
                              \"role\": \"EDITOR\",
                              \"actorAccountId\": 1
                            }
                            """
                    )
                )
            )
        )
        @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Membre ajouté.",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WorkspaceDtos.TeamMemberResponse.class))),
            @ApiResponse(responseCode = "400", description = "Règle métier invalide.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Workspace ou compte introuvable.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
        })
    public WorkspaceDtos.TeamMemberResponse addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody WorkspaceDtos.AddMemberRequest request
    ) {
        return workspaceService.addMember(workspaceId, request);
    }

    @PatchMapping("/{workspaceId}/members/{accountId}/role")
    @Operation(
        summary = "Modifier le rôle d'un membre",
        description = "Met à jour le rôle d'un membre de workspace (action réservée aux admins).",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Nouveau rôle et acteur qui réalise l'action.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WorkspaceDtos.UpdateRoleRequest.class),
                examples = @ExampleObject(
                    name = "Exemple mise à jour rôle",
                    value = """
                        {
                          \"role\": \"ADMIN\",
                          \"actorAccountId\": 1
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rôle membre mis à jour.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WorkspaceDtos.TeamMemberResponse.class))),
        @ApiResponse(responseCode = "400", description = "Règle métier invalide.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "Workspace ou membre introuvable.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public WorkspaceDtos.TeamMemberResponse updateRole(
            @PathVariable Long workspaceId,
            @PathVariable Long accountId,
            @Valid @RequestBody WorkspaceDtos.UpdateRoleRequest request
    ) {
        return workspaceService.updateRole(workspaceId, accountId, request);
    }

    @PostMapping("/{workspaceId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Envoyer une invitation",
        description = "Crée une invitation par email pour rejoindre un workspace avec un rôle cible.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Email invité, rôle proposé et acteur qui envoie l'invitation.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WorkspaceDtos.InvitationRequest.class),
                examples = @ExampleObject(
                    name = "Exemple invitation",
                    value = """
                        {
                          \"inviteeEmail\": \"bob@acme.io\",
                          \"role\": \"VIEWER\",
                          \"actorAccountId\": 1
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Invitation créée.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WorkspaceDtos.InvitationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Règle métier invalide.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "Workspace introuvable.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public WorkspaceDtos.InvitationResponse invite(
            @PathVariable Long workspaceId,
            @Valid @RequestBody WorkspaceDtos.InvitationRequest request
    ) {
        return workspaceService.invite(workspaceId, request);
    }

    @GetMapping("/invitations/mine")
    @Operation(summary = "Lister mes invitations", description = "Retourne les invitations PENDING associées à l'email du compte acteur.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des invitations en attente.", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Paramètre invalide.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public List<WorkspaceDtos.InvitationResponse> myInvitations(@RequestParam Long actorAccountId) {
        return workspaceService.findMyPendingInvitations(actorAccountId);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    @Operation(
        summary = "Accepter une invitation",
        description = "Accepte une invitation en attente et ajoute le compte acteur comme membre du workspace.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Compte acteur qui accepte l'invitation.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WorkspaceDtos.InvitationActionRequest.class),
                examples = @ExampleObject(
                    name = "Exemple acceptation",
                    value = """
                        {
                          \"actorAccountId\": 2
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation acceptée.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WorkspaceDtos.InvitationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Règle métier invalide.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "Invitation introuvable.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public WorkspaceDtos.InvitationResponse acceptInvitation(
            @PathVariable Long invitationId,
            @Valid @RequestBody WorkspaceDtos.InvitationActionRequest request
    ) {
        return workspaceService.acceptInvitation(invitationId, request.actorAccountId());
    }

    @PostMapping("/invitations/{invitationId}/decline")
    @Operation(
        summary = "Refuser une invitation",
        description = "Refuse une invitation en attente pour le compte acteur.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Compte acteur qui refuse l'invitation.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WorkspaceDtos.InvitationActionRequest.class),
                examples = @ExampleObject(
                    name = "Exemple refus",
                    value = """
                        {
                          \"actorAccountId\": 2
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitation refusée.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WorkspaceDtos.InvitationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Règle métier invalide.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "Invitation introuvable.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public WorkspaceDtos.InvitationResponse declineInvitation(
            @PathVariable Long invitationId,
            @Valid @RequestBody WorkspaceDtos.InvitationActionRequest request
    ) {
        return workspaceService.declineInvitation(invitationId, request.actorAccountId());
    }
}
