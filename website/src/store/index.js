import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import {
  buildTaxonomyOfClass,
  evaluateXPathQuery,
  populateHashMapEntityType,
} from '@/utils/owlparser/utils.js';
import { nonNulls } from '@/utils/utils/utils.js';

// You can name the return value of `defineStore()` anything you want,
// but it's best to use the name of the store and surround it with `use`
// and `Store` (e.g. `useUserStore`, `useCartStore`, `useProductStore`)
// the first argument is a unique id of the store across your application
export const useOntologyStore = defineStore('ontology', () => {
  const backendHost = import.meta.env.DEV
    ? `http://localhost:${import.meta.env.BACKEND_PORT || '8080'}`
    : location.origin;

  const entities = ref([]);
  const taxonomy = ref([]);
  const taxonomyTree = ref([]);

  const entityTypes = computed(() =>
    entities.value
      .map((it) => it.type)
      .filter((value, index, self) => self.indexOf(value) === index)
  );

  async function fetchEntities() {
    const response = await fetch(`${backendHost}/static/MaCODA.owl`);
    if (!response.ok)
      return console.error('Failed to fetch ontology', response);
    const text = await response.text();
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(text, 'text/xml');
    const strQuery =
      '/rdf:RDF/owl:Class | /rdf:RDF/owl:ObjectProperty | /rdf:RDF/owl:DatatypeProperty | /rdf:RDF/owl:NamedIndividual';
    const thisIterator = evaluateXPathQuery(strQuery, xmlDoc, true);
    let thisResult;
    const hashmapEntityType = new Map();
    const hashmapTaxonomy = new Map();
    while ((thisResult = thisIterator.iterateNext())) {
      const strEntityId = evaluateXPathQuery('@rdf:about', thisResult, false);
      const strEntityLabel = strEntityId.substring(
        strEntityId.indexOf('#') + 1
      );

      hashmapEntityType.set(
        strEntityLabel,
        populateHashMapEntityType(thisResult, xmlDoc)
      );

      if (thisResult.nodeName === 'owl:Class') {
        // If it is a first level class (has no subclasses), add to the hashmapTaxonomy, including its subclasses tree (recursively)
        if (
          !evaluateXPathQuery(
            'rdfs:subClassOf/@rdf:resource',
            thisResult,
            false
          )
        ) {
          hashmapTaxonomy.set(
            strEntityLabel,
            buildTaxonomyOfClass(thisResult, xmlDoc)
          );
        }
      }
    }

    //var tableEntityData = [];
    const tableEntityData = []; // Declared in global scope because its data is used (read) also in query table
    for (var [key, value] of hashmapEntityType) {
      tableEntityData.push(value);
    }

    var tableTaxonomyData = [];
    for (var [key, value] of hashmapTaxonomy) {
      tableTaxonomyData.push(value);
    }

    entities.value = tableEntityData;
    taxonomy.value = tableTaxonomyData;
  }

  async function fetchOntologyInfo() {
    const url = new URL(`${backendHost}/api/ontologyInfo`);
    const response = await fetch(url);
    console.log(response);
    if (!response.ok)
      return console.error('Failed to fetch ontology info', response);
    return await response.json();
  }

  async function fetchTaxonomyTree(taxonomyTree, type) {
    const url = new URL(`${backendHost}/api/tree`);
    url.search = new URLSearchParams(nonNulls({ type })).toString();
    const response = await fetch(url);
    if (!response.ok)
      return console.error('Failed to fetch taxonomy tree', response);
    const responseObj = await response.json();
    const result = [];
    if (taxonomyTree && taxonomyTree.length > 0) {
      result.push(...taxonomyTree);
    }
    result.push(
      ...responseObj.taxonomyTree.rootEntries
        .filter(
          (it) =>
            !taxonomyTree ||
            !taxonomyTree.find((node) => node.data.iri === it.iri)
        )
        .map((it) => ({
          key: it.iri,
          label: it.label,
          leaf: it.directChildrenCount === 0,
          data: it,
        }))
    );
    return result;
  }

  async function fetchExpandedTaxonomyTree(node) {
    const url = new URL(`${backendHost}/api/tree/expand`);
    url.search = new URLSearchParams(
      nonNulls({ iri: node.data.iri, type: node.data.type })
    ).toString();
    const response = await fetch(url);
    if (!response.ok)
      return console.error('Failed to fetch taxonomy tree expand', response);
    const responseObj = await response.json();
    const result = [];
    if (node.children && node.children.length > 0) {
      result.push(...node.children);
    }
    result.push(
      ...responseObj.taxonomyTree.rootEntries
        .filter(
          (it) =>
            !node.children ||
            !node.children.find((child) => child.data.iri === it.iri)
        )
        .map((it) => ({
          key: it.iri,
          label: it.label,
          leaf: it.directChildrenCount === 0,
          data: it,
        }))
    );
    return result;
  }

  function toTreeNode(entry) {
    return {
      key: entry.iri,
      label: entry.label,
      leaf: entry.directChildrenCount === 0,
      data: entry,
      children: entry.expandedChildren?.map(toTreeNode),
    };
  }

  async function fetchSelectedTaxonomyTree(iri, type) {
    const url = new URL(`${backendHost}/api/tree/select`);
    url.search = new URLSearchParams(nonNulls({ iri, type })).toString();
    const response = await fetch(url);
    if (!response.ok)
      return console.error('Failed to fetch taxonomy tree select', response);
    const responseObj = await response.json();
    return {
      tree: responseObj.taxonomyTree.rootEntries.map(toTreeNode),
      type: responseObj.entityType,
    };
  }

  async function fetchEntityInfo(iri) {
    const url = new URL(`${backendHost}/api/entityInfo`);
    url.search = new URLSearchParams(nonNulls({ iri })).toString();
    const response = await fetch(url);
    if (!response.ok)
      return console.error('Failed to fetch selected entity info', response);
    return await response.json();
  }

  async function fetchSearchEntities(
    query,
    types = null,
    subClassOf = null,
    offset = null,
    limit = null
  ) {
    const url = new URL(`${backendHost}/api/search`);
    url.search = new URLSearchParams(
      nonNulls({
        query,
        types: types ? JSON.stringify(types) : null,
        subClassOf,
        offset,
        limit,
      })
    ).toString();
    const response = await fetch(url);
    if (!response.ok)
      return console.error('Failed to fetch search entities', response);
    const responseObj = await response.json();
    return responseObj.entities;
  }

  async function fetchIndividualProperties(query, classIri) {
    const url = new URL(`${backendHost}/api/editor/individualProperties`);
    url.search = new URLSearchParams(nonNulls({ query, classIri })).toString();
    const response = await fetch(url);
    if (!response.ok)
      return console.error('Failed to fetch search entities', response);
    const responseObj = await response.json();
    return responseObj.properties;
  }

  async function fetchOntologyEntities() {
    const url = new URL(`${backendHost}/api/ontology/entities`);
    const response = await fetch(url);

    if (!response.ok) {
      return console.error('Failed to fetch ontology entities', response);
    }

    const responseObj = await response.json();

    return responseObj.entities;
  }

  async function runSqwrlQuery(queryString) {
    console.log(queryString);
    const url = new URL(`${backendHost}/api/sqwrl`);

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        queryString,
      }),
    });
    console.log(response);

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || 'Failed to run SQWRL query');
    }

    return await response.json();
  }

  /**
   * Executa uma pesquisa de datasets através do backend MyCODA.
   *
   * O pedido contêm os critérios, ordenação, campos selecionados
   * e paginação. A query string OpenAIRE é construída pelo backend.
   *
   * @param {Object} request Pedido no formato DatasetSearchRequest.
   * @param {AbortSignal|null} signal Permite cancelar o pedido HTTP.
   * @returns {Promise<Object>} Resposta normalizada da pesquisa.
   */
  async function searchDatasets(request, signal = null) {
    const url = new URL(`${backendHost}/api/datasets/search`);

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(request),
      signal,
    });

    const contentType = response.headers.get('content-type') ?? '';

    const responseObj = contentType.includes('application/json')
      ? await response.json().catch(() => null)
      : null;

    if (!response.ok) {
      const error = new Error(
        responseObj?.message ??
        `Failed to search datasets. HTTP status: ${response.status}`
      );

      error.code = responseObj?.code ?? 'DATASET_SEARCH_ERROR';
      error.status = response.status;

      throw error;
    }

    return {
      queryString: responseObj?.queryString ?? '',
      page: responseObj?.page ?? request.page ?? 1,
      pageSize: responseObj?.pageSize ?? request.pageSize ?? 20,
      totalResults: responseObj?.totalResults ?? null,
      hasNextPage: responseObj?.hasNextPage ?? false,
      selectedFields: responseObj?.selectedFields ?? [],
      results: responseObj?.results ?? [],
      warnings: responseObj?.warnings ?? [],
    };
  }

  /**
   * Carrega um vocabulário de datasets exposto pelo backend MyCODA.
   *
   * @param {string} vocabulary Nome do vocabulário.
   * @returns {Promise<Array>} Opções { code, title }.
   */
  async function fetchDatasetVocabulary(vocabulary) {
    const url = new URL(
      `${backendHost}/api/datasets/vocabularies/${encodeURIComponent(vocabulary)}`
    )

    const response = await fetch(url, {
      headers: {
        Accept: 'application/json',
      },
    })

    const responseObj = await response.json().catch(() => null)

    if (!response.ok) {
      throw new Error(
        responseObj?.message ??
        `Failed to load dataset vocabulary: ${vocabulary}.`
      )
    }

    return responseObj ?? []
  }

  /** Compatibilidade com chamadas existentes. */
  async function fetchDatasetCountries() {
    return fetchDatasetVocabulary('countries')
  }

  /**
   * Pesquisa problemas de otimização na OPL.
   *
   * @param {Object} request Pedido de pesquisa OPL
   * @param {AbortSignal|null} signal Sinal opcional para cancelar o pedido
   * @returns {Promise<Object>} Resultados normalizados da pesquisa
   */
  async function searchOplDatasets(request, signal = null) {
    const url = new URL(
      `${backendHost}/api/datasets/opl/search`
    )

    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
      },
      body: JSON.stringify(request),
      signal,
    })

    const contentType =
      response.headers.get('content-type') ?? ''

    const responseObj =
      contentType.includes('application/json')
        ? await response.json().catch(() => null)
        : null

    if (!response.ok) {
      const error = new Error(
        responseObj?.message ??
        `Failed to search OPL datasets. HTTP status: ${response.status}`
      )

      error.code =
        responseObj?.code ?? 'OPL_DATASET_SEARCH_ERROR'

      error.status = response.status

      throw error
    }

    return {
      page:
        responseObj?.page ??
        request.page ??
        1,

      pageSize:
        responseObj?.pageSize ??
        request.pageSize ??
        20,

      totalResults:
        responseObj?.totalResults ?? 0,

      hasNextPage:
        responseObj?.hasNextPage ?? false,

      results:
        responseObj?.results ?? [],

      warnings:
        responseObj?.warnings ?? [],
    }
  }


  /**
   * Executa uma pesquisa híbrida OpenAIRE + OPL.
   *
   * O backend separa os critérios por fonte e devolve os resultados
   * agrupados em openAire e opl.
   *
   * @param {Object} request Pedido de pesquisa híbrida.
   * @param {AbortSignal|null} signal Sinal opcional para cancelar o pedido.
   * @returns {Promise<Object>} Resposta híbrida normalizada.
   */
 /**
 * Executa uma pesquisa híbrida OpenAIRE + OPL.
 *
 * @param {Object} request Pedido da pesquisa híbrida.
 * @param {AbortSignal|null} signal Sinal opcional de cancelamento.
 * @returns {Promise<Object>} Resposta híbrida normalizada.
 */
