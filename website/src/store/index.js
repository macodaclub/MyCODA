import {defineStore} from 'pinia'
import {computed, ref} from "vue";
import {buildTaxonomyOfClass, evaluateXPathQuery, populateHashMapEntityType} from "@/utils/owlparser/utils.js";
import {nonNulls} from "@/utils/utils/utils.js";

// You can name the return value of `defineStore()` anything you want,
// but it's best to use the name of the store and surround it with `use`
// and `Store` (e.g. `useUserStore`, `useCartStore`, `useProductStore`)
// the first argument is a unique id of the store across your application
export const useOntologyStore = defineStore('ontology', () => {
    const entities = ref([]);
    const taxonomy = ref([]);
    const taxonomyTree = ref([]);

    const entityTypes = computed(() => entities.value.map(it => it.type).filter((value, index, self) => self.indexOf(value) === index));

    async function fetchEntities() {
        const response = await fetch('http://127.0.0.1:8081/static/MaCODA.owl');
        if (!response.ok) return console.error('Failed to fetch ontology', response);
        const text = await response.text();
        const parser = new DOMParser();
        const xmlDoc = parser.parseFromString(text, "text/xml");
        const strQuery = "/rdf:RDF/owl:Class | /rdf:RDF/owl:ObjectProperty | /rdf:RDF/owl:DatatypeProperty | /rdf:RDF/owl:NamedIndividual";
        const thisIterator = evaluateXPathQuery(strQuery, xmlDoc, true);
        let thisResult;
        const hashmapEntityType = new Map();
        const hashmapTaxonomy = new Map();
        while (thisResult = thisIterator.iterateNext()) {

            const strEntityId = evaluateXPathQuery("@rdf:about", thisResult, false);
            const strEntityLabel = strEntityId.substring(strEntityId.indexOf('#') + 1);

            hashmapEntityType.set(strEntityLabel, populateHashMapEntityType(thisResult, xmlDoc));

            if (thisResult.nodeName === "owl:Class") {
                // If it is a first level class (has no subclasses), add to the hashmapTaxonomy, including its subclasses tree (recursively)
                if (!evaluateXPathQuery("rdfs:subClassOf/@rdf:resource", thisResult, false)) {
                    hashmapTaxonomy.set(strEntityLabel, buildTaxonomyOfClass(thisResult, xmlDoc));
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
        const url = new URL('http://127.0.0.1:8081/api/ontologyInfo');
        const response = await fetch(url);
        if (!response.ok) return console.error('Failed to fetch ontology info', response);
        return await response.json();
    }

    async function fetchTaxonomyTree(taxonomyTree, type) {
        const url = new URL('http://127.0.0.1:8081/api/tree');
        url.search = new URLSearchParams(nonNulls({type})).toString();
        const response = await fetch(url);
        if (!response.ok) return console.error('Failed to fetch taxonomy tree', response);
        const responseObj = await response.json();
        const result = [];
        if (taxonomyTree && taxonomyTree.length > 0) {
            result.push(...taxonomyTree);
        }
        result.push(...responseObj.taxonomyTree.rootEntries
            .filter(it => !taxonomyTree || !taxonomyTree.find(node => node.data.iri === it.iri))
            .map(it => ({
                key: it.iri,
                label: it.label,
                leaf: it.directChildrenCount === 0,
                data: it
            })));
        return result;
    }

    async function fetchExpandedTaxonomyTree(node) {
        const url = new URL('http://127.0.0.1:8081/api/tree/expand');
        url.search = new URLSearchParams(nonNulls({iri: node.data.iri, type: node.data.type})).toString();
        const response = await fetch(url);
        if (!response.ok) return console.error('Failed to fetch taxonomy tree', response);
        const responseObj = await response.json();
        const result = [];
        if (node.children && node.children.length > 0) {
            result.push(...node.children);
        }
        result.push(...responseObj.taxonomyTree.rootEntries
            .filter(it => !node.children || !node.children.find(child => child.data.iri === it.iri))
            .map(it => ({
                key: it.iri,
                label: it.label,
                leaf: it.directChildrenCount === 0,
                data: it
            })));
        return result;
    }

    function toTreeNode(entry) {
        return {
            key: entry.iri,
            label: entry.label,
            leaf: entry.directChildrenCount === 0,
            data: entry,
            children: entry.expandedChildren?.map(toTreeNode)
        }
    }

    async function fetchSelectedTaxonomyTree(iri, type) {
        const url = new URL('http://127.0.0.1:8081/api/tree/select');
        url.search = new URLSearchParams(nonNulls({iri, type})).toString();
        const response = await fetch(url);
        if (!response.ok) return console.error('Failed to fetch taxonomy tree', response);
        const responseObj = await response.json();
        return {
            tree: responseObj.taxonomyTree.rootEntries.map(toTreeNode),
            type: responseObj.entityType
        };
    }

    async function fetchSelectedEntityInfo(iri) {
        const url = new URL('http://127.0.0.1:8081/api/entityInfo');
        url.search = new URLSearchParams(nonNulls({iri})).toString();
        const response = await fetch(url);
        if (!response.ok) return console.error('Failed to fetch taxonomy tree', response);
        return await response.json();
    }

    return {
        entities,
        taxonomy,
        taxonomyTree,
        entityTypes,
        fetchEntities,
        fetchOntologyInfo,
        fetchTaxonomyTree,
        fetchExpandedTaxonomyTree,
        fetchSelectedTaxonomyTree,
        fetchSelectedEntityInfo
    }
});