<script setup>
import { computed, ref } from 'vue'

import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import Checkbox from 'primevue/checkbox'
import Textarea from 'primevue/textarea'
import Message from 'primevue/message'
import { ENTITY_TYPES, entityTypeOptions, relationalOperatorOptions } from '../data/entityTypes'
import { predefinedQueryOptions, predefinedQueries } from '../data/predefinedQueries'
import { normalizeEntityType, buildEntityOptionsByType } from '../services/ontologyEntityUtils'
import { buildOntologyQuery } from '../services/ontologyQueryBuilderService'
import { executeOntologyQuery } from '../services/ontologyQueryRunnerService'
import { useOntologyEntities } from '../composables/useOntologyEntities'
import { createEmptyQueryRow, createQueryRowFromPredefined } from '../services/ontologyQueryRowService'


const props = defineProps({
  entities: {
    type: [Array, Object],
    default: () => []
  }
})

// State
const queryRows = ref([])
const generatedQuery = ref('')
const selectedPredefinedQuery = ref(null)

const queryResults = ref([])
const isRunningQuery = ref(false)
const queryError = ref(null)

const copyMessage = ref('')
const hasExecutedQuery = ref(false)

// Computed
const propsEntities = computed(() => {
  if (Array.isArray(props.entities)) {
    return props.entities
  }

  if (Array.isArray(props.entities?.entities)) {
    return props.entities.entities
  }

  return []
})

const {
  normalizedEntities,
  hasEntities,
  isLoadingEntities,
  entitiesError
} = useOntologyEntities(propsEntities)

const filteredEntitiesByType = computed(() => {
  return {
    [ENTITY_TYPES.CLASS]: buildEntityOptionsByType(
      normalizedEntities.value,
      ENTITY_TYPES.CLASS
    ),
    [ENTITY_TYPES.INDIVIDUAL]: buildEntityOptionsByType(
      normalizedEntities.value,
      ENTITY_TYPES.INDIVIDUAL
    ),
    [ENTITY_TYPES.DATATYPE_PROPERTY]: buildEntityOptionsByType(
      normalizedEntities.value,
      ENTITY_TYPES.DATATYPE_PROPERTY
    ),
    [ENTITY_TYPES.OBJECT_PROPERTY]: buildEntityOptionsByType(
      normalizedEntities.value,
      ENTITY_TYPES.OBJECT_PROPERTY
    ),
    [ENTITY_TYPES.RELATIONAL_OPERATOR]: relationalOperatorOptions
  }
})

// Actions
function resetGeneratedQueryState() {
  generatedQuery.value = ''
  queryResults.value = []
  queryError.value = null
  copyMessage.value = ''
  hasExecutedQuery.value = false
}

function startNewQuery() {
  queryRows.value = [
    {
      ...createEmptyQueryRow(),
      entitytype: ENTITY_TYPES.CLASS
    }
  ]

  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
}

function addQueryRow() {
  const newRow = createEmptyQueryRow()

  if (!queryRows.value.length) {
    newRow.entitytype = ENTITY_TYPES.CLASS
  }

  queryRows.value.push(newRow)
  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
}

function removeQueryRow(row) {
  queryRows.value = queryRows.value.filter(item => item.id !== row.id)
  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
}

function clearQuery() {
  queryRows.value = []
  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
}

function loadPredefinedQuery() {
  if (!selectedPredefinedQuery.value) {
    return
  }

  const selectedRows = predefinedQueries[selectedPredefinedQuery.value] ?? []

  queryRows.value = selectedRows.map(createQueryRowFromPredefined)

  resetGeneratedQueryState()
}

function generateQuery() {
  const validationError = validateQueryRows()

  if (validationError) {
    generatedQuery.value = ''
    queryResults.value = []
    queryError.value = validationError
    return
  }

  const result = buildOntologyQuery(queryRows.value)

  generatedQuery.value = result.query
  queryError.value = result.error
}

async function executeQuery() {
  queryError.value = null
  queryResults.value = []

  if (!generatedQuery.value) {
    generateQuery()
  }

  if (!generatedQuery.value) {
    return
  }

  try {
    isRunningQuery.value = true

    const result = await executeOntologyQuery(generatedQuery.value)

    queryResults.value = result.results
    queryError.value = result.error
    hasExecutedQuery.value = true
  } finally {
    isRunningQuery.value = false
  }
}

const hasQueryRows = computed(() => queryRows.value.length > 0)

const canGenerateQuery = computed(() => {
  return canUseQueryBuilder.value && hasQueryRows.value
})

const canRunQuery = computed(() => {
  return Boolean(
    generatedQuery.value &&
    !queryError.value &&
    !isRunningQuery.value
  )
})

