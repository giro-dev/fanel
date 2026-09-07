ALTER TABLE member ADD COLUMN pin VARCHAR(8);

CREATE TABLE meal_plan (
    id CHAR(36) NOT NULL PRIMARY KEY,
    household_id CHAR(36) NOT NULL,
    iso_year INT NOT NULL,
    iso_week INT NOT NULL,
    CONSTRAINT uq_meal_plan UNIQUE (household_id, iso_year, iso_week),
    CONSTRAINT fk_meal_plan_household FOREIGN KEY (household_id) REFERENCES household(id)
);

CREATE TABLE meal_slot (
    id CHAR(36) NOT NULL PRIMARY KEY,
    meal_plan_id CHAR(36) NOT NULL,
    day_of_week INT NOT NULL,
    meal_type VARCHAR(16) NOT NULL,
    text VARCHAR(500),
    CONSTRAINT uq_meal_slot UNIQUE (meal_plan_id, day_of_week, meal_type),
    CONSTRAINT fk_meal_slot_plan FOREIGN KEY (meal_plan_id) REFERENCES meal_plan(id)
);

CREATE INDEX idx_meal_plan_household ON meal_plan(household_id);

CREATE TABLE shopping_list (
    id CHAR(36) NOT NULL PRIMARY KEY,
    household_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_shopping_list_household FOREIGN KEY (household_id) REFERENCES household(id)
);

CREATE TABLE shopping_item (
    id CHAR(36) NOT NULL PRIMARY KEY,
    list_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    done BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_shopping_item_list FOREIGN KEY (list_id) REFERENCES shopping_list(id)
);

CREATE INDEX idx_shopping_list_household ON shopping_list(household_id);
CREATE INDEX idx_shopping_item_list ON shopping_item(list_id);

CREATE TABLE calendar_event (
    id CHAR(36) NOT NULL PRIMARY KEY,
    household_id CHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    event_date DATE NOT NULL,
    event_time TIME,
    added_by CHAR(36),
    CONSTRAINT fk_calendar_event_household FOREIGN KEY (household_id) REFERENCES household(id)
);

CREATE INDEX idx_calendar_event_household ON calendar_event(household_id, event_date);

CREATE TABLE chore (
    id CHAR(36) NOT NULL PRIMARY KEY,
    household_id CHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    assignee_id CHAR(36),
    done BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_chore_household FOREIGN KEY (household_id) REFERENCES household(id)
);

CREATE INDEX idx_chore_household ON chore(household_id);
