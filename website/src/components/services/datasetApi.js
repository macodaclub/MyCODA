import { mockDatasetResults } from '../data/mockOntologyEntities'

// Temporary mock API.
// Later this function will call the backend endpoint.
export async function searchDatasets({ payload, queryString }) {
  console.log('Dataset API payload:', payload)
  console.log('Dataset API query string:', queryString)

  return mockDatasetResults
}