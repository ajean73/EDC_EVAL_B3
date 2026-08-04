package fr.edc3.pmt.domain.repository;

import fr.edc3.pmt.domain.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}
