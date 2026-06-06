import { runOntologyQuery } from './ontologyApi'

export async function executeOntologyQuery(query) {
  if (!query) {
    return {
      results: [],
      count: 0,
      executedQuery: '',
      error: 'Generate a valid query before running it.'
    }
  }

  try {
    const result = await runOntologyQuery(query)

    return {
      results: normalizeQueryResults(result),
      count: result?.count ?? 0,
      executedQuery: result?.query ?? query,
      error: null
    }
  } catch (error) {
    return {
      results: [],
      count: 0,
      executedQuery: query,
      error: error.message || 'Unexpected error while running query.'
    }
  }
}

function normalizeQueryResults(result) {
  if (Array.isArray(result)) {
    return result.map(item => {
      if (typeof item === 'string') {
        return {
          result: item
        }
      }

      return item
    })
  }

  if (Array.isArray(result?.results)) {
    return result.results.map(item => {
      if (typeof item === 'string') {
        return {
          result: item
        }
      }

      return item
    })
  }

  if (Array.isArray(result?.data)) {
    return result.data
  }

  return []
}