package fr.edc3.pmt.domain.service;

import fr.edc3.pmt.api.dto.WorkspaceDtos;
import fr.edc3.pmt.domain.enums.MemberRole;
import fr.edc3.pmt.domain.model.ProjectInvitation;
import fr.edc3.pmt.domain.model.TeamMember;
import fr.edc3.pmt.domain.model.Workspace;
import fr.edc3.pmt.domain.repository.AccountRepository;
import fr.edc3.pmt.domain.repository.ProjectInvitationRepository;
import fr.edc3.pmt.domain.repository.TeamMemberRepository;
import fr.edc3.pmt.domain.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ProjectInvitationRepository invitationRepository;
    private final AccountRepository accountRepository;
    private final PermissionService permissionService;

    @Transactional
    public WorkspaceDtos.WorkspaceResponse createWorkspace(WorkspaceDtos.CreateWorkspaceRequest request) {
        accountRepository.findById(request.ownerAccountId())
                .orElseThrow(() -> new NotFoundException("Owner account not found"));

        Workspace workspace = Workspace.builder()
                .name(request.name())
                .description(request.description())
                .startDate(request.startDate())
                .ownerAccountId(request.ownerAccountId())
                .build();

        Workspace saved = workspaceRepository.save(workspace);

        teamMemberRepository.save(TeamMember.builder()
                .workspaceId(saved.getId())
                .accountId(saved.getOwnerAccountId())
                .role(MemberRole.ADMIN)
                .build());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
        public List<WorkspaceDtos.WorkspaceResponse> findAll(Long actorAccountId) {
                List<Long> workspaceIds = teamMemberRepository.findByAccountId(actorAccountId)
                                .stream()
                                .map(TeamMember::getWorkspaceId)
                                .distinct()
                                .toList();

                if (workspaceIds.isEmpty()) {
                        return List.of();
                }

                return workspaceRepository.findAllById(workspaceIds).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDtos.WorkspaceResponse findById(Long workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));
        return toResponse(workspace);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDtos.TeamMemberResponse> findMembers(Long workspaceId) {
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));
        List<TeamMember> members = teamMemberRepository.findByWorkspaceId(workspaceId);
        Map<Long, String> usernamesByAccountId = accountRepository.findAllById(
                        members.stream().map(TeamMember::getAccountId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(a -> a.getId(), a -> a.getUsername()));

        return members
                .stream()
                .map(m -> toTeamMemberResponse(m, usernamesByAccountId))
                .toList();
    }

    @Transactional
    public WorkspaceDtos.TeamMemberResponse addMember(Long workspaceId, WorkspaceDtos.AddMemberRequest request) {
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));
        accountRepository.findById(request.accountId())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        permissionService.requireRole(workspaceId, request.actorAccountId(), MemberRole.ADMIN);

        if (teamMemberRepository.existsByWorkspaceIdAndAccountId(workspaceId, request.accountId())) {
            throw new ApiException("Account already member of workspace");
        }

        TeamMember saved = teamMemberRepository.save(TeamMember.builder()
                .workspaceId(workspaceId)
                .accountId(request.accountId())
                .role(request.role())
                .build());

        String username = accountRepository.findById(saved.getAccountId())
                .map(a -> a.getUsername())
                .orElse("unknown");

        return new WorkspaceDtos.TeamMemberResponse(
                saved.getId(),
                saved.getWorkspaceId(),
                saved.getAccountId(),
                username,
                saved.getRole(),
                saved.getJoinedAt()
        );
    }

    @Transactional
    public WorkspaceDtos.TeamMemberResponse updateRole(Long workspaceId, Long accountId, WorkspaceDtos.UpdateRoleRequest request) {
        permissionService.requireRole(workspaceId, request.actorAccountId(), MemberRole.ADMIN);

        TeamMember member = teamMemberRepository.findByWorkspaceIdAndAccountId(workspaceId, accountId)
                .orElseThrow(() -> new NotFoundException("Member not found"));

        member.setRole(request.role());

        String username = accountRepository.findById(member.getAccountId())
                .map(a -> a.getUsername())
                .orElse("unknown");

        return new WorkspaceDtos.TeamMemberResponse(
                member.getId(),
                member.getWorkspaceId(),
                member.getAccountId(),
                username,
                member.getRole(),
                member.getJoinedAt()
        );
    }

    @Transactional
    public WorkspaceDtos.InvitationResponse invite(Long workspaceId, WorkspaceDtos.InvitationRequest request) {
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));

        permissionService.requireRole(workspaceId, request.actorAccountId(), MemberRole.ADMIN);

        ProjectInvitation invitation = ProjectInvitation.builder()
                .workspaceId(workspaceId)
                .inviteeEmail(request.inviteeEmail())
                .role(request.role())
                .invitedBy(request.actorAccountId())
                .build();

        ProjectInvitation saved = invitationRepository.save(invitation);

        return new WorkspaceDtos.InvitationResponse(
                saved.getId(),
                saved.getWorkspaceId(),
                saved.getInviteeEmail(),
                saved.getRole(),
                saved.getState(),
                saved.getInvitedBy(),
                getUsernameByAccountId(saved.getInvitedBy()),
                saved.getCreatedAt(),
                saved.getRespondedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDtos.InvitationResponse> findMyPendingInvitations(Long actorAccountId) {
        String email = accountRepository.findById(actorAccountId)
                .map(a -> a.getEmail())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        return invitationRepository.findByInviteeEmailIgnoreCaseAndStateOrderByCreatedAtDesc(email, fr.edc3.pmt.domain.enums.InvitationState.PENDING)
                .stream()
                .map(i -> new WorkspaceDtos.InvitationResponse(
                        i.getId(),
                        i.getWorkspaceId(),
                        i.getInviteeEmail(),
                        i.getRole(),
                        i.getState(),
                        i.getInvitedBy(),
                        getUsernameByAccountId(i.getInvitedBy()),
                        i.getCreatedAt(),
                        i.getRespondedAt()
                ))
                .toList();
    }

    @Transactional
    public WorkspaceDtos.InvitationResponse acceptInvitation(Long invitationId, Long actorAccountId) {
        var invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found"));

        if (invitation.getState() != fr.edc3.pmt.domain.enums.InvitationState.PENDING) {
            throw new ApiException("Invitation already processed");
        }

        String email = accountRepository.findById(actorAccountId)
                .map(a -> a.getEmail())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        if (!invitation.getInviteeEmail().equalsIgnoreCase(email)) {
            throw new ApiException("Invitation does not belong to this account");
        }

        if (!teamMemberRepository.existsByWorkspaceIdAndAccountId(invitation.getWorkspaceId(), actorAccountId)) {
            teamMemberRepository.save(TeamMember.builder()
                    .workspaceId(invitation.getWorkspaceId())
                    .accountId(actorAccountId)
                    .role(invitation.getRole())
                    .build());
        }

        invitation.setState(fr.edc3.pmt.domain.enums.InvitationState.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        ProjectInvitation saved = invitationRepository.save(invitation);

        return new WorkspaceDtos.InvitationResponse(
                saved.getId(),
                saved.getWorkspaceId(),
                saved.getInviteeEmail(),
                saved.getRole(),
                saved.getState(),
                saved.getInvitedBy(),
                getUsernameByAccountId(saved.getInvitedBy()),
                saved.getCreatedAt(),
                saved.getRespondedAt()
        );
    }

    @Transactional
    public WorkspaceDtos.InvitationResponse declineInvitation(Long invitationId, Long actorAccountId) {
        var invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Invitation not found"));

        if (invitation.getState() != fr.edc3.pmt.domain.enums.InvitationState.PENDING) {
            throw new ApiException("Invitation already processed");
        }

        String email = accountRepository.findById(actorAccountId)
                .map(a -> a.getEmail())
                .orElseThrow(() -> new NotFoundException("Account not found"));

        if (!invitation.getInviteeEmail().equalsIgnoreCase(email)) {
            throw new ApiException("Invitation does not belong to this account");
        }

        invitation.setState(fr.edc3.pmt.domain.enums.InvitationState.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        ProjectInvitation saved = invitationRepository.save(invitation);

        return new WorkspaceDtos.InvitationResponse(
                saved.getId(),
                saved.getWorkspaceId(),
                saved.getInviteeEmail(),
                saved.getRole(),
                saved.getState(),
                saved.getInvitedBy(),
                getUsernameByAccountId(saved.getInvitedBy()),
                saved.getCreatedAt(),
                saved.getRespondedAt()
        );
    }

    private WorkspaceDtos.WorkspaceResponse toResponse(Workspace w) {
        return new WorkspaceDtos.WorkspaceResponse(
                w.getId(),
                w.getName(),
                w.getDescription(),
                w.getStartDate(),
                w.getOwnerAccountId(),
                w.getCreatedAt()
        );
    }

        private String getUsernameByAccountId(Long accountId) {
                return accountRepository.findById(accountId)
                                .map(a -> a.getUsername())
                                .orElse("unknown");
        }

        private WorkspaceDtos.TeamMemberResponse toTeamMemberResponse(TeamMember member, Map<Long, String> usernamesByAccountId) {
                return new WorkspaceDtos.TeamMemberResponse(
                                member.getId(),
                                member.getWorkspaceId(),
                                member.getAccountId(),
                                usernamesByAccountId.getOrDefault(member.getAccountId(), "unknown"),
                                member.getRole(),
                                member.getJoinedAt()
                );
        }
}
