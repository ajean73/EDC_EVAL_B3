package fr.edc3.pmt.api;

import fr.edc3.pmt.api.dto.NotificationDtos;
import fr.edc3.pmt.api.dto.WorkItemDtos;
import fr.edc3.pmt.domain.service.NotificationService;
import fr.edc3.pmt.domain.service.WorkItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QueryController {

    // Endpoints de consultation/agrégation pour les écrans dashboard et notifications.

    private final WorkItemService workItemService;
    private final NotificationService notificationService;

    @GetMapping("/workspaces/{workspaceId}/dashboard")
    public List<WorkItemDtos.DashboardStatusResponse> dashboard(
            @org.springframework.web.bind.annotation.PathVariable Long workspaceId,
            @RequestParam Long actorAccountId
    ) {
        return workItemService.dashboard(workspaceId, actorAccountId);
    }

    @GetMapping("/notifications")
    public List<NotificationDtos.NotificationResponse> notifications(@RequestParam Long accountId) {
        return notificationService.findByAccountId(accountId);
    }
}
