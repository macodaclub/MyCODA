<script setup>
import TreeTable from 'primevue/treetable';
import Column from 'primevue/column';
import Button from 'primevue/button';
import InputText from "primevue/inputtext";
import Article from "@/components/Article.vue";
import {useOntologyStore} from "@/store";
import {storeToRefs} from "pinia";
import {ref} from "vue";

defineProps({
  rows: {
    type: Number,
    required: false,
    default: 10
  }
});

const ontologyStore = useOntologyStore();
const {taxonomy} = storeToRefs(ontologyStore);

const filters = ref({
  name: '',
  axioms: '',
  comment: '',
});
</script>

<template>
  <Article>
    <template #header>
      Browse the Ontology Taxonomy
    </template>
    <TreeTable :filters="filters" filterMode="lenient" :value="taxonomy" paginator :rows="rows"
               :rowsPerPageOptions="[10, 20, 50]"
               removableSort resizableColumns columnResizeMode="fit" filterDisplay="row" showGridlines
               sortMode="multiple"
               :tableStyle="{width:'100%', tableLayout: 'fixed'}">
      <Column field="name" header="Classes Hierarchy (Taxonomy)" :sortable="true" expander :showFilterMenu="false"
              :style="{whiteSpace: 'nowrap'}">
        <template #filter>
          <InputText v-model="filters['name']" class="p-column-filter p-fluid p-column-filter-row"/>
        </template>
      </Column>
      <Column field="axioms" header="Axioms" :sortable="true" :showFilterMenu="false" :style="{whiteSpace: 'nowrap'}">
        <template #filter>
          <InputText v-model="filters['axioms']" class="p-column-filter p-fluid p-column-filter-row"/>
        </template>
      </Column>
      <Column field="comment" header="Comment" :sortable="true" :showFilterMenu="false" :style="{whiteSpace: 'nowrap'}">
        <template #filter>
          <InputText v-model="filters['comment']" class="p-column-filter p-fluid p-column-filter-row"/>
        </template>
      </Column>
      <template #paginatorstart>
        <Button icon="pi pi-refresh" text rounded/>
      </template>
      <template #paginatorend>
        <Button icon="pi pi-download" text rounded/>
      </template>
    </TreeTable>
  </Article>
</template>

<style scoped>

</style>