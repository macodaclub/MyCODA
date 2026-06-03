import { ENTITY_TYPES } from '../data/entityTypes'

export function normalizeEntityType(type) {
  const value = String(type ?? '').trim().toLowerCase()

  switch (value) {
    case 'class':
    case 'classes':
      return ENTITY_TYPES.CLASS

    case 'individual':
    case 'individuals':
    case 'namedindividual':
    case 'named individual':
    case 'namedindividuals':
      return ENTITY_TYPES.INDIVIDUAL

    case 'datatypeproperty':
    case 'datatype property':
    case 'data property':
    case 'dataproperty':
    case 'data_property':
    case 'data-property':
      return ENTITY_TYPES.DATATYPE_PROPERTY

    case 'objectproperty':
    case 'object property':
    case 'object_property':
    case 'object-property':
      return ENTITY_TYPES.OBJECT_PROPERTY

    case 'relationaloperator':
    case 'relational operator':
      return ENTITY_TYPES.RELATIONAL_OPERATOR

    case 'literal':
      return ENTITY_TYPES.LITERAL

    default:
      return type
  }
}

export function extractNameFromIri(value) {
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

export function normalizeOntologyEntities(entities) {
  return entities
    .map(entity => ({
      ...entity,
      type: normalizeEntityType(entity.type),
      name: entity.name ?? entity.label ?? extractNameFromIri(entity.details ?? entity.id)
    }))
    .filter(entity => entity.name)
}

export function buildEntityOptionsByType(entities, type) {
  return entities
    .filter(entity => entity.type === type)
    .map(entity => ({
      label: entity.name,
      value: entity.name
    }))
    .sort((a, b) =>
      a.label.localeCompare(b.label, undefined, { sensitivity: 'base' })
    )
}