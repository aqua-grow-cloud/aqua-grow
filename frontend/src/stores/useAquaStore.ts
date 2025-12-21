import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Pond, Batch, FeedLog, SampleLog, HarvestLog, Expenditure } from '../types';

export const useAquaStore = defineStore('aqua', () => {
    // --- State ---
    const ponds = ref<Pond[]>([
        { id: 'p1', name: 'Pond A1', area: 5000, depth: 1.5, type: 'Earthen', status: 'Active', currentBatchId: 'b1' },
        { id: 'p2', name: 'Pond B2', area: 3000, depth: 1.2, type: 'Lined', status: 'Inactive', currentBatchId: null },
    ]);

    const batches = ref<Batch[]>([
        { id: 'b1', pondId: 'p1', species: 'L. vannamei', stockingDate: '2023-11-01', seedSize: 0.05, initialQuantity: 100000, status: 'Active' },
    ]);

    const feedLogs = ref<FeedLog[]>([]);
    const sampleLogs = ref<SampleLog[]>([]);
    const harvestLogs = ref<HarvestLog[]>([]);
    const expenditures = ref<Expenditure[]>([]);

    // --- Getters ---
    const activePonds = computed(() => ponds.value.filter(p => p.status === 'Active'));
    const getPondById = (id: string) => ponds.value.find(p => p.id === id);
    const getBatchById = (id: string) => batches.value.find(b => b.id === id);
    const getBatchesByPondId = (pondId: string) => batches.value.filter(b => b.pondId === pondId);

    // --- Actions ---
    function addPond(pond: Omit<Pond, 'id'>) {
        const newId = `p${Date.now()}`;
        ponds.value.push({ ...pond, id: newId });
    }

    function updatePond(id: string, updates: Partial<Pond>) {
        const index = ponds.value.findIndex(p => p.id === id);
        if (index !== -1) {
            ponds.value[index] = { ...ponds.value[index], ...updates } as Pond;
        }
    }

    function deletePond(id: string) {
        ponds.value = ponds.value.filter(p => p.id !== id);
    }

    function addBatch(batch: Omit<Batch, 'id'>) {
        const newId = `b${Date.now()}`;
        const newBatch = { ...batch, id: newId };
        batches.value.push(newBatch);

        // Automatically link to pond if not already linked
        if (batch.pondId) {
            updatePond(batch.pondId, { currentBatchId: newId, status: 'Active' });
        }
    }

    function logFeed(log: Omit<FeedLog, 'id'>) {
        feedLogs.value.push({ ...log, id: `f${Date.now()}` });
    }

    function logSample(log: Omit<SampleLog, 'id'>) {
        sampleLogs.value.push({ ...log, id: `s${Date.now()}` });
    }

    function recordHarvest(log: Omit<HarvestLog, 'id'>) {
        harvestLogs.value.push({ ...log, id: `h${Date.now()}` });

        // Update batch status
        const batch = getBatchById(log.batchId);
        if (batch) {
            batch.status = 'Harvested';
            updatePond(batch.pondId, { currentBatchId: null, status: 'Inactive' }); // Set pond to inactive after harvest
        }
    }

    return {
        ponds,
        batches,
        feedLogs,
        sampleLogs,
        harvestLogs,
        expenditures,
        activePonds,
        getPondById,
        getBatchById,
        getBatchesByPondId,
        addPond,
        updatePond,
        deletePond,
        addBatch,
        logFeed,
        logSample,
        recordHarvest,
    };
});
