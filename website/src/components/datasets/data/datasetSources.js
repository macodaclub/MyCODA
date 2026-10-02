export const DATASET_SOURCES = Object.freeze({
  OPENAIRE: 'OPENAIRE',
  OPL: 'OPL',
  HYBRID: 'HYBRID',
})

export const datasetSourceOptions = [
  {
    label: 'OpenAIRE',
    value: DATASET_SOURCES.OPENAIRE,
  },
  {
    label: 'Optimisation Problem Library',
    value: DATASET_SOURCES.OPL,
  },
  {
    label: 'OpenAIRE + OPL',
    value: DATASET_SOURCES.HYBRID,
  },
]

/**
 * Valor guardado pelo Select de critérios.
 *
 * Em pesquisas simples o field é suficiente. Em HYBRID é necessário
 * preservar também a fonte para evitar colisões entre campos com o mesmo
 * nome (por exemplo type e authors).
 */
export function getCriterionSelection(source, field, querySource) {
  if (!field) {
    return ''
  }

  return querySource === DATASET_SOURCES.HYBRID
    ? `${source}:${field}`
    : field
}
