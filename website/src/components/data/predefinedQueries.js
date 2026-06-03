import { ENTITY_TYPES } from './entityTypes'

export const predefinedQueryOptions = [
  {
    label: 'What are the metaheuristics published after 2015?',
    value: 'metaheuristicsAfter2015'
  },
  {
    label: 'What are the Java libraries implementing pNSGA-II?',
    value: 'javaLibrariesPNsgaII'
  },
  {
    label: 'What are the metaheuristics tested in Knapsack problem?',
    value: 'metaheuristicsKnapsack'
  },
  {
    label: 'What are the metaheuristics tested in ZDT problem having Java library implementations?',
    value: 'metaheuristicsZdtJavaLibraries'
  },
  {
    label: 'Which order relations have been proposed to many-objective optimisation?',
    value: 'manyObjectiveOrderRelations'
  },
  {
    label: 'Who are the researchers working both in decomposition-based and indicator-based metaheuristics?',
    value: 'researchersDecompositionAndIndicatorBased'
  }
]

export const predefinedQueries = {
  metaheuristicsAfter2015: [
    {
      entitytype: ENTITY_TYPES.CLASS,
      entity: 'MetaHeuristic',
      inoutput: true,
      orderedby: false
    },
    {
      entitytype: ENTITY_TYPES.DATATYPE_PROPERTY,
      entity: 'hasDevelopingYear',
      inoutput: true,
      orderedby: true
    },
    {
      entitytype: ENTITY_TYPES.RELATIONAL_OPERATOR,
      entity: 'swrlb:greaterThanOrEqual',
      inoutput: false,
      orderedby: false
    },
    {
      entitytype: ENTITY_TYPES.LITERAL,
      entity: '2015',
      inoutput: false,
      orderedby: false
    }
  ],

  javaLibrariesPNsgaII: [
    {
      entitytype: ENTITY_TYPES.CLASS,
      entity: 'ImplementationLibrary',
      inoutput: true,
      orderedby: true
    },
    {
      entitytype: ENTITY_TYPES.OBJECT_PROPERTY,
      entity: 'hasImplementationLanguage',
      inoutput: false,
      orderedby: false
    },
    {
      entitytype: ENTITY_TYPES.INDIVIDUAL,
      entity: 'Java',
      inoutput: false,
      orderedby: false
    },
    {
      entitytype: ENTITY_TYPES.OBJECT_PROPERTY,
      entity: 'hasSearchAlgorithm',
      inoutput: false,
      orderedby: false
    },
    {
      entitytype: ENTITY_TYPES.INDIVIDUAL,
      entity: 'pNSGA-II',
      inoutput: false,
      orderedby: false
    }
  ],

  metaheuristicsKnapsack: [
    {
      entitytype: ENTITY_TYPES.CLASS,
      entity: 'MetaHeuristic',
      inoutput: true,
      orderedby: true
    },
    {
      entitytype: ENTITY_TYPES.OBJECT_PROPERTY,
      entity: 'canSolve',
      inoutput: false,
      orderedby: false
    },
    {
      entitytype: ENTITY_TYPES.CLASS,
      entity: 'Knapsack',
      inoutput: false,
      orderedby: false
    }
  ],
  metaheuristicsZdtJavaLibraries: [
  {
    entitytype: ENTITY_TYPES.CLASS,
    entity: 'MetaHeuristic',
    orderedby: true,
    inoutput: true
  },
  {
    entitytype: ENTITY_TYPES.OBJECT_PROPERTY,
    entity: 'canSolve',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.CLASS,
    entity: 'ZDT',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.CLASS,
    entity: 'MetaHeuristic',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.OBJECT_PROPERTY,
    entity: 'useLibrary',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.CLASS,
    entity: 'ImplementationLibrary',
    orderedby: true,
    inoutput: true
  },
  {
    entitytype: ENTITY_TYPES.OBJECT_PROPERTY,
    entity: 'hasImplementationLanguage',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.INDIVIDUAL,
    entity: 'Java',
    orderedby: false,
    inoutput: false
  }
],

manyObjectiveOrderRelations: [
  {
    entitytype: ENTITY_TYPES.CLASS,
    entity: 'MetaHeuristic',
    orderedby: false,
    inoutput: true
  },
  {
    entitytype: ENTITY_TYPES.DATATYPE_PROPERTY,
    entity: 'isManyObjectiveOptimization',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.LITERAL,
    entity: true,
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.CLASS,
    entity: 'MetaHeuristic',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.OBJECT_PROPERTY,
    entity: 'usesOrderRelation',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.CLASS,
    entity: 'OrderRelation',
    orderedby: true,
    inoutput: true
  }
],

researchersDecompositionAndIndicatorBased: [
  {
    entitytype: ENTITY_TYPES.CLASS,
    entity: 'Decomposition_based',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.OBJECT_PROPERTY,
    entity: 'hasAuthor',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.CLASS,
    entity: 'Researcher',
    orderedby: true,
    inoutput: true
  },
  {
    entitytype: ENTITY_TYPES.CLASS,
    entity: 'Indicator_based',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.OBJECT_PROPERTY,
    entity: 'hasAuthor',
    orderedby: false,
    inoutput: false
  },
  {
    entitytype: ENTITY_TYPES.CLASS,
    entity: 'Researcher',
    orderedby: false,
    inoutput: false
  }
]
}