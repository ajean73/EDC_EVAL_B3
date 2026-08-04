package fr.edc3.pmt.domain.repository;

import fr.edc3.pmt.domain.model.WorkItemHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkItemHistoryRepository extends JpaRepository<WorkItemHistory, Long> {
    List<WorkItemHistory> findByWorkItemIdOrderByChangedAtDesc(Long workItemId);
}
