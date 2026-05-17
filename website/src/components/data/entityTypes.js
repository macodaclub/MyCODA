export const ENTITY_TYPES = {
  CLASS: 'Class',
  INDIVIDUAL: 'Individual',
  DATATYPE_PROPERTY: 'DatatypeProperty',
  OBJECT_PROPERTY: 'ObjectProperty'
}



export const entityTypeOptions = [
  { label: 'All types', value: null },
  { label: 'Class', value: ENTITY_TYPES.CLASS },
  { label: 'Individual', value: ENTITY_TYPES.INDIVIDUAL },
  { label: 'Datatype Property', value: ENTITY_TYPES.DATATYPE_PROPERTY },
  { label: 'Object Property', value: ENTITY_TYPES.OBJECT_PROPERTY }
]

export function isValidEntityType(type) {
  return Object.values(ENTITY_TYPES).includes(type)
}

