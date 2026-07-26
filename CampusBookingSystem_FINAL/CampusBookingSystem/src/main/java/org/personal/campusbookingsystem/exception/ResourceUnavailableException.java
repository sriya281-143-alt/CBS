package org.personal.campusbookingsystem.exception;

/**
 * Thrown when a booking cannot proceed because the requested resource
 * is not available, e.g. it is marked OCCUPIED/MAINTENANCE, or it is
 * already booked (double-booking) for the requested date/time slot.
 */
public class ResourceUnavailableException extends Exception {

    public ResourceUnavailableException(String message) {
        super(message);
    }
}
