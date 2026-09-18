package dev.agiro.fanel.chores.api;

/** How often a recurring chore repeats, starting from its due date. Mirrors {@code calendar.api.RecurrenceFrequency}
 * but kept local to this module so {@code chores} does not depend on {@code calendar}. */
public enum RecurrenceFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}
