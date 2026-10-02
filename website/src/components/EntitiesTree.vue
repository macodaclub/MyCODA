<script setup>
import Tree from 'primevue/tree'
import SelectButton from 'primevue/selectbutton'
import Button from 'primevue/button'
import Divider from 'primevue/divider'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { computed, onMounted, ref, watch } from 'vue'
import { useOntologyStore } from '@/store'
import Clipboard from '@/components/Clipboard.vue'
import OntologyGraph from '@/components/ontology/OntologyGraph.vue'

const props = defineProps({
  usesRouter: {
    type: Boolean,
    default: false,
  },
  initialSelectedIri: {
    type: [String, null],
    default: null,
  },
  initialSelectedEntityType: {
    type: [String, null],
    default: 'Class',
  },
  allowEdits: {
    type: Boolean,
    default: false,
  },
})

const router = useRouter()
const route = useRoute()
const ontologyStore = useOntologyStore()

const {
  fetchOntologyInfo,
  fetchTaxonomyTree,
  fetchExpandedTaxonomyTree,
  fetchSelectedTaxonomyTree,
  fetchEntityInfo,
} = ontologyStore

const ontologyInfo = ref(null)
const taxonomyTree = ref(null)
const expandedKeys = ref({})
const selectedIri = ref(props.initialSelectedIri)
const selectedEntityType = ref(props.initialSelectedEntityType)
const selectedEntityInfo = ref(null)
const showGraph = ref(false)

const entityTypesWithCounts = computed(() => [
  {
    value: 'Class',
    label: `Classes${ontologyInfo.value === null ? '' : ` (${ontologyInfo.value.counts.classes})`}`,
  },
  {
    value: 'Property',
    label: `Properties${ontologyInfo.value === null ? '' : ` (${ontologyInfo.value.counts.properties})`}`,
  },
  {
    value: 'Individual',
    label: `Individuals${ontologyInfo.value === null ? '' : ` (${ontologyInfo.value.counts.individuals})`}`,
  },
])

const infoPanelTitle = computed(() => {
  if (selectedIri.value && selectedEntityType.value) {
    return `${selectedEntityType.value} Information`
  }

  return 'Ontology Information'
})

onMounted(async () => {
  ontologyInfo.value = await fetchOntologyInfo()

  if (props.usesRouter) {
    await selectEntityFromPath(window.location.href)
    return
  }

  if (selectedIri.value != null) {
    await selectEntity(selectedIri.value, selectedEntityType.value)
  } else {
    await deselectEntity(selectedEntityType.value)
  }
})

if (props.usesRouter) {
  watch(route, async to => {
    expandedKeys.value = {}
    await selectEntityFromPath(to.fullPath)
  })
}

async function onTreeNodeExpand(node) {
  if (
    node.children === undefined ||
    node.children.length < node.data.directChildrenCount
  ) {
    node.loading = true

    try {
      node.children = await fetchExpandedTaxonomyTree(node)
    } finally {
      node.loading = false
    }
  }
}

async function onTreeRootExpand() {
  taxonomyTree.value = await fetchTaxonomyTree(
    taxonomyTree.value,
    selectedEntityType.value,
  )
}

async function onSelectEntity(iri, type) {
  if (!iri) return

  if (props.usesRouter) {
    await router.push({
      path: '/browse',
      query: { iri, type },
    })
    return
  }

  await selectEntity(iri, type)
}

async function selectEntityFromPath(path) {
  const queryParams = path
    .split(/[?&]/)
    .filter(value => value.includes('='))
    .map(queryParam => {
      const [name, value] = queryParam.split('=')
      return { name, value }
    })

  const typeParam = queryParams.find(value => value.name === 'type')
  const iriParam = queryParams.find(value => value.name === 'iri')

  const type = typeParam?.value ?? selectedEntityType.value
  selectedEntityType.value = type

  if (iriParam) {
    await selectEntity(decodeURIComponent(iriParam.value), type)
  } else {
    await deselectEntity(type)
  }
}

