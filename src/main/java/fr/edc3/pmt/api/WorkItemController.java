package fr.edc3.pmt.api;

import fr.edc3.pmt.api.dto.WorkItemDtos;
import fr.edc3.pmt.domain.service.WorkItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/work-items")
@RequiredArgsConstructor
public class WorkItemController {

    private final WorkItemService workItemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkItemDtos.WorkItemResponse create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody WorkItemDtos.CreateWorkItemRequest request
    ) {
        return workItemService.create(workspaceId, request);
    }

    @GetMapping
    public List<WorkItemDtos.WorkItemResponse> findByWorkspace(
            @PathVariable Long workspaceId,
            @RequestParam Long actorAccountId
    ) {
        return workItemService.findByWorkspace(workspaceId, actorAccountId);
    }

    @GetMapping("/{workItemId}")
    public WorkItemDtos.WorkItemResponse findOne(
            @PathVariable Long workspaceId,
            @PathVariable Long workItemId,
            @RequestParam Long actorAccountId
    ) {
        return workItemService.findOne(workspaceId, workItemId, actorAccountId);
    }

    @PatchMapping("/{workItemId}")
    public WorkItemDtos.WorkItemResponse update(
            @PathVariable Long workspaceId,
            @PathVariable Long workItemId,
            @Valid @RequestBody WorkItemDtos.UpdateWorkItemRequest request
    ) {
        return workItemService.update(workspaceId, workItemId, request);
    }

    @PatchMapping("/{workItemId}/assignee")
    public WorkItemDtos.WorkItemResponse assign(
            @PathVariable Long workspaceId,
            @PathVariable Long workItemId,
            @Valid @RequestBody WorkItemDtos.AssignWorkItemRequest request
    ) {
        return workItemService.assign(workspaceId, workItemId, request);
    }

    @GetMapping("/{workItemId}/history")
    public List<WorkItemDtos.WorkItemHistoryResponse> history(
            @PathVariable Long workspaceId,
            @PathVariable Long workItemId,
            @RequestParam Long actorAccountId
    ) {
        return workItemService.findHistory(workspaceId, workItemId, actorAccountId);
    }
}
