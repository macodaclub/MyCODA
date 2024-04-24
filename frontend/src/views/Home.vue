<script setup>

import EntitiesTable from "@/components/EntitiesTable.vue";
import TaxonomyTable from "@/components/TaxonomyTable.vue";
</script>

<template>
  <main>
    <EntitiesTable/>
    <TaxonomyTable/>
  </main>
</template>

<style scoped>
main {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
  width: 100%;
}
</style>