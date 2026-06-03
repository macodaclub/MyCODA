import { runOntologyQuery } from './ontologyApi'

export async function executeOntologyQuery(query) {
  if (!query) {
    return {
      results: [],
      error: 'Generate a valid query before running it.'
    }
  }

  try {
    const result = await runOntologyQuery(query)

    return {
      results: normalizeQueryResults(result),
      error: null
    }
  } catch (error) {
    return {
      results: [],
      error: error.message || 'Unexpected error while running query.'
    }
  }
}

function normalizeQueryResults(result) {
  if (Array.isArray(result)) {
    return result
  }

  if (Array.isArray(result?.results)) {
    return result.results
  }

  if (Array.isArray(result?.data)) {
    return result.data
  }

  return []
}