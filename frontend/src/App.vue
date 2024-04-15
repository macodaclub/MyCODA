<script setup>
import TopBar from "@/components/TopBar.vue";
</script>

<template>
  <div id="main-container">
    <TopBar/>
    <router-view/>
  </div>
</template>

<style scoped>
#main-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  align-items: center;
}
</style>
