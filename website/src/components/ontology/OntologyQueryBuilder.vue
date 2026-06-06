<script setup>
import { computed, ref } from 'vue'
import SelectButton from 'primevue/selectbutton'

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
import { validateQuery } from '../services/ontologyQueryValidationService'


const props = defineProps({
  entities: {
    type: [Array, Object],
    default: () => []
  }
})

// State
const invalidRowIndex = ref(null)
const queryRows = ref([])
const generatedQuery = ref('')
const selectedPredefinedQuery = ref(null)

const queryResults = ref([])
const isRunningQuery = ref(false)
const queryError = ref(null)

const copyMessage = ref('')
const hasExecutedQuery = ref(false)

const queryResultCount = ref(null)
const executedQuery = ref('')

const queryMode = ref('builder')
const manualQuery = ref('')

const queryModeOptions = [
  {
    label: 'Visual builder',
    value: 'builder'
  },
  {
    label: 'Advanced SQWRL',
    value: 'advanced'
  }
]

// Computed

const canAddClassRow = computed(() => {
  return (
    canUseQueryBuilder.value &&
    normalizeEntityType(lastQueryRow.value?.entitytype) === ENTITY_TYPES.OBJECT_PROPERTY
  )
})

const canAddObjectPropertyRow = computed(() => {
  return canUseQueryBuilder.value && hasClassRow.value
})

const lastQueryRow = computed(() => {
  if (!queryRows.value.length) {
    return null
  }

  return queryRows.value[queryRows.value.length - 1]
})

const hasClassRow = computed(() => {
  return queryRows.value.some(row =>
    normalizeEntityType(row.entitytype) === ENTITY_TYPES.CLASS
  )
})

const canAddDataPropertyRow = computed(() => {
  return canUseQueryBuilder.value && hasClassRow.value
})

const canAddFilterRows = computed(() => {
  return (
    canUseQueryBuilder.value &&
    normalizeEntityType(lastQueryRow.value?.entitytype) === ENTITY_TYPES.DATATYPE_PROPERTY
  )
})

const canAddIndividualRow = computed(() => {
  return (
    canUseQueryBuilder.value &&
    normalizeEntityType(lastQueryRow.value?.entitytype) === ENTITY_TYPES.OBJECT_PROPERTY
  )
})

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

async function executeQuery() {
  queryError.value = null
  queryResults.value = []
  hasExecutedQuery.value = false
  queryResultCount.value = null
  executedQuery.value = ''

  let queryToExecute = ''

  if (queryMode.value === 'builder') {
    if (!generatedQuery.value) {
      generateQuery()
    }

    if (!generatedQuery.value || queryError.value) {
      return
    }

    queryToExecute = generatedQuery.value
  } else {
    queryToExecute = manualQuery.value?.trim()

    if (!queryToExecute) {
      queryError.value = 'Write a SQWRL query before running it.'
      return
    }
  }

  try {
    isRunningQuery.value = true

    const result = await executeOntologyQuery(queryToExecute)

    queryResults.value = result.results
    queryError.value = result.error
    queryResultCount.value = result.count
    executedQuery.value = result.executedQuery
    hasExecutedQuery.value = true
  } finally {
    isRunningQuery.value = false
  }
}


function moveRowUp(row) {
  const index = queryRows.value.findIndex(item => item.id === row.id)

  if (index <= 0) {
    return
  }

  const rows = [...queryRows.value]
  const previousRow = rows[index - 1]

  rows[index - 1] = rows[index]
  rows[index] = previousRow

  queryRows.value = rows
  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
}

function moveRowDown(row) {
  const index = queryRows.value.findIndex(item => item.id === row.id)

  if (index === -1 || index >= queryRows.value.length - 1) {
    return
  }

  const rows = [...queryRows.value]
  const nextRow = rows[index + 1]

  rows[index + 1] = rows[index]
  rows[index] = nextRow

  queryRows.value = rows
  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
}

function addIndividualRow() {
  const newRow = createEmptyQueryRow()

  newRow.entitytype = ENTITY_TYPES.INDIVIDUAL

  queryRows.value.push(newRow)
  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
}

function addObjectPropertyRow() {
  const newRow = createEmptyQueryRow()

  newRow.entitytype = ENTITY_TYPES.OBJECT_PROPERTY

  queryRows.value.push(newRow)
  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
}

function addClassRow() {
  const newRow = createEmptyQueryRow()

  newRow.entitytype = ENTITY_TYPES.CLASS

  queryRows.value.push(newRow)
  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
}

function addDataPropertyRow() {
  const newRow = createEmptyQueryRow()

  newRow.entitytype = ENTITY_TYPES.DATATYPE_PROPERTY

  queryRows.value.push(newRow)
  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
}

