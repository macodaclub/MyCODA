<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useOntologyStore } from '@/store'

import * as XLSX from 'xlsx'
import { jsPDF } from 'jspdf'
import autoTable from 'jspdf-autotable'

import DOMPurify from 'dompurify'
import katex from 'katex'
import 'katex/dist/katex.min.css'

import Button from 'primevue/button'
import Select from 'primevue/select'
import MultiSelect from 'primevue/multiselect'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'

import { predefinedQueriesDatasets as predefinedQueries } from './data/predefinedQueries'
import {
  DATASET_SOURCES,
  datasetSourceOptions,
  getCriterionSelection,
} from './data/datasetSources'

import {
  oplFilteringCriteria,
  oplSortFields,
  oplOutputFieldOptions,
  defaultOplOutputFields,
  defaultHybridOplOutputFields,
  hybridOplSortFields,
  hybridOplOutputFieldOptions,
  hasOplPredefinedValueOptions,
  getOplPredefinedValueOptions,
} from './data/oplQueryOptions'

import {
  filteringCriteria,
  includeExcludeOptions,
  includeOnlyOptions,
  sortFields,
  hybridOpenAireSortFields,
  sortOrders,
  outputFieldOptions,
  hybridOpenAireOutputFieldOptions,
  defaultOutputFields,
  supportsOpenAireExclude,
  isOpenAireDateCriterion,
  hasOpenAirePredefinedValueOptions,
  getOpenAirePredefinedValueOptions,
  isOpenAireVocabularyCriterion,
  getOpenAireVocabularyName,
  isOpenAireOrcidCriterion,
  isOpenAireRorCriterion,
  getOpenAireIdentifierSearchUrl,
} from './data/datasetQueryOptions'

const ontologyStore = useOntologyStore()

const defaultHybridOutputFields = [
  ...new Set([
    ...defaultOutputFields,
    ...defaultHybridOplOutputFields,
  ]),
]

const selectedSource = ref(DATASET_SOURCES.OPENAIRE)
const selectedPredefinedQuery = ref(null)
const criteria = ref([
  createEmptyCriterion(DATASET_SOURCES.OPENAIRE),
])
const selectedFields = ref([...defaultOutputFields])

const results = ref([])
const errorMessage = ref('')
const warnings = ref([])
const loading = ref(false)

const currentPage = ref(1)
const pageSize = ref(20)
const hasNextPage = ref(false)
const totalResults = ref(null)

const resultFilters = ref(createEmptyResultFilters())

const publicationDateFilter = ref({
  operator: 'equals',
  value: '',
})

const vocabularyOptions = ref({
  countries: [],
  sdgs: [],
  'fields-of-science': [],
  'instance-types': [],
})
const loadingVocabularies = ref(false)

const filteredRowsForExport = ref(null)

const rowsForExport = computed(() =>
  filteredRowsForExport.value ?? resultsFilteredByDate.value
)



const publicationDateOperatorOptions = [
  { label: 'Equals', value: 'equals' },
  { label: 'Greater than or equal to', value: 'greaterOrEqual' },
  { label: 'Less than or equal to', value: 'lessOrEqual' },
]

const isOplSource = computed(
  () => selectedSource.value === DATASET_SOURCES.OPL,
)

const isHybridSource = computed(
  () => selectedSource.value === DATASET_SOURCES.HYBRID,
)

const activePredefinedQueries = computed(() =>
  predefinedQueries.filter(
    query => query.source === selectedSource.value,
  ),
)

function renderAbstract(value) {
  if (!value) {
    return ''
  }

  const rendered = value.replace(
    /\\\((.*?)\\\)/g,
    (_, expression) =>
      katex.renderToString(expression, {
        throwOnError: false,
      }),
  )

  return DOMPurify.sanitize(rendered)
}

function onResultsFilter(event) {
  filteredRowsForExport.value =
    event.filteredValue ?? null
}

function clearResultFilters() {
  resetResultFilters()
  filteredRowsForExport.value = null
}

function getExportFields() {
  return selectedFields.value.map(field => {
    const option = activeOutputFieldOptions.value.find(
      item => item.value === field,
    )

    return {
      field,
      label: option?.label ?? field,
    }
  })
}

function normalizeExportValue(value) {
  if (value === null || value === undefined) {
    return ''
  }

  if (Array.isArray(value)) {
    return value
      .map(item => {
        if (typeof item === 'object' && item !== null) {
          return item.name ?? item.label ?? JSON.stringify(item)
        }

        return String(item)
      })
      .join(', ')
  }

  if (typeof value === 'object') {
    return JSON.stringify(value)
  }

  return String(value)
}

function getExportRows() {
  const fields = getExportFields()

  return rowsForExport.value.map(row => {
    const result = {}

    fields.forEach(({ field, label }) => {
      let value = row[field]

      if (field === 'subjects') {
        value = row.subjectsText ?? row.subjects
      } else if (field === 'sdg') {
        value = row.sdgText ?? row.sdg
      } else if (field === 'fos') {
        value = row.fosText ?? row.fos
      }

      result[label] = normalizeExportValue(value)
    })

    return result
  })
}

function downloadBlob(content, filename, type) {
  const blob = new Blob(
    [content],
    { type },
  )

  const url = URL.createObjectURL(blob)

  const link = document.createElement('a')
  link.href = url
  link.download = filename

  document.body.appendChild(link)
  link.click()
  link.remove()

  URL.revokeObjectURL(url)
}

function exportCsv() {
  const rows = getExportRows()

  if (!rows.length) {
    return
  }

  const headers = Object.keys(rows[0])

  const escapeCsv = value =>
    `"${String(value ?? '').replace(/"/g, '""')}"`

  const csv = [
    headers.map(escapeCsv).join(','),
    ...rows.map(row =>
      headers
        .map(header => escapeCsv(row[header]))
        .join(',')
    ),
  ].join('\n')

  downloadBlob(
    '\uFEFF' + csv,
    'mycoda-datasets.csv',
    'text/csv;charset=utf-8;',
  )
}

