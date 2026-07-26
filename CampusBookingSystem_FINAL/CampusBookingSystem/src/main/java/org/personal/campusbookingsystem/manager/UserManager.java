package org.personal.campusbookingsystem.manager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.personal.campusbookingsystem.model.User;
import org.personal.campusbookingsystem.util.CsvUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds all users in memory as an {@link ObservableList} and keeps them
 * in sync with data/users.csv (via {@link CsvUtil}). Login is done by
 * email (there is no separate username field once the User class was
 * flattened).
 *
 * Note: unlike {@link ResourceManager}, the initial demo accounts are
 * still seeded from a small hardcoded list below - only Resource
 * start-up data was moved out of Java and into a bundled CSV per this
 * refactor's scope. The same seedFromBundledCsvIfMissing() pattern used
 * in ResourceManager could be applied here too if that's ever needed.
 */
public class UserManager {

    private static final String DATA_DIR = "data";
    private static final String FILE_NAME = "users.csv";

    private final ObservableList<User> users = FXCollections.observableArrayList();

    public UserManager() {
        loadUsers();
        if (users.isEmpty()) {
            seedDemoUsers();
            saveUsers();
        }
    }

    private void loadUsers() {
        users.clear();
        List<String[]> rows = CsvUtil.readRows(Path.of(DATA_DIR, FILE_NAME));
        for (int i = 0; i < rows.size(); i++) {
            String[] p = rows.get(i);
            if (i == 0 && p.length > 0 && "userId".equalsIgnoreCase(p[0])) {
                continue; // header row
            }
            if (p.length < 5) {
                continue;
            }
            users.add(new User(p[0], p[1], p[2], p[3], p[4]));
        }
    }

    public void saveUsers() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"userId", "name", "email", "password", "role"});
        for (User u : users) {
            rows.add(new String[]{u.getUserId(), u.getName(), u.getEmail(), u.getPassword(), u.getRole()});
        }
        CsvUtil.writeRows(Path.of(DATA_DIR, FILE_NAME), rows);
    }

    public User login(String email, String password) {
        User user = findByEmail(email);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public User findByEmail(String email) {
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(email)) {
                return u;
            }
        }
        return null;
    }

    public void addUser(User user) {
        users.add(user);
        saveUsers();
    }

    /** The live, observable backing list - safe to bind directly (e.g. a stat Label's size() binding). */
    public ObservableList<User> getAllUsers() {
        return users;
    }

    private void seedDemoUsers() {
        users.add(new User("U001", "Aisha Shakya", "aisha@student.edu", "student123", "STUDENT"));
        users.add(new User("U002", "James Olen", "james@student.edu", "student123", "STUDENT"));
        users.add(new User("U003", "Rahul Patel", "rahul@staff.edu", "staff123", "STAFF"));
        users.add(new User("U004", "Safir Gautam", "safir@admin.edu", "admin123", "ADMIN"));
    }
}
