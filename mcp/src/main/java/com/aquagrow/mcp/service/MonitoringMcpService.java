package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.model.MonitoringLog;
import com.aquagrow.mcp.model.SampleLog;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only water-quality and growth-sample tools. Samples are logged through preview_sample / confirm_sample.
 * Weights in sample logs are in grams.
 */
@Service
@RequiredArgsConstructor
public class MonitoringMcpService {

    private final FarmDataClient farmData;
    private final ResolverService resolver;

    @Tool(name = "list_monitoring_logs",
          description = "List all water-quality readings (dissolved oxygen, pH, temperature, ammonia, salinity, turbidity).")
    public List<MonitoringLog> listMonitoringLogs() {
        return farmData.monitoringLogs();
    }

    @Tool(name = "list_monitoring_logs_by_batch",
          description = "List the water-quality readings of one batch.")
    public List<MonitoringLog> listMonitoringLogsByBatch(
            @ToolParam(description = "Batch id, batch name, or pond name (e.g. 'Pond 3')") String batchId) {
        return farmData.monitoringLogsByBatch(resolver.resolveAnyBatch(batchId).id());
    }

    @Tool(name = "list_sample_logs",
          description = "List all fish growth samples (count, total and average weight in grams, length in cm, survival estimate).")
    public List<SampleLog> listSampleLogs() {
        return farmData.sampleLogs();
    }

    @Tool(name = "list_sample_logs_by_batch",
          description = "List the growth samples of one batch.")
    public List<SampleLog> listSampleLogsByBatch(
            @ToolParam(description = "Batch id, batch name, or pond name (e.g. 'Pond 3')") String batchId) {
        return farmData.sampleLogsByBatch(resolver.resolveAnyBatch(batchId).id());
    }
}
