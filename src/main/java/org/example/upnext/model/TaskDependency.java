package org.example.upnext.model;

import java.time.OffsetDateTime;


public class TaskDependency {
    private Long depId;
    private Long predecessorTaskId;
    private Long successorTaskId;
    private OffsetDateTime createdAt;

    public TaskDependency() {}
    public TaskDependency(Long predecessorTaskId, Long successorTaskId) {
        this.predecessorTaskId = predecessorTaskId;
        this.successorTaskId = successorTaskId;
    }

    public Long getDepId() {
        return depId;
    }

    public void setDepId(Long depId) {
        this.depId = depId;
    }

    public Long getPredecessorTaskId() {
        return predecessorTaskId;
    }

    public void setPredecessorTaskId(Long predecessorTaskId) {
        this.predecessorTaskId = predecessorTaskId;
    }

    public Long getSuccessorTaskId() {
        return successorTaskId;
    }

    public void setSuccessorTaskId(Long successorTaskId) {
        this.successorTaskId = successorTaskId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
