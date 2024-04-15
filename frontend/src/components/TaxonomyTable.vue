<script setup>
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Button from 'primevue/button';
import {useOntologyStore} from "@/store/store.js";
import {storeToRefs} from "pinia";
import Article from "@/components/Article.vue";

defineProps({
  rows: {
    type: Number,
    required: false,
    default: 10
  }
});

const ontologyStore = useOntologyStore();
const {taxonomy} = storeToRefs(ontologyStore);
</script>

<template>
  <Article>
    <template #header>
      Browse the Ontology Taxonomy
    </template>
    <DataTable :value="taxonomy" paginator :rows="rows" :rowsPerPageOptions="[10, 20, 50]"
               removableSort resizableColumns columnResizeMode="fit" showGridlines
               :tableStyle="{width:'100%', tableLayout: 'fixed'}">
      <Column field="name" header="Classes Hierarchy (Taxonomy)" sortable/>
      <Column field="axioms" header="Axioms" sortable/>
      <Column field="comment" header="Comment" sortable/>
      <template #paginatorstart>
        <Button icon="pi pi-refresh" text rounded/>
      </template>
      <template #paginatorend>
        <Button icon="pi pi-download" text rounded/>
      </template>
    </DataTable>
  </Article>
</template>

<style scoped>

</style>