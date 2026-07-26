package org.personal.campusbookingsystem.manager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.personal.campusbookingsystem.exception.UnauthorizedAccessException;
import org.personal.campusbookingsystem.model.Resource;
import org.personal.campusbookingsystem.model.User;
import org.personal.campusbookingsystem.util.CsvUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds all resources in memory as an {@link ObservableList} and keeps
 * them in sync with data/resources.csv (via {@link CsvUtil}).
 *
 * Because {@code resources} is Observable, any JavaFX control bound
 * directly to {@link #getAll()} (a TableView, a FilteredList feeding a
 * card grid, a size()-bound stat Label, ...) picks up add/remove
 * changes automatically - no manual "reload the table" call is needed
 * after {@link #addResource} / {@link #removeResource}. In-place edits
 * to an existing Resource's fields (see {@link #updateResource}) are
 * turned into an observable change too, via {@link #touch(Resource)}.
 */
public class ResourceManager {

    private static final String DATA_DIR = "data";
    private static final String FILE_NAME = "resources.csv";
    private static final String SEED_CLASSPATH_LOCATION = "/org/personal/campusbookingsystem/data/resources-seed.csv";

    private final ObservableList<Resource> resources = FXCollections.observableArrayList();

    public ResourceManager() {
        seedFromBundledCsvIfMissing();
        loadResources();
    }

    /**
     * On first run there is no data/resources.csv yet. Rather than hardcoding a
     * List<Resource> of starter data directly in Java, we ship a small starter
     * CSV file on the classpath (src/main/resources/.../data/resources-seed.csv)
     * and copy it into the data/ folder the very first time the app runs. Every
     * run after that loads straight from data/resources.csv, exactly like any
     * other change the user makes.
     */
    private void seedFromBundledCsvIfMissing() {
        Path path = Path.of(DATA_DIR, FILE_NAME);
        if (Files.exists(path)) {
            return;
        }
        List<String[]> seedRows = CsvUtil.readRowsFromClasspath(ResourceManager.class, SEED_CLASSPATH_LOCATION);
        if (!seedRows.isEmpty()) {
            CsvUtil.writeRows(path, seedRows);
        }
    }

    private void loadResources() {
        resources.clear();
        List<String[]> rows = CsvUtil.readRows(Path.of(DATA_DIR, FILE_NAME));
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            if (i == 0 && isHeaderRow(r)) {
                continue; // skip "resourceId,name,resourceType,capacity,status"
            }
            if (r.length < 5) {
                continue;
            }
            int capacity;
            try {
                capacity = Integer.parseInt(r[3].trim());
            } catch (NumberFormatException e) {
                capacity = 0;
            }
            resources.add(new Resource(r[0], r[1], r[2], capacity, r[4]));
        }
    }

    private boolean isHeaderRow(String[] row) {
        return row.length > 0 && "resourceId".equalsIgnoreCase(row[0]);
    }

    public void saveResources() {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"resourceId", "name", "resourceType", "capacity", "status"});
        for (Resource r : resources) {
            rows.add(new String[]{
                    r.getResourceId(), r.getName(), r.getResourceType(),
                    String.valueOf(r.getCapacity()), r.getStatus()
            });
        }
        CsvUtil.writeRows(Path.of(DATA_DIR, FILE_NAME), rows);
    }

    public void addResource(Resource resource, User actingUser) throws UnauthorizedAccessException {
        requireAdmin(actingUser);
        resources.add(resource); // ObservableList add -> bound UI updates automatically
        saveResources();
    }

    public boolean removeResource(String resourceId, User actingUser) throws UnauthorizedAccessException {
        requireAdmin(actingUser);
        boolean removed = resources.removeIf(r -> r.getResourceId().equalsIgnoreCase(resourceId));
        if (removed) {
            saveResources();
        }
        return removed;
    }

    /**
     * Persists in-place edits made to an existing resource (name/capacity/type/status)
     * and fires an observable "update" event for it, so any bound TableView/label
     * refreshes without the controller needing to manually reload anything.
     */
    public void updateResource(Resource resource, User actingUser) throws UnauthorizedAccessException {
        requireAdmin(actingUser);
        touch(resource);
        saveResources();
    }

    /**
     * Re-publishes an already-mutated Resource into the ObservableList at its
     * current index. Plain-field mutation (setStatus/setName/...) on an object
     * already inside an ObservableList does NOT by itself notify listeners -
     * list.set(i, sameObject) is what actually fires the change event that
     * TableView/FilteredList/Bindings are listening for.
     */
    public void touch(Resource resource) {
        int index = resources.indexOf(resource);
        if (index >= 0) {
            resources.set(index, resource);
        }
    }

    /** Only Admins are allowed to add, edit, or delete resources. */
    private void requireAdmin(User actingUser) throws UnauthorizedAccessException {
        if (actingUser == null || !actingUser.isAdmin()) {
            throw new UnauthorizedAccessException("Only Admins can manage resources.");
        }
    }

    public Resource findById(String resourceId) {
        for (Resource r : resources) {
            if (r.getResourceId().equalsIgnoreCase(resourceId)) {
                return r;
            }
        }
        return null;
    }

    /** The live, observable backing list - safe to bind directly to a TableView or wrap in a FilteredList. */
    public ObservableList<Resource> getAll() {
        return resources;
    }

    public List<Resource> getAvailable() {
        List<Resource> available = new ArrayList<>();
        for (Resource r : resources) {
            if (r.isAvailable()) {
                available.add(r);
            }
        }
        return available;
    }

    public List<Resource> search(String keyword, String type, String status) {
        List<Resource> results = new ArrayList<>();
        for (Resource r : resources) {
            boolean matchesKeyword = keyword == null || keyword.isBlank()
                    || r.getName().toLowerCase().contains(keyword.toLowerCase());
            boolean matchesType = type == null || type.isBlank() || "All Types".equalsIgnoreCase(type)
                    || r.getResourceType().equalsIgnoreCase(type);
            boolean matchesStatus = status == null || status.isBlank() || "All Status".equalsIgnoreCase(status)
                    || r.getStatus().equalsIgnoreCase(status);

            if (matchesKeyword && matchesType && matchesStatus) {
                results.add(r);
            }
        }
        return results;
    }
}
