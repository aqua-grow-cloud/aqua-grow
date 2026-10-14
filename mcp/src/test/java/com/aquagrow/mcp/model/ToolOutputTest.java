package com.aquagrow.mcp.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.execution.DefaultToolCallResultConverter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the model sees. Dates must read as 2026-10-13, not as the number array [2026,10,13] that Jackson
 * writes by default. Uses the same converter Spring AI applies to every tool result, on responses captured
 * from the real API.
 */
class ToolOutputTest {

    private static final Pattern DATE_ARRAY = Pattern.compile("\\[\\s*\\d{4}\\s*,\\s*\\d{1,2}\\s*,\\s*\\d{1,2}");
    private static final Pattern ISO_DATE = Pattern.compile("\"\\d{4}-\\d{2}-\\d{2}\"");
    private static final Pattern ISO_DATE_TIME = Pattern.compile("\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}");

    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
    private final DefaultToolCallResultConverter converter = new DefaultToolCallResultConverter();

    private <T> String toolOutput(String fixture, Class<T> type) throws IOException {
        String json = new ClassPathResource("api-contract/" + fixture + ".json").getContentAsString(StandardCharsets.UTF_8);
        List<T> items = mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, type));
        assertThat(items).as(fixture).isNotEmpty();
        return converter.convert(items, List.class);
    }

    private void assertReadableDates(String fixture, String output) {
        assertThat(DATE_ARRAY.matcher(output).find()).as("%s must not contain date arrays: %s", fixture, output).isFalse();
        assertThat(ISO_DATE.matcher(output).find() || ISO_DATE_TIME.matcher(output).find())
                .as("%s should contain ISO dates: %s", fixture, output).isTrue();
    }

    @Test
    void everyModelWithDatesWritesThemAsIsoText() throws IOException {
        record Case(String fixture, Class<?> type) {
        }
        for (Case c : List.of(
                new Case("batches", Batch.class), new Case("feed-logs", FeedLog.class),
                new Case("feed-stock", FeedStock.class), new Case("feed-orders", FeedOrder.class),
                new Case("harvest-logs", HarvestLog.class), new Case("fish-stock-events", FishStockEvent.class),
                new Case("employees", Employee.class), new Case("salary-payments", SalaryPayment.class),
                new Case("expenditures", Expenditure.class), new Case("monitoring-logs-by-batch", MonitoringLog.class),
                new Case("sample-logs-by-batch", SampleLog.class), new Case("suppliers", Supplier.class))) {
            assertReadableDates(c.fixture(), toolOutput(c.fixture(), c.type()));
        }
    }

    @Test
    void dateTimesKeepTheirTimeOfDay() throws IOException {
        String output = toolOutput("feed-orders", FeedOrder.class);

        assertThat(ISO_DATE_TIME.matcher(output).find()).as(output).isTrue();
    }

    @Test
    void fishSeedsInsideBatchesAreReadableToo() throws IOException {
        String output = toolOutput("batches", Batch.class);

        assertThat(output).contains("\"stockingDate\":\"20").contains("\"fishSeeds\":[{");
        assertThat(DATE_ARRAY.matcher(output).find()).isFalse();
    }
}
