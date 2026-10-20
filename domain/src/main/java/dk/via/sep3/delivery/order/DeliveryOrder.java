package dk.via.sep3.delivery.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class DeliveryOrder {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private String warehouseId;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String productName;

    private OffsetDateTime deliveryWindowStart;
    private OffsetDateTime deliveryWindowEnd;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    // Optimistisk låsning. Hibernate tæller op ved hver skrivning og afviser
    // en UPDATE, hvis rækken er ændret siden den blev læst.
    @Version
    private long version;

    protected DeliveryOrder() {
    }

    public DeliveryOrder(String warehouseId, String customerName, String productName,
                         OffsetDateTime deliveryWindowStart, OffsetDateTime deliveryWindowEnd) {
        this.id = UUID.randomUUID();
        this.status = Status.PENDING;
        this.warehouseId = warehouseId;
        this.customerName = customerName;
        this.productName = productName;
        this.deliveryWindowStart = deliveryWindowStart;
        this.deliveryWindowEnd = deliveryWindowEnd;
        this.createdAt = OffsetDateTime.now();
    }

    /** Eneste måde at ændre status på. Tjekker tabellen før der skrives. */
    public void moveTo(Status to) {
        if (!StatusMachine.canTransition(status, to)) {
            throw new InvalidTransitionException(status, to);
        }
        this.status = to;
    }

    public UUID getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getProductName() {
        return productName;
    }

    public OffsetDateTime getDeliveryWindowStart() {
        return deliveryWindowStart;
    }

    public OffsetDateTime getDeliveryWindowEnd() {
        return deliveryWindowEnd;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }
}
