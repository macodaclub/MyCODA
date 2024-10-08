<script setup>

import Showdown from "showdown";
import {computed} from "vue";

const props = defineProps({
  md: {
    type: String,
    required: true
  }
});

const ghRepoUrl = import.meta.env.GITHUB_REPO_URL;

const mdConverter = new Showdown.Converter();
const html = computed(() => mdConverter.makeHtml(props.md).replaceAll("%ghRepoUrl%", ghRepoUrl));

</script>

<template>
  <div class="flex justify-center">
    <div class="prose max-w-none w-11/12 border rounded-xl bg-surface-0 p-8 m-4 mt-5" v-html="html"/>
  </div>
</template>