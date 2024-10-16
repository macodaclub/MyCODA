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
import Divider from 'primevue/divider';
import InputGroup from 'primevue/inputgroup';
import Checkbox from 'primevue/checkbox';
import Popover from 'primevue/popover';
import ConfirmDialog from "primevue/confirmdialog";
import {useDialog} from 'primevue/usedialog';
import {useConfirm} from "primevue/useconfirm";
import {useDebounceFn} from "@vueuse/core"
import {useRouter} from 'vue-router'
import {computed, defineAsyncComponent, reactive, ref, watch} from "vue";
import _ from "lodash"
import {useOntologyStore} from "@/store";
import {camelCaseToCapitalized, deepToRaw, delay, generateIriSuffix} from "@/utils/utils/utils.js";
import TransitionExpand from "@/components/TransitionExpand.vue";

const EntitiesTree = defineAsyncComponent(() => import("@/components/EntitiesTree.vue"));
const TutorialVideoPlayer = defineAsyncComponent(() => import('@/components/TutorialVideoPlayer.vue'));

const isDev = import.meta.env.DEV;
const ghRepoUrl = import.meta.env.GITHUB_REPO_URL;
const mycodaOntologyIriPrefix = import.meta.env.MYCODA_ONTOLOGY_IRI_PREFIX;

const ontologyStore = useOntologyStore();
const {backendHost, fetchEntityInfo, fetchSearchEntities, fetchIndividualProperties} = ontologyStore;

const router = useRouter();

const dialog = useDialog();
const confirm = useConfirm();

const showTutorialVideo = () => {
  dialog.open(TutorialVideoPlayer, {
    props: {
      header: 'Article Submission Tutorial',
      style: {
        width: '70vw',
      },
      breakpoints: {
        '960px': '75vw',
        '640px': '90vw'
      },
      modal: true,
      dismissableMask: true
    }
  });
};

const activeStep = ref(1);

const popoverHelpArticleForm = ref();
const popoverHelpSubmittedArticle = ref();
const popoverHelpIdentifiedTerms = ref();
const popoverHelpContribute = ref();

const titleInput = ref(isDev ? "A Kotlin implementation of the pNSGA-II algorithm (PMOEA)" : "");
const abstractInput = ref(isDev ? "The p NSGA-II algorithm, a preference-based multi-objective evolutionary algorithm—a subclass of multi-objective evolutionary algorithm—has been widely recognized for its efficiency in handling complex optimization problems involving multiple objectives. Originally implemented in a Java Library, the algorithm has recently been adapted to Kotlin, reflecting a growing trend in modern software development towards more concise and expressive programming languages. The adaptation to Kotlin not only preserves the algorithm's robust performance but also enhances its usability and integration with contemporary software ecosystems. One of the principal contributors to the development of the p NSGA-II algorithm was Carlos Coello Coello, whose work has significantly influenced the field of evolutionary computation. The algorithm's capability to incorporate user preferences in the optimization process makes it particularly valuable for real-world applications where decision-makers often have specific goals or priorities. As such, the p NSGA-II algorithm continues to be a vital tool in both academic research and industrial applications, driving advancements in fields ranging from engineering design to artificial intelligence." : "");
const keywordsInput = ref(isDev ? "pNSGA-II, Kotlin, PMOEA" : "");
const authorsInput = ref(isDev ? "Tiago Nunes, Vítor B. Fernandes, Michael T.M. Emmerich" : "");
const emailInput = ref(isDev ? "tmlns@iscte-iul.pt" : "");
const consentStorage = ref(isDev);
const consentContribution = ref(isDev);

const titleSegments = ref(null);
const abstractSegments = ref(null);
const keywordSegments = ref(null);
const authorsSegments = ref(null);
const referencedEntities = ref(null);

const isEntityPreviewVisible = ref(false);
const entityPreviewTree = ref(null);
const previewingEntity = ref(null);

const addedEntities = ref([]);
const editedEntities = ref([]);
const identifiedTerms = computed(() => {
  let result = [...referencedEntities.value.map(it => {
    const context = ["Referenced"];
    return {
      context: context,
      entity: it
    }
  })];

  editedEntities.value.forEach(edited => {
    const index = result.findIndex(it => edited.entity.iri === it.entity.iri);
    if (index !== -1) {
      const el = result[index];
      result[index] = {
        context: el.context.concat(["Edited"]),
        entity: el.entity
      };
    } else {
      result.push({
        context: ["Edited"],
        entity: edited.entity
      });
    }
  });

  result = result.concat([...addedEntities.value.map(it => ({
    context: ["Added"],
    entity: it.entity
  }))]);

  return result;
});

const isEntityEditorDialogVisible = ref(false);
watch(isEntityEditorDialogVisible, (to) => {
  if (!to) {
    editingEntityInputDefaultValues.forEach(it => editingEntityInfo[it.key] = it.defaultValue);
    editingEntityLabelDebounced.value = null;
    synonymSuggestions.value = null;
    shouldHideSynonymSuggestions.value = false;
    editingEntitySynonymOfInput.value = null;
    shouldShowSynonymOfInput.value = false;
    editingEntitySynonymOfInputRef.value = null;
  }
});
const editingEntityLabelDebounced = ref(null);
const editingEntityLabelInputDebounceFn = useDebounceFn(() => {
  editingEntityLabelDebounced.value = editingEntityInfo.label;
}, 500);
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
const selectedNewProperty = ref("");
const selectedNewPropertyOptions = ref([]);
watch(selectedNewProperty, (to) => {
  if (to && to.property && to.property.iri) {
    const lastEl = editingEntityInfo.properties[editingEntityInfo.properties.length - 1];
    editingEntityInfo.properties = editingEntityInfo.properties.toSpliced(editingEntityInfo.properties.length - 1, 1, Object.assign(lastEl, to));
    selectedNewProperty.value = "";
  }
});

const editingEntityInfo = reactive({
  iri: null,
  label: null,
  synonyms: [],
  type: null,
  comment: null,
  subClassOf: null,
  individualType: null,
  properties: [
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
      value: {
        literal: null,
        individual: null, // { label: "", iri: "" }
      }
    }
  ],
  domain: null,
  range: null,
});

const editingEntityInfoSnapshots = [];

const editingEntityInputDefaultValues = Object.entries(editingEntityInfo).map(([key, value]) => ({
  key: key,
  defaultValue: deepToRaw(value),
}));

const entitiesAutoCompleteOptions = ref([]);

const editExistingEntityInput = ref(null);
const editExistingEntityInputRef = ref();
const editExistingEntityAutoCompleteOptions = ref([]);

