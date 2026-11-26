package com.aquagrow.mcp.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * These classes only describe what aqua-grow-api sends. They must never invent a value the API did not
 * send (a random id, "now", a status), because that would silently turn missing data into plausible data.
 */
class ModelsTest {

    @Test
    void feedOrder_hasNoInventedValues() {
        for (FeedOrder o : List.of(new FeedOrder(), FeedOrder.builder().build())) {
            assertThat(o.getId()).isNull();
            assertThat(o.getStatus()).isNull();
            assertThat(o.getPaymentStatus()).isNull();
            assertThat(o.getOrderDate()).isNull();
        }
    }

    @Test
    void feedStock_hasNoInventedValues() {
        for (FeedStock s : List.of(new FeedStock(), FeedStock.builder().build())) {
            assertThat(s.getId()).isNull();
            assertThat(s.getUnit()).isNull();
            assertThat(s.getLastUpdated()).isNull();
        }
    }

    @Test
    void supplier_hasNoInventedValues() {
        for (Supplier s : List.of(new Supplier(), Supplier.builder().build())) {
            assertThat(s.getId()).isNull();
            assertThat(s.getCreatedAt()).isNull();
            assertThat(s.getUpdatedAt()).isNull();
        }
    }

    @Test
    void fishSeed_hasNoInventedStatus_andABatchStillTreatsAMissingStatusAsNotHarvested() {
        assertThat(new FishSeed().getStatus()).isNull();
        assertThat(FishSeed.builder().build().getStatus()).isNull();

        Batch batch = Batch.builder().id("b-1").fishSeeds(List.of(FishSeed.builder().pondId("p-1").build())).build();
        assertThat(batch.getStatus()).isEqualTo("Active");
        Batch harvested = Batch.builder().id("b-2").fishSeeds(List.of(FishSeed.builder().status("Harvested").build())).build();
        assertThat(harvested.getStatus()).isEqualTo("Completed");
    }

    @Test
    void aBatchWithoutSeedsStillHasAnEmptyListNotNull() {
        assertThat(Batch.builder().build().getFishSeeds()).isEmpty();
        assertThat(new Batch().getFishSeeds()).isEmpty();
    }
}
