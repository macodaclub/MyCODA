<script setup>
import {
  searchableEntityFields,
  entityDataKey,
  defaultSortField,
  defaultSortOrder,
  defaultRows,
  rowsPerPageOptions,
  entityTableText
} from '../data/entityTableConfig'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import InputText from 'primevue/inputtext'
import Select from 'primevue/select'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import { ENTITY_TYPES, entityTypeOptions } from '../data/entityTypes'

import { ref , computed } from 'vue'
import { FilterMatchMode } from '@primevue/core/api'


const props = defineProps({
  entities: {
    type: Array,
    required: true
  }
})

// estado para a entidade selecionada
const selectedEntity = ref(null)
const showEntityDialog = ref(false)

function openEntityDetails(entity) {
  selectedEntity.value = entity
  showEntityDialog.value = true
}


const filters = ref(createInitialFilters())

function getSeverity(type) {
  switch (type) {
    case ENTITY_TYPES.CLASS:
      return 'info'

    case ENTITY_TYPES.INDIVIDUAL:
      return 'success'

    case ENTITY_TYPES.OBJECT_PROPERTY:
      return 'warning'

    case ENTITY_TYPES.DATATYPE_PROPERTY:
      return 'secondary'

    default:
      return null
  }
}

//mostrar um resumo das entidades
    const totalEntities = computed(() => props.entities.length)

    const totalClasses = computed(() =>
    props.entities.filter(entity => entity.type === 'Class').length
    )

    const totalIndividuals = computed(() =>
    props.entities.filter(entity => entity.type === 'Individual').length
    )

    const totalObjectProperties = computed(() =>
    props.entities.filter(entity => entity.type === 'ObjectProperty').length
    )

    const totalDatatypeProperties = computed(() =>
    props.entities.filter(entity => entity.type === 'DatatypeProperty').length
    )

    // exportar entidades n pagina atual

    const dataTableRef = ref(null)
    function exportEntitiesCsv() {
    dataTableRef.value.exportCSV()
    }
    // limpar filtros da tabela
function clearFilters() {
  filters.value = createInitialFilters()
  activeTypeFilter.value = null
}

// shrink text
function truncateText(text, maxLength = 80) {
  if (!text) return ''

  if (text.length <= maxLength) {
    return text
  }

  return text.substring(0, maxLength) + '...'
}

function filterByType(type) {
  activeTypeFilter.value = type
  filters.value.type.value = type
}

const activeTypeFilter = ref(null)

function onTypeFilterChange(filterModel, filterCallback) {
  activeTypeFilter.value = filterModel.value
  filterCallback()
}
// verificar se há filtros ativos
const hasActiveFilters = computed(() => {
  return Boolean(
    filters.value.global.value ||
    filters.value.type.value ||
    filters.value.name.value ||
    filters.value.details.value ||
    filters.value.comment.value
  )
})

const filteredEntitiesCount = ref(props.entities.length)

function onValueChange(filteredData) {
  filteredEntitiesCount.value = filteredData.length
}
//evitar repetição na criação dos filtros.
function createInitialFilters() {
  return {
    global: {
      value: null,
      matchMode: FilterMatchMode.CONTAINS
    },
    type: {
      value: null,
      matchMode: FilterMatchMode.CONTAINS
    },
    name: {
      value: null,
      matchMode: FilterMatchMode.CONTAINS
    },
    details: {
      value: null,
      matchMode: FilterMatchMode.CONTAINS
    },
    comment: {
      value: null,
      matchMode: FilterMatchMode.CONTAINS
    }
  }
}


</script>

<template>
<div class="entity-summary">
<!--
<div
  class="summary-card"
  :class="{ active: activeTypeFilter === null }"
  v-tooltip="'Show all ontology entities'"
  @click="filterByType(null)"
>
  <span>Total</span>
  <strong>{{ totalEntities }}</strong>
</div>

<div
  class="summary-card"
  :class="{ active: activeTypeFilter === ENTITY_TYPES.CLASS }"
  v-tooltip="'Filter by classes'"
  @click="filterByType(ENTITY_TYPES.CLASS)"
>
  <span>Classes</span>
  <strong>{{ totalClasses }}</strong>
</div>

<div
  class="summary-card"
  :class="{ active: activeTypeFilter === ENTITY_TYPES.INDIVIDUAL }"
  v-tooltip="'Filter by individuals'"
  @click="filterByType(ENTITY_TYPES.INDIVIDUAL)"
>
  <span>Individuals</span>
  <strong>{{ totalIndividuals }}</strong>
</div>

   <div
    class="summary-card"
    :class="{ active: activeTypeFilter === ENTITY_TYPES.OBJECT_PROPERTY }"
    v-tooltip="'Filter by object properties'"
    @click="filterByType(ENTITY_TYPES.OBJECT_PROPERTY)"
    >
    <span>Object Properties</span>
    <strong>{{ totalObjectProperties }}</strong>
    </div>

    <div
    class="summary-card"
    :class="{ active: activeTypeFilter === ENTITY_TYPES.DATATYPE_PROPERTY }"
    v-tooltip="'Filter by datatype properties'"
    @click="filterByType(ENTITY_TYPES.DATATYPE_PROPERTY)"
    >
    <span>Datatype Properties</span>
    <strong>{{ totalDatatypeProperties }}</strong>
    </div> -->
