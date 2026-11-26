package com.aquagrow.api.security;

import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.Employee;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.model.FishSeed;
import com.aquagrow.api.model.Pond;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.EmployeeRepository;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.repository.PondRepository;
import com.aquagrow.api.security.TenantIsolationException.Kind;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hibernate's tenant filter only applies to queries; lookups by primary key, the load before an update or
 * delete, and saving an object that carries an existing id all bypass it. These tests run against a real
 * (in-memory) database to prove the guard closes every one of those paths, for all entities through the
 * same listener.
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class TenantGuardListenerTest {

    @Autowired PondRepository ponds;
    @Autowired EmployeeRepository employees;
    @Autowired BatchRepository batches;
    @Autowired FeedStockRepository feedStock;
    @Autowired EntityManager em;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private static void as(String tenant) {
        TenantContext.clear();
        TenantContext.setTenantId(tenant);
    }

    private static void asSubTenant(String tenant, String parent) {
        as(tenant);
        TenantContext.setParentTenantId(parent);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private String pondOf(String tenant) {
        as(tenant);
        Pond pond = ponds.saveAndFlush(Pond.builder().name("Pond of " + tenant).status("Active").build());
        flushAndClear();
        return pond.getId();
    }

    private static TenantIsolationException isolation(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof TenantIsolationException e) {
                return e;
            }
        }
        return null;
    }

    private static void assertIsolation(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, Kind expected) {
        assertThatThrownBy(action).satisfies(e -> {
            TenantIsolationException iso = isolation(e);
            assertThat(iso).as("expected a tenant isolation error, got %s", e).isNotNull();
            assertThat(iso.getKind()).isEqualTo(expected);
        });
    }

    private String pondName(String id) {
        return jdbc.queryForObject("select name from ponds where id = ?", String.class, id);
    }

    private String pondTenant(String id) {
        return jdbc.queryForObject("select tenant_id from ponds where id = ?", String.class, id);
    }

    // ---- the owner is unaffected ----

    @Test
    void theOwnerCanLoadUpdateAndDeleteTheirOwnRecords() {
        String id = pondOf("farm-1");

        as("farm-1");
        Pond loaded = ponds.findById(id).orElseThrow();
        loaded.setName("Renamed");
        ponds.saveAndFlush(loaded);
        flushAndClear();
        assertThat(pondName(id)).isEqualTo("Renamed");

        ponds.deleteById(id);
        flushAndClear();
        assertThat(jdbc.queryForObject("select count(*) from ponds where id = ?", Integer.class, id)).isZero();
    }

    // ---- reads by id ----

    @Test
    void anotherFarmCannotLoadARecordById() {
        String id = pondOf("farm-1");

        as("farm-2");

        assertIsolation(() -> ponds.findById(id), Kind.NOT_FOUND);
    }

    @Test
    void anotherFarmCannotLoadARecordByIdForAnyEntityType() {
        as("farm-1");
        Employee employee = employees.saveAndFlush(Employee.builder().name("Ravi").salary(20000.0).status("Active").build());
        Batch batch = batches.saveAndFlush(Batch.builder().name("Sept").stockingDate(LocalDate.of(2026, 9, 1)).build());
        flushAndClear();

        as("farm-2");

        assertIsolation(() -> employees.findById(employee.getId()), Kind.NOT_FOUND);
        assertIsolation(() -> batches.findById(batch.getId()), Kind.NOT_FOUND);
    }

    // ---- writes ----

    @Test
    void anotherFarmCannotChangeARecordByPostingItsId() {
        String id = pondOf("farm-1");
        as("farm-2");

        // what a "hijack" request does: Spring Data merges an object that carries an existing id
        assertThatThrownBy(() -> ponds.saveAndFlush(Pond.builder().id(id).name("HIJACKED").status("Active").tenantId("farm-2").build()))
                .satisfies(e -> assertThat(isolation(e)).isNotNull());

        flushAndClear();
        assertThat(pondName(id)).isEqualTo("Pond of farm-1");
        assertThat(pondTenant(id)).isEqualTo("farm-1");
    }

    @Test
    void anotherFarmCannotDeleteARecord() {
        String id = pondOf("farm-1");
        as("farm-2");

        assertIsolation(() -> ponds.deleteById(id), Kind.NOT_FOUND);
        flushAndClear();

        assertThat(jdbc.queryForObject("select count(*) from ponds where id = ?", Integer.class, id)).isEqualTo(1);
    }

    @Test
    void anotherFarmCannotDeleteARecordItAlreadyHoldsAnObjectFor() {
        as("farm-1");
        Pond pond = ponds.saveAndFlush(Pond.builder().name("Pond").status("Active").build());
        flushAndClear();
        as("farm-2");

        // Spring Data reloads a detached object before deleting it, so this is stopped by the load check
        // (not found) or by the delete check (forbidden); either way the record must survive.
        assertThatThrownBy(() -> {
            ponds.delete(pond);
            em.flush();
        }).satisfies(e -> assertThat(isolation(e)).isNotNull());

        em.clear();
        assertThat(jdbc.queryForObject("select count(*) from ponds where id = ?", Integer.class, pond.getId())).isEqualTo(1);
    }

    @Test
    void aRecordCannotBeCreatedForAnotherFarm() {
        as("farm-2");

        assertIsolation(() -> ponds.saveAndFlush(Pond.builder().name("Injected").status("Active").tenantId("farm-1").build()),
                Kind.FORBIDDEN);
    }

    @Test
    void aRecordCannotBeHandedToAnotherFarmByChangingItsTenant() {
        String id = pondOf("farm-1");
        as("farm-1");
        Pond pond = ponds.findById(id).orElseThrow();
        pond.setTenantId("farm-2");

        assertIsolation(() -> ponds.saveAndFlush(pond), Kind.FORBIDDEN);

        em.clear();
        assertThat(pondTenant(id)).isEqualTo("farm-1");
    }

    @Test
    void aNewRecordWithoutAnOwnerBelongsToTheCaller() {
        as("farm-1");

        Pond pond = ponds.saveAndFlush(Pond.builder().name("No owner given").status("Active").build());

        assertThat(pond.getTenantId()).isEqualTo("farm-1");
    }

    @Test
    void aBatchCannotBeCreatedWithASeedThatBelongsToAnotherFarm() {
        as("farm-2");
        Batch batch = Batch.builder().name("Mixed").stockingDate(LocalDate.of(2026, 9, 1))
                .fishSeeds(List.of(FishSeed.builder().pondId("p").fishTypeId("ft").quantity(1).tenantId("farm-1").build()))
                .build();

        assertIsolation(() -> batches.saveAndFlush(batch), Kind.FORBIDDEN);
    }

    @Test
    void aBatchWithItsOwnSeedsLoadsFine() {
        as("farm-1");
        Batch saved = batches.saveAndFlush(Batch.builder().name("Own").stockingDate(LocalDate.of(2026, 9, 1))
                .fishSeeds(List.of(FishSeed.builder().pondId("p").fishTypeId("ft").quantity(5).tenantId("farm-1").build()))
                .build());
        flushAndClear();

        Batch loaded = batches.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getFishSeeds()).hasSize(1);
        assertThat(loaded.getFishSeeds().get(0).getQuantity()).isEqualTo(5);
    }

    // ---- the one deliberate sharing: a sub-account uses its parent farm's feed stock ----

    private String stockOf(String tenant) {
        as(tenant);
        FeedStock stock = feedStock.saveAndFlush(FeedStock.builder().feedType("King Fish Feed").currentStock(1000.0).threshold(10.0).unit("kg").build());
        flushAndClear();
        return stock.getId();
    }

    @Test
    void aSubAccountCanLoadAndUpdateItsParentFarmsFeedStock() {
        String rootStock = stockOf("root-farm");

        asSubTenant("sub-farm", "root-farm");
        FeedStock stock = feedStock.findById(rootStock).orElseThrow();
        stock.setCurrentStock(900.0);
        feedStock.saveAndFlush(stock);
        flushAndClear();

        assertThat(jdbc.queryForObject("select current_stock from feed_stock where id = ?", Double.class, rootStock)).isEqualTo(900.0);
    }

    @Test
    void aSubAccountCannotTouchSomeoneElsesFeedStock() {
        String strangersStock = stockOf("another-farm");

        asSubTenant("sub-farm", "root-farm");

        assertIsolation(() -> feedStock.findById(strangersStock), Kind.NOT_FOUND);
    }

    @Test
    void aParentFarmDoesNotGetAccessToItsSubAccountsOtherRecords() {
        String subPond = pondOf("sub-farm");

        asSubTenant("root-farm", null);   // the parent acting as itself
        TenantContext.clear();
        as("root-farm");

        assertIsolation(() -> ponds.findById(subPond), Kind.NOT_FOUND);
    }

    // ---- no tenant bound: nothing is checked (startup, background work, tests) ----

    @Test
    void withoutABoundTenantNothingIsChecked() {
        String id = pondOf("farm-1");

        TenantContext.clear();

        assertThat(ponds.findById(id)).isPresent();
    }
}
