package fr.edc3.pmt.api;

import fr.edc3.pmt.api.dto.WorkspaceDtos;
import fr.edc3.pmt.domain.enums.InvitationState;
import fr.edc3.pmt.domain.enums.MemberRole;
import fr.edc3.pmt.domain.service.WorkspaceService;
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
class WorkspaceControllerIntegrationTest {

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private WorkspaceController workspaceController;

    @Test
    void findAll_shouldReturnWorkspaceListFromService() {
        List<WorkspaceDtos.WorkspaceResponse> expected = List.of(
                new WorkspaceDtos.WorkspaceResponse(1L, "Projet", "Description", LocalDate.of(2026, 8, 1), 4L, LocalDateTime.now())
        );
        when(workspaceService.findAll(4L)).thenReturn(expected);

        List<WorkspaceDtos.WorkspaceResponse> actual = workspaceController.findAll(4L);

        assertEquals(expected, actual);
    }

    @Test
    void invite_shouldReturnCreatedInvitationFromService() {
        WorkspaceDtos.InvitationRequest request = new WorkspaceDtos.InvitationRequest("member@pmt.local", MemberRole.MEMBER, 4L);
        WorkspaceDtos.InvitationResponse expected = new WorkspaceDtos.InvitationResponse(
                9L,
                3L,
                "member@pmt.local",
                MemberRole.MEMBER,
                InvitationState.PENDING,
                4L,
                "admin",
                LocalDateTime.now(),
                null
        );
        when(workspaceService.invite(3L, request)).thenReturn(expected);

        WorkspaceDtos.InvitationResponse actual = workspaceController.invite(3L, request);

        assertEquals(expected, actual);
    }

    @Test
    void acceptInvitation_shouldDelegateToService() {
        WorkspaceDtos.InvitationActionRequest request = new WorkspaceDtos.InvitationActionRequest(5L);
        WorkspaceDtos.InvitationResponse expected = new WorkspaceDtos.InvitationResponse(
                8L,
                3L,
                "member@pmt.local",
                MemberRole.MEMBER,
                InvitationState.ACCEPTED,
                4L,
                "admin",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );
        when(workspaceService.acceptInvitation(8L, 5L)).thenReturn(expected);

        WorkspaceDtos.InvitationResponse actual = workspaceController.acceptInvitation(8L, request);

        assertEquals(expected, actual);
    }

    @Test
    void create_shouldDelegateToService() {
        WorkspaceDtos.CreateWorkspaceRequest request = new WorkspaceDtos.CreateWorkspaceRequest(
                "Projet", "Description", LocalDate.of(2026, 8, 1), 4L
        );
        WorkspaceDtos.WorkspaceResponse expected = new WorkspaceDtos.WorkspaceResponse(
                2L, "Projet", "Description", LocalDate.of(2026, 8, 1), 4L, LocalDateTime.now()
        );
        when(workspaceService.createWorkspace(request)).thenReturn(expected);

        WorkspaceDtos.WorkspaceResponse actual = workspaceController.create(request);

        assertEquals(expected, actual);
    }

    @Test
    void findById_shouldDelegateToService() {
        WorkspaceDtos.WorkspaceResponse expected = new WorkspaceDtos.WorkspaceResponse(
                7L, "Projet", "Description", LocalDate.of(2026, 8, 1), 4L, LocalDateTime.now()
        );
        when(workspaceService.findById(7L)).thenReturn(expected);

        WorkspaceDtos.WorkspaceResponse actual = workspaceController.findById(7L);

        assertEquals(expected, actual);
    }

    @Test
    void members_shouldDelegateToService() {
        List<WorkspaceDtos.TeamMemberResponse> expected = List.of(
                new WorkspaceDtos.TeamMemberResponse(1L, 7L, 4L, "admin", MemberRole.ADMIN, LocalDateTime.now())
        );
        when(workspaceService.findMembers(7L)).thenReturn(expected);

        List<WorkspaceDtos.TeamMemberResponse> actual = workspaceController.members(7L);

        assertEquals(expected, actual);
    }

    @Test
    void addMember_shouldDelegateToService() {
        WorkspaceDtos.AddMemberRequest request = new WorkspaceDtos.AddMemberRequest(8L, MemberRole.MEMBER, 4L);
        WorkspaceDtos.TeamMemberResponse expected = new WorkspaceDtos.TeamMemberResponse(
                10L, 7L, 8L, "new-user", MemberRole.MEMBER, LocalDateTime.now()
        );
        when(workspaceService.addMember(7L, request)).thenReturn(expected);

        WorkspaceDtos.TeamMemberResponse actual = workspaceController.addMember(7L, request);

        assertEquals(expected, actual);
    }

    @Test
    void updateRole_shouldDelegateToService() {
        WorkspaceDtos.UpdateRoleRequest request = new WorkspaceDtos.UpdateRoleRequest(MemberRole.OBSERVER, 4L);
        WorkspaceDtos.TeamMemberResponse expected = new WorkspaceDtos.TeamMemberResponse(
                10L, 7L, 8L, "user", MemberRole.OBSERVER, LocalDateTime.now()
        );
        when(workspaceService.updateRole(7L, 8L, request)).thenReturn(expected);

        WorkspaceDtos.TeamMemberResponse actual = workspaceController.updateRole(7L, 8L, request);

        assertEquals(expected, actual);
    }

    @Test
    void myInvitations_shouldDelegateToService() {
        List<WorkspaceDtos.InvitationResponse> expected = List.of(
                new WorkspaceDtos.InvitationResponse(1L, 7L, "mail@pmt.local", MemberRole.MEMBER, InvitationState.PENDING, 4L, "admin", LocalDateTime.now(), null)
        );
        when(workspaceService.findMyPendingInvitations(8L)).thenReturn(expected);

        List<WorkspaceDtos.InvitationResponse> actual = workspaceController.myInvitations(8L);

        assertEquals(expected, actual);
    }

    @Test
    void declineInvitation_shouldDelegateToService() {
        WorkspaceDtos.InvitationActionRequest request = new WorkspaceDtos.InvitationActionRequest(5L);
        WorkspaceDtos.InvitationResponse expected = new WorkspaceDtos.InvitationResponse(
                8L,
                3L,
                "member@pmt.local",
                MemberRole.MEMBER,
                InvitationState.DECLINED,
                4L,
                "admin",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );
        when(workspaceService.declineInvitation(8L, 5L)).thenReturn(expected);

        WorkspaceDtos.InvitationResponse actual = workspaceController.declineInvitation(8L, request);

        assertEquals(expected, actual);
    }
}
