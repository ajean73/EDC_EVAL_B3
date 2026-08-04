package fr.edc3.pmt.api;

import fr.edc3.pmt.api.dto.WorkspaceDtos;
import fr.edc3.pmt.domain.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceDtos.WorkspaceResponse create(@Valid @RequestBody WorkspaceDtos.CreateWorkspaceRequest request) {
        return workspaceService.createWorkspace(request);
    }

    @GetMapping
    public List<WorkspaceDtos.WorkspaceResponse> findAll(@RequestParam Long actorAccountId) {
        return workspaceService.findAll(actorAccountId);
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceDtos.WorkspaceResponse findById(@PathVariable Long workspaceId) {
        return workspaceService.findById(workspaceId);
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceDtos.TeamMemberResponse> members(@PathVariable Long workspaceId) {
        return workspaceService.findMembers(workspaceId);
    }

    @PostMapping("/{workspaceId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceDtos.TeamMemberResponse addMember(
            @PathVariable Long workspaceId,
            @Valid @RequestBody WorkspaceDtos.AddMemberRequest request
    ) {
        return workspaceService.addMember(workspaceId, request);
    }

    @PatchMapping("/{workspaceId}/members/{accountId}/role")
    public WorkspaceDtos.TeamMemberResponse updateRole(
            @PathVariable Long workspaceId,
            @PathVariable Long accountId,
            @Valid @RequestBody WorkspaceDtos.UpdateRoleRequest request
    ) {
        return workspaceService.updateRole(workspaceId, accountId, request);
    }

    @PostMapping("/{workspaceId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceDtos.InvitationResponse invite(
            @PathVariable Long workspaceId,
            @Valid @RequestBody WorkspaceDtos.InvitationRequest request
    ) {
        return workspaceService.invite(workspaceId, request);
    }

    @GetMapping("/invitations/mine")
    public List<WorkspaceDtos.InvitationResponse> myInvitations(@RequestParam Long actorAccountId) {
        return workspaceService.findMyPendingInvitations(actorAccountId);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public WorkspaceDtos.InvitationResponse acceptInvitation(
            @PathVariable Long invitationId,
            @Valid @RequestBody WorkspaceDtos.InvitationActionRequest request
    ) {
        return workspaceService.acceptInvitation(invitationId, request.actorAccountId());
    }

    @PostMapping("/invitations/{invitationId}/decline")
    public WorkspaceDtos.InvitationResponse declineInvitation(
            @PathVariable Long invitationId,
            @Valid @RequestBody WorkspaceDtos.InvitationActionRequest request
    ) {
        return workspaceService.declineInvitation(invitationId, request.actorAccountId());
    }
}