function addFilterRows() {
  queryRows.value.push({
    ...createEmptyQueryRow(),
    entitytype: ENTITY_TYPES.RELATIONAL_OPERATOR
  })

  queryRows.value.push({
    ...createEmptyQueryRow(),
    entitytype: ENTITY_TYPES.LITERAL
  })

  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
}

function resetGeneratedQueryState() {
  generatedQuery.value = ''
  queryResults.value = []
  queryError.value = null
  copyMessage.value = ''
  hasExecutedQuery.value = false
  invalidRowIndex.value = null
  queryResultCount.value = null
  executedQuery.value = ''
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

  newRow.entitytype = suggestNextEntityType()

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
  manualQuery.value = ''
  queryMode.value = 'builder'

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
  const validationError = validateQuery(queryRows.value)

  if (validationError) {
    generatedQuery.value = ''
    queryResults.value = []
    queryError.value = validationError.message
    invalidRowIndex.value = validationError.rowIndex
    return
  }

  const result = buildOntologyQuery(queryRows.value)

  generatedQuery.value = result.query
  queryError.value = result.error
  invalidRowIndex.value = null
}

const hasQueryRows = computed(() => queryRows.value.length > 0)

const canGenerateQuery = computed(() => {
  return canUseQueryBuilder.value && hasQueryRows.value
})

const canRunQuery = computed(() => {
  if (isRunningQuery.value) {
    return false
  }

  if (queryMode.value === 'builder') {
    return Boolean(generatedQuery.value) && !queryError.value
  }

  return Boolean(manualQuery.value?.trim())
})

