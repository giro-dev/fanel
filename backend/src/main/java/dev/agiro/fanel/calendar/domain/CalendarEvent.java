package dev.agiro.fanel.calendar.domain;

import dev.agiro.fanel.shared.domain.UuidEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "calendar_event")
public class CalendarEvent extends UuidEntity {
    @Column(name = "household_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID householdId;
    private String title;
    @Column(name = "event_date", nullable = false)
    private LocalDate date;
    @Column(name = "event_time")
    private LocalTime time;
    @Column(name = "added_by")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID addedBy;

    @ElementCollection
    @CollectionTable(name = "calendar_event_assignee", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "member_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private List<UUID> assigneeIds = new ArrayList<>();

    protected CalendarEvent() {}

    public CalendarEvent(UUID householdId, String title, LocalDate date, LocalTime time, UUID addedBy) {
        this.householdId = householdId;
        this.title = title;
        this.date = date;
        this.time = time;
        this.addedBy = addedBy;
    }

    public UUID getHouseholdId() { return householdId; }
    public String getTitle() { return title; }
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public UUID getAddedBy() { return addedBy; }
    public List<UUID> getAssigneeIds() { return assigneeIds; }

    public void setTitle(String title) { this.title = title; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setTime(LocalTime time) { this.time = time; }
    public void setAssigneeIds(List<UUID> assigneeIds) {
        this.assigneeIds = assigneeIds == null ? new ArrayList<>() : new ArrayList<>(assigneeIds);
    }
}
