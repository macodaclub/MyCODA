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
  { label: 'Constraint hardness', value: 'constraintHardness' },
  { label: 'Equality constraints', value: 'constraintEquality' },
  { label: 'Modality', value: 'modality' },
  { label: 'Noise type', value: 'noiseType' },
  { label: 'Entity type', value: 'type' },
  { label: 'Problem source', value: 'problemSources' },
  { label: 'Dynamic type', value: 'dynamicTypes' },
  { label: 'Fidelity level', value: 'fidelityLevels' },
  { label: 'Evaluation time', value: 'evaluationTimes' },
  { label: 'Suite problems', value: 'problems' },
  { label: 'Instances', value: 'instances' },
  { label: 'Code example', value: 'codeExamples' },
  { label: 'Implementation', value: 'implementationNames' },
  { label: 'Implementation link', value: 'implementationLinks' },
  { label: 'Implementation language', value: 'implementationLanguages' },
  { label: 'Implementation evaluation time', value: 'implementationEvaluationTimes' },
  { label: 'Tag', value: 'tags' },
  { label: 'Allows partial evaluation', value: 'allowsPartialEvaluation' },
  { label: 'Objectives independently evaluable', value: 'canEvaluateObjectivesIndependently' },
  { label: 'Author', value: 'authors' },
  { label: 'Reference title', value: 'referenceTitles' },
  { label: 'Link', value: 'links' },
]

export const oplSortFields = [
  { label: 'No sorting', value: '' },
  ...oplFilteringCriteria.map(option => ({ ...option })),
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
  { label: 'Constraint hardness', value: 'constraintHardness' },
  { label: 'Equality constraints', value: 'constraintEquality' },
  { label: 'Modality', value: 'modality' },
  { label: 'Noise type', value: 'noiseType' },
  { label: 'Entity type', value: 'type' },
  { label: 'Problem source', value: 'problemSources' },
  { label: 'Dynamic type', value: 'dynamicTypes' },
  { label: 'Fidelity levels', value: 'fidelityLevels' },
  { label: 'Evaluation times', value: 'evaluationTimes' },
  { label: 'Suite problems', value: 'problems' },
  { label: 'Instances', value: 'instances' },
  { label: 'Code examples', value: 'codeExamples' },
  { label: 'Implementation IDs', value: 'implementationIds' },
  { label: 'Implementations', value: 'implementationNames' },
  { label: 'Implementation links', value: 'implementationLinks' },
  { label: 'Implementation languages', value: 'implementationLanguages' },
  { label: 'Implementation evaluation times', value: 'implementationEvaluationTimes' },
  { label: 'Tags', value: 'tags' },
  { label: 'Allows partial evaluation', value: 'allowsPartialEvaluation' },
  { label: 'Objectives independently evaluable', value: 'canEvaluateObjectivesIndependently' },
  { label: 'OPL data source', value: 'source' },
  { label: 'Authors', value: 'authors' },
  { label: 'Reference titles', value: 'referenceTitles' },
  { label: 'All links', value: 'links' },
]

export const defaultOplOutputFields = [
  'name',
  'description',
  'objectives',
  'variableTypes',
  'modality',
  'type',
  'problemSources',
  'implementationLinks',
  'referenceTitles',
]

export const oplPredefinedValueOptions = {
  type: [
    { label: 'Problem', value: 'problem' },
    { label: 'Suite', value: 'suite' },
    { label: 'Generator', value: 'generator' },
  ],
  variableTypes: [
    { label: 'Continuous', value: 'continuous' },
    { label: 'Integer', value: 'integer' },
    { label: 'Binary', value: 'binary' },
    { label: 'Categorical', value: 'categorical' },
    { label: 'Unknown', value: 'unknown' },
  ],
  allowsPartialEvaluation: [
    { label: 'Yes', value: 'yes' },
    { label: 'No', value: 'no' },
    { label: 'Some', value: 'some' },
  ],
  canEvaluateObjectivesIndependently: [
    { label: 'Yes', value: 'yes' },
    { label: 'No', value: 'no' },
    { label: 'Some', value: 'some' },
  ],
}

const hybridOplFieldAliases = Object.freeze({
  id: 'oplId',
  type: 'oplType',
  source: 'oplSource',
  authors: 'oplAuthors',
  constraintHardness: 'oplConstraintHardness',
  constraintEquality: 'oplConstraintEquality',
  problemSources: 'oplProblemSources',
  dynamicTypes: 'oplDynamicTypes',
  fidelityLevels: 'oplFidelityLevels',
  evaluationTimes: 'oplEvaluationTimes',
  problems: 'oplProblems',
  instances: 'oplInstances',
  codeExamples: 'oplCodeExamples',
  implementationIds: 'oplImplementationIds',
  implementationNames: 'oplImplementationNames',
  implementationLinks: 'oplImplementationLinks',
  implementationLanguages: 'oplImplementationLanguages',
  implementationEvaluationTimes: 'oplImplementationEvaluationTimes',
  tags: 'oplTags',
  allowsPartialEvaluation: 'oplAllowsPartialEvaluation',
  canEvaluateObjectivesIndependently: 'oplCanEvaluateObjectivesIndependently',
})

export function toHybridOplField(field) {
  return hybridOplFieldAliases[field] ?? field
}

export const hybridOplOutputFieldOptions = oplOutputFieldOptions.map(option => ({
  label: `OPL — ${option.label}`,
  value: toHybridOplField(option.value),
}))

export const defaultHybridOplOutputFields = defaultOplOutputFields.map(toHybridOplField)

export const hybridOplSortFields = oplSortFields
  .filter(option => option.value)
  .map(option => ({ label: `OPL — ${option.label}`, value: toHybridOplField(option.value) }))

export function hasOplPredefinedValueOptions(field) {
  return Object.prototype.hasOwnProperty.call(oplPredefinedValueOptions, field)
}

export function getOplPredefinedValueOptions(field) {
  return oplPredefinedValueOptions[field] ?? []
}
