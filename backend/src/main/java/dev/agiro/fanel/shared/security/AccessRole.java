package dev.agiro.fanel.shared.security;

/**
 * Household-level role of the authenticated caller, decoupled from any single module's own role
 * enum so that {@code shared} never depends on a feature module (see AGENTS.md module rules).
 */
public enum AccessRole {
    ADMIN, ADULT, CHILD
}
