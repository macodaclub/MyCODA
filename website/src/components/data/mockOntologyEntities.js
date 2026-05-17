import { ENTITY_TYPES } from './entityTypes'

export const mockOntologyEntities = [
  {
    type: ENTITY_TYPES.CLASS,
    name: 'MetaHeuristic',
    details: 'ascendentClassesHierarchy: [Algorithm]',
    comment: 'A metaheuristic optimization method.'
  },
  {
    type: ENTITY_TYPES.INDIVIDUAL,
    name: 'NSGA-II',
    details: 'ascendentClassesHierarchy: [MetaHeuristic]',
    comment: 'Non-dominated Sorting Genetic Algorithm II.'
  },
  {
    type: ENTITY_TYPES.INDIVIDUAL,
    name: 'NSGA-III',
    details: 'ascendentClassesHierarchy: [MetaHeuristic]',
    comment: 'Many-objective evolutionary algorithm.'
  },
  {
    type: ENTITY_TYPES.DATATYPE_PROPERTY,
    name: 'hasDevelopingYear',
    details: 'classDomain: [MetaHeuristic], dataTypeRange: [integer]',
    comment: 'Year when the method was developed.'
  },
  {
    type: ENTITY_TYPES.OBJECT_PROPERTY,
    name: 'canSolve',
    details: 'classDomain: [MetaHeuristic], classRange: [Problem]',
    comment: 'Relates an algorithm with a problem it can solve.'
  }
]