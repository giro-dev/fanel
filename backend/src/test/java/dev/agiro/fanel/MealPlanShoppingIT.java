package dev.agiro.fanel;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that planning a meal with a recipe adds its ingredients to the default shopping
 * list, via the {@code MealPlanned} domain event (menu -> shopping), without either module
 * calling the other directly.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class MealPlanShoppingIT {
    @Autowired MockMvc mvc;

    @Test
    void planningARecipeAddsItsIngredientsToTheShoppingList() throws Exception {
        String householdJson = mvc.perform(post("/api/v1/households").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Casa","locale":"ca","timezone":"Europe/Madrid"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String householdId = com.jayway.jsonpath.JsonPath.read(householdJson, "$.id");

        String recipeJson = mvc.perform(post("/api/v1/households/" + householdId + "/recipes")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Truita de patates","servings":4,
                                 "ingredients":[{"name":"Ous","quantity":4,"unit":"unitat"},
                                                {"name":"Patates","quantity":3,"unit":"unitat"}]}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String recipeId = com.jayway.jsonpath.JsonPath.read(recipeJson, "$.id");

        mvc.perform(put("/api/v1/households/" + householdId + "/menu/slots?year=2026&week=10")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":1,\"mealType\":\"DINNER\",\"recipeId\":\"" + recipeId + "\"}"))
                .andExpect(status().isOk());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mvc.perform(get("/api/v1/households/" + householdId + "/shopping/lists/default")
                                .with(httpBasic("admin", "admin")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.items.length()").value(2))
                        .andExpect(jsonPath("$.items[*].name").value(org.hamcrest.Matchers.containsInAnyOrder("Ous", "Patates"))));
    }
}
