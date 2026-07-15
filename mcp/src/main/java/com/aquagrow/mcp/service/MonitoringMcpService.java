package com.aquagrow.mcp.service;

import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.MonitoringLog;
import com.aquagrow.mcp.model.SampleLog;
import com.aquagrow.mcp.repository.MonitoringLogRepository;
import com.aquagrow.mcp.repository.SampleLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitoringMcpService {

    private final MonitoringLogRepository monitoringLogRepository;
    private final SampleLogRepository sampleLogRepository;
    private final TenantFilterActivator tenantFilterActivator;

    // ---- Monitoring Logs (water quality) ----

    @Tool(name = "list_monitoring_logs",
          description = "List all water quality monitoring logs for a tenant. Includes DO, pH, temperature, ammonia, salinity, turbidity.")
    @Transactional(readOnly = true)
    public List<MonitoringLog> listMonitoringLogs(String tenantId) {
        tenantFilterActivator.activateFor(tenantId);
        return monitoringLogRepository.findByTenantId(tenantId);
    }

    @Tool(name = "list_monitoring_logs_by_batch",
          description = "List water quality monitoring logs for a specific batch.")
    @Transactional(readOnly = true)
    public List<MonitoringLog> listMonitoringLogsByBatch(String tenantId, String batchId) {
        tenantFilterActivator.activateFor(tenantId);
        return monitoringLogRepository.findByTenantIdAndBatchId(tenantId, batchId);
    }

    @Tool(name = "create_monitoring_log",
          description = "Record water quality parameters. Provide batchId, date (YYYY-MM-DD), and any combination of: doLevel (mg/L), ph, temperature (°C), ammonia (mg/L), salinity (ppt), turbidity (NTU).")
    @Transactional
    public MonitoringLog createMonitoringLog(String tenantId, String batchId, String date,
                                              Double doLevel, Double ph, Double temperature,
                                              Double ammonia, Double salinity, Double turbidity) {
        MonitoringLog log = MonitoringLog.builder()
            .tenantId(tenantId)
            .batchId(batchId)
            .date(LocalDate.parse(date))
            .doLevel(doLevel)
            .ph(ph)
            .temperature(temperature)
            .ammonia(ammonia)
            .salinity(salinity)
            .turbidity(turbidity)
            .build();
        return monitoringLogRepository.save(log);
    }

    // ---- Sample Logs (fish growth sampling) ----

    @Tool(name = "list_sample_logs",
          description = "List all fish growth sample logs for a tenant. Samples measure average weight and survival rate.")
    @Transactional(readOnly = true)
    public List<SampleLog> listSampleLogs(String tenantId) {
        tenantFilterActivator.activateFor(tenantId);
        return sampleLogRepository.findByTenantId(tenantId);
    }

    @Tool(name = "list_sample_logs_by_batch",
          description = "List growth sample logs for a specific batch.")
    @Transactional(readOnly = true)
    public List<SampleLog> listSampleLogsByBatch(String tenantId, String batchId) {
        tenantFilterActivator.activateFor(tenantId);
        return sampleLogRepository.findByTenantIdAndBatchId(tenantId, batchId);
    }

    @Tool(name = "create_sample_log",
          description = "Record a fish growth sampling event. Provide batchId, date (YYYY-MM-DD), sampleCount, totalSampleWeight (g), averageLength (cm), and optional survivalEstimate (%).")
    @Transactional
    public SampleLog createSampleLog(String tenantId, String batchId, String fishTypeId,
                                      String date, Integer sampleCount, Double totalSampleWeight,
                                      Double averageLength, Double survivalEstimate) {
        double avgWeight = sampleCount != null && sampleCount > 0
            ? totalSampleWeight / sampleCount : 0;
        SampleLog log = SampleLog.builder()
            .tenantId(tenantId)
            .batchId(batchId)
            .fishTypeId(fishTypeId)
            .date(LocalDate.parse(date))
            .sampleCount(sampleCount)
            .totalSampleWeight(totalSampleWeight)
            .averageWeight(avgWeight)
            .averageLength(averageLength)
            .survivalEstimate(survivalEstimate)
            .build();
        return sampleLogRepository.save(log);
    }
}
