CREATE TABLE recipe (
    id CHAR(36) NOT NULL PRIMARY KEY,
    household_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    servings INT NOT NULL DEFAULT 1,
    notes VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_recipe_household FOREIGN KEY (household_id) REFERENCES household(id)
);

CREATE INDEX idx_recipe_household ON recipe(household_id);

CREATE TABLE recipe_tag (
    recipe_id CHAR(36) NOT NULL,
    tag VARCHAR(100) NOT NULL,
    CONSTRAINT fk_recipe_tag_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id)
);

CREATE TABLE recipe_ingredient (
    id CHAR(36) NOT NULL PRIMARY KEY,
    recipe_id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    quantity DOUBLE PRECISION,
    unit VARCHAR(50),
    category VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_recipe_ingredient_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id)
);

CREATE INDEX idx_recipe_ingredient_recipe ON recipe_ingredient(recipe_id);

ALTER TABLE meal_slot ADD COLUMN recipe_id CHAR(36);
