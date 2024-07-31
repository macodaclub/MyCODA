<script setup>

import EntitiesTree from "@/components/EntitiesTree.vue";
</script>

<template>
  <main class="browse-page">
    <EntitiesTree usesRouter />
    <!--    <EntitiesTable :entities="entities"/>
        <TaxonomyTable/>
        <QueryBuilder/>-->
  </main>
</template>

<style scoped>

.browse-page {
  padding: 20px;
}
</style>