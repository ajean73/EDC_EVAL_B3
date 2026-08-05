package fr.edc3.pmt.api;

import fr.edc3.pmt.api.dto.NotificationDtos;
import fr.edc3.pmt.api.dto.WorkItemDtos;
import fr.edc3.pmt.domain.service.NotificationService;
import fr.edc3.pmt.domain.service.WorkItemService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Queries", description = "Endpoints de consultation (dashboard et notifications)")
public class QueryController {

    // Endpoints de consultation/agrégation pour les écrans dashboard et notifications.

    private final WorkItemService workItemService;
    private final NotificationService notificationService;

    @GetMapping("/workspaces/{workspaceId}/dashboard")
        @Operation(summary = "Dashboard par statut", description = "Retourne le nombre de tâches par statut pour un workspace donné.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agrégation des statuts.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Workspace introuvable.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
        })
    public List<WorkItemDtos.DashboardStatusResponse> dashboard(
            @Parameter(description = "Identifiant du workspace.", example = "1")
            @PathVariable Long workspaceId,
            @Parameter(description = "Identifiant du compte acteur.", example = "2")
            @RequestParam Long actorAccountId
    ) {
        return workItemService.dashboard(workspaceId, actorAccountId);
    }

    @GetMapping("/notifications")
        @Operation(summary = "Lister les notifications", description = "Retourne les notifications d'un compte en ordre antéchronologique.")
        @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste des notifications.", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "400", description = "Paramètre invalide.",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
        })
    public List<NotificationDtos.NotificationResponse> notifications(@RequestParam Long accountId) {
        return notificationService.findByAccountId(accountId);
    }
}
