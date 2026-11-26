<template>
  <v-card variant="outlined" class="rounded-lg">
    <v-card-title class="px-6 py-4 bg-slate-50 border-b d-flex align-center">
      <v-icon icon="mdi-map-marker-radius" color="cyan" class="mr-2"></v-icon>
      Pond Utilisation
      <v-spacer></v-spacer>
      <div class="d-flex gap-3 text-caption font-weight-regular">
        <span v-for="s in statusConfig" :key="s.status" class="d-flex align-center gap-1">
          <v-icon :icon="s.icon" :color="s.color" size="x-small"></v-icon>
          {{ s.status }}: {{ countByStatus(s.status) }}
        </span>
      </div>
    </v-card-title>

    <!-- Utilisation bar -->
    <div class="px-6 pt-3 pb-2">
      <div class="d-flex justify-space-between text-caption mb-1">
        <span>Pond Utilisation</span>
        <span class="font-weight-bold">{{ activePonds.length }} / {{ ponds?.length ?? 0 }} active</span>
      </div>
      <v-progress-linear
        :model-value="utilisationPct"
        color="primary"
        height="8"
        rounded
        bg-color="grey-lighten-3"
      ></v-progress-linear>
    </div>

    <!-- Pond Grid -->
    <div class="px-6 pb-6 pt-2">
      <div class="pond-grid">
        <v-tooltip
          v-for="pond in sortedPonds"
          :key="pond.id"
          :text="pondTooltip(pond)"
          location="top"
        >
          <template #activator="{ props: tp }">
            <div
              v-bind="tp"
              class="pond-cell rounded cursor-pointer"
              :class="pondCellClass(pond)"
              @click="$router.push({ name: 'batches' })"
            >
              <v-icon :icon="pondIcon(pond)" size="small" class="mb-1"></v-icon>
              <div class="text-caption font-weight-medium text-truncate" style="max-width:72px">
                {{ pond.name || 'Pond' }}
              </div>
              <div class="text-caption opacity-70">{{ pond.area ? pond.area + ' m²' : '' }}</div>
            </div>
          </template>
        </v-tooltip>
      </div>
    </div>

    <!-- Stats row -->
    <v-divider></v-divider>
    <div class="px-6 py-3 d-flex gap-6 flex-wrap">
      <div class="text-center">
        <div class="text-h6 font-weight-bold text-primary">{{ totalArea.toLocaleString() }} m²</div>
        <div class="text-caption text-grey">Total Area</div>
      </div>
      <div class="text-center">
        <div class="text-h6 font-weight-bold text-success">{{ activeArea.toLocaleString() }} m²</div>
        <div class="text-caption text-grey">Active Area</div>
      </div>
      <div class="text-center">
        <div class="text-h6 font-weight-bold text-grey">{{ freeArea.toLocaleString() }} m²</div>
        <div class="text-caption text-grey">Available Area</div>
      </div>
    </div>
  </v-card>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {useRouter} from 'vue-router';
import {usePonds} from '../../ponds/composables/usePonds';
import {useBatches} from '../../batches/composables/useBatches';
import type {Pond} from '@/api/generated/model';

const router = useRouter();
const { ponds } = usePonds();
const { batches } = useBatches();

const statusConfig = [
  { status: 'Active', color: 'success', icon: 'mdi-waves' },
  { status: 'Available', color: 'info', icon: 'mdi-check-circle' },
  { status: 'Maintenance', color: 'warning', icon: 'mdi-wrench' },
  { status: 'Inactive', color: 'grey', icon: 'mdi-close-circle' },
];

const sortedPonds = computed(() => {
  const order: Record<string, number> = { Active: 0, Available: 1, Maintenance: 2, Inactive: 3 };
  return [...(ponds.value || [])].sort((a, b) =>
    (order[a.status || 'Inactive'] ?? 9) - (order[b.status || 'Inactive'] ?? 9)
  );
});

const activePonds = computed(() => ponds.value?.filter(p => p.status === 'Active') || []);
const utilisationPct = computed(() => {
  const total = ponds.value?.length || 0;
  return total > 0 ? Math.round((activePonds.value.length / total) * 100) : 0;
});

const totalArea = computed(() => (ponds.value || []).reduce((s, p) => s + (p.area || 0), 0));
const activeArea = computed(() => activePonds.value.reduce((s, p) => s + (p.area || 0), 0));
const freeArea = computed(() =>
  (ponds.value || []).filter(p => p.status === 'Available').reduce((s, p) => s + (p.area || 0), 0)
);

function countByStatus(status: string): number {
  return ponds.value?.filter(p => p.status === status).length || 0;
}

function getBatchForPond(pond: Pond) {
  if (!pond.currentBatchId) return null;
  return batches.value?.find(b => b.id === pond.currentBatchId) || null;
}

function pondCellClass(pond: Pond): string {
  switch (pond.status) {
    case 'Active': return 'pond-active';
    case 'Available': return 'pond-available';
    case 'Maintenance': return 'pond-maintenance';
    default: return 'pond-inactive';
  }
}

function pondIcon(pond: Pond): string {
  switch (pond.status) {
    case 'Active': return 'mdi-waves';
    case 'Available': return 'mdi-check-circle-outline';
    case 'Maintenance': return 'mdi-wrench';
    default: return 'mdi-close-circle-outline';
  }
}

function pondTooltip(pond: Pond): string {
  const batch = getBatchForPond(pond);
  const parts = [`${pond.name || 'Pond'} — ${pond.status}`];
  if (pond.area) parts.push(`Area: ${pond.area} m²`);
  if (batch) parts.push(`Batch: ${batch.name || batch.stockingDate}`);
  return parts.join(' · ');
}
</script>

<style scoped>
.pond-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(88px, 1fr));
  gap: 10px;
}
.pond-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 10px 6px;
  min-height: 80px;
  transition: transform 0.15s, box-shadow 0.15s;
  border: 2px solid transparent;
}
.pond-cell:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
}
.pond-active {
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.4);
  color: #16a34a;
}
.pond-available {
  background: rgba(59, 130, 246, 0.08);
  border-color: rgba(59, 130, 246, 0.25);
  color: #2563eb;
}
.pond-maintenance {
  background: rgba(245, 158, 11, 0.1);
  border-color: rgba(245, 158, 11, 0.35);
  color: #d97706;
}
.pond-inactive {
  background: rgba(107, 114, 128, 0.08);
  border-color: rgba(107, 114, 128, 0.2);
  color: #6b7280;
}
.cursor-pointer { cursor: pointer; }
</style>
