import { ENTITY_TYPES } from './entityTypes'

export const mockOntologyEntities = [
  {
    type: ENTITY_TYPES.CLASS,
    name: 'MetaHeuristic',
    details: 'ascendentClassesHierarchy: [Algorithm]',
    comment: 'A metaheuristic optimization method.',
  },
  {
    type: ENTITY_TYPES.INDIVIDUAL,
    name: 'NSGA-II',
    details: 'ascendentClassesHierarchy: [MetaHeuristic]',
    comment: 'Non-dominated Sorting Genetic Algorithm II.',
  },
  {
    type: ENTITY_TYPES.INDIVIDUAL,
    name: 'NSGA-III',
    details: 'ascendentClassesHierarchy: [MetaHeuristic]',
    comment: 'Many-objective evolutionary algorithm.',
  },
  {
    type: ENTITY_TYPES.DATATYPE_PROPERTY,
    name: 'hasDevelopingYear',
    details: 'classDomain: [MetaHeuristic], dataTypeRange: [integer]',
    comment: 'Year when the method was developed.',
  },
  {
    type: ENTITY_TYPES.OBJECT_PROPERTY,
    name: 'canSolve',
    details: 'classDomain: [MetaHeuristic], classRange: [Problem]',
    comment: 'Relates an algorithm with a problem it can solve.',
  },
]

export const mockDatasetResults = [
  {
    id: 1,
    type: 'dataset',
    title: 'Example optimization benchmarking dataset',
    authors: 'Carola Doerr',
    publicationDate: '2025-01-01',
    publisher: 'OpenAIRE',
  },
  {
    id: 2,
    type: 'publication',
    title: 'Benchmarking evolutionary multi-objective optimization algorithms',
    authors: 'Carola Doerr',
    publicationDate: '2026-01-01',
    publisher: 'OpenAIRE',
  },
  {
    id: 3,
    type: 'software',
    title: 'BBOB benchmarking software package',
    authors: 'COCO Platform',
    publicationDate: '2024-06-15',
    publisher: 'COCO / BBOB',
  },
]