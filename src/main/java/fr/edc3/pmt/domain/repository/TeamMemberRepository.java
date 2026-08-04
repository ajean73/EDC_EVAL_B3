package fr.edc3.pmt.domain.repository;

import fr.edc3.pmt.domain.enums.MemberRole;
import fr.edc3.pmt.domain.model.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Accès aux adhésions de comptes dans les workspaces.
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    // Utilisé pour afficher les membres d'un projet.
    List<TeamMember> findByWorkspaceId(Long workspaceId);
    // Utilisé pour lister les workspaces visibles par un compte.
    List<TeamMember> findByAccountId(Long accountId);
    Optional<TeamMember> findByWorkspaceIdAndAccountId(Long workspaceId, Long accountId);
    boolean existsByWorkspaceIdAndAccountId(Long workspaceId, Long accountId);
    boolean existsByWorkspaceIdAndAccountIdAndRole(Long workspaceId, Long accountId, MemberRole role);
}
