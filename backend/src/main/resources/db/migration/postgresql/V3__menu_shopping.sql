CREATE TABLE meal_slot (
    id CHAR(36) NOT NULL PRIMARY KEY,
    household_id CHAR(36) NOT NULL,
    date DATE NOT NULL,
    meal VARCHAR(16) NOT NULL,
    text VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_meal_slot_household FOREIGN KEY (household_id) REFERENCES household(id),
    CONSTRAINT uk_meal_slot_household_date_meal UNIQUE (household_id, date, meal)
);

CREATE INDEX idx_meal_slot_household_date ON meal_slot(household_id, date);

CREATE TABLE shopping_item (
    id CHAR(36) NOT NULL PRIMARY KEY,
    household_id CHAR(36) NOT NULL,
    text VARCHAR(200) NOT NULL,
    done BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    done_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_shopping_item_household FOREIGN KEY (household_id) REFERENCES household(id)
);

CREATE INDEX idx_shopping_item_household_done_created
    ON shopping_item(household_id, done, created_at);
