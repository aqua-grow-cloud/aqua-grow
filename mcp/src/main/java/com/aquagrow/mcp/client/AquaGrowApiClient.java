package com.aquagrow.mcp.client;

import com.aquagrow.mcp.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.List;

/**
 * Calls aqua-grow-api on behalf of the current caller, forwarding their bearer
 * token so the API applies its own tenant scoping and business rules (stock
 * deduction, stock events). All failures surface as {@link ToolUserException}
 * with a message that is safe to show the model.
 */
@Component
@Slf4j
public class AquaGrowApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;

    public AquaGrowApiClient(@Qualifier("aquaGrowApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public BulkFeedLogResponse bulkFeed(BulkFeedLogRequest request) {
        return post("/api/feed-logs/bulk", request, BulkFeedLogResponse.class);
    }

    public SampleLogResult createSample(SampleLogPayload payload) {
        return post("/api/sample-logs", payload, SampleLogResult.class);
    }

    public List<FeedStockView> getFeedStock() {
        return call(() -> restClient.get()
                .uri("/api/feed-stock")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + CurrentUser.bearerToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toToolException(res);
                })
                .body(new ParameterizedTypeReference<List<FeedStockView>>() {
                }));
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        return call(() -> restClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + CurrentUser.bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toToolException(res);
                })
                .body(responseType));
    }

    private <T> T call(java.util.function.Supplier<T> request) {
        try {
            T result = request.get();
            if (result == null) {
                throw new ToolUserException("The Aqua Grow API returned an empty response");
            }
            return result;
        } catch (ToolUserException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("Aqua Grow API call failed: {}", e.getMessage());
            throw new ToolUserException("The Aqua Grow API is unavailable right now. Nothing was saved; try again later.", e);
        }
    }

    private ToolUserException toToolException(ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        if (status.value() == 401 || status.value() == 403) {
            return new ToolUserException("The Aqua Grow API did not accept your credentials for this action.");
        }
        if (status.is5xxServerError()) {
            log.warn("Aqua Grow API returned {}", status.value());
            return new ToolUserException("The Aqua Grow API failed to process the request. Nothing was saved; try again later.");
        }
        String message = null;
        try {
            JsonNode node = MAPPER.readTree(response.getBody());
            if (node != null && node.hasNonNull("message")) {
                message = node.get("message").asText();
            }
        } catch (IOException ignored) {
            // fall through to the generic message
        }
        return new ToolUserException(message != null && !message.isBlank()
                ? message
                : "The request was rejected by the Aqua Grow API (HTTP " + status.value() + ").");
    }
}
