<script setup>
import {ref} from "vue";
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Button from 'primevue/button';
import InputText from 'primevue/inputtext';
import AutoComplete from 'primevue/autocomplete';
import Article from "@/components/Article.vue";
import {useOntologyStore} from "@/store";
import {storeToRefs} from "pinia";

defineProps({
  rows: {
    type: Number,
    required: false,
    default: 10
  },
  headerTitle: {
    type: String,
    required: false,
    default: "Browse and Filter the Ontology by Type of Entity"
  },
  entities: {
    type: Array,
    required: true,
  },
});

// TODO: Fix long text cells (e.g. entity details)

const ontologyStore = useOntologyStore();
const {entityTypes} = storeToRefs(ontologyStore);

const filters = ref({
  type: {value: '', matchMode: 'contains'},
  name: {value: '', matchMode: 'contains'},
  details: {value: '', matchMode: 'contains'},
  comment: {value: '', matchMode: 'contains'},
});

const typeSuggestions = ref([]);
const updateTypesAutoComplete = () => {
  typeSuggestions.value = [...entityTypes.value];
};
</script>

<template>
  <Article>
    <template #header>
      {{ headerTitle }}
    </template>
    <DataTable v-model:filters="filters" stripedRows :value="entities" paginator :rows="rows"
               :rowsPerPageOptions="[10, 20, 50]"
               removableSort filterDisplay="row" resizableColumns columnResizeMode="fit" showGridlines
               sortMode="multiple"
               :tableStyle="{width:'100%', tableLayout: 'fixed'}">
      <Column field="type" header="Type" :sortable="true" :showFilterMenu="false"
              :style="{whiteSpace: 'nowrap', width: '15rem'}">
        <template #filter="{ filterModel, filterCallback }">
          <AutoComplete v-model="filterModel.value" :suggestions="typeSuggestions"
                        @complete="updateTypesAutoComplete"
                        @change="filterCallback"
                        :completeOnFocus="true"
                        :autoOptionFocus="true"/>
        </template>
      </Column>
      <Column field="name" header="Name" :sortable="true" :showFilterMenu="false"
              :style="{whiteSpace: 'nowrap', width: '18rem'}">
        <template #filter="{ filterModel, filterCallback }">
          <InputText v-model="filterModel.value" @input="filterCallback"/>
        </template>
      </Column>
      <Column field="details" header="Details" :sortable="true" :showFilterMenu="false"
              :style="{whiteSpace: 'nowrap'}">
        <template #filter="{ filterModel, filterCallback }">
          <InputText v-model="filterModel.value" @input="filterCallback"/>
        </template>
      </Column>
      <Column field="comment" header="Comment" :sortable="true" :showFilterMenu="false"
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