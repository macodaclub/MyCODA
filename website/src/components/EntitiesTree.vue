<script setup>

import Tree from 'primevue/tree';
import SelectButton from 'primevue/selectbutton';
import Badge from 'primevue/badge';
import Button from 'primevue/button';
import Divider from 'primevue/divider';
import {RouterLink, useRoute, useRouter} from 'vue-router';
import {substringAfter} from "@/utils/utils/utils.js";
import {computed, onMounted, ref, watch} from "vue";
import {useOntologyStore} from "@/store";

const props = defineProps({
  usesRouter: {
    type: Boolean,
    default: false
  },
  initialSelectedIri: {
    type: [String, null],
    default: null
  },
  initialSelectedEntityType: {
    type: [String, null],
    default: "Class"
  },
  allowEdits: {
    type: Boolean,
    default: false
  }
});

const router = useRouter();
const route = useRoute();
const ontologyStore = useOntologyStore();
const {
  fetchOntologyInfo,
  fetchTaxonomyTree,
  fetchExpandedTaxonomyTree,
  fetchSelectedTaxonomyTree,
  fetchSelectedEntityInfo
} = ontologyStore;

const ontologyInfo = ref(null);
const taxonomyTree = ref(null);
const expandedKeys = ref({});
const selectedIri = ref(props.initialSelectedIri);
const selectedEntityType = ref(props.initialSelectedEntityType);
const selectedEntityInfo = ref(null);

onMounted(async () => {
  if (props.usesRouter) {
    await selectEntityFromPath(window.location.href);
  } else {
    if (selectedIri.value != null) {
      await selectEntity(selectedIri.value, selectedEntityType.value);
    } else {
      await deselectEntity(selectedEntityType.value);
    }
  }
});

onMounted(async () => {
  ontologyInfo.value = await fetchOntologyInfo();
});

if (props.usesRouter) {
  watch(route, async (to) => {
    console.log("watch route");
    console.log(to.fullPath);
    expandedKeys.value = {};
    await selectEntityFromPath(to.fullPath);
  });
}

const entityTypesWithCounts = computed(() => [
  {
    value: "Class",
    label: `Classes${ontologyInfo.value === null ? "" : ` (${ontologyInfo.value.counts.classes})`}`,
  },
  {
    value: "Property",
    label: `Properties${ontologyInfo.value === null ? "" : ` (${ontologyInfo.value.counts.properties})`}`,
  },
  {
    value: "Individual",
    label: `Individuals${ontologyInfo.value === null ? "" : ` (${ontologyInfo.value.counts.individuals})`}`,
  },
]);

const infoPanelTitle = computed(() => {
  const iri = selectedIri.value;
  const type = selectedEntityType.value;
  if (iri && type) {
    return `${type} Information`;
  } else {
    return "Ontology Information";
  }
});

const onTreeNodeExpand = async (node) => {
  if (node.children === undefined || node.children.length < node.data.directChildrenCount) {
    node.loading = true;
    node.children = await fetchExpandedTaxonomyTree(node);
    node.loading = false;
  }
};

const onTreeRootExpand = async () => {
  taxonomyTree.value = await fetchTaxonomyTree(taxonomyTree.value, selectedEntityType.value);
};

const onSelectEntity = async (iri, type) => {
  if (props.usesRouter) {
    await router.push({path: 'browse', query: {iri, type}});
  } else {
    await selectEntity(iri, type);
  }
};

const selectEntityFromPath = async (path) => {
  const queryParams = path.split(/[?&]/)
      .filter(it => it.includes("="))
      .map(queryParam => {
        const nameAndValue = queryParam.split("=");
        return {
          name: nameAndValue[0],
          value: nameAndValue[1]
        };
      });
  let type = null;
  if (queryParams.find(it => it.name === "type")) {
    type = queryParams.find(it => it.name === "type").value;
    selectedEntityType.value = type;
  }
  if (queryParams.find(it => it.name === "iri")) {
    const iri = decodeURIComponent(queryParams.find(it => it.name === "iri").value);
    await selectEntity(iri, type);
  } else {
    await deselectEntity(type);
  }
}

