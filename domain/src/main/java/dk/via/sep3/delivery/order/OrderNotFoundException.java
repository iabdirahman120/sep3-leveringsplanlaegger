package dk.via.sep3.delivery.order;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID id) {
        super("Ordren " + id + " findes ikke");
    }
}