async function selectEntity(iri, requestedType) {
  const result = await fetchSelectedTaxonomyTree(iri, requestedType)
  if (!result) return

  const { tree, type } = result
  const expandedKeysObj = {}
  const stack = [...tree]

  while (stack.length > 0) {
    const currentObj = stack.pop()

    if (currentObj.children?.length) {
      expandedKeysObj[currentObj.key] = true
      stack.push(...currentObj.children)
    }
  }

  taxonomyTree.value = tree
  expandedKeys.value = expandedKeysObj
  selectedIri.value = iri
  selectedEntityType.value = type
  selectedEntityInfo.value = await fetchEntityInfo(iri, type)
}

async function deselectEntity(type) {
  selectedIri.value = null
  selectedEntityInfo.value = null
  expandedKeys.value = {}
  taxonomyTree.value = await fetchTaxonomyTree([], type)
}

async function onSelectEntityType({ value }) {
  const type = value
  selectedIri.value = null
  selectedEntityInfo.value = null
  expandedKeys.value = {}
  showGraph.value = false

  if (props.usesRouter) {
    await router.push({
      path: '/browse',
      query: { type },
    })
    return
  }

  await deselectEntity(type)
}

function openGraph() {
  showGraph.value = true
}

function closeGraph() {
  showGraph.value = false
}

async function onSelectGraphEntity(iri, type) {
  if (
    !iri ||
    type === 'Datatype' ||
    type === 'Literal' ||
    type === 'AnnotationValue'
  ) {
    return
  }

  await onSelectEntity(iri, type)
  showGraph.value = true
}

defineExpose({
  selectEntity,
})
</script>

