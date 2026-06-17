export const ENTITY_TYPES = {
  CLASS: 'Class',
  INDIVIDUAL: 'Individual',
  DATATYPE_PROPERTY: 'DatatypeProperty',
  OBJECT_PROPERTY: 'ObjectProperty',
  RELATIONAL_OPERATOR: 'RelationalOperator',
  LITERAL: 'Literal'
}


export const entityTypeOptions = [
  { label: 'Class', value: ENTITY_TYPES.CLASS },
  { label: 'Individual', value: ENTITY_TYPES.INDIVIDUAL },
  { label: 'Data Property', value: ENTITY_TYPES.DATATYPE_PROPERTY },
  { label: 'Object Property', value: ENTITY_TYPES.OBJECT_PROPERTY },
/*   { label: 'Relational Operator', value: ENTITY_TYPES.RELATIONAL_OPERATOR },
  { label: 'Literal', value: ENTITY_TYPES.LITERAL } */
]

export const relationalOperatorOptions = [
  { label: 'equal', value: 'swrlb:equal' },
  { label: 'notEqual', value: 'swrlb:notEqual' },
  { label: 'lessThan', value: 'swrlb:lessThan' },
  { label: 'lessThanOrEqual', value: 'swrlb:lessThanOrEqual' },
  { label: 'greaterThan', value: 'swrlb:greaterThan' },
  { label: 'greaterThanOrEqual', value: 'swrlb:greaterThanOrEqual' },
  { label: 'stringEqualIgnoreCase', value: 'swrlb:stringEqualIgnoreCase' },
  { label: 'booleanNot', value: 'swrlb:booleanNot' }
]
export function isValidEntityType(type) {
  return Object.values(ENTITY_TYPES).includes(type)
}

