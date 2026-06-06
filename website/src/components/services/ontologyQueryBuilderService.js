import { ENTITY_TYPES } from '../data/entityTypes'
import { normalizeEntityType } from './ontologyEntityUtils'

export function buildOntologyQuery(queryRows) {
  const atoms = []
  const selectedVariables = []
  const orderedVariables = []

  let lastSubjectVariable = null

  for (let i = 0; i < queryRows.length; i++) {
    const currentRow = queryRows[i]
    const currentType = normalizeEntityType(currentRow.entitytype)

    if (!currentRow.entity) {
      continue
    }

    if (currentType === ENTITY_TYPES.CLASS) {
      const classVariable = createVariableName(currentRow.entity)

      atoms.push(`${currentRow.entity}(${classVariable})`)
      lastSubjectVariable = classVariable

      if (currentRow.inoutput) {
        addUnique(selectedVariables, classVariable)
      }

      if (currentRow.orderedby) {
        addUnique(orderedVariables, classVariable)
      }

      continue
    }

    if (currentType === ENTITY_TYPES.OBJECT_PROPERTY) {
      const nextRow = queryRows[i + 1]
      const nextType = normalizeEntityType(nextRow?.entitytype)

      const propertyVariable = createVariableName(currentRow.entity)

      if (!lastSubjectVariable) {
        return {
          query: '',
          error: `Object Property ${currentRow.entity} needs a previous Class.`
        }
      }

      if (!nextRow) {
        atoms.push(
          `${currentRow.entity}(${lastSubjectVariable}, ${propertyVariable})`
        )

        if (currentRow.inoutput) {
          addUnique(selectedVariables, propertyVariable)
        }

        if (currentRow.orderedby) {
          addUnique(orderedVariables, propertyVariable)
        }

        continue
      }

      if (nextType === ENTITY_TYPES.INDIVIDUAL) {
        atoms.push(
          `${currentRow.entity}(${lastSubjectVariable}, ${nextRow.entity})`
        )

        if (currentRow.inoutput) {
          addUnique(selectedVariables, propertyVariable)
        }

        continue
      }

      if (nextType === ENTITY_TYPES.CLASS) {
        const objectVariable = createVariableName(nextRow.entity)

        atoms.push(
          `${currentRow.entity}(${lastSubjectVariable}, ${objectVariable})`
        )

        if (currentRow.inoutput) {
          addUnique(selectedVariables, objectVariable)
        }

        continue
      }
    }

    if (currentType === ENTITY_TYPES.DATATYPE_PROPERTY) {
      const propertyVariable = createVariableName(currentRow.entity)

      if (!lastSubjectVariable) {
        return {
          query: '',
          error: `Data Property ${currentRow.entity} needs a previous Class.`
        }
      }

      atoms.push(
        `${currentRow.entity}(${lastSubjectVariable}, ${propertyVariable})`
      )

      if (currentRow.inoutput) {
        addUnique(selectedVariables, propertyVariable)
      }

      if (currentRow.orderedby) {
        addUnique(orderedVariables, propertyVariable)
      }

      continue
    }

    if (currentType === ENTITY_TYPES.RELATIONAL_OPERATOR) {
      const previousRow = queryRows[i - 1]
      const nextRow = queryRows[i + 1]

      if (!previousRow || !nextRow) {
        continue
      }

      const previousVariable = createVariableName(previousRow.entity)

      atoms.push(
        `${currentRow.entity}(${previousVariable}, ${nextRow.entity})`
      )

      continue
    }
  }

  if (!atoms.length) {
    return {
      query: '',
      error: 'No valid query atoms were generated.'
    }
  }

  if (!selectedVariables.length) {
    return {
      query: '',
      error: 'Select at least one field as In Output.'
    }
  }

  const selectAtoms = selectedVariables.map(variable =>
    `sqwrl:select(${variable})`
  )

  const orderAtoms = orderedVariables.map(variable =>
    `sqwrl:orderBy(${variable})`
  )

  const query = [
    atoms.join(' ^ '),
    '->',
    [...selectAtoms, ...orderAtoms].join(' ^ ')
  ].join(' ')

  return {
    query,
    error: null
  }
}

function createVariableName(entityName) {
  const safeName = String(entityName)
    .replace(/[^A-Za-z0-9_]/g, '_')
    .replace(/_+/g, '_')

  return `?_${safeName}_`
}

function addUnique(array, value) {
  if (!array.includes(value)) {
    array.push(value)
  }
}