const shouldHideSynonymSuggestions = ref(false);
const synonymSuggestions = ref(null);
const editingEntitySynonymOfInputRef = ref(null);
watch(editingEntitySynonymOfInputRef, (to) => to && to.$el && to.$el.querySelector('input') ? to.$el.querySelector('input').focus() : {});
const editingEntitySynonymOfInput = ref(null);
const shouldShowSynonymOfInput = ref(false);
watch(editingEntityLabelDebounced, async (to) => {
  if (!to || editingEntityInfo.iri) return;
  const url = new URL(`${backendHost}/api/editor/synonymSuggestions`);
  url.search = new URLSearchParams({query: to}).toString();
  const response = await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
    },
  });
  if (!response.ok) return console.error('Failed to find synonym suggestions', response);
  synonymSuggestions.value = (await response.json()).synonymSuggestions;
});

const submissionId = ref(null);
const githubIssueUrl = ref(null);

watch(activeStep, async (to) => {
  window.scrollTo({top: 0, left: 0, behavior: 'smooth'});
  if (to === 3) {
    const response = await submitChanges();
    submissionId.value = response.submissionId;
    githubIssueUrl.value = response.githubIssueUrl;
  }
});

const browseRoute = router.resolve({name: 'browse'});

const susForm = ref([
  {
    question: "I think that I would like to use this system frequently.",
    answer: null
  },
  {
    question: "I found the system unnecessarily complex.",
    answer: null
  },
  {
    question: "I thought the system was easy to use.",
    answer: null
  },
  {
    question: "I think that I would need the support of a technical person to be able to use this system.",
    answer: null
  },
  {
    question: "I found the various functions in this system were well integrated.",
    answer: null
  },
  {
    question: "I thought there was too much inconsistency in this system.",
    answer: null
  },
  {
    question: "I would imagine that most people would learn to use this system very quickly.",
    answer: null
  },
  {
    question: "I found the system very cumbersome to use.",
    answer: null
  },
  {
    question: "I felt very confident using the system.",
    answer: null
  },
  {
    question: "I needed to learn a lot of things before I could get going with this system.",
    answer: null
  },
]);
const susFormSubmitted = ref(false);
const susFormExpanded = ref(true);

const feedbackInput = ref("");
const feedbackSubmitted = ref(false);
const feedbackExpanded = ref(true);

const onSubmitForm = async (stepperActivateCallback) => {
  const url = `${backendHost}/api/submission/submitForm`;
  const formData = new FormData();
  formData.append("title", titleInput.value);
  formData.append("abstract", abstractInput.value);
  formData.append("keywords", keywordsInput.value);
  formData.append("authors", authorsInput.value);
  const response = await fetch(url, {
    method: "POST",
    body: formData,
  });
  if (!response.ok) return console.error('Failed to fetch ontology info', response);
  const responseData = await response.json();
  titleSegments.value = responseData.titleSegments;
  abstractSegments.value = responseData.abstractSegments;
  keywordSegments.value = responseData.keywordSegments;
  authorsSegments.value = responseData.authorsSegments;
  referencedEntities.value = responseData.referencedEntities;
  addNewArticleEntity();
  stepperActivateCallback(2);
};

const previewEntity = async (entity) => {
  previewingEntity.value = {iri: entity.iri, type: entity.type};
  isEntityPreviewVisible.value = true;
};

const getSeverityForContext = context => ({
  "Referenced": "secondary",
  "Added": "success",
  "Edited": "info",
})[context];

const autoCompleteEntities = async (event, types = null, subClassOf = null) => {
  const filteredAddedEntities = [...addedEntities.value.map(it => it.entity).filter(it => (types === null || types.includes(it.type)) && it.label.toLowerCase().startsWith(event.query.toLowerCase()))];
  entitiesAutoCompleteOptions.value = filteredAddedEntities.concat(await fetchSearchEntities(event.query, types, subClassOf && !subClassOf.startsWith("[New Entity]") ? subClassOf : null));
};

const autoCompleteEditExistingEntity = async (event) => {
  editExistingEntityAutoCompleteOptions.value = await fetchSearchEntities(event.query);
};

function findChangesInProperties(oldList, newList) {
  const diffs = [];
  newList.forEach(newElement => {
    if (!oldList.some(oldElement => _.isEqual(oldElement, newElement))) {
      diffs.push({change: 'Added Property', property: newElement});
    }
  });
  oldList.forEach(oldElement => {
    if (!newList.some(newElement => _.isEqual(newElement, oldElement))) {
      diffs.push({change: 'Removed Property', property: oldElement});
    }
  });
  return diffs;
}

const addNewArticleEntity = () => {
  const keywordProperties =
      keywordsInput.value.split(",")
          .map(it => it.trim())
          .map(it => {
            return {
              property: {
                iri: `${mycodaOntologyIriPrefix}#hasKeyword`,
                label: "has keyword",
                type: "Property"
              },
              range: {
                iri: "http://www.w3.org/2001/XMLSchema#string",
                label: "string",
                type: "Datatype"
              },
              value: it
            }
          });
  const authorProperties = authorsSegments.value
      .filter(it => it.entityReferenceIndex !== null)
      .map(it => {
        const entity = referencedEntities.value[it.entityReferenceIndex];
        return {
          property: {
            iri: `${mycodaOntologyIriPrefix}#hasAuthor`,
            label: "has author",
            type: "Property"
          },
          range: {
            iri: `${mycodaOntologyIriPrefix}#OWLClass_9598b33d_f57f_4f27_942e_fa47ade955e3`,
            label: "Researcher",
            type: "Individual"
          },
          value: {
            iri: entity.iri,
            label: entity.label,
            type: entity.type
          }
        }
      });
  const properties = [].concat(
      keywordProperties,
      authorProperties,
  )
  const articleEntity = {
    entity: {
      iri: `[New Entity]#${generateIriSuffix("OWLIndividual")}`,
      label: titleInput.value,
      type: "Individual",
    },
    synonyms: [],
    individualType: {
      iri: `${mycodaOntologyIriPrefix}#Article`,
      label: "Article",
      type: "Class"
    },
    properties
  };
  addedEntities.value = addedEntities.value.concat(articleEntity);
};

