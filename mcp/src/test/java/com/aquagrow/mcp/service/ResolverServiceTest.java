package com.aquagrow.mcp.service;

import com.aquagrow.mcp.client.AquaGrowApiClient;
import com.aquagrow.mcp.client.FeedStockView;
import com.aquagrow.mcp.client.ToolUserException;
import com.aquagrow.mcp.config.TenantFilterActivator;
import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.model.FishSeed;
import com.aquagrow.mcp.model.FishType;
import com.aquagrow.mcp.model.Pond;
import com.aquagrow.mcp.repository.BatchRepository;
import com.aquagrow.mcp.repository.FishTypeRepository;
import com.aquagrow.mcp.repository.PondRepository;
import com.aquagrow.mcp.service.ResolverService.AmbiguousReferenceException;
import com.aquagrow.mcp.service.ResolverService.ResolvedBatch;
import com.aquagrow.mcp.service.ResolverService.ResolvedFishType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolverServiceTest {

    private static final String TENANT = "t1";

    @Mock BatchRepository batchRepository;
    @Mock PondRepository pondRepository;
    @Mock FishTypeRepository fishTypeRepository;
    @Mock TenantFilterActivator tenantFilterActivator;
    @Mock AquaGrowApiClient apiClient;

    private ResolverService resolver;

    @BeforeEach
    void setUp() {
        resolver = new ResolverService(batchRepository, pondRepository, fishTypeRepository,
                tenantFilterActivator, apiClient);
    }

    private static Pond pond(String id, String name) {
        return Pond.builder().id(id).name(name).tenantId(TENANT).status("Active").build();
    }

    private static FishSeed seed(String pondId, String status) {
        return FishSeed.builder().pondId(pondId).fishTypeId("ft-1").status(status).tenantId(TENANT).build();
    }

    private static Batch batch(String id, String name, String pondId, String seedStatus) {
        return Batch.builder().id(id).name(name).tenantId(TENANT)
                .stockingDate(LocalDate.of(2026, 9, 1))
                .fishSeeds(List.of(seed(pondId, seedStatus))).build();
    }

    private void givenFarm(List<Batch> batches, List<Pond> ponds) {
        when(tenantFilterActivator.activate()).thenReturn(TENANT);
        when(batchRepository.findByTenantId(TENANT)).thenReturn(batches);
        when(pondRepository.findByTenantId(TENANT)).thenReturn(ponds);
        lenient().when(fishTypeRepository.findByTenantId(TENANT))
                .thenReturn(List.of(FishType.builder().id("ft-1").name("Tilapia").tenantId(TENANT).build()));
    }

    // ---- batches ----

    @Test
    void resolveBatch_byExactId() {
        givenFarm(List.of(batch("b-1", "Sept Tilapia", "p-1", "Active"), batch("b-2", "Other", "p-2", "Active")),
                List.of(pond("p-1", "Pond 1"), pond("p-2", "Pond 2")));

        ResolvedBatch resolved = resolver.resolveBatch("b-2");

        assertThat(resolved.id()).isEqualTo("b-2");
        assertThat(resolved.pondNames()).containsExactly("Pond 2");
    }

    @Test
    void resolveBatch_byNameIgnoringCaseAndExtraSpaces() {
        givenFarm(List.of(batch("b-1", "Sept Tilapia", "p-1", "Active"), batch("b-2", "Other", "p-2", "Active")),
                List.of(pond("p-1", "Pond 1"), pond("p-2", "Pond 2")));

        assertThat(resolver.resolveBatch("  sept   TILAPIA ").id()).isEqualTo("b-1");
    }

    @Test
    void resolveBatch_byPondName() {
        givenFarm(List.of(batch("b-1", "Sept Tilapia", "p-1", "Active"), batch("b-3", "Oct Catfish", "p-3", "Active")),
                List.of(pond("p-1", "Pond 1"), pond("p-3", "Pond 3")));

        ResolvedBatch resolved = resolver.resolveBatch("pond 3");

        assertThat(resolved.id()).isEqualTo("b-3");
        assertThat(resolved.name()).isEqualTo("Oct Catfish");
        assertThat(resolved.pondNames()).containsExactly("Pond 3");
    }

    @Test
    void resolveBatch_byPondId() {
        givenFarm(List.of(batch("b-3", "Oct Catfish", "p-3", "Active")), List.of(pond("p-3", "Pond 3")));

        assertThat(resolver.resolveBatch("p-3").id()).isEqualTo("b-3");
    }

    @Test
    void resolveBatch_uniquePartialNameMatchWhenNoExactMatch() {
        givenFarm(List.of(batch("b-1", "Sept Tilapia", "p-1", "Active"), batch("b-2", "Oct Catfish", "p-2", "Active")),
                List.of(pond("p-1", "Pond 1"), pond("p-2", "Pond 2")));

        assertThat(resolver.resolveBatch("catfish").id()).isEqualTo("b-2");
    }

    @Test
    void resolveBatch_exactMatchBeatsPartialMatches() {
        givenFarm(List.of(batch("b-1", "Pond", "p-1", "Active"), batch("b-2", "Pond North", "p-2", "Active")),
                List.of(pond("p-1", "Pond 1"), pond("p-2", "Pond 2")));

        assertThat(resolver.resolveBatch("pond").id()).isEqualTo("b-1");
    }

    @Test
    void resolveBatch_ambiguous_listsCandidatesForTheUserToChoose() {
        givenFarm(List.of(batch("b-1", "Sept Tilapia", "p-1", "Active"), batch("b-2", "Oct Tilapia", "p-2", "Active")),
                List.of(pond("p-1", "Pond 1"), pond("p-2", "Pond 2")));

        assertThatThrownBy(() -> resolver.resolveBatch("tilapia"))
                .isInstanceOf(AmbiguousReferenceException.class)
                .hasMessageContaining("b-1").hasMessageContaining("Sept Tilapia").hasMessageContaining("Pond 1")
                .hasMessageContaining("b-2").hasMessageContaining("Oct Tilapia").hasMessageContaining("Pond 2")
                .hasMessageContaining("which");
    }

    @Test
    void resolveBatch_pondWithTwoActiveBatches_isAmbiguous() {
        givenFarm(List.of(batch("b-1", "Batch A", "p-1", "Active"), batch("b-2", "Batch B", "p-1", "Active")),
                List.of(pond("p-1", "Pond 1")));

        assertThatThrownBy(() -> resolver.resolveBatch("Pond 1")).isInstanceOf(AmbiguousReferenceException.class);
    }

    @Test
    void resolveBatch_noMatch_listsActiveBatches() {
        givenFarm(List.of(batch("b-1", "Sept Tilapia", "p-1", "Active"), batch("b-2", "Old", "p-2", "Harvested")),
                List.of(pond("p-1", "Pond 1"), pond("p-2", "Pond 2")));

        assertThatThrownBy(() -> resolver.resolveBatch("zebra"))
                .isInstanceOf(ToolUserException.class)
                .isNotInstanceOf(AmbiguousReferenceException.class)
                .hasMessageContaining("No active batch matches 'zebra'")
                .hasMessageContaining("Sept Tilapia")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("Old"));
    }

    @Test
    void resolveBatch_completedBatchIsExcluded_andSaidSo() {
        givenFarm(List.of(batch("b-2", "Old Batch", "p-2", "Harvested")), List.of(pond("p-2", "Pond 2")));

        assertThatThrownBy(() -> resolver.resolveBatch("old batch"))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void resolveBatch_activeBatchWinsOverCompletedWithSameName() {
        givenFarm(List.of(batch("b-old", "Tilapia", "p-1", "Harvested"), batch("b-new", "Tilapia", "p-1", "Active")),
                List.of(pond("p-1", "Pond 1")));

        assertThat(resolver.resolveBatch("tilapia").id()).isEqualTo("b-new");
    }

    @Test
    void resolveBatch_batchWithoutSeedsIsNotActive() {
        Batch empty = Batch.builder().id("b-9").name("Empty").tenantId(TENANT).fishSeeds(List.of()).build();
        givenFarm(List.of(empty), List.of());

        assertThatThrownBy(() -> resolver.resolveBatch("empty")).isInstanceOf(ToolUserException.class);
    }

    @Test
    void resolveBatch_blankReference() {
        assertThatThrownBy(() -> resolver.resolveBatch("  "))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("batch");
        assertThatThrownBy(() -> resolver.resolveBatch(null)).isInstanceOf(ToolUserException.class);
    }

    @Test
    void resolveBatch_readsOnlyTheCallersTenant() {
        givenFarm(List.of(batch("b-1", "Sept Tilapia", "p-1", "Active")), List.of(pond("p-1", "Pond 1")));

        resolver.resolveBatch("b-1");

        verify(tenantFilterActivator).activate();
        verify(batchRepository).findByTenantId(TENANT);
        verify(pondRepository).findByTenantId(TENANT);
        verifyNoMoreInteractions(batchRepository, pondRepository);
    }

    // ---- fish type within a batch ----

    private void givenBatchWithFishTypes(Batch batch, List<FishType> types, List<Pond> ponds) {
        when(tenantFilterActivator.activate()).thenReturn(TENANT);
        when(batchRepository.findById(batch.getId())).thenReturn(Optional.of(batch));
        when(fishTypeRepository.findByTenantId(TENANT)).thenReturn(types);
        lenient().when(pondRepository.findByTenantId(TENANT)).thenReturn(ponds);
    }

    private static FishType fishType(String id, String name) {
        return FishType.builder().id(id).name(name).tenantId(TENANT).build();
    }

    private static Batch mixedBatch() {
        FishSeed tilapia = FishSeed.builder().pondId("p-1").fishTypeId("ft-1").status("Active").tenantId(TENANT).build();
        FishSeed catfish = FishSeed.builder().pondId("p-2").fishTypeId("ft-2").status("Active").tenantId(TENANT).build();
        return Batch.builder().id("b-1").name("Mixed").tenantId(TENANT).fishSeeds(List.of(tilapia, catfish)).build();
    }

    @Test
    void resolveFishType_singleTypeBatchNeedsNoReference() {
        givenBatchWithFishTypes(batch("b-1", "Sept", "p-1", "Active"),
                List.of(fishType("ft-1", "Tilapia"), fishType("ft-9", "Unused")), List.of(pond("p-1", "Pond 1")));

        ResolvedFishType resolved = resolver.resolveFishType("b-1", null);

        assertThat(resolved.id()).isEqualTo("ft-1");
        assertThat(resolved.name()).isEqualTo("Tilapia");
        assertThat(resolved.pondNames()).containsExactly("Pond 1");
    }

    @Test
    void resolveFishType_multipleTypesWithoutReference_asksWhich() {
        givenBatchWithFishTypes(mixedBatch(), List.of(fishType("ft-1", "Tilapia"), fishType("ft-2", "Catfish")),
                List.of(pond("p-1", "Pond 1"), pond("p-2", "Pond 2")));

        assertThatThrownBy(() -> resolver.resolveFishType("b-1", " "))
                .isInstanceOf(AmbiguousReferenceException.class)
                .hasMessageContaining("Tilapia").hasMessageContaining("Catfish").hasMessageContaining("which");
    }

    @Test
    void resolveFishType_byNameIgnoringCase_andPartial() {
        givenBatchWithFishTypes(mixedBatch(), List.of(fishType("ft-1", "Tilapia"), fishType("ft-2", "Catfish")),
                List.of(pond("p-1", "Pond 1"), pond("p-2", "Pond 2")));

        assertThat(resolver.resolveFishType("b-1", "CATFISH").id()).isEqualTo("ft-2");
        assertThat(resolver.resolveFishType("b-1", "til").id()).isEqualTo("ft-1");
        assertThat(resolver.resolveFishType("b-1", "ft-2").pondNames()).containsExactly("Pond 2");
    }

    @Test
    void resolveFishType_typeNotInThisBatch_listsTheBatchsTypes() {
        givenBatchWithFishTypes(mixedBatch(), List.of(fishType("ft-1", "Tilapia"), fishType("ft-2", "Catfish"),
                fishType("ft-3", "Shrimp")), List.of());

        assertThatThrownBy(() -> resolver.resolveFishType("b-1", "shrimp"))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("No fish type matches 'shrimp'")
                .hasMessageContaining("Tilapia").hasMessageContaining("Catfish");
    }

    @Test
    void resolveFishType_sameTypeInTwoPonds_reportsBothPonds() {
        FishSeed a = FishSeed.builder().pondId("p-1").fishTypeId("ft-1").status("Active").tenantId(TENANT).build();
        FishSeed b = FishSeed.builder().pondId("p-2").fishTypeId("ft-1").status("Active").tenantId(TENANT).build();
        Batch twoPonds = Batch.builder().id("b-1").name("Two").tenantId(TENANT).fishSeeds(List.of(a, b)).build();
        givenBatchWithFishTypes(twoPonds, List.of(fishType("ft-1", "Tilapia")),
                List.of(pond("p-1", "Pond 1"), pond("p-2", "Pond 2")));

        assertThat(resolver.resolveFishType("b-1", null).pondNames()).containsExactlyInAnyOrder("Pond 1", "Pond 2");
    }

    @Test
    void resolveFishType_batchWithoutFishTypes() {
        Batch empty = Batch.builder().id("b-1").name("Empty").tenantId(TENANT).fishSeeds(List.of()).build();
        givenBatchWithFishTypes(empty, List.of(), List.of());

        assertThatThrownBy(() -> resolver.resolveFishType("b-1", null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("no fish");
    }

    @Test
    void resolveFishType_batchFromAnotherTenantIsNotFound() {
        Batch foreign = Batch.builder().id("b-x").name("Foreign").tenantId("other").fishSeeds(List.of()).build();
        when(tenantFilterActivator.activate()).thenReturn(TENANT);
        when(batchRepository.findById("b-x")).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> resolver.resolveFishType("b-x", null))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("Batch not found");
    }

    // ---- feed types ----

    private static FeedStockView stock(String feedType) {
        return new FeedStockView("id-" + feedType, feedType, 100.0, 10.0, "kg");
    }

    @Test
    void resolveFeedType_exactIgnoringCase_returnsStoredSpelling() {
        when(apiClient.getFeedStock()).thenReturn(List.of(stock("King Fish Feed"), stock("Starter Crumble")));

        assertThat(resolver.resolveFeedType(" king fish   feed ")).isEqualTo("King Fish Feed");
    }

    @Test
    void resolveFeedType_uniquePartialMatch() {
        when(apiClient.getFeedStock()).thenReturn(List.of(stock("King Fish Feed"), stock("Starter Crumble")));

        assertThat(resolver.resolveFeedType("starter")).isEqualTo("Starter Crumble");
    }

    @Test
    void resolveFeedType_ambiguous() {
        when(apiClient.getFeedStock()).thenReturn(List.of(stock("Floating Feed 2mm"), stock("Floating Feed 4mm")));

        assertThatThrownBy(() -> resolver.resolveFeedType("floating"))
                .isInstanceOf(AmbiguousReferenceException.class)
                .hasMessageContaining("Floating Feed 2mm").hasMessageContaining("Floating Feed 4mm");
    }

    @Test
    void resolveFeedType_unknown_listsValidTypes() {
        when(apiClient.getFeedStock()).thenReturn(List.of(stock("King Fish Feed"), stock("Starter Crumble")));

        assertThatThrownBy(() -> resolver.resolveFeedType("mystery"))
                .isInstanceOf(ToolUserException.class)
                .hasMessageContaining("No feed type matches 'mystery'")
                .hasMessageContaining("King Fish Feed").hasMessageContaining("Starter Crumble");
    }

    @Test
    void resolveFeedType_noStockAtAll() {
        when(apiClient.getFeedStock()).thenReturn(List.of());

        assertThatThrownBy(() -> resolver.resolveFeedType("anything"))
                .isInstanceOf(ToolUserException.class).hasMessageContaining("no feed stock");
    }

    @Test
    void resolveFeedType_blankReference() {
        assertThatThrownBy(() -> resolver.resolveFeedType(" ")).isInstanceOf(ToolUserException.class);
    }
}