const selectEntity = async (iri, _type) => {
  const {tree, type} = await fetchSelectedTaxonomyTree(iri, _type);
  const expandedKeysObj = {};
  const stack = [...tree];
  while (stack?.length > 0) {
    const currentObj = stack.pop();
    if (currentObj.children && currentObj.children.length > 0) {
      expandedKeysObj[currentObj.key] = true;
      stack.push(...currentObj.children);
    }
  }
  taxonomyTree.value = tree;
  expandedKeys.value = expandedKeysObj;
  selectedIri.value = iri;
  selectedEntityType.value = type;
  selectedEntityInfo.value = await fetchSelectedEntityInfo(iri, type);
}

const deselectEntity = async (type) => {
  selectedIri.value = null;
  selectedEntityInfo.value = null;
  expandedKeys.value = {};
  taxonomyTree.value = await fetchTaxonomyTree([], type);
}

const onSelectEntityType = async ({value}) => {
  const type = value;
  selectedIri.value = null;
  expandedKeys.value = {};
  if (props.usesRouter) {
    await router.push({path: 'browse', query: {type}});
  } else {
    await deselectEntity(type);
  }
}

defineExpose({
  selectEntity
});
</script>

<template>
  <div class="root">
    <div class="taxonomy-tree-panel">
      <div class="tree-header">
        <RouterLink to="/browse"><h4 class="font-semibold">MyCODA Ontology</h4></RouterLink>
        <SelectButton v-model="selectedEntityType"
                      :options="entityTypesWithCounts"
                      @change="onSelectEntityType"
                      optionLabel="label"
                      optionValue="value"
                      :allowEmpty="false"
                      pt:pcButton:root:class="text-sm"/>
      </div>
      <Tree :value="taxonomyTree" :expandedKeys="expandedKeys" @nodeExpand="onTreeNodeExpand"
            @nodeSelect.prevent
            loadingMode="icon"
            pt:root:class="overflow-auto">
        <template #default="{node}">
          <a @click.prevent="onSelectEntity(node.data.iri, node.data.type)"
             :class="{'selected-entity': node.data.iri === selectedIri}"
             :href="node.data.iri">{{ node.label }}</a>
          <span class="tree-children-count" v-if="!node.leaf">({{ node.data.allChildrenCount }})</span>
          <Badge v-if="!node.data.iri.startsWith('http://localhost:8081/ontologies/')" value="Imported"
                 size="small" severity="contrast"/>
          <Button @click="onTreeNodeExpand(node)"
                  v-if="node.children && node.data.directChildrenCount > node.children.length && expandedKeys && expandedKeys[node.key] === true"
                  size="small" label="…"
                  style="margin-left: 6px; padding: 0.1rem 0.3rem" severity="secondary" outlined/>
        </template>
      </Tree>
      <Button @click="onTreeRootExpand"
              v-if="taxonomyTree && taxonomyTree.length === 1" size="small" label="…"
              style="margin-left: 1.7rem; margin-bottom: 1.5rem; padding: 0.1rem 0.3rem" severity="secondary" outlined/>
    </div>
    <div class="info-panel">
      <h4 class="info-panel-title font-semibold">{{ infoPanelTitle }}</h4>
      <div class="header-divider">
        <Divider layout="horizontal" align="center"/>
      </div>
      <div class="info-fields">
        <div v-if="selectedIri" class="info-field">
          <h4 class="capitalize font-semibold">IRI</h4>
          <a :href="selectedIri" target=”_blank”>{{ selectedIri }}</a>
        </div>
        <template v-else-if="ontologyInfo">
          <div v-for="annotation in ontologyInfo.annotations" class="info-field">
            <h4 class="capitalize font-semibold">{{ annotation.property }}</h4>
            <template v-if="annotation.value.startsWith('http')">
              <a :href="annotation.value" target=”_blank”>{{ annotation.value }}</a>
            </template>
            <template v-else>
              <p>{{ annotation.value }}</p>
            </template>
          </div>
        </template>
        <template v-if="selectedEntityInfo">
          <div v-for="annotation in selectedEntityInfo.annotations" class="info-field">
            <h4 class="capitalize font-semibold">{{ annotation.propertyLabel }}</h4>
            <p>{{ annotation.value }}</p>
          </div>
          <template v-if="selectedEntityInfo.type === 'Class' && selectedEntityInfo.classInfo">
            <div
                v-if="selectedEntityInfo.classInfo.equivalentClasses && selectedEntityInfo.classInfo.equivalentClasses.length > 0"
                class="info-field">
              <h4 class="capitalize font-semibold">Synonyms</h4>
              <a v-for="equivalentClass in selectedEntityInfo.classInfo.equivalentClasses" :href="equivalentClass.iri"
                 @click.prevent="onSelectEntity(equivalentClass.iri, equivalentClass.type)">{{
                  equivalentClass.label
                }}</a>
            </div>
          </template>
          <template v-if="selectedEntityInfo.type === 'Individual' && selectedEntityInfo.individualInfo !== null">
            <div
                v-if="selectedEntityInfo.individualInfo.types && selectedEntityInfo.individualInfo.types.length > 0"
                class="info-field">
              <h4 class="capitalize font-semibold">
                Type{{ selectedEntityInfo.individualInfo.types.length > 1 ? "s" : "" }}</h4>
              <a v-for="type in selectedEntityInfo.individualInfo.types" :href="type.iri"
                 @click.prevent="onSelectEntity(type.iri, type.type)">{{
                  type.label
                }}</a>
            </div>
          </template>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.root {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 20px;

  .taxonomy-tree-panel {
    flex-grow: 1;
    overflow: hidden;
    flex-basis: 25em;
    border-radius: 10px;
    border: 1px solid #e2e8f0;
    background-color: #ffffff;

    a {
      color: var(--color-text);
    }

    .p-badge {
      margin-left: 6px;
    }

    .tree-header {
      display: flex;
      flex-direction: row;
      flex-wrap: wrap;
      align-items: center;
      gap: 20px;
      justify-content: space-between;
      margin: 1.5rem 2rem 0;
    }

    .selected-entity {
      color: var(--p-primary-color);
      font-weight: bold;
    }

    .tree-children-count {
      margin-left: 2px;
      color: var(--p-text-muted-color);
    }
  }

  .info-panel {
    flex-grow: 1;
    overflow: hidden;
    text-wrap: wrap;
    word-wrap: break-word;
    overflow-wrap: break-word;
    flex-basis: 25em;
    border-radius: 10px;
    border: 1px solid #e2e8f0;
    background-color: #ffffff;

    a {
      color: var(--color-text);
    }

    h4 {
      padding: 0;
      margin: 0;
    }

    p {
      padding: 0;
      margin: 0;
    }

    .info-panel-title {
      margin: 1.5rem 2rem 0;
    }

    .header-divider {
      margin: 0 2rem;
    }

    .info-fields {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      margin: 0 2rem 1.5rem;

      .info-field {
        display: flex;
        flex-direction: column;
        gap: 0.1rem;
      }
    }
  }
}
</style>

<style>
.p-tree {
  .p-tree-node-children {
    position: relative;
  }

  .p-tree-node-children::before {
    content: "‎";
    border-right: 1px dotted #AAAAAA;
    position: absolute;
    width: 3px;
    height: 100%;
  }

  .p-tree-container .p-tree-node:focus:not(:focus-visible) > .p-tree-node-content {
    outline: none;
  }
}
</style>