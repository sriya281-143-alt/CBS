package org.personal.campusbookingsystem.util;

import org.personal.campusbookingsystem.model.User;

/**
 * Holds the currently authenticated user for the lifetime of the
 * running application so every controller/screen can access it without
 * needing to pass it explicitly through every FXML scene change.
 */
public final class Session {

    private static User currentUser;

    private Session() {
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
