package org.personal.campusbookingsystem.model;

/**
 * A single flat class for any bookable campus resource (no
 * inheritance). resourceType just holds a plain String such as
 * "Study Room", "Lab Equipment" or "Event Space".
 */
public class Resource {

    private String resourceId;
    private String name;
    private String resourceType;
    private int capacity;
    private String status; // "AVAILABLE", "LIMITED", "OCCUPIED", "MAINTENANCE"

    public Resource() {
    }

    public Resource(String resourceId, String name, String resourceType, int capacity, String status) {
        this.resourceId = resourceId;
        this.name = name;
        this.resourceType = resourceType;
        this.capacity = capacity;
        this.status = status;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(status) || "LIMITED".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return name + " (" + resourceType + ")";
    }
}
