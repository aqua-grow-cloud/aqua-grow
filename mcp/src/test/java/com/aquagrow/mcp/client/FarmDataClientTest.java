package com.aquagrow.mcp.client;

import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.model.Employee;
import com.aquagrow.mcp.model.Expenditure;
import com.aquagrow.mcp.model.FeedLog;
import com.aquagrow.mcp.model.FeedOrder;
import com.aquagrow.mcp.model.FeedStock;
import com.aquagrow.mcp.model.FishStockEvent;
import com.aquagrow.mcp.model.FishType;
import com.aquagrow.mcp.model.HarvestLog;
import com.aquagrow.mcp.model.MonitoringLog;
import com.aquagrow.mcp.model.Pond;
import com.aquagrow.mcp.model.SalaryPayment;
import com.aquagrow.mcp.model.SampleLog;
import com.aquagrow.mcp.model.Supplier;
import com.aquagrow.mcp.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Runs every read against responses captured from the real aqua-grow-api (src/test/resources/api-contract,
 * refreshed with scripts/mcp-smoke/run.sh --capture), so a change in the API's JSON shows up here.
 */
class FarmDataClientTest {

    private static final String BASE = "http://api.test";

    private MockRestServiceServer server;
    private FarmDataClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FarmDataClient(builder.build());
        Jwt jwt = Jwt.withTokenValue("the-token").header("alg", "none").subject("user-1")
                .claim("tenant_id", "t1").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        TenantContext.setTenantId("farm-1");   // the fixtures were captured from farm-1
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private static String fixture(String name) {
        try {
            return new ClassPathResource("api-contract/" + name + ".json").getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("missing fixture " + name, e);
        }
    }

    private void expectGet(String path, String fixtureName) {
        server.expect(requestTo(BASE + path))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer the-token"))
                .andRespond(withSuccess(fixture(fixtureName), MediaType.APPLICATION_JSON));
    }

    // ---- every endpoint: right path, caller's token, real JSON parses ----

    private record Case(String name, String path, String fixture, Function<FarmDataClient, List<?>> call) {
    }

    private static final String ID = "b-1";

    private static List<Case> listCases() {
        return List.of(
                new Case("ponds", "/api/ponds", "ponds", FarmDataClient::ponds),
                new Case("activePonds", "/api/ponds/active", "ponds-active", FarmDataClient::activePonds),
                new Case("batches", "/api/batches", "batches", FarmDataClient::batches),
                new Case("feedLogs", "/api/feed-logs", "feed-logs", FarmDataClient::feedLogs),
                new Case("feedLogsByBatch", "/api/feed-logs/batch/b-1", "feed-logs-by-batch", c -> c.feedLogsByBatch(ID)),
                new Case("feedStock", "/api/feed-stock", "feed-stock", FarmDataClient::feedStock),
                new Case("lowStock", "/api/feed-stock/low", "feed-stock-low", FarmDataClient::lowStock),
                new Case("feedOrders", "/api/feed-orders", "feed-orders", FarmDataClient::feedOrders),
                new Case("monitoringLogsByBatch", "/api/monitoring-logs/batch/b-1", "monitoring-logs-by-batch", c -> c.monitoringLogsByBatch(ID)),
                new Case("sampleLogsByBatch", "/api/sample-logs/batch/b-1", "sample-logs-by-batch", c -> c.sampleLogsByBatch(ID)),
                new Case("harvestLogs", "/api/harvest-logs", "harvest-logs", FarmDataClient::harvestLogs),
                new Case("harvestLogsByBatch", "/api/harvest-logs/batch/b-1", "harvest-logs-by-batch", c -> c.harvestLogsByBatch(ID)),
                new Case("fishStockEvents", "/api/fish-stock-events", "fish-stock-events", FarmDataClient::fishStockEvents),
                new Case("fishStockEventsByBatch", "/api/fish-stock-events/batch/b-1", "fish-stock-events-by-batch", c -> c.fishStockEventsByBatch(ID)),
                new Case("employees", "/api/employees", "employees", FarmDataClient::employees),
                new Case("salaryPayments", "/api/salary-payments", "salary-payments", FarmDataClient::salaryPayments),
                new Case("salaryPaymentsByEmployee", "/api/salary-payments/employee/e-1", "salary-payments-by-employee", c -> c.salaryPaymentsByEmployee("e-1")),
                new Case("expenditures", "/api/expenditures", "expenditures", FarmDataClient::expenditures),
                new Case("expendituresByBatch", "/api/expenditures/batch/b-1", "expenditures-by-batch", c -> c.expendituresByBatch(ID)),
                new Case("fishTypes", "/api/fish-types", "fish-types", FarmDataClient::fishTypes),
                new Case("suppliers", "/api/suppliers", "suppliers", FarmDataClient::suppliers));
    }

    @Test
    void everyListEndpoint_callsTheRightPathWithTheCallersToken_andParsesRealJson() {
        for (Case c : listCases()) {
            server.reset();
            expectGet(c.path(), c.fixture());

            List<?> result = c.call().apply(client);

            assertThat(result).as(c.name()).isNotEmpty();
            assertThat(result.get(0)).as(c.name()).isNotNull();
            server.verify();
        }
    }

