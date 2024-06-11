<script setup>

import Article from "@/components/Article.vue";
import Button from 'primevue/button';
import Dropdown from 'primevue/dropdown';
import InputText from 'primevue/inputtext';
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import SelectButton from 'primevue/selectbutton';
import {computed, ref} from "vue";
import {useOntologyStore} from "@/store/store.js";
import {storeToRefs} from "pinia";
import AutoComplete from "primevue/autocomplete";
import EntitiesTable from "@/components/EntitiesTable.vue";

const ontologyStore = useOntologyStore();
const {entities} = storeToRefs(ontologyStore);

const entityTypes = ref([
  "Class",
  "Individual",
  "DatatypeProperty",
  "ObjectProperty",
  "RelationalOperator",
  "Literal",
]);

const selectedPredefinedQuery = ref(null);
const predefinedQueries = ref([
  {
    value: "1",
    label: "What are the metaheuristics published after 2015?",
    queryTableData: [
      {
        "entityType": "Class",
        "entity": "MetaHeuristic",
        "inOutput": true,
        "orderBy": false,
      },
      {
        "entityType": "DatatypeProperty",
        "entity": "hasDevelopingYear",
        "inOutput": true,
        "orderBy": true,
      },
      {
        "entityType": "RelationalOperator",
        "entity": "swrlb:greaterThanOrEqual",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Literal",
        "entity": "2015",
        "inOutput": false,
        "orderBy": false,
      },
    ]
  },
  {
    value: "2",
    label: "What are the Java libraries implementing pNSGA-II?",
    queryTableData: [
      {
        "entityType": "Class",
        "entity": "ImplementationLibrary",
        "inOutput": true,
        "orderBy": true,
      },
      {
        "entityType": "ObjectProperty",
        "entity": "hasImplementationLanguage",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Individual",
        "entity": "Java",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "ObjectProperty",
        "entity": "hasSearchAlgorithm",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Individual",
        "entity": "pNSGA-II",
        "inOutput": false,
        "orderBy": false,
      },
    ]
  },
  {
    value: "3",
    label: "What are the metaheuristics that were tested in Knapsack problem?",
    queryTableData: [
      {
        "entityType": "Class",
        "entity": "MetaHeuristic",
        "inOutput": true,
        "orderBy": true,
      },
      {
        "entityType": "ObjectProperty",
        "entity": "canSolve",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Class",
        "entity": "Knapsack",
        "inOutput": false,
        "orderBy": false,
      },
    ]
  },
  {
    value: "4",
    label: "What are the metaheuristics that were tested in ZDT problem having Java libraries implementations?",
    queryTableData: [
      {
        "entityType": "Class",
        "entity": "MetaHeuristic",
        "inOutput": true,
        "orderBy": true,
      },
      {
        "entityType": "ObjectProperty",
        "entity": "canSolve",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Class",
        "entity": "ZDT",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Class",
        "entity": "MetaHeuristic",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "ObjectProperty",
        "entity": "useLibrary",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Class",
        "entity": "ImplementationLibrary",
        "inOutput": true,
        "orderBy": true,
      },
      {
        "entityType": "ObjectProperty",
        "entity": "hasImplementationLanguage",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Individual",
        "entity": "Java",
        "inOutput": false,
        "orderBy": false,
      },
    ]
  },
  {
    value: "5",
    label: "Which order relations have been proposed to many-objective optimisation?",
    queryTableData: [
      {
        "entityType": "Class",
        "entity": "MetaHeuristic",
        "inOutput": true,
        "orderBy": false,
      },
      {
        "entityType": "DatatypeProperty",
        "entity": "isManyObjectiveProblem",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Literal",
        "entity": "true",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Class",
        "entity": "MetaHeuristic",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "ObjectProperty",
        "entity": "usesOrderRelation",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Class",
        "entity": "OrderRelation",
        "inOutput": true,
        "orderBy": true,
      },
    ]
  },
  {
    value: "6",
    label: "Who are the researchers working both in decomposition-based and indicator-based metaheuristics?",
    queryTableData: [
      {
        "entityType": "Class",
        "entity": "Decomposition_based",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "ObjectProperty",
        "entity": "hasAuthor",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Class",
        "entity": "Researcher",
        "inOutput": true,
        "orderBy": true,
      },
      {
        "entityType": "Class",
        "entity": "Indicator_based",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "ObjectProperty",
        "entity": "hasAuthor",
        "inOutput": false,
        "orderBy": false,
      },
      {
        "entityType": "Class",
        "entity": "Researcher",
        "inOutput": false,
        "orderBy": false,
      },
    ]
  },
]);

