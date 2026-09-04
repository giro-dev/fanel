package dev.agiro.fanel;

import dev.agiro.fanel.menu.api.MealType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
abstract class MenuControllerIT {
    @Autowired MockMvc mvc;

    @Test
    void managesWeeklyMealSlots() throws Exception {
        UUID householdId = createHousehold();
        LocalDate date = LocalDate.of(2026, 9, 1);

        mvc.perform(put("/api/v1/households/{id}/menu/{date}/{meal}", householdId, date, MealType.DINNER)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Pasta\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Pasta"));

        mvc.perform(get("/api/v1/households/{id}/menu", householdId)
                        .param("week", "2026-W36")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots[0].text").value("Pasta"));

        mvc.perform(put("/api/v1/households/{id}/menu/{date}/{meal}", householdId, date, MealType.DINNER)
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(""));

        mvc.perform(get("/api/v1/households/{id}/menu", householdId)
                        .param("week", "2026-W36")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isEmpty());
    }

    @Test
    void rejectsUnknownHousehold() throws Exception {
        mvc.perform(get("/api/v1/households/{id}/menu", UUID.randomUUID())
                        .param("week", "2026-W36")
                        .with(httpBasic("admin", "admin")))
                .andExpect(status().isNotFound());
    }

    private UUID createHousehold() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/households")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Menu Casa\",\"locale\":\"ca\",\"timezone\":\"Europe/Madrid\"}"))
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