    @Test
    void parsedRecordsCarryTheFieldsTheToolsNeed() {
        expectGet("/api/ponds", "ponds");
        Pond pond = client.ponds().get(0);
        assertThat(pond.getId()).isNotBlank();
        assertThat(pond.getName()).isNotBlank();
        assertThat(pond.getStatus()).isNotBlank();
        assertThat(pond.getArea()).isNotNull();

        server.reset();
        expectGet("/api/batches", "batches");
        Batch batch = client.batches().stream().filter(b -> !b.getFishSeeds().isEmpty()).findFirst().orElseThrow();
        assertThat(batch.getId()).isNotBlank();
        assertThat(batch.getStockingDate()).isNotNull();
        assertThat(batch.getFishSeeds().get(0).getPondId()).isNotBlank();
        assertThat(batch.getFishSeeds().get(0).getQuantity()).isNotNull();
        assertThat(batch.getStatus()).isIn("Active", "Completed");   // derived from the seeds, not read from the JSON

        server.reset();
        expectGet("/api/feed-orders", "feed-orders");
        FeedOrder order = client.feedOrders().get(0);
        assertThat(order.getOrderDate()).isNotNull();
        assertThat(order.getStatus()).isNotNull();
        assertThat(order.getPaymentStatus()).isNotNull();

        server.reset();
        expectGet("/api/feed-stock", "feed-stock");
        FeedStock stock = client.feedStock().get(0);
        assertThat(stock.getFeedType()).isNotBlank();
        assertThat(stock.getCurrentStock()).isNotNull();
        assertThat(stock.getLastUpdated()).isNotNull();

        server.reset();
        expectGet("/api/harvest-logs", "harvest-logs");
        HarvestLog harvest = client.harvestLogs().get(0);
        assertThat(harvest.getDate()).isNotNull();
        assertThat(harvest.getBiomassWeight()).isNotNull();
        assertThat(harvest.getRevenue()).isNotNull();

        server.reset();
        expectGet("/api/expenditures", "expenditures");
        Expenditure expenditure = client.expenditures().get(0);
        assertThat(expenditure.getDate()).isNotNull();
        assertThat(expenditure.getCategory()).isNotBlank();
        assertThat(expenditure.getAmount()).isNotNull();

        server.reset();
        expectGet("/api/salary-payments", "salary-payments");
        SalaryPayment payment = client.salaryPayments().get(0);
        assertThat(payment.getMonth()).matches("\\d{4}-\\d{2}");
        assertThat(payment.getBaseSalary()).isNotNull();

        server.reset();
        expectGet("/api/employees", "employees");
        Employee employee = client.employees().get(0);
        assertThat(employee.getName()).isNotBlank();
        assertThat(employee.getStatus()).isNotBlank();

        server.reset();
        expectGet("/api/fish-types", "fish-types");
        FishType fishType = client.fishTypes().get(0);
        assertThat(fishType.getName()).isNotBlank();
        assertThat(fishType.getCultivationType()).isNotNull();

        server.reset();
        expectGet("/api/suppliers", "suppliers");
        assertThat(client.suppliers().get(0).getName()).isNotBlank();

        server.reset();
        expectGet("/api/fish-stock-events", "fish-stock-events");
        FishStockEvent event = client.fishStockEvents().get(0);
        assertThat(event.getType()).isNotNull();
        assertThat(event.getDate()).isNotNull();

        server.reset();
        expectGet("/api/feed-logs", "feed-logs");
        FeedLog log = client.feedLogs().get(0);
        assertThat(log.getDate()).isNotNull();
        assertThat(log.getAmount()).isNotNull();
    }

    @Test
    void singleRecordEndpoints() {
        expectGet("/api/ponds/p-1", "pond");
        assertThat(client.pond("p-1").getName()).isNotBlank();

        server.reset();
        expectGet("/api/batches/b-1", "batch");
        assertThat(client.batch("b-1").getFishSeeds()).isNotEmpty();
    }

    @Test
    void subTenantStockIsWhateverTheApiReturns_theParentsStock() {
        expectGet("/api/feed-stock", "feed-stock-as-sub-tenant");

        List<FeedStock> stock = client.feedStock();

        assertThat(stock).isNotEmpty();
        assertThat(stock.get(0).getTenantId()).isEqualTo("farm-1");
    }

    // ---- lookups by id must not leak another farm's records ----
    // The API looks batches and ponds up by primary key, which bypasses Hibernate's tenant filter (verified
    // with scripts/mcp-smoke/run.sh --capture). The client therefore checks the owner itself.

    @Test
    void batchByIdBelongingToAnotherFarm_isHiddenAsNotFound() {
        expectGet("/api/batches/b-1", "batch");
        TenantContext.setTenantId("farm-2");

        assertThatThrownBy(() -> client.batch("b-1"))
                .isInstanceOf(ToolUserException.class).hasMessage("Batch not found: b-1");
    }

