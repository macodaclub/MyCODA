<script setup>
import {ref, watch} from "vue";
import {delay} from "@/utils/utils/utils.js";

const props = defineProps({
  text: String
});

const copied = ref(false);

const copy = async () => {
  if(copied.value) return;
  copied.value = true;
  await navigator.clipboard.writeText(props.text);
  await delay(3000);
  copied.value = false;
};

watch(() => props.text, () => {
  copied.value = false;
});
</script>

<template>
  <slot :copy="copy" :copied="copied"></slot>
</template>

<style scoped>

</style>