const onSaveEditedEntity = () => {
  const label = editingEntityInfo.label;
  const synonyms = editingEntityInfo.synonyms.filter(it => it.trim() !== "");
  const type = editingEntityInfo.type.value;
  const comment = editingEntityInfo.comment;
  let newEntity;
  if (type === "Class") {
    newEntity = {
      entity: {
        iri: `[New Entity]#${generateIriSuffix("OWLClass")}`,
        label,
        type,
      },
      synonyms,
      comment,
      subClassOf: editingEntityInfo.subClassOf,
    }
  } else if (type === "Individual") {
    newEntity = {
      entity: {
        iri: `[New Entity]#${generateIriSuffix("OWLIndividual")}`,
        label,
        type,
      },
      synonyms,
      comment,
      individualType: editingEntityInfo.individualType,
      properties: editingEntityInfo.properties.slice(0, -1).map(it => ({
        property: it.property,
        range: it.range,
        value: it.range.type === "Datatype" ? it.value.literal : it.value.individual,
      })),
    }
  } else if (type === "Property") {
    newEntity = {
      entity: {
        iri: `[New Entity]#${generateIriSuffix("OWLProperty")}`, // TODO: Specify ObjectProperty / DataProperty
        label,
        type,
      },
      synonyms,
      comment,
      domain: editingEntityInfo.domain,
      range: editingEntityInfo.range,
    }
  }
  if (editingEntityInfo.iri) {
    if (editingEntityInfo.iri.startsWith("[New Entity]")) {
      const index = addedEntities.value.findIndex(it => it.entity.iri === editingEntityInfo.iri);
      addedEntities.value = addedEntities.value.toSpliced(index, 1, newEntity);
      addedEntities.value[index] = newEntity;
    } else {
      const from = editingEntityInfoSnapshots.find(it => it.iri === editingEntityInfo.iri);
      const to = deepToRaw(editingEntityInfo);
      if (to.properties && to.properties.length > 0) {
        const index = to.properties.findIndex(it => !it.property);
        if (index) {
          to.properties.splice(index, 1);
        }
      }
      let edits = [];
      Object.keys(to).forEach(key => {
        if (key !== 'properties') {
          if (!_.isEqual(from[key], to[key])) {
            edits.push({
              change: 'Updated Field',
              field: camelCaseToCapitalized(key),
              value: to[key]
            });
          }
        } else {
          if (to.type.value === 'Individual' && to.properties !== null) {
            findChangesInProperties(from.properties, to.properties).forEach(it => edits.push({
              change: it.change,
              field: it.property.property,
              value: it.property.range.type === "Datatype" ? it.property.value.literal : it.property.value.individual
            }));
          }
        }
      });
      if (edits.length > 0) {
        const editedEntity = {
          entity: {
            iri: to.iri,
            label: to.label,
            type: to.type.value,
          },
          edits,
          entityInfo: to
        }
        const index = editedEntities.value.findIndex(it => it.entity.iri === to.iri);
        editedEntities.value = index !== -1 ? editedEntities.value.toSpliced(index, 1, editedEntity) : editedEntities.value.concat([editedEntity]);
      } else {
        const index = editedEntities.value.findIndex(it => it.entity.iri === to.iri);
        if (index !== -1) {
          editedEntities.value = editedEntities.value.toSpliced(index, 1);
        }
      }
    }
  } else {
    addedEntities.value = addedEntities.value.concat(newEntity);
  }
  isEntityEditorDialogVisible.value = false;
};

watch(() => editingEntityInfo.properties, (to) => {
  if (!to) return;
  if (to.length === 0 || to[to.length - 1] && to[to.length - 1].property && to[to.length - 1].property.label) {
    editingEntityInfo.properties = to.concat({
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
      value: {
        literal: null,
        individual: null, // { label: "", iri: "" }
      }
    });
  }
}, {deep: true});
const onRemoveNewIndividualProperty = (index) => {
  editingEntityInfo.properties = editingEntityInfo.properties.toSpliced(index, 1);
};
const onAddNewIndividualPropertyValue = (event, data, index) => {
  editingEntityInfo.properties = editingEntityInfo.properties.toSpliced(index + 1, 0, {
    property: data.property,
    range: data.range,
    value: {
      literal: null,
      individual: null
    }
  });
};
const autoCompleteNewIndividualProperties = async (event) => {
  // TODO: Filter added properties, so it shows only the ones that are applied to a certain class
  const filteredAddedProperties = [...addedEntities.value.filter(it => it.entity.type === "Property" && it.entity.label.toLowerCase().startsWith(event.query.toLowerCase())).map(it => ({
    property: it.entity,
    range: it.range
  })).filter(it => !editingEntityInfo.properties.find(other => it.property.iri === other.property.iri))];
  // TODO: Allow creating new properties here

  const properties = await fetchIndividualProperties(event.query, editingEntityInfo.individualType.iri.startsWith("[New Entity]") ? null : editingEntityInfo.individualType.iri);
  const filteredProperties = [...properties.filter(it => !editingEntityInfo.properties.find(other => it.property.iri === other.property.iri))];
  selectedNewPropertyOptions.value = filteredAddedProperties.concat(filteredProperties);
};

const shouldShowNewPropertyValuePlusIcon = (data, index) => {
  return editingEntityInfo.properties.findLastIndex(it => data.property.iri === it.property.iri) === index;
};

const submitChanges = async () => {
  const url = `${backendHost}/api/submission/submitChanges`;
  const body = {
    formData: {
      articleTitle: titleInput.value,
      articleAbstract: abstractInput.value,
      articleKeywords: keywordsInput.value,
      articleAuthors: authorsInput.value,
      emailAddress: emailInput.value
    },
    addedEntities: addedEntities.value,
    editedEntities: editedEntities.value.map(it => ({
      entity: it.entity,
      edits: it.edits,
    }))
  }
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!response.ok) return console.error('Failed to submit changes', response);
  return await response.json();
};

const openTargetBlank = url => {
  window.open(url, '_blank');
}

