package org.personal.campusbookingsystem.model;

/**
 * A single flat class representing any user of the system (no
 * inheritance). The role field just holds a plain String such as
 * "STUDENT", "STAFF" or "ADMIN", which keeps role checks simple
 * (basic equalsIgnoreCase comparisons in the controllers/managers).
 */
public class User {

    private String userId;
    private String name;
    private String email;
    private String password;
    private String role; // "STUDENT", "STAFF", "ADMIN"

    public User() {
    }

    public User(String userId, String name, String email, String password, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isStudent() {
        return "STUDENT".equalsIgnoreCase(role);
    }

    public boolean isStaff() {
        return "STAFF".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return name + " (" + role + ")";
    }
}
