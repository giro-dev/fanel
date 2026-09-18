package dev.agiro.fanel;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the ADMIN/ADULT/CHILD authorization model: admins manage everything, adults are
 * limited to their related children (both for member management and for assigned chores).
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class RolesAndGuardiansIT {
    @Autowired MockMvc mvc;

    @Test
    void adultCanOnlyManageRelatedChildren() throws Exception {
        String householdId = createHousehold("Tutors");
        String adultId = addMember(householdId, "Mare", "ADULT", "mare", "secret123");
        String otherAdultId = addMember(householdId, "Altre adult", "ADULT", "altre", "secret123");
        String childId = addMember(householdId, "Fill", "CHILD", null, null);

        // Not yet a guardian: the adult cannot set the child's credentials.
        mvc.perform(put("/api/v1/households/" + householdId + "/members/" + childId + "/credentials")
                        .with(httpBasic("mare", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"fill","password":"secret123"}
                                """))
                .andExpect(status().isForbidden());

        // Only admins can assign guardians.
        mvc.perform(put("/api/v1/households/" + householdId + "/members/" + childId + "/guardians")
                        .with(httpBasic("mare", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guardianIds\":[\"" + adultId + "\"]}"))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/households/" + householdId + "/members/" + childId + "/guardians")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guardianIds\":[\"" + adultId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardianIds[0]").value(adultId));

        // Now the related adult can manage the child's credentials...
        mvc.perform(put("/api/v1/households/" + householdId + "/members/" + childId + "/credentials")
                        .with(httpBasic("mare", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"fill","password":"secret123"}
                                """))
                .andExpect(status().isOk());

        // ...but the unrelated adult still cannot.
        mvc.perform(put("/api/v1/households/" + householdId + "/members/" + childId + "/credentials")
                        .with(httpBasic("altre", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"fill2","password":"secret123"}
                                """))
                .andExpect(status().isForbidden());

        // Only admins can change roles.
        mvc.perform(put("/api/v1/households/" + householdId + "/members/" + otherAdultId + "/role")
                        .with(httpBasic("mare", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/households/" + householdId + "/members/" + otherAdultId + "/role")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void choresAreScopedToRelatedChildren() throws Exception {
        String householdId = createHousehold("Tasques per rol");
        String adultId = addMember(householdId, "Pare", "ADULT", "pare", "secret123");
        String childId = addMember(householdId, "Filla", "CHILD", null, null);
        String otherChildId = addMember(householdId, "Fill d'altri", "CHILD", null, null);

        mvc.perform(put("/api/v1/households/" + householdId + "/members/" + childId + "/guardians")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guardianIds\":[\"" + adultId + "\"]}"))
                .andExpect(status().isOk());

        String ownChoreId = createChore(householdId, "Parar taula", childId);
        String otherChoreId = createChore(householdId, "Deures d'un altre", otherChildId);
        String sharedChoreId = createChore(householdId, "Tasca compartida", null);

        mvc.perform(get("/api/v1/households/" + householdId + "/chores").with(httpBasic("pare", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.containsInAnyOrder(ownChoreId, sharedChoreId)));

        mvc.perform(patch("/api/v1/households/" + householdId + "/chores/" + otherChoreId)
                        .with(httpBasic("pare", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true}"))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/api/v1/households/" + householdId + "/chores/" + ownChoreId)
                        .with(httpBasic("pare", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true}"))
                .andExpect(status().isOk());
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

    private String addMember(String householdId, String name, String role, String username, String password) throws Exception {
        String response = mvc.perform(post("/api/v1/households/" + householdId + "/members").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","role":"%s"}
                                """.formatted(name, role)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String memberId = JsonPath.read(response, "$.id");
        if (username != null) {
            mvc.perform(put("/api/v1/households/" + householdId + "/members/" + memberId + "/credentials")
                            .with(httpBasic("admin", "admin"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"%s","password":"%s"}
                                    """.formatted(username, password)))
                    .andExpect(status().isOk());
        }
        return memberId;
    }

    private String createChore(String householdId, String title, String assigneeId) throws Exception {
        String body = assigneeId == null
                ? "{\"title\":\"%s\"}".formatted(title)
                : "{\"title\":\"%s\",\"assigneeId\":\"%s\"}".formatted(title, assigneeId);
        String response = mvc.perform(post("/api/v1/households/" + householdId + "/chores")
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }
}
