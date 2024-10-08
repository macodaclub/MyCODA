<script setup>
import InputText from 'primevue/inputtext';
import Button from 'primevue/button';
import {ref} from "vue";
import {useOntologyStore} from "@/store";
import Message from 'primevue/message';

const ontologyStore = useOntologyStore();
const {backendHost} = ontologyStore;

const passwordInput = ref();
const reloadOntologyButtonIcon = ref("pi pi-refresh");
const isWrongPasswordErrorMessageVisible = ref(false);

const reloadOntology = async () => {
  reloadOntologyButtonIcon.value = "pi pi-spin pi-spinner"
  const response = await fetch(`${backendHost}/api/curator/reloadOntology`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Basic ${btoa(`curator:${passwordInput.value}`)}`,
    },
  });
  if (!response.ok) {
    isWrongPasswordErrorMessageVisible.value = true;
    reloadOntologyButtonIcon.value = "pi pi-refresh";
    return console.error('Failed to authenticate', response);
  }
  console.log(response);
  isWrongPasswordErrorMessageVisible.value = false;
  reloadOntologyButtonIcon.value = "pi pi-check"
};
</script>

<template>
  <div class="flex justify-center">
    <div class="flex flex-col max-w-none w-11/12 border rounded-xl bg-surface-0 p-8 m-4 mt-5">
      <div class="prose mb-10"><h1>Curator</h1></div>
      <div class="flex flex-col gap-4">
        <div class="flex flex-row gap-1">
          <InputText type="password" v-model="passwordInput" placeholder="Password"/>
          <Button label="Reload Ontology" :icon="reloadOntologyButtonIcon" @click="reloadOntology()"/>
        </div>
        <Message v-if="isWrongPasswordErrorMessageVisible" severity="error">Wrong curator password!</Message>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>