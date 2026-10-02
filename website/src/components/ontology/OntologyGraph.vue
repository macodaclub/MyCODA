<script setup>
import cytoscape from 'cytoscape'
import Button from 'primevue/button'
import {
  nextTick,
  onBeforeUnmount,
  ref,
  watch,
} from 'vue'
import { useOntologyStore } from '@/store'

const props = defineProps({
  selectedIri: {
    type: String,
    default: null,
  },

  selectedEntityInfo: {
    type: Object,
    default: null,
  },

  ontologyInfo: {
    type: Object,
    default: null,
  },

  depth: {
    type: Number,
    default: 1,
  },
})

const emit = defineEmits([
  'select-entity',
])

const ontologyStore = useOntologyStore()
const {
  fetchOntologyGraph,
} = ontologyStore

const graphContainer = ref(null)
const loading = ref(false)
const graphError = ref('')
const graphElements = ref([])

let cy = null

function destroyGraph() {
  if (cy) {
    cy.destroy()
    cy = null
  }
}

function fitGraph() {
  cy?.fit(undefined, 40)
}

function buildOntologyRootElements() {
  const ontologyIri =
    props.ontologyInfo?.annotations
      ?.find(annotation =>
        String(annotation.property)
          .toLowerCase()
          .includes('iri'),
      )
      ?.value ??
    'MYCODA_ONTOLOGY'

  return [
    {
      group: 'nodes',
      data: {
        id: ontologyIri,
        iri: ontologyIri,
        label: 'MyCODA Ontology',
        type: 'Ontology',
        selected: true,
      },
    },
  ]
}

async function renderGraph() {
  await nextTick()

  if (!graphContainer.value) {
    return
  }

  destroyGraph()

  cy = cytoscape({
    container: graphContainer.value,

    elements: graphElements.value,

    layout: {
      name: 'cose',
      animate: false,
      fit: true,
      padding: 40,
      nodeRepulsion: 8000,
      idealEdgeLength: 140,
    },

    style: [
      {
        selector: 'node',
        style: {
          width: 48,
          height: 48,
          label: 'data(label)',
          'text-wrap': 'wrap',
          'text-max-width': 145,
          'text-valign': 'bottom',
          'text-margin-y': 8,
          'font-size': 11,
          color: '#0f172a',
          'background-color': '#94a3b8',
          'border-width': 2,
          'border-color': '#64748b',
        },
      },

      {
        selector: 'node[type = "Ontology"]',
        style: {
          shape: 'round-rectangle',
          width: 80,
          height: 50,
          'background-color': '#f97316',
          'border-color': '#ea580c',
        },
      },

      {
        selector: 'node[type = "Class"]',
        style: {
          shape: 'round-rectangle',
          'background-color': '#fdba74',
          'border-color': '#f97316',
        },
      },

      {
        selector: 'node[type = "Individual"]',
        style: {
          shape: 'ellipse',
          'background-color': '#bfdbfe',
          'border-color': '#3b82f6',
        },
      },

      {
        selector: 'node[type = "Property"]',
        style: {
          shape: 'diamond',
          'background-color': '#ddd6fe',
          'border-color': '#8b5cf6',
        },
      },

      {
        selector: 'node[type = "Datatype"]',
        style: {
          shape: 'rectangle',
          'background-color': '#d1fae5',
          'border-color': '#10b981',
        },
      },

      {
        selector: 'node[type = "Annotation"]',
        style: {
          shape: 'hexagon',
          'background-color': '#fde68a',
          'border-color': '#ca8a04',
        },
      },

      {
        selector: 'node[selected = true]',
        style: {
          width: 64,
          height: 64,
          'border-width': 4,
          'border-color': '#ea580c',
          'background-color': '#fb923c',
          'font-weight': 'bold',
        },
      },

      {
        selector: 'edge',
        style: {
          width: 1.5,
          label: 'data(label)',
          'font-size': 9,
          color: '#475569',
          'text-rotation': 'autorotate',
          'text-background-color': '#ffffff',
          'text-background-opacity': 0.9,
          'text-background-padding': 3,
          'line-color': '#94a3b8',
          'target-arrow-color': '#94a3b8',
          'target-arrow-shape': 'triangle',
          'curve-style': 'bezier',
        },
      },

      {
        selector: 'edge[label = "subClassOf"]',
        style: {
          'line-style': 'solid',
        },
      },

      {
        selector: 'edge[label = "rdf:type"]',
        style: {
          'line-style': 'dashed',
        },
      },

      {
        selector: 'node:selected',
        style: {
          'overlay-opacity': 0,
          'border-width': 4,
          'border-color': '#0f172a',
        },
      },
    ],
  })

  cy.on('tap', 'node', event => {
    const data = event.target.data()

    if (
      data.iri &&
      data.iri !== props.selectedIri &&
      data.type !== 'Datatype' &&
      data.type !== 'Annotation'
    ) {
      emit(
        'select-entity',
        data.iri,
        data.type,
      )
    }
  })

  fitGraph()
}