function exportJson() {
  const rows = getExportRows()

  downloadBlob(
    JSON.stringify(rows, null, 2),
    'mycoda-datasets.json',
    'application/json;charset=utf-8;',
  )
}

function exportXlsx() {
  const rows = getExportRows()

  if (!rows.length) {
    return
  }

  const worksheet =
    XLSX.utils.json_to_sheet(rows)

  const workbook =
    XLSX.utils.book_new()

  XLSX.utils.book_append_sheet(
    workbook,
    worksheet,
    'Datasets',
  )

  XLSX.writeFile(
    workbook,
    'mycoda-datasets.xlsx',
  )
}

function exportPdf() {
  const rows = getExportRows()

  if (!rows.length) {
    return
  }

  const headers =
    Object.keys(rows[0])

  const body = rows.map(row =>
    headers.map(header => row[header])
  )

  const document = new jsPDF({
    orientation: 'landscape',
    unit: 'mm',
    format: 'a4',
  })

  document.setFontSize(14)
  document.text(
    'MyCODA - Dataset Results',
    14,
    15,
  )

  autoTable(document, {
    head: [headers],
    body,
    startY: 20,
    styles: {
      fontSize: 7,
      cellPadding: 2,
      overflow: 'linebreak',
    },
    headStyles: {
      fontStyle: 'bold',
    },
  })

  document.save(
    'mycoda-datasets.pdf',
  )
}

const activeFilteringCriteria = computed(() => {
  if (isHybridSource.value) {
    return [
      ...filteringCriteria.map(option => ({
        label: `OpenAIRE — ${option.label}`,
        value: `${DATASET_SOURCES.OPENAIRE}:${option.value}`,
        source: DATASET_SOURCES.OPENAIRE,
        field: option.value,
      })),
      ...oplFilteringCriteria.map(option => ({
        label: `OPL — ${option.label}`,
        value: `${DATASET_SOURCES.OPL}:${option.value}`,
        source: DATASET_SOURCES.OPL,
        field: option.value,
      })),
    ]
  }

  const source = isOplSource.value
    ? DATASET_SOURCES.OPL
    : DATASET_SOURCES.OPENAIRE

  const options = isOplSource.value
    ? oplFilteringCriteria
    : filteringCriteria

  return options.map(option => ({
    ...option,
    source,
    field: option.value,
  }))
})

const activeSortFields = computed(() => {
  if (isHybridSource.value) {
    return [
      { label: 'No sorting', value: '' },
      ...hybridOpenAireSortFields,
      ...hybridOplSortFields,
    ]
  }

  return isOplSource.value
    ? oplSortFields
    : sortFields
})

const activeOutputFieldOptions = computed(() => {
  if (isHybridSource.value) {
    return [
      ...hybridOpenAireOutputFieldOptions,
      ...hybridOplOutputFieldOptions,
    ]
  }

  return isOplSource.value
    ? oplOutputFieldOptions
    : outputFieldOptions
})

const globalFilterFields = computed(() => {
  const fieldAliases = {
    subjects: 'subjectsText',
    sdg: 'sdgText',
    fos: 'fosText',
  }

  return selectedFields.value.map(
    field => fieldAliases[field] ?? field,
  )
})

const resultsHeading = computed(() => {
  if (isHybridSource.value) {
    return 'OpenAIRE + OPL Results'
  }

  return isOplSource.value
    ? 'Optimisation Problem Library (OPL) Results'
    : 'Research Products (RP) Querying Results'
})

const criteriaNote = computed(() => {
  if (isHybridSource.value) {
    return 'Filter criteria are combined using AND. Repeated values for the same filter are combined using OR. For OpenAIRE, Exclude is supported only for title and keywords; OPL supports Include/Exclude for all exposed fields.'
  }

  if (isOplSource.value) {
    return 'Filter criteria are combined using AND. Repeated values for the same filter are combined using OR. Include/Exclude is supported for the exposed OPL fields.'
  }

  return 'Filter criteria are combined using AND. Repeated values for the same filter are combined using OR. Exclude is supported only for title and keywords.'
})

const typeResultOptions = computed(() =>
  getUniqueResultOptions('type'),
)

const citationsResultOptions = computed(() =>
  getUniqueResultOptions('citations'),
)

const publisherResultOptions = computed(() =>
  getUniqueResultOptions('publisher'),
)

const resultsFilteredByDate = computed(() => {
  if (isOplSource.value) {
    return results.value
  }

  const selectedDate = publicationDateFilter.value.value

  if (!selectedDate) {
    return results.value
  }

  return results.value.filter(result => {
    if (!result.publicationDate) {
      return false
    }

    const resultDate = String(result.publicationDate).slice(0, 10)

    if (publicationDateFilter.value.operator === 'greaterOrEqual') {
      return resultDate >= selectedDate
    }

    if (publicationDateFilter.value.operator === 'lessOrEqual') {
      return resultDate <= selectedDate
    }

    return resultDate === selectedDate
  })
})

