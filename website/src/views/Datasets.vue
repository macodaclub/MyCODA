<script setup>
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from "primevue/button";
import { ref } from 'vue'

// dados exemplo
const datasets = ref([
  { id: 1, name: 'Dataset A', size: '10MB' },
  { id: 2, name: 'Dataset B', size: '25MB' },
  { id: 3, name: 'Dataset C', size: '5MB' }
])
</script>

<template>
  <div class="flex justify-center">
    <div class="flex flex-col max-w-none w-11/12 border rounded-xl bg-surface-0 p-8 m-4 mt-5">
      
      <div class="prose mb-10">
        <h1>Datasets - Exemple</h1>
      </div>

      <DataTable :value="datasets" paginator :rows="5">
        <Column field="id" header="ID" />
        <Column field="name" header="Nome" />
        <Column field="size" header="Tamanho" />

        <Column header="Ações">
          <template #body="slotProps">
            <Button label="Ver" size="small" />
          </template>
        </Column>
      </DataTable>

    </div>
  </div>
</template>