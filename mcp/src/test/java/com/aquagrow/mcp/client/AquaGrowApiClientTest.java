package com.aquagrow.mcp.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AquaGrowApiClientTest {

    private static final String BASE = "http://api.test";

    private MockRestServiceServer server;
    private AquaGrowApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AquaGrowApiClient(builder.build());
        Jwt jwt = Jwt.withTokenValue("the-token").header("alg", "none").subject("user-1")
                .claim("tenant_id", "t1").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static BulkFeedLogRequest feedRequest() {
        return new BulkFeedLogRequest("batch-1", "King Fish Feed", List.of(
                new BulkFeedLogRequest.Entry(LocalDate.of(2026, 10, 1), null, 440.0)));
    }

    @Test
    void bulkFeed_postsJsonWithCallersBearerTokenAndParsesResponse() {
        server.expect(requestTo(BASE + "/api/feed-logs/bulk"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer the-token"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.batchId").value("batch-1"))
                .andExpect(jsonPath("$.entries[0].date").value("2026-10-01"))
                .andExpect(jsonPath("$.entries[0].amount").value(440.0))
                .andRespond(withSuccess(
                        "{\"createdIds\":[\"a\"],\"totalKg\":440.0,\"remainingStockKg\":4560.0,\"extra\":1}",
                        MediaType.APPLICATION_JSON));

        BulkFeedLogResponse response = client.bulkFeed(feedRequest());

        assertThat(response.createdIds()).containsExactly("a");
        assertThat(response.totalKg()).isEqualTo(440.0);
        assertThat(response.remainingStockKg()).isEqualTo(4560.0);
        server.verify();
    }

    @Test
    void createSample_postsToSampleLogs() {
        server.expect(requestTo(BASE + "/api/sample-logs"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer the-token"))
                .andExpect(jsonPath("$.sampleCount").value(20))
                .andExpect(jsonPath("$.date").value("2026-10-12"))
                .andRespond(withSuccess("{\"id\":\"s1\",\"batchId\":\"batch-1\",\"averageWeight\":50.0,"
                        + "\"tenantId\":\"t1\"}", MediaType.APPLICATION_JSON));

        SampleLogResult result = client.createSample(new SampleLogPayload(
                "batch-1", null, LocalDate.of(2026, 10, 12), 20, 1000.0, 50.0, 12.0, 95.0));

        assertThat(result.id()).isEqualTo("s1");
        assertThat(result.averageWeight()).isEqualTo(50.0);
    }

    @Test
    void getFeedStock_parsesList() {
        server.expect(requestTo(BASE + "/api/feed-stock"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer the-token"))
                .andRespond(withSuccess("[{\"id\":\"1\",\"feedType\":\"King Fish Feed\",\"currentStock\":5000.0,"
                        + "\"threshold\":100.0,\"unit\":\"kg\",\"tenantId\":\"root\"}]", MediaType.APPLICATION_JSON));

        List<FeedStockView> stock = client.getFeedStock();

        assertThat(stock).hasSize(1);
        assertThat(stock.get(0).feedType()).isEqualTo("King Fish Feed");
        assertThat(stock.get(0).currentStock()).isEqualTo(5000.0);
    }

    @Test
    void businessError400_surfacesApiMessage() {
        server.expect(requestTo(BASE + "/api/feed-logs/bulk"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Insufficient global feed stock for 'X': available 10.00 kg, "
                                + "requested 20.00 kg\",\"type\":\"BUSINESS_ERROR\"}"));

        assertThatThrownBy(() -> client.bulkFeed(feedRequest()))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("Insufficient global feed stock");
    }

    @Test
    void clientErrorWithoutBody_givesGenericMessage() {
        server.expect(requestTo(BASE + "/api/feed-logs/bulk"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThatThrownBy(() -> client.bulkFeed(feedRequest()))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("HTTP 422");
    }

    @Test
    void unauthorized_doesNotLeakTokenOrBody() {
        server.expect(requestTo(BASE + "/api/feed-logs/bulk"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("secret detail the-token"));

        assertThatThrownBy(() -> client.bulkFeed(feedRequest()))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("credentials")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("the-token").doesNotContain("secret"));
    }

    @Test
    void serverError_isGenericAndHidesDetails() {
        server.expect(requestTo(BASE + "/api/feed-logs/bulk"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"NullPointerException at FeedService.java:123\"}"));

        assertThatThrownBy(() -> client.bulkFeed(feedRequest()))
                .isInstanceOf(ToolUserException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("NullPointer").contains("Nothing was saved"));
    }

    @Test
    void connectionFailure_isReportedAsUnavailable() {
        server.expect(requestTo(BASE + "/api/feed-logs/bulk"))
                .andRespond(request -> {
                    throw new java.io.IOException("connection refused to 10.0.0.5");
                });

        assertThatThrownBy(() -> client.bulkFeed(feedRequest()))
                .isInstanceOf(ToolUserException.class)
                .satisfies(e -> assertThat(e.getMessage()).contains("unavailable").doesNotContain("10.0.0.5"));
    }

    @Test
    void noAuthenticatedUser_failsBeforeAnyRequest() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> client.bulkFeed(feedRequest()))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("No authenticated user");
        server.verify();
    }
}
