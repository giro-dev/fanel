package dev.agiro.fanel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
abstract class ChoreRecurrenceIT {
    @Autowired MockMvc mvc;

    @Test
    void completingARecurringChoreAdvancesDueDateAndRotatesAssignee() throws Exception {
        String householdJson = mvc.perform(post("/api/v1/households").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Casa Tasques","locale":"ca","timezone":"Europe/Madrid"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String householdId = com.jayway.jsonpath.JsonPath.read(householdJson, "$.id");

        String aliceJson = mvc.perform(post("/api/v1/households/" + householdId + "/members")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","role":"ADULT","color":"#ff0000"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String aliceId = com.jayway.jsonpath.JsonPath.read(aliceJson, "$.id");

        String bobJson = mvc.perform(post("/api/v1/households/" + householdId + "/members")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Bob","role":"ADULT","color":"#00ff00"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String bobId = com.jayway.jsonpath.JsonPath.read(bobJson, "$.id");

        // Create a weekly chore, starting with Alice, rotating Alice -> Bob
        String choreJson = mvc.perform(post("/api/v1/households/" + householdId + "/chores")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Treure les escombraries",
                                  "assigneeId": "%s",
                                  "dueDate": "2026-01-05",
                                  "recurrenceFreq": "WEEKLY",
                                  "recurrenceInterval": 1,
                                  "rotationMemberIds": ["%s", "%s"]
                                }
                                """.formatted(aliceId, aliceId, bobId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dueDate").value("2026-01-05"))
                .andExpect(jsonPath("$.assigneeId").value(aliceId))
                .andReturn().getResponse().getContentAsString();
        String choreId = com.jayway.jsonpath.JsonPath.read(choreJson, "$.id");

        // Marking it done rolls it to the next week and hands it to Bob, instead of leaving it checked
        mvc.perform(patch("/api/v1/households/" + householdId + "/chores/" + choreId)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"done": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(false))
                .andExpect(jsonPath("$.dueDate").value("2026-01-12"))
                .andExpect(jsonPath("$.assigneeId").value(bobId));

        // Completing again rotates back to Alice and advances another week
        mvc.perform(patch("/api/v1/households/" + householdId + "/chores/" + choreId)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"done": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(false))
                .andExpect(jsonPath("$.dueDate").value("2026-01-19"))
                .andExpect(jsonPath("$.assigneeId").value(aliceId));

        // Turning off recurrence makes a later completion stick as done
        mvc.perform(put("/api/v1/households/" + householdId + "/chores/" + choreId + "/recurrence")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dueDate": null, "recurrenceFreq": null, "recurrenceInterval": null, "rotationMemberIds": []}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recurrenceFreq").doesNotExist());

        mvc.perform(patch("/api/v1/households/" + householdId + "/chores/" + choreId)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"done": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.assigneeId").value(aliceId));
    }
}