const canClearQuery = computed(() => {
  return (
    queryRows.value.length > 0 ||
    generatedQuery.value ||
    manualQuery.value ||
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

function isFirstRow(row) {
  return queryRows.value.findIndex(item => item.id === row.id) === 0
}

function isLastRow(row) {
  return queryRows.value.findIndex(item => item.id === row.id) === queryRows.value.length - 1
}

function suggestNextEntityType() {
  const lastRow = queryRows.value[queryRows.value.length - 1]

  if (!lastRow?.entitytype) {
    return ENTITY_TYPES.CLASS
  }

  const lastType = normalizeEntityType(lastRow.entitytype)

  switch (lastType) {
    case ENTITY_TYPES.CLASS:
      return ENTITY_TYPES.OBJECT_PROPERTY

    case ENTITY_TYPES.OBJECT_PROPERTY:
      return ENTITY_TYPES.CLASS

    case ENTITY_TYPES.DATATYPE_PROPERTY:
      return ENTITY_TYPES.RELATIONAL_OPERATOR

    case ENTITY_TYPES.RELATIONAL_OPERATOR:
      return ENTITY_TYPES.LITERAL

    case ENTITY_TYPES.LITERAL:
      return ENTITY_TYPES.CLASS

    case ENTITY_TYPES.INDIVIDUAL:
      return ENTITY_TYPES.OBJECT_PROPERTY

    default:
      return ENTITY_TYPES.CLASS
  }
}

function getEntityPlaceholder(row) {
  const type = normalizeEntityType(row.entitytype)

  switch (type) {
    case ENTITY_TYPES.CLASS:
      return 'Select a class'

    case ENTITY_TYPES.OBJECT_PROPERTY:
      return 'Select an object property'

    case ENTITY_TYPES.DATATYPE_PROPERTY:
      return 'Select a data property'

    case ENTITY_TYPES.INDIVIDUAL:
      return 'Select an individual'

    case ENTITY_TYPES.RELATIONAL_OPERATOR:
      return 'Select an operator'

    default:
      return 'Select entity'
  }
}

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


function getRowClass(data) {
  const index = queryRows.value.findIndex(row => row.id === data.id)

  return {
    'invalid-query-row': invalidRowIndex.value === index
  }
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

        <div class="action-group">
          <Button label="New query" icon="pi pi-file" severity="secondary" outlined :disabled="!canUseQueryBuilder"
            @click="startNewQuery" />

          <Button label="Clear" icon="pi pi-trash" severity="secondary" outlined :disabled="!canClearQuery"
            @click="clearQuery" />
        </div>

        <div class="action-group">
          <Button label="Add row" icon="pi pi-plus" :disabled="!canUseQueryBuilder" @click="addQueryRow" />

          <Button label="Object property" icon="pi pi-share-alt" severity="secondary" outlined
            :disabled="!canAddObjectPropertyRow" @click="addObjectPropertyRow" />

          <Button label="Class" icon="pi pi-box" severity="secondary" outlined :disabled="!canAddClassRow"
            @click="addClassRow" />

          <Button label="Individual" icon="pi pi-user" severity="secondary" outlined :disabled="!canAddIndividualRow"
            @click="addIndividualRow" />

          <Button label="Data property" icon="pi pi-database" severity="secondary" outlined
            :disabled="!canAddDataPropertyRow" @click="addDataPropertyRow" />

          <Button label="Filter" icon="pi pi-filter" severity="secondary" outlined :disabled="!canAddFilterRows"
            @click="addFilterRows" />
        </div>
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

    <div class="query-table-section">
      <div class="query-table-header">
        <h3>Query construction</h3>
        <p>
          Build the query step by step using classes, properties, individuals, operators and literals.
        </p>
      </div>

      <DataTable :value="queryRows" dataKey="id" responsiveLayout="scroll" stripedRows showGridlines
        :rowClass="getRowClass" emptyMessage="No query rows added yet. Click New query or Add row to start.">
        <Column header="#" style="width: 70px">
          <template #body="{ index }">
            {{ index + 1 }}
          </template>
        </Column>
        <Column header="Entity Type">
          <template #body="{ data }">
            <Select v-model="data.entitytype" :options="entityTypeOptions" optionLabel="label" optionValue="value"
              placeholder="Select type" class="w-full" @change="onEntityTypeChange(data)" />
          </template>
        </Column>

        <Column header="Entity">
          <template #body="{ data }">
            <InputText v-if="normalizeEntityType(data.entitytype) === ENTITY_TYPES.LITERAL" v-model="data.entity"
              placeholder="Insert literal value" class="w-full" @input="resetGeneratedQueryState" />

            <Select v-else v-model="data.entity" :options="getEntityOptions(data)" optionLabel="label"
              optionValue="value" :placeholder="getEntityPlaceholder(data)" filter class="w-full"
              :disabled="!data.entitytype || !hasEntities" @change="resetGeneratedQueryState" />
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

        <Column header="Actions" style="width: 150px">
          <template #body="{ data }">
            <div class="row-actions">
              <Button icon="pi pi-arrow-up" severity="secondary" text rounded :disabled="isFirstRow(data)"
                @click="moveRowUp(data)" />

              <Button icon="pi pi-arrow-down" severity="secondary" text rounded :disabled="isLastRow(data)"
                @click="moveRowDown(data)" />

              <Button icon="pi pi-times" severity="danger" text rounded @click="removeQueryRow(data)" />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>
    <div class="query-actions-section">
      <div class="query-actions-header">
        <h3>Build and execute</h3>
        <p>
          Generate the SQWRL query and run it against the ontology.
        </p>
      </div>
      <div class="generate-actions">
        <Button label="Generate query" icon="pi pi-code" severity="secondary" outlined
          :disabled="queryMode !== 'builder' || !canGenerateQuery" @click="generateQuery" />

        <Button label="Run query" icon="pi pi-play" :loading="isRunningQuery" :disabled="!canRunQuery"
          @click="executeQuery" />
      </div>
    </div>

    <div class="query-mode-selector">
      <SelectButton v-model="queryMode" :options="queryModeOptions" optionLabel="label" optionValue="value" />
    </div>

    <div class="generated-query-section">
      <div v-if="queryMode === 'builder'">
        <h3>Generated query</h3>

        <Textarea v-model="generatedQuery" readonly rows="5" class="w-full"
          placeholder="Generated query will appear here..." />
      </div>

      <div v-else>
        <h3>Advanced SQWRL query</h3>

        <Textarea v-model="manualQuery" rows="6" class="w-full" placeholder="Write your SQWRL query here..." />

        <small>
          Example: owl:Thing(?x) -> sqwrl:select(?x)
        </small>
      </div>

      <Message v-if="copyMessage" severity="success" :closable="false">
        {{ copyMessage }}
      </Message>
    </div>

    <Message v-if="queryError" severity="error" :closable="false">
      {{ queryError }}
    </Message>

    <div v-if="hasExecutedQuery || queryResults.length" class="query-results-section">
      <h3>Query results</h3>

      <Message v-if="hasExecutedQuery && !queryError" severity="info" :closable="false">
        Query executed successfully. Results found: {{ queryResultCount ?? queryResults.length }}.
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
  flex-direction: column;
  gap: 0.75rem;
  align-items: flex-end;
}

.action-group {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  justify-content: flex-end;
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
    align-items: stretch;
  }

  .action-group {
    justify-content: flex-start;
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

.query-table-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.query-table-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}

.query-table-header p {
  margin: 0.25rem 0 0;
  color: var(--text-color-secondary);
  font-size: 0.9rem;
}

.query-actions-section {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.query-actions-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}

.query-actions-header p {
  margin: 0.25rem 0 0;
  color: var(--text-color-secondary);
  font-size: 0.9rem;
}

.row-actions {
  display: flex;
  gap: 0.25rem;
  justify-content: center;
  align-items: center;
}

:deep(.invalid-query-row) {
  background: #ffe4e6;
}
</style>