package dk.via.sep3.delivery.order;

public class StaleVersionException extends RuntimeException {

    public StaleVersionException(long expected, long actual) {
        super("Ordren er ændret af en anden (forventet version " + expected + ", aktuel " + actual + "). Genindlæs");
    }
}
