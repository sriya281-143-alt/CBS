package org.personal.campusbookingsystem.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.personal.campusbookingsystem.exception.UnauthorizedAccessException;
import org.personal.campusbookingsystem.manager.BookingManager;
import org.personal.campusbookingsystem.manager.ResourceManager;
import org.personal.campusbookingsystem.manager.UserManager;
import org.personal.campusbookingsystem.model.Booking;
import org.personal.campusbookingsystem.model.Resource;
import org.personal.campusbookingsystem.model.User;
import org.personal.campusbookingsystem.util.Session;

import java.net.URL;
import java.util.List;
import java.util.UUID;

/**
 * Controls the Admin Dashboard: system statistics, pending booking
 * approvals, and full resource management (add/edit/delete). Only
 * reachable by users whose role is ADMIN (LoginController routes here).
 *
 * All tables and stat labels are bound once, in {@link #initialize()},
 * directly to the managers' live ObservableLists (or a FilteredList/
 * Bindings.size() wrapper around one). Approve/reject/add/edit/delete
 * actions only ever call the corresponding Manager method - none of
 * them re-set a table's items or manually re-count a label afterwards,
 * because the binding already takes care of that.
 */
public class AdminDashboardController {

    private static final double CARD_GAP = 20;
    private static final double CONTENT_PADDING = 70; // 35px left + 35px right on the content VBox
    private static final int STAT_CARD_COUNT = 4;

    @FXML private Label topBarAdminAvatarLabel;
    @FXML private Label topBarAdminLabel;
    @FXML private Label topBarAdminRoleLabel;

    @FXML private ScrollPane scrollPane;
    @FXML private VBox statCard1;
    @FXML private VBox statCard2;
    @FXML private VBox statCard3;
    @FXML private VBox statCard4;

    @FXML private Label totalResourcesLabel;
    @FXML private Label pendingBookingsLabel;
    @FXML private Label registeredUsersLabel;
    @FXML private Label availableResourcesLabel;

    @FXML private TableView<Booking> pendingBookingsTable;
    @FXML private TableColumn<Booking, String> colStudentName;
    @FXML private TableColumn<Booking, String> colBookingResourceAdmin;
    @FXML private TableColumn<Booking, String> colBookingDateAdmin;
    @FXML private TableColumn<Booking, String> colBookingTimeAdmin;
    @FXML private TableColumn<Booking, Void> colBookingActionsAdmin;

    @FXML private TableView<Resource> resourceTable;
    @FXML private TableColumn<Resource, String> colResId;
    @FXML private TableColumn<Resource, String> colResName;
    @FXML private TableColumn<Resource, String> colResType;
    @FXML private TableColumn<Resource, String> colResCapacity;
    @FXML private TableColumn<Resource, String> colResStatus;
    @FXML private TableColumn<Resource, Void> colResActions;

