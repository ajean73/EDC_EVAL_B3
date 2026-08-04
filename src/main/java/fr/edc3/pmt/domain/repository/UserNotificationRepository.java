package fr.edc3.pmt.domain.repository;

import fr.edc3.pmt.domain.model.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Requêtes des notifications utilisateur côté API.
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
