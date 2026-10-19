package dev.agiro.fanel;

import com.jayway.jsonpath.JsonPath;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
abstract class ShoppingCatalogIT {
    @Autowired MockMvc mvc;

    private String base;
    private String listId;

    @BeforeEach
    void createHouseholdAndList() throws Exception {
        String household = mvc.perform(post("/api/v1/households").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Casa Bilingüe","locale":"ca","timezone":"Europe/Madrid"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        base = "/api/v1/households/" + JsonPath.read(household, "$.id") + "/shopping";
        String list = mvc.perform(post(base + "/lists").with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Compra\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        listId = JsonPath.read(list, "$.id");
    }

    @Test
    void seedCatalogIsBilingualAndCategorized() throws Exception {
        mvc.perform(get(base + "/categories").with(httpBasic("admin", "admin")).header("Accept-Language", "es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(10)))
                .andExpect(jsonPath("$[*].name", hasItems("Lácteos y huevos", "Limpieza", "Higiene")))
                .andExpect(jsonPath("$[*].nameCa", hasItems("Lactis i ous", "Neteja")));
        mvc.perform(get(base + "/products").with(httpBasic("admin", "admin")).header("Accept-Language", "ca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThan(200)))
                .andExpect(jsonPath("$[?(@.nameEs == 'Detergente para la ropa')].name").value("Detergent per a la roba"))
                .andExpect(jsonPath("$[?(@.nameEs == 'Leche')].category").value("Lactis i ous"));
    }

    @Test
    void sameProductTypedInCatalanAndSpanishIsOneItem() throws Exception {
        String added = addItem("ca", "Llet").andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Llet"))
                .andExpect(jsonPath("$.category").value("Lactis i ous"))
                .andReturn().getResponse().getContentAsString();
        String itemId = JsonPath.read(added, "$.id");

        addItem("es", "leche").andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.name").value("Leche"))
                .andExpect(jsonPath("$.category").value("Lácteos y huevos"));

        mvc.perform(get(base + "/lists/" + listId).with(httpBasic("admin", "admin")).header("Accept-Language", "ca"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Llet"));
    }

    @Test
    void unknownProductIsSharedAndCanBeTranslated() throws Exception {
        String added = addItem("ca", "Fesols del ganxet").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String itemId = JsonPath.read(added, "$.id");
        String productId = JsonPath.read(added, "$.productId");

        // ShoppingProductCreated is still being completed asynchronously; SQLite may report busy meanwhile.
        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mvc.perform(patch(base + "/products/" + productId).with(httpBasic("admin", "admin"))
                                .header("Accept-Language", "es")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nameEs\":\"Alubias del ganxet\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.name").value("Alubias del ganxet"))
                        .andExpect(jsonPath("$.translated").value(true)));

        addItem("es", "Alubias del ganxet").andExpect(jsonPath("$.id").value(itemId));
        addItem("ca", "fesols del ganxet").andExpect(jsonPath("$.id").value(itemId));
    }

    @Test
    void sharedProductsAreReadOnly() throws Exception {
        String added = addItem("ca", "Rentaplats").andReturn().getResponse().getContentAsString();
        mvc.perform(patch(base + "/products/" + JsonPath.read(added, "$.productId"))
                        .with(httpBasic("admin", "admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nameCa\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    private org.springframework.test.web.servlet.ResultActions addItem(String language, String name) throws Exception {
        return mvc.perform(post(base + "/lists/" + listId + "/items").with(httpBasic("admin", "admin"))
                .header("Accept-Language", language)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + name + "\"}"));
    }
}
