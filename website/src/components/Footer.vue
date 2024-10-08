<script setup>
import Button from "primevue/button";
const ghRepoUrl = import.meta.env.GITHUB_REPO_URL;
</script>

<template>
  <footer class="flex flex-col gap-2 items-center bg-neutral-800 py-8 text-white text-center">
    <div class="font-bold text-lg">MyCODA Platform</div>
    <div>
      <router-link :to="{name: 'home'}" tag="div" class="!text-white hover:bg-primary-300">Home</router-link> ·
      <router-link :to="{name: 'about'}" tag="div" class="!text-white hover:bg-primary-300">About</router-link> ·
      <router-link :to="{name: 'browse'}" tag="div" class="!text-white hover:bg-primary-300">Browse</router-link> ·
      <router-link :to="{name: 'contribute'}" tag="div" class="!text-white hover:bg-primary-300">Contribute</router-link>
    </div>
    <div class="flex flex-row gap-3 pt-2">
      <Button as="a" :href="ghRepoUrl" target="_blank" icon="pi pi-github" rounded outlined/>
      <Button as="a" href="mailto: macodaclub@gmail.com" icon="pi pi-envelope" rounded outlined/>
    </div>
  </footer>
</template>

<style scoped>

</style>