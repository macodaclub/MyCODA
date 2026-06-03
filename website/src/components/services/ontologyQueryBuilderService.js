import { ENTITY_TYPES } from '../data/entityTypes'
import { normalizeEntityType } from './ontologyEntityUtils'

export function buildOntologyQuery(queryRows) {
  let finalQuery = ''
  let finalQueryOutput = ''
  let finalQueryOrdering = ''

  let subject = ''
  let object = ''

  const rows = queryRows ?? []

  for (let i = 0; i < rows.length; i++) {
    const currentRow = rows[i]
    const nextRow = rows[i + 1]

    if (
      !currentRow.entitytype ||
      currentRow.entity === null ||
      currentRow.entity === undefined ||
      currentRow.entity === ''
    ) {
      continue
    }

    const currentType = normalizeEntityType(currentRow.entitytype)
    const nextType = normalizeEntityType(nextRow?.entitytype)

    switch (currentType) {
      case ENTITY_TYPES.CLASS:
        subject = `_${currentRow.entity}_`

        if (!queryAlreadyContainsClass(finalQuery, currentRow.entity)) {
          finalQuery = appendQueryPart(
            finalQuery,
            `${currentRow.entity}(?${subject})`
          )
        }

        if (currentRow.inoutput) {
          finalQueryOutput = appendQueryPart(
            finalQueryOutput,
            `sqwrl:select(?${subject})`
          )
        }

        if (currentRow.orderedby) {
          finalQueryOrdering = appendQueryPart(
            finalQueryOrdering,
            `sqwrl:orderBy(?${subject})`
          )
        }

        break

      case ENTITY_TYPES.DATATYPE_PROPERTY:
        if (!nextRow) break

        object = `_${currentRow.entity}_`

        if (nextType === ENTITY_TYPES.RELATIONAL_OPERATOR) {
          finalQuery = appendQueryPart(
            finalQuery,
            `${currentRow.entity}(?${subject} , ?${object})`
          )

          if (currentRow.inoutput) {
            finalQueryOutput = appendQueryPart(
              finalQueryOutput,
              `sqwrl:select(?${object})`
            )
          }

          if (currentRow.orderedby) {
            finalQueryOrdering = appendQueryPart(
              finalQueryOrdering,
              `sqwrl:orderBy(?${object})`
            )
          }
        }

        if (nextType === ENTITY_TYPES.LITERAL) {
          finalQuery = appendQueryPart(
            finalQuery,
            `${currentRow.entity}(?${subject} , ${nextRow.entity})`
          )

          if (currentRow.inoutput) {
            finalQueryOutput = appendQueryPart(
              finalQueryOutput,
              `sqwrl:select(?${subject})`
            )
          }

          if (currentRow.orderedby) {
            finalQueryOrdering = appendQueryPart(
              finalQueryOrdering,
              `sqwrl:orderBy(?${subject})`
            )
          }

          i++
        }

        break

      case ENTITY_TYPES.OBJECT_PROPERTY:
        if (!nextRow) break

        if (nextType === ENTITY_TYPES.CLASS) {
          object = `_${nextRow.entity}_`

          finalQuery = appendQueryPart(
            finalQuery,
            `${currentRow.entity}(?${subject} , ?${object})`
          )

          if (currentRow.inoutput) {
            finalQueryOutput = appendQueryPart(
              finalQueryOutput,
              `sqwrl:select(?${object})`
            )
          }

          if (currentRow.orderedby) {
            finalQueryOrdering = appendQueryPart(
              finalQueryOrdering,
              `sqwrl:orderBy(?${object})`
            )
          }
        }

        if (nextType === ENTITY_TYPES.INDIVIDUAL) {
          finalQuery = appendQueryPart(
            finalQuery,
            `${currentRow.entity}(?${subject} , ${nextRow.entity})`
          )

          if (currentRow.inoutput) {
            finalQueryOutput = appendQueryPart(
              finalQueryOutput,
              `sqwrl:select(?${subject})`
            )
          }

          if (currentRow.orderedby) {
            finalQueryOrdering = appendQueryPart(
              finalQueryOrdering,
              `sqwrl:orderBy(?${subject})`
            )
          }

          i++
        }

        break

      case ENTITY_TYPES.RELATIONAL_OPERATOR:
        if (nextType === ENTITY_TYPES.LITERAL) {
          finalQuery = appendQueryPart(
            finalQuery,
            `${currentRow.entity}(?${object} , ${nextRow.entity})`
          )

          i++
        }

        break

      default:
        break
    }
  }

  if (!finalQuery || !finalQueryOutput) {
    return {
      query: '',
      error: 'The query is incomplete. Add at least one Class and mark one field as In Output.'
    }
  }

  return {
    query:
      finalQuery +
      ' -> ' +
      finalQueryOutput +
      (finalQueryOrdering ? ' ^ ' + finalQueryOrdering : ''),
    error: null
  }
}

function appendQueryPart(currentQuery, newPart) {
  return currentQuery ? `${currentQuery} ^ ${newPart}` : newPart
}

function queryAlreadyContainsClass(query, className) {
  return query.startsWith(`${className}(`) || query.includes(` ${className}(`)
}