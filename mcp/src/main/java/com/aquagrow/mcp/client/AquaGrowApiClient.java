package com.aquagrow.mcp.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls aqua-grow-api on behalf of the current caller, forwarding their bearer token so the API
 * applies its own tenant scoping and business rules (stock deduction, stock events). Writes live here; reads
 * are in {@link FarmDataClient}.
 */
@Component
public class AquaGrowApiClient {

    private final ApiHttp http;

    public AquaGrowApiClient(@Qualifier("aquaGrowApiRestClient") RestClient restClient) {
        this.http = new ApiHttp(restClient);
    }

    public BulkFeedLogResponse bulkFeed(BulkFeedLogRequest request) {
        return http.post(request, BulkFeedLogResponse.class, "/api/feed-logs/bulk");
    }

    public SampleLogResult createSample(SampleLogPayload payload) {
        return http.post(payload, SampleLogResult.class, "/api/sample-logs");
    }

}
