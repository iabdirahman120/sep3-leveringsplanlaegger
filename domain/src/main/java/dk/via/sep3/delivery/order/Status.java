package dk.via.sep3.delivery.order;

/**
 * De syv tilstande en ordre kan være i. Gemmes som tekst i databasen,
 * så rækkefølgen i enum'en kan ændres uden at data går i stykker.
 */
public enum Status {
    PENDING,
    ASSIGNED,
    PICKED_UP,
    IN_TRANSIT,
    DELIVERED,
    FAILED,
    CANCELLED
}