function createRowId() {
  return globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random()}`
}

function getDefaultCriterionSource(querySource = selectedSource.value) {
  return querySource === DATASET_SOURCES.OPL
    ? DATASET_SOURCES.OPL
    : DATASET_SOURCES.OPENAIRE
}

function createEmptyCriterion(querySource = selectedSource.value) {
  return {
    id: createRowId(),
    source: getDefaultCriterionSource(querySource),
    field: '',
    criterionSelection: '',
    operator: 'INCLUDE',
    value: '',
    sortedByField: '',
    sortedInAscOrDescOrder: '',
  }
}

function createEmptyResultFilters() {
  return {
    global: { value: null, matchMode: 'contains' },

    // OpenAIRE
    
    type: { value: null, matchMode: 'equals' },
    
    instanceType: { value: null, matchMode: 'contains' },
    title: { value: null, matchMode: 'contains' },
    abstract: { value: null, matchMode: 'contains' },
    subjectsText: { value: null, matchMode: 'contains' },
    authors: { value: null, matchMode: 'contains' },
    publicationDate: { value: null, matchMode: 'contains' },
    publisher: { value: null, matchMode: 'equals' },
    scientificEvent: { value: null, matchMode: 'contains' },
    citations: { value: null, matchMode: 'contains' },
    influenceClass: { value: null, matchMode: 'contains' },
    popularityClass: { value: null, matchMode: 'contains' },
    impulseClass: { value: null, matchMode: 'contains' },
    citationCountClass: { value: null, matchMode: 'contains' },
    relatedMaterials: { value: null, matchMode: 'contains' },
    countryCode: { value: null, matchMode: 'contains' },
    sdgText: { value: null, matchMode: 'contains' },
    fosText: { value: null, matchMode: 'contains' },

    // OPL
    id: { value: null, matchMode: 'contains' },
    name: { value: null, matchMode: 'contains' },
    longName: { value: null, matchMode: 'contains' },
    description: { value: null, matchMode: 'contains' },
    objectives: { value: null, matchMode: 'contains' },
    variableTypes: { value: null, matchMode: 'contains' },
    variableDimensions: { value: null, matchMode: 'contains' },
    constraintTypes: { value: null, matchMode: 'contains' },
    numberOfConstraints: { value: null, matchMode: 'contains' },
    modality: { value: null, matchMode: 'contains' },
    noiseType: { value: null, matchMode: 'contains' },
    source: { value: null, matchMode: 'contains' },
    referenceTitles: { value: null, matchMode: 'contains' },
    links: { value: null, matchMode: 'contains' },

    // OPL aliases used by Hybrid
    oplId: { value: null, matchMode: 'contains' },
    oplType: { value: null, matchMode: 'contains' },
    oplSource: { value: null, matchMode: 'contains' },
    oplAuthors: { value: null, matchMode: 'contains' },
  }
}

function extractDoiFromRelatedMaterials(value) {
  if (!value) {
    return ''
  }

  const match = String(value).match(
    /(?:https?:\/\/(?:dx\.)?doi\.org\/|doi:)?(10\.\d{4,9}\/[^\s,]+)/i,
  )

  return match?.[1] ?? ''
}

function stripAbstractHtml(value) {
  if (!value) {
    return ''
  }

  const documentParser = new DOMParser()
  const document = documentParser.parseFromString(
    String(value),
    'text/html',
  )

  return document.body.textContent ?? ''
}

function addDatasetToOntology(data) {
  const transferId =
    globalThis.crypto?.randomUUID?.() ??
    `${Date.now()}-${Math.random()}`

  const payload = {
    title: data.title ?? '',
    abstract: stripAbstractHtml(data.abstract),
    keywords: Array.isArray(data.subjects)
      ? data.subjects.join(', ')
      : '',
    authors: data.authors ?? '',
    reference: '',
    doi: extractDoiFromRelatedMaterials(
      data.relatedMaterials,
    ),
  }

  localStorage.setItem(
    `mycoda-article-${transferId}`,
    JSON.stringify(payload),
  )

  const url =
    `/contribute/submit-paper?import=${encodeURIComponent(transferId)}`

  window.open(
    url,
    '_blank',
    'noopener,noreferrer',
  )
}

function resetResultFilters() {
  resultFilters.value = createEmptyResultFilters()
  publicationDateFilter.value = {
    operator: 'equals',
    value: '',
  }
}

function resetSearchState() {
  results.value = []
  errorMessage.value = ''
  warnings.value = []
  currentPage.value = 1
  pageSize.value = 20
  hasNextPage.value = false
  totalResults.value = null
  resetResultFilters()
}

function addCriterion() {
  criteria.value.push(createEmptyCriterion())
}

function getDefaultSelectedFields(source = selectedSource.value) {
  if (source === DATASET_SOURCES.OPL) {
    return [...defaultOplOutputFields]
  }

  if (source === DATASET_SOURCES.HYBRID) {
    return [...defaultHybridOutputFields]
  }

  return [...defaultOutputFields]
}

function resetCriteria() {
  selectedPredefinedQuery.value = null
  criteria.value = [createEmptyCriterion()]
  selectedFields.value = getDefaultSelectedFields()
  resetSearchState()
}

function applyPredefinedQuery() {
  const selected = predefinedQueries.find(
    query => query.id === selectedPredefinedQuery.value,
  )

  if (!selected) {
    return
  }

  if (selected.enabled === false) {
    errorMessage.value =
      'This predefined query is not available yet because its complete implementation is still pending.'
    return
  }

  const source = selected.source ?? DATASET_SOURCES.OPENAIRE
  selectedSource.value = source

  const firstSort = selected.sort?.[0] ?? null

  criteria.value = selected.criteria.map((item, index) => {
    const criterionSource = item.source ?? getDefaultCriterionSource(source)

    return {
      id: createRowId(),
      source: criterionSource,
      field: item.field,
      criterionSelection: getCriterionSelection(
        criterionSource,
        item.field,
        source,
      ),
      operator: item.operator ?? 'INCLUDE',
      value: item.value,
      sortedByField:
        index === 0
          ? firstSort?.field ?? ''
          : '',
      sortedInAscOrDescOrder:
        index === 0
          ? firstSort?.direction ?? ''
          : '',
    }
  })

  selectedFields.value = selected.selectedFields?.length
    ? [...selected.selectedFields]
    : getDefaultSelectedFields(source)

  results.value = []
  errorMessage.value = ''
  warnings.value = []

  currentPage.value = selected.page ?? 1
  pageSize.value = selected.pageSize ?? 20
  hasNextPage.value = false
  totalResults.value = null

  resetResultFilters()
}

function removeCriterion(row) {
  criteria.value = criteria.value.filter(item => item.id !== row.id)

  if (criteria.value.length === 0) {
    criteria.value = [createEmptyCriterion()]
  }
}

function isDateCriterion(row) {
  return (
    row.source === DATASET_SOURCES.OPENAIRE &&
    isOpenAireDateCriterion(row.field)
  )
}

function isVocabularyCriterion(row) {
  return (
    row.source === DATASET_SOURCES.OPENAIRE &&
    isOpenAireVocabularyCriterion(row.field)
  )
}

function getVocabularyOptions(row) {
  if (row.source !== DATASET_SOURCES.OPENAIRE) {
    return []
  }

  const vocabularyName = getOpenAireVocabularyName(row.field)
  if (!vocabularyName) {
    return []
  }

  return vocabularyOptions.value[vocabularyName] ?? []
}

function isOrcidCriterion(row) {
  return (
    row.source === DATASET_SOURCES.OPENAIRE &&
    isOpenAireOrcidCriterion(row.field)
  )
}

function isRorCriterion(row) {
  return (
    row.source === DATASET_SOURCES.OPENAIRE &&
    isOpenAireRorCriterion(row.field)
  )
}

function getIdentifierPlaceholder(row) {
  if (isOrcidCriterion(row)) {
    return '0000-0002-1825-0097'
  }

  if (row.field === 'rorId') {
    return 'https://ror.org/052gg0110'
  }

  if (row.field === 'relOrganizationId') {
    return 'ROR or OpenAIRE Organization ID'
  }

  return 'Insert value'
}

function openIdentifierSearch(row) {
  const url = getOpenAireIdentifierSearchUrl(row.field)
  if (!url) {
    return
  }

  window.open(url, '_blank', 'noopener,noreferrer')
}

function hasPredefinedValueOptions(row) {
  return row.source === DATASET_SOURCES.OPL
    ? hasOplPredefinedValueOptions(row.field)
    : hasOpenAirePredefinedValueOptions(row.field)
}

function getPredefinedValueOptions(row) {
  return row.source === DATASET_SOURCES.OPL
    ? getOplPredefinedValueOptions(row.field)
    : getOpenAirePredefinedValueOptions(row.field)
}

function supportsExclude(row) {
  if (row.source === DATASET_SOURCES.OPL) {
    return true
  }

  return (
    row.source === DATASET_SOURCES.OPENAIRE &&
    supportsOpenAireExclude(row.field)
  )
}

function getIncludeExcludeOptions(row) {
  return supportsExclude(row)
    ? includeExcludeOptions
    : includeOnlyOptions
}

function onFilteringCriterionChange(row) {
  const selectedOption = activeFilteringCriteria.value.find(
    option => option.value === row.criterionSelection,
  )

  if (!selectedOption) {
    row.source = getDefaultCriterionSource()
    row.field = ''
    row.value = ''
    row.operator = 'INCLUDE'
    return
  }

  row.source = selectedOption.source
  row.field = selectedOption.field
  row.value = ''

  if (!supportsExclude(row)) {
    row.operator = 'INCLUDE'
  }
}

function getUniqueResultOptions(field) {
  const values = results.value
    .map(result => result[field])
    .filter(value => value !== null && value !== undefined && value !== '')
    .map(value => String(value))

  return [...new Set(values)]
    .sort((a, b) => a.localeCompare(b))
    .map(value => ({
      label: value,
      value,
    }))
}

function validateCriteria() {
  if (!criteria.value.length) {
    throw new Error('Please add at least one filtering criterion.')
  }

  if (!selectedFields.value.length) {
    throw new Error('Please select at least one result field.')
  }

  criteria.value.forEach((criterion, index) => {
    if (!criterion.field) {
      throw new Error(
        `Please select the filtering criterion in row ${index + 1}.`,
      )
    }

    if (!String(criterion.value ?? '').trim()) {
      throw new Error(
        `Please assign a value to criterion ${criterion.field}.`,
      )
    }

    if (
      criterion.operator === 'EXCLUDE' &&
      !supportsExclude(criterion)
    ) {
      throw new Error(
        criterion.source === DATASET_SOURCES.OPENAIRE
          ? 'The exclude operator is only supported for OpenAIRE Main title and Subjects / keywords.'
          : `The exclude operator is not supported for ${criterion.field}.`,
      )
    }

    const hasSortField = Boolean(criterion.sortedByField)
    const hasSortDirection = Boolean(criterion.sortedInAscOrDescOrder)

    if (hasSortField !== hasSortDirection) {
      throw new Error(
        `Please complete the sorting configuration in row ${index + 1}.`,
      )
    }
  })

  if (isHybridSource.value) {
    const sourceSet = new Set(
      criteria.value.map(criterion => criterion.source),
    )

    if (!sourceSet.has(DATASET_SOURCES.OPENAIRE)) {
      throw new Error(
        'A Hybrid query requires at least one OpenAIRE criterion.',
      )
    }

    if (!sourceSet.has(DATASET_SOURCES.OPL)) {
      throw new Error(
        'A Hybrid query requires at least one OPL criterion.',
      )
    }
  }
}

function buildSortRequest() {
  return criteria.value
    .filter(
      criterion =>
        criterion.sortedByField &&
        criterion.sortedInAscOrDescOrder,
    )
    .map(criterion => ({
      field: criterion.sortedByField,
      direction: criterion.sortedInAscOrDescOrder,
    }))
}

function buildOpenAireRequest() {
  return {
    criteria: criteria.value.map(criterion => ({
      source: DATASET_SOURCES.OPENAIRE,
      field: criterion.field,
      operator: criterion.operator,
      value: String(criterion.value).trim(),
    })),
    sort: buildSortRequest(),
    selectedFields: [...selectedFields.value],
    page: currentPage.value,
    pageSize: pageSize.value,
  }
}

function buildOplRequest() {
  const sortingCriterion = criteria.value.find(
    criterion =>
      criterion.sortedByField &&
      criterion.sortedInAscOrDescOrder,
  )

  return {
    criteria: criteria.value.map(criterion => ({
      field: criterion.field,
      operator: criterion.operator,
      value: String(criterion.value).trim(),
    })),
    page: currentPage.value,
    pageSize: pageSize.value,
    sortField: sortingCriterion?.sortedByField || null,
    sortOrder: sortingCriterion?.sortedInAscOrDescOrder || null,
  }
}

function buildHybridRequest() {
  return {
    criteria: criteria.value.map(criterion => ({
      source: criterion.source,
      field: criterion.field,
      operator: criterion.operator,
      value: String(criterion.value).trim(),
    })),
    sort: buildSortRequest(),
    selectedFields: [...selectedFields.value],
    page: currentPage.value,
    pageSize: pageSize.value,
  }
}

function buildDatasetRequest() {
  validateCriteria()

  if (isHybridSource.value) {
    return buildHybridRequest()
  }

  if (isOplSource.value) {
    return buildOplRequest()
  }

  return buildOpenAireRequest()
}

function normalizeList(value) {
  return Array.isArray(value)
    ? value.join(', ')
    : (value ?? '')
}

function normalizeOpenAireResult(result) {
  return {
    ...result,
    subjectsText: normalizeList(result.subjects),
    sdgText: normalizeList(result.sdg),
    fosText: normalizeList(result.fos),
    authorDetails: Array.isArray(result.authorDetails)
      ? result.authorDetails
      : [],
  }
}

function normalizeOplResult(result) {
  return {
    ...result,
    objectives: normalizeList(result.objectives),
    variableTypes: normalizeList(result.variableTypes),
    variableDimensions: normalizeList(result.variableDimensions),
    constraintTypes: normalizeList(result.constraintTypes),
    numberOfConstraints: normalizeList(result.numberOfConstraints),
    authors: normalizeList(result.authors),
    referenceTitles: normalizeList(result.referenceTitles),
    links: normalizeList(result.links),
  }
}

function normalizeHybridResult(result) {
  return {
    ...result,

    // OpenAIRE
    subjectsText: normalizeList(result.subjects),
    sdgText: normalizeList(result.sdg),
    fosText: normalizeList(result.fos),
    authorDetails: Array.isArray(result.authorDetails)
      ? result.authorDetails
      : [],

    // OPL fields without name collisions
    name: result.oplName ?? '',
    longName: result.oplLongName ?? '',
    description: result.oplDescription ?? '',
    objectives: normalizeList(result.oplObjectives),
    variableTypes: normalizeList(result.oplVariableTypes),
    variableDimensions: normalizeList(result.oplVariableDimensions),
    constraintTypes: normalizeList(result.oplConstraintTypes),
    numberOfConstraints: normalizeList(result.oplNumberOfConstraints),
    modality: result.oplModality ?? '',
    noiseType: result.oplNoiseType ?? '',
    referenceTitles: normalizeList(result.oplReferenceTitles),
    links: normalizeList(result.oplLinks),

    // Explicit OPL aliases used by Hybrid
    oplId: result.oplId ?? '',
    oplType: result.oplType ?? '',
    oplSource: 'OPL',
    oplAuthors: normalizeList(result.oplAuthors),
  }
}

function getWarningKey(warning, index) {
  if (typeof warning === 'string') {
    return `warning-${index}-${warning}`
  }

  return [
    warning?.source,
    warning?.code,
    index,
  ].filter(Boolean).join('-')
}

function getWarningText(warning) {
  if (typeof warning === 'string') {
    return warning
  }

  const prefix = warning?.source
    ? `${warning.source}: `
    : ''

  return `${prefix}${warning?.message ?? warning?.code ?? 'Warning'}`
}

async function runQuery({ resetPage = true } = {}) {
  try {
    loading.value = true
    errorMessage.value = ''
    warnings.value = []

    if (resetPage) {
      currentPage.value = 1
    }

    resetResultFilters()

    const request = buildDatasetRequest()

    const response = isHybridSource.value
      ? await ontologyStore.searchHybridDatasets(request)
      : isOplSource.value
        ? await ontologyStore.searchOplDatasets(request)
        : await ontologyStore.searchDatasets(request)

    const responseResults = response?.results ?? []

    results.value =
      isHybridSource.value
        ? responseResults.map(
            normalizeHybridResult
          )
        : isOplSource.value
          ? responseResults.map(
              normalizeOplResult
            )
          : responseResults.map(
              normalizeOpenAireResult
            )

    currentPage.value =
      response?.page ?? request.page

    pageSize.value =
      response?.pageSize ?? request.pageSize

    hasNextPage.value =
      response?.hasNextPage ?? false

    totalResults.value = isHybridSource.value
      ? response?.totalResults ?? 0
      : response?.totalResults ?? null

    warnings.value = response?.warnings ?? []
  } catch (error) {
    errorMessage.value = error?.message ?? 'Failed to search datasets.'
    results.value = []
    warnings.value = []
    hasNextPage.value = false
    totalResults.value = null
  } finally {
    loading.value = false
  }
}

async function goToPreviousPage() {
  if (currentPage.value <= 1 || loading.value) {
    return
  }

  currentPage.value -= 1
  await runQuery({ resetPage: false })
}

async function goToNextPage() {
  if (!hasNextPage.value || loading.value) {
    return
  }

  currentPage.value += 1
  await runQuery({ resetPage: false })
}

async function changePageSize() {
  currentPage.value = 1
  await runQuery({ resetPage: false })
}

function isOutputFieldSelected(field) {
  return selectedFields.value.includes(field)
}

async function loadDatasetVocabularies() {
  try {
    loadingVocabularies.value = true

    const vocabularyNames = [
      'countries',
      'sdgs',
      'fields-of-science',
      'instance-types',
    ]

    const responses = await Promise.all(
      vocabularyNames.map(vocabulary =>
        ontologyStore.fetchDatasetVocabulary(vocabulary),
      ),
    )

    vocabularyNames.forEach((vocabulary, index) => {
      vocabularyOptions.value[vocabulary] =
        (responses[index] ?? []).map(item => ({
          label: item.title,
          value: item.code,
        }))
    })
  } catch (error) {
    console.error('Failed to load OpenAIRE vocabularies:', error)
  } finally {
    loadingVocabularies.value = false
  }
}

watch(selectedSource, source => {
  selectedPredefinedQuery.value = null
  criteria.value = [createEmptyCriterion(source)]
  selectedFields.value = getDefaultSelectedFields(source)
  resetSearchState()
})

onMounted(() => {
  loadDatasetVocabularies()
})
</script>


<template>
  <div class="dataset-query-container">
    <section class="dataset-query-header">
      <div class="dataset-query-info">
        <h2>
          {{
            isHybridSource
              ? 'OpenAIRE + OPL Querying'
              : isOplSource
                ? 'Optimisation Problem Library (OPL) Querying'
                : 'Research Products (RP) Querying'
          }}
        </h2>

        <p>
          {{
            isOplSource
              ? 'Build your query interactively using the table below or select a predefined query and customize it.'
              : isHybridSource
                ? 'Build a combined OpenAIRE and OPL query or select the predefined hybrid query.'
                : 'Build your query interactively using the table below or select a predefined query and customize it.'
          }}
        </p>


      </div>
    </section>

    
<div class="dataset-query-toolbar">
      <div class="dataset-source-field">
        <label for="dataset-source">Data source</label>

        <Select
          id="dataset-source"
          v-model="selectedSource"
          :options="datasetSourceOptions"
          option-label="label"
          option-value="value"
          class="dataset-source-select"
        />
      </div>

      <div class="predefined-query-field">
        <label for="predefined-query">Predefined query</label>

        <Select
          id="predefined-query"
          v-model="selectedPredefinedQuery"
          :options="activePredefinedQueries"
          option-label="label"
          option-value="id"
          placeholder="Select a predefined query"
          class="predefined-query-select"
          @change="applyPredefinedQuery"
        />
      </div>

      <div class="dataset-query-actions">
        <Button
          label="Add criterion"
          icon="pi pi-plus"
          @click="addCriterion"
        />

        <Button
          label="Reset"
          icon="pi pi-refresh"
          severity="secondary"
          outlined
          @click="resetCriteria"
        />

        <Button
          label="Run query"
          icon="pi pi-search"
          :loading="loading"
          @click="runQuery()"
        />
      </div>
    </div>

    <section class="criteria-table-section">
        <DataTable
          :value="criteria"
          data-key="id"
          responsive-layout="scroll"
          class="criteria-table"
        >
        <Column header="Filtering criterion" style="min-width: 220px">
          <template #body="{ data }">
            <Select
              v-model="data.criterionSelection"
              :options="activeFilteringCriteria"
              option-label="label"
              option-value="value"
              placeholder="Select criterion"
              class="w-full"
              @change="onFilteringCriterionChange(data)"
            />
          </template>
        </Column>

        <Column header="Value" style="min-width: 260px">
          <template #body="{ data }">
            <Select
              v-if="isVocabularyCriterion(data)"
              v-model="data.value"
              :options="getVocabularyOptions(data)"
              option-label="label"
              option-value="value"
              placeholder="Select value"
              filter
              show-clear
              :loading="loadingVocabularies"
              class="w-full"
            />

            <Select
              v-else-if="hasPredefinedValueOptions(data)"
              v-model="data.value"
              :options="getPredefinedValueOptions(data)"
              option-label="label"
              option-value="value"
              placeholder="Select value"
              class="w-full"
            />

            <InputText
              v-else-if="isDateCriterion(data)"
              v-model="data.value"
              type="date"
              class="w-full"
            />

            <div
              v-else-if="isOrcidCriterion(data) || isRorCriterion(data)"
              class="identifier-value-editor"
            >
              <InputText
                v-model="data.value"
                :placeholder="getIdentifierPlaceholder(data)"
                class="identifier-value-input"
              />

              <Button
                icon="pi pi-external-link"
                severity="secondary"
                text
                rounded
                :title="isOrcidCriterion(data) ? 'Search ORCID' : 'Search ROR'"
                @click="openIdentifierSearch(data)"
              />
            </div>

            <InputText
              v-else
              v-model="data.value"
              placeholder="Insert value"
              class="w-full"
            />
          </template>
        </Column>

        <Column header="Include / Exclude" style="min-width: 170px">
          <template #body="{ data }">
            <Select
              v-model="data.operator"
              :options="getIncludeExcludeOptions(data)"
              option-label="label"
              option-value="value"
              class="w-full"
            />
          </template>
        </Column>

        <Column header="Sort by" style="min-width: 200px">
          <template #body="{ data }">
            <Select
              v-model="data.sortedByField"
              :options="activeSortFields"
              option-label="label"
              option-value="value"
              class="w-full"
            />
          </template>
        </Column>

        <Column header="Order" style="min-width: 160px">
          <template #body="{ data }">
            <Select
              v-model="data.sortedInAscOrDescOrder"
              :options="sortOrders"
              option-label="label"
              option-value="value"
              class="w-full"
            />
          </template>
        </Column>

        <Column header="" style="width: 80px">
          <template #body="{ data }">
            <Button
              icon="pi pi-trash"
              severity="danger"
              text
              rounded
              @click="removeCriterion(data)"
            />
          </template>
        </Column>
      </DataTable>



      <p class="criteria-note">
        <u>Note</u>:
        <i>{{ criteriaNote }}</i>
      </p>

      <div v-if="errorMessage" class="query-error">
        {{ errorMessage }}
      </div>

      <div
        v-for="(warning, index) in warnings"
        :key="getWarningKey(warning, index)"
        class="query-warning"
      >
        {{ getWarningText(warning) }}
      </div>

      <section
        v-if="results.length"
        class="results-section"
      >
        <div class="results-header">
          <h3>{{ resultsHeading }}</h3>

          <InputText
            v-model="resultFilters.global.value"
            placeholder="Search current page..."
            class="results-global-search"
          />
        </div>

        <div class="dataset-output-fields">
          <label for="dataset-output-fields">
            Result fields
          </label>

          <MultiSelect
            id="dataset-output-fields"
            v-model="selectedFields"
            :options="activeOutputFieldOptions"
            option-label="label"
            option-value="value"
            placeholder="Select fields to display"
            display="chip"
            class="dataset-output-fields-select mb-5"
          />
        </div>
        <div class="dataset-export-toolbar">
          <Button
            label="Clear filters"
            icon="pi pi-filter-slash"
            severity="secondary"
            outlined
            @click="clearResultFilters"
          />

          <Button
            label="CSV"
            icon="pi pi-download"
            severity="secondary"
            outlined
            @click="exportCsv"
          />

          <Button
            label="JSON"
            icon="pi pi-download"
            severity="secondary"
            outlined
            @click="exportJson"
          />

          <Button
            label="XLSX"
            icon="pi pi-download"
            severity="secondary"
            outlined
            @click="exportXlsx"
          />

          <Button
            label="PDF"
            icon="pi pi-download"
            severity="secondary"
            outlined
            @click="exportPdf"
          />
        </div>

          <DataTable
            v-model:filters="resultFilters"
            :value="resultsFilteredByDate"
            data-key="id"
            responsive-layout="scroll"
            class="results-table styled-results-table"
            show-gridlines
            filter-display="row"
            :global-filter-fields="globalFilterFields"
            :reorderable-columns="true"
            resizableColumns
            columnResizeMode="fit"
            @filter="onResultsFilter"
          >
          <Column
            v-if="isOutputFieldSelected('oplId')"
            field="oplId"
            header="OPL Identifier"
            :show-filter-menu="false"
            style="min-width: 180px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter OPL identifier"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('oplType')"
            field="oplType"
            header="OPL Type"
            :show-filter-menu="false"
            style="min-width: 160px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter OPL type"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('oplSource')"
            field="oplSource"
            header="OPL Source"
            :show-filter-menu="false"
            style="min-width: 120px"
          />

          <Column
            v-if="isOutputFieldSelected('oplAuthors')"
            field="oplAuthors"
            header="OPL Authors"
            :show-filter-menu="false"
            style="min-width: 260px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter OPL authors"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('id')"
            field="id"
            header="Identifier"
            :show-filter-menu="false"
            style="min-width: 180px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter identifier"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('name')"
            field="name"
            header="Name"
            :show-filter-menu="false"
            style="min-width: 180px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter name"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('longName')"
            field="longName"
            header="Long name"
            :show-filter-menu="false"
            style="min-width: 260px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter long name"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('description')"
            field="description"
            header="Description"
            :show-filter-menu="false"
            style="min-width: 360px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter description"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('objectives')"
            field="objectives"
            header="Objectives"
            :show-filter-menu="false"
            style="min-width: 150px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter objectives"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('variableTypes')"
            field="variableTypes"
            header="Variable types"
            :show-filter-menu="false"
            style="min-width: 220px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter variable types"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('variableDimensions')"
            field="variableDimensions"
            header="Variable dimensions"
            :show-filter-menu="false"
            style="min-width: 210px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter dimensions"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('constraintTypes')"
            field="constraintTypes"
            header="Constraint types"
            :show-filter-menu="false"
            style="min-width: 220px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter constraint types"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('numberOfConstraints')"
            field="numberOfConstraints"
            header="Number of constraints"
            :show-filter-menu="false"
            style="min-width: 220px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter constraints"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('modality')"
            field="modality"
            header="Modality"
            :show-filter-menu="false"
            style="min-width: 160px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter modality"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('noiseType')"
            field="noiseType"
            header="Noise type"
            :show-filter-menu="false"
            style="min-width: 170px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter noise type"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('source')"
            field="source"
            header="Source"
            :show-filter-menu="false"
            style="min-width: 120px"
          />

          <Column
            v-if="isOutputFieldSelected('referenceTitles')"
            field="referenceTitles"
            header="Reference titles"
            :show-filter-menu="false"
            style="min-width: 320px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter reference titles"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('links')"
            field="links"
            header="Links"
            :show-filter-menu="false"
            style="min-width: 320px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter links"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
            <template #body="{ data }">
              <a
                v-if="data.links"
                :href="data.links.split(',')[0].trim()"
                target="_blank"
                rel="noopener noreferrer"
                class="related-material-link"
              >
                {{ data.links.split(',')[0].trim() }}
              </a>
            </template>
          </Column>

          <Column
            header="Add to ontology"
            :show-filter-menu="false"
            style="width: 150px"
          >
            <template #body="{ data }">
              <Button
                label="Add"
                icon="pi pi-plus"
                size="small"
                outlined
                @click="addDatasetToOntology(data)"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('type')"
            field="type"
            header="Type"
            :show-filter-menu="false"
            style="width: 140px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <Select
                v-model="filterModel.value"
                :options="typeResultOptions"
                option-label="label"
                option-value="value"
                placeholder="All types"
                show-clear
                class="w-full"
                @change="filterCallback()"
              />
            </template>
            <template #body="{ data }">
              {{ data.type || '' }}
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('title')"
            field="title"
            header="Title"
            :show-filter-menu="false"
            style="min-width: 50px"
            body-class="dataset-title-cell"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter title"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
            <template #body="{ data }">
              {{ data.title || '' }}
            </template>
          </Column>

          <Column
              v-if="isOutputFieldSelected('abstract')"
              field="abstract"
              header="Abstract"
              :show-filter-menu="false"
              style="min-width: 100px"
            >
              <template #filter="{ filterModel, filterCallback }">
                <InputText
                  v-model="filterModel.value"
                  placeholder="Filter abstract"
                  class="w-full"
                  @input="filterCallback()"
                />
              </template>

              <template #body="{ data }">
                <div
                  v-if="data.abstract"
                  class="dataset-abstract"
                  v-html="renderAbstract(data.abstract)"
                />
              </template>
            </Column>

          <Column
            v-if="
              isOutputFieldSelected(
                'instanceType'
              )
            "
            field="instanceType"
            header="Instance Type"
            :show-filter-menu="false"
            style="min-width: 170px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter instance type"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="
              isOutputFieldSelected(
                'subjects'
              )
            "
            field="subjectsText"
            header="Subjects / Keywords"
            :show-filter-menu="false"
            style="min-width: 240px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter subjects"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('authors')"
            field="authors"
            header="Authors"
            :show-filter-menu="false"
            style="min-width: 280px"
          >
            <template
              #filter="{
                filterModel,
                filterCallback,
              }"
            >
              <InputText
                v-model="filterModel.value"
                placeholder="Filter authors"
                class="w-full"
                @input="filterCallback()"
              />
            </template>

            <template #body="{ data }">

              <template
                v-if="
                  data.authorDetails &&
                  data.authorDetails.length > 0
                "
              >
                <div
                  v-for="(
                    author,
                    authorIndex
                  ) in data.authorDetails"
                  :key="
                    `${data.id}-${authorIndex}-${author.orcid ?? author.name}`
                  "
                  class="dataset-author"
                >
                  <span>
                    {{ author.name }}
                  </span>

                  <a
                    v-if="author.orcid"
                    :href="
                      `https://orcid.org/${author.orcid}`
                    "
                    target="_blank"
                    rel="noopener noreferrer"
                    class="dataset-author-orcid"
                  >
                    {{ author.orcid }}
                    <i
                      class="
                        pi pi-external-link
                        dataset-author-orcid-icon
                      "
                    />
                  </a>
                </div>
              </template>

              <span v-else>
                {{ data.authors || '' }}
              </span>

            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('publicationDate')"
            field="publicationDate"
            header="Publication date"
            :show-filter-menu="false"
            style="min-width: 260px"
          >
            <template #filter>
              <div class="publication-date-filter">
                <input
                  v-model="publicationDateFilter.value"
                  type="date"
                  class="p-inputtext p-component publication-date-input"
                />

                <Select
                  v-model="publicationDateFilter.operator"
                  :options="publicationDateOperatorOptions"
                  option-label="label"
                  option-value="value"
                  dropdown-icon="pi pi-filter"
                  class="publication-date-operator-select"
                  panel-class="publication-date-operator-panel"
                  append-to="body"
                >
                  <template #value>
                    <span class="publication-date-operator-empty-value"></span>
                  </template>
                </Select>
              </div>
            </template>
            <template #body="{ data }">
              {{ data.publicationDate || '' }}
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('publisher')"
            field="publisher"
            header="Publisher"
            :show-filter-menu="false"
            style="width: 220px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <Select
                v-model="filterModel.value"
                :options="publisherResultOptions"
                option-label="label"
                option-value="value"
                placeholder="All publishers"
                show-clear
                class="w-full"
                @change="filterCallback()"
              />
            </template>
            <template #body="{ data }">
              {{ data.publisher || '' }}
            </template>
          </Column>

            <Column
              v-if="isOutputFieldSelected('scientificEvent')"
              field="scientificEvent"
              header="Journal / Container"
              :show-filter-menu="false"
              style="min-width: 230px"
            >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter event / container"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="
              isOutputFieldSelected(
                'influenceClass'
              )
            "
            field="influenceClass"
            header="Influence Class"
            :show-filter-menu="false"
            style="min-width: 150px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter influence class"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="
              isOutputFieldSelected(
                'popularityClass'
              )
            "
            field="popularityClass"
            header="Popularity Class"
            :show-filter-menu="false"
            style="min-width: 160px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter popularity class"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="
              isOutputFieldSelected(
                'impulseClass'
              )
            "
            field="impulseClass"
            header="Impulse Class"
            :show-filter-menu="false"
            style="min-width: 140px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter impulse class"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="
              isOutputFieldSelected(
                'citationCountClass'
              )
            "
            field="citationCountClass"
            header="Citation Count Class"
            :show-filter-menu="false"
            style="min-width: 180px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter citation class"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('citations')"
            field="citations"
            header="Citations"
            :show-filter-menu="false"
            style="width: 140px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <Select
                v-model="filterModel.value"
                :options="citationsResultOptions"
                option-label="label"
                option-value="value"
                placeholder="All"
                show-clear
                class="w-full"
                @change="filterCallback()"
              />
            </template>
            <template #body="{ data }">
              {{ data.citations ?? '' }}
            </template>
          </Column>

          <Column
            v-if="
              isOutputFieldSelected(
                'countryCode'
              )
            "
            field="countryCode"
            header="Country"
            :show-filter-menu="false"
            style="min-width: 110px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter country"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="
              isOutputFieldSelected(
                'sdg'
              )
            "
            field="sdgText"
            header="SDG"
            :show-filter-menu="false"
            style="min-width: 230px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter SDG"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="
              isOutputFieldSelected(
                'fos'
              )
            "
            field="fosText"
            header="Fields of Science"
            :show-filter-menu="false"
            style="min-width: 230px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter fields of science"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
          </Column>

          <Column
            v-if="isOutputFieldSelected('relatedMaterials')"
            field="relatedMaterials"
            header="Related materials"
            :show-filter-menu="false"
            style="min-width: 360px"
          >
            <template #filter="{ filterModel, filterCallback }">
              <InputText
                v-model="filterModel.value"
                placeholder="Filter related materials"
                class="w-full"
                @input="filterCallback()"
              />
            </template>
            <template #body="{ data }">
              <a
                v-if="data.relatedMaterials"
                :href="data.relatedMaterials.split(',')[0].trim()"
                target="_blank"
                rel="noopener noreferrer"
                class="related-material-link"
              >
                {{ data.relatedMaterials.split(',')[0].trim() }}
              </a>
            </template>
          </Column>
        </DataTable>

        <div class="dataset-pagination">
          <div class="dataset-pagination-info">
            <span>Page {{ currentPage }}</span>

            <span v-if="totalResults !== null">
              — {{ totalResults }} results
            </span>

            <span v-else>
              — {{ results.length }} results on this page
            </span>
          </div>

          <div class="dataset-pagination-actions">
            <Select
              v-model="pageSize"
              :options="[10, 20, 50, 100]"
              class="dataset-page-size"
              @change="changePageSize"
            />

            <Button
              label="Previous"
              icon="pi pi-chevron-left"
              severity="secondary"
              outlined
              :disabled="currentPage <= 1 || loading"
              @click="goToPreviousPage"
            />

            <Button
              label="Next"
              icon="pi pi-chevron-right"
              icon-pos="right"
              :disabled="!hasNextPage || loading"
              @click="goToNextPage"
            />
          </div>
        </div>
      </section>
    </section>
  </div>
</template>

<style scoped src="./styles/DatasetQueries.css"></style>
