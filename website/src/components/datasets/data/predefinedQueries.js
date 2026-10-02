import { defaultOutputFields } from './datasetQueryOptions.js'
import { defaultOplOutputFields } from './oplQueryOptions.js'

/**
 * Queries predefinidas para a pesquisa de datasets e problemas
 * de otimização.
 *
 * Cada query identifica a respetiva fonte:
 * - OPENAIRE: research products obtidos através da OpenAIRE;
 * - OPL: problems, suites and generators obtained from the official OPL catalogue;
 * - BBOB: fonte a implementar posteriormente.
 *
 * O frontend filtra esta lista de acordo com a fonte selecionada.
 */

const defaultOpenAireSelectedFields = [...defaultOutputFields]
const defaultOplSelectedFields = [...defaultOplOutputFields]

export const predefinedQueriesDatasets = [
  {
    id: 1,
    source: 'OPENAIRE',
    label:
      "What are the datasets published by author 'Carola Doerr'?",
    enabled: true,

    criteria: [
      {
        source: 'OPENAIRE',
        field: 'authorFullName',
        operator: 'INCLUDE',
        value: 'Carola Doerr',
      },
      {
        source: 'OPENAIRE',
        field: 'type',
        operator: 'INCLUDE',
        value: 'dataset',
      },
    ],

    sort: [],

    selectedFields: [
      ...defaultOpenAireSelectedFields,
    ],

    page: 1,
    pageSize: 20,
  },

  {
    id: 2,
    source: 'OPENAIRE',
    label:
      "What are the publications of author 'Carola Doerr' in 2025 and 2026 sorted by publication date in descendent order?",
    enabled: true,

    criteria: [
      {
        source: 'OPENAIRE',
        field: 'authorFullName',
        operator: 'INCLUDE',
        value: 'Carola Doerr',
      },
      {
        source: 'OPENAIRE',
        field: 'type',
        operator: 'INCLUDE',
        value: 'publication',
      },
      {
        source: 'OPENAIRE',
        field: 'fromPublicationDate',
        operator: 'INCLUDE',
        value: '2025-01-01',
      },
      {
        source: 'OPENAIRE',
        field: 'toPublicationDate',
        operator: 'INCLUDE',
        value: '2026-12-31',
      },
    ],

    sort: [
      {
        field: 'publicationDate',
        direction: 'DESC',
      },
    ],

    selectedFields: [
      ...defaultOpenAireSelectedFields,
    ],

    page: 1,
    pageSize: 20,
  },

  {
    id: 3,
    source: 'OPENAIRE',
    label:
      "What are the publications of author 'Carola Doerr' in 2024 and 2025 sorted by citation count in descendent order?",
    enabled: true,

    criteria: [
      {
        source: 'OPENAIRE',
        field: 'authorFullName',
        operator: 'INCLUDE',
        value: 'Carola Doerr',
      },
      {
        source: 'OPENAIRE',
        field: 'type',
        operator: 'INCLUDE',
        value: 'publication',
      },
      {
        source: 'OPENAIRE',
        field: 'fromPublicationDate',
        operator: 'INCLUDE',
        value: '2024-01-01',
      },
      {
        source: 'OPENAIRE',
        field: 'toPublicationDate',
        operator: 'INCLUDE',
        value: '2025-12-31',
      },
    ],

    sort: [
      {
        field: 'citationCount',
        direction: 'DESC',
      },
    ],

    selectedFields: [
      ...defaultOpenAireSelectedFields,
    ],

    page: 1,
    pageSize: 20,
  },

  {
    id: 4,
    source: 'OPENAIRE',
    label:
      "What are the software published by author 'Carola Doerr' sorted by publication date in descendent order?",
    enabled: true,

    criteria: [
      {
        source: 'OPENAIRE',
        field: 'authorFullName',
        operator: 'INCLUDE',
        value: 'Carola Doerr',
      },
      {
        source: 'OPENAIRE',
        field: 'type',
        operator: 'INCLUDE',
        value: 'software',
      },
    ],

    sort: [
      {
        field: 'publicationDate',
        direction: 'DESC',
      },
    ],

    selectedFields: [
      ...defaultOpenAireSelectedFields,
    ],

    page: 1,
    pageSize: 20,
  },

  {
    id: 5,
    source: 'OPENAIRE',
    label:
      "What are the datasets published by Sorbonne University since 2021 with keywords 'Benchmark' sorted by popularity in descendent order?",
    enabled: true,

    criteria: [
      {
        source: 'OPENAIRE',
        field: 'rorId',
        operator: 'INCLUDE',
        value: 'https://ror.org/02en5vm52',
      },
      {
        source: 'OPENAIRE',
        field: 'type',
        operator: 'INCLUDE',
        value: 'dataset',
      },
      {
        source: 'OPENAIRE',
        field: 'fromPublicationDate',
        operator: 'INCLUDE',
        value: '2021-01-01',
      },
      {
        source: 'OPENAIRE',
        field: 'subjects',
        operator: 'INCLUDE',
        value: 'Benchmark',
      },
    ],

    sort: [
      {
        field: 'popularity',
        direction: 'DESC',
      },
    ],

    selectedFields: [
      ...defaultOpenAireSelectedFields,
    ],

    page: 1,
    pageSize: 20,
  },

  {
    id: 6,
    source: 'OPENAIRE',
    label:
      "What are the datasets published in France in 2025 with keywords 'Benchmark' sorted by citation count in descendent order?",
    enabled: true,

    criteria: [
      {
        source: 'OPENAIRE',
        field: 'countryCode',
        operator: 'INCLUDE',
        value: 'FR',
      },
      {
        source: 'OPENAIRE',
        field: 'type',
        operator: 'INCLUDE',
        value: 'dataset',
      },
      {
        source: 'OPENAIRE',
        field: 'fromPublicationDate',
        operator: 'INCLUDE',
        value: '2025-01-01',
      },
      {
        source: 'OPENAIRE',
        field: 'toPublicationDate',
        operator: 'INCLUDE',
        value: '2025-12-31',
      },
      {
        source: 'OPENAIRE',
        field: 'subjects',
        operator: 'INCLUDE',
        value: 'Benchmark',
      },
    ],

    sort: [
      {
        field: 'citationCount',
        direction: 'DESC',
      },
    ],

    selectedFields: [
      ...defaultOpenAireSelectedFields,
    ],

    page: 1,
    pageSize: 20,
  },

  {
    id: 7,
    source: 'OPENAIRE',
    label:
      "What are the research products related to project 'Proj-123'?",
    enabled: true,

    criteria: [
      {
        source: 'OPENAIRE',
        field: 'relProjectCode',
        operator: 'INCLUDE',
        value: 'Proj-123',
      },
    ],

    sort: [
      {
        field: 'publicationDate',
        direction: 'DESC',
      },
    ],

    selectedFields: [
      ...defaultOpenAireSelectedFields,
    ],

    page: 1,
    pageSize: 20,
  },

  /**
   * Query existente no projeto Node-RED.
   *
   * Esta pesquisa combina dados OpenAIRE com dados OPL.
   * A pesquisa híbrida está ativa e combina critérios das duas fontes.
   */
{
  id: 8,
  source: 'HYBRID',
  label:
    "What are the datasets published by author 'Carola Doerr' with 'bbob' in the title and '1 objective'?",
  enabled: true,

  criteria: [
    {
      source: 'OPENAIRE',
      field: 'authorFullName',
      operator: 'INCLUDE',
      value: 'Carola Doerr',
    },
    {
      source: 'OPENAIRE',
      field: 'type',
      operator: 'INCLUDE',
      value: 'dataset',
    },
    {
      source: 'OPENAIRE',
      field: 'mainTitle',
      operator: 'INCLUDE',
      value: 'bbob',
    },
    {
      source: 'OPL',
      field: 'name',
      operator: 'INCLUDE',
      value: 'bbob',
    },
    {
      source: 'OPL',
      field: 'objectives',
      operator: 'INCLUDE',
      value: '1',
    },
  ],

  sort: [],

  selectedFields: [
    'source',
    'type',
    'title',
    'publicationDate',
    'authors',
    'publisher',
    'relatedMaterials',
    'name',
    'description',
    'objectives',
    'variableTypes',
    'modality',
    'referenceTitles',
    'links',
  ],

  page: 1,
  pageSize: 20,
},

  /**
   * Query OPL usada como exemplo da pesquisa direta no catálogo oficial OpenOptimizationOrg/OPL.
   */
  {
    id: 'opl-bbob-single-objective',
    source: 'OPL',
    label: "What are the single-objective BBOB optimisation problems?",
    enabled: true,

    criteria: [
      {
        source: 'OPL',
        field: 'name',
        operator: 'INCLUDE',
        value: 'bbob',
      },
      {
        source: 'OPL',
        field: 'objectives',
        operator: 'INCLUDE',
        value: '1',
      },
    ],

    sort: [
      {
        field: 'name',
        direction: 'ASC',
      },
    ],

    selectedFields: [
      ...defaultOplSelectedFields,
    ],

    page: 1,
    pageSize: 20,
  },

  /**
   * Query BBOB/COCO existente no projeto Node-RED.
   *
   * Continua desativada até a fonte BBOB/COCO ser implementada.
   */
  {
    id: 9,
    source: 'BBOB',
    label:
      'What are the BBOB-COCO datasets for the noisy CMA-ESPLUSSEL and mixint CMA-ESwM benchmarkings?',
    enabled: false,
    implementationStatus: 'BBOB_NOT_IMPLEMENTED',

    criteria: [
      {
        source: 'BBOB',
        field: 'suite',
        operator: 'INCLUDE',
        value: 'noisy',
      },
      {
        source: 'BBOB',
        field: 'algorithm',
        operator: 'INCLUDE',
        value: 'CMA-ESPLUSSEL',
      },
      {
        source: 'BBOB',
        field: 'suite',
        operator: 'INCLUDE',
        value: 'mixint',
      },
      {
        source: 'BBOB',
        field: 'algorithm',
        operator: 'INCLUDE',
        value: 'CMA-ESwM',
      },
    ],

    sort: [],

    selectedFields: [],

    page: 1,
    pageSize: 20,
  },


  {
  id: 10,
  source: 'OPENAIRE',

  label:
    "What are the publications of author 'Carola Doerr' from '2000' in 'ACM Transactions on Evolutionary Learning and Optimization' Journal?",

  enabled: true,

  criteria: [
    {
      source: 'OPENAIRE',
      field: 'authorFullName',
      operator: 'INCLUDE',
      value: 'Carola Doerr',
    },
    {
      source: 'OPENAIRE',
      field: 'type',
      operator: 'INCLUDE',
      value: 'publication',
    },
    {
      source: 'OPENAIRE',
      field: 'fromPublicationDate',
      operator: 'INCLUDE',
      value: '2000-01-01',
    },
    {
      source: 'OPENAIRE',
      field: 'relHostingDataSourceId',
      operator: 'INCLUDE',
      value:
        'ACM Transactions on Evolutionary Learning and Optimization',
    },
  ],

  sort: [
    {
      field: 'publicationDate',
      direction: 'DESC',
    },
  ],

  selectedFields: [
    ...defaultOpenAireSelectedFields,
  ],

  page: 1,
  pageSize: 20,
},
]