ALTER TABLE recipe ADD COLUMN description VARCHAR(4000);
ALTER TABLE recipe ADD COLUMN image_mime_type VARCHAR(100);
ALTER TABLE recipe ADD COLUMN image_data TEXT;

CREATE TABLE recipe_step (
    recipe_id CHAR(36) NOT NULL,
    step_order INTEGER NOT NULL,
    step VARCHAR(4000),
    PRIMARY KEY (recipe_id, step_order),
    CONSTRAINT fk_recipe_step_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id)
);
