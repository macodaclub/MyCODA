import { isValidEntityType } from '../data/entityTypes'

export function normalizeEntity(entity) {
  return {
    type: isValidEntityType(entity.type) ? entity.type : 'Unknown',
    name: entity.name || '',
    details: entity.details || '',
    comment: entity.comment || ''
  }
}

export function normalizeEntities(entities) {
  return entities.map(normalizeEntity)
}

export function sortEntitiesByName(entities) {
  return [...entities].sort((a, b) =>
    a.name.localeCompare(b.name, undefined, { sensitivity: 'base' })
  )
}