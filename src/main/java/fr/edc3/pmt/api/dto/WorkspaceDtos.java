package fr.edc3.pmt.api.dto;

import fr.edc3.pmt.domain.enums.InvitationState;
import fr.edc3.pmt.domain.enums.MemberRole;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class WorkspaceDtos {
    private WorkspaceDtos() {
    }

    public record CreateWorkspaceRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank String description,
            @NotNull LocalDate startDate,
            @NotNull Long ownerAccountId
    ) {
    }

    public record WorkspaceResponse(
            Long id,
            String name,
            String description,
            LocalDate startDate,
            Long ownerAccountId,
            LocalDateTime createdAt
    ) {
    }

    public record AddMemberRequest(
            @NotNull Long accountId,
            @NotNull MemberRole role,
            @NotNull Long actorAccountId
    ) {
    }

    public record UpdateRoleRequest(
            @NotNull MemberRole role,
            @NotNull Long actorAccountId
    ) {
    }

    public record TeamMemberResponse(
            Long id,
            Long workspaceId,
            Long accountId,
            String username,
            MemberRole role,
            LocalDateTime joinedAt
    ) {
    }

    public record InvitationRequest(
            @NotBlank @Email String inviteeEmail,
            @NotNull MemberRole role,
            @NotNull Long actorAccountId
    ) {
    }

    public record InvitationResponse(
            Long id,
            Long workspaceId,
            String inviteeEmail,
            MemberRole role,
            InvitationState state,
            Long invitedBy,
            String invitedByUsername,
            LocalDateTime createdAt,
            LocalDateTime respondedAt
    ) {
    }

    public record InvitationActionRequest(
            @NotNull Long actorAccountId
    ) {
    }
}
