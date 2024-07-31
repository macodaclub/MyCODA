<script setup>
import InputText from 'primevue/inputtext';
import Textarea from 'primevue/textarea';
import FloatLabel from 'primevue/floatlabel';
import Button from 'primevue/button';
import Stepper from 'primevue/stepper';
import StepList from 'primevue/steplist';
import StepPanels from 'primevue/steppanels';
import Step from 'primevue/step';
import StepPanel from 'primevue/steppanel';
import Dialog from 'primevue/dialog';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Badge from 'primevue/badge';
import Select from 'primevue/select';
import {useDebounceFn} from "@vueuse/core"
import {useRouter} from 'vue-router'
import {ref} from "vue";
import EntitiesTree from "@/components/EntitiesTree.vue";

const router = useRouter();

const activeStep = ref(1);

const titleInput = ref("The NSGA-II algorithm, a Diversity VS. Convergence based algorithm – subclass of MOEA –, has implementation language Java.");
const abstractInput = ref("The NSGA-II algorithm, a Diversity VS. Convergence based algorithm – subclass of Multi-Objective Evolutionary Algorithm –, has implementation language Java, and the author was Jorge Navarro.");
const keywordsInput = ref("NSGA-II, Java, MOEA");

const titleSegments = ref(null);
const abstractSegments = ref(null);
const keywordSegments = ref(null);
const referencedEntities = ref(null);

const isEntityPreviewVisible = ref(false);
const entityPreviewTree = ref(null);
const previewingEntity = ref(null);

const entityChanges = ref([]);

const isNewEntityDialogVisible = ref(false);
const newEntityLabelInput = ref("");
const newEntityLabelInputDebounced = ref("");
const newEntityLabelInputDebounceFn = useDebounceFn(() => {
  newEntityLabelInputDebounced.value = newEntityLabelInput.value
}, 500);
const selectedNewEntityType = ref(null);
const newEntityCommentInput = ref("");
const newClassSubClassOfInput = ref("");
const newIndividualTypeInput = ref("");
const entityTypeOptions = [
  {
    "value": "Class",
    "description": "A category of things.",
  },
  {
    "value": "Individual",
    "description": "An instance of a certain class.",
  },
  {
    "value": "Property",
    "description": "An attribute, or characteristic of something.",
  },
];

const browseRoute = router.resolve({name: 'browse'});

const onSubmitForm = async (stepperActivateCallback) => {
  const url = "http://127.0.0.1:8081/api/submission/submitForm";
  const formData = new FormData();
  formData.append("title", titleInput.value);
  formData.append("abstract", abstractInput.value);
  formData.append("keywords", keywordsInput.value);
  const response = await fetch(url, {
    method: "POST",
    body: formData,
  });
  if (!response.ok) return console.error('Failed to fetch ontology info', response);
  const responseData = await response.json()
  titleSegments.value = responseData.titleSegments;
  abstractSegments.value = responseData.abstractSegments;
  keywordSegments.value = responseData.keywordSegments;
  referencedEntities.value = responseData.referencedEntities;
  entityChanges.value = [...referencedEntities.value.map(it => ({
    context: ["Referenced"],
    iri: it.iri,
    label: it.label,
    type: it.type,
  }))];
  stepperActivateCallback(2);
}

const previewEntity = async (segment) => {
  const referencedEntity = referencedEntities.value[segment.entityReferenceIndex];
  previewingEntity.value = {iri: referencedEntity.iri, type: referencedEntity.type};
  isEntityPreviewVisible.value = true;
}

const getSeverityForContext = context => ({
  "Referenced": "secondary",
  "New": "success",
  "Edited": "warn",
})[context];
</script>

