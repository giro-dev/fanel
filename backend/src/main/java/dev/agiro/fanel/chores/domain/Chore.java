package dev.agiro.fanel.chores.domain;

import dev.agiro.fanel.chores.api.RecurrenceFrequency;
import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Null means the chore does not repeat. When set, marking it done rolls it {@link #recurrenceInterval}
     * {@link #recurrenceFreq} forward from {@link #dueDate} instead of leaving it checked off. */
    @Column(name = "recurrence_freq")
    @Enumerated(EnumType.STRING)
    private RecurrenceFrequency recurrenceFreq;
    @Column(name = "recurrence_interval")
    private Integer recurrenceInterval;

    /** Members the assignee rotates through on each completion, in order; empty means no rotation
     * (the assignee stays the same). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chore_rotation_member", joinColumns = @JoinColumn(name = "chore_id"))
    @OrderColumn(name = "list_position")
    @Column(name = "member_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private List<UUID> rotationMemberIds = new ArrayList<>();

    protected Chore() {}

    public Chore(UUID householdId, String title, UUID assigneeId, LocalDate dueDate,
                RecurrenceFrequency recurrenceFreq, Integer recurrenceInterval, List<UUID> rotationMemberIds) {
        this.householdId = householdId;
        this.title = title;
        this.assigneeId = assigneeId;
        this.done = false;
        this.createdAt = Instant.now();
        this.dueDate = dueDate;
        this.recurrenceFreq = recurrenceFreq;
        this.recurrenceInterval = recurrenceFreq == null ? null : recurrenceInterval;
        if (rotationMemberIds != null) this.rotationMemberIds = new ArrayList<>(rotationMemberIds);
    }

    public UUID getHouseholdId() { return householdId; }
    public String getTitle() { return title; }
    public UUID getAssigneeId() { return assigneeId; }
    public boolean isDone() { return done; }
    public Instant getCreatedAt() { return createdAt; }
    public LocalDate getDueDate() { return dueDate; }
    public RecurrenceFrequency getRecurrenceFreq() { return recurrenceFreq; }
    public Integer getRecurrenceInterval() { return recurrenceInterval; }
    public List<UUID> getRotationMemberIds() { return rotationMemberIds; }

    public void setTitle(String title) { this.title = title; }
    public void setAssigneeId(UUID assigneeId) { this.assigneeId = assigneeId; }

    public void setRecurrence(LocalDate dueDate, RecurrenceFrequency recurrenceFreq, Integer recurrenceInterval,
                              List<UUID> rotationMemberIds) {
        this.dueDate = dueDate;
        this.recurrenceFreq = recurrenceFreq;
        this.recurrenceInterval = recurrenceFreq == null ? null : recurrenceInterval;
        this.rotationMemberIds = rotationMemberIds == null ? new ArrayList<>() : new ArrayList<>(rotationMemberIds);
    }

    /** Marks the chore done. If it recurs, it's immediately rolled to its next occurrence instead: the due date
     * moves forward by the recurrence step and the assignee rotates to the next member in
     * {@link #rotationMemberIds} (wrapping around), leaving the chore unchecked again. */
    public void setDone(boolean done) {
        if (done && recurrenceFreq != null) {
            advanceToNextOccurrence();
            return;
        }
        this.done = done;
    }

    private void advanceToNextOccurrence() {
        LocalDate base = dueDate != null ? dueDate : LocalDate.now();
        int interval = recurrenceInterval == null || recurrenceInterval < 1 ? 1 : recurrenceInterval;
        this.dueDate = switch (recurrenceFreq) {
            case DAILY -> base.plusDays(interval);
            case WEEKLY -> base.plusWeeks(interval);
            case MONTHLY -> base.plusMonths(interval);
            case YEARLY -> base.plusYears(interval);
        };
        if (!rotationMemberIds.isEmpty()) {
            int currentIndex = assigneeId == null ? -1 : rotationMemberIds.indexOf(assigneeId);
            this.assigneeId = rotationMemberIds.get((currentIndex + 1) % rotationMemberIds.size());
        }
        this.done = false;
    }
}