</div>





  <div>
    <div class="table-toolbar">
        <div>
                <InputText
                v-model="filters.global.value"
                :placeholder="entityTableText.globalSearchPlaceholder"
                class="w-full md:w-20rem"
            />
        </div>
        <div class="toolbar-actions">
            <Button
            :label="entityTableText.clearFiltersLabel"
            icon="pi pi-filter-slash"
            severity="secondary"
            outlined
            @click="clearFilters"
            />
            <Button
            :label="entityTableText.exportCsvLabel"
            icon="pi pi-download"
            severity="secondary"
            outlined
             class="mr-2"
            @click="exportEntitiesCsv"
            />
        </div>


    </div>
    
    <div v-if="hasActiveFilters" class="filter-alert">
        <i class="pi pi-filter"></i>
    <span>Showing {{ filteredEntitiesCount }} of {{ totalEntities }} entities.</span>

    <Button
        label="Clear"
        text
        size="small"
        @click="clearFilters"
    />
    </div>

  <DataTable
    ref="dataTableRef"
    :value="entities"
    v-model:filters="filters"
    paginator
   :rows="defaultRows"
   :rowsPerPageOptions="rowsPerPageOptions"
    :dataKey="entityDataKey"
    filterDisplay="row"
    :globalFilterFields="searchableEntityFields"
    :sortField="defaultSortField"
    :sortOrder="defaultSortOrder"
    stripedRows
    showGridlines
    responsiveLayout="scroll"
    @value-change="onValueChange"
  >
    <template #empty>
        <div class="empty-state">
        <i class="pi pi-search"></i>

        <strong>No ontology entities found</strong>

        <span>
            Try changing the filters or clearing the current search.
        </span>
        </div>
    </template>
      <Column
        field="type"
        header="Type"
        sortable
        filter
        style="width: 180px"
      >
        <template #body="{ data }">
          <Tag
            :value="data.type"
            :severity="getSeverity(data.type)"
          />
        </template>

        <template #filter="{ filterModel, filterCallback }">
        <Select
          v-model="filterModel.value"
          :options="entityTypeOptions"
          optionLabel="label"
          optionValue="value"
          :placeholder="entityTableText.filterTypePlaceholder"
          class="w-full"
          @change="onTypeFilterChange(filterModel, filterCallback)"
        />
        </template>
        
      </Column>

        <Column
        field="name"
        header="Name"
        sortable
        filter
        style="width: 220px"
        >
        <template #filter="{ filterModel, filterCallback }">
          <InputText
            v-model="filterModel.value"
            type="text"
            placeholder="Filter name"
            @input="filterCallback()"
          />
        </template>
      </Column>

    <Column
    field="details"
    header="Details"
    filter
    >
    <template #body="{ data }">
        <span :title="data.details">
        {{ truncateText(data.details, 80) }}
        </span>
    </template>

    <template #filter="{ filterModel, filterCallback }">
        <InputText
        v-model="filterModel.value"
        type="text"
        placeholder="Filter details"
        @input="filterCallback()"
        />
    </template>
    </Column>

    <Column
    field="comment"
    header="Comment"
    filter
    >
    <template #body="{ data }">
        <span :title="data.comment">
        {{ truncateText(data.comment, 100) }}
        </span>
    </template>

    <template #filter="{ filterModel, filterCallback }">
        <InputText
        v-model="filterModel.value"
        type="text"
        placeholder="Filter comment"
        @input="filterCallback()"
        />
    </template>
    </Column>
      <Column
        header="Actions"
        style="width: 120px"
        >
        <template #body="{ data }">
            <Button
            :label="entityTableText.viewLabel"
            icon="pi pi-eye"
            size="small"
            severity="secondary"
            @click="openEntityDetails(data)"
            />
        </template>
        </Column>
    </DataTable>
    <Dialog
    v-model:visible="showEntityDialog"
    modal
    :header="selectedEntity ? `Entity Details — ${selectedEntity.name}` : 'Entity Details'"
    :style="{ width: '45rem' }"
    >
    <div v-if="selectedEntity" class="entity-details">
        <div class="detail-row">
        <strong>Type:</strong>

        <Tag
            :value="selectedEntity.type"
            :severity="getSeverity(selectedEntity.type)"
        />
        </div>

        <div class="detail-row">
        <strong>Name:</strong>
        <span>{{ selectedEntity.name }}</span>
        </div>

        <div class="detail-row">
        <strong>Details:</strong>
        <span>{{ selectedEntity.details || 'No details available.' }}</span>
        </div>

        <div class="detail-row">
        <strong>Comment:</strong>
        <span>{{ selectedEntity.comment || 'No comment available.' }}</span>
        </div>

        <div class="dialog-actions">

        </div>
    </div>
    </Dialog>
  </div>
</template>

<style scoped src="./styles/OntologyEntitiesTable.css"></style>