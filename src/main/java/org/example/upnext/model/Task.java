package org.example.upnext.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;



public class Task {
    private Long taskId;
    private Long projectId;
    private Long parentTaskId;   // nullable
    private String title;
    private String description;
    private Long assigneeId;     // nullable
    private String status;       // TODO, IN_PROGRESS, BLOCKED, ON_HOLD, DONE, CANCELLED
    private String priority;     // LOW, MEDIUM, HIGH, CRITICAL
    private LocalDate startDate;
    private LocalDate dueDate;
    private double progressPct;  // 0–100
    private Double estimatedHours;
    private Double actualHours;
    private boolean blocked;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Task() {}
    public Task(Long projectId, String title) {
        this.projectId = projectId;
        this.title = title;
        this.status = "TODO";
        this.priority = "MEDIUM";
    }


}
