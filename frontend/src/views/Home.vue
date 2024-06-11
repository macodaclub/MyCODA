<script setup>

import EntitiesTable from "@/components/EntitiesTable.vue";
import TaxonomyTable from "@/components/TaxonomyTable.vue";
import QueryBuilder from "@/components/QueryBuilder.vue";
import {onMounted} from "vue";
import {useOntologyStore} from "@/store/store.js";
import {storeToRefs} from "pinia";

const ontologyStore = useOntologyStore();
const {entities} = storeToRefs(ontologyStore);
const {fetchEntities} = ontologyStore;

onMounted(async () => {
  await fetchEntities();
});
</script>

<template>
  <main>
    <EntitiesTable :entities="entities"/>
    <TaxonomyTable/>
    <QueryBuilder/>
  </main>
</template>

<style scoped>
main {
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
  padding: 20px;
  width: 100%;
}
</style>