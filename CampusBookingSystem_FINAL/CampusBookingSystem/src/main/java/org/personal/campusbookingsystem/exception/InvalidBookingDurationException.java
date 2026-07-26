package org.personal.campusbookingsystem.exception;

/**
 * Thrown when the requested booking timeframe is invalid, e.g. the end
 * time is not after the start time, or the requested duration exceeds
 * the maximum allowed booking length.
 */
public class InvalidBookingDurationException extends Exception {

    public InvalidBookingDurationException(String message) {
        super(message);
    }
}
