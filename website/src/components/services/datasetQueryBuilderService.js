export function buildDatasetPayload(criteria) {
  return criteria
    .filter(item => item.filteringCriterion || item.filteringCriterionValue)
    .map(({ id, ...item }) => item)
}

export function buildDatasetQueryString(criteria) {
  const validCriteria = buildDatasetPayload(criteria)

  if (validCriteria.length === 0) {
    throw new Error('Please add at least one filtering criterion.')
  }

  const queryParts = []

  for (const criterion of validCriteria) {
    const {
      filteringCriterion,
      filteringCriterionValue,
      includeExcludeFilter,
      sortedByField,
      sortedInAscOrDescOrder,
    } = criterion

    if (!filteringCriterion) {
      throw new Error('Please select a filtering criterion.')
    }

    if (!filteringCriterionValue) {
      throw new Error(`Please assign a value to ${filteringCriterion}.`)
    }

    if (filteringCriterion !== 'showFieldInOutputTable') {
      let value = filteringCriterionValue

      if (filteringCriterion === 'rorId') {
        value = value.replace(/\s/g, '')
      }

      if (includeExcludeFilter === 'exclude') {
        value = `NOT ${value}`
      }

      queryParts.push(
        `${encodeURIComponent(filteringCriterion)}=${encodeURIComponent(value)}`,
      )
    }

    if (sortedByField || sortedInAscOrDescOrder) {
      if (!sortedByField || !sortedInAscOrDescOrder) {
        throw new Error('Please complete the sorting criteria.')
      }

      queryParts.push(
        `sortBy=${encodeURIComponent(`${sortedByField} ${sortedInAscOrDescOrder}`)}`,
      )
    }
  }

  return queryParts.join('&')
}