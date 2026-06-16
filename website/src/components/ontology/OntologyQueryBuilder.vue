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

/**
 * State
 */
const invalidRowIndex = ref(null)
const queryRows = ref([])

const generatedQuery = ref('')
const executableQuery = ref('')
const manualQuery = ref('')

const selectedPredefinedQuery = ref(null)
const queryMode = ref('builder')

const queryResults = ref([])
const isRunningQuery = ref(false)
const queryError = ref(null)

const copyMessage = ref('')
const hasExecutedQuery = ref(false)

const queryResultCount = ref(null)
const executedQuery = ref('')

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

/**
 * Entities
 */
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

const filteredEntitiesByType = computed(() => ({
  [ENTITY_TYPES.CLASS]: [
    {
      label: 'owl:Thing',
      value: 'owl:Thing'
    },
    ...buildEntityOptionsByType(normalizedEntities.value, ENTITY_TYPES.CLASS)
  ],
  [ENTITY_TYPES.INDIVIDUAL]: buildEntityOptionsByType(normalizedEntities.value, ENTITY_TYPES.INDIVIDUAL),
  [ENTITY_TYPES.DATATYPE_PROPERTY]: buildEntityOptionsByType(normalizedEntities.value, ENTITY_TYPES.DATATYPE_PROPERTY),
  [ENTITY_TYPES.OBJECT_PROPERTY]: buildEntityOptionsByType(normalizedEntities.value, ENTITY_TYPES.OBJECT_PROPERTY),
  [ENTITY_TYPES.RELATIONAL_OPERATOR]: relationalOperatorOptions
}))

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

/**
 * Builder state
 */
const lastQueryRow = computed(() => {
  if (!queryRows.value.length) {
    return null
  }

  return queryRows.value[queryRows.value.length - 1]
})

const hasQueryRows = computed(() => {
  return queryRows.value.length > 0
})

const canUseQueryBuilder = computed(() => {
  return hasEntities.value && !isLoadingEntities.value && !entitiesError.value
})

const canGenerateQuery = computed(() => {
  return canUseQueryBuilder.value && hasQueryRows.value
})

const canRunQuery = computed(() => {
  if (isRunningQuery.value) {
    return false
  }

  if (queryMode.value === 'builder') {
    return Boolean(executableQuery.value) && !queryError.value
  }

  return Boolean(manualQuery.value?.trim())
})

const canClearQuery = computed(() => {
  return (
    queryRows.value.length > 0 ||
    generatedQuery.value ||
    executableQuery.value ||
    manualQuery.value ||
    queryResults.value.length > 0 ||
    queryError.value
  )
})

/**
 * Add row buttons
 */
const canAddObjectPropertyRow = computed(() => {
  return (
    canUseQueryBuilder.value &&
    isLastRowType(ENTITY_TYPES.CLASS) &&
    isRowFilled(lastQueryRow.value)
  )
})

const canAddDataPropertyRow = computed(() => {
  return (
    canUseQueryBuilder.value &&
    isLastRowType(ENTITY_TYPES.CLASS) &&
    isRowFilled(lastQueryRow.value)
  )
})

const canAddClassRow = computed(() => {
  return (
    canUseQueryBuilder.value &&
    isLastRowType(ENTITY_TYPES.OBJECT_PROPERTY) &&
    isRowFilled(lastQueryRow.value)
  )
})

const canAddIndividualRow = computed(() => {
  return (
    canUseQueryBuilder.value &&
    isLastRowType(ENTITY_TYPES.OBJECT_PROPERTY) &&
    isRowFilled(lastQueryRow.value)
  )
})

const canAddFilterRows = computed(() => {
  return (
    canUseQueryBuilder.value &&
    isLastRowType(ENTITY_TYPES.DATATYPE_PROPERTY) &&
    isRowFilled(lastQueryRow.value)
  )
})

/**
 * Main actions
 */
function startNewQuery() {
  queryRows.value = [
    {
      ...createEmptyQueryRow(),
      entitytype: ENTITY_TYPES.CLASS,
      entity: null,
      inoutput: true,
      orderedby: false
    }
  ]

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

  queryRows.value = selectedRows.map(row => {
    const queryRow = createQueryRowFromPredefined(row)
    syncRowEntityWithCurrentOptions(queryRow)
    return queryRow
  })

  resetGeneratedQueryState()
}