const queryString = ref("");

const queryTableData = ref([
  {
    "entityType": "Class",
    "entity": "MetaHeuristic",
    "inOutput": true,
    "orderBy": true,
  }
]);

const yesNoOptions = [
  {
    value: false,
    label: "No",
  },
  {
    value: true,
    label: "Yes",
  }
];

const entitySuggestions = ref([]);
const updateEntitiesAutoComplete = (event, index) => {
  const entityType = queryTableData.value[index].entityType
  if (["Class", "Individual", "DatatypeProperty", "ObjectProperty"].includes(entityType)) {
    entitySuggestions.value = entities.value.filter(it => it.type === entityType && it.name.toLowerCase().includes(event.query.toLowerCase())).slice(0, 10).map(it => it.name);
  } else if (entityType === "RelationalOperator") {
    entitySuggestions.value = [
      "swrlb:equal",
      "swrlb:notEqual",
      "swrlb:lessThan",
      "swrlb:lessThanOrEqual",
      "swrlb:greaterThan",
      "swrlb:greaterThanOrEqual",
      "swrlb:stringEqualIgnoreCase",
      "swrlb:booleanNot",
    ];
  }
};
const areEntitiesDisabled = computed(() => queryTableData.value.map(it => it.entityType === ""));
const areEntitiesAutoComplete = computed(() => queryTableData.value.map(it => it.entityType !== "Literal"));

const clearEntity = index => {
  queryTableData.value[index].entity = "";
}

const addRowToQueryTable = () => {
  queryTableData.value.push({
    "entityType": "",
    "entity": "",
    "inOutput": false,
    "orderBy": false,
  });
};

const removeRowFromQueryTable = index => {
  queryTableData.value.splice(index, 1);
};

const resetQueryTable = () => {
  queryTableData.value = [];
};

const selectPredefinedQuery = () => {
  const selected = predefinedQueries.value.find(it => it.value === selectedPredefinedQuery.value);
  if (selected.queryTableData !== undefined) {
    queryTableData.value = [...selected.queryTableData];
  }
}

const generateQuery = () => {
  const queryTableDataValue = queryTableData.value;
  let usedEntitiesOrIndividuals = [];
  let lastVariable = null;
  const queryAtoms = queryTableDataValue
      .map((entry, index) => {
        if (entry.entityType === "Class") {
          lastVariable = entry.entity;
          if (usedEntitiesOrIndividuals.includes(entry.entity)) {
            return null;
          }
          usedEntitiesOrIndividuals.push(entry.entity);
          return `${entry.entity}(?_${entry.entity}_)`;
        } else if (["DatatypeProperty", "ObjectProperty"].includes(entry.entityType)) {
          if (index + 1 < queryTableDataValue.length) {
            const nextEntry = queryTableDataValue[index + 1];
            let secondArgument;
            if (["Individual", "Literal"].includes(nextEntry.entityType)) {
              secondArgument = nextEntry.entity;
            } else if (nextEntry.entityType === "RelationalOperator") {
              secondArgument = `?_${entry.entity}_`;
            } else {
              secondArgument = `?_${nextEntry.entity}_`;
            }
            const result = `${entry.entity}(?_${lastVariable}_, ${secondArgument})`;
            if (nextEntry.entityType === "RelationalOperator") {
              lastVariable = entry.entity;
            }
            return result;
          }
        } else if (entry.entityType === "RelationalOperator") {
          if (index + 1 < queryTableDataValue.length) {
            const nextEntry = queryTableDataValue[index + 1];
            return `${entry.entity}(?_${lastVariable}_, ${nextEntry.entity})`;
          }
        } else {
          return null;
        }
      })
      .filter(it => it !== null);
  const selectOperators = queryTableDataValue.filter(it => it.inOutput).map(it => `sqwrl:select(?_${it.entity}_)`);
  const orderByOperators = queryTableDataValue.filter(it => it.orderBy).map(it => `sqwrl:orderBy(?_${it.entity}_)`);
  queryString.value = `${queryAtoms.join(" ^ ")} -> ${selectOperators.concat(orderByOperators).join(" ^ ")}`;
}