<template>
  <div class="root">
    <div class="taxonomy-tree-panel">
      <div class="tree-header">
        <RouterLink to="/browse">
          <h4 class="font-semibold text-lg">MyCODA Ontology</h4>
        </RouterLink>

        <SelectButton
          v-model="selectedEntityType"
          :options="entityTypesWithCounts"
          option-label="label"
          option-value="value"
          :allow-empty="false"
          pt:pcButton:root:class="text-sm"
          @change="onSelectEntityType"
        />
      </div>

      <Tree
        :value="taxonomyTree"
        :expanded-keys="expandedKeys"
        loading-mode="icon"
        pt:root:class="overflow-auto"
        @node-expand="onTreeNodeExpand"
        @node-select.prevent
      >
        <template #default="{ node }">
          <a
            :href="node.data.iri"
            :class="{ 'selected-entity': node.data.iri === selectedIri }"
            @click.prevent="onSelectEntity(node.data.iri, node.data.type)"
          >
            {{ node.label }}
          </a>

          <span v-if="!node.leaf" class="tree-children-count">
            ({{ node.data.allChildrenCount }})
          </span>

          <Button
            v-if="
              node.children &&
              node.data.directChildrenCount > node.children.length &&
              expandedKeys &&
              expandedKeys[node.key] === true
            "
            size="small"
            label="…"
            severity="secondary"
            outlined
            style="margin-left: 6px; padding: 0.1rem 0.3rem"
            @click="onTreeNodeExpand(node)"
          />
        </template>
      </Tree>

      <Button
        v-if="taxonomyTree && taxonomyTree.length === 1"
        size="small"
        label="…"
        severity="secondary"
        outlined
        style="margin-left: 1.7rem; margin-bottom: 1.5rem; padding: 0.1rem 0.3rem"
        @click="onTreeRootExpand"
      />
    </div>

    <div class="info-panel">
      <div v-if="showGraph" class="graph-panel-content">
        <div class="graph-panel-header">
          <h4 class="font-semibold text-lg">
            {{ selectedEntityInfo?.entity?.label ?? 'Ontology Graph' }}
          </h4>

          <Button
            label="Close graph"
            icon="pi pi-times"
            size="small"
            severity="secondary"
            outlined
            @click="closeGraph"
          />
        </div>

        <OntologyGraph
          :selected-iri="selectedIri"
          :selected-entity-info="selectedEntityInfo"
          :ontology-info="ontologyInfo"
          @select-entity="onSelectGraphEntity"
        />
      </div>

      <template v-else>
        <div class="info-panel-title flex flex-row flex-wrap place-content-between items-center gap-2">
          <h4 class="font-semibold text-lg">{{ infoPanelTitle }}</h4>

          <div class="info-panel-actions">
            <Button
              label="View graph"
              icon="pi pi-sitemap"
              size="small"
              severity="secondary"
              @click="openGraph"
            />

            <Clipboard v-if="selectedIri" v-slot="{ copy, copied }" :text="selectedIri">
              <Button
                :label="copied ? 'Copied IRI' : 'Copy IRI'"
                :icon="copied ? 'pi pi-check' : 'pi pi-clipboard'"
                size="small"
                outlined
                severity="secondary"
                @click="copy()"
              />
            </Clipboard>
          </div>
        </div>

        <div class="header-divider">
          <Divider layout="horizontal" align="center" />
        </div>

        <div class="info-fields">
          <template v-if="ontologyInfo && selectedIri === null">
            <div
              v-for="annotation in ontologyInfo.annotations"
              :key="`${annotation.property}-${annotation.value}`"
              class="info-field"
            >
              <h4 class="capitalize font-semibold">{{ annotation.property }}</h4>

              <a
                v-if="annotation.value.startsWith('http')"
                :href="annotation.value"
                target="_blank"
                rel="noopener noreferrer"
              >
                {{ annotation.value }}
              </a>

              <p v-else>{{ annotation.value }}</p>
            </div>
          </template>

          <template v-if="selectedEntityInfo">
            <div v-if="selectedEntityInfo.comment" class="info-field">
              <h4 class="capitalize font-semibold">Description</h4>
              <p>{{ selectedEntityInfo.comment }}</p>
            </div>

            <div
              v-if="
                selectedEntityInfo.entity.type === 'Class' &&
                selectedEntityInfo.classInfo?.equivalentClasses?.length
              "
              class="info-field"
            >
              <h4 class="capitalize font-semibold">Synonyms</h4>

              <a
                v-for="equivalentClass in selectedEntityInfo.classInfo.equivalentClasses"
                :key="equivalentClass.iri"
                :href="equivalentClass.iri"
                @click.prevent="onSelectEntity(equivalentClass.iri, equivalentClass.type)"
              >
                {{ equivalentClass.label }}
              </a>
            </div>

            <div
              v-if="selectedEntityInfo.annotations?.length"
              class="info-field"
            >
              <h4 class="capitalize font-semibold">Annotations</h4>

              <DataTable
                :value="selectedEntityInfo.annotations"
                striped-rows
                pt:root:class="mt-2"
              >
                <Column field="property" header="Property">
                  <template #body="{ data }">
                    <a :href="data.property.iri" class="font-medium" @click.prevent>
                      {{ data.property.label }}
                    </a>
                  </template>
                </Column>

                <Column field="values" header="Value(s)">
                  <template #body="{ data }">
                    <template v-for="(entity, index) in data.values" :key="`${entity.iri ?? entity.label}-${index}`">
                      <a
                        :href="entity.iri"
                        @click.prevent="
                          entity.type !== 'Datatype'
                            ? onSelectEntity(entity.iri, entity.type)
                            : undefined
                        "
                      >
                        {{ entity.label }}
                      </a>

                      <span v-if="index < data.values.length - 1" class="font-medium">, </span>
                    </template>
                  </template>
                </Column>
              </DataTable>
            </div>

            <template
              v-if="
                selectedEntityInfo.entity.type === 'Individual' &&
                selectedEntityInfo.individualInfo
              "
            >
              <div
                v-if="selectedEntityInfo.individualInfo.types?.length"
                class="info-field"
              >
                <h4 class="capitalize font-semibold">
                  Type{{ selectedEntityInfo.individualInfo.types.length > 1 ? 's' : '' }}
                </h4>

                <a
                  v-for="type in selectedEntityInfo.individualInfo.types"
                  :key="type.iri"
                  :href="type.iri"
                  @click.prevent="onSelectEntity(type.iri, type.type)"
                >
                  {{ type.label }}
                </a>
              </div>

              <div
                v-if="selectedEntityInfo.individualInfo.properties?.length"
                class="info-field"
              >
                <h4 class="capitalize font-semibold">Properties</h4>

                <DataTable
                  :value="selectedEntityInfo.individualInfo.properties"
                  striped-rows
                  pt:root:class="mt-2"
                >
                  <Column field="property" header="Property">
                    <template #body="{ data }">
                      <a
                        :href="data.property.iri"
                        class="font-medium"
                        @click.prevent="onSelectEntity(data.property.iri, 'Property')"
                      >
                        {{ data.property.label }}
                      </a>
                    </template>
                  </Column>

                  <Column field="values" header="Value(s)">
                    <template #body="{ data }">
                      <template v-for="(entity, index) in data.values" :key="`${entity.iri ?? entity.label}-${index}`">
                        <a
                          :href="entity.iri"
                          @click.prevent="
                            entity.type !== 'Datatype'
                              ? onSelectEntity(entity.iri, entity.type)
                              : undefined
                          "
                        >
                          {{ entity.label }}
                        </a>

                        <span v-if="index < data.values.length - 1" class="font-medium">, </span>
                      </template>
                    </template>
                  </Column>
                </DataTable>
              </div>
            </template>

            <template
              v-if="
                selectedEntityInfo.entity.type === 'Property' &&
                selectedEntityInfo.propertyInfo
              "
            >
              <div v-if="selectedEntityInfo.propertyInfo.domain" class="info-field">
                <h4 class="capitalize font-semibold">Domain</h4>
                <a
                  :href="selectedEntityInfo.propertyInfo.domain.iri"
                  @click.prevent="
                    onSelectEntity(
                      selectedEntityInfo.propertyInfo.domain.iri,
                      selectedEntityInfo.propertyInfo.domain.type,
                    )
                  "
                >
                  {{ selectedEntityInfo.propertyInfo.domain.label }}
                </a>
              </div>

              <div v-if="selectedEntityInfo.propertyInfo.range" class="info-field">
                <h4 class="capitalize font-semibold">Range</h4>
                <a
                  :href="selectedEntityInfo.propertyInfo.range.iri"
                  @click.prevent="
                    onSelectEntity(
                      selectedEntityInfo.propertyInfo.range.iri,
                      selectedEntityInfo.propertyInfo.range.type,
                    )
                  "
                >
                  {{ selectedEntityInfo.propertyInfo.range.label }}
                </a>
              </div>
            </template>
          </template>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.root {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 20px;
}

