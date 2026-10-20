package dk.via.sep3.delivery.order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Overgangstabellen. Den findes kun her, ikke i C# og ikke i klienten.
 * Tabellen følger projektforslaget afsnit 3. Rollerne (admin og chauffør)
 * kommer med login efter proof of concept.
 */
public final class StatusMachine {

    private static final Map<Status, EnumSet<Status>> ALLOWED = new EnumMap<>(Status.class);

    static {
        ALLOWED.put(Status.PENDING, EnumSet.of(Status.ASSIGNED, Status.CANCELLED));
        ALLOWED.put(Status.ASSIGNED, EnumSet.of(Status.PICKED_UP, Status.PENDING, Status.CANCELLED));
        ALLOWED.put(Status.PICKED_UP, EnumSet.of(Status.IN_TRANSIT, Status.FAILED));
        ALLOWED.put(Status.IN_TRANSIT, EnumSet.of(Status.DELIVERED, Status.FAILED));
        // failed er ikke en sluttilstand: ordren kan planlægges igen
        ALLOWED.put(Status.FAILED, EnumSet.of(Status.ASSIGNED, Status.CANCELLED));
        ALLOWED.put(Status.DELIVERED, EnumSet.noneOf(Status.class));
        ALLOWED.put(Status.CANCELLED, EnumSet.noneOf(Status.class));
    }

    private StatusMachine() {
    }

    public static Set<Status> allowedFrom(Status from) {
        return EnumSet.copyOf(ALLOWED.get(from));
    }

    public static boolean canTransition(Status from, Status to) {
        return ALLOWED.get(from).contains(to);
    }
}
