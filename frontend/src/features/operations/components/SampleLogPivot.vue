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
import { computed } from 'vue';
import { sampleLogService } from '../api/sampleLogService';
import { useBatches } from '../../batches/composables/useBatches';
import { fishTypeService } from '../../settings/api/fishTypeService';

const { batches } = useBatches();
// Fetch lots of logs to get good coverage. Ideally this is a specific backend aggregation.
const { data: logsData, isLoading } = sampleLogService.useSearch(computed(() => ({ size: 100 })));
const { data: fishTypes } = fishTypeService.useGetAll();

const logs = computed(() => {
    const raw = logsData.value;
    return (raw && 'content' in raw && Array.isArray((raw as any).content)) 
        ? (raw as any).content 
        : (Array.isArray(raw) ? raw : []);
});

const pivotData = computed(() => {
    if (!logs.value || !batches.value) return [];

    const batchMap = new Map();

    // Grouping
    logs.value.forEach((log: any) => {
        if (!batchMap.has(log.batchId)) {
            const batch = batches.value.find((b: any) => b.id === log.batchId);
            batchMap.set(log.batchId, {
                batchId: log.batchId,
                batchName: batch?.name || 'Unknown Batch',
                speciesData: {} 
            });
        }
        
        const entry = batchMap.get(log.batchId);
        const speciesId = log.fishTypeId || 'Unknown'; // Fallback if type missing

        if (!entry.speciesData[speciesId]) {
            entry.speciesData[speciesId] = { totalWeight: 0, totalCount: 0 };
        }

        // Weighted Average Calculation: Sum(TotalWeight) / Sum(Count)
        // If totalSampleWeight is missing, fallback to averageWeight * sampleCount
        const weight = log.totalSampleWeight || (log.averageWeight * log.sampleCount) || 0;
        const count = log.sampleCount || 0;

        entry.speciesData[speciesId].totalWeight += weight;
        entry.speciesData[speciesId].totalCount += count;
    });

    // Flatten to Row Structure
    return Array.from(batchMap.values()).map((entry: any) => {
        const metrics: Record<string, number> = {};
        for (const [speciesId, data] of Object.entries(entry.speciesData)) {
            const d = data as any;
            if (d.totalCount > 0) {
                metrics[speciesId] = d.totalWeight / d.totalCount;
            }
        }
        return {
            batchId: entry.batchId,
            batchName: entry.batchName,
            metrics
        };
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
