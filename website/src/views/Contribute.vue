<script setup>
import Button from "primevue/button";
import {useRouter} from "vue-router";

const router = useRouter();

const ghRepoUrl = import.meta.env.GITHUB_REPO_URL;
</script>

<template>
  <div class="flex justify-center">
    <div class="flex flex-col max-w-none w-11/12 border rounded-xl bg-surface-0 p-8 m-4 mt-5">
      <div class="prose mb-10"><h1>Contribute</h1></div>
      <div class="flex flex-row flex-wrap gap-4">
        <div class="flex grow flex-col basis-64 gap-6 place-content-between rounded-lg bg-neutral-100 p-6">
          <div class="flex flex-col gap-4">
            <div class="font-semibold text-lg">Contribute with an Article</div>
            <div>
              If you've written an article within the domain of Many-Criteria Optimization and Decision Analysis, you
              are welcome to use our tool designed to help enrich the ontology from your article, and make your work
              more accessible to others.
            </div>
            <div>This tool will assist you in contributing to the Knowledge Base by finding any known
              terms referenced in your article's title, abstract, keywords and authors, and providing the relevant
              context.
            </div>
          </div>
          <div>
            <Button icon="pi pi-book"
                    class="rounded-lg bg-gradient-to-br from-primary-400 to-primary-700 text-white border-none px-6 py-3 font-bold hover:ring-2 ring-offset-1 ring-offset-surface-0 ring-gray-600 transition-all"
                    label="Submit an Article"
                    @click="router.push({
    name: 'submissionForm',
  });"/>
          </div>
        </div>
        <div class="flex grow flex-col basis-64 gap-4 place-content-between rounded-lg bg-neutral-100 p-6">
          <div class="flex flex-col gap-4">
            <div class="font-semibold text-lg">Propose changes to the Ontology</div>
            <div>
              If you have any specific changes in mind to enhance the current Knowledge Base, you are welcome to create
              a new Github Issue describing them in detail.
            </div>
            <div>
              After the Issue is submitted, the Knowledge Curators will review the changes proposed and update the
              ontology accordingly.
            </div>
          </div>
          <div>
            <Button
                as="a"
                :href="`${ghRepoUrl}/issues/new/choose`"
                target="_blank"
                icon="pi pi-github"
                class="!text-white rounded-lg bg-gradient-to-br from-blue-950 to-gray-950 border-none px-6 py-3 font-bold hover:ring-2 ring-offset-1 ring-offset-surface-0 ring-gray-600 transition-all"
                label="Create a GitHub Issue"/>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>