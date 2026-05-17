import { normalizeEntities, sortEntitiesByName } from './entityMapper'
import { mockOntologyEntities } from '../data/mockOntologyEntities'
import { ontologyConfig } from '../config/ontologyConfig'
import { useOntologyStore } from '@/store'

async function getMockOntologyEntities() {
  await delay(ontologyConfig.mockDelayMs)

  return normalizeEntities(mockOntologyEntities)
}

async function getBackendOntologyEntities() {
  const ontologyStore = useOntologyStore()

  const entities = await ontologyStore.fetchOntologyEntities()

  return normalizeEntities(entities ?? [])
}

export async function getOntologyEntities() {
  const entities = ontologyConfig.useMockData
    ? await getMockOntologyEntities()
    : await getBackendOntologyEntities()

  return sortEntitiesByName(entities)
}

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}