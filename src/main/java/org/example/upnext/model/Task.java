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
    private String assigneeName; // transient, filled by DAO for UI
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

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public Long getParentTaskId() { return parentTaskId; }
    public void setParentTaskId(Long parentTaskId) { this.parentTaskId = parentTaskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public double getProgressPct() { return progressPct; }
    public void setProgressPct(double progressPct) { this.progressPct = progressPct; }

    public Double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }

    public Double getActualHours() { return actualHours; }
    public void setActualHours(Double actualHours) { this.actualHours = actualHours; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
