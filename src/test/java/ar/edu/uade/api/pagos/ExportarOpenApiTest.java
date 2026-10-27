package ar.edu.uade.api.pagos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// Genera el contrato OpenAPI (el "archivo del Swagger") en docs/, sin tener que levantar el servidor:
//   mvn test -Dtest=ExportarOpenApiTest
@SpringBootTest
@AutoConfigureMockMvc
class ExportarOpenApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void exportarContrato() throws Exception {
        Path docs = Path.of("docs");
        Files.createDirectories(docs);

        String json = mvc.perform(get("/v3/api-docs")).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        String jsonLegible = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(objectMapper.readTree(json));
        Files.writeString(docs.resolve("openapi.json"), jsonLegible + System.lineSeparator(), StandardCharsets.UTF_8);

        String yaml = mvc.perform(get("/v3/api-docs.yaml")).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        Files.writeString(docs.resolve("openapi.yaml"), yaml, StandardCharsets.UTF_8);

        assertTrue(jsonLegible.contains("\"/api/v1/payments\""));
        assertTrue(yaml.contains("/api/v1/payments"));
    }
}