const canClearQuery = computed(() => {
  return (
    queryRows.value.length > 0 ||
    generatedQuery.value ||
    queryResults.value.length > 0 ||
    queryError.value
  )
})

const entityCounts = computed(() => {
  return {
    classes: normalizedEntities.value.filter(
      entity => entity.type === ENTITY_TYPES.CLASS
    ).length,

    individuals: normalizedEntities.value.filter(
      entity => entity.type === ENTITY_TYPES.INDIVIDUAL
    ).length,

    objectProperties: normalizedEntities.value.filter(
      entity => entity.type === ENTITY_TYPES.OBJECT_PROPERTY
    ).length,

    datatypeProperties: normalizedEntities.value.filter(
      entity => entity.type === ENTITY_TYPES.DATATYPE_PROPERTY
    ).length
  }
})

async function copyGeneratedQuery() {
  if (!generatedQuery.value) {
    return
  }

  try {
    await navigator.clipboard.writeText(generatedQuery.value)

    copyMessage.value = 'Query copied to clipboard.'

    setTimeout(() => {
      copyMessage.value = ''
    }, 2500)
  } catch (error) {
    copyMessage.value = 'Unable to copy query.'
  }
}

const canUseQueryBuilder = computed(() => {
  return hasEntities.value && !isLoadingEntities.value && !entitiesError.value
})

// Helpers


function getEntityOptions(row) {
  const normalizedType = normalizeEntityType(row.entitytype)

  if (!normalizedType || normalizedType === ENTITY_TYPES.LITERAL) {
    return []
  }

  return filteredEntitiesByType.value[normalizedType] ?? []
}

function onEntityTypeChange(row) {
  row.entitytype = normalizeEntityType(row.entitytype)
  row.entity = null

  if (!canUseInOutput(row)) {
    row.inoutput = false
  }

  if (!canUseOrderedBy(row)) {
    row.orderedby = false
  }

  resetGeneratedQueryState()
}

function canUseInOutput(row) {
  const type = normalizeEntityType(row.entitytype)

  return [
    ENTITY_TYPES.CLASS,
    ENTITY_TYPES.DATATYPE_PROPERTY,
    ENTITY_TYPES.OBJECT_PROPERTY
  ].includes(type)
}

function canUseOrderedBy(row) {
  const type = normalizeEntityType(row.entitytype)

  return [
    ENTITY_TYPES.CLASS,
    ENTITY_TYPES.DATATYPE_PROPERTY,
    ENTITY_TYPES.OBJECT_PROPERTY
  ].includes(type)
}

function validateQueryRows() {
  if (!queryRows.value.length) {
    return 'Add at least one query row.'
  }

  const incompleteRowIndex = queryRows.value.findIndex(row => {
    return !row.entitytype || row.entity === null || row.entity === undefined || row.entity === ''
  })

  if (incompleteRowIndex !== -1) {
    return `Row ${incompleteRowIndex + 1} is incomplete. Select an Entity Type and an Entity.`
  }

  const hasOutput = queryRows.value.some(row => row.inoutput)

  if (!hasOutput) {
    return 'Select at least one field as In Output.'
  }

  return null
}
</script>

