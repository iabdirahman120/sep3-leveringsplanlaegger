package dk.via.sep3.delivery.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderEventRepository extends JpaRepository<OrderEventEntity, UUID> {

    List<OrderEventEntity> findAllByOrderIdOrderByOccurredAtAsc(UUID orderId);
}
