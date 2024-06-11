import {defineStore} from 'pinia'
import {computed, ref} from "vue";
import {buildTaxonomyOfClass, evaluateXPathQuery, populateHashMapEntityType} from "@/utils/owlparser/utils.js";

// You can name the return value of `defineStore()` anything you want,
// but it's best to use the name of the store and surround it with `use`
// and `Store` (e.g. `useUserStore`, `useCartStore`, `useProductStore`)
// the first argument is a unique id of the store across your application
export const useOntologyStore = defineStore('ontology', () => {
    const entities = ref([]);
    const taxonomy = ref([]);

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

    return {entities, taxonomy, entityTypes, fetchEntities}
})