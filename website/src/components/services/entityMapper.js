import { normalizeEntityType } from './ontologyEntityUtils'

export function normalizeEntities(entities) {
  return entities
    .map(entity => {
      const queryName = getShortNameFromIri(entity.details ?? entity.iri ?? entity.id)

      return {
        ...entity,
        type: normalizeEntityType(entity.type),
        name: entity.name ?? queryName,
        queryName: queryName || entity.name
      }
    })
    .filter(entity => entity.name)
}

export function sortEntitiesByName(entities) {
  return [...entities].sort((a, b) =>
    a.name.localeCompare(b.name, undefined, { sensitivity: 'base' })
  )
}

function getShortNameFromIri(value) {
  if (!value) {
    return ''
  }

  const text = String(value)

  if (text.includes('#')) {
    return text.substring(text.lastIndexOf('#') + 1)
  }

  if (text.includes('/')) {
    return text.substring(text.lastIndexOf('/') + 1)
  }

  return text
}