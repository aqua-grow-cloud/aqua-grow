package com.aquagrow.mcp.mcp;

import com.aquagrow.mcp.AquaGrowMcpApplication;
import com.aquagrow.mcp.service.*;
import org.mockito.Mockito;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Builds the real tool registry from service objects whose dependencies are mocks. */
final class TestToolRegistry {

    private TestToolRegistry() {
    }

    static <T> T service(Class<T> type) throws Exception {
        Constructor<?> ctor = type.getDeclaredConstructors()[0];
        Object[] args = Arrays.stream(ctor.getParameterTypes()).map(Mockito::mock).toArray();
        return type.cast(ctor.newInstance(args));
    }

    static ToolCallbackProvider provider() throws Exception {
        return new AquaGrowMcpApplication().aquaGrowToolCallbacks(
                service(PondMcpService.class), service(BatchMcpService.class), service(FeedMcpService.class),
                service(MonitoringMcpService.class), service(HarvestMcpService.class),
                service(HRFinanceMcpService.class), service(CatalogMcpService.class),
                service(FeedingWriteMcpService.class), service(SamplingWriteMcpService.class),
                service(FinanceMcpService.class));
    }

    static Set<String> registeredTools() throws Exception {
        return Arrays.stream(provider().getToolCallbacks())
                .map(ToolCallback::getToolDefinition)
                .map(d -> d.name())
                .collect(Collectors.toSet());
    }
}
