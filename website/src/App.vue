<script setup>
import Header from "@/components/Header.vue";
import Footer from "@/components/Footer.vue";
import DynamicDialog from "primevue/dynamicdialog";
</script>

<template>
  <div class="h-full flex flex-col">
    <div class="flex-[1_0_auto]">
      <Header/>
      <main>
        <router-view/>
      </main>
    </div>
    <div class="flex-shrink-0">
      <Footer/>
    </div>
  </div>
  <DynamicDialog/>
</template>