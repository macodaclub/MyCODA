import { computed, onMounted, ref } from 'vue'

import { getOntologyEntities } from '../services/ontologyApi'
import { normalizeOntologyEntities } from '../services/ontologyEntityUtils'

export function useOntologyEntities(propsEntities) {
  const localEntities = ref([])
  const isLoadingEntities = ref(false)
  const entitiesError = ref(null)

  const normalizedEntities = computed(() => {
    const sourceEntities = propsEntities.value.length
      ? propsEntities.value
      : localEntities.value

    return normalizeOntologyEntities(sourceEntities)
  })

  const hasEntities = computed(() => normalizedEntities.value.length > 0)

  onMounted(() => {
    loadEntitiesIfNeeded()
  })

  async function loadEntitiesIfNeeded() {
    if (propsEntities.value.length) {
      return
    }

    try {
      isLoadingEntities.value = true
      entitiesError.value = null

      const result = await getOntologyEntities()

      localEntities.value = Array.isArray(result)
        ? result
        : result.entities ?? []
    } catch (error) {
      entitiesError.value =
        error.message || 'Failed to load ontology entities.'
    } finally {
      isLoadingEntities.value = false
    }
  }

  return {
    normalizedEntities,
    hasEntities,
    isLoadingEntities,
    entitiesError,
    loadEntitiesIfNeeded
  }
}