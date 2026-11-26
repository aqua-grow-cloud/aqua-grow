package com.aquagrow.resourcemonitor.logging;

import com.aquagrow.resourcemonitor.model.Classification;
import com.aquagrow.resourcemonitor.model.PodSnapshot;
import com.aquagrow.resourcemonitor.rules.AnomalyResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceLoggerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void toJsonLineIncludesAllFieldsAndOmitsReasonsWhenOk() throws Exception {
        ResourceLogger logger = new ResourceLogger();
        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                200L, 300_000_000L, 500L, 512_000_000L,
                0, true, "Running", Collections.emptyList());
        AnomalyResult result = new AnomalyResult(Classification.OK, List.of());

        String json = logger.toJsonLine(snapshot, result);
        JsonNode node = mapper.readTree(json);

        assertThat(node.get("pod").asText()).isEqualTo("aqua-grow-api-abc");
        assertThat(node.get("container").asText()).isEqualTo("api");
        assertThat(node.get("level").asText()).isEqualTo("OK");
        assertThat(node.get("ready").asBoolean()).isTrue();
        assertThat(node.get("phase").asText()).isEqualTo("Running");
        assertThat(node.has("reasons")).isFalse();
    }

    @Test
    void toJsonLineIncludesReasonsWhenNotOk() throws Exception {
        ResourceLogger logger = new ResourceLogger();
        PodSnapshot snapshot = new PodSnapshot(
                "aqua-grow-api-abc", "api",
                480L, 490_000_000L, 500L, 512_000_000L,
                0, true, "Running", Collections.emptyList());
        AnomalyResult result = new AnomalyResult(Classification.WARN, List.of("mem_usage_pct_of_limit>85"));

        String json = logger.toJsonLine(snapshot, result);
        JsonNode node = mapper.readTree(json);

        assertThat(node.get("level").asText()).isEqualTo("WARN");
        assertThat(node.get("reasons").get(0).asText()).isEqualTo("mem_usage_pct_of_limit>85");
    }
}
