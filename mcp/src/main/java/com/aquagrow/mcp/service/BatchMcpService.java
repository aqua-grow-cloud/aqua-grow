package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.model.Batch;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/** Read-only batch tools (see {@link PondMcpService} for how data and tenancy work). */
@Service
@RequiredArgsConstructor
public class BatchMcpService {

    private final FarmDataClient farmData;
    private final ResolverService resolver;

    @Tool(name = "list_batches",
          description = "List all batches of the signed-in farm. A batch is a cultivation cycle with fish seeds stocked in ponds; "
                  + "its status is Active until all its seeds are harvested.")
    public List<Batch> listBatches() {
        return farmData.batches();
    }

    @Tool(name = "get_batch",
          description = "Get one batch with its fish seeds. Accepts the batch id, the batch name, or a pond name.")
    public Batch getBatch(@ToolParam(description = "Batch id, batch name, or pond name (e.g. 'Pond 3')") String batchId) {
        return farmData.batch(resolver.resolveAnyBatch(batchId).id());
    }
}
