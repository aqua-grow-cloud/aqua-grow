package com.aquagrow.mcp.client;

import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.model.Employee;
import com.aquagrow.mcp.model.Expenditure;
import com.aquagrow.mcp.model.FeedLog;
import com.aquagrow.mcp.model.FeedOrder;
import com.aquagrow.mcp.model.FeedStock;
import com.aquagrow.mcp.model.FishStockEvent;
import com.aquagrow.mcp.model.FishType;
import com.aquagrow.mcp.model.HarvestLog;
import com.aquagrow.mcp.model.MonitoringLog;
import com.aquagrow.mcp.model.Pond;
import com.aquagrow.mcp.model.SalaryPayment;
import com.aquagrow.mcp.model.SampleLog;
import com.aquagrow.mcp.model.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads farm data from aqua-grow-api as the current caller. The API applies the tenant (and, for
 * sub-tenants, the parent farm's feed stock) itself, so nothing here filters by tenant.
 *
 * One method per endpoint; lists that the API pages (monitoring logs, sample logs) are read page by
 * page until the last one.
 */
@Component
public class FarmDataClient {

    static final int DEFAULT_PAGE_SIZE = 500;
    static final int DEFAULT_MAX_PAGES = 100;

    private final ApiHttp http;
    private final int pageSize;
    private final int maxPages;

    @org.springframework.beans.factory.annotation.Autowired
    public FarmDataClient(@Qualifier("aquaGrowApiRestClient") RestClient restClient) {
        this(restClient, DEFAULT_PAGE_SIZE, DEFAULT_MAX_PAGES);
    }

    FarmDataClient(RestClient restClient, int pageSize, int maxPages) {
        this.http = new ApiHttp(restClient);
        this.pageSize = pageSize;
        this.maxPages = maxPages;
    }

    // ---- ponds and batches ----

    public List<Pond> ponds() {
        return list(Pond.class, "/api/ponds");
    }

    public List<Pond> activePonds() {
        return list(Pond.class, "/api/ponds/active");
    }

    public Pond pond(String id) {
        return one(Pond.class, "Pond", id, "/api/ponds/{id}");
    }

    public List<Batch> batches() {
        return list(Batch.class, "/api/batches");
    }

    public Batch batch(String id) {
        return one(Batch.class, "Batch", id, "/api/batches/{id}");
    }

    // ---- feed ----

    public List<FeedLog> feedLogs() {
        return list(FeedLog.class, "/api/feed-logs");
    }

    public List<FeedLog> feedLogsByBatch(String batchId) {
        return list(FeedLog.class, "/api/feed-logs/batch/{id}", batchId);
    }

    /** The farm's feed stock; for a sub-tenant the API returns the parent farm's global stock. */
    public List<FeedStock> feedStock() {
        return list(FeedStock.class, "/api/feed-stock");
    }

    public List<FeedStock> lowStock() {
        return list(FeedStock.class, "/api/feed-stock/low");
    }

    public List<FeedOrder> feedOrders() {
        return list(FeedOrder.class, "/api/feed-orders");
    }

    // ---- water quality, growth, harvest, fish stock ----

    public List<MonitoringLog> monitoringLogs() {
        return allPages(MonitoringLog.class, "/api/monitoring-logs");
    }

    public List<MonitoringLog> monitoringLogsByBatch(String batchId) {
        return list(MonitoringLog.class, "/api/monitoring-logs/batch/{id}", batchId);
    }

    public List<SampleLog> sampleLogs() {
        return allPages(SampleLog.class, "/api/sample-logs");
    }

    public List<SampleLog> sampleLogsByBatch(String batchId) {
        return list(SampleLog.class, "/api/sample-logs/batch/{id}", batchId);
    }

    public List<HarvestLog> harvestLogs() {
        return list(HarvestLog.class, "/api/harvest-logs");
    }

    public List<HarvestLog> harvestLogsByBatch(String batchId) {
        return list(HarvestLog.class, "/api/harvest-logs/batch/{id}", batchId);
    }

    public List<FishStockEvent> fishStockEvents() {
        return list(FishStockEvent.class, "/api/fish-stock-events");
    }

    public List<FishStockEvent> fishStockEventsByBatch(String batchId) {
        return list(FishStockEvent.class, "/api/fish-stock-events/batch/{id}", batchId);
    }

    // ---- people and money ----

    public List<Employee> employees() {
        return list(Employee.class, "/api/employees");
    }

    public List<SalaryPayment> salaryPayments() {
        return list(SalaryPayment.class, "/api/salary-payments");
    }

    public List<SalaryPayment> salaryPaymentsByEmployee(String employeeId) {
        return list(SalaryPayment.class, "/api/salary-payments/employee/{id}", employeeId);
    }

    public List<Expenditure> expenditures() {
        return list(Expenditure.class, "/api/expenditures");
    }

    public List<Expenditure> expendituresByBatch(String batchId) {
        return list(Expenditure.class, "/api/expenditures/batch/{id}", batchId);
    }

    // ---- catalogue ----

    public List<FishType> fishTypes() {
        return list(FishType.class, "/api/fish-types");
    }

    public List<Supplier> suppliers() {
        return list(Supplier.class, "/api/suppliers");
    }

    // ---- helpers ----

    private <T> List<T> list(Class<T> type, String uriTemplate, Object... vars) {
        List<T> result = http.get(listOf(type), uriTemplate, vars);
        return result == null ? List.of() : result;
    }

    private <T> T one(Class<T> type, String what, String id, String uriTemplate) {
        try {
            return http.get(type, uriTemplate, id);
        } catch (ApiNotFoundException e) {
            throw new ToolUserException(what + " not found: " + id);
        }
    }

    private <T> List<T> allPages(Class<T> type, String uriTemplate) {
        ParameterizedTypeReference<ApiPage<T>> pageType = ParameterizedTypeReference.forType(
                ResolvableType.forClassWithGenerics(ApiPage.class, type).getType());
        String template = uriTemplate + "?page={page}&size={size}";
        List<T> all = new ArrayList<>();
        for (int page = 0; page < maxPages; page++) {
            ApiPage<T> current = http.get(pageType, template, page, pageSize);
            if (current.content() != null) {
                all.addAll(current.content());
            }
            if (current.last() || current.content() == null || current.content().isEmpty()) {
                return all;
            }
        }
        throw new ToolUserException("Too many records to read at once (more than " + (long) maxPages * pageSize
                + "). Narrow the question, for example to one batch.");
    }

    private static <T> ParameterizedTypeReference<List<T>> listOf(Class<T> type) {
        return ParameterizedTypeReference.forType(ResolvableType.forClassWithGenerics(List.class, type).getType());
    }
}