    // Managers load straight from the CSV files and write straight back to
    // them after every change, so there is no need for a shared "app context".
    private final UserManager userManager = new UserManager();
    private final ResourceManager resourceManager = new ResourceManager();
    private final BookingManager bookingManager = new BookingManager(resourceManager);

    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = Session.getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            // A non-admin should never reach this screen (LoginController routes by role),
            // but this guard keeps the class safe if it is ever opened directly.
            return;
        }

        topBarAdminAvatarLabel.setText(getInitials(currentUser.getName()));
        topBarAdminLabel.setText(currentUser.getName());
        topBarAdminRoleLabel.setText(currentUser.getRole());

        setupPendingBookingsTable();
        setupResourceTable();
        setupStatBindings();

        // Whenever the window (and so the scroll pane's viewport) changes width,
        // resize the 4 stat cards so the row always fills edge-to-edge.
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> resizeStatCards());
        resizeStatCards();
    }

    /** Resizes the 4 fixed stat cards so they always fill the current viewport width exactly. */
    private void resizeStatCards() {
        if (scrollPane.getViewportBounds() == null) {
            return;
        }
        double usableWidth = scrollPane.getViewportBounds().getWidth() - CONTENT_PADDING;
        if (usableWidth <= 0) {
            return;
        }
        double cardWidth = (usableWidth - (STAT_CARD_COUNT - 1) * CARD_GAP) / STAT_CARD_COUNT;

        for (VBox card : List.of(statCard1, statCard2, statCard3, statCard4)) {
            card.setPrefWidth(cardWidth);
            card.setMinWidth(cardWidth);
            card.setMaxWidth(cardWidth);
        }
    }

    private void setupPendingBookingsTable() {
        colStudentName.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCreatorName()));
        colBookingResourceAdmin.setCellValueFactory(d -> {
            Resource r = resourceManager.findById(d.getValue().getResourceId());
            return new SimpleStringProperty(r != null ? r.getName() : d.getValue().getResourceId());
        });
        colBookingDateAdmin.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getBookingDate().toString()));
        colBookingTimeAdmin.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getTimeRangeText()));

        colBookingActionsAdmin.setCellFactory(col -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn = new Button("Reject");
            private final HBox box = new HBox(8, approveBtn, rejectBtn);
            {
                approveBtn.getStyleClass().add("approve-button");
                rejectBtn.getStyleClass().add("reject-button");
                approveBtn.setOnAction(e -> updateStatus(getTableView().getItems().get(getIndex()), "CONFIRMED"));
                rejectBtn.setOnAction(e -> updateStatus(getTableView().getItems().get(getIndex()), "REJECTED"));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Bound once: approving/rejecting a booking removes it from this FilteredList
        // automatically (its status no longer matches "PENDING"), since the FilteredList
        // is watching BookingManager's live ObservableList.
        FilteredList<Booking> pending = new FilteredList<>(
                bookingManager.getAllBookings(), b -> b.getStatus().equalsIgnoreCase("PENDING"));
        pendingBookingsTable.setItems(pending);
    }

    /** Turns a name like "Safir Gautam" into initials like "SG", used for the avatar badge. */
    private String getInitials(String name) {
        if (name == null || name.isBlank()) {
            return "?";
        }
        String[] words = name.trim().split("\\s+");
        String initials = "" + Character.toUpperCase(words[0].charAt(0));
        if (words.length > 1) {
            initials += Character.toUpperCase(words[1].charAt(0));
        }
        return initials;
    }

    private void updateStatus(Booking booking, String status) {
        try {
            bookingManager.updateBookingStatus(booking.getBookingId(), status, currentUser);
            // No manual table/label refresh needed - see the class-level javadoc.
        } catch (UnauthorizedAccessException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    private void setupResourceTable() {
        colResId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResourceId()));
        colResName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colResType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getResourceType()));
        colResCapacity.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getCapacity())));
        colResStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));

        colResActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(8, editBtn, deleteBtn);
            {
                editBtn.setOnAction(e -> openEditResourceDialog(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteResource(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Bound once, directly to the manager's live ObservableList: add/edit/delete
        // all flow straight through to this table with no manual "reload" call.
        resourceTable.setItems(resourceManager.getAll());
    }

    private void deleteResource(Resource resource) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete " + resource.getName() + "? This cannot be undone.");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    resourceManager.removeResource(resource.getResourceId(), currentUser);
                    // No manual table/label refresh needed - see the class-level javadoc.
                } catch (UnauthorizedAccessException e) {
                    new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
                }
            }
        });
    }

    /**
     * Binds every dashboard stat label directly to the size of a Filtered/ObservableList,
     * so they stay correct automatically as resources/bookings/users change - from this
     * screen or any other (e.g. a booking created on the Resources screen).
     */
    private void setupStatBindings() {
        totalResourcesLabel.textProperty().bind(Bindings.size(resourceManager.getAll()).asString());
        registeredUsersLabel.textProperty().bind(Bindings.size(userManager.getAllUsers()).asString());

        FilteredList<Resource> available = new FilteredList<>(resourceManager.getAll(), Resource::isAvailable);
        availableResourcesLabel.textProperty().bind(Bindings.size(available).asString());

        FilteredList<Booking> pending = new FilteredList<>(
                bookingManager.getAllBookings(), b -> b.getStatus().equalsIgnoreCase("PENDING"));
        pendingBookingsLabel.textProperty().bind(Bindings.size(pending).asString());
    }

    // ---------------------------------------------------------------
    // Add / Edit resource dialog (built in code to avoid a dedicated
    // FXML file for a simple form)
    // ---------------------------------------------------------------

    @FXML
    private void handleAddResource() {
        openResourceForm(null);
    }

    private void openEditResourceDialog(Resource resource) {
        openResourceForm(resource);
    }

    private void openResourceForm(Resource existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Add Resource" : "Edit Resource");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(25));

        TextField nameField = new TextField(existing != null ? existing.getName() : "");
        TextField capacityField = new TextField(existing != null ? String.valueOf(existing.getCapacity()) : "");
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Study Room", "Lab Equipment", "Event Space"));
        typeCombo.getSelectionModel().select(existing != null ? existing.getResourceType() : "Study Room");
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList(
                "AVAILABLE", "LIMITED", "OCCUPIED", "MAINTENANCE"));
        statusCombo.getSelectionModel().select(existing != null ? existing.getStatus() : "AVAILABLE");

        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Capacity:"), capacityField);
        grid.addRow(2, new Label("Type:"), typeCombo);
        grid.addRow(3, new Label("Status:"), statusCombo);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-text");
        grid.add(errorLabel, 0, 4, 2, 1);

        Button saveBtn = new Button(existing == null ? "Add Resource" : "Save Changes");
        saveBtn.getStyleClass().add("login-button");
        saveBtn.setOnAction(e -> {
            String name = nameField.getText();
            int capacity;
            try {
                capacity = Integer.parseInt(capacityField.getText().trim());
            } catch (NumberFormatException ex) {
                errorLabel.setText("Capacity must be a whole number.");
                return;
            }
            if (name == null || name.isBlank()) {
                errorLabel.setText("Name is required.");
                return;
            }

            try {
                if (existing != null) {
                    existing.setName(name);
                    existing.setCapacity(capacity);
                    existing.setResourceType(typeCombo.getValue());
                    existing.setStatus(statusCombo.getValue());
                    resourceManager.updateResource(existing, currentUser);
                } else {
                    String id = "R" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                    resourceManager.addResource(
                            new Resource(id, name, typeCombo.getValue(), capacity, statusCombo.getValue()),
                            currentUser);
                }
            } catch (UnauthorizedAccessException ex) {
                errorLabel.setText(ex.getMessage());
                return;
            }

            // No manual table/label refresh needed - see the class-level javadoc.
            dialog.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, cancelBtn, saveBtn);
        grid.add(buttons, 0, 5, 2, 1);

        Scene scene = new Scene(grid);
        // This dialog is built entirely in Java (no backing FXML file), so unlike the
        // FXML screens it has no root element to attach stylesheets="..." to - the
        // programmatic scene.getStylesheets().add(...) below is intentionally kept here.
        scene.getStylesheets().add(
                getClass().getResource("/org/personal/campusbookingsystem/css/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        try {
            URL url = getClass().getResource("/org/personal/campusbookingsystem/login/login.fxml");
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) topBarAdminLabel.getScene().getWindow();
            // login.fxml declares stylesheets="@../css/style.css" on its own root,
            // so the new Scene picks up the styling without adding it here in code.
            stage.setScene(new Scene(root));
            stage.setTitle("Campus Booking System");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
