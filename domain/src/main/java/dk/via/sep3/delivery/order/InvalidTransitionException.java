package dk.via.sep3.delivery.order;

public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(Status from, Status to) {
        super("Overgangen " + from + " -> " + to + " er ikke tilladt");
    }
}