async function loadGraph() {
  graphError.value = ''

  if (!props.selectedIri) {
    graphElements.value =
      buildOntologyRootElements()

    await renderGraph()
    return
  }

  const entityType =
    props.selectedEntityInfo?.entity?.type

  if (!entityType) {
    graphElements.value = []
    destroyGraph()
    return
  }

  loading.value = true

  try {
    const response =
      await fetchOntologyGraph(
        props.selectedIri,
        entityType,
        props.depth,
      )

    graphElements.value = [
      ...(response?.nodes ?? []),
      ...(response?.edges ?? []),
    ]

    await renderGraph()
  } catch (error) {
    graphElements.value = []
    destroyGraph()

    graphError.value =
      error?.message ??
      'Failed to load ontology graph.'
  } finally {
    loading.value = false
  }
}

watch(
  () => [
    props.selectedIri,
    props.selectedEntityInfo?.entity?.type,
    props.depth,
  ],
  loadGraph,
  {
    immediate: true,
  },
)

onBeforeUnmount(destroyGraph)

defineExpose({
  loadGraph,
  renderGraph,
  fitGraph,
})
</script>

<template>
  <div class="ontology-graph-wrapper">
    <div class="ontology-graph-toolbar">
      <div class="ontology-graph-title">
        <strong>
          {{
            selectedEntityInfo?.entity?.label ??
            'MyCODA Ontology'
          }}
        </strong>

        <small v-if="selectedEntityInfo?.entity?.type">
          {{ selectedEntityInfo.entity.type }}
        </small>
      </div>

      <Button
        label="Fit graph"
        icon="pi pi-expand"
        size="small"
        severity="secondary"
        outlined
        :disabled="loading || graphElements.length === 0"
        @click="fitGraph"
      />
    </div>

    <div
      v-if="loading"
      class="ontology-graph-state"
    >
      <i class="pi pi-spin pi-spinner" />
      <span>Loading graph...</span>
    </div>

    <div
      v-else-if="graphError"
      class="ontology-graph-error"
    >
      {{ graphError }}
    </div>

    <div
      v-show="!loading && !graphError"
      ref="graphContainer"
      class="ontology-graph"
    />

    <p class="ontology-graph-help">
      Select a node to navigate to that ontology entity.
    </p>
  </div>
</template>

<style scoped>
.ontology-graph-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  width: 100%;
}

.ontology-graph-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.ontology-graph-title {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.ontology-graph-title strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ontology-graph-title small {
  color: var(--p-text-muted-color);
}

.ontology-graph,
.ontology-graph-state,
.ontology-graph-error {
  width: 100%;
  height: 560px;
  min-height: 420px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #f8fafc;
}

.ontology-graph-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.65rem;
  color: var(--p-text-muted-color);
}

.ontology-graph-error {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
  color: #991b1b;
  background: #fef2f2;
  text-align: center;
}

.ontology-graph-help {
  margin: 0;
  color: var(--p-text-muted-color);
  font-size: 0.85rem;
}
</style>
