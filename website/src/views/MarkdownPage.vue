<script setup>

import Showdown from "showdown";
import {computed} from "vue";

const props = defineProps({
  md: {
    type: String,
    required: true
  }
});

const mdConverter = new Showdown.Converter();
const html = computed(() => mdConverter.makeHtml(props.md));

</script>

<template>
  <div class="md-content" v-html="html"/>
</template>

<style scoped>
.md-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 16px;
}
</style>