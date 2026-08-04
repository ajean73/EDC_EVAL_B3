package fr.edc3.pmt.api;

import fr.edc3.pmt.api.dto.NotificationDtos;
import fr.edc3.pmt.api.dto.WorkItemDtos;
import fr.edc3.pmt.domain.enums.NotificationKind;
import fr.edc3.pmt.domain.enums.WorkItemStatus;
import fr.edc3.pmt.domain.service.NotificationService;
import fr.edc3.pmt.domain.service.WorkItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryControllerIntegrationTest {

    @Mock
    private WorkItemService workItemService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private QueryController queryController;

    @Test
    void dashboard_shouldDelegateToWorkItemService() {
        List<WorkItemDtos.DashboardStatusResponse> expected = List.of(
                new WorkItemDtos.DashboardStatusResponse(WorkItemStatus.TODO, 3)
        );
        when(workItemService.dashboard(4L, 6L)).thenReturn(expected);

        List<WorkItemDtos.DashboardStatusResponse> actual = queryController.dashboard(4L, 6L);

        assertEquals(expected, actual);
    }

    @Test
    void notifications_shouldDelegateToNotificationService() {
        List<NotificationDtos.NotificationResponse> expected = List.of(
                new NotificationDtos.NotificationResponse(1L, 6L, 9L, NotificationKind.WORK_ITEM_ASSIGNED, true, LocalDateTime.now(), LocalDateTime.now())
        );
        when(notificationService.findByAccountId(6L)).thenReturn(expected);

        List<NotificationDtos.NotificationResponse> actual = queryController.notifications(6L);

        assertEquals(expected, actual);
    }
}
