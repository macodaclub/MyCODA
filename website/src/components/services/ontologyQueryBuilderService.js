import { ENTITY_TYPES } from '../data/entityTypes'
import { normalizeEntityType } from './ontologyEntityUtils'

function normalizeOperator(operator) {
  const value = String(operator ?? '').trim()

  if (!value) {
    return ''
  }

  if (value.startsWith('swrlb:')) {
    return value
  }

  return `swrlb:${value}`
}

function formatLiteralValue(value) {
  if (typeof value === 'boolean') {
    return value
  }

  const text = String(value ?? '').trim()

  if (text === 'true' || text === 'false') {
    return text
  }

  if (text !== '' && !Number.isNaN(Number(text))) {
    return text
  }

  return `"${text}"`
}

export function buildOntologyQuery(rows) {
  const atoms = []
  const outputAtoms = []
  const orderByAtoms = []

  let currentSubject = null
  let pendingObjectProperty = null
  let pendingOperator = null
  let lastDataVariable = null

  let classCounter = 1
  let valueCounter = 1

  for (let index = 0; index < rows.length; index += 1) {
    const row = rows[index]
    const type = normalizeEntityType(row.entitytype)
    const entity = row.entity

    if (!type || entity === null || entity === undefined || entity === '') {
      return {
        query: '',
        error: `Row ${index + 1} is incomplete.`
      }
    }

    if (type === ENTITY_TYPES.CLASS) {
      const variable = `?x${classCounter}`
      classCounter += 1

      if (pendingObjectProperty && currentSubject) {
        atoms.push(`${pendingObjectProperty}(${currentSubject}, ${variable})`)
        pendingObjectProperty = null
      }

      atoms.push(`${entity}(${variable})`)
      currentSubject = variable

      if (row.inoutput) {
        outputAtoms.push(`sqwrl:select(${variable})`)
      }

      if (row.orderedby) {
        orderByAtoms.push(`sqwrl:orderBy(${variable})`)
      }

      continue
    }

    if (type === ENTITY_TYPES.OBJECT_PROPERTY) {
      if (!currentSubject) {
        return {
          query: '',
          error: `Row ${index + 1} has an object property without a previous class.`
        }
      }

      pendingObjectProperty = entity
      continue
    }

    if (type === ENTITY_TYPES.INDIVIDUAL) {
      if (!pendingObjectProperty || !currentSubject) {
        return {
          query: '',
          error: `Row ${index + 1} has an individual without a previous object property.`
        }
      }

      atoms.push(`${pendingObjectProperty}(${currentSubject}, ${entity})`)
      pendingObjectProperty = null

      if (row.inoutput) {
        outputAtoms.push(`sqwrl:select(${entity})`)
      }

      if (row.orderedby) {
        orderByAtoms.push(`sqwrl:orderBy(${entity})`)
      }

      continue
    }

    if (type === ENTITY_TYPES.DATATYPE_PROPERTY) {
      if (!currentSubject) {
        return {
          query: '',
          error: `Row ${index + 1} has a data property without a previous class.`
        }
      }

      const variable = `?v${valueCounter}`
      valueCounter += 1

      atoms.push(`${entity}(${currentSubject}, ${variable})`)
      lastDataVariable = variable

      if (row.inoutput) {
        outputAtoms.push(`sqwrl:select(${variable})`)
      }

      if (row.orderedby) {
        orderByAtoms.push(`sqwrl:orderBy(${variable})`)
      }

      continue
    }

    if (type === ENTITY_TYPES.RELATIONAL_OPERATOR) {
      if (!lastDataVariable) {
        return {
          query: '',
          error: `Row ${index + 1} has an operator without a previous data property.`
        }
      }

      pendingOperator = normalizeOperator(entity)
      continue
    }

    if (type === ENTITY_TYPES.LITERAL) {
      if (!pendingOperator || !lastDataVariable) {
        return {
          query: '',
          error: `Row ${index + 1} has a literal without a previous operator.`
        }
      }

      atoms.push(`${pendingOperator}(${lastDataVariable}, ${formatLiteralValue(entity)})`)
      pendingOperator = null
      continue
    }

    return {
      query: '',
      error: `Unsupported entity type in row ${index + 1}.`
    }
  }

  if (pendingObjectProperty) {
    return {
      query: '',
      error: 'There is an object property without a target class or individual.'
    }
  }

  if (pendingOperator) {
    return {
      query: '',
      error: 'There is a relational operator without a literal value.'
    }
  }

  if (!atoms.length) {
    return {
      query: '',
      error: 'No query atoms were generated.'
    }
  }

  const selectAtoms = outputAtoms.length
    ? outputAtoms
    : [`sqwrl:select(${currentSubject})`]

  return {
    query: `${atoms.join(' ^ ')} -> ${[...selectAtoms, ...orderByAtoms].join(' ^ ')}`,
    error: null
  }
}