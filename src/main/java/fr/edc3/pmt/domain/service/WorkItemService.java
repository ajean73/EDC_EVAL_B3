package fr.edc3.pmt.domain.service;

import fr.edc3.pmt.api.dto.WorkItemDtos;
import fr.edc3.pmt.domain.enums.HistoryAction;
import fr.edc3.pmt.domain.enums.MemberRole;
import fr.edc3.pmt.domain.enums.WorkItemStatus;
import fr.edc3.pmt.domain.model.WorkItem;
import fr.edc3.pmt.domain.model.WorkItemHistory;
import fr.edc3.pmt.domain.repository.TeamMemberRepository;
import fr.edc3.pmt.domain.repository.WorkItemHistoryRepository;
import fr.edc3.pmt.domain.repository.WorkItemRepository;
import fr.edc3.pmt.domain.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final WorkItemHistoryRepository workItemHistoryRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final PermissionService permissionService;
    private final NotificationService notificationService;

    @Transactional
    public WorkItemDtos.WorkItemResponse create(Long workspaceId, WorkItemDtos.CreateWorkItemRequest request) {
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));

        permissionService.requireRole(workspaceId, request.creatorAccountId(), MemberRole.ADMIN, MemberRole.MEMBER);

        if (request.assignedAccountId() != null &&
            !teamMemberRepository.existsByWorkspaceIdAndAccountId(workspaceId, request.assignedAccountId())) {
            throw new ApiException("Assigned account is not member of workspace");
        }

        WorkItem item = WorkItem.builder()
                .workspaceId(workspaceId)
                .title(request.title())
                .description(request.description())
                .dueDate(request.dueDate())
                .priority(request.priority())
                .creatorAccountId(request.creatorAccountId())
                .assignedAccountId(request.assignedAccountId())
                .build();

        WorkItem saved = workItemRepository.save(item);

        // Chaque modification majeure est historisée pour audit et traçabilité.
        workItemHistoryRepository.save(WorkItemHistory.builder()
                .workItemId(saved.getId())
                .changedBy(request.creatorAccountId())
                .action(HistoryAction.CREATED)
                .newValues("{\"status\":\"" + saved.getStatus() + "\",\"assigned_account_id\":" + saved.getAssignedAccountId() + "}")
                .build());

        if (saved.getAssignedAccountId() != null) {
            notificationService.createTaskAssignedNotification(saved.getAssignedAccountId(), saved.getId());
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkItemDtos.WorkItemResponse> findByWorkspace(Long workspaceId, Long actorAccountId) {
        permissionService.requireRole(workspaceId, actorAccountId, MemberRole.ADMIN, MemberRole.MEMBER, MemberRole.OBSERVER);
        return workItemRepository.findByWorkspaceId(workspaceId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkItemDtos.WorkItemResponse findOne(Long workspaceId, Long workItemId, Long actorAccountId) {
        permissionService.requireRole(workspaceId, actorAccountId, MemberRole.ADMIN, MemberRole.MEMBER, MemberRole.OBSERVER);
        WorkItem item = workItemRepository.findByIdAndWorkspaceId(workItemId, workspaceId)
                .orElseThrow(() -> new NotFoundException("Work item not found"));
        return toResponse(item);
    }

    @Transactional
    public WorkItemDtos.WorkItemResponse update(Long workspaceId, Long workItemId, WorkItemDtos.UpdateWorkItemRequest request) {
        permissionService.requireRole(workspaceId, request.actorAccountId(), MemberRole.ADMIN, MemberRole.MEMBER);

        WorkItem item = workItemRepository.findByIdAndWorkspaceId(workItemId, workspaceId)
                .orElseThrow(() -> new NotFoundException("Work item not found"));
        Long previousAssignedAccountId = item.getAssignedAccountId();

        if (request.assignedAccountId() != null &&
                !teamMemberRepository.existsByWorkspaceIdAndAccountId(workspaceId, request.assignedAccountId())) {
            throw new ApiException("Assigned account is not member of workspace");
        }

        String previousValues = toHistorySnapshot(item);

        if (request.title() != null) {
            item.setTitle(request.title());
        }
        if (request.description() != null) {
            item.setDescription(request.description());
        }
        if (request.dueDate() != null) {
            item.setDueDate(request.dueDate());
        }
        if (request.priority() != null) {
            item.setPriority(request.priority());
        }
        if (Boolean.TRUE.equals(request.clearAssignee())) {
            item.setAssignedAccountId(null);
        } else if (request.assignedAccountId() != null) {
            item.setAssignedAccountId(request.assignedAccountId());
        }
        if (request.status() != null) {
            item.setStatus(request.status());
            if (request.status() == WorkItemStatus.DONE && item.getCompletedAt() == null && request.completedAt() == null) {
                item.setCompletedAt(java.time.LocalDateTime.now());
            }
            // On nettoie completedAt si la tâche quitte l'état DONE sans date explicite.
            if (request.status() != WorkItemStatus.DONE && request.completedAt() == null) {
                item.setCompletedAt(null);
            }
        }
        if (request.completedAt() != null) {
            item.setCompletedAt(request.completedAt());
        }

        WorkItem saved = workItemRepository.save(item);

        workItemHistoryRepository.save(WorkItemHistory.builder()
                .workItemId(saved.getId())
                .changedBy(request.actorAccountId())
                .action(HistoryAction.UPDATED)
                .previousValues(previousValues)
                .newValues(toHistorySnapshot(saved))
                .build());

        // Notification uniquement si l'assignation a effectivement changé.
        if (!Objects.equals(previousAssignedAccountId, saved.getAssignedAccountId()) && saved.getAssignedAccountId() != null) {
            notificationService.createTaskAssignedNotification(saved.getAssignedAccountId(), saved.getId());
        }

        return toResponse(saved);
    }

    @Transactional
    public WorkItemDtos.WorkItemResponse assign(Long workspaceId, Long workItemId, WorkItemDtos.AssignWorkItemRequest request) {
        permissionService.requireRole(workspaceId, request.actorAccountId(), MemberRole.ADMIN, MemberRole.MEMBER);

        if (!teamMemberRepository.existsByWorkspaceIdAndAccountId(workspaceId, request.assignedAccountId())) {
            throw new ApiException("Assigned account is not member of workspace");
        }

        WorkItem item = workItemRepository.findByIdAndWorkspaceId(workItemId, workspaceId)
                .orElseThrow(() -> new NotFoundException("Work item not found"));

        String previousValues = toHistorySnapshot(item);

        item.setAssignedAccountId(request.assignedAccountId());
        WorkItem saved = workItemRepository.save(item);

        workItemHistoryRepository.save(WorkItemHistory.builder()
                .workItemId(saved.getId())
                .changedBy(request.actorAccountId())
                .action(HistoryAction.ASSIGNED)
                .previousValues(previousValues)
                .newValues(toHistorySnapshot(saved))
                .build());

        notificationService.createTaskAssignedNotification(saved.getAssignedAccountId(), saved.getId());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkItemDtos.WorkItemHistoryResponse> findHistory(Long workspaceId, Long workItemId, Long actorAccountId) {
        permissionService.requireRole(workspaceId, actorAccountId, MemberRole.ADMIN, MemberRole.MEMBER, MemberRole.OBSERVER);

        workItemRepository.findByIdAndWorkspaceId(workItemId, workspaceId)
                .orElseThrow(() -> new NotFoundException("Work item not found"));

        return workItemHistoryRepository.findByWorkItemIdOrderByChangedAtDesc(workItemId)
                .stream()
                .map(h -> new WorkItemDtos.WorkItemHistoryResponse(
                        h.getId(),
                        h.getWorkItemId(),
                        h.getChangedBy(),
                        h.getAction(),
                        h.getPreviousValues(),
                        h.getNewValues(),
                        h.getChangedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkItemDtos.DashboardStatusResponse> dashboard(Long workspaceId, Long actorAccountId) {
        permissionService.requireRole(workspaceId, actorAccountId, MemberRole.ADMIN, MemberRole.MEMBER, MemberRole.OBSERVER);

        return List.of(WorkItemStatus.values()).stream()
                .map(status -> new WorkItemDtos.DashboardStatusResponse(
                        status,
                        workItemRepository.countByWorkspaceIdAndStatus(workspaceId, status)
                ))
                .toList();
    }

        private WorkItemDtos.WorkItemResponse toResponse(WorkItem item) {
        return new WorkItemDtos.WorkItemResponse(
                item.getId(),
                item.getWorkspaceId(),
                item.getTitle(),
                item.getDescription(),
                item.getDueDate(),
                item.getPriority(),
                item.getStatus(),
                item.getCreatorAccountId(),
                item.getAssignedAccountId(),
                item.getCompletedAt(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private String toHistorySnapshot(WorkItem item) {
        return "{\"title\":\"" + item.getTitle() +
            "\",\"status\":\"" + item.getStatus() +
                "\",\"assigned_account_id\":" + item.getAssignedAccountId() +
            ",\"priority\":\"" + item.getPriority() +
            "\",\"due_date\":\"" + item.getDueDate() +
            "\",\"completed_at\":\"" + item.getCompletedAt() + "\"}";
    }
}
