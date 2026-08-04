package fr.edc3.pmt.domain.service;

import fr.edc3.pmt.domain.enums.MemberRole;
import fr.edc3.pmt.domain.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final TeamMemberRepository teamMemberRepository;

    public void requireRole(Long workspaceId, Long accountId, MemberRole... allowedRoles) {
        // Une opération est autorisée uniquement si l'utilisateur appartient au workspace.
        var member = teamMemberRepository.findByWorkspaceIdAndAccountId(workspaceId, accountId)
                .orElseThrow(() -> new ApiException("Account is not a member of workspace"));

        // érification simple de whitelist des rôles autorisés pour l'action.
        for (MemberRole role : allowedRoles) {
            if (member.getRole() == role) {
                return;
            }
        }

        throw new ApiException("Permission denied");
    }
}
