<script setup>

</script>

<template>
  <div class="card">
    <h2>
      <slot name="header"/>
    </h2>
    <div class="content">
      <slot/>
    </div>
  </div>
</template>

<style scoped>
div.card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-radius: 10px;
  background-color: var(--color-content-background);
  border: 1px solid #e2e8f0;

  .content {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
}
</style>