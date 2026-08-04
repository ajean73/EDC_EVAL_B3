package fr.edc3.pmt.domain.repository;

import fr.edc3.pmt.domain.enums.WorkItemStatus;
import fr.edc3.pmt.domain.model.WorkItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    List<WorkItem> findByWorkspaceId(Long workspaceId);
    Optional<WorkItem> findByIdAndWorkspaceId(Long id, Long workspaceId);
    long countByWorkspaceIdAndStatus(Long workspaceId, WorkItemStatus status);
}
