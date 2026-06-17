<script setup>
import { ref, onMounted, computed } from 'vue'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import Button from 'primevue/button'
import Skeleton from 'primevue/skeleton'
import EntitiesTree from "./EntitiesTree.vue";
import OntologyQueryBuilder from '@/components/ontology/OntologyQueryBuilder.vue'

import {getOntologyEntities} from './services/ontologyApi' 
import OntologyEntitiesTable from './ontology/OntologyEntitiesTable.vue'

const entityData = ref([])
const loading = ref(false)
const errorMessage = ref('')

async function loadEntities() {
  if (loading.value) return

  try {
    loading.value = true
    errorMessage.value = ''

    entityData.value = await getOntologyEntities()
  } catch (error) {
    console.error(error)
    errorMessage.value = 'Could not load ontology entities.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadEntities()
})

const hasEntities = computed(() => entityData.value.length > 0)
</script>

<template>
  <div class="flex justify-center">
    <div class="w-11/12 border rounded-xl bg-surface-0 p-8 m-4 mt-5 ontology-card">
      <div class="mb-10">
        <p class="ontology-description">
          Explore, query and visualise the ontology interactively.
        </p>
      </div>

      <Tabs value="entities">
        <TabList>
          <Tab value="entities">
            Ontology Entities
<!--             <span class="tab-count">
              {{ entityData.length }}
            </span>
 -->          </Tab>
          <Tab value="browse-page">
            <div class="tab-title">
              <i class="pi pi-code"></i>
              <span>Ontology Browser</span>
            </div>
          </Tab>
          <Tab value="query-builder">
            <div class="tab-title">
              <i class="pi pi-code"></i>
              <span>Interactive Query Builder</span>
            </div>
          </Tab>
          <Tab value="ontology-visualization">
            <div class="tab-title">
              <i class="pi pi-sitemap"></i>
              <span>Ontology Visualization</span>
            </div>
          </Tab>
          <Tab value="ontology-statistics">
            <div class="tab-title">
              <i class="pi pi-sitemap"></i>
              <span>Ontology Statistics</span>
            </div>
          </Tab>
        </TabList>

        <TabPanels>
          <TabPanel value="entities">
            <section class="tab-panel-content">
            <div class="section-header">

              <div class="section-actions">
                <p>
                  Browse and filter ontology entities by type.
                </p>

<!--                 <Button
                  label="Reload entities"
                  icon="pi pi-refresh"
                  severity="secondary"
                  outlined
                  size="small"
                  :loading="loading"
                  :disabled="loading"
                  @click="loadEntities"
                />
 -->              </div>
            </div>

            <div v-if="loading" class="entities-loading">
              <Skeleton width="100%" height="3rem" class="mb-3" />
              <Skeleton width="100%" height="3rem" class="mb-3" />
              <Skeleton width="100%" height="3rem" class="mb-3" />
              <Skeleton width="100%" height="3rem" class="mb-3" />
            </div>

              <div v-else-if="errorMessage" class="error-message">
                {{ errorMessage }}
              </div>

              <div v-else-if="!hasEntities" class="empty-message">
                No ontology entities available.
              </div>

              <OntologyEntitiesTable
                v-else
                :entities="entityData"
              />
            </section>
          </TabPanel>

          <TabPanel value="ontology-visualization">
            <section class="tab-panel-content">
              <div class="section-header">
                <div>
                  <h2>WebOWL</h2>
                </div>

                <p>
                  Explore visualy the ontology classes, individuals and their relationships.
                </p>
              </div>

              <!-- Aqui entra depois a visualização da ontologia -->

            </section>
          </TabPanel>

          <TabPanel value="ontology-statistics">
            <section class="tab-panel-content">
              <div class="section-header">
                <div>
                  <h2>Ontology Statistics</h2>
                </div>

                <p>
                  Ontology: number of classes, individuals, object properties and data properties.
                </p>
              </div>

              <!-- Aqui entra depois a visualização da ontologia -->

            </section>
          </TabPanel>

          <TabPanel value="query-builder">
            <section class="tab-panel-content">
              <OntologyQueryBuilder :entities="entities" />

              <!-- Aqui entra depois o query builder -->
              <!-- <OntologyQueryBuilder :entity-data="entityData" /> -->
            </section>
          </TabPanel>
          <TabPanel value="browse-page">
            <section class="tab-panel-content">
                  <EntitiesTree usesRouter />
                  <EntitiesTable :entities="entities"/>
    
            </section>
          </TabPanel>
        </TabPanels>
      </Tabs>
    </div>
  </div>
</template>

<style scoped>
.ontology-card {
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.entities-loading {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.page-label {
  display: inline-block;
  color: #f36b21;
  font-size: 0.78rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.12em;
  margin-bottom: 0.75rem;
}

.page-title {
  font-size: 2rem;
  font-weight: 800;
  color: #101828;
  margin-bottom: 1rem;
}

.ontology-description {
  color: var(--text-color-secondary);
  max-width: 720px;
  line-height: 1.6;
}

.tab-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
}

.tab-title i {
  color: #f36b21;
}

.tab-panel-content {
  padding: 1.5rem 0.5rem 0.5rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  gap: 2rem;
  align-items: flex-end;
  margin-bottom: 1.5rem;
}

.section-header h2 {
  margin-top: 0.5rem;
  font-size: 1.35rem;
  font-weight: 800;
  color: #101828;
}

.section-header p {
  max-width: 460px;
  color: var(--text-color-secondary);
  line-height: 1.5;
}

.section-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #fff0e8;
  color: #f36b21;
  border: 1px solid #ffd7c0;
  font-weight: 800;
  font-size: 0.78rem;
  border-radius: 30px;
  padding: 4px 10px;
}

@media (max-width: 900px) {
  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}


/*Css erro*/

.empty-message {
  padding: 1rem;
  border-radius: 8px;
  background: var(--surface-50);
  color: var(--text-color-secondary);
  border: 1px solid var(--surface-border);
}


.section-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.section-actions p {
  margin: 0;
}

/*botão de contagem */
.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 0.5rem;
  min-width: 1.5rem;
  height: 1.5rem;
  padding: 0 0.4rem;
  border-radius: 999px;
  background: #fff0e8;
  color: #f36b21;
  font-size: 0.75rem;
  font-weight: 700;
}
</style>