package edu.regis.tcubed;

/**
 * Enumeration representing workflow statuses for tasks within TCubed,
 * mapping workflow states to database bucket identifiers and UI display names.
 *
 * @author Oscar Castillo Saucedo
 */
public enum TaskStatus {

    /**
     * Task is in the To-Do backlog/bucket.
     */
    TODO(1, "To-Do"),

    /**
     * Task is actively being worked on.
     */
    IN_PROGRESS(2, "In Progress"),

    /**
     * Task is completed.
     */
    DONE(3, "Done"),

    /**
     * Task is archived or deleted.
     */
    TRASH(4, "Trash");

    private final int bucketId;
    private final String displayName;

    /**
     * Constructs a {@code TaskStatus} enum constant.
     *
     * @param bucketId    the associated database bucket ID
     * @param displayName the human-readable status name for UI presentation
     */
    TaskStatus(int bucketId, String displayName) {
        this.bucketId = bucketId;
        this.displayName = displayName;
    }

    /**
     * Retrieves the database bucket ID associated with this status.
     *
     * @return the bucket ID
     */
    public int getBucketId() {
        return bucketId;
    }

    /**
     * Retrieves the human-readable display name for this status.
     *
     * @return the display name string
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Resolves a {@code TaskStatus} from a given bucket ID.
     * Defaults to {@link #TODO} if no matching status is found.
     *
     * @param id the database bucket ID to match
     * @return the matching {@code TaskStatus}, or {@link #TODO} by default
     */
    public static TaskStatus fromBucketId(int id) {
        for (TaskStatus status : values()) {
            if (status.bucketId == id) {
                return status;
            }
        }
        return TODO;
    }
}
