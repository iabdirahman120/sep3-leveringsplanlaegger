package dk.via.sep3.delivery.seed;

import dk.via.sep3.delivery.order.DeliveryOrder;
import dk.via.sep3.delivery.order.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Opdigtede demodata. Ingen rigtige kunder, adresser eller ordrer fra
 * Skousen må ligge i repoet, se SEP3 - Data og GDPR.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final OrderRepository orders;

    public DemoDataSeeder(OrderRepository orders) {
        this.orders = orders;
    }

    @Override
    public void run(String... args) {
        if (orders.count() > 0) {
            return;
        }
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        orders.saveAll(List.of(
                order("lager-aarhus", "Kunde 1001", "Vaskemaskine 8 kg", tomorrow, 8),
                order("lager-aarhus", "Kunde 1002", "Køle-fryseskab 186 cm", tomorrow, 10),
                order("lager-aarhus", "Kunde 1003", "Opvaskemaskine 60 cm", tomorrow, 12),
                order("lager-vejle", "Kunde 2001", "Tørretumbler 9 kg", tomorrow, 9),
                order("lager-vejle", "Kunde 2002", "Induktionskomfur", tomorrow, 13)));
    }

    private static DeliveryOrder order(String warehouse, String customer, String product, LocalDate day, int hour) {
        OffsetDateTime start = OffsetDateTime.of(day, LocalTime.of(hour, 0), ZoneOffset.ofHours(1));
        return new DeliveryOrder(warehouse, customer, product, start, start.plusHours(2));
    }
}
