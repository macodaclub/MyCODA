<script setup>

</script>

<template>
  <article>
    <h2><slot name="header"/></h2>
    <slot />
  </article>
</template>

<style scoped>
article {
  margin: 20px;
  padding: 16px 24px;
  border-radius: 16px;
  background-color: var(--color-content-background);
  box-shadow: 0 20px 27px 0 rgba(0,0,0,.05);;
}
</style>