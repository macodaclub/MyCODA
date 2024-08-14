<script setup>
import headerBgUrl from '@/assets/img/headerBg.png'
import AutoComplete from "primevue/autocomplete";
import InputIcon from "primevue/inputicon";
import IconField from "primevue/iconfield";
import {ref, watch} from "vue";
import {useRouter} from "vue-router";
import {useOntologyStore} from "@/store/index.js";

const ontologyStore = useOntologyStore();
const {fetchSearchEntities} = ontologyStore;

const router = useRouter();

const searchEntityInput = ref(null);
const searchEntityAutoCompleteOptions = ref([]);

const autoCompleteSearchEntity = async (event) => {
  searchEntityAutoCompleteOptions.value = await fetchSearchEntities(event.query);
};

watch(searchEntityInput, (to) => {
  if (to && to.iri) {
    onSearchTerm(to);
    searchEntityInput.value = null;
  }
});

const onSearchTerm = entity => {
  router.push({
    name: 'browse',
    query: {iri: entity.iri, type: entity.type}
  });
}
</script>

<template>
  <header
      :style="{backgroundImage: `linear-gradient(to right, rgba(24, 9, 2, 0.9), rgba(24, 9, 2, 0.5)), linear-gradient(to top, rgba(24, 9, 2, 0.8), rgba(24, 9, 2, 0.5)),  url(${headerBgUrl})`}">
    <div class="title-container-wrapper mb-2">
      <div class="title-container flex flex-col gap-1">
        <h1 class="title text-primary font-extrabold text-4xl mt-2">MyCODA</h1>
        <h4 class="heading text-surface-0 font-bold text-md">Many Criteria Optimization and Decision Analysis (MaCODA)
          Platform</h4>
      </div>
    </div>
    <div class="flex flex-row justify-between flex-wrap-reverse max-w-[1300px] mx-auto my-0 gap-6">
      <div class="nav-bar">
        <router-link :to="{name: 'home'}" tag="div" class="nav-btn">Home</router-link>
        <router-link :to="{name: 'education'}" tag="div" class="nav-btn">Education</router-link>
        <router-link :to="{name: 'events'}" tag="div" class="nav-btn">Events</router-link>
        <router-link :to="{name: 'browse'}" tag="div" class="nav-btn">Browse</router-link>
        <router-link :to="{name: 'submissionForm'}" tag="div" class="nav-btn">Contribute</router-link>
      </div>
      <div class="translate-y-[-8px]">
        <IconField>
          <AutoComplete v-model="searchEntityInput"
                        id="searchEntityInput"
                        forceSelection
                        optionLabel="label"
                        :suggestions="searchEntityAutoCompleteOptions"
                        @complete="e => autoCompleteSearchEntity(e)"
                        placeholder="Search…"/>
          <InputIcon class="pi pi-search"/>
        </IconField>
      </div>
    </div>
  </header>
</template>

<style scoped>
header {

  background-color: #ffffff;
  background-size: cover;
  background-position: center;

  .title-container-wrapper {
    padding: 0 1rem;
  }

  .title-container {
    line-height: 1.5;
    max-width: 1280px;
    margin: 0 auto;
    padding: 1rem 0;

    .title {
      text-shadow: 2px 0 #00000080, -2px 0 #00000080, 0 2px #00000080, 0 -2px #00000080;
    }
  }

  .nav-bar {
    @media (min-width: 600px) {
      display: flex;
      flex-direction: row;
    }

    .nav-btn {
      display: block;
      font-weight: bold;
      transition: 0.4s;
      padding: 0.5rem 1rem;
    }

    .nav-btn {
      display: block;
      font-weight: bold;
      transition: 0.4s;
      padding: 0.5rem 1rem;
      color: #FFFFFF;
    }

    .nav-btn:hover {
      background-color: var(--color-background-mute);
      color: var(--p-primary-color);
    }
  }
}

.router-link-active {
  @apply bg-surface-50 rounded-t-lg;
  color: var(--p-primary-color) !important;
  font-weight: 800 !important;
}
</style>