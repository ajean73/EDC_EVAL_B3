package fr.edc3.pmt.domain.repository;

import fr.edc3.pmt.domain.enums.WorkItemStatus;
import fr.edc3.pmt.domain.model.WorkItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Accès aux tâches et agrégats de statut.
public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    List<WorkItem> findByWorkspaceId(Long workspaceId);
    Optional<WorkItem> findByIdAndWorkspaceId(Long id, Long workspaceId);
    // Compteur utilisé par le dashboard Kanban.
    long countByWorkspaceIdAndStatus(Long workspaceId, WorkItemStatus status);
}
