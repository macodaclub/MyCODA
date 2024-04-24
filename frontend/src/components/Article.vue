<script setup>

</script>

<template>
  <div class="card">
    <h2><slot name="header"/></h2>
    <slot />
  </div>
</template>

<style scoped>
div.card {
  margin: 20px;
  padding: 16px;
  border-radius: 10px;
  background-color: var(--color-content-background);
  border: 1px solid #e2e8f0;
}
</style>