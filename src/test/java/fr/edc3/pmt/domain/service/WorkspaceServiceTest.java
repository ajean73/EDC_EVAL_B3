package fr.edc3.pmt.domain.service;

import fr.edc3.pmt.api.dto.WorkspaceDtos;
import fr.edc3.pmt.domain.enums.InvitationState;
import fr.edc3.pmt.domain.enums.MemberRole;
import fr.edc3.pmt.domain.model.Account;
import fr.edc3.pmt.domain.model.ProjectInvitation;
import fr.edc3.pmt.domain.model.TeamMember;
import fr.edc3.pmt.domain.model.Workspace;
import fr.edc3.pmt.domain.repository.AccountRepository;
import fr.edc3.pmt.domain.repository.ProjectInvitationRepository;
import fr.edc3.pmt.domain.repository.TeamMemberRepository;
import fr.edc3.pmt.domain.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private TeamMemberRepository teamMemberRepository;
    @Mock
    private ProjectInvitationRepository invitationRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void createWorkspace_shouldCreateWorkspaceAndOwnerMembership() {
        WorkspaceDtos.CreateWorkspaceRequest request = new WorkspaceDtos.CreateWorkspaceRequest(
                "Projet A", "Description", LocalDate.of(2026, 8, 1), 10L
        );

        when(accountRepository.findById(10L)).thenReturn(Optional.of(Account.builder().id(10L).build()));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
            Workspace w = invocation.getArgument(0);
            w.setId(21L);
            return w;
        });

        WorkspaceDtos.WorkspaceResponse response = workspaceService.createWorkspace(request);

        assertEquals(21L, response.id());
        assertEquals("Projet A", response.name());

        ArgumentCaptor<TeamMember> memberCaptor = ArgumentCaptor.forClass(TeamMember.class);
        verify(teamMemberRepository).save(memberCaptor.capture());
        assertEquals(21L, memberCaptor.getValue().getWorkspaceId());
        assertEquals(10L, memberCaptor.getValue().getAccountId());
        assertEquals(MemberRole.ADMIN, memberCaptor.getValue().getRole());
    }

    @Test
    void findAll_shouldReturnEmptyList_whenUserHasNoWorkspace() {
        when(teamMemberRepository.findByAccountId(99L)).thenReturn(List.of());

        List<WorkspaceDtos.WorkspaceResponse> result = workspaceService.findAll(99L);

        assertTrue(result.isEmpty());
        verify(workspaceRepository, never()).findAllById(any());
    }

    @Test
    void findMembers_shouldReturnUsernameFallbackForUnknownAccount() {
        Long workspaceId = 5L;
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(Workspace.builder().id(workspaceId).build()));
        when(teamMemberRepository.findByWorkspaceId(workspaceId))
                .thenReturn(List.of(
                        TeamMember.builder().id(1L).workspaceId(workspaceId).accountId(100L).role(MemberRole.ADMIN).build(),
                        TeamMember.builder().id(2L).workspaceId(workspaceId).accountId(200L).role(MemberRole.MEMBER).build()
                ));
        when(accountRepository.findAllById(List.of(100L, 200L)))
                .thenReturn(List.of(Account.builder().id(100L).username("owner").build()));

        List<WorkspaceDtos.TeamMemberResponse> result = workspaceService.findMembers(workspaceId);

        assertEquals(2, result.size());
        assertEquals("owner", result.get(0).username());
        assertEquals("unknown", result.get(1).username());
    }

    @Test
    void addMember_shouldFail_whenAlreadyExists() {
        when(workspaceRepository.findById(1L)).thenReturn(Optional.of(Workspace.builder().id(1L).build()));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(Account.builder().id(2L).build()));
        when(teamMemberRepository.existsByWorkspaceIdAndAccountId(1L, 2L)).thenReturn(true);

        WorkspaceDtos.AddMemberRequest request = new WorkspaceDtos.AddMemberRequest(2L, MemberRole.MEMBER, 1L);

        ApiException ex = assertThrows(ApiException.class, () -> workspaceService.addMember(1L, request));

        assertEquals("Account already member of workspace", ex.getMessage());
    }

    @Test
    void acceptInvitation_shouldJoinWorkspaceAndSetAcceptedState() {
        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(8L)
                .workspaceId(4L)
                .inviteeEmail("user@pmt.local")
                .role(MemberRole.MEMBER)
                .state(InvitationState.PENDING)
                .invitedBy(12L)
                .createdAt(LocalDateTime.now())
                .build();

        when(invitationRepository.findById(8L)).thenReturn(Optional.of(invitation));
        when(accountRepository.findById(30L)).thenReturn(Optional.of(Account.builder().id(30L).email("user@pmt.local").build()));
        when(teamMemberRepository.existsByWorkspaceIdAndAccountId(4L, 30L)).thenReturn(false);
        when(invitationRepository.save(any(ProjectInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findById(12L)).thenReturn(Optional.of(Account.builder().id(12L).username("admin").build()));

        WorkspaceDtos.InvitationResponse result = workspaceService.acceptInvitation(8L, 30L);

        assertEquals(InvitationState.ACCEPTED, result.state());
        verify(teamMemberRepository).save(any(TeamMember.class));
        verify(invitationRepository).save(invitation);
    }

    @Test
    void declineInvitation_shouldFail_whenInvitationBelongsToAnotherUser() {
        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(9L)
                .workspaceId(4L)
                .inviteeEmail("target@pmt.local")
                .role(MemberRole.MEMBER)
                .state(InvitationState.PENDING)
                .invitedBy(12L)
                .createdAt(LocalDateTime.now())
                .build();

        when(invitationRepository.findById(9L)).thenReturn(Optional.of(invitation));
        when(accountRepository.findById(31L)).thenReturn(Optional.of(Account.builder().id(31L).email("other@pmt.local").build()));

        ApiException ex = assertThrows(ApiException.class, () -> workspaceService.declineInvitation(9L, 31L));

        assertEquals("Invitation does not belong to this account", ex.getMessage());
    }

    @Test
    void findById_shouldThrow_whenWorkspaceDoesNotExist() {
        when(workspaceRepository.findById(404L)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> workspaceService.findById(404L));

        assertEquals("Workspace not found", ex.getMessage());
    }

    @Test
    void findMyPendingInvitations_shouldReturnMappedResponses() {
        Long actorId = 70L;
        when(accountRepository.findById(actorId))
                .thenReturn(Optional.of(Account.builder().id(actorId).email("member@pmt.local").build()));

        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(3L)
                .workspaceId(1L)
                .inviteeEmail("member@pmt.local")
                .role(MemberRole.OBSERVER)
                .state(InvitationState.PENDING)
                .invitedBy(5L)
                .createdAt(LocalDateTime.now())
                .build();

        when(invitationRepository.findByInviteeEmailIgnoreCaseAndStateOrderByCreatedAtDesc("member@pmt.local", InvitationState.PENDING))
                .thenReturn(List.of(invitation));
        when(accountRepository.findById(5L)).thenReturn(Optional.of(Account.builder().id(5L).username("admin").build()));

        List<WorkspaceDtos.InvitationResponse> result = workspaceService.findMyPendingInvitations(actorId);

        assertEquals(1, result.size());
        assertEquals("admin", result.get(0).invitedByUsername());
    }

    @Test
    void acceptInvitation_shouldFail_whenAlreadyProcessed() {
        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(8L)
                .workspaceId(4L)
                .inviteeEmail("user@pmt.local")
                .role(MemberRole.MEMBER)
                .state(InvitationState.ACCEPTED)
                .invitedBy(12L)
                .createdAt(LocalDateTime.now())
                .build();

        when(invitationRepository.findById(8L)).thenReturn(Optional.of(invitation));

        ApiException ex = assertThrows(ApiException.class, () -> workspaceService.acceptInvitation(8L, 30L));

        assertEquals("Invitation already processed", ex.getMessage());
    }

    @Test
    void declineInvitation_shouldSetDeclinedState_whenValid() {
        ProjectInvitation invitation = ProjectInvitation.builder()
                .id(10L)
                .workspaceId(4L)
                .inviteeEmail("decline@pmt.local")
                .role(MemberRole.MEMBER)
                .state(InvitationState.PENDING)
                .invitedBy(12L)
                .createdAt(LocalDateTime.now())
                .build();

        when(invitationRepository.findById(10L)).thenReturn(Optional.of(invitation));
        when(accountRepository.findById(32L)).thenReturn(Optional.of(Account.builder().id(32L).email("decline@pmt.local").build()));
        when(invitationRepository.save(any(ProjectInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRepository.findById(12L)).thenReturn(Optional.of(Account.builder().id(12L).username("admin").build()));

        WorkspaceDtos.InvitationResponse result = workspaceService.declineInvitation(10L, 32L);

        assertEquals(InvitationState.DECLINED, result.state());
        assertNotNull(result.respondedAt());
    }
}
