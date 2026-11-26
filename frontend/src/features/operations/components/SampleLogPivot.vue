<template>
  <v-card class="mb-4">
    <v-card-title>Sample Performance (Pivot)</v-card-title>
    <v-card-text>
      <div v-if="isLoading" class="d-flex justify-center pa-4">
        <v-progress-circular indeterminate color="primary"></v-progress-circular>
      </div>

      <v-table v-else density="compact" class="text-caption">
        <thead>
          <tr>
            <th class="text-left font-weight-bold">Batch</th>
            <th v-for="speciesId in allSpeciesIds" :key="speciesId" class="text-center font-weight-bold">
              {{ getFishTypeName(speciesId) }} (g)
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in pivotData" :key="row.batchId">
            <td class="font-weight-medium">{{ row.batchName }}</td>
            <td v-for="speciesId in allSpeciesIds" :key="speciesId" class="text-center">
              <span v-if="row.metrics[speciesId]" class="text-primary font-weight-bold">
                {{ row.metrics[speciesId].toFixed(2) }}
              </span>
              <span v-else class="text-grey-lighten-2">-</span>
            </td>
          </tr>
        </tbody>
      </v-table>
    </v-card-text>
  </v-card>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {sampleLogService} from '../api/sampleLogService';
import {useBatches} from '../../batches/composables/useBatches';
import {fishTypeService} from '../../settings/api/fishTypeService';

const { batches } = useBatches();
const { data: pivotRows, isLoading } = sampleLogService.useGetPivot();
const { data: fishTypes } = fishTypeService.useGetAll();

// Build pivot table: one row per batch, one column per species (fishTypeId)
// The pivot endpoint aggregates weight per batch, but not per species.
// We reconstruct species columns from the batch's fishSeeds to show current ABW per species.
// Since the pivot endpoint doesn't break down by fishTypeId, we show the batch-level
// weighted avg weight in all species columns that batch contains.
const pivotData = computed(() => {
    if (!pivotRows.value || !batches.value) return [];

    return pivotRows.value.map(row => {
        const batch = batches.value.find((b: any) => b.id === row.batchId);
        const batchName = batch?.name || 'Unknown Batch';

        // Collect all fishTypeIds in this batch
        const speciesIds: string[] = [...new Set(
            (batch?.fishSeeds || []).map((s: any) => s.fishTypeId).filter(Boolean)
        )] as string[];

        // Map each species to the weighted avg ABW for this batch
        const metrics: Record<string, number> = {};
        speciesIds.forEach(id => {
            if (row.weightedAvgWeight > 0) {
                metrics[id] = row.weightedAvgWeight;
            }
        });

        return { batchId: row.batchId, batchName, metrics };
    });
});

const allSpeciesIds = computed(() => {
    const ids = new Set<string>();
    pivotData.value.forEach(row => {
        Object.keys(row.metrics).forEach(id => ids.add(id));
    });
    return Array.from(ids).sort();
});

function getFishTypeName(id?: string) {
    if (!id || id === 'Unknown') return 'Unknown';
    return (fishTypes.value as any)?.find((t: any) => t.id === id)?.name || id;
}
</script>
