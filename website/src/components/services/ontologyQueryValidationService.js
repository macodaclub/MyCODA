import { ENTITY_TYPES } from '../data/entityTypes';
import { normalizeEntityType } from './ontologyEntityUtils';

export function validateQueryRows(queryRows) {
  if (!queryRows.length) {
    return {
      message: 'Add at least one query row.',
      rowIndex: null,
    };
  }

  const incompleteRowIndex = queryRows.findIndex((row) => {
    return (
      !row.entitytype ||
      row.entity === null ||
      row.entity === undefined ||
      row.entity === ''
    );
  });

  if (incompleteRowIndex !== -1) {
    return {
      message: `Row ${incompleteRowIndex + 1} is incomplete. Select an Entity Type and an Entity.`,
      rowIndex: incompleteRowIndex,
    };
  }

  const outputRowIndex = queryRows.findIndex((row) => row.inoutput);

  if (outputRowIndex === -1) {
    return {
      message: 'Select at least one field as In Output.',
      rowIndex: null,
    };
  }

  return null;
}

export function validateQuerySequence(queryRows) {
  const firstType = normalizeEntityType(queryRows[0]?.entitytype);

  if (firstType !== ENTITY_TYPES.CLASS) {
    return {
      message: 'The first row must be a Class.',
      rowIndex: 0,
    };
  }
  for (let i = 0; i < queryRows.length; i++) {
    const currentType = normalizeEntityType(queryRows[i].entitytype);
    const nextType = normalizeEntityType(queryRows[i + 1]?.entitytype);

if (currentType === ENTITY_TYPES.OBJECT_PROPERTY) {
  const hasNextRow = Boolean(queryRows[i + 1])

  const isValidNextType =
    nextType === ENTITY_TYPES.CLASS ||
    nextType === ENTITY_TYPES.INDIVIDUAL

  const canBeLastOutputProperty =
    !hasNextRow && queryRows[i].inoutput

  if (!isValidNextType && !canBeLastOutputProperty) {
    return {
      message: `Row ${i + 1} is an Object Property. The next row must be a Class or an Individual, or the Object Property must be marked as In Output.`,
      rowIndex: i
    }
  }
}
if (currentType === ENTITY_TYPES.DATATYPE_PROPERTY) {
  const hasNextRow = Boolean(queryRows[i + 1])

  const isValidNextType =
    nextType === ENTITY_TYPES.RELATIONAL_OPERATOR ||
    nextType === ENTITY_TYPES.LITERAL

  const canBeLastOutputProperty =
    !hasNextRow && queryRows[i].inoutput

  if (!isValidNextType && !canBeLastOutputProperty) {
    return {
      message: `Row ${i + 1} is a Data Property. The next row must be a Relational Operator, a Literal, or the Data Property must be marked as In Output.`,
      rowIndex: i
    }
  }
}

    if (currentType === ENTITY_TYPES.RELATIONAL_OPERATOR) {
      if (nextType !== ENTITY_TYPES.LITERAL) {
        return {
          message: `Row ${i + 1} is a Relational Operator. The next row must be a Literal.`,
          rowIndex: i,
        };
      }
    }
  }

  return null;
}

export function validateQuery(queryRows) {
  return validateQueryRows(queryRows) || validateQuerySequence(queryRows);
}
