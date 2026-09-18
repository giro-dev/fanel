package dev.agiro.fanel;

import com.jayway.jsonpath.JsonPath;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies editing/deleting members, and that deleting a member cleans up its chore assignment
 * and calendar assignments (via the {@code MemberDeleted} domain event) instead of leaving
 * dangling references.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class MemberLifecycleIT {
    @Autowired MockMvc mvc;

    @Test
    void editingAndDeletingAMember() throws Exception {
        String householdId = createHousehold("Cicle de vida");
        String memberId = addMember(householdId, "Nen", "CHILD");

        mvc.perform(patch("/api/v1/households/" + householdId + "/members/" + memberId)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nena","color":"#ff00aa"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nena"))
                .andExpect(jsonPath("$.color").value("#ff00aa"));

        mvc.perform(delete("/api/v1/households/" + householdId + "/members/" + memberId)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/households/" + householdId + "/members").with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deletingAMemberClearsChoreAndCalendarReferences() throws Exception {
        String householdId = createHousehold("Neteja de referencies");
        String memberId = addMember(householdId, "Membre", "ADULT");

        String choreResponse = mvc.perform(post("/api/v1/households/" + householdId + "/chores")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tasca\",\"assigneeId\":\"" + memberId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String choreId = JsonPath.read(choreResponse, "$.id");

        String eventResponse = mvc.perform(post("/api/v1/households/" + householdId + "/calendar")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Esdeveniment\",\"date\":\"2026-01-10\",\"assigneeIds\":[\"" + memberId + "\"]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String eventId = JsonPath.read(eventResponse, "$.id");

        mvc.perform(delete("/api/v1/households/" + householdId + "/members/" + memberId)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNoContent());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            String choresJson = mvc.perform(get("/api/v1/households/" + householdId + "/chores").with(httpBasic("admin", "admin")))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            java.util.List<java.util.Map<String, Object>> chores = JsonPath.read(choresJson, "$[?(@.id=='" + choreId + "')]");
            org.assertj.core.api.Assertions.assertThat(chores).hasSize(1);
            org.assertj.core.api.Assertions.assertThat(chores.get(0).get("assigneeId")).isNull();

            String calendarJson = mvc.perform(get("/api/v1/households/" + householdId + "/calendar").with(httpBasic("admin", "admin")))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            java.util.List<java.util.Map<String, Object>> calendarEvents = JsonPath.read(calendarJson, "$[?(@.id=='" + eventId + "')]");
            org.assertj.core.api.Assertions.assertThat(calendarEvents).hasSize(1);
            org.assertj.core.api.Assertions.assertThat((java.util.List<?>) calendarEvents.get(0).get("assigneeIds")).isEmpty();
        });
    }

    private String createHousehold(String name) throws Exception {
        String response = mvc.perform(post("/api/v1/households").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","locale":"ca","timezone":"Europe/Madrid"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String addMember(String householdId, String name, String role) throws Exception {
        String response = mvc.perform(post("/api/v1/households/" + householdId + "/members").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","role":"%s"}
                                """.formatted(name, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
