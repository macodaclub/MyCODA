/**
 * Configuração OpenAIRE usada pelo query builder de datasets.
 */

export const filteringCriteria = [
  { label: 'Author full name', value: 'authorFullName' },
  { label: 'Author ORCID', value: 'authorOrcid' },
  { label: 'Type', value: 'type' },
  { label: 'Main title', value: 'mainTitle' },
  { label: 'Persistent identifier', value: 'pid' },
  { label: 'Subjects / keywords', value: 'subjects' },
  { label: 'Country code', value: 'countryCode' },
  { label: 'Affiliated organization — ROR ID', value: 'rorId' },
  { label: 'Publisher', value: 'publisher' },
  { label: 'Project code', value: 'relProjectCode' },
  { label: 'From publication date', value: 'fromPublicationDate' },
  { label: 'To publication date', value: 'toPublicationDate' },
  { label: 'Instance type', value: 'instanceType' },
  { label: 'SDG', value: 'sdg' },
  { label: 'Fields of Science', value: 'fos' },
  { label: 'Influence class', value: 'influenceClass' },
  { label: 'Popularity class', value: 'popularityClass' },
  { label: 'Impulse class', value: 'impulseClass' },
  { label: 'Citation count class', value: 'citationCountClass' },
  { label: 'Related organization — ROR / OpenAIRE ID', value: 'relOrganizationId' },
  { label: 'Journal / hosting data source', value: 'relHostingDataSourceId' },
];

export const includeExcludeOptions = [
  { label: 'Include', value: 'INCLUDE' },
  { label: 'Exclude', value: 'EXCLUDE' },
];

export const includeOnlyOptions = [{ label: 'Include', value: 'INCLUDE' }];

/**
 * Ordenação nativa da API OpenAIRE (pesquisa OPENAIRE simples).
 */
export const sortFields = [
  { label: 'No sorting', value: '' },
  { label: 'Relevance', value: 'relevance' },
  { label: 'Publication date', value: 'publicationDate' },
  { label: 'Date of collection', value: 'dateOfCollection' },
  { label: 'Influence', value: 'influence' },
  { label: 'Popularity', value: 'popularity' },
  { label: 'Citation count', value: 'citationCount' },
  { label: 'Impulse', value: 'impulse' },
];

/**
 * No HYBRID a ordenação é aplicada depois do join OpenAIRE + OPL.
 * Por isso apenas expomos campos que existem no HybridDatasetResult.
 */
export const hybridOpenAireSortFields = [
  { label: 'OpenAIRE — Identifier', value: 'openAireId' },
  { label: 'OpenAIRE — Type', value: 'type' },
  { label: 'OpenAIRE — Title', value: 'title' },
  { label: 'OpenAIRE — Authors', value: 'authors' },
  { label: 'OpenAIRE — Publication date', value: 'publicationDate' },
  { label: 'OpenAIRE — Publisher', value: 'publisher' },
  { label: 'OpenAIRE — Citations', value: 'citations' },
  { label: 'OpenAIRE — Related materials', value: 'relatedMaterials' },
];

export const sortOrders = [
  { label: 'None', value: '' },
  { label: 'Ascending', value: 'ASC' },
  { label: 'Descending', value: 'DESC' },
];

export const dateCriteria = ['fromPublicationDate', 'toPublicationDate'];

export const openAireExcludeFields = ['mainTitle', 'subjects'];

const citationClassOptions = [
  { label: 'C1 - Top 0.01%', value: 'C1' },
  { label: 'C2 - Top 0.1%', value: 'C2' },
  { label: 'C3 - Top 1%', value: 'C3' },
  { label: 'C4 - Top 10%', value: 'C4' },
  { label: 'C5 - Average', value: 'C5' },
];

export const predefinedValueOptions = {
  type: [
    { label: 'Publication', value: 'publication' },
    { label: 'Dataset', value: 'dataset' },
    { label: 'Software', value: 'software' },
    { label: 'Other', value: 'other' },
  ],
  influenceClass: citationClassOptions,
  popularityClass: citationClassOptions,
  impulseClass: citationClassOptions,
  citationCountClass: citationClassOptions,
};

export const outputFieldOptions = [
  {
    label: 'Type',
    value: 'type',
  },
  {
    label: 'Instance type',
    value: 'instanceType',
  },
  {
    label: 'Title',
    value: 'title',
  },
  { label: 'Abstract', value: 'abstract' },
  {
    label: 'Subjects / keywords',
    value: 'subjects',
  },
  {
    label: 'Publication date',
    value: 'publicationDate',
  },
  {
    label: 'Authors',
    value: 'authors',
  },
  {
    label: 'Publisher',
    value: 'publisher',
  },
  {
    label: 'Scientific event / container',
    value: 'scientificEvent',
  },
  {
    label: 'Citations',
    value: 'citations',
  },
  {
    label: 'Influence class',
    value: 'influenceClass',
  },
  {
    label: 'Popularity class',
    value: 'popularityClass',
  },
  {
    label: 'Impulse class',
    value: 'impulseClass',
  },
  {
    label: 'Citation count class',
    value: 'citationCountClass',
  },
  {
    label: 'Related materials',
    value: 'relatedMaterials',
  },
  {
    label: 'Country code',
    value: 'countryCode',
  },
  {
    label: 'Sustainable Development Goals',
    value: 'sdg',
  },
  {
    label: 'Fields of Science',
    value: 'fos',
  },
];

export const hybridOpenAireOutputFieldOptions = outputFieldOptions.map(
  (option) => ({
    label: `OpenAIRE — ${option.label}`,
    value: option.value,
  })
);

export const defaultOutputFields = [
  'type',
  'title',
  'publicationDate',
  'authors',
  'publisher',
  'citations',
  'relatedMaterials',
]

export function supportsOpenAireExclude(field) {
  return openAireExcludeFields.includes(field);
}

export function isOpenAireDateCriterion(field) {
  return dateCriteria.includes(field);
}

export function hasOpenAirePredefinedValueOptions(field) {
  return Object.prototype.hasOwnProperty.call(predefinedValueOptions, field);
}

export function getOpenAirePredefinedValueOptions(field) {
  return predefinedValueOptions[field] ?? [];
}

/** Vocabulários servidos pelo backend para critérios OpenAIRE. */
export const openAireVocabularyByField = {
  countryCode: 'countries',
  sdg: 'sdgs',
  fos: 'fields-of-science',
  instanceType: 'instance-types',
};

export function getOpenAireVocabularyName(field) {
  return openAireVocabularyByField[field] ?? null;
}

export function isOpenAireVocabularyCriterion(field) {
  return Boolean(getOpenAireVocabularyName(field));
}

export function isOpenAireOrcidCriterion(field) {
  return field === 'authorOrcid';
}

export function isOpenAireRorCriterion(field) {
  return field === 'rorId' || field === 'relOrganizationId';
}

export function getOpenAireIdentifierSearchUrl(field) {
  if (field === 'authorOrcid') {
    return 'https://orcid.org/orcid-search/search';
  }

  if (field === 'rorId' || field === 'relOrganizationId') {
    return 'https://ror.org/search';
  }

  return null;
}