.taxonomy-tree-panel,
.info-panel {
  flex-grow: 1;
  overflow: hidden;
  flex-basis: 25em;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background-color: #ffffff;
}

.taxonomy-tree-panel a,
.info-panel a {
  color: var(--color-text);
}

.tree-header {
  display: flex;
  flex-flow: row wrap;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin: 1.5rem 2rem 0;
}

.tree-header h4,
.info-panel h4,
.info-panel p {
  padding: 0;
  margin: 0;
}

.selected-entity {
  color: var(--p-primary-color) !important;
  font-weight: bold;
}

.tree-children-count {
  margin-left: 2px;
  color: var(--p-text-muted-color);
}

.info-panel {
  min-height: 620px;
  text-wrap: wrap;
  overflow-wrap: break-word;
}

.info-panel-title {
  margin: 1.5rem 2rem 0;
}

.info-panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.header-divider {
  margin: 0 2rem;
}

.info-fields {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin: 0 2rem 1.5rem;
}

.info-field {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.graph-panel-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin: 1.5rem 2rem;
}

.graph-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

@media (max-width: 900px) {
  .taxonomy-tree-panel,
  .info-panel {
    flex-basis: 100%;
  }
}
</style>

<style>
.p-tree .p-tree-node-children {
  position: relative;
}

.p-tree .p-tree-node-children::before {
  content: "‎";
  position: absolute;
  width: 3px;
  height: 98%;
  border-right: 1px dotted #aaaaaa;
}

.p-tree .p-tree-container .p-tree-node:focus:not(:focus-visible) > .p-tree-node-content {
  outline: none;
}
</style>
