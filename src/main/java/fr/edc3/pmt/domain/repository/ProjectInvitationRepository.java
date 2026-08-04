package fr.edc3.pmt.domain.repository;

import fr.edc3.pmt.domain.enums.InvitationState;
import fr.edc3.pmt.domain.model.ProjectInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Requêtes de suivi des invitations projet.
public interface ProjectInvitationRepository extends JpaRepository<ProjectInvitation, Long> {
    List<ProjectInvitation> findByWorkspaceId(Long workspaceId);
    List<ProjectInvitation> findByInviteeEmailIgnoreCaseAndStateOrderByCreatedAtDesc(String inviteeEmail, InvitationState state);
}
