package com.aquagrow.mcp;

import com.aquagrow.mcp.service.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
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
     * The only tools that change data are the preview/confirm pairs for feeding and samples; they save
     * through aqua-grow-api so its business rules (stock deduction, stock events) always apply.
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
            FinanceMcpService financeService) {

        return MethodToolCallbackProvider.builder()
            .toolObjects(pondService, batchService, feedService,
                         monitoringService, harvestService,
                         hrFinanceService, catalogService,
                         feedingWriteService, samplingWriteService, financeService)
            .build();
    }
}