    @Test
    void pondByIdBelongingToAnotherFarm_isHiddenAsNotFound() {
        expectGet("/api/ponds/p-1", "pond");
        TenantContext.setTenantId("farm-2");

        assertThatThrownBy(() -> client.pond("p-1"))
                .isInstanceOf(ToolUserException.class).hasMessage("Pond not found: p-1");
    }

    @Test
    void byIdLookupWithoutABoundTenant_failsClosed() {
        expectGet("/api/ponds/p-1", "pond");
        TenantContext.clear();

        assertThatThrownBy(() -> client.pond("p-1"))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("No tenant");
    }

    @Test
    void byIdLookupOfARecordWithoutAnOwner_failsClosed() {
        server.expect(requestTo(BASE + "/api/ponds/p-1"))
                .andRespond(withSuccess("{\"id\":\"p-1\",\"name\":\"Pond\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.pond("p-1"))
                .isInstanceOf(ToolUserException.class).hasMessage("Pond not found: p-1");
    }

    // ---- paged lists ----

    @Test
    void pagedList_walksEveryPageUntilTheLastOne() {
        expectGet("/api/monitoring-logs?page=0&size=500", "monitoring-logs-page");
        expectGet("/api/monitoring-logs?page=1&size=500", "monitoring-logs-page-last");

        List<MonitoringLog> logs = client.monitoringLogs();

        assertThat(logs).hasSize(3);
        assertThat(logs).extracting(MonitoringLog::getDate).doesNotContainNull();
        server.verify();
    }

    @Test
    void pagedSampleLogs_stopOnTheFirstPageWhenItIsTheLast() {
        String lastPage = fixture("sample-logs-page").replace("\"last\": false", "\"last\": true");
        server.expect(requestTo(BASE + "/api/sample-logs?page=0&size=500"))
                .andRespond(withSuccess(lastPage, MediaType.APPLICATION_JSON));

        List<SampleLog> logs = client.sampleLogs();

        assertThat(logs).isNotEmpty();
        server.verify();
    }

    @Test
    void pagedList_emptyPageEndsTheWalk() {
        server.expect(requestTo(BASE + "/api/sample-logs?page=0&size=500"))
                .andRespond(withSuccess("{\"content\":[],\"last\":false,\"number\":0}", MediaType.APPLICATION_JSON));

        assertThat(client.sampleLogs()).isEmpty();
    }

    @Test
    void pagedList_aRunawayServerIsStoppedByThePageCap() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer capped = MockRestServiceServer.bindTo(builder).build();
        FarmDataClient small = new FarmDataClient(builder.build(), 500, 3);
        capped.expect(ExpectedCount.manyTimes(), requestTo(org.hamcrest.Matchers.startsWith(BASE + "/api/monitoring-logs")))
                .andRespond(withSuccess(fixture("monitoring-logs-page"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(small::monitoringLogs)
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("Too many records");
    }

    // ---- errors and safety ----

    @Test
    void missingBatch_isAFriendlyNotFound() {
        server.expect(requestTo(BASE + "/api/batches/nope")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.batch("nope"))
                .isInstanceOf(ToolUserException.class)
                .hasMessage("Batch not found: nope");
    }

    @Test
    void missingPond_passesThroughTheApisOwnMessage() {
        server.expect(requestTo(BASE + "/api/ponds/nope")).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"Pond not found with id: nope\",\"type\":\"BUSINESS_ERROR\"}"));

        assertThatThrownBy(() -> client.pond("nope"))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("Pond not found");
    }

    @Test
    void idsFromTheModelAreEncodedNotInterpreted() {
        server.expect(requestTo(BASE + "/api/feed-logs/batch/a%2Fb%20c%3Fx%3D1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.feedLogsByBatch("a/b c?x=1")).isEmpty();
    }

    @Test
    void unauthorizedAndServerErrors_useSafeReadWording() {
        server.expect(requestTo(BASE + "/api/ponds")).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("secret the-token"));
        assertThatThrownBy(client::ponds).isInstanceOf(ToolUserException.class)
                .hasMessageContaining("credentials")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("the-token").doesNotContain("secret"));

        server.reset();
        server.expect(requestTo(BASE + "/api/ponds")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("{\"message\":\"NullPointerException at X.java:1\"}"));
        assertThatThrownBy(client::ponds).isInstanceOf(ToolUserException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("NullPointer").doesNotContain("Nothing was saved")
                        .contains("Try again later"));
    }

    @Test
    void connectionFailure_isReportedAsUnavailableWithoutSayingNothingWasSaved() {
        server.expect(requestTo(BASE + "/api/ponds")).andRespond(request -> {
            throw new IOException("connection refused to 10.0.0.5");
        });

        assertThatThrownBy(client::ponds).isInstanceOf(ToolUserException.class)
                .satisfies(e -> assertThat(e.getMessage()).contains("unavailable").doesNotContain("10.0.0.5")
                        .doesNotContain("Nothing was saved"));
    }

    @Test
    void noAuthenticatedUser_failsBeforeAnyRequest() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(client::ponds).isInstanceOf(ToolUserException.class)
                .hasMessageContaining("No authenticated user");
        server.verify();
    }
}