function generateQuery() {
  generatedQuery.value = ''
  executableQuery.value = ''
  queryResults.value = []
  queryError.value = null
  invalidRowIndex.value = null

  const invalidOptionIndex = queryRows.value.findIndex(row => {
    return !syncRowEntityWithCurrentOptions(row)
  })

  if (invalidOptionIndex !== -1) {
    queryError.value = 'There are invalid entities or entities that no longer exist in the current ontology options.'
    invalidRowIndex.value = invalidOptionIndex
    return
  }

  const validationError = validateQuery(queryRows.value)

  if (validationError) {
    queryError.value = validationError.message
    invalidRowIndex.value = validationError.rowIndex
    return
  }

  const executableResult = buildOntologyQuery(queryRows.value)

  if (executableResult.error) {
    queryError.value = executableResult.error
    invalidRowIndex.value = null
    return
  }

  const readableResult = buildReadableOntologyQuery(queryRows.value)

  if (readableResult.error) {
    queryError.value = readableResult.error
    invalidRowIndex.value = null
    return
  }

  executableQuery.value = executableResult.query
  generatedQuery.value = readableResult.query
}

async function executeQuery() {
  queryError.value = null
  queryResults.value = []
  hasExecutedQuery.value = false
  queryResultCount.value = null
  executedQuery.value = ''

  let queryToExecute = ''

  if (queryMode.value === 'builder') {
    if (!executableQuery.value) generateQuery()

    if (!executableQuery.value || queryError.value) return

    queryToExecute = executableQuery.value
  } else {
    const readableAdvancedQuery = manualQuery.value?.trim()

    if (!readableAdvancedQuery) {
      queryError.value = 'Write a SQWRL query before running it.'
      return
    }

    queryToExecute = normalizeAdvancedQueryToExecutableQuery(readableAdvancedQuery)

    console.log('Advanced readable query:', readableAdvancedQuery)
    console.log('Advanced executable query:', queryToExecute)
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

/**
 * Row actions
 */
function addQueryRow() {
  if (queryRows.value.length > 0 && !isRowFilled(lastQueryRow.value)) {
    queryError.value = 'Complete the last row before adding a new one.'
    invalidRowIndex.value = queryRows.value.length - 1
    return
  }

  addTypedQueryRow(suggestNextEntityType())
}

function addObjectPropertyRow() {
  addTypedQueryRow(ENTITY_TYPES.OBJECT_PROPERTY)
}

function addDataPropertyRow() {
  addTypedQueryRow(ENTITY_TYPES.DATATYPE_PROPERTY)
}

function addClassRow() {
  addTypedQueryRow(ENTITY_TYPES.CLASS)
}

function addIndividualRow() {
  addTypedQueryRow(ENTITY_TYPES.INDIVIDUAL)
}

function addFilterRows() {
  if (
    !isLastRowType(ENTITY_TYPES.DATATYPE_PROPERTY) ||
    !isRowFilled(lastQueryRow.value)
  ) {
    return
  }

  queryRows.value.push({
    ...createEmptyQueryRow(),
    entitytype: ENTITY_TYPES.RELATIONAL_OPERATOR,
    entity: null,
    inoutput: false,
    orderedby: false
  })

  queryRows.value.push({
    ...createEmptyQueryRow(),
    entitytype: ENTITY_TYPES.LITERAL,
    entity: '',
    inoutput: false,
    orderedby: false
  })

  selectedPredefinedQuery.value = null
  resetGeneratedQueryState()
}

function removeQueryRow(row) {
  queryRows.value = queryRows.value.filter(item => item.id !== row.id)
  selectedPredefinedQuery.value = null

  resetGeneratedQueryState()
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

function resetGeneratedQueryState() {
  generatedQuery.value = ''
  executableQuery.value = ''
  queryResults.value = []
  queryError.value = null
  copyMessage.value = ''
  hasExecutedQuery.value = false
  invalidRowIndex.value = null
  queryResultCount.value = null
  executedQuery.value = ''
}

/**
 * Row helpers
 */
function addTypedQueryRow(entitytype) {
  queryRows.value.push({
    ...createEmptyQueryRow(),
    entitytype,
    entity: null,
    inoutput: false,
    orderedby: false
  })

  selectedPredefinedQuery.value = null
  resetGeneratedQueryState()
}

function isFirstRow(row) {
  return queryRows.value.findIndex(item => item.id === row.id) === 0
}

function isLastRow(row) {
  return queryRows.value.findIndex(item => item.id === row.id) === queryRows.value.length - 1
}

function isLastRowType(entityType) {
  return normalizeEntityType(lastQueryRow.value?.entitytype) === entityType
}

function isRowFilled(row) {
  if (!row) {
    return false
  }

  const type = normalizeEntityType(row.entitytype)

  if (!type) {
    return false
  }

  if (type === ENTITY_TYPES.LITERAL) {
    return row.entity !== null && row.entity !== undefined && String(row.entity).trim() !== ''
  }

  return row.entity !== null && row.entity !== undefined && row.entity !== ''
}

function suggestNextEntityType() {
  const lastRow = lastQueryRow.value

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

function getRowClass(data) {
  const index = queryRows.value.findIndex(row => row.id === data.id)

  return {
    'invalid-query-row': invalidRowIndex.value === index
  }
}

/**
 * Entity helpers
 */
function normalizeForMatching(value) {
  return String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '')
}

function syncRowEntityWithCurrentOptions(row) {
  const type = normalizeEntityType(row.entitytype)

  if (type === ENTITY_TYPES.LITERAL) {
    return isRowFilled(row)
  }

  if (!type || !row.entity) {
    return false
  }

  const currentValue = row.entity
  const normalizedCurrentValue = normalizeForMatching(currentValue)

  const options = getEntityOptions(row)

  if (!options.length) {
    return false
  }

  const exactOptionMatch = options.find(option => option.value === currentValue)

  if (exactOptionMatch) {
    return true
  }

  const compatibleOptionMatch = options.find(option => {
    return (
      normalizeForMatching(option.value) === normalizedCurrentValue ||
      normalizeForMatching(option.label) === normalizedCurrentValue
    )
  })

  if (compatibleOptionMatch) {
    row.entity = compatibleOptionMatch.value
    return true
  }

  const compatibleEntityMatch = normalizedEntities.value.find(entity => {
    if (normalizeEntityType(entity.type) !== type) {
      return false
    }

    return (
      normalizeForMatching(entity.name) === normalizedCurrentValue ||
      normalizeForMatching(entity.queryName) === normalizedCurrentValue ||
      normalizeForMatching(entity.iri) === normalizedCurrentValue ||
      normalizeForMatching(entity.details) === normalizedCurrentValue
    )
  })

  if (compatibleEntityMatch) {
    row.entity = compatibleEntityMatch.queryName ?? compatibleEntityMatch.name
    return true
  }

  return false
}

function getEntityOptions(row) {
  const normalizedType = normalizeEntityType(row.entitytype)

  if (!normalizedType || normalizedType === ENTITY_TYPES.LITERAL) {
    return []
  }

  return filteredEntitiesByType.value[normalizedType] ?? []
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

function getEntityDisplayLabel(row) {
  const type = normalizeEntityType(row.entitytype)

  if (type === ENTITY_TYPES.LITERAL) {
    return String(row.entity ?? '')
  }

  const options = getEntityOptions(row)
  const option = options.find(item => item.value === row.entity)

  return option?.label ?? String(row.entity ?? '')
}

function canUseInOutput(row) {
  const type = normalizeEntityType(row.entitytype)

  return [
    ENTITY_TYPES.CLASS,
    ENTITY_TYPES.INDIVIDUAL,
    ENTITY_TYPES.DATATYPE_PROPERTY,
    ENTITY_TYPES.OBJECT_PROPERTY
  ].includes(type)
}

function canUseOrderedBy(row) {
  const type = normalizeEntityType(row.entitytype)

  return [
    ENTITY_TYPES.CLASS,
    ENTITY_TYPES.INDIVIDUAL,
    ENTITY_TYPES.DATATYPE_PROPERTY,
    ENTITY_TYPES.OBJECT_PROPERTY
  ].includes(type)
}

/**
 * Readable query builder
 */
function normalizeReadableOperator(operator) {
  const value = String(operator ?? '').trim()

  if (!value) {
    return ''
  }

  if (value.startsWith('swrlb:')) {
    return value
  }

  return `swrlb:${value}`
}

function formatReadableLiteral(value) {
  if (typeof value === 'boolean') {
    return String(value)
  }

  const text = String(value ?? '').trim()

  if (text === 'true' || text === 'false') {
    return text
  }

  if (text !== '' && !Number.isNaN(Number(text))) {
    return text
  }

  return `"${text}"`
}

function buildReadableOntologyQuery(rows) {
  const atoms = []
  const outputAtoms = []
  const orderByAtoms = []

  let currentSubjectVariable = null
  let pendingObjectProperty = null
  let pendingOperator = null
  let lastDataPropertyVariable = null

  let classVariableIndex = 1
  let valueVariableIndex = 1

  for (let index = 0; index < rows.length; index += 1) {
    const row = rows[index]
    const type = normalizeEntityType(row.entitytype)
    const label = getEntityDisplayLabel(row)

    if (!type || !label) {
      return {
        query: '',
        error: `Row ${index + 1} is incomplete.`
      }
    }

    if (type === ENTITY_TYPES.CLASS) {
      const variable = `?x${classVariableIndex}`
      classVariableIndex += 1

      if (pendingObjectProperty && currentSubjectVariable) {
        atoms.push(`${pendingObjectProperty}(${currentSubjectVariable}, ${variable})`)
        pendingObjectProperty = null
      }

      atoms.push(`${label}(${variable})`)
      currentSubjectVariable = variable

      if (row.inoutput) {
        outputAtoms.push(`sqwrl:select(${variable})`)
      }

      if (row.orderedby) {
        orderByAtoms.push(`sqwrl:orderBy(${variable})`)
      }

      continue
    }

    if (type === ENTITY_TYPES.OBJECT_PROPERTY) {
      if (!currentSubjectVariable) {
        return {
          query: '',
          error: `Row ${index + 1} has an object property without a previous class.`
        }
      }

      pendingObjectProperty = label
      continue
    }

    if (type === ENTITY_TYPES.INDIVIDUAL) {
      if (!pendingObjectProperty || !currentSubjectVariable) {
        return {
          query: '',
          error: `Row ${index + 1} has an individual without a previous object property.`
        }
      }

      atoms.push(`${pendingObjectProperty}(${currentSubjectVariable}, ${label})`)
      pendingObjectProperty = null

      if (row.inoutput) {
        outputAtoms.push(`sqwrl:select(${label})`)
      }

      if (row.orderedby) {
        orderByAtoms.push(`sqwrl:orderBy(${label})`)
      }

      continue
    }

    if (type === ENTITY_TYPES.DATATYPE_PROPERTY) {
      if (!currentSubjectVariable) {
        return {
          query: '',
          error: `Row ${index + 1} has a data property without a previous class.`
        }
      }

      const variable = `?v${valueVariableIndex}`
      valueVariableIndex += 1

      atoms.push(`${label}(${currentSubjectVariable}, ${variable})`)
      lastDataPropertyVariable = variable

      if (row.inoutput) {
        outputAtoms.push(`sqwrl:select(${variable})`)
      }

      if (row.orderedby) {
        orderByAtoms.push(`sqwrl:orderBy(${variable})`)
      }

      continue
    }

    if (type === ENTITY_TYPES.RELATIONAL_OPERATOR) {
      if (!lastDataPropertyVariable) {
        return {
          query: '',
          error: `Row ${index + 1} has an operator without a previous data property.`
        }
      }

      pendingOperator = normalizeReadableOperator(label)
      continue
    }

    if (type === ENTITY_TYPES.LITERAL) {
      if (!pendingOperator || !lastDataPropertyVariable) {
        return {
          query: '',
          error: `Row ${index + 1} has a literal without a previous relational operator.`
        }
      }

      atoms.push(`${pendingOperator}(${lastDataPropertyVariable}, ${formatReadableLiteral(label)})`)
      pendingOperator = null
      continue
    }

    return {
      query: '',
      error: `Unsupported entity type in row ${index + 1}.`
    }
  }

  if (pendingObjectProperty) {
    return {
      query: '',
      error: 'There is an object property without a target class or individual.'
    }
  }

  if (pendingOperator) {
    return {
      query: '',
      error: 'There is a relational operator without a literal value.'
    }
  }

  if (!atoms.length) {
    return {
      query: '',
      error: 'No query atoms were generated.'
    }
  }

  const selectedAtoms = outputAtoms.length
    ? outputAtoms
    : [`sqwrl:select(${currentSubjectVariable})`]

  return {
    query: `${atoms.join(' ^ ')} -> ${[...selectedAtoms, ...orderByAtoms].join(' ^ ')}`,
    error: null
  }
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function normalizeAdvancedQueryToExecutableQuery(query) {
  let convertedQuery = String(query ?? '').trim()

  if (!convertedQuery) {
    return ''
  }

  const replacements = normalizedEntities.value
    .filter(entity => entity?.name && entity?.queryName)
    .map(entity => ({
      label: entity.name,
      queryName: entity.queryName
    }))
    .sort((a, b) => b.label.length - a.label.length)

  replacements.forEach(({ label, queryName }) => {
    const escapedLabel = escapeRegExp(label)

    // Substitui classes/propriedades usadas como predicado:
    // Implementation Library(?x1)
    // has implementation language(?x1, Java)
    convertedQuery = convertedQuery.replace(
      new RegExp(`(^|\\^|\\s|->)\\s*${escapedLabel}\\s*\\(`, 'g'),
      (match, prefix) => `${prefix} ${queryName}(`
    )

    // Substitui indivíduos usados como argumento:
    // (?x1, Java)
    // (?x1, p NSGA-II)
    convertedQuery = convertedQuery.replace(
      new RegExp(`([,(]\\s*)${escapedLabel}(\\s*[,)])`, 'g'),
      `$1${queryName}$2`
    )
  })

  return convertedQuery.replace(/\s+/g, ' ').trim()
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
          optionValue="value" placeholder="Or select a predefined query" class="predefined-query-select"
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