package fr.edc3.pmt.domain.service;

import fr.edc3.pmt.api.dto.WorkItemDtos;
import fr.edc3.pmt.domain.enums.MemberRole;
import fr.edc3.pmt.domain.enums.WorkItemPriority;
import fr.edc3.pmt.domain.enums.WorkItemStatus;
import fr.edc3.pmt.domain.model.WorkItem;
import fr.edc3.pmt.domain.model.Workspace;
import fr.edc3.pmt.domain.repository.TeamMemberRepository;
import fr.edc3.pmt.domain.repository.WorkItemHistoryRepository;
import fr.edc3.pmt.domain.repository.WorkItemRepository;
import fr.edc3.pmt.domain.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkItemServiceTest {

    @Mock
    private WorkItemRepository workItemRepository;
    @Mock
    private WorkItemHistoryRepository workItemHistoryRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private PermissionService permissionService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WorkItemService workItemService;

    @Test
    void create_shouldFail_whenAssignedUserIsNotWorkspaceMember() {
        Long workspaceId = 2L;
        WorkItemDtos.CreateWorkItemRequest request = new WorkItemDtos.CreateWorkItemRequest(
                "Task", "Details", LocalDate.of(2026, 9, 10), WorkItemPriority.HIGH, 99L, 88L
        );

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(Workspace.builder().id(workspaceId).build()));
        when(teamMemberRepository.existsByWorkspaceIdAndAccountId(workspaceId, 88L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> workItemService.create(workspaceId, request));

        assertEquals("Assigned account is not member of workspace", ex.getMessage());
        verify(notificationService, never()).createTaskAssignedNotification(any(), any());
    }

    @Test
    void create_shouldCreateAndNotify_whenAssigneeExists() {
        Long workspaceId = 3L;
        WorkItemDtos.CreateWorkItemRequest request = new WorkItemDtos.CreateWorkItemRequest(
                "Task", "Details", LocalDate.of(2026, 9, 10), WorkItemPriority.MEDIUM, 7L, 9L
        );

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(Workspace.builder().id(workspaceId).build()));
        when(teamMemberRepository.existsByWorkspaceIdAndAccountId(workspaceId, 9L)).thenReturn(true);
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(55L);
            return item;
        });

        WorkItemDtos.WorkItemResponse result = workItemService.create(workspaceId, request);

        assertEquals(55L, result.id());
        verify(workItemHistoryRepository).save(any());
        verify(notificationService).createTaskAssignedNotification(9L, 55L);
    }

    @Test
    void update_shouldClearAssigneeAndSetCompletedAtForDone() {
        Long workspaceId = 4L;
        WorkItem current = WorkItem.builder()
                .id(11L)
                .workspaceId(workspaceId)
                .title("Old")
                .description("Old desc")
                .priority(WorkItemPriority.LOW)
                .status(WorkItemStatus.TODO)
                .creatorAccountId(2L)
                .assignedAccountId(9L)
                .build();

        WorkItemDtos.UpdateWorkItemRequest request = new WorkItemDtos.UpdateWorkItemRequest(
                "New", "New desc", LocalDate.of(2026, 9, 20), WorkItemPriority.HIGH,
                WorkItemStatus.DONE, null, true, null, 2L
        );

        when(workItemRepository.findByIdAndWorkspaceId(11L, workspaceId)).thenReturn(Optional.of(current));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkItemDtos.WorkItemResponse result = workItemService.update(workspaceId, 11L, request);

        assertEquals("New", result.title());
        assertEquals(WorkItemStatus.DONE, result.status());
        assertNull(result.assignedAccountId());
        assertNotNull(result.completedAt());
        verify(workItemHistoryRepository).save(any());
    }

    @Test
    void assign_shouldFail_whenAccountIsNotMember() {
        WorkItemDtos.AssignWorkItemRequest request = new WorkItemDtos.AssignWorkItemRequest(88L, 1L);

        when(teamMemberRepository.existsByWorkspaceIdAndAccountId(2L, 88L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> workItemService.assign(2L, 3L, request));

        assertEquals("Assigned account is not member of workspace", ex.getMessage());
    }

    @Test
    void dashboard_shouldReturnAllStatusesWithCounts() {
        when(workItemRepository.countByWorkspaceIdAndStatus(2L, WorkItemStatus.TODO)).thenReturn(3L);
        when(workItemRepository.countByWorkspaceIdAndStatus(2L, WorkItemStatus.IN_PROGRESS)).thenReturn(2L);
        when(workItemRepository.countByWorkspaceIdAndStatus(2L, WorkItemStatus.IN_REVIEW)).thenReturn(1L);
        when(workItemRepository.countByWorkspaceIdAndStatus(2L, WorkItemStatus.DONE)).thenReturn(4L);

        List<WorkItemDtos.DashboardStatusResponse> result = workItemService.dashboard(2L, 1L);

        assertEquals(4, result.size());
        assertEquals(3L, result.stream().filter(r -> r.status() == WorkItemStatus.TODO).findFirst().orElseThrow().count());
        assertEquals(4L, result.stream().filter(r -> r.status() == WorkItemStatus.DONE).findFirst().orElseThrow().count());
    }

    @Test
    void findOne_shouldThrow_whenItemDoesNotExist() {
        when(workItemRepository.findByIdAndWorkspaceId(4L, 2L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> workItemService.findOne(2L, 4L, 1L));

        assertEquals("Work item not found", ex.getMessage());
    }

    @Test
    void findByWorkspace_shouldReturnMappedItems() {
        when(workItemRepository.findByWorkspaceId(2L)).thenReturn(List.of(
                WorkItem.builder()
                        .id(1L)
                        .workspaceId(2L)
                        .title("Task")
                        .description("Desc")
                        .priority(WorkItemPriority.MEDIUM)
                        .status(WorkItemStatus.TODO)
                        .creatorAccountId(10L)
                        .build()
        ));

        List<WorkItemDtos.WorkItemResponse> result = workItemService.findByWorkspace(2L, 1L);

        assertEquals(1, result.size());
        assertEquals("Task", result.get(0).title());
    }

    @Test
    void update_shouldThrow_whenAssignedAccountIsNotMember() {
        WorkItem current = WorkItem.builder()
                .id(11L)
                .workspaceId(4L)
                .title("Old")
                .description("Old desc")
                .priority(WorkItemPriority.LOW)
                .status(WorkItemStatus.TODO)
                .creatorAccountId(2L)
                .build();

        WorkItemDtos.UpdateWorkItemRequest request = new WorkItemDtos.UpdateWorkItemRequest(
                null, null, null, null, null, 88L, null, null, 2L
        );

        when(workItemRepository.findByIdAndWorkspaceId(11L, 4L)).thenReturn(Optional.of(current));
        when(teamMemberRepository.existsByWorkspaceIdAndAccountId(4L, 88L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> workItemService.update(4L, 11L, request));

        assertEquals("Assigned account is not member of workspace", ex.getMessage());
    }

    @Test
    void assign_shouldNotify_whenAssignmentSucceeds() {
        WorkItem item = WorkItem.builder()
                .id(3L)
                .workspaceId(2L)
                .title("Task")
                .description("Desc")
                .priority(WorkItemPriority.MEDIUM)
                .status(WorkItemStatus.IN_PROGRESS)
                .creatorAccountId(1L)
                .assignedAccountId(null)
                .build();

        WorkItemDtos.AssignWorkItemRequest request = new WorkItemDtos.AssignWorkItemRequest(5L, 1L);

        when(teamMemberRepository.existsByWorkspaceIdAndAccountId(2L, 5L)).thenReturn(true);
        when(workItemRepository.findByIdAndWorkspaceId(3L, 2L)).thenReturn(Optional.of(item));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkItemDtos.WorkItemResponse result = workItemService.assign(2L, 3L, request);

        assertEquals(5L, result.assignedAccountId());
        verify(workItemHistoryRepository).save(any());
        verify(notificationService).createTaskAssignedNotification(5L, 3L);
    }

    @Test
    void update_shouldClearCompletedAt_whenStatusMovesOutOfDone() {
        WorkItem current = WorkItem.builder()
                .id(14L)
                .workspaceId(4L)
                .title("Done task")
                .description("Old desc")
                .priority(WorkItemPriority.HIGH)
                .status(WorkItemStatus.DONE)
                .creatorAccountId(2L)
                .completedAt(LocalDateTime.now().minusDays(1))
                .build();

        WorkItemDtos.UpdateWorkItemRequest request = new WorkItemDtos.UpdateWorkItemRequest(
                null, null, null, null, WorkItemStatus.IN_PROGRESS, null, null, null, 2L
        );

        when(workItemRepository.findByIdAndWorkspaceId(14L, 4L)).thenReturn(Optional.of(current));
        when(workItemRepository.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkItemDtos.WorkItemResponse result = workItemService.update(4L, 14L, request);

        assertEquals(WorkItemStatus.IN_PROGRESS, result.status());
        assertNull(result.completedAt());
    }
}
