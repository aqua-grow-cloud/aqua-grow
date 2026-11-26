package com.aquagrow.api.repository;

import com.aquagrow.api.model.FeedStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class FeedStockRepositoryTest {

    @Autowired
    private FeedStockRepository feedStockRepository;

    @BeforeEach
    void setUp() {
        FeedStock stock = new FeedStock();
        stock.setId("stock-1");
        stock.setTenantId("root-farm");
        stock.setFeedType("Pellet A");
        stock.setCurrentStock(100.0);
        stock.setThreshold(20.0);
        stock.setUnit("kg");
        feedStockRepository.save(stock);
    }

    @Test
    void deductStock_withSufficientStock_returnsOneAndDeducts() {
        int updated = feedStockRepository.deductStock(
                "root-farm", "Pellet A", 30.0, LocalDateTime.now());

        assertThat(updated).isEqualTo(1);
        Optional<FeedStock> stock = feedStockRepository.findByTenantIdAndFeedType("root-farm", "Pellet A");
        assertThat(stock).isPresent();
        assertThat(stock.get().getCurrentStock()).isEqualTo(70.0);
    }

    @Test
    void deductStock_withInsufficientStock_returnsZero() {
        int updated = feedStockRepository.deductStock(
                "root-farm", "Pellet A", 200.0, LocalDateTime.now());

        assertThat(updated).isEqualTo(0);
        // Stock unchanged
        Optional<FeedStock> stock = feedStockRepository.findByTenantIdAndFeedType("root-farm", "Pellet A");
        assertThat(stock.get().getCurrentStock()).isEqualTo(100.0);
    }

    @Test
    void deductStock_withWrongTenant_returnsZero() {
        int updated = feedStockRepository.deductStock(
                "other-tenant", "Pellet A", 10.0, LocalDateTime.now());

        assertThat(updated).isEqualTo(0);
    }

    @Test
    void deductStock_withWrongFeedType_returnsZero() {
        int updated = feedStockRepository.deductStock(
                "root-farm", "Unknown Type", 10.0, LocalDateTime.now());

        assertThat(updated).isEqualTo(0);
    }
}
