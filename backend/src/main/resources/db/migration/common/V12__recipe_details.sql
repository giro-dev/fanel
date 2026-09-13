ALTER TABLE recipe ADD COLUMN description VARCHAR(4000);

ALTER TABLE recipe ADD COLUMN image_mime_type VARCHAR(100);
ALTER TABLE recipe ADD COLUMN image_data VARCHAR(4194304);

CREATE TABLE recipe_step (
    recipe_id CHAR(36) NOT NULL,
    step_order INT NOT NULL,
    step VARCHAR(4000) NOT NULL,
    CONSTRAINT fk_recipe_step_recipe FOREIGN KEY (recipe_id) REFERENCES recipe(id)
);

CREATE INDEX idx_recipe_step_recipe ON recipe_step(recipe_id);
