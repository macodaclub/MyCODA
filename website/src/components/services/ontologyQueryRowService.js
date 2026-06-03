export function createEmptyQueryRow() {
  return {
    id: crypto.randomUUID(),
    entitytype: null,
    entity: null,
    inoutput: false,
    orderedby: false
  }
}

export function createQueryRowFromPredefined(row) {
  return {
    id: crypto.randomUUID(),
    entitytype: row.entitytype ?? null,
    entity: row.entity ?? null,
    inoutput: row.inoutput ?? false,
    orderedby: row.orderedby ?? false
  }
}