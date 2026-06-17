<script setup>
import { ref } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import InputText from 'primevue/inputtext'
import Dialog from 'primevue/dialog'

const rows = ref([
  {
    id: 1,
    cue: 'EMO Dataset 1',
  },
  {
    id: 2,
    cue: 'EMO Dataset 2',
  },
])

const showDialog = ref(false)
const newCue = ref('')

function openDialog() {
  newCue.value = ''
  showDialog.value = true
}

function saveCue() {
  if (!newCue.value.trim()) return

  rows.value.push({
    id: Date.now(),
    cue: newCue.value.trim(),
  })

  showDialog.value = false
}
</script>

<template>
  <div class="p-6">
    <div class="flex justify-between items-center mb-4">
      <h1 class="text-3xl font-bold">
        Query
      </h1>

      <Button
        label="New query"
        icon="pi pi-plus"
        @click="openDialog"
      />
    </div>

    <DataTable
      :value="rows"
      class="w-full"
      responsive-layout="scroll"
    >
      <Column field="id" header="ID" />
      <Column field="cue" header="CUE" />
    </DataTable>

    <Dialog
      v-model:visible="showDialog"
      modal
      header="Criar CUE"
      :style="{ width: '30rem' }"
    >
      <div class="flex flex-col gap-3">
        <label for="cue" class="font-semibold">
          Nome da CUE
        </label>

        <InputText
          id="cue"
          v-model="newCue"
          placeholder="Introduza o nome da CUE"
          class="w-full"
        />
      </div>

      <template #footer>
        <Button
          label="Cancelar"
          severity="secondary"
          outlined
          @click="showDialog = false"
        />

        <Button
          label="Guardar"
          icon="pi pi-check"
          @click="saveCue"
        />
      </template>
    </Dialog>
  </div>
</template>