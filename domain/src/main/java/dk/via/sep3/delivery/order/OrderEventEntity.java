package dk.via.sep3.delivery.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Audit-log. Append-only: ingen settere, og den skrives i samme transaktion
 * som statusskiftet, så loggen og ordren aldrig kan være uenige.
 */
@Entity
@Table(name = "order_events")
public class OrderEventEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status toStatus;

    private String note;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;

    protected OrderEventEntity() {
    }

    public OrderEventEntity(UUID orderId, Status fromStatus, Status toStatus, String note) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
        this.occurredAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Status getFromStatus() {
        return fromStatus;
    }

    public Status getToStatus() {
        return toStatus;
    }

    public String getNote() {
        return note;
    }

    public OffsetDateTime getOccurredAt() {
        return occurredAt;
    }
}
