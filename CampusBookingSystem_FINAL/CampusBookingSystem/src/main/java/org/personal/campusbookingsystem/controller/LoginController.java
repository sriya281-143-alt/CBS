package org.personal.campusbookingsystem.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.personal.campusbookingsystem.manager.UserManager;
import org.personal.campusbookingsystem.model.User;
import org.personal.campusbookingsystem.util.Session;

import java.net.URL;
import java.util.Optional;

/**
 * Controls the Login Form: checks the entered email/password against
 * users.csv (via UserManager) and then does simple role-based routing
 * to either the Resource Listing Form (Student/Staff) or the Admin
 * Dashboard (Admin). Login is by EMAIL since the flattened User class
 * has no separate username field.
 */
public class LoginController {

    @FXML
    private TextField usernameField; // actually holds the email address

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final UserManager userManager = new UserManager();

    @FXML
    private void handleLogin(ActionEvent event) {

        String email = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter both email and password.");
            return;
        }

        User user = userManager.login(email, password);

        if (user == null) {
            showError("Invalid email or password. Please try again.");
            return;
        }

        Session.setCurrentUser(user);

        try {
            String fxml = user.isAdmin()
                    ? "/org/personal/campusbookingsystem/dashboard/admin-dashboard.fxml"
                    : "/org/personal/campusbookingsystem/resources/resources.fxml";

            URL url = getClass().getResource(fxml);
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            // admin-dashboard.fxml / resources.fxml both declare
            // stylesheets="@../css/style.css" on their own root element, so the
            // new Scene picks up the styling without adding it here in code.
            Scene scene = new Scene(root);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(user.isAdmin() ? "Admin Dashboard" : "Campus Resources");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Unable to open the dashboard. Please try again.");
        }
    }

    /**
     * Simple "Forgot Password" flow: ask for the email, look it up in
     * users.csv, and just show the password back to the user. There is
     * no email/SMS step since this is a file-based desktop app with no
     * network access.
     */
    @FXML
    private void handleForgotPassword(ActionEvent event) {

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Forgot Password");
        dialog.setHeaderText("Recover your password");
        dialog.setContentText("Enter your email:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            return;
        }

        String email = result.get().trim();
        User user = userManager.findByEmail(email);

        if (user == null) {
            new Alert(Alert.AlertType.ERROR, "No account found with email \"" + email + "\".").showAndWait();
            return;
        }

        new Alert(Alert.AlertType.INFORMATION,
                "Your password is: " + user.getPassword()).showAndWait();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
