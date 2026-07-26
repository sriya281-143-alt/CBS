package org.personal.campusbookingsystem.exception;

/**
 * Thrown when a user tries to do something their role does not allow,
 * e.g. a Student trying to approve/reject a booking, or a non-Admin
 * trying to add/edit/delete a resource.
 */
public class UnauthorizedAccessException extends Exception {

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