<template>
  <main class="article-submission-page">
    <div class="content-card">
      <Stepper v-model:value="activeStep" linear pt:root:class="mx-4 mb-0 mt-6">
        <StepList>
          <Step :value="1">Submit Article</Step>
          <Step :value="2">Identify Terms</Step>
          <Step :value="3">Review Contributions</Step>
        </StepList>
        <StepPanels pt:root:class="mt-2">
          <StepPanel v-slot="{ activateCallback }" :value="1">
            <form class="flex flex-col gap-6" @submit.prevent="onSubmitForm(activateCallback)">
              <div class="flex flex-col gap-6">
                <h2 class="text-xl font-bold mb-2">Article Submission Form</h2>
                <FloatLabel>
                  <InputText fluid id="titleInput" v-model="titleInput"/>
                  <label for="titleInput">Title</label>
                </FloatLabel>
                <FloatLabel pt:root:class="mt-2">
                  <Textarea fluid id="abstractInput" v-model="abstractInput" rows="5" cols="30"/>
                  <label for="abstractInput">Abstract</label>
                </FloatLabel>
                <FloatLabel>
                  <InputText fluid id="keywordsInput" v-model="keywordsInput"/>
                  <label for="keywordsInput">Keywords</label>
                </FloatLabel>
              </div>
              <div class="flex flex-col items-start">
                <Button type="submit" label="Submit Article"/>
              </div>
            </form>
          </StepPanel>
          <StepPanel v-slot="{ activateCallback }" :value="2">
            <div class="flex flex-col gap-8">
              <div>
                <h3 class="text-lg font-semibold mb-4">Submitted Article</h3>
                <div class="flex flex-col gap-2 border bg-surface-50 p-2">
                  <div class="border bg-surface-0 p-4">
                    <h3 class="text-lg font-semibold mb-2">Title</h3>
                    <template v-for="segment in titleSegments">
                      <span v-if="segment.entityReferenceIndex === null">{{ segment.text }}</span>
                      <div v-else style="display: inline-block">
                        <a :href="referencedEntities[segment.entityReferenceIndex].iri"
                           @click.prevent="previewEntity(segment)">{{ segment.text }}</a>
                      </div>
                    </template>
                  </div>
                  <div class="border bg-surface-0 p-4">
                    <h3 class="text-lg font-semibold mb-2">Abstract</h3>
                    <template v-for="segment in abstractSegments">
                      <span v-if="segment.entityReferenceIndex === null">{{ segment.text }}</span>
                      <a v-else :href="referencedEntities[segment.entityReferenceIndex].iri"
                         @click.prevent="previewEntity(segment)">{{ segment.text }}</a>
                    </template>
                  </div>
                  <div class="border bg-surface-0 p-4">
                    <h3 class="text-lg font-semibold mb-2">Keywords</h3>
                    <template v-for="segment in keywordSegments">
                      <span v-if="segment.entityReferenceIndex === null">{{ segment.text }}</span>
                      <a v-else :href="referencedEntities[segment.entityReferenceIndex].iri"
                         @click.prevent="previewEntity(segment)">{{ segment.text }}</a>
                    </template>
                  </div>
                </div>
              </div>
              <div>
                <h3 class="text-lg font-semibold mb-4">Identified Terms</h3>
                <DataTable :value="entityChanges" editMode="cell" showGridlines>
                  <Column field="context" header="Context" style="width: 4rem;">
                    <template #body="{ data, field }">
                      <div class="flex flex-row gap-2">
                        <Badge v-for="context in data[field]" :value="context"
                               :severity="getSeverityForContext(context)"/>
                      </div>
                    </template>
                  </Column>
                  <Column field="type" header="Type" style="width: 4rem;"/>
                  <Column field="label" header="Term"/>
                </DataTable>
                <Button class="mt-4" @click="isNewEntityDialogVisible = true" label="Add a new term"
                        icon="pi pi-plus-circle" size="small"/>
              </div>
            </div>
            <Dialog v-model:visible="isEntityPreviewVisible" modal header="Entity Preview" maximizable dismissableMask
                    pt:root:class="w-11/12 bg-[#f8fafc]">
              <EntitiesTree ref="entityPreviewTree" :initialSelectedIri="previewingEntity.iri"
                            :initialSelectedEntityType="previewingEntity.type"/>
            </Dialog>
            <Dialog v-model:visible="isNewEntityDialogVisible" modal header="Add a new term">
              <div class="flex flex-col gap-6">
                <div class="flex flex-col gap-2">
                  <label for="newEntityLabelInput">What term would you like to introduce?</label>
                  <InputText v-model="newEntityLabelInput"
                             @input="newEntityLabelInputDebounceFn"
                             id="newEntityLabelInput"
                             fluid
                             placeholder="New term..."/>
                </div>
                <div v-if="newEntityLabelInputDebounced"
                     class="flex flex-col gap-3 border rounded-border border-primary relative px-4 pt-2 pb-3 text-sm">
                  <h4 class="absolute top-[-0.55rem] start-[0.5rem] text-xs bg-surface-0 px-2 text-primary">
                    Suggestion</h4>
                  <a href=""><i
                      class="pi pi-times-circle text-sm absolute top-[-0.4rem] right-2 text-primary bg-surface-0 px-1"/></a>
                  <div>Is <span class="font-semibold">{{ newEntityLabelInputDebounced }}</span> a synonym of an
                    existing term?
                  </div>
                  <div class="flex flex-row gap-2">
                    <Button label="KUR" severity="secondary" outlined rounded size="small"/>
                    <Button label="KneePoint" severity="secondary" outlined rounded size="small"/>
                    <Button label="Knapsack" severity="secondary" outlined rounded size="small"/>
                    <Button label="Other…" severity="secondary" outlined rounded size="small"/>
                  </div>
                  <p>Verify: <a :href="browseRoute.href" target="_blank" class=""><i
                      class="pi pi-external-link text-sm ml-1 me-2"/>Browse
                    knowledge
                    base</a>
                  </p>
                </div>
                <div v-if="newEntityLabelInputDebounced" class="flex flex-col gap-2">
                  <label for="selectedNewEntityType">Which type is the new term?</label>
                  <Select v-model="selectedNewEntityType" id="selectedNewEntityType" :options="entityTypeOptions"
                          optionLabel="value"
                          placeholder="Select type…">
                    <template #option="{option}">
                      <div class="flex flex-col">
                        <p class="font-medium">{{ option.value }}</p>
                        <p class="text-sm">{{ option.description }}</p>
                      </div>
                    </template>
                  </Select>
                </div>
                <div v-if="selectedNewEntityType !== null && selectedNewEntityType.value === 'Class'"
                     class="flex flex-col gap-2">
                  <div class="flex flex-col gap-2">
                    <label for="newEntitySubClassOfInput">Does this class have a super class?</label>
                    <InputText v-model="newClassSubClassOfInput" id="newEntitySubClassOfInput" fluid
                               placeholder="No."/>
                  </div>
                  <div class="flex flex-col gap-2">
                    <label for="newEntityCommentInput">How would you describe the new term?</label>
                    <Textarea fluid v-model="newEntityCommentInput" id="newEntityCommentInput" rows="5"
                              placeholder="Describe the term…"/>
                  </div>
                </div>
                <template v-if="selectedNewEntityType !== null && selectedNewEntityType.value === 'Individual'">
                  <div class="flex flex-col gap-2">
                    <label for="newEntitySubClassOfInput">Which class does this individual belong to?</label>
                    <InputText v-model="newIndividualTypeInput" id="newIndividualTypeInput" fluid
                               placeholder="Select a class…"/>
                  </div>
                  <div class="flex flex-col gap-2">
                    <label for="newEntityCommentInput">How would you describe the new term?</label>
                    <Textarea fluid v-model="newEntityCommentInput" id="newEntityCommentInput" rows="5"
                              placeholder="Describe the term…"/>
                  </div>
                  <div class="flex flex-col gap-2">
                    <div>Properties</div>

                  </div>
                </template>
                <div class="flex flex-row justify-end gap-2">
                  <Button label="Cancel" severity="secondary" outlined size="small"
                          @click="isNewEntityDialogVisible = false"/>
                  <Button label="Add term" :disabled="!selectedNewEntityType" size="small"/>
                </div>
              </div>
            </Dialog>
          </StepPanel>
        </StepPanels>
      </Stepper>
    </div>
  </main>
</template>

<style scoped>
.article-submission-page {
  color: var(--color-text);
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;

  gap: 20px;
  padding: 20px;

  > .content-card {
    border-radius: 10px;
    border: 1px solid #e2e8f0;
    background-color: #ffffff;
  }
}
</style>