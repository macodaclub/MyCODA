<script setup>
import {onMounted, ref} from "vue";
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import AutoComplete from 'primevue/autocomplete';
import Article from "@/components/Article.vue";
import {useOntologyStore} from "@/store/store.js";
import {storeToRefs} from "pinia";

defineProps({
  rows: {
    type: Number,
    required: false,
    default: 10
  }
});

const ontologyStore = useOntologyStore();
const {entities, entityTypes} = storeToRefs(ontologyStore)
const {updateEntities} = ontologyStore

const filters = ref({
  type: {value: '', matchMode: 'contains'},
  name: {value: '', matchMode: 'contains'},
  details: {value: '', matchMode: 'contains'},
  comment: {value: '', matchMode: 'contains'},
});

onMounted(() => {
  updateEntities();
});

const typeSuggestions = ref([]);
const updateTypesAutoComplete = event => {
  typeSuggestions.value = entityTypes.value.filter(it => it.toLowerCase().includes(event.query.toLowerCase()));
};
</script>

<template>
  <Article>
    <template #header>
      Browse and Filter the Ontology by Type of Entity
    </template>
    <DataTable v-model:filters="filters" :value="entities" paginator :rows="rows" :rowsPerPageOptions="[10, 20, 50]"
               removableSort filterDisplay="row" resizableColumns columnResizeMode="fit" showGridlines
               :tableStyle="{width:'100%', tableLayout: 'fixed'}">
      <Column field="type" header="Type" sortable :showFilterMenu="false"
              :style="{whiteSpace: 'nowrap', width: '15rem'}">
        <template #filter="{ filterModel, filterCallback }">
          <AutoComplete v-model="filterModel.value" :suggestions="typeSuggestions"
                        @complete="updateTypesAutoComplete"
                        @change="filterCallback"
                        :completeOnFocus="true"
                        :autoOptionFocus="true"/>
        </template>
      </Column>
      <Column field="name" header="Name" sortable :showFilterMenu="false"
              :style="{whiteSpace: 'nowrap', width: '18rem'}">
        <template #filter="{ filterModel, filterCallback }">
          <InputText v-model="filterModel.value" @input="filterCallback"/>
        </template>
      </Column>
      <Column field="details" header="Details" sortable :showFilterMenu="false"
              :style="{whiteSpace: 'nowrap'}">
        <template #filter="{ filterModel, filterCallback }">
          <InputText v-model="filterModel.value" @input="filterCallback"/>
        </template>
      </Column>
      <Column field="comment" header="Comment" sortable :showFilterMenu="false"
              :style="{whiteSpace: 'nowrap'}">
        <template #filter="{ filterModel, filterCallback }">
          <InputText v-model="filterModel.value" @input="filterCallback"/>
        </template>
      </Column>
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