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
    </div>
</div>
  <div>
    <div class="table-toolbar">

        <div class="toolbar-actions">
            <Button
            :label="entityTableText.exportCsvLabel"
            icon="pi pi-download"
            severity="secondary"
            outlined
            @click="exportEntitiesCsv"
            />

            <Button
            :label="entityTableText.clearFiltersLabel"
            icon="pi pi-filter-slash"
            severity="secondary"
            outlined
            @click="clearFilters"
            />
        </div>

        <div>
                <InputText
                v-model="filters.global.value"
                :placeholder="entityTableText.globalSearchPlaceholder"
                class="w-full md:w-20rem"
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



<style scoped>
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.toolbar-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 1rem;
}

.entity-details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.detail-row {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 1rem;
}

.detail-row strong {
  color: var(--text-color);
}

.detail-row span {
  color: var(--text-color-secondary);
  line-height: 1.5;
}

/* css mostrar um resumo das entidades */

.entity-summary {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 1rem;
  margin-bottom: 1.25rem;
}

.entity-name {
  font-weight: 700;
  color: #f36b21;
}

.summary-card {
  border: 1px solid var(--surface-border);
  border-radius: 12px;
  padding: 1rem;
  background: var(--surface-50);
  cursor: pointer;
  transition: all 0.2s ease;
}

.summary-card:hover {
  border-color: #f36b21;
  background: #fff7ed;
  transform: translateY(-1px);
}
.summary-card span {
  display: block;
  color: var(--text-color-secondary);
  font-size: 0.85rem;
  margin-bottom: 0.35rem;
}

.summary-card strong {
  font-size: 1.4rem;
  color: var(--text-color);
}

.summary-card.active {
  border-color: #f36b21;
  background: #fff0e8;
  box-shadow: 0 6px 14px rgba(243, 107, 33, 0.12);
}

.summary-card.active span {
  color: #b9470d;
}

.summary-card.active strong {
  color: #f36b21;
}

@media (max-width: 1100px) {
  .entity-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

/*iltro não encontra resultados.*/
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 2rem;
  color: var(--text-color-secondary);
}

.empty-state i {
  font-size: 2rem;
  color: #f36b21;
}

.empty-state strong {
  color: var(--text-color);
  font-size: 1rem;
}

.filter-alert {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
  padding: 0.75rem 1rem;
  border-radius: 10px;
  background: #fff7ed;
  color: #b9470d;
  border: 1px solid #fed7aa;
}

.filter-alert i {
  color: #f36b21;
}
</style>