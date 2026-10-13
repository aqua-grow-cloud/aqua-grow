package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.FarmDataClient;
import com.aquagrow.mcp.model.Pond;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read-only pond tools. Data comes from aqua-grow-api as the signed-in caller, so the API applies the
 * farm (tenant) rules; no tenant is ever taken from a tool parameter.
 */
@Service
@RequiredArgsConstructor
public class PondMcpService {

    private final FarmDataClient farmData;

    @Tool(name = "list_ponds",
          description = "List all ponds of the signed-in farm. Returns pond details including area, depth, type, status, and GPS coordinates.")
    public List<Pond> listPonds() {
        return farmData.ponds();
    }

    @Tool(name = "get_pond",
          description = "Get one pond by its id.")
    public Pond getPond(@ToolParam(description = "The pond id (from list_ponds)") String pondId) {
        return farmData.pond(pondId);
    }

    @Tool(name = "list_active_ponds",
          description = "List all ponds with status 'Active'.")
    public List<Pond> listActivePonds() {
        return farmData.activePonds();
    }
}
