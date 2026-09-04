package dev.agiro.fanel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
abstract class ShoppingControllerIT {
    @Autowired MockMvc mvc;

    @Test
    void managesShoppingItems() throws Exception {
        UUID householdId = createHousehold();
        UUID first = addItem(householdId, "Milk");
        addItem(householdId, "Bread");

        mvc.perform(patch("/api/v1/households/{householdId}/shopping/{id}", householdId, first)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true));

        mvc.perform(delete("/api/v1/households/{id}/shopping/done", householdId)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(1));

        mvc.perform(get("/api/v1/households/{id}/shopping", householdId)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        String remainingBody = mvc.perform(get("/api/v1/households/{id}/shopping", householdId)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID remainingId = firstId(remainingBody);

        mvc.perform(delete("/api/v1/households/{householdId}/shopping/{id}", householdId, remainingId)
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsUnknownHousehold() throws Exception {
        mvc.perform(get("/api/v1/households/{id}/shopping", UUID.randomUUID())
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    private UUID createHousehold() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/households")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shopping Casa\",\"locale\":\"ca\",\"timezone\":\"Europe/Madrid\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return firstId(result.getResponse().getContentAsString());
    }

    private UUID addItem(UUID householdId, String text) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/households/{id}/shopping", householdId)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"" + text + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return firstId(result.getResponse().getContentAsString());
    }

    private UUID firstId(String body) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
        if (!matcher.find()) {
            throw new AssertionError("Response did not contain an id: " + body);
        }
        return UUID.fromString(matcher.group(1));
    }
}
