<template>
  <v-container fluid>
    <PageHeader title="Daily Operations" />

    <v-card>
      <v-tabs
        v-model="tab"
        color="primary"
        align-tabs="start"
      >
        <v-tab value="feed">Feed Routine</v-tab>
        <v-tab value="sampling">Sampling</v-tab>
        <v-tab value="monitoring">Monitoring</v-tab>
        <v-tab value="feedHistory">Feed History</v-tab>
      </v-tabs>

      <v-window v-model="tab">
        <v-window-item value="feed">
          <v-container fluid>
            <FeedLogForm />
          </v-container>
        </v-window-item>

        <v-window-item value="sampling">
          <v-container fluid>
             <SampleLogForm />
          </v-container>
        </v-window-item>

        <v-window-item value="monitoring">
          <v-container fluid>
             <MonitoringLogForm />
          </v-container>
        </v-window-item>

        <v-window-item value="feedHistory">
          <v-container fluid>
             <FeedLogPivotTable />
          </v-container>
        </v-window-item>
      </v-window>
    </v-card>
  </v-container>
</template>

<script setup lang="ts">
import {ref} from 'vue';
import PageHeader from '../../components/PageHeader.vue';
import FeedLogForm from './components/FeedLogForm.vue';
import SampleLogForm from './components/SampleLogForm.vue';
import MonitoringLogForm from './components/MonitoringLogForm.vue';
import FeedLogPivotTable from './components/FeedLogPivotTable.vue';

const tab = ref('feed');
</script>
