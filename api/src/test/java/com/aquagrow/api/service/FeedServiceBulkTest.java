package com.aquagrow.api.service;

import com.aquagrow.api.dto.BulkFeedLogRequest;
import com.aquagrow.api.dto.BulkFeedLogResponse;
import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.FeedLog;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.repository.FeedLogRepository;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class FeedServiceBulkTest {

    private static final String FEED = "King Fish Feed";

    @Autowired
    private FeedLogRepository feedLogRepository;

    @Autowired
    private FeedStockRepository feedStockRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private FeedService feedService;

    @BeforeEach
    void setUp() {
        feedService = new FeedService(feedLogRepository, feedStockRepository, entityManager);
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private void givenStock(String tenantId, double kg) {
        FeedStock stock = new FeedStock();
        stock.setId("stock-" + tenantId);
        stock.setTenantId(tenantId);
        stock.setFeedType(FEED);
        stock.setCurrentStock(kg);
        stock.setThreshold(50.0);
        stock.setUnit("kg");
        feedStockRepository.save(stock);
        entityManager.flush();
        entityManager.clear();
    }

    private void pinFilterTo(String tenantId) {
        entityManager.unwrap(Session.class).enableFilter("tenantFilter").setParameter("tenantId", tenantId);
    }

    private double stockOf(String tenantId) {
        entityManager.flush();
        entityManager.clear();
        return feedStockRepository.findById("stock-" + tenantId).orElseThrow().getCurrentStock();
    }

    private static List<BulkFeedLogRequest.Entry> dailyEntries(LocalDate from, int days, double kg) {
        List<BulkFeedLogRequest.Entry> entries = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            entries.add(new BulkFeedLogRequest.Entry(from.plusDays(i), null, kg));
        }
        return entries;
    }

    @Test
    void bulk_elevenBagsOf40kgFor12Days_createsTwelveLogsAndDeducts5280kgOnce() {
        givenStock("farm", 10000.0);
        TenantContext.setTenantId("farm");
        pinFilterTo("farm");

        BulkFeedLogResponse response = feedService.createFeedLogsBulk(new BulkFeedLogRequest(
                "batch-1", FEED, dailyEntries(LocalDate.of(2026, 10, 1), 12, 440.0)));

        assertThat(response.createdIds()).hasSize(12);
        assertThat(response.totalKg()).isEqualTo(5280.0);
        assertThat(response.remainingStockKg()).isEqualTo(4720.0);
        assertThat(stockOf("farm")).isEqualTo(4720.0);

        List<FeedLog> logs = feedLogRepository.findByBatchId("batch-1");
        assertThat(logs).hasSize(12);
        assertThat(logs).allSatisfy(l -> {
            assertThat(l.getTenantId()).isEqualTo("farm");
            assertThat(l.getFeedType()).isEqualTo(FEED);
            assertThat(l.getAmount()).isEqualTo(440.0);
        });
        assertThat(logs).extracting(FeedLog::getDate)
                .contains(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 12));
    }

    @Test
    void bulk_insufficientStock_throwsAndSavesNothing() {
        givenStock("farm", 1000.0);
        TenantContext.setTenantId("farm");
        pinFilterTo("farm");

        assertThatThrownBy(() -> feedService.createFeedLogsBulk(new BulkFeedLogRequest(
                "batch-1", FEED, dailyEntries(LocalDate.of(2026, 10, 1), 12, 440.0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient");

        assertThat(feedLogRepository.findByBatchId("batch-1")).isEmpty();
        assertThat(stockOf("farm")).isEqualTo(1000.0);
    }

    @Test
    void bulk_subTenant_deductsFromRootStockAndLogsBelongToSubTenant() {
        givenStock("root-farm", 10000.0);
        TenantContext.setTenantId("sub-farm");
        TenantContext.setParentTenantId("root-farm");
        pinFilterTo("sub-farm");

        BulkFeedLogResponse response = feedService.createFeedLogsBulk(new BulkFeedLogRequest(
                "batch-1", FEED, dailyEntries(LocalDate.of(2026, 10, 1), 12, 440.0)));

        assertThat(response.remainingStockKg()).isEqualTo(4720.0);
        assertThat(stockOf("root-farm")).isEqualTo(4720.0);
        assertThat(feedLogRepository.findByBatchId("batch-1"))
                .hasSize(12)
                .allSatisfy(l -> assertThat(l.getTenantId()).isEqualTo("sub-farm"));
    }

    @Test
    void bulk_unknownFeedType_throwsAndSavesNothing() {
        givenStock("farm", 10000.0);
        TenantContext.setTenantId("farm");
        pinFilterTo("farm");

        assertThatThrownBy(() -> feedService.createFeedLogsBulk(new BulkFeedLogRequest(
                "batch-1", "No Such Feed", dailyEntries(LocalDate.of(2026, 10, 1), 2, 10.0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not found");

        assertThat(feedLogRepository.findByBatchId("batch-1")).isEmpty();
        assertThat(stockOf("farm")).isEqualTo(10000.0);
    }

    @Test
    void bulk_missingTenantContext_throws() {
        assertThatThrownBy(() -> feedService.createFeedLogsBulk(new BulkFeedLogRequest(
                "batch-1", FEED, dailyEntries(LocalDate.of(2026, 10, 1), 1, 10.0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant context");
    }

    @Test
    void bulk_invalidInput_isRejectedBeforeTouchingStock() {
        givenStock("farm", 1000.0);
        TenantContext.setTenantId("farm");
        pinFilterTo("farm");
        LocalDate d = LocalDate.of(2026, 10, 1);

        List<BulkFeedLogRequest> invalid = List.of(
                new BulkFeedLogRequest("batch-1", FEED, List.of()),
                new BulkFeedLogRequest("batch-1", FEED, null),
                new BulkFeedLogRequest(" ", FEED, dailyEntries(d, 1, 10.0)),
                new BulkFeedLogRequest("batch-1", "", dailyEntries(d, 1, 10.0)),
                new BulkFeedLogRequest("batch-1", FEED, dailyEntries(d, 1, 0.0)),
                new BulkFeedLogRequest("batch-1", FEED, dailyEntries(d, 1, -5.0)),
                new BulkFeedLogRequest("batch-1", FEED, List.of(new BulkFeedLogRequest.Entry(null, null, 10.0))),
                new BulkFeedLogRequest("batch-1", FEED, List.of(new BulkFeedLogRequest.Entry(d, null, null))),
                new BulkFeedLogRequest("batch-1", FEED, List.of(
                        new BulkFeedLogRequest.Entry(d, "08:00", 10.0),
                        new BulkFeedLogRequest.Entry(d, "08:00", 10.0))),
                new BulkFeedLogRequest("batch-1", FEED, dailyEntries(d, 367, 1.0)));

        for (BulkFeedLogRequest request : invalid) {
            assertThatThrownBy(() -> feedService.createFeedLogsBulk(request))
                    .isInstanceOf(BusinessException.class);
        }
        assertThat(feedLogRepository.findByBatchId("batch-1")).isEmpty();
        assertThat(stockOf("farm")).isEqualTo(1000.0);
    }

    @Test
    void bulk_sameDateDifferentTimes_isAllowed() {
        givenStock("farm", 1000.0);
        TenantContext.setTenantId("farm");
        pinFilterTo("farm");
        LocalDate d = LocalDate.of(2026, 10, 1);

        BulkFeedLogResponse response = feedService.createFeedLogsBulk(new BulkFeedLogRequest(
                "batch-1", FEED, List.of(
                        new BulkFeedLogRequest.Entry(d, "08:00", 20.0),
                        new BulkFeedLogRequest.Entry(d, "17:00", 20.0))));

        assertThat(response.createdIds()).hasSize(2);
        assertThat(response.totalKg()).isEqualTo(40.0);
    }
}
