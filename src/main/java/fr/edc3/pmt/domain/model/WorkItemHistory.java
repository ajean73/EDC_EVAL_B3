package fr.edc3.pmt.domain.model;

import fr.edc3.pmt.domain.enums.HistoryAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_item_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkItemHistory {
    // Trace d'audit des modifications de tâches (avant/après en JSONB).

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_item_id", nullable = false)
    private Long workItemId;

    @Column(name = "changed_by", nullable = false)
    private Long changedBy;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "history_action")
    private HistoryAction action;

    @Column(name = "previous_values", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String previousValues;

    @Column(name = "new_values", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String newValues;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    void prePersist() {
        // Horodatage de l'événement si non fourni explicitement.
        if (this.changedAt == null) {
            this.changedAt = LocalDateTime.now();
        }
    }
}