<template>
  <div class="query-builder-section">
    <div class="section-header">
      <div>
        <p>
          Create ontology queries by selecting classes, properties, individuals,
          operators and literals.
        </p>
      </div>

      <div class="actions">
        <Select v-model="selectedPredefinedQuery" :options="predefinedQueryOptions" optionLabel="label"
          optionValue="value" placeholder="Select a predefined query" class="predefined-query-select"
          :disabled="!canUseQueryBuilder" @change="loadPredefinedQuery" />
        <Button label="New query" icon="pi pi-file" severity="secondary" outlined :disabled="!canUseQueryBuilder"
          @click="startNewQuery" />

        <Button label="Add row" icon="pi pi-plus" :disabled="!canUseQueryBuilder" @click="addQueryRow" />

        <Button label="Clear" icon="pi pi-trash" severity="secondary" outlined :disabled="!canClearQuery"
          @click="clearQuery" />
      </div>
    </div>



    <Message v-if="isLoadingEntities" severity="info" :closable="false">
      Loading ontology entities...
    </Message>

    <Message v-if="entitiesError" severity="error" :closable="false">
      {{ entitiesError }}
    </Message>

    <Message v-if="!isLoadingEntities && !entitiesError && !hasEntities" severity="warn" :closable="false">
      No ontology entities were loaded. Check if the backend endpoint is running and if the entities are being passed to
      the Query Builder.
    </Message>

    <div v-if="hasEntities" class="entities-summary">
      <span>
        Classes: <strong>{{ entityCounts.classes }}</strong>
      </span>

      <span>
        Individuals: <strong>{{ entityCounts.individuals }}</strong>
      </span>

      <span>
        Object Properties: <strong>{{ entityCounts.objectProperties }}</strong>
      </span>

      <span>
        Data Properties: <strong>{{ entityCounts.datatypeProperties }}</strong>
      </span>
    </div>

    <DataTable :value="queryRows" dataKey="id" responsiveLayout="scroll" stripedRows showGridlines
      emptyMessage="No query rows added yet. Click New query or Add row to start.">
      <Column header="Entity">
        <template #body="{ data }">
          <InputText v-if="normalizeEntityType(data.entitytype) === ENTITY_TYPES.LITERAL" v-model="data.entity"
            placeholder="Insert literal value" class="w-full" @input="resetGeneratedQueryState" />

          <Select v-else v-model="data.entity" :options="getEntityOptions(data)" optionLabel="label" optionValue="value"
            placeholder="Select entity" filter class="w-full" :disabled="!data.entitytype || !hasEntities"
            @change="resetGeneratedQueryState" />
        </template>
      </Column>

      <Column header="Entity">
        <template #body="{ data }">
          <InputText v-if="normalizeEntityType(data.entitytype) === ENTITY_TYPES.LITERAL" v-model="data.entity"
            placeholder="Insert literal value" class="w-full" @input="resetGeneratedQueryState" />

          <Select v-else v-model="data.entity" :options="getEntityOptions(data)" optionLabel="label" optionValue="value"
            placeholder="Select entity" filter class="w-full" :disabled="!data.entitytype || !hasEntities"
            @change="resetGeneratedQueryState" />
        </template>
      </Column>

      <Column header="In Output">
        <template #body="{ data }">
          <Checkbox v-model="data.inoutput" binary :disabled="!canUseInOutput(data)"
            @change="resetGeneratedQueryState" />
        </template>
      </Column>

      <Column header="Ordered By">
        <template #body="{ data }">
          <Checkbox v-model="data.orderedby" binary :disabled="!canUseOrderedBy(data)"
            @change="resetGeneratedQueryState" />
        </template>
      </Column>

      <Column header="Actions">
        <template #body="{ data }">
          <Button icon="pi pi-times" severity="danger" text rounded @click="removeQueryRow(data)" />
        </template>
      </Column>
    </DataTable>

    <div class="generate-actions">
      <Button label="Generate query" icon="pi pi-code" severity="secondary" outlined :disabled="!canGenerateQuery"
        @click="generateQuery" />

      <Button label="Run query" icon="pi pi-play" :loading="isRunningQuery" :disabled="!canRunQuery"
        @click="executeQuery" />
    </div>
    <div class="generated-query-section">
      <div class="generated-query-header">
        <h3>Generated query</h3>

        <Button v-if="generatedQuery" label="Copy" icon="pi pi-copy" severity="secondary" outlined size="small"
          @click="copyGeneratedQuery" />
      </div>

      <Textarea v-model="generatedQuery" rows="4" autoResize readonly class="w-full generated-query"
        placeholder="Generated query will appear here..." />

      <Message v-if="copyMessage" severity="success" :closable="false">
        {{ copyMessage }}
      </Message>
    </div>

    <Message v-if="queryError" severity="error" :closable="false">
      {{ queryError }}
    </Message>

    <div v-if="hasExecutedQuery || queryResults.length" class="query-results-section">
      <h3>Query results</h3>

      <Message v-if="hasExecutedQuery && !queryError && !queryResults.length" severity="info" :closable="false">
        The query executed successfully, but no results were returned.
      </Message>

      <DataTable v-if="queryResults.length" :value="queryResults" paginator :rows="10" stripedRows showGridlines
        responsiveLayout="scroll">
        <Column v-for="column in Object.keys(queryResults[0] ?? {})" :key="column" :field="column" :header="column"
          sortable />
      </DataTable>
    </div>
  </div>
</template>

<style scoped>
.query-builder-section {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
}

.section-header h2 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 700;
}

.section-header p {
  margin: 0.25rem 0 0;
  color: var(--text-color-secondary);
}

.actions {
  display: flex;
  gap: 0.75rem;
}

.generate-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
}

.generated-query {
  font-family: monospace;
}

.predefined-query-select {
  min-width: 420px;
}

@media (max-width: 900px) {
  .section-header {
    flex-direction: column;
  }

  .actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .predefined-query-select {
    min-width: 100%;
  }
}

.entities-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  border: 1px solid var(--surface-border);
  border-radius: 12px;
  background: var(--surface-card);
  color: var(--text-color-secondary);
  font-size: 0.9rem;
}

.entities-summary strong {
  color: var(--text-color);
}

.generated-query-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.generated-query-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.generated-query-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}
.query-results-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.query-results-section h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}
</style>