package org.personal.campusbookingsystem.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.personal.campusbookingsystem.exception.UnauthorizedAccessException;
import org.personal.campusbookingsystem.manager.BookingManager;
import org.personal.campusbookingsystem.manager.ResourceManager;
import org.personal.campusbookingsystem.model.Booking;
import org.personal.campusbookingsystem.model.Resource;
import org.personal.campusbookingsystem.model.User;
import org.personal.campusbookingsystem.util.Session;

import java.net.URL;

/**
 * Controls the Resource Listing Form. Displays every campus resource as
 * a searchable/filterable card grid and lets the current user open the
 * Booking Form for any available resource. Also drives the "My
 * Bookings" table where the user can review and cancel their own
 * bookings.
 *
 * The resource cards and the "My Bookings" table are both driven by
 * {@link FilteredList}s wrapped around the manager's live
 * {@link ObservableList}. Once wired up in {@link #initialize()}, a
 * change made anywhere (this screen, the booking dialog, the Admin
 * Dashboard) propagates back to {@link ResourceManager}/{@link
 * BookingManager}'s ObservableList and is picked up here automatically -
 * no controller code calls "refresh the table" after a mutation anymore.
 */
public class ResourcesController {

    private static final double CARD_MIN_WIDTH = 280;
    private static final double CARD_MAX_WIDTH = 400;
    private static final double CARD_GAP = 25;
    private static final double CONTENT_PADDING = 70; // 35px left + 35px right on the content VBox

    @FXML private VBox resourcesView;
    @FXML private VBox myBookingsView;
    @FXML private VBox profileView;

    @FXML private Button dashboardButton;
    @FXML private Button myBookingsButton;
    @FXML private Button profileButton;

    @FXML private Label topBarAvatarLabel;
    @FXML private Label topBarUserLabel;
    @FXML private Label topBarRoleLabel;

    @FXML private Label profileAvatarLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileRoleLabel;
    @FXML private Label profileUserIdLabel;
    @FXML private Label profileEmailLabel;

    @FXML private Label availableCountLabel;
    @FXML private Label activeBookingsLabel;
    @FXML private Label pendingRequestsLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;

    @FXML private ScrollPane scrollPane;
    @FXML private FlowPane resourceCardsPane;

    @FXML private TableView<Booking> myBookingsTable;
    @FXML private TableColumn<Booking, String> colBookingResource;
    @FXML private TableColumn<Booking, String> colBookingDate;
    @FXML private TableColumn<Booking, String> colBookingTime;
    @FXML private TableColumn<Booking, String> colBookingPurpose;
    @FXML private TableColumn<Booking, String> colBookingStatus;
    @FXML private TableColumn<Booking, Void> colBookingAction;

    // Managers load straight from the CSV files and write straight back to
    // them after every change, so there is no need for a shared "app context".
    private final ResourceManager resourceManager = new ResourceManager();
    private final BookingManager bookingManager = new BookingManager(resourceManager);

    private User currentUser;

    // Live view over resourceManager.getAll(), narrowed by the current search
    // keyword + type/status filters. Any add/remove/update on the manager's
    // ObservableList - from any screen - flows straight through this and
    // triggers redrawCardsAtCurrentWidth() via the listener set up below.
    private FilteredList<Resource> filteredResources;

