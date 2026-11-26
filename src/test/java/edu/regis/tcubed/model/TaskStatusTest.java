package edu.regis.tcubed.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskStatusTest {

    @Test
    void mapsStandardBucketPositionsCorrectly() {
        assertEquals(TaskStatus.TODO, TaskStatus.fromBucketId(1));
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.fromBucketId(2));
        assertEquals(TaskStatus.DONE, TaskStatus.fromBucketId(3));
        assertEquals(TaskStatus.TRASH, TaskStatus.fromBucketId(4));
    }

    @Test
    void mapsBucketPositionsFiveAndAboveToArchived() {
        assertEquals(TaskStatus.ARCHIVED, TaskStatus.fromBucketId(5));
        assertEquals(TaskStatus.ARCHIVED, TaskStatus.fromBucketId(6));
        assertEquals(TaskStatus.ARCHIVED, TaskStatus.fromBucketId(100));
        assertEquals("Archived", TaskStatus.ARCHIVED.getDisplayName());
    }

    @Test
    void fallsBackToTodoForUnknownLowerBucketIds() {
        assertEquals(TaskStatus.TODO, TaskStatus.fromBucketId(0));
        assertEquals(TaskStatus.TODO, TaskStatus.fromBucketId(-5));
    }

    @Test
    void taskModelRetentionFieldsAndStatusMapping() {
        Task task = new Task(1, "Test Task", "Desc", 1, 5, "user1");
        assertEquals("Archived", task.getStatus());

        task.setUpdatedAt("2026-11-24 12:00");
        task.setDaysRemaining(7);

        assertEquals("2026-11-24 12:00", task.getUpdatedAt());
        assertEquals(7, task.getDaysRemaining());
    }
}