async function searchHybridDatasets(request, signal = null) {
  const url = new URL(
    `${backendHost}/api/datasets/hybrid/search`
  )

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
    },
    body: JSON.stringify(request),
    signal,
  })

  const contentType =
    response.headers.get('content-type') ?? ''

  const responseObj =
    contentType.includes('application/json')
      ? await response.json().catch(() => null)
      : null

  if (!response.ok) {
    const error = new Error(
      responseObj?.message ??
        `Failed to perform hybrid dataset search. HTTP status: ${response.status}`
    )

    error.code =
      responseObj?.code ?? 'HYBRID_DATASET_SEARCH_ERROR'

    error.status = response.status

    throw error
  }

  return {
    page:
      responseObj?.page ??
      request.page ??
      1,

    pageSize:
      responseObj?.pageSize ??
      request.pageSize ??
      20,

    totalResults:
      responseObj?.totalResults ?? 0,

    hasNextPage:
      responseObj?.hasNextPage ?? false,

    selectedFields:
      responseObj?.selectedFields ?? [],

    results:
      responseObj?.results ?? [],

    openAire:
      responseObj?.openAire ?? null,

    opl:
      responseObj?.opl ?? null,

    warnings:
      responseObj?.warnings ?? [],
  }
}

async function fetchOntologyGraph(
  iri,
  type,
  depth = 1,
) {
  const url = new URL(
    `${backendHost}/api/ontology/graph`,
  )

  url.search = new URLSearchParams(
    nonNulls({
      iri,
      type,
      depth,
    }),
  ).toString()

  const response = await fetch(url, {
    headers: {
      Accept: 'application/json',
    },
  })

  const responseObj =
    await response.json().catch(() => null)

  if (!response.ok) {
    const error = new Error(
      responseObj?.message ??
        'Failed to load ontology graph.',
    )

    error.code =
      responseObj?.code ??
      'ONTOLOGY_GRAPH_ERROR'

    error.status = response.status

    throw error
  }

  return {
    nodes: responseObj?.nodes ?? [],
    edges: responseObj?.edges ?? [],
  }
}


  return {
    backendHost,
    entities,
    taxonomy,
    taxonomyTree,
    entityTypes,
    fetchEntities,
    fetchOntologyInfo,
    fetchOntologyEntities,
    fetchTaxonomyTree,
    fetchExpandedTaxonomyTree,
    fetchSelectedTaxonomyTree,
    fetchEntityInfo,
    fetchSearchEntities,
    fetchIndividualProperties,
    runSqwrlQuery,
    searchDatasets,
    fetchDatasetVocabulary,
    fetchDatasetCountries,
    searchOplDatasets,
    searchHybridDatasets,
    fetchOntologyGraph,
  };
});