const runQuery = async () => {
  const response = await fetch("http://127.0.0.1:8081/sqwrl", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      "queryString": queryString.value
    }),
  });
  if (!response.ok) return console.error('Failed to run query', response);
  const resultEntityNames = await response.json();
  queryResultEntities.value = resultEntityNames.map(name => entities.value.find(it => it.name === name)).filter(it => it != null);
};

const queryResultEntities = ref(null);
const shouldShowQueryResults = computed(() => queryResultEntities.value != null);
</script>

<template>
  <Article>
    <template #header>
      Query the Ontology (select a predefined query or create your own query)
    </template>
    <div class="row">
      <Button @click="addRowToQueryTable">Add new row to query table</Button>
      <Button @click="resetQueryTable">Reset the query table</Button>
      <Dropdown class="expanded" v-model="selectedPredefinedQuery" :options="predefinedQueries"
                optionValue="value"
                optionLabel="label"
                placeholder="Select a predefined query…"
                @change="selectPredefinedQuery"/>
      <Button @click="generateQuery" class="last-button">Generate the query</Button>
    </div>
    <form class="row" @submit.prevent="runQuery">
      <InputText type="text" class="expanded" v-model="queryString"/>
      <Button class="last-button" type="submit">Run the query</Button>
    </form>
    <DataTable :value="queryTableData" editMode="cell" stripedRows resizableColumns columnResizeMode="fit"
               showGridlines :tableStyle="{width:'100%', tableLayout: 'fixed'}">
      <Column header="Delete Row">
        <template #body="{ index }">
          <Button @click="removeRowFromQueryTable(index)" icon="pi pi-times" class="small-rounded-button"
                  severity="danger"
                  rounded/>
        </template>
      </Column>
      <Column field="entityType" header="Entity Type">
        <template #body="{ data, field, index }">
          <Dropdown v-model="data[field]"
                    :options="entityTypes"
                    @change="clearEntity(index)"
                    placeholder="Select type"/>
        </template>
      </Column>
      <Column field="entity" header="Entity">
        <template #body="{ data, field, index }">
          <template v-if="areEntitiesAutoComplete[index]">
            <AutoComplete v-model="data[field]"
                          :suggestions="entitySuggestions"
                          @complete="updateEntitiesAutoComplete($event, index)"
                          :completeOnFocus="true"
                          :autoOptionFocus="true"
                          :disabled="areEntitiesDisabled[index]"
            />
          </template>
          <template v-else>
            <InputText v-model="data[field]" type="text" :disabled="areEntitiesDisabled[index]"/>
          </template>
        </template>
      </Column>
      <Column field="inOutput" header="In Output">
        <template #body="slotProps">
          <SelectButton v-model="slotProps.data.inOutput" :options="yesNoOptions" optionLabel="label"
                        optionValue="value" :allowEmpty="false"/>
        </template>
      </Column>
      <Column field="orderBy" header="Order By">
        <template #body="slotProps">
          <SelectButton v-model="slotProps.data.orderBy" :options="yesNoOptions" optionLabel="label"
                        optionValue="value" :allowEmpty="false"/>
        </template>
      </Column>
    </DataTable>
  </Article>
  <EntitiesTable v-if="shouldShowQueryResults" headerTitle="Query Results" :entities="queryResultEntities" />
</template>

<style scoped>
.row {
  display: flex;
  justify-content: space-between;
  gap: 10px;

  .expanded {
    flex-grow: 1;
  }

  .last-button {
    justify-content: center;
    min-width: 12rem;
  }
}

.small-rounded-button {
  width: 1.6rem;
  height: 1.6rem;
}
</style>