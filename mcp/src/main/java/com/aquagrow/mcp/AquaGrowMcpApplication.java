package com.aquagrow.mcp;

import com.aquagrow.mcp.mcp.WriteToolFilteringProvider;
import com.aquagrow.mcp.service.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AquaGrowMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(AquaGrowMcpApplication.class, args);
    }

    /**
     * Register all MCP tool service beans so Spring AI discovers
     * the @Tool-annotated methods and exposes them to MCP clients.
     * Legacy create_/update_/delete_ tools are hidden unless
     * aquagrow.mcp.legacy-write-tools.enabled=true.
     */
    @Bean
    public ToolCallbackProvider aquaGrowToolCallbacks(
            PondMcpService pondService,
            BatchMcpService batchService,
            FeedMcpService feedService,
            MonitoringMcpService monitoringService,
            HarvestMcpService harvestService,
            HRFinanceMcpService hrFinanceService,
            CatalogMcpService catalogService,
            FeedingWriteMcpService feedingWriteService,
            SamplingWriteMcpService samplingWriteService,
            FinanceMcpService financeService,
            @Value("${aquagrow.mcp.legacy-write-tools.enabled:false}") boolean legacyWriteToolsEnabled) {

        ToolCallbackProvider all = MethodToolCallbackProvider.builder()
            .toolObjects(pondService, batchService, feedService,
                         monitoringService, harvestService,
                         hrFinanceService, catalogService,
                         feedingWriteService, samplingWriteService, financeService)
            .build();
        return new WriteToolFilteringProvider(all, legacyWriteToolsEnabled);
    }
}