const onEditEntity = async (entity) => {
  editingEntityInfo.label = entity.label;
  editingEntityLabelDebounced.value = entity.label;
  editingEntityInfo.type = entityTypeOptions.find(it => it.value === entity.type);
  if (entity.iri.startsWith("[New Entity]")) {
    const entityInfo = addedEntities.value.find(it => it.entity.iri === entity.iri);
    editingEntityInfo.synonyms = entityInfo.synonyms.filter(it => it.trim() !== "");
    editingEntityInfo.comment = entityInfo.comment;
    if (entity.type === "Class") {
      editingEntityInfo.subClassOf = entityInfo.subClassOf;
    } else if (entity.type === "Individual") {
      editingEntityInfo.individualType = entityInfo.individualType;
      editingEntityInfo.properties = entityInfo.properties.map(it => ({
        property: it.property,
        range: it.range,
        value: {
          literal: it.range.type === "Datatype" ? it.value : null,
          individual: it.range.type !== "Datatype" ? it.value : null,
        }
      }));
    } else if (entity.type === "Property") {
      editingEntityInfo.domain = entityInfo.domain;
      editingEntityInfo.range = entityInfo.range;
    }
  } else {
    const editedEntity = editedEntities.value.find(it => it.entity.iri === entity.iri);
    if (editedEntity) {
      const entityInfo = deepToRaw(editedEntity.entityInfo);
      editingEntityInfo.synonyms = entityInfo.synonyms.filter(it => it.trim() !== "");
      editingEntityInfo.comment = entityInfo.comment;
      if (entity.type === "Class") {
        editingEntityInfo.subClassOf = entityInfo.subClassOf;
      } else if (entity.type === "Individual") {
        editingEntityInfo.individualType = entityInfo.individualType;
        editingEntityInfo.properties = entityInfo.properties.map(it => ({
          property: it.property,
          range: it.range,
          value: it.value,
        }));
      } else if (entity.type === "Property") {
        editingEntityInfo.domain = entityInfo.domain;
        editingEntityInfo.range = entityInfo.range;
      }
    } else {
      const entityInfo = await fetchEntityInfo(entity.iri);
      const synonymsAnnotation = entityInfo.annotations.find(it => it.property.iri === `${mycodaOntologyIriPrefix}#altLabel`);
      if (synonymsAnnotation) {
        editingEntityInfo.synonyms = synonymsAnnotation.values.map(it => it.label);
      } else {
        editingEntityInfo.synonyms = [];
      }
      editingEntityInfo.comment = entityInfo.comment;
      if (entity.type === "Class") {
        editingEntityInfo.subClassOf = entityInfo.classInfo.subClassOf.length > 0 ? entityInfo.classInfo.subClassOf[0] : null;
      } else if (entity.type === "Individual") {
        editingEntityInfo.individualType = entityInfo.individualInfo.types[0];
        editingEntityInfo.properties = entityInfo.individualInfo.properties.flatMap(it => it.values.map(value => ({
          property: it.property,
          range: it.range,
          value: {
            literal: it.range.type === "Datatype" ? value.label : null,
            individual: it.range.type !== "Datatype" ? value : null,
          }
        })));
      } else if (entity.type === "Property") {
        editingEntityInfo.domain = entityInfo.propertyInfo.domain;
        editingEntityInfo.range = entityInfo.propertyInfo.range;
      }
    }
  }
  editingEntityInfo.iri = entity.iri;
  if (!editedEntities.value.some(it => it.entity.iri === entity.iri)) {
    editingEntityInfoSnapshots.push(deepToRaw(editingEntityInfo));
  }
  isEntityEditorDialogVisible.value = true;
};

const onRemoveAddedEntity = entity => {
  const index = addedEntities.value.findIndex(it => it.entity.iri === entity.iri);
  addedEntities.value = addedEntities.value.toSpliced(index, 1);
};

const onEditAnExistingTerm = async () => {
  if (editExistingEntityInput.value && editExistingEntityInput.value.iri) {
    await onEditEntity(editExistingEntityInput.value);
  } else {
    console.log(editExistingEntityInputRef.value.$el);
    editExistingEntityInputRef.value.$el.querySelector('input').focus();
  }
};

const onSelectSynonym = async (synonym) => {
  confirm.require({
    group: 'addSynonym',
    message: `Would you like to add "<span class="font-semibold">${editingEntityInfo.label}</span>" as a synonym of <a href="${synonym.iri}" target="_blank">${synonym.label}</a>?`,
    header: 'Add Synonym',
    rejectProps: {
      label: 'Cancel',
      severity: 'secondary',
      outlined: true,
      size: 'small'
    },
    acceptProps: {
      label: 'Add Synonym',
      size: 'small'
    },
    accept: async () => {
      const synonymToAdd = editingEntityInfo.label
      isEntityEditorDialogVisible.value = false;
      await delay(500);
      await onEditEntity(synonym);
      if (!editingEntityInfo.synonyms.includes(synonymToAdd) && synonymToAdd !== synonym.label) {
        editingEntityInfo.synonyms.push(synonymToAdd);
      }
    },
    reject: () => {
    }
  });
};

const submitSus = async () => {
  const url = `${backendHost}/api/submission/submitSus`;
  const body = {
    submissionId: submissionId.value,
    answers: susForm.value.map(it => it.answer)
  }
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!response.ok) return console.error('Failed to submit SUS Answers', response);
  return await response.json();
};

