package dk.via.sep3.delivery.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final OrderEventRepository events;

    public OrderService(OrderRepository orders, OrderEventRepository events) {
        this.orders = orders;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<DeliveryOrder> listOrders() {
        return orders.findAllByOrderByDeliveryWindowStartAsc();
    }

    @Transactional(readOnly = true)
    public DeliveryOrder getOrder(UUID id) {
        return orders.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<OrderEventEntity> timeline(UUID id) {
        return events.findAllByOrderIdOrderByOccurredAtAsc(id);
    }

    @Transactional(readOnly = true)
    public Set<Status> allowedTransitions(UUID id) {
        return StatusMachine.allowedFrom(getOrder(id).getStatus());
    }

    /**
     * Statusskift og audit-log i én transaktion. Versionen tjekkes først mod
     * det klienten så. Skriver en anden samtidig, fanger @Version det ved flush.
     */
    @Transactional
    public DeliveryOrder transition(UUID id, Status to, long expectedVersion, String note) {
        DeliveryOrder order = getOrder(id);
        if (order.getVersion() != expectedVersion) {
            throw new StaleVersionException(expectedVersion, order.getVersion());
        }
        Status from = order.getStatus();
        order.moveTo(to);
        events.save(new OrderEventEntity(id, from, to, note));
        return orders.saveAndFlush(order);
    }
}
