package dev.agiro.fanel.chores.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chore")
public class Chore extends UuidEntity {
    @Column(name = "household_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID householdId;
    private String title;
    @Column(name = "assignee_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID assigneeId;
    private boolean done;
    private Instant createdAt;

    protected Chore() {}

    public Chore(UUID householdId, String title, UUID assigneeId) {
        this.householdId = householdId;
        this.title = title;
        this.assigneeId = assigneeId;
        this.done = false;
        this.createdAt = Instant.now();
    }

    public UUID getHouseholdId() { return householdId; }
    public String getTitle() { return title; }
    public UUID getAssigneeId() { return assigneeId; }
    public boolean isDone() { return done; }
    public Instant getCreatedAt() { return createdAt; }

    public void setTitle(String title) { this.title = title; }
    public void setAssigneeId(UUID assigneeId) { this.assigneeId = assigneeId; }
    public void setDone(boolean done) { this.done = done; }
}