const submitFeedback = async () => {
  const url = `${backendHost}/api/submission/submitFeedback`;
  const body = {
    submissionId: submissionId.value,
    feedback: feedbackInput.value
  }
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  });
  if (!response.ok) return console.error('Failed to submit Feedback', response);
  return await response.json();
};
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
                <div class="flex flex-row justify-start items-center gap-2 mb-2">
                  <h2 class="text-xl font-bold">Article Submission Form</h2>
                  <Button class="size-6 border border-gray-400" pt:icon:class="text-xs" icon="pi pi-question" rounded
                          severity="secondary" @click="e => popoverHelpArticleForm.toggle(e)"/>
                </div>
                <FloatLabel>
                  <InputText fluid id="titleInput" v-model="titleInput"/>
                  <label for="titleInput">Title</label>
                </FloatLabel>
                <FloatLabel pt:root:class="mt-2">
                  <Textarea fluid id="abstractInput" v-model="abstractInput" rows="5" cols="30" autoResize/>
                  <label for="abstractInput">Abstract</label>
                </FloatLabel>
                <FloatLabel>
                  <InputText fluid id="keywordsInput" v-model="keywordsInput"/>
                  <label for="keywordsInput">Keywords (separated by commas)</label>
                </FloatLabel>
                <FloatLabel pt:root:class="mt-2">
                  <InputText fluid id="authorsInput" v-model="authorsInput"/>
                  <label for="authorsInput">Authors (separated by commas)</label>
                </FloatLabel>
                <FloatLabel pt:root:class="mt-2">
                  <InputText fluid id="emailInput" v-model="emailInput"/>
                  <label for="emailInput">Email address</label>
                </FloatLabel>
                <div class="flex flex-col gap-4">
                  <div class="flex flex-row items-center gap-2 text-sm">
                    <Checkbox v-model="consentStorage" binary required id="consentStorage" name="consentStorage"/>
                    <label for="consentStorage"><strong class="text-red-700">*</strong> I consent to the storage of my
                      contact and article information and its
                      use for assisting me in contributing to the MyCODA Knowledge Base, contacting me regarding my
                      contribution, and helping improve the MyCODA platform.
                      I consent to the addition of the title, keywords and authors to the Knowledge Base.
                      (Your contact information and abstract will not be imported to the Knowledge Base, and will not be
                      shared with
                      the public.)</label>
                  </div>
                </div>
              </div>
              <div class="flex flex-col items-start">
                <Divider class="mt-0"/>
                <Button type="submit" icon="pi pi-check-circle" label="Submit Article"/>
              </div>
            </form>
          </StepPanel>
          <StepPanel v-slot="{ activateCallback }" :value="2">
            <div class="flex flex-col gap-8">
              <div class="leading-loose">
                <div class="flex flex-row justify-start items-center gap-2 mb-4">
                  <h3 class="text-lg font-semibold">Submitted Article</h3>
                  <Button class="size-6 border border-gray-400" pt:icon:class="text-xs" icon="pi pi-question" rounded
                          severity="secondary" @click="e => popoverHelpSubmittedArticle.toggle(e)"/>
                </div>
                <div class="flex flex-col gap-2 border bg-surface-50 p-2">
                  <div class="border bg-surface-0 p-4">
                    <h3 class="text-lg font-semibold mb-2">Title</h3>
                    <template v-for="segment in titleSegments">
                      <span v-if="segment.entityReferenceIndex === null">{{ segment.text }}</span>
                      <Button size="small" class="py-[0.05rem] px-1 leading-normal font-medium" outlined
                              severity="primary" v-else
                              @click.prevent="previewEntity(referencedEntities[segment.entityReferenceIndex])">{{
                          segment.text
                        }}
                      </Button>
                    </template>
                  </div>
                  <div class="border bg-surface-0 p-4">
                    <h3 class="text-lg font-semibold mb-2">Abstract</h3>
                    <template v-for="segment in abstractSegments">
                      <span v-if="segment.entityReferenceIndex === null">{{ segment.text }}</span>
                      <Button size="small" class="py-[0.05rem] px-1 leading-normal font-medium" outlined
                              severity="primary" v-else
                              @click.prevent="previewEntity(referencedEntities[segment.entityReferenceIndex])">{{
                          segment.text
                        }}
                      </Button>
                    </template>
                  </div>
                  <div class="border bg-surface-0 p-4">
                    <h3 class="text-lg font-semibold mb-2">Keywords</h3>
                    <template v-for="segment in keywordSegments">
                      <span v-if="segment.entityReferenceIndex === null">{{ segment.text }}</span>
                      <Button size="small" class="py-[0.05rem] px-1 leading-normal font-medium" outlined
                              severity="primary" v-else
                              @click.prevent="previewEntity(referencedEntities[segment.entityReferenceIndex])">{{
                          segment.text
                        }}
                      </Button>
                    </template>
                  </div>
                  <div class="border bg-surface-0 p-4">
                    <h3 class="text-lg font-semibold mb-2">Authors</h3>
                    <template v-for="segment in authorsSegments">
                      <span v-if="segment.entityReferenceIndex === null">{{ segment.text }}</span>
                      <Button size="small" class="py-[0.05rem] px-1 leading-normal font-medium" outlined
                              severity="primary" v-else
                              @click.prevent="previewEntity(referencedEntities[segment.entityReferenceIndex])">{{
                          segment.text
                        }}
                      </Button>
                    </template>
                  </div>
                </div>
              </div>
              <div>
                <div class="flex flex-row justify-start items-center gap-2 mb-4">
                  <h3 class="text-lg font-semibold">Identified Terms</h3>
                  <Button class="size-6 border border-gray-400" pt:icon:class="text-xs" icon="pi pi-question" rounded
                          severity="secondary" @click="e => popoverHelpIdentifiedTerms.toggle(e)"/>
                </div>
                <DataTable :value="identifiedTerms" editMode="cell" showGridlines stripedRows>
                  <Column field="context" header="Context" style="width: 4rem;">
                    <template #body="{ data, field }">
                      <div class="flex flex-row gap-2">
                        <Badge v-for="context in data[field]" :value="context"
                               :severity="getSeverityForContext(context)"/>
                      </div>
                    </template>
                  </Column>
                  <Column field="entity.type" header="Type" style="width: 4rem;"/>
                  <Column field="entity.label" header="Term">
                    <template #body="{ data }">
                      <Button size="small" class="py-[0.05rem] px-1 leading-normal font-medium" outlined
                              severity="primary"
                              @click="!data.entity.iri.startsWith('[New Entity]') ? previewEntity(data.entity) : onEditEntity(data.entity)">
                        <Badge v-if="data.entity.iri.startsWith('[New Entity]')" severity="secondary" size="small">New
                        </Badge>
                        {{
                          data.entity.label
                        }}
                      </Button>
                    </template>
                  </Column>
                  <Column class="w-0" header="Actions">
                    <template #body="{ data }">
                      <div class="flex flex-row gap-2">
                        <Button icon="pi pi-pen-to-square" outlined size="small" severity="info"
                                pt:root:class="h-8" @click="onEditEntity(data.entity)" label="Edit"/>
                        <Button v-if="!data.entity.iri.startsWith('[New Entity]')" icon="pi pi-external-link" outlined
                                size="small" severity="secondary"
                                pt:root:class="h-8"
                                @click="openTargetBlank(router.resolve({name: 'browse', query: {iri: data.entity.iri, type: data.entity.type}}).href)"
                                label="Browse"/>
                        <Button v-if="data.entity.iri.startsWith('[New Entity]')" icon="pi pi-times-circle" outlined
                                size="small" severity="danger" label="Remove"
                                pt:root:class="h-8" @click="onRemoveAddedEntity(data.entity)"/>
                      </div>
                    </template>
                  </Column>
                </DataTable>
              </div>
              <div>
                <div class="flex flex-row justify-start items-center gap-2 mb-4">
                  <h3 class="text-lg font-semibold">Contribute</h3>
                  <Button class="size-6 border border-gray-400" pt:icon:class="text-xs" icon="pi pi-question" rounded
                          severity="secondary" @click="e => popoverHelpContribute.toggle(e)"/>
                </div>
                <div class="flex flex-row justify-start gap-4 flex-wrap">
                  <Button @click="isEntityEditorDialogVisible = true" label="Add a new term"
                          icon="pi pi-plus-circle" size="small" severity="contrast" outlined/>
                  <InputGroup size="small" class="w-fit">
                    <Button icon="pi pi-pen-to-square" label="Edit an existing term" size="small" severity="contrast"
                            outlined @click="onEditAnExistingTerm()"/>
                    <AutoComplete v-model="editExistingEntityInput"
                                  id="editExistingEntityInput"
                                  ref="editExistingEntityInputRef"
                                  pt:pcInput:root:class="w-full"
                                  forceSelection
                                  size="small"
                                  optionLabel="label"
                                  :suggestions="editExistingEntityAutoCompleteOptions"
                                  @complete="e => autoCompleteEditExistingEntity(e)"
                                  placeholder="Term…"/>
                  </InputGroup>
                </div>
              </div>
              <div>
                <div class="flex flex-col gap-4">
                  <div class="flex flex-row items-center gap-2 text-sm">
                    <Checkbox v-model="consentContribution" binary required id="consentContribution"
                              name="consentContribution"/>
                    <label for="consentContribution"><strong class="text-red-700">*</strong> I consent to the storage
                      and
                      sharing of all the information provided above, with the understanding that it will be publicly
                      accessible in the MyCODA Knowledge Base.</label>
                  </div>
                </div>
                <Divider/>
                <Button @click="consentContribution ? activateCallback(3) : {}" label="Submit Contribution"
                        icon="pi pi-check-circle"/>
              </div>
            </div>
            <Dialog v-model:visible="isEntityPreviewVisible" modal header="Entity Preview" maximizable dismissableMask
                    pt:root:class="w-11/12 bg-[#f8fafc]">
              <EntitiesTree ref="entityPreviewTree" :initialSelectedIri="previewingEntity.iri"
                            :initialSelectedEntityType="previewingEntity.type"/>
            </Dialog>
            <Dialog v-model:visible="isEntityEditorDialogVisible" modal :dismissableMask="!!editingEntityInfo.iri"
                    pt:root:class="w-full max-w-[48rem]"
                    :header="editingEntityInfo.iri ? 'Edit term' : 'Add a new term'">
              <div class="flex flex-col gap-6">
                <div class="flex flex-col gap-2">
                  <label for="newEntityLabelInput" class="font-medium">{{
                      editingEntityInfo.iri ? 'What is the label of this term?' : 'What term would you like to introduce?'
                    }}</label>
                  <div class="flex flex-row gap-2">
                    <InputText v-model="editingEntityInfo.label"
                               @input="editingEntityLabelInputDebounceFn"
                               id="newEntityLabelInput"
                               fluid
                               :disabled="editingEntityInfo.iri && !editingEntityInfo.iri.startsWith('[New Entity]')"
                               :placeholder="editingEntityInfo.iri ? 'Label…' : 'New term…'"/>
                    <Button v-if="editingEntityInfo.synonyms.length === 0" icon="pi pi-equals" severity="secondary"
                            v-tooltip.bottom="'Add Synonyms'" @click="editingEntityInfo.synonyms.push('')"/>
                  </div>
                </div>
                <div v-if="editingEntityInfo.synonyms && editingEntityInfo.synonyms.length > 0">
                  <DataTable :value="editingEntityInfo.synonyms" stripedRows size="small">
                    <Column header="Synonyms" style="width: 12rem">
                      <template #body="{index}">
                        <InputText fluid v-model="editingEntityInfo.synonyms[index]"
                                   placeholder="Synonym…"/>
                      </template>
                    </Column>
                    <Column>
                      <template #body="{ data, index }" style="width: 0">
                        <div class="flex flex-row justify-end gap-2">
                          <Button
                              @click="data.trim() !== '' ? editingEntityInfo.synonyms.push('') : {}" icon="pi pi-plus"
                              size="small" severity="secondary" rounded
                              pt:root:class="!px-0 !py-0 size-6" outlined
                              v-if="index === editingEntityInfo.synonyms.length - 1 && data.trim() !== ''"/>
                          <Button @click="editingEntityInfo.synonyms.splice(index, 1)" icon="pi pi-times" size="small"
                                  severity="danger" rounded
                                  pt:root:class="!px-0 !py-0 size-6" outlined/>
                        </div>
                      </template>
                    </Column>
                  </DataTable>
                </div>
                <div v-if="editingEntityLabelDebounced && !editingEntityInfo.iri && !shouldHideSynonymSuggestions"
                     class="flex flex-col gap-3 border rounded-border border-primary relative px-4 pt-2 pb-3 text-sm">
                  <h4 class="absolute top-[-0.55rem] start-[0.5rem] text-xs bg-surface-0 px-2 text-primary">
                    Suggestion</h4>
                  <a href="" @click.prevent="shouldHideSynonymSuggestions = true"><i
                      class="pi pi-times-circle text-sm absolute top-[-0.4rem] right-2 text-primary bg-surface-0 px-1"/></a>
                  <div>Is <span class="font-semibold">{{ editingEntityLabelDebounced }}</span> a synonym of an
                    existing term?
                  </div>
                  <div class="flex flex-row gap-2 items-start">
                    <form
                        @submit.prevent="editingEntitySynonymOfInput && editingEntitySynonymOfInput.iri ? onSelectSynonym(editingEntitySynonymOfInput) : {}">
                      <InputGroup v-if="shouldShowSynonymOfInput" pt:root:class="w-auto">
                        <AutoComplete v-model="editingEntitySynonymOfInput"
                                      id="editingEntitySynonymOfInput"
                                      ref="editingEntitySynonymOfInputRef"
                                      forceSelection
                                      optionLabel="label"
                                      :suggestions="entitiesAutoCompleteOptions"
                                      @complete="e => autoCompleteEntities(e)"
                                      placeholder="Synonym of…"/>
                        <Button type="submit" icon="pi pi-check" outlined severity="secondary"
                                :disabled="!editingEntitySynonymOfInput || !editingEntitySynonymOfInput.iri"/>
                      </InputGroup>
                    </form>
                    <Button v-if="!shouldShowSynonymOfInput" label="Select Synonym…" severity="secondary" outlined
                            rounded size="small" @click="shouldShowSynonymOfInput = true;"/>
                    <template v-for="synonymSuggestion in synonymSuggestions">
                      <Button :label="synonymSuggestion.label" severity="secondary" outlined rounded size="small"
                              @click="onSelectSynonym(synonymSuggestion)"/>
                    </template>
                  </div>
                  <p>Verify: <a :href="browseRoute.href" target="_blank" class=""><i
                      class="pi pi-external-link text-sm ml-1 me-2"/>Browse
                    knowledge
                    base</a>
                  </p>
                </div>
                <template v-if="editingEntityLabelDebounced">
                  <div class="flex flex-col gap-2">
                    <label for="newEntityCommentInput" class="font-medium">How would you describe the term?</label>
                    <Textarea fluid v-model="editingEntityInfo.comment" id="newEntityCommentInput" rows="3" autoResize
                              placeholder="Describe the term…"/>
                  </div>
                  <div class="flex flex-col gap-2">
                    <label for="selectedNewEntityType" class="font-medium">Which type is the term?</label>
                    <Select v-model="editingEntityInfo.type" id="selectedNewEntityType" :options="entityTypeOptions"
                            :disabled="editingEntityInfo.iri && !editingEntityInfo.iri.startsWith('[New Entity]')"
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
                <template v-if="editingEntityInfo.type !== null && editingEntityInfo.type.value === 'Class'">
                  <div class="flex flex-col gap-2">
                    <label for="newClassSubClassOfInput" class="font-medium">Does this class have a super class?</label>
                    <AutoComplete v-model="editingEntityInfo.subClassOf"
                                  id="newClassSubClassOfInput"
                                  pt:pcInput:root:class="w-full"
                                  forceSelection
                                  optionLabel="label"
                                  :suggestions="entitiesAutoCompleteOptions"
                                  @complete="e => autoCompleteEntities(e, ['Class'])"
                                  placeholder="No."/>
                  </div>
                </template>
                <template v-if="editingEntityInfo.type !== null && editingEntityInfo.type.value === 'Individual'">
                  <div class="flex flex-col gap-2">
                    <label for="newEntitySubClassOfInput" class="font-medium">Which class does this individual belong
                      to?</label>
                    <AutoComplete v-model="editingEntityInfo.individualType"
                                  id="newIndividualTypeInput"
                                  pt:pcInput:root:class="w-full"
                                  forceSelection
                                  optionLabel="label"
                                  :suggestions="entitiesAutoCompleteOptions"
                                  @complete="e => autoCompleteEntities(e, ['Class'])"
                                  placeholder="Type…"/>
                  </div>
                  <template v-if="editingEntityInfo.individualType && editingEntityInfo.individualType.iri">
                    <div class="flex flex-col gap-2">
                      <div class="font-medium">What are the properties of this individual?</div>
                      <DataTable :value="editingEntityInfo.properties" rowGroupMode="rowspan"
                                 groupRowsBy="property.label"
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
                                            placeholder="Add property…"/>
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
                              <template v-if="!data.range || !data.range.type || data.range.type === 'Datatype'">
                                <InputText fluid v-model="editingEntityInfo.properties[index].value.literal"
                                           placeholder="Value…"/>
                              </template>
                              <template v-else>
                                <AutoComplete v-model="editingEntityInfo.properties[index].value.individual"
                                              pt:pcInput:root:class="w-full"
                                              forceSelection
                                              optionLabel="label"
                                              :suggestions="entitiesAutoCompleteOptions"
                                              @complete="e => autoCompleteEntities(e, data.range.type === 'Entity' ? null : ['Individual'], data.range.type === 'Entity' ? null : data.range.iri)"
                                              :placeholder="data.range.type === 'Entity' ? 'Term…' : `Individual…`"/>
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
                <template v-if="editingEntityInfo.type !== null && editingEntityInfo.type.value === 'Property'">
                  <div class="flex flex-col gap-2">
                    <label for="newEntitySubClassOfInput" class="font-medium">Which type of individuals may have this
                      property?</label>
                    <AutoComplete v-model="editingEntityInfo.domain"
                                  id="newPropertyDomain"
                                  pt:pcInput:root:class="w-full"
                                  forceSelection
                                  optionLabel="label"
                                  :suggestions="entitiesAutoCompleteOptions"
                                  @complete="e => autoCompleteEntities(e, ['Class'])"
                                  placeholder="Every type."/>
                  </div>
                  <div class="flex flex-col gap-2">
                    <label for="newEntitySubClassOfInput" class="font-medium">What type is the value of this
                      property?</label>
                    <AutoComplete v-model="editingEntityInfo.range"
                                  id="newPropertyRange"
                                  pt:pcInput:root:class="w-full"
                                  forceSelection
                                  optionLabel="label"
                                  :suggestions="entitiesAutoCompleteOptions"
                                  @complete="e => autoCompleteEntities(e, ['Datatype', 'Class'])"
                                  placeholder="Value type…"/>
                  </div>
                </template>
                <div class="flex flex-row justify-end gap-2">
                  <Button label="Cancel" severity="secondary" outlined size="small"
                          @click="isEntityEditorDialogVisible = false"/>
                  <Button :label="editingEntityInfo.iri ? 'Edit term' : 'Add term'"
                          :disabled="!editingEntityInfo.type || editingEntityInfo.type.value === 'Individual' && (!editingEntityInfo.individualType || !editingEntityInfo.individualType.iri) || editingEntityInfo.type.value === 'Property' && (!editingEntityInfo.range || !editingEntityInfo.range.iri)"
                          @click="onSaveEditedEntity" size="small"/>
                </div>
              </div>
            </Dialog>
            <ConfirmDialog group="addSynonym">
              <template #message="slotProps">
                <span v-html="slotProps.message.message"/>
              </template>
            </ConfirmDialog>
          </StepPanel>
          <StepPanel :value="3">
            <div class="flex flex-col gap-8 px-4 pb-4 pt-2">
              <div>
                <h3 class="text-lg font-semibold mb-4">🙌 Thank you for your contribution to the MyCODA Knowledge
                  Base!</h3>
                <div class="flex flex-col gap-2">
                  <p>Your contribution has been submitted through a GitHub Issue on the <a
                      :href="ghRepoUrl" target="_blank">MyCODA Github Repository</a>.</p>
                  <p>A curator will review the proposed changes and update the ontology accordingly. This may take a few
                    days.</p>
                </div>
                <Button class="mt-6" :icon="githubIssueUrl ? 'pi pi-github' : 'pi pi-spin pi-spinner'"
                        label="View GitHub Issue" size="small"
                        :disabled="!githubIssueUrl"
                        severity="contrast"
                        @click="openTargetBlank(githubIssueUrl)"/>
                <Divider class="my-6"/>
                <div class="text-lg font-semibold mb-5">How was your experience while using this tool?</div>
                <div class="flex flex-col border px-2 py-2 overflow-auto"
                     :class="[susFormExpanded ? 'bg-gray-50' : 'cursor-pointer hover:bg-gray-50', susFormSubmitted ? 'bg-gray-50' : '']"
                     @[!susFormExpanded&&`click`]="susFormExpanded = true">
                  <div class="text-sm font-semibold text-gray-500 flex flex-row gap-2 items-center"
                       :class="susFormExpanded ? 'cursor-pointer hover:bg-gray-50' : ''"
                       @click="susFormExpanded = !susFormExpanded">
                    <i class="pi"
                       :class="susFormSubmitted ? 'pi-check-circle text-emerald-700' : susFormExpanded ? 'pi-minus-circle' : 'pi-plus-circle'"/>
                    System Usability Scale (10 questions)
                  </div>
                  <TransitionExpand :expanded="susFormExpanded">
                    <div v-if="susFormExpanded">
                      <div class="flex flex-col gap-10 px-2 pt-6">
                        <div class="flex flex-col gap-4" v-for="(question, index) in susForm">
                          <div class="font-medium">{{ index + 1 }}. {{ question.question }}</div>
                          <div class="flex flex-row items-end">
                            <div class="flex flex-col items-center gap-1">
                              <div class="text-xs font-semibold text-center text-gray-700">Strongly<br/>disagree</div>
                              <div
                                  class="flex items-center justify-center border border-black border-r-0 h-7 w-20 text-xs cursor-pointer hover:bg-primary-50"
                                  :class="question.answer === 1 ? 'bg-primary-400 text-white hover:!bg-primary-400' : ''"
                                  @click="susForm[index].answer = 1"
                              >
                                1
                              </div>
                            </div>
                            <div class="flex flex-col items-center gap-1">
                              <div
                                  class="flex items-center justify-center border border-black border-r-0 h-7 w-20 text-xs cursor-pointer hover:bg-primary-50"
                                  :class="question.answer === 2 ? 'bg-primary-400 text-white hover:!bg-primary-400' : ''"
                                  @click="susForm[index].answer = 2"
                              >
                                2
                              </div>
                            </div>
                            <div class="flex flex-col items-center gap-1">
                              <div
                                  class="flex items-center justify-center border border-black border-r-0 h-7 w-20 text-xs cursor-pointer hover:bg-primary-50"
                                  :class="question.answer === 3 ? 'bg-primary-400 text-white hover:!bg-primary-400' : ''"
                                  @click="susForm[index].answer = 3"
                              >
                                3
                              </div>
                            </div>
                            <div class="flex flex-col items-center gap-1">
                              <div
                                  class="flex items-center justify-center border border-black border-r-0 h-7 w-20 text-xs cursor-pointer hover:bg-primary-50"
                                  :class="question.answer === 4 ? 'bg-primary-400 text-white hover:!bg-primary-400' : ''"
                                  @click="susForm[index].answer = 4"
                              >
                                4
                              </div>
                            </div>
                            <div class="flex flex-col items-center gap-1">
                              <div class="text-xs font-semibold text-center text-gray-700">Strongly<br/>agree</div>
                              <div
                                  class="flex items-center justify-center border border-black h-7 w-20 text-xs cursor-pointer hover:bg-primary-50"
                                  :class="question.answer === 5 ? 'bg-primary-400 text-white hover:!bg-primary-400' : ''"
                                  @click="susForm[index].answer = 5"
                              >
                                5
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                      <Button icon="pi pi-check" class="self-start mt-8 ms-2 mb-2" label="Submit Answers" size="small"
                              :disabled="susForm.some(it=> it.answer === null)"
                              @click="submitSus(); susFormSubmitted = true; susFormExpanded = false"/>
                    </div>
                  </TransitionExpand>
                </div>
                <div class="flex flex-col border px-2 py-2 overflow-auto mt-4"
                     :class="[feedbackExpanded ? 'bg-gray-50' : 'cursor-pointer hover:bg-gray-50', feedbackSubmitted ? 'bg-gray-50' : '']"
                     @[!feedbackExpanded&&`click`]="feedbackExpanded = true">
                  <div class="text-sm font-semibold text-gray-500 flex flex-row gap-2 items-center"
                       :class="feedbackExpanded ? 'cursor-pointer hover:bg-gray-50' : ''"
                       @click="feedbackExpanded = !feedbackExpanded">
                    <i class="pi"
                       :class="feedbackSubmitted ? 'pi-check-circle text-emerald-700' : feedbackExpanded ? 'pi-minus-circle' : 'pi-plus-circle'"/>
                    Provide Your Feedback
                  </div>
                  <TransitionExpand :expanded="feedbackExpanded">
                    <div v-if="feedbackExpanded" class="flex flex-col mt-4 mx-2">
                      <Textarea v-model="feedbackInput" autoResize rows="6" cols="30" />
                      <Button icon="pi pi-check" class="self-start mt-4 mb-2" label="Submit Feedback" size="small"
                              :disabled="feedbackInput.length === 0"
                              @click="submitFeedback(); feedbackSubmitted = true; feedbackExpanded = false"/>
                    </div>
                  </TransitionExpand>
                </div>
              </div>
            </div>
          </StepPanel>
        </StepPanels>
      </Stepper>
    </div>
  </main>
  <Popover ref="popoverHelpArticleForm" pt:root:class="bg-gray-50">
    <div class="flex flex-col gap-2 max-w-[28rem]">
      <h2 class="font-semibold">Help: Article Submission Form</h2>
      <Divider class="m-0"/>
      <p class="text-sm">If you've written an article within the domain of Many-Criteria Optimization and Decision
        Analysis, you are welcome to use our tool designed to help enrich the ontology from your article, and make your
        work more accessible to others.</p>
      <p class="text-sm">To begin, please fill the form with your article information and email address.</p>
      <p class="text-sm">This information will be used to assist you in contributing to the MyCODA Knowledge Base, by
        finding any known terms, and providing context and suggestions.</p>
      <Divider class="m-0"/>
      <p class="text-sm">For more information, check the <a href="" @click.prevent="showTutorialVideo()">tutorial
        video</a>, or <a
          href="mailto: macodaclub@gmail.com">contact us</a>.</p>
    </div>
  </Popover>
  <Popover ref="popoverHelpSubmittedArticle" pt:root:class="bg-gray-50">
    <div class="flex flex-col gap-2 max-w-[28rem]">
      <h2 class="font-semibold">Help: Submitted Article</h2>
      <Divider class="m-0"/>
      <p class="text-sm">Any terms identified in your article that already exist in the knowledge base will appear
        highlighted below in orange.</p>
      <p class="text-sm">You can gather information about the current knowledge by clicking on any identified terms, or
        by <a :href="browseRoute.href" @click.prevent="openTargetBlank(browseRoute.href)">browsing the ontology</a>.</p>
      <p class="text-sm">Use these resources to understand how the knowledge around your topics is structured, and what
        information is missing.</p>
      <Divider class="m-0"/>
      <p class="text-sm">For more information, check the <a href="" @click.prevent="showTutorialVideo()">tutorial
        video</a>, or <a
          href="mailto: macodaclub@gmail.com">contact
        us</a>.</p>
    </div>
  </Popover>
  <Popover ref="popoverHelpIdentifiedTerms" pt:root:class="bg-gray-50">
    <div class="flex flex-col gap-2 max-w-[28rem]">
      <h2 class="font-semibold">Help: Identified Terms</h2>
      <Divider class="m-0"/>
      <p class="text-sm">This table shows known terms referenced in the article, as well as terms you've added
        or edited.</p>
      <p class="text-sm">You can edit or browse existing terms, or you can edit or remove new terms.</p>
      <Divider class="m-0"/>
      <p class="text-sm font-semibold mb-1">Types information:</p>
      <p class="text-sm" v-for="type in entityTypeOptions"><span class="font-semibold">{{ type.value }}</span>:
        {{ type.description }}</p>
      <Divider class="m-0"/>
      <p class="text-sm">For more information, check the <a href="" @click.prevent="showTutorialVideo()">tutorial
        video</a>, or <a
          href="mailto: macodaclub@gmail.com">contact
        us</a>.</p>
    </div>
  </Popover>
  <Popover ref="popoverHelpContribute" pt:root:class="bg-gray-50">
    <div class="flex flex-col gap-2 max-w-[28rem]">
      <h2 class="font-semibold">Help: Contribute</h2>
      <Divider class="m-0"/>
      <p class="text-sm">You are welcome to contribute to the MyCODA knowledge by adding new terms, or editing existing
        terms.</p>
      <p class="text-sm">Any changes to the knowledge after submitting your contribution will be reviewed by a curator,
        before they are implemented in the ontology.</p>
      <Divider class="m-0"/>
      <p class="text-sm">For more information, check the <a href="" @click.prevent="showTutorialVideo()">tutorial
        video</a>, or <a
          href="mailto: macodaclub@gmail.com">contact us</a>.</p>
    </div>
  </Popover>
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