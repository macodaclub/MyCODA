<script setup>
import Header from "@/components/Header.vue";
import DynamicDialog from "primevue/dynamicdialog";
</script>

<template>
  <Header/>
  <main>
    <router-view/>
  </main>
  <DynamicDialog />
</template>