    @FXML
    public void initialize() {
        currentUser = Session.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String initials = getInitials(currentUser.getName());
        topBarAvatarLabel.setText(initials);
        topBarUserLabel.setText(currentUser.getName());
        topBarRoleLabel.setText(currentUser.getRole());

        profileAvatarLabel.setText(initials);
        profileNameLabel.setText(currentUser.getName());
        profileRoleLabel.setText("Role: " + currentUser.getRole());
        profileUserIdLabel.setText("User ID: " + currentUser.getUserId());
        profileEmailLabel.setText("Email: " + currentUser.getEmail());

        setupFilterCombos();
        setupResourceCardBinding();
        setupMyBookingsTable();
        setupStatBindings();
        setActiveNav(dashboardButton);

        // Whenever the window (and so the scroll pane's viewport) changes width,
        // redraw the cards at a new width so the row always fills edge-to-edge.
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> redrawCardsAtCurrentWidth());
    }

    /** Turns a name like "Aisha Shakya" into initials like "AS", used for the avatar badge. */
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

    /** Highlights the clicked sidebar button and un-highlights the other two. */
    private void setActiveNav(Button active) {
        dashboardButton.getStyleClass().setAll("sidebar-button");
        myBookingsButton.getStyleClass().setAll("sidebar-button");
        profileButton.getStyleClass().setAll("sidebar-button");
        active.getStyleClass().setAll("sidebar-button-selected");
    }

    private void setupFilterCombos() {
        ObservableList<String> types = FXCollections.observableArrayList("All Types");
        ObservableList<String> statuses = FXCollections.observableArrayList(
                "All Status", "AVAILABLE", "LIMITED", "OCCUPIED", "MAINTENANCE");

        for (Resource r : resourceManager.getAll()) {
            if (!types.contains(r.getResourceType())) {
                types.add(r.getResourceType());
            }
        }

        typeFilterCombo.setItems(types);
        statusFilterCombo.setItems(statuses);
        typeFilterCombo.getSelectionModel().selectFirst();
        statusFilterCombo.getSelectionModel().selectFirst();

        // If an Admin adds a resource of a brand-new type while this screen is open,
        // pick it up in the filter dropdown automatically instead of only at next login.
        resourceManager.getAll().addListener((ListChangeListener<Resource>) change -> {
            for (Resource r : resourceManager.getAll()) {
                if (!types.contains(r.getResourceType())) {
                    types.add(r.getResourceType());
                }
            }
        });
    }

    /**
     * Wires the resource card grid to a FilteredList over the manager's live
     * ObservableList. handleSearch()/handleResetFilters() only ever change the
     * FilteredList's predicate from here on - they never call a "reload" method.
     */
    private void setupResourceCardBinding() {
        filteredResources = new FilteredList<>(resourceManager.getAll(), r -> true);
        filteredResources.addListener((ListChangeListener<Resource>) change -> redrawCardsAtCurrentWidth());
        redrawCardsAtCurrentWidth();
    }

    private void setupMyBookingsTable() {
        colBookingResource.setCellValueFactory(data -> {
            Resource r = resourceManager.findById(data.getValue().getResourceId());
            return new SimpleStringProperty(r != null ? r.getName() : data.getValue().getResourceId());
        });
        colBookingDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getBookingDate().toString()));
        colBookingTime.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTimeRangeText()));
        colBookingPurpose.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPurpose()));
        colBookingStatus.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));

        colBookingAction.setCellFactory(col -> new TableCell<>() {
            private final Button cancelBtn = new Button("Cancel");
            {
                cancelBtn.setOnAction(e -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    handleCancelBooking(booking);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                Booking booking = getTableView().getItems().get(getIndex());
                boolean cancellable = booking.getStatus().equalsIgnoreCase("PENDING")
                        || booking.getStatus().equalsIgnoreCase("CONFIRMED");
                cancelBtn.setDisable(!cancellable);
                setGraphic(cancelBtn);
            }
        });

        // Bound once: BookingManager.createBooking()/cancelBooking() mutate the
        // same underlying ObservableList, so this table never needs a manual reload.
        FilteredList<Booking> myBookings = new FilteredList<>(
                bookingManager.getAllBookings(), b -> b.getUserId().equals(currentUser.getUserId()));
        myBookingsTable.setItems(myBookings);
    }

    private void handleCancelBooking(Booking booking) {
        try {
            bookingManager.cancelBooking(booking.getBookingId(), currentUser);
            // No manual table/card refresh needed: cancelBooking() updates the
            // shared ObservableLists in place, which the bound table, the
            // FilteredList-backed card grid, and the stat label bindings all observe.
        } catch (UnauthorizedAccessException e) {
            new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
        }
    }

    /**
     * Binds every dashboard stat label directly to the size of a Filtered/ObservableList,
     * so they stay correct automatically as bookings/resources are created, cancelled,
     * approved, or rejected - from this screen or any other.
     */
    private void setupStatBindings() {
        FilteredList<Resource> available = new FilteredList<>(resourceManager.getAll(), Resource::isAvailable);
        availableCountLabel.textProperty().bind(Bindings.size(available).asString());

        FilteredList<Booking> myBookings = new FilteredList<>(
                bookingManager.getAllBookings(), b -> b.getUserId().equals(currentUser.getUserId()));

        FilteredList<Booking> myActive = new FilteredList<>(myBookings, b -> b.getStatus().equalsIgnoreCase("CONFIRMED"));
        activeBookingsLabel.textProperty().bind(Bindings.size(myActive).asString());

        FilteredList<Booking> myPending = new FilteredList<>(myBookings, b -> b.getStatus().equalsIgnoreCase("PENDING"));
        pendingRequestsLabel.textProperty().bind(Bindings.size(myPending).asString());
    }

    // ---------------------------------------------------------------
    // Resource card grid - sized dynamically so it always fills the
    // full row width, however wide the window currently is.
    // ---------------------------------------------------------------

    private void redrawCardsAtCurrentWidth() {
        double cardWidth = computeCardWidth();
        resourceCardsPane.getChildren().clear();
        for (Resource r : filteredResources) {
            resourceCardsPane.getChildren().add(buildResourceCard(r, cardWidth));
        }
    }

    /**
     * Works out how wide each card should be so that as many columns as
     * possible (at least CARD_MIN_WIDTH each) fit exactly into the
     * current viewport width, with no left-over space on the right.
     */
    private double computeCardWidth() {
        double viewportWidth = (scrollPane.getViewportBounds() != null)
                ? scrollPane.getViewportBounds().getWidth()
                : 1200;

        double usableWidth = viewportWidth - CONTENT_PADDING;
        if (usableWidth < CARD_MIN_WIDTH) {
            return CARD_MIN_WIDTH;
        }

        int columns = (int) ((usableWidth + CARD_GAP) / (CARD_MIN_WIDTH + CARD_GAP));
        if (columns < 1) {
            columns = 1;
        }

        double cardWidth = (usableWidth - (columns - 1) * CARD_GAP) / columns;
        return Math.min(cardWidth, CARD_MAX_WIDTH);
    }

    private VBox buildResourceCard(Resource r, double cardWidth) {
        VBox card = new VBox();
        card.getStyleClass().add("resource-card");
        card.setSpacing(10);
        card.setPrefWidth(cardWidth);
        card.setMinWidth(cardWidth);
        card.setMaxWidth(cardWidth);

        Label badge = new Label(r.getStatus());
        badge.getStyleClass().add(statusBadgeClass(r.getStatus()));

        Label title = new Label(r.getName());
        title.getStyleClass().add("resource-title");

        Label type = new Label("Type: " + r.getResourceType());
        Label capacity = new Label("👥 Up to " + r.getCapacity() + " people");

        Button bookBtn = new Button("Book Now");
        bookBtn.getStyleClass().add("book-button");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setDisable(!r.isAvailable());
        bookBtn.setOnAction(e -> openBookingDialog(r));

        card.getChildren().addAll(badge, title, type, capacity, bookBtn);
        return card;
    }

    private String statusBadgeClass(String status) {
        if ("AVAILABLE".equalsIgnoreCase(status)) return "available-badge";
        if ("LIMITED".equalsIgnoreCase(status)) return "limited-badge";
        if ("OCCUPIED".equalsIgnoreCase(status)) return "occupied-badge";
        return "maintenance-badge";
    }

    private void openBookingDialog(Resource resource) {
        try {
            URL url = getClass().getResource("/org/personal/campusbookingsystem/booking/booking.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();

            BookingController controller = loader.getController();
            controller.setUp(resource, currentUser, resourceManager, bookingManager);
            // No callback needed to manually re-render cards/stats/table any more -
            // BookingManager.createBooking() mutates the same shared ObservableLists
            // this screen is already bound to.

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("New Booking");
            // booking.fxml declares stylesheets="@../css/style.css" on its own root,
            // so the new Scene picks up the styling without adding it here in code.
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Unable to open the booking form.").showAndWait();
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText();
        String type = typeFilterCombo.getValue();
        String status = statusFilterCombo.getValue();
        filteredResources.setPredicate(r -> matches(r, keyword, type, status));
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        typeFilterCombo.getSelectionModel().selectFirst();
        statusFilterCombo.getSelectionModel().selectFirst();
        filteredResources.setPredicate(r -> true);
    }

    private boolean matches(Resource r, String keyword, String type, String status) {
        boolean matchesKeyword = keyword == null || keyword.isBlank()
                || r.getName().toLowerCase().contains(keyword.toLowerCase());
        boolean matchesType = type == null || type.isBlank() || "All Types".equalsIgnoreCase(type)
                || r.getResourceType().equalsIgnoreCase(type);
        boolean matchesStatus = status == null || status.isBlank() || "All Status".equalsIgnoreCase(status)
                || r.getStatus().equalsIgnoreCase(status);
        return matchesKeyword && matchesType && matchesStatus;
    }

    @FXML
    private void showResourcesView() {
        resourcesView.setVisible(true);
        resourcesView.setManaged(true);
        myBookingsView.setVisible(false);
        myBookingsView.setManaged(false);
        profileView.setVisible(false);
        profileView.setManaged(false);
        setActiveNav(dashboardButton);
    }

    @FXML
    private void showMyBookingsView() {
        resourcesView.setVisible(false);
        resourcesView.setManaged(false);
        myBookingsView.setVisible(true);
        myBookingsView.setManaged(true);
        profileView.setVisible(false);
        profileView.setManaged(false);
        setActiveNav(myBookingsButton);
    }

    @FXML
    private void showProfile() {
        resourcesView.setVisible(false);
        resourcesView.setManaged(false);
        myBookingsView.setVisible(false);
        myBookingsView.setManaged(false);
        profileView.setVisible(true);
        profileView.setManaged(true);
        setActiveNav(profileButton);
    }

    @FXML
    private void handleLogout() {
        Session.clear();
        try {
            URL url = getClass().getResource("/org/personal/campusbookingsystem/login/login.fxml");
            Parent root = FXMLLoader.load(url);
            Stage stage = (Stage) dashboardButton.getScene().getWindow();
            // login.fxml declares stylesheets="@../css/style.css" on its own root,
            // so the new Scene picks up the styling without adding it here in code.
            stage.setScene(new Scene(root));
            stage.setTitle("Campus Booking System");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
