package com.aquagrow.mcp.client;

import com.aquagrow.mcp.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * The one place that talks HTTP to aqua-grow-api on behalf of the current caller: forwards their
 * bearer token and turns every failure into a {@link ToolUserException} whose message is safe to show
 * the model (no URLs, tokens or stack traces). Reads and writes differ only in the wording of
 * "nothing was saved".
 *
 * Callers pass URI templates (for example {@code /api/ponds/{id}}) and values separately, so ids that
 * come from the model are always percent-encoded.
 */
@Slf4j
class ApiHttp {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;

    ApiHttp(RestClient restClient) {
        this.restClient = restClient;
    }

    <T> T get(Class<T> type, String uriTemplate, Object... vars) {
        return call(false, () -> restClient.get()
                .uri(uriTemplate, vars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + CurrentUser.bearerToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res, false);
                })
                .body(type));
    }

    <T> T get(ParameterizedTypeReference<T> type, String uriTemplate, Object... vars) {
        return call(false, () -> restClient.get()
                .uri(uriTemplate, vars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + CurrentUser.bearerToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res, false);
                })
                .body(type));
    }

    <T> T post(Object body, Class<T> type, String uriTemplate, Object... vars) {
        return call(true, () -> restClient.post()
                .uri(uriTemplate, vars)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + CurrentUser.bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw toException(res, true);
                })
                .body(type));
    }

    private <T> T call(boolean write, Supplier<T> request) {
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
            throw new ToolUserException("The Aqua Grow API is unavailable right now. "
                    + (write ? "Nothing was saved; try again later." : "Try again later."), e);
        }
    }

    private ToolUserException toException(ClientHttpResponse response, boolean write) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        if (status.value() == 401 || status.value() == 403) {
            return new ToolUserException("The Aqua Grow API did not accept your credentials for this action.");
        }
        if (status.value() == 404) {
            return new ApiNotFoundException("The requested record was not found.");
        }
        if (status.is5xxServerError()) {
            log.warn("Aqua Grow API returned {}", status.value());
            return new ToolUserException("The Aqua Grow API failed to process the request. "
                    + (write ? "Nothing was saved; try again later." : "Try again later."));
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
