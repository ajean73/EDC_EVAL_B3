package fr.edc3.pmt.api;

import fr.edc3.pmt.api.dto.WorkItemDtos;
import fr.edc3.pmt.domain.enums.HistoryAction;
import fr.edc3.pmt.domain.enums.WorkItemPriority;
import fr.edc3.pmt.domain.enums.WorkItemStatus;
import fr.edc3.pmt.domain.service.WorkItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkItemControllerIntegrationTest {

    @Mock
    private WorkItemService workItemService;

    @InjectMocks
    private WorkItemController workItemController;

    @Test
    void create_shouldDelegateToService() {
        WorkItemDtos.CreateWorkItemRequest request = new WorkItemDtos.CreateWorkItemRequest(
                "Task", "Desc", LocalDate.of(2026, 8, 20), WorkItemPriority.HIGH, 3L, null
        );
        WorkItemDtos.WorkItemResponse expected = new WorkItemDtos.WorkItemResponse(
                11L, 2L, "Task", "Desc", LocalDate.of(2026, 8, 20), WorkItemPriority.HIGH,
                WorkItemStatus.TODO, 3L, null, null, null, null
        );
        when(workItemService.create(2L, request)).thenReturn(expected);

        WorkItemDtos.WorkItemResponse actual = workItemController.create(2L, request);

        assertEquals(expected, actual);
    }

    @Test
    void update_shouldDelegateToService() {
        WorkItemDtos.UpdateWorkItemRequest request = new WorkItemDtos.UpdateWorkItemRequest(
                "Updated", "Desc", null, WorkItemPriority.MEDIUM, WorkItemStatus.IN_PROGRESS, null, null, null, 3L
        );
        WorkItemDtos.WorkItemResponse expected = new WorkItemDtos.WorkItemResponse(
                11L, 2L, "Updated", "Desc", null, WorkItemPriority.MEDIUM,
                WorkItemStatus.IN_PROGRESS, 3L, null, null, null, null
        );
        when(workItemService.update(2L, 11L, request)).thenReturn(expected);

        WorkItemDtos.WorkItemResponse actual = workItemController.update(2L, 11L, request);

        assertEquals(expected, actual);
    }

    @Test
    void history_shouldDelegateToService() {
        List<WorkItemDtos.WorkItemHistoryResponse> expected = List.of(
                new WorkItemDtos.WorkItemHistoryResponse(1L, 11L, 3L, HistoryAction.UPDATED, "{}", "{}", LocalDateTime.now())
        );
        when(workItemService.findHistory(2L, 11L, 3L)).thenReturn(expected);

        List<WorkItemDtos.WorkItemHistoryResponse> actual = workItemController.history(2L, 11L, 3L);

        assertEquals(expected, actual);
    }
}
