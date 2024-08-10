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
import AutoComplete from 'primevue/autocomplete';
import {useDebounceFn} from "@vueuse/core"
import {useRouter} from 'vue-router'
import {computed, ref, watch} from "vue";
import EntitiesTree from "@/components/EntitiesTree.vue";
import {useOntologyStore} from "@/store";

const ontologyStore = useOntologyStore();
const {backendHost, fetchSearchEntities, fetchIndividualProperties} = ontologyStore;

const router = useRouter();

const activeStep = ref(1);

const titleInput = ref("The p NSGA-II algorithm, a preference based multi-objective evolutionary algorithm – subclass of MOEA –, has implementation language Java and C#.");
const abstractInput = ref("The p NSGA-II algorithm, a preference based multi-objective evolutionary algorithm – subclass of multi-objective evolutionary algorithm –, has implementation language Java and C#, and one of the authors was Pradyumn Kumar Shukla.");
const keywordsInput = ref("p NSGA-II, Java, MOEA, C#");

const titleSegments = ref(null);
const abstractSegments = ref(null);
const keywordSegments = ref(null);
const referencedEntities = ref(null);

const isEntityPreviewVisible = ref(false);
const entityPreviewTree = ref(null);
const previewingEntity = ref(null);

const edits = ref([]);
const addedEntities = ref([]);
const identifiedTerms = computed(() => {
  let result = [...referencedEntities.value.map(it => ({
    context: ["Referenced"],
    iri: it.iri,
    label: it.label,
    type: it.type,
  }))];

  result = result.concat([...addedEntities.value.map(it => ({
    context: ["Added"],
    label: it.label,
    type: it.type
  }))]);

  return result;
})

const isNewEntityDialogVisible = ref(false);
const newEntityLabelInput = ref("");
const newEntityLabelInputDebounced = ref("");
const newEntityLabelInputDebounceFn = useDebounceFn(() => {
  newEntityLabelInputDebounced.value = newEntityLabelInput.value
}, 500);
const selectedNewEntityType = ref(null);
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
const newEntityCommentInput = ref("");
const newClassSubClassOfInput = ref(null);
const newIndividualType = ref(null);
const selectedNewProperty = ref("");
const selectedNewPropertyOptions = ref([]);
const newIndividualProperties = ref([
  {
    property: {
      iri: "",
      label: "",
      type: "",
    },
    range: {
      iri: "",
      label: "",
      type: ""
    },
    rangeIsLiteral: true,
    value: {
      literal: null,
      individual: null, // { label: "", iri: "" }
    }
  }
]);
watch(selectedNewProperty, (to) => {
  if (to && to.property && to.property.iri) {
    const lastEl = newIndividualProperties.value[newIndividualProperties.value.length - 1];
    newIndividualProperties.value.splice(newIndividualProperties.value.length - 1, 1, Object.assign(lastEl, to));
    selectedNewProperty.value = "";
  }
});

const entitiesAutoCompleteOptions = ref([]);

const browseRoute = router.resolve({name: 'browse'});

const onSubmitForm = async (stepperActivateCallback) => {
  const url = `${backendHost}/api/submission/submitForm`;
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
  stepperActivateCallback(2);
}

const previewEntity = async (segment) => {
  const referencedEntity = referencedEntities.value[segment.entityReferenceIndex];
  previewingEntity.value = {iri: referencedEntity.iri, type: referencedEntity.type};
  isEntityPreviewVisible.value = true;
}

const getSeverityForContext = context => ({
  "Referenced": "secondary",
  "Added": "success",
  "Edited": "warn",
})[context];

const autoCompleteEntities = async (event, type, subClassOf = null) => {
  const filteredAddedEntities = [...addedEntities.value.filter(it => it.type === type)];
  entitiesAutoCompleteOptions.value = (await fetchSearchEntities(event.query, type, subClassOf))
      .concat(filteredAddedEntities);
}

const onAddEntity = () => {
  const label = newEntityLabelInput.value;
  const type = selectedNewEntityType.value.value;
  const comment = newEntityCommentInput.value;
  let newEntity;
  if (type === "Class") {
    newEntity = {
      label,
      type,
      subClassOf: newClassSubClassOfInput.value,
      comment,
    }
  } else if (type === "Individual") {
    newEntity = {
      label,
      type,
      individualType: newIndividualType.value,
      comment,
    }
  } else if (type === "Property") {
    newEntity = {
      label,
      type,
      comment,
    }
  }
  addedEntities.value.push(newEntity);
  isNewEntityDialogVisible.value = false;
}

watch(newIndividualProperties, (to) => {
  console.log(to);
  if (to.length === 0 || to[to.length - 1] && to[to.length - 1].property && to[to.length - 1].property.label) {
    newIndividualProperties.value.push({
      property: {
        iri: "",
        label: "",
        type: "",
      },
      range: {
        iri: "",
        label: "",
        type: ""
      },
      rangeIsLiteral: true,
      value: {
        literal: null,
        individual: null, // { label: "", iri: "" }
      }
    });
  }
}, {deep: true});
const onRemoveNewIndividualProperty = (index) => {
  newIndividualProperties.value.splice(index, 1);
};
const onAddNewIndividualPropertyValue = (event, data, index) => {
  newIndividualProperties.value.splice(index + 1, 0, {
    property: data.property,
    range: data.range,
    rangeIsLiteral: data.rangeIsLiteral,
    value: {
      literal: null,
      individual: null
    }
  });
};
const autoCompleteNewIndividualProperties = async (event) => {
  //const filteredAddedEntities = [...addedEntities.value.filter(it => it.type === type)];
  // TODO: Support added properties
  //    .concat(filteredAddedEntities);
  // TODO: Filter out already selected properties
  // TODO: Allow creating new properties here

  const properties = await fetchIndividualProperties(event.query, newIndividualType.value.iri);
  const filteredProperties = [...properties.filter(it => !newIndividualProperties.value.find(other => it.property.iri === other.property.iri))];
  //newIndividualPropertyOptions.value = [...properties.map(it => it.property)];
  selectedNewPropertyOptions.value = filteredProperties;
}

