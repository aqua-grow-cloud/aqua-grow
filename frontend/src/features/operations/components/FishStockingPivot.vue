<template>
  <v-card variant="outlined" class="rounded-lg">
    <v-card-title class="px-6 py-4 d-flex align-center">
      <v-icon icon="mdi-table-pivot" class="mr-2" color="primary"></v-icon>
      Fish Stocking Pivot
    </v-card-title>
    <v-divider></v-divider>
    
    <div class="table-container">
      <table class="pivot-table">
        <thead>
          <tr>
            <th>Status</th>
            <th>Batch</th>
            <th>Type</th>
            <th class="text-right">SUM of Quantity</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="statusGroup in groupedData" :key="statusGroup.status">
            <!-- Status Group Rows -->
            <template v-for="(batch, batchIndex) in statusGroup.batches" :key="batch.id">
              <template v-for="(type, typeIndex) in batch.types" :key="type.name">
                <tr>
                  <!-- Status Column: Only show on first row of the group -->
                  <td v-if="batchIndex === 0 && typeIndex === 0" :rowspan="statusGroup.totalRows">
                    {{ statusGroup.status }}
                  </td>
                  
                  <!-- Batch Column: Only show on first row of the batch -->
                  <td v-if="typeIndex === 0" :rowspan="batch.types.length + 1">
                    {{ formatDate(batch.stockingDate) }}
                     <div class="text-caption text-grey">{{ batch.name }}</div>
                  </td>
                  
                  <!-- Type and Quantity -->
                  <td>{{ type.name }}</td>
                  <td class="text-right">{{ type.quantity.toLocaleString() }}</td>
                </tr>
              </template>
              
              <!-- Batch Subtotal Row -->
              <tr class="font-weight-bold bg-grey-lighten-4">
               <!-- Batch date is rowspanned, so we just need empty here or specific layout -->
               <!-- Actually, user requested: "2024-07-11 Total" in the Batch column? 
                    Let's look at the example:
                    Batch column has "2024-07-11 Total" aligned with empty Type column.
               -->
                <td colspan="2" class="text-right">{{ formatDate(batch.stockingDate) }} Total</td>
                <td class="text-right">{{ batch.totalQuantity.toLocaleString() }}</td>
              </tr>
            </template>
            
            <!-- Status Subtotal Row -->
            <tr class="font-weight-bold bg-grey-lighten-3">
              <td colspan="3">{{ statusGroup.status }} Total</td>
              <td class="text-right">{{ statusGroup.totalQuantity.toLocaleString() }}</td>
            </tr>
          </template>
          
          <!-- Grand Total -->
          <tr class="font-weight-black bg-grey-lighten-2 text-h6">
            <td colspan="3">Grand Total</td>
            <td class="text-right">{{ grandTotal.toLocaleString() }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { Batch } from '../../../api/generated/model';

const props = defineProps<{
  batches: Batch[];
}>();

// Helper interface for processing
interface FishTypeSummary {
  name: string;
  quantity: number;
}

interface ProcessedBatch {
  id: string;
  name: string;
  stockingDate?: string;
  types: FishTypeSummary[];
  totalQuantity: number;
  // totalRows includes the data rows + 1 subtotal row
  totalRows: number; 
}

interface StatusGroup {
  status: string;
  batches: ProcessedBatch[];
  totalQuantity: number;
  // totalRows includes all batch rows (inc subtotals) + 1 status subtotal row
  totalRows: number;
}

const groupedData = computed<StatusGroup[]>(() => {
  if (!props.batches) return [];

  // Flatten all seeds into a structure we can group
  interface FlatSeed {
      status: string;
      batchId: string;
      batchName: string;
      stockingDate?: string;
      type: string;
      quantity: number;
  }

  const allSeeds: FlatSeed[] = [];
  props.batches.forEach(b => {
      if (b.fishSeeds) {
          b.fishSeeds.forEach(seed => {
              allSeeds.push({
                  status: seed.status || (b.status === 'Active' ? 'Active' : 'Completed'), // Fallback if seed status missing
                  batchId: b.id || '',
                  batchName: b.name || '',
                  stockingDate: b.stockingDate,
                  type: seed.type || 'Unknown',
                  quantity: seed.quantity || 0
              });
          });
      }
  });

  const groups: Record<string, FlatSeed[]> = {};
  
  // 1. Group by Status
  allSeeds.forEach(seed => {
    const status = seed.status || 'Active'; // Default to active if unknown
    if (!groups[status]) groups[status] = [];
    groups[status].push(seed);
  });

  const result: StatusGroup[] = [];

  // 2. Process each status group
  Object.keys(groups).sort().forEach(status => {
    const seedsInStatus = groups[status];
    if (!seedsInStatus) return; // FIX: Ensure seedsInStatus exists
    
    // Group by Batch within this status
    const batchMap: Record<string, FlatSeed[]> = {};
    seedsInStatus.forEach(s => {
        if (!batchMap[s.batchId]) batchMap[s.batchId] = [];
        const batchSeeds = batchMap[s.batchId];
        if (batchSeeds) { // FIX: Ensure batchSeeds exists array wise before push, although it should
             batchSeeds.push(s);
        }
    });

    // Convert batchMap to ProcessedBatch array
    const processedBatches: ProcessedBatch[] = Object.keys(batchMap).reduce((acc: ProcessedBatch[], batchId) => {
        const seeds = batchMap[batchId];
        if (!seeds || seeds.length === 0) return acc;
        
        const representative = seeds[0];
        if (!representative) return acc; // FIX: Double check representative exists
        
        // Aggregate by type
        const typeMap: Record<string, number> = {};
        seeds.forEach(s => {
            typeMap[s.type] = (typeMap[s.type] || 0) + s.quantity;
        });

        const types: FishTypeSummary[] = Object.keys(typeMap).sort().map(typeName => ({
            name: typeName,
            quantity: typeMap[typeName] || 0
        }));
        
        const batchTotal = types.reduce((sum, t) => sum + t.quantity, 0);

        acc.push({
            id: batchId,
            name: representative.batchName,
            stockingDate: representative.stockingDate,
            types,
            totalQuantity: batchTotal,
            totalRows: types.length + 1 // +1 for batch subtotal
        });
        
        return acc;
    }, []);

    // Sort processed batches by date
    processedBatches.sort((a, b) => 
        new Date(a.stockingDate || '').getTime() - new Date(b.stockingDate || '').getTime()
    );

    if (processedBatches.length > 0) {
        const groupTotal = processedBatches.reduce((sum, b) => sum + b.totalQuantity, 0);
        const groupTotalRows = processedBatches.reduce((sum, b) => sum + b.totalRows, 0) + 1; // +1 for status total

        result.push({
            status,
            batches: processedBatches,
            totalQuantity: groupTotal,
            totalRows: groupTotalRows
        });
    }
  });

  return result;
});

const grandTotal = computed(() => {
    return groupedData.value.reduce((sum, g) => sum + g.totalQuantity, 0);
});

function formatDate(dateStr?: string) {
  if (!dateStr) return '-';
  // Use simple ISO date like the user example "2024-07-11"
  return dateStr.substring(0, 10);
}
</script>

<style scoped>
.table-container {
    overflow-x: auto;
}
.pivot-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.875rem;
}

.pivot-table th {
    background-color: #f8fafc; /* bg-slate-50 */
    padding: 12px 16px;
    text-align: left;
    font-weight: 600;
    border-bottom: 2px solid #e2e8f0;
}

.pivot-table td {
    padding: 8px 16px;
    border: 1px solid #e2e8f0;
    vertical-align: top;
}

.pivot-table tr:hover td {
    background-color: #f8fafc;
}
</style>
