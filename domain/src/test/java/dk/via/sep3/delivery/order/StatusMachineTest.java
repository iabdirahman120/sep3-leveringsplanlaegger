package dk.via.sep3.delivery.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Hele overgangstabellen uden database, jf. Definition of Done. */
class StatusMachineTest {

    private static final Map<Status, Set<Status>> EXPECTED = Map.of(
            Status.PENDING, EnumSet.of(Status.ASSIGNED, Status.CANCELLED),
            Status.ASSIGNED, EnumSet.of(Status.PICKED_UP, Status.PENDING, Status.CANCELLED),
            Status.PICKED_UP, EnumSet.of(Status.IN_TRANSIT, Status.FAILED),
            Status.IN_TRANSIT, EnumSet.of(Status.DELIVERED, Status.FAILED),
            Status.FAILED, EnumSet.of(Status.ASSIGNED, Status.CANCELLED),
            Status.DELIVERED, EnumSet.noneOf(Status.class),
            Status.CANCELLED, EnumSet.noneOf(Status.class));

    @ParameterizedTest
    @EnumSource(Status.class)
    void everyPairMatchesTheTable(Status from) {
        for (Status to : Status.values()) {
            assertEquals(EXPECTED.get(from).contains(to), StatusMachine.canTransition(from, to),
                    from + " -> " + to);
        }
    }

    @Test
    void deliveredAndCancelledAreFinal() {
        assertEquals(Set.of(), StatusMachine.allowedFrom(Status.DELIVERED));
        assertEquals(Set.of(), StatusMachine.allowedFrom(Status.CANCELLED));
    }

    @Test
    void orderRejectsIllegalMove() {
        DeliveryOrder order = new DeliveryOrder("lager-aarhus", "Kunde 1", "Vaskemaskine", null, null);
        assertThrows(InvalidTransitionException.class, () -> order.moveTo(Status.DELIVERED));
        assertEquals(Status.PENDING, order.getStatus());
    }

    @Test
    void failedCanBePlannedAgain() {
        DeliveryOrder order = new DeliveryOrder("lager-aarhus", "Kunde 1", "Vaskemaskine", null, null);
        order.moveTo(Status.ASSIGNED);
        order.moveTo(Status.PICKED_UP);
        order.moveTo(Status.FAILED);
        order.moveTo(Status.ASSIGNED);
        assertEquals(Status.ASSIGNED, order.getStatus());
    }
}
