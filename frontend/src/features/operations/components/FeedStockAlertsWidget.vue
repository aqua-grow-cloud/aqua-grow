<template>
  <v-card variant="outlined" class="rounded-lg h-100">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b d-flex align-center">
      <v-icon icon="mdi-alert-circle" color="warning" class="mr-2"></v-icon>
      Feed Stock Alerts
      <v-badge v-if="lowItems.length > 0" :content="lowItems.length" color="error" inline class="ml-2"></v-badge>
    </v-card-title>

    <div v-if="isLoading" class="d-flex justify-center py-8">
      <v-progress-circular indeterminate color="warning"></v-progress-circular>
    </div>

    <template v-else-if="lowItems.length === 0">
      <v-card-text class="text-center py-10">
        <v-icon icon="mdi-check-circle" size="48" color="success" class="mb-2"></v-icon>
        <div class="text-success font-weight-medium">All feed stocks are above threshold</div>
      </v-card-text>
    </template>

    <template v-else>
      <v-list density="compact" class="py-0">
        <v-list-item
          v-for="item in lowItems"
          :key="item.id"
          class="border-b"
        >
          <template #prepend>
            <v-icon
              :icon="stockLevel(item) === 'critical' ? 'mdi-alert' : 'mdi-alert-outline'"
              :color="stockLevel(item) === 'critical' ? 'error' : 'warning'"
            ></v-icon>
          </template>
          <v-list-item-title class="font-weight-medium">{{ item.feedType }}</v-list-item-title>
          <v-list-item-subtitle class="text-caption">
            Stock: <strong>{{ item.currentStock }} {{ item.unit || 'kg' }}</strong>
            &nbsp;·&nbsp; Threshold: {{ item.threshold }} {{ item.unit || 'kg' }}
          </v-list-item-subtitle>
          <template #append>
            <v-chip
              :color="stockLevel(item) === 'critical' ? 'error' : 'warning'"
              size="x-small"
              variant="flat"
            >
              {{ stockLevel(item) === 'critical' ? 'Critical' : 'Low' }}
            </v-chip>
          </template>
        </v-list-item>
      </v-list>

      <!-- All stock summary -->
      <v-divider></v-divider>
      <div class="px-6 py-3">
        <div class="text-caption text-grey mb-2">All Feed Stock</div>
        <v-row dense>
          <v-col v-for="item in allItems" :key="item.id" cols="12">
            <div class="d-flex align-center justify-space-between mb-1">
              <span class="text-caption font-weight-medium">{{ item.feedType }}</span>
              <span class="text-caption">{{ item.currentStock }} / {{ item.threshold }} {{ item.unit || 'kg' }}</span>
            </div>
            <v-progress-linear
              :model-value="stockPct(item)"
              :color="stockPct(item) < 50 ? (stockPct(item) < 25 ? 'error' : 'warning') : 'success'"
              height="6"
              rounded
              bg-color="grey-lighten-3"
            ></v-progress-linear>
          </v-col>
        </v-row>
      </div>
    </template>
  </v-card>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {feedStockService} from '../../inventory/api/feedStockService';

const { data: allStockItems, isLoading } = feedStockService.useGetAll();
const { data: lowStockItems } = feedStockService.useGetLowStock();

type FeedStockItem = { id?: string; feedType?: string; currentStock?: number; threshold?: number; unit?: string };

const lowItems = computed<FeedStockItem[]>(() => lowStockItems.value || []);
const allItems = computed<FeedStockItem[]>(() => allStockItems.value || []);

function stockLevel(item: FeedStockItem): 'critical' | 'low' {
  const pct = stockPct(item);
  return pct < 25 ? 'critical' : 'low';
}

function stockPct(item: FeedStockItem): number {
  const cur = item.currentStock ?? 0;
  const thr = item.threshold ?? 1;
  return Math.min(100, Math.round((cur / (thr * 2)) * 100));
}
</script>
