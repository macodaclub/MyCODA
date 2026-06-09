import { ENTITY_TYPES } from './entityTypes'

function row(entitytype, entity, options = {}) {
  return {
    entitytype,
    entity,
    inoutput: options.inoutput ?? false,
    orderedby: options.orderedby ?? false
  }
}

export const predefinedQueryOptions = [
  {
    label: 'Show all individuals',
    value: 'allIndividuals'
  },
  {
    label: 'Show all metaheuristics',
    value: 'allMetaheuristics'
  },
  {
    label: 'Show metaheuristics with DOI',
    value: 'metaheuristicsWithDoi'
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
  allIndividuals: [
    row(ENTITY_TYPES.CLASS, 'owl:Thing', {
      inoutput: true,
      orderedby: false
    })
  ],

  allMetaheuristics: [
    row(ENTITY_TYPES.CLASS, 'Meta Heuristic', {
      inoutput: true,
      orderedby: true
    })
  ],

  metaheuristicsWithDoi: [
    row(ENTITY_TYPES.CLASS, 'Meta Heuristic', {
      inoutput: true,
      orderedby: false
    }),
    row(ENTITY_TYPES.DATATYPE_PROPERTY, 'has doi', {
      inoutput: true,
      orderedby: true
    })
  ],

  javaLibrariesPNsgaII: [
    row(ENTITY_TYPES.CLASS, 'Implementation Library', {
      inoutput: true,
      orderedby: true
    }),
    row(ENTITY_TYPES.OBJECT_PROPERTY, 'has implementation language'),
    row(ENTITY_TYPES.INDIVIDUAL, 'Java'),
    row(ENTITY_TYPES.OBJECT_PROPERTY, 'has search algorithm'),
    row(ENTITY_TYPES.INDIVIDUAL, 'pNSGA-II')
  ],

  metaheuristicsKnapsack: [
    row(ENTITY_TYPES.CLASS, 'Meta Heuristic', {
      inoutput: true,
      orderedby: true
    }),
    row(ENTITY_TYPES.OBJECT_PROPERTY, 'can solve'),
    row(ENTITY_TYPES.CLASS, 'Knapsack')
  ],

  metaheuristicsZdtJavaLibraries: [
    row(ENTITY_TYPES.CLASS, 'Meta Heuristic', {
      inoutput: true,
      orderedby: true
    }),
    row(ENTITY_TYPES.OBJECT_PROPERTY, 'can solve'),
    row(ENTITY_TYPES.CLASS, 'ZDT'),
    row(ENTITY_TYPES.OBJECT_PROPERTY, 'use library'),
    row(ENTITY_TYPES.CLASS, 'Implementation Library', {
      inoutput: true,
      orderedby: true
    }),
    row(ENTITY_TYPES.OBJECT_PROPERTY, 'has implementation language'),
    row(ENTITY_TYPES.INDIVIDUAL, 'Java')
  ],

  manyObjectiveOrderRelations: [
    row(ENTITY_TYPES.CLASS, 'Meta Heuristic', {
      inoutput: true
    }),
    row(ENTITY_TYPES.DATATYPE_PROPERTY, 'is many objective optimization'),
    row(ENTITY_TYPES.RELATIONAL_OPERATOR, 'swrlb:equal'),
    row(ENTITY_TYPES.LITERAL, true),
    row(ENTITY_TYPES.OBJECT_PROPERTY, 'uses order relation'),
    row(ENTITY_TYPES.CLASS, 'Order Relation', {
      inoutput: true,
      orderedby: true
    })
  ],

  researchersDecompositionAndIndicatorBased: [
    row(ENTITY_TYPES.CLASS, 'Decomposition based'),
    row(ENTITY_TYPES.OBJECT_PROPERTY, 'has author'),
    row(ENTITY_TYPES.CLASS, 'Researcher', {
      inoutput: true,
      orderedby: true
    }),
    row(ENTITY_TYPES.CLASS, 'Indicator based'),
    row(ENTITY_TYPES.OBJECT_PROPERTY, 'has author'),
    row(ENTITY_TYPES.CLASS, 'Researcher')
  ]
}