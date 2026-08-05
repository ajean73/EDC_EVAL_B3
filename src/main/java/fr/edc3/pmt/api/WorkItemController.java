package fr.edc3.pmt.api;

import fr.edc3.pmt.api.dto.WorkItemDtos;
import fr.edc3.pmt.domain.service.WorkItemService;
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
@RequestMapping("/api/workspaces/{workspaceId}/work-items")
@RequiredArgsConstructor
@Tag(name = "Work Items", description = "Gestion des taches, assignations et historique")
public class WorkItemController {

    // Les changements métier (droits, historique, transitions) sont gérés dans le service.

    private final WorkItemService workItemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Créer une tâche",
        description = "Crée une tâche dans un workspace avec priorité et assignation initiale optionnelle.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Données de création de la tâche.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WorkItemDtos.CreateWorkItemRequest.class),
                examples = @ExampleObject(
                    name = "Exemple création tâche",
                    value = """
                        {
                          \"title\": \"Implémenter la recherche\",
                          \"description\": \"Ajouter un filtre multi-critères\",
                          \"dueDate\": \"2026-08-31\",
                          \"priority\": \"HIGH\",
                          \"creatorAccountId\": 1,
                          \"assignedAccountId\": 2
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tâche créée.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WorkItemDtos.WorkItemResponse.class))),
        @ApiResponse(responseCode = "400", description = "Règle métier invalide.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "Workspace ou compte introuvable.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public WorkItemDtos.WorkItemResponse create(
        @Parameter(description = "Identifiant du workspace.", example = "1")
        @PathVariable Long workspaceId,
            @Valid @RequestBody WorkItemDtos.CreateWorkItemRequest request
    ) {
        return workItemService.create(workspaceId, request);
    }

    @GetMapping
    @Operation(summary = "Lister les tâches", description = "Retourne les tâches d'un workspace visibles par le compte acteur.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Liste des tâches.", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "Paramètre invalide.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public List<WorkItemDtos.WorkItemResponse> findByWorkspace(
        @Parameter(description = "Identifiant du workspace.", example = "1")
        @PathVariable Long workspaceId,
        @Parameter(description = "Identifiant du compte acteur (membre du workspace).", example = "2")
            @RequestParam Long actorAccountId
    ) {
        return workItemService.findByWorkspace(workspaceId, actorAccountId);
    }

    @GetMapping("/{workItemId}")
    @Operation(summary = "Récupérer une tâche", description = "Retourne le détail d'une tâche par identifiant dans le workspace.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Détail de la tâche.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WorkItemDtos.WorkItemResponse.class))),
        @ApiResponse(responseCode = "404", description = "Tâche ou workspace introuvable.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public WorkItemDtos.WorkItemResponse findOne(
        @Parameter(description = "Identifiant du workspace.", example = "1")
        @PathVariable Long workspaceId,
        @Parameter(description = "Identifiant de la tâche.", example = "10")
        @PathVariable Long workItemId,
        @Parameter(description = "Identifiant du compte acteur.", example = "2")
            @RequestParam Long actorAccountId
    ) {
        return workItemService.findOne(workspaceId, workItemId, actorAccountId);
    }

    @PatchMapping("/{workItemId}")
    @Operation(
        summary = "Modifier une tâche",
        description = "Met à jour les attributs d'une tâche (titre, statut, priorité, assignation, dates).",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Champs à mettre à jour. Tous les champs sont optionnels sauf actorAccountId.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WorkItemDtos.UpdateWorkItemRequest.class),
                examples = @ExampleObject(
                    name = "Exemple mise à jour",
                    value = """
                        {
                          \"status\": \"IN_PROGRESS\",
                          \"priority\": \"MEDIUM\",
                          \"actorAccountId\": 2
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tâche mise à jour.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WorkItemDtos.WorkItemResponse.class))),
        @ApiResponse(responseCode = "400", description = "Règle métier invalide.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "Tâche ou workspace introuvable.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public WorkItemDtos.WorkItemResponse update(
        @Parameter(description = "Identifiant du workspace.", example = "1")
        @PathVariable Long workspaceId,
        @Parameter(description = "Identifiant de la tâche.", example = "10")
        @PathVariable Long workItemId,
            @Valid @RequestBody WorkItemDtos.UpdateWorkItemRequest request
    ) {
        return workItemService.update(workspaceId, workItemId, request);
    }

    @PatchMapping("/{workItemId}/assignee")
    @Operation(
        summary = "Assigner une tâche",
        description = "Assigne explicitement une tâche à un membre du workspace.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Compte cible et acteur qui réalise l'assignation.",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WorkItemDtos.AssignWorkItemRequest.class),
                examples = @ExampleObject(
                    name = "Exemple assignation",
                    value = """
                        {
                          \"assignedAccountId\": 3,
                          \"actorAccountId\": 1
                        }
                        """
                )
            )
        )
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tâche assignée.",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WorkItemDtos.WorkItemResponse.class))),
        @ApiResponse(responseCode = "400", description = "Règle métier invalide.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "404", description = "Tâche, workspace ou compte introuvable.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public WorkItemDtos.WorkItemResponse assign(
        @Parameter(description = "Identifiant du workspace.", example = "1")
        @PathVariable Long workspaceId,
        @Parameter(description = "Identifiant de la tâche.", example = "10")
        @PathVariable Long workItemId,
            @Valid @RequestBody WorkItemDtos.AssignWorkItemRequest request
    ) {
        return workItemService.assign(workspaceId, workItemId, request);
    }

    @GetMapping("/{workItemId}/history")
    @Operation(summary = "Consulter l'historique", description = "Retourne l'historique des modifications de la tâche en ordre antéchronologique.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historique des changements.", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Tâche ou workspace introuvable.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public List<WorkItemDtos.WorkItemHistoryResponse> history(
        @Parameter(description = "Identifiant du workspace.", example = "1")
        @PathVariable Long workspaceId,
        @Parameter(description = "Identifiant de la tâche.", example = "10")
        @PathVariable Long workItemId,
        @Parameter(description = "Identifiant du compte acteur.", example = "2")
            @RequestParam Long actorAccountId
    ) {
        return workItemService.findHistory(workspaceId, workItemId, actorAccountId);
    }
}