const shouldShowNewPropertyValuePlusIcon = (data, index) => {
  return newIndividualProperties.value.findLastIndex(it => data.property === it.property) === index;
}
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
                <DataTable :value="identifiedTerms" editMode="cell" showGridlines>
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
                  <label for="newEntityLabelInput" class="font-medium">What term would you like to introduce?</label>
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
                <template v-if="newEntityLabelInputDebounced">
                  <div class="flex flex-col gap-2">
                    <label for="newEntityCommentInput" class="font-medium">How would you describe the term?</label>
                    <Textarea fluid v-model="newEntityCommentInput" id="newEntityCommentInput" rows="3"
                              placeholder="Describe the term…"/>
                  </div>
                  <div class="flex flex-col gap-2">
                    <label for="selectedNewEntityType" class="font-medium">Which type is the term?</label>
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
                </template>
                <template v-if="selectedNewEntityType !== null && selectedNewEntityType.value === 'Class'">
                  <div class="flex flex-col gap-2">
                    <label for="newClassSubClassOfInput" class="font-medium">Does this class have a super class?</label>
                    <AutoComplete v-model="newClassSubClassOfInput"
                                  id="newClassSubClassOfInput"
                                  pt:pcInput:root:class="w-full"
                                  forceSelection
                                  optionLabel="label"
                                  :suggestions="entitiesAutoCompleteOptions"
                                  @complete="e => autoCompleteEntities(e, 'Class')"
                                  placeholder="No."/>
                  </div>
                </template>
                <template v-if="selectedNewEntityType !== null && selectedNewEntityType.value === 'Individual'">
                  <div class="flex flex-col gap-2">
                    <label for="newEntitySubClassOfInput" class="font-medium">Which class does this individual belong
                      to?</label>
                    <AutoComplete v-model="newIndividualType"
                                  id="newIndividualTypeInput"
                                  pt:pcInput:root:class="w-full"
                                  forceSelection
                                  optionLabel="label"
                                  :suggestions="entitiesAutoCompleteOptions"
                                  @complete="e => autoCompleteEntities(e, 'Class')"
                                  placeholder="Type…"/>
                  </div>
                  <template v-if="newIndividualType && newIndividualType.iri">
                    <div class="flex flex-col gap-2">
                      <div class="font-medium">What are the properties of this individual?</div>
                      <DataTable :value="newIndividualProperties" rowGroupMode="rowspan"
                                 :groupRowsBy="['property.label', 'range.label']"
                                 stripedRows size="small">
                        <Column field="property.label" header="Property" style="width: 12rem">
                          <template #body="{data, index}">
                            <template v-if="!data || !data.property || !data.property.label">
                              <AutoComplete v-model="selectedNewProperty"
                                            pt:pcInput:root:class="w-full"
                                            forceSelection
                                            dropdown
                                            optionLabel="property.label"
                                            :suggestions="selectedNewPropertyOptions"
                                            @complete="e => autoCompleteNewIndividualProperties(e)"
                                            placeholder="Add property..."/>
                            </template>
                            <template v-else>
                              {{ data.property.label }}
                            </template>
                          </template>
                        </Column>
                        <Column field="range.label" header="Type" style="width: 12rem">
                        </Column>
                        <Column field="value.literal" header="Value(s)" style="width: 12rem">
                          <template #body="{data, index}">
                            <template v-if="data && data.property && data.property.label">
                              <template v-if="data.rangeIsLiteral">
                                <InputText fluid v-model="newIndividualProperties[index].value.literal"
                                           placeholder="Value…"/>
                              </template>
                              <template v-else>
                                <AutoComplete v-model="newIndividualProperties[index].value.individual"
                                              pt:pcInput:root:class="w-full"
                                              forceSelection
                                              optionLabel="label"
                                              :suggestions="entitiesAutoCompleteOptions"
                                              @complete="e => autoCompleteEntities(e, 'Individual', data.range.iri)"
                                              placeholder="Individual…"/>
                              </template>
                            </template>
                          </template>
                        </Column>
                        <Column>
                          <template #body="{ data, index }" style="width: 0">
                            <div class="flex flex-row justify-end gap-2"
                                 v-if="data && data.property && data.property.label">
                              <Button
                                  @click="e => onAddNewIndividualPropertyValue(e, data, index)" icon="pi pi-plus"
                                  size="small" severity="secondary" rounded
                                  pt:root:class="!px-0 !py-0 size-6" outlined
                                  v-if="shouldShowNewPropertyValuePlusIcon(data, index)"/>
                              <Button @click="onRemoveNewIndividualProperty(index)" icon="pi pi-times" size="small"
                                      severity="danger" rounded
                                      pt:root:class="!px-0 !py-0 size-6" outlined/>
                            </div>
                          </template>
                        </Column>
                      </DataTable>
                    </div>
                  </template>
                </template>
                <div class="flex flex-row justify-end gap-2">
                  <Button label="Cancel" severity="secondary" outlined size="small"
                          @click="isNewEntityDialogVisible = false"/>
                  <Button label="Add term" :disabled="!selectedNewEntityType" @click="onAddEntity" size="small"/>
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