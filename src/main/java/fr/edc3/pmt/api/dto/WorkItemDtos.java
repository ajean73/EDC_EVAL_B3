package fr.edc3.pmt.api.dto;

import fr.edc3.pmt.domain.enums.HistoryAction;
import fr.edc3.pmt.domain.enums.WorkItemPriority;
import fr.edc3.pmt.domain.enums.WorkItemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class WorkItemDtos {
    private WorkItemDtos() {
    }

    public record CreateWorkItemRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank String description,
            LocalDate dueDate,
            @NotNull WorkItemPriority priority,
            @NotNull Long creatorAccountId,
            Long assignedAccountId
    ) {
    }

    public record UpdateWorkItemRequest(
            String title,
            String description,
            LocalDate dueDate,
            WorkItemPriority priority,
            WorkItemStatus status,
            Long assignedAccountId,
            Boolean clearAssignee,
            LocalDateTime completedAt,
            @NotNull Long actorAccountId
    ) {
    }

    public record AssignWorkItemRequest(
            @NotNull Long assignedAccountId,
            @NotNull Long actorAccountId
    ) {
    }

    public record WorkItemResponse(
            Long id,
            Long workspaceId,
            String title,
            String description,
            LocalDate dueDate,
            WorkItemPriority priority,
            WorkItemStatus status,
            Long creatorAccountId,
            Long assignedAccountId,
            LocalDateTime completedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record WorkItemHistoryResponse(
            Long id,
            Long workItemId,
            Long changedBy,
            HistoryAction action,
            String previousValues,
            String newValues,
            LocalDateTime changedAt
    ) {
    }

    public record DashboardStatusResponse(
            WorkItemStatus status,
            long count
    ) {
    }
}
