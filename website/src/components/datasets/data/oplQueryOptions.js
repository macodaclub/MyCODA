export const oplFilteringCriteria = [
  { label: 'Identifier', value: 'id' },
  { label: 'Name', value: 'name' },
  { label: 'Long name', value: 'longName' },
  { label: 'Description', value: 'description' },
  { label: 'Number of objectives', value: 'objectives' },
  { label: 'Variable type', value: 'variableTypes' },
  { label: 'Variable dimension', value: 'variableDimensions' },
  { label: 'Constraint type', value: 'constraintTypes' },
  { label: 'Number of constraints', value: 'numberOfConstraints' },
  { label: 'Modality', value: 'modality' },
  { label: 'Noise type', value: 'noiseType' },
  { label: 'Type', value: 'type' },
  { label: 'Author', value: 'authors' },
  { label: 'Reference title', value: 'referenceTitles' },
  { label: 'Link', value: 'links' },
]

export const oplSortFields = [
  { label: 'No sorting', value: '' },
  { label: 'Identifier', value: 'id' },
  { label: 'Name', value: 'name' },
  { label: 'Long name', value: 'longName' },
  { label: 'Description', value: 'description' },
  { label: 'Number of objectives', value: 'objectives' },
  { label: 'Variable type', value: 'variableTypes' },
  { label: 'Variable dimension', value: 'variableDimensions' },
  { label: 'Constraint type', value: 'constraintTypes' },
  { label: 'Number of constraints', value: 'numberOfConstraints' },
  { label: 'Modality', value: 'modality' },
  { label: 'Noise type', value: 'noiseType' },
  { label: 'Type', value: 'type' },
  { label: 'Author', value: 'authors' },
  { label: 'Reference title', value: 'referenceTitles' },
  { label: 'Link', value: 'links' },
]

export const oplOutputFieldOptions = [
  { label: 'Identifier', value: 'id' },
  { label: 'Name', value: 'name' },
  { label: 'Long name', value: 'longName' },
  { label: 'Description', value: 'description' },
  { label: 'Objectives', value: 'objectives' },
  { label: 'Variable types', value: 'variableTypes' },
  { label: 'Variable dimensions', value: 'variableDimensions' },
  { label: 'Constraint types', value: 'constraintTypes' },
  { label: 'Number of constraints', value: 'numberOfConstraints' },
  { label: 'Modality', value: 'modality' },
  { label: 'Noise type', value: 'noiseType' },
  { label: 'Type', value: 'type' },
  { label: 'Source', value: 'source' },
  { label: 'Authors', value: 'authors' },
  { label: 'Reference titles', value: 'referenceTitles' },
  { label: 'Links', value: 'links' },
]

export const defaultOplOutputFields = [
  'name',
  'description',
  'objectives',
  'variableTypes',
  'modality',
  'type',
  'links',
]

export const oplPredefinedValueOptions = {
  type: [
    { label: 'Problem', value: 'problem' },
    { label: 'Suite', value: 'suite' },
    { label: 'Implementation', value: 'implementation' },
  ],

  modality: [
    { label: 'Unimodal', value: 'unimodal' },
    { label: 'Multimodal', value: 'multimodal' },
  ],

  variableTypes: [
    { label: 'Continuous', value: 'continuous' },
    { label: 'Integer', value: 'integer' },
    { label: 'Binary', value: 'binary' },
    { label: 'Categorical', value: 'categorical' },
    { label: 'Mixed', value: 'mixed' },
  ],
}

/**
 * Estes campos colidem semanticamente com campos OpenAIRE (ou com o id
 * composto da linha híbrida), pelo que recebem aliases explícitos no HYBRID.
 */
const hybridOplFieldAliases = Object.freeze({
  id: 'oplId',
  type: 'oplType',
  source: 'oplSource',
  authors: 'oplAuthors',
})

export function toHybridOplField(field) {
  return hybridOplFieldAliases[field] ?? field
}

export const hybridOplOutputFieldOptions = oplOutputFieldOptions.map(
  option => ({
    label: `OPL — ${option.label}`,
    value: toHybridOplField(option.value),
  }),
)

export const defaultHybridOplOutputFields = defaultOplOutputFields.map(
  toHybridOplField,
)

export const hybridOplSortFields = oplSortFields
  .filter(option => option.value)
  .map(option => ({
    label: `OPL — ${option.label}`,
    value: toHybridOplField(option.value),
  }))

export function hasOplPredefinedValueOptions(field) {
  return Object.prototype.hasOwnProperty.call(
    oplPredefinedValueOptions,
    field,
  )
}

export function getOplPredefinedValueOptions(field) {
  return oplPredefinedValueOptions[field] ?? []
}
