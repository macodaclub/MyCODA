function nsResolver(prefix) { // function to handle xml namespaces
    var ns = {
        'rdf': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#',
        'owl': 'http://www.w3.org/2002/07/owl#',
        'rdfs': 'http://www.w3.org/2000/01/rdf-schema#',
        'xhtml': 'http://www.w3.org/1999/xhtml',
        'mathml': 'http://www.w3.org/1998/Math/MathML'
    };
    return ns[prefix] || null;
}

export function evaluateXPathQuery(pXPathExpression, pContextNode, pIterate) {
    // pIterates false means that evaluateXPathQuery function returns only one result
    // pIterates true returns the iterator to be iterated outside the function
    // ATTENTION: pIterates true RAISES PROBLEMS INSIDE OTHER FUNCTIONS BECAUSE OF PARAMETERS BY VALUE VS BY REFERENCE !!! BETTER NOT TO USE pIterate true inside other functions

    var xpathQueryResultIterator = document.evaluate(pXPathExpression, pContextNode, nsResolver, XPathResult.ANY_TYPE, null);
    if (pIterate) {
        return xpathQueryResultIterator;
    } else {
        var thisResult = xpathQueryResultIterator.iterateNext();
        return thisResult ? thisResult.textContent : "";
    }
}

export function populateHashMapEntityType(pContextNode, pXmlDoc) {
    var strEntityId = evaluateXPathQuery("@rdf:about", pContextNode, false);
    var strEntityLabel = evaluateXPathQuery("rdfs:label", pContextNode, false);
    if (!strEntityLabel) strEntityLabel = strEntityId.substring(strEntityId.indexOf('#') + 1);
    var strEntityComment = evaluateXPathQuery("rdfs:comment", pContextNode, false);
    var strEntityType = "" + pContextNode.nodeName;
    var strEntityTypeToBeVisualizedInEntityTable = strEntityType.replaceAll("owl:Class", "Class").replaceAll("owl:DatatypeProperty", "DatatypeProperty").replaceAll("owl:ObjectProperty", "ObjectProperty").replaceAll("owl:NamedIndividual", "Individual");

    var objClass = {};
    objClass = {
        "name": strEntityLabel,
        "comment": strEntityComment,
        "type": strEntityTypeToBeVisualizedInEntityTable
    };
    objClass["details"] = "";
    // Iterate through details (classes that the individual belongs) of owl:NamedIndividual and create a listIndividualDetails
    if (strEntityType === "owl:NamedIndividual") {
        var listIndividualDetails = [];
        var strParentClassId = evaluateXPathQuery("rdf:type/@rdf:resource", pContextNode, false);
        while (strParentClassId) {
            var strParentClassLabel = evaluateXPathQuery("//owl:Class[@rdf:about='" + strParentClassId + "']/rdfs:label", pContextNode, false);
            if (!strParentClassLabel) strParentClassLabel = strParentClassId.substring(strParentClassId.indexOf('#') + 1);
            listIndividualDetails.push(strParentClassLabel);
            strParentClassId = evaluateXPathQuery("//owl:Class[@rdf:about='" + strParentClassId + "']/rdfs:subClassOf/@rdf:resource", pContextNode, false);
        }
        if (listIndividualDetails.length) objClass["details"] = JSON.stringify({ascendentClassesHierarchy: listIndividualDetails}).replaceAll("\"", "").replaceAll(",", ", ").replaceAll(":", ": ").replaceAll("{", " ").replaceAll("}", " ");
    }

    // Iterate through details (individuals) of owl:Class and create a listClassDetails
    if (strEntityType === "owl:Class") {
        // AscendentClasseshierarchy of the current class
        var listAscendentClasses = [];
        var strSuperClassId = evaluateXPathQuery("//owl:Class[@rdf:about='" + strEntityId + "']/rdfs:subClassOf/@rdf:resource", pContextNode, false);
        while (strSuperClassId) {
            let strSuperClassLabel = evaluateXPathQuery("//owl:Class[@rdf:about='" + strSuperClassId + "']/rdfs:label", pContextNode, false);
            if (!strSuperClassLabel) strSuperClassLabel = strSuperClassId.substring(strSuperClassId.indexOf('#') + 1);
            listAscendentClasses.push(strSuperClassLabel);
            strSuperClassId = evaluateXPathQuery(
                "//owl:Class[@rdf:about='" + strSuperClassId + "']/rdfs:subClassOf/@rdf:resource", pContextNode, false);
        }
        if (listAscendentClasses.length) objClass["details"] = JSON.stringify({ascendentClassesHierarchy: listAscendentClasses}).replaceAll("\"", "").replaceAll(",", ", ").replaceAll(":", ": ").replaceAll("{", " ").replaceAll("}", " ");

        // Direct SubClasses of the current class
        var listDirectSubClasses = [];
        var xpathQueryResultIterator = document.evaluate("//owl:Class[rdfs:subClassOf/@rdf:resource='" + strEntityId + "']/@rdf:about", pContextNode, nsResolver, XPathResult.ANY_TYPE, null);
        var thisResult = xpathQueryResultIterator.iterateNext();
        while (thisResult) {
            const strSubClassId = thisResult.textContent;
            let strSubClassLabel = evaluateXPathQuery("//owl:Class[@rdf:about='" + strSubClassId + "']/rdfs:label", pContextNode, false);
            if (!strSubClassLabel) strSubClassLabel = strSubClassId.substring(strSubClassId.indexOf('#') + 1);
            listDirectSubClasses.push(strSubClassLabel);
            thisResult = xpathQueryResultIterator.iterateNext();
        }
        if (listDirectSubClasses.length) objClass["details"] = objClass["details"] + " " + JSON.stringify({directSubClasses: listDirectSubClasses}).replaceAll("\"", "").replaceAll(",", ", ").replaceAll(":", ": ").replaceAll("{", " \n\n").replaceAll("}", " ");

        // Class Individuals
        const listClassIndividuals = [];
        var xpathQueryResultIterator = document.evaluate("//owl:NamedIndividual/rdf:type[@rdf:resource='" + strEntityId + "']/../@rdf:about", pContextNode, nsResolver, XPathResult.ANY_TYPE, null);
        var thisResult = xpathQueryResultIterator.iterateNext();
        while (thisResult) {
            var strIndividualId = thisResult.textContent;
            let strIndividualLabel = evaluateXPathQuery("//owl:NamedIndividual[@rdf:about='" + strIndividualId + "']/rdfs:label", pContextNode, false);
            if (!strIndividualLabel) strIndividualLabel = strIndividualId.substring(strIndividualId.indexOf('#') + 1);
            listClassIndividuals.push(strIndividualLabel);
            thisResult = xpathQueryResultIterator.iterateNext();
        }
        if (listClassIndividuals.length) objClass["details"] = objClass["details"] + " " + JSON.stringify({classIndividuals: listClassIndividuals}).replaceAll("\"", "").replaceAll(",", ", ").replaceAll(":", ": ").replaceAll("{", " \n\n").replaceAll("}", " ");
    }

    // Iterate through details (domain and range) of owl:ObjectProperty and create a listObjectPropertyDomainDetails and listObjectPropertyRangeDetails
    if (strEntityType === "owl:ObjectProperty") {
        // owl:ObjectProperty Domain
        var listObjectPropertyDomainDetails = [];
        var strObjectPropertyDomainClassId = evaluateXPathQuery("rdfs:domain/@rdf:resource", pContextNode, false);
        var strObjectPropertyDomainClassLabel = evaluateXPathQuery("/rdf:RDF/owl:Class[@rdf:about='" + strObjectPropertyDomainClassId + "']/rdfs:label", pXmlDoc, false);
        if (!strObjectPropertyDomainClassLabel) strObjectPropertyDomainClassLabel = strObjectPropertyDomainClassId.substring(strObjectPropertyDomainClassId.indexOf('#') + 1);
        if (strObjectPropertyDomainClassLabel) listObjectPropertyDomainDetails.push(strObjectPropertyDomainClassLabel);
        if (listObjectPropertyDomainDetails.length) objClass["details"] = JSON.stringify({classDomain: listObjectPropertyDomainDetails}).replaceAll("\"", "").replaceAll(",", ", ").replaceAll(":", ": ").replaceAll("{", " ").replaceAll("}", " ");

        // owl:ObjectProperty Range
        var listObjectPropertyRangeDetails = [];
        var strObjectPropertyRangeClassId = evaluateXPathQuery("rdfs:range/@rdf:resource", pContextNode, false);
        var strObjectPropertyRangeClassLabel = evaluateXPathQuery("/rdf:RDF/owl:Class[@rdf:about='" + strObjectPropertyRangeClassId + "']/rdfs:label", pXmlDoc, false);
        if (!strObjectPropertyRangeClassLabel) strObjectPropertyRangeClassLabel = strObjectPropertyRangeClassId.substring(strObjectPropertyRangeClassId.indexOf('#') + 1);
        if (strObjectPropertyRangeClassLabel) listObjectPropertyRangeDetails.push(strObjectPropertyRangeClassLabel);
        if (listObjectPropertyRangeDetails.length) objClass["details"] = objClass["details"] + " " + JSON.stringify({classRange: listObjectPropertyRangeDetails}).replaceAll("\"", "").replaceAll(",", ", ").replaceAll(":", ": ").replaceAll("{", " \n\n").replaceAll("}", " ");
    }

    // Iterate through details (domain and range) of owl:DatatypeProperty and create a listDatatypePropertyDomainDetails and listDatatypePropertyRangeDetails
    if (strEntityType === "owl:DatatypeProperty") {
        // owl:DatatypeProperty Domain
        var listDatatypePropertyDomainDetails = [];
        var strDatatypePropertyDomainClassId = evaluateXPathQuery("rdfs:domain/@rdf:resource", pContextNode, false);
        var strDatatypePropertyDomainClassLabel = evaluateXPathQuery("/rdf:RDF/owl:Class[@rdf:about='" + strDatatypePropertyDomainClassId + "']/rdfs:label", pXmlDoc, false);
        if (!strDatatypePropertyDomainClassLabel) strDatatypePropertyDomainClassLabel = strDatatypePropertyDomainClassId.substring(strDatatypePropertyDomainClassId.indexOf('#') + 1);
        if (strDatatypePropertyDomainClassLabel) listDatatypePropertyDomainDetails.push(strDatatypePropertyDomainClassLabel);
        if (listDatatypePropertyDomainDetails.length)
            objClass["details"] = JSON.stringify({classDomain: listDatatypePropertyDomainDetails}).replaceAll("\"", "").replaceAll(",", ", ").replaceAll(":", ": ").replaceAll("{", " ").replaceAll("}", " ");

        // owl:DatatypeProperty Range
        var listDatatypePropertyRangeDetails = [];
        var strDatatypePropertyRangeClassId = evaluateXPathQuery("rdfs:range/@rdf:resource", pContextNode, false);
        var strDatatypePropertyRangeDatatypeLabel = strDatatypePropertyRangeClassId.substring(strDatatypePropertyRangeClassId.indexOf('#') + 1);
        if (strDatatypePropertyRangeDatatypeLabel) listDatatypePropertyRangeDetails.push(strDatatypePropertyRangeDatatypeLabel);
        if (listDatatypePropertyRangeDetails.length)
            objClass["details"] = objClass["details"] + " " + JSON.stringify({dataTypeRange: listDatatypePropertyRangeDetails}).replaceAll("\"", "").replaceAll(",", ", ").replaceAll(":", ": ").replaceAll("{", " \n\n").replaceAll("}", " ");
    }
//alert("strClassLabel: " + strClassLabel + "\n length:  " + listClassDetails.length + "\n" + JSON.stringify(listClassDetails).replaceAll("\"",""));
    return objClass;
}

export function buildTaxonomyOfClass(pContextNode, pXmlDoc) {
    //Given a node to a class in pContextNode and the root node of the document, buildTaxonomyOfClass builds the entire hierarchy branch (subclasses) of this node (class)
    // First: extract data of this class such as id (iri), label and axioms (e.g. disjointwith)
    // Second: iterate through subclasses
    var strClassId = evaluateXPathQuery("@rdf:about", pContextNode, false);
    var strClassLabel = evaluateXPathQuery("rdfs:label", pContextNode, false);
    if (!strClassLabel) strClassLabel = strClassId.substring(strClassId.indexOf('#') + 1);
    var strClassComment = evaluateXPathQuery("rdfs:comment", pContextNode, false);

    // Iterate through DisjointClasses and create a listDisjointClasses
    var listDisjointClasses = [];

    var xpathQueryResultIterator = document.evaluate("owl:disjointWith/@rdf:resource", pContextNode, nsResolver, XPathResult.ANY_TYPE, null);
    var thisResult = xpathQueryResultIterator.iterateNext();
    while (thisResult) {
        var strDisjointWithClassLabel = evaluateXPathQuery("/rdf:RDF/owl:Class[@rdf:about='" + thisResult.textContent + "']/rdfs:label", pXmlDoc, false);
        listDisjointClasses.push(strDisjointWithClassLabel);
        thisResult = xpathQueryResultIterator.iterateNext();
    }

    // Create a JSON object with this class data
    var objClass = {};
    objClass = {"name": strClassLabel, "comment": strClassComment};
    if (listDisjointClasses.length) objClass["axioms"] = JSON.stringify({disjointWith: listDisjointClasses}).replaceAll("\"", "").replaceAll(",", ", ").replaceAll(":", ": ").replaceAll("{", " ").replaceAll("}", " ");

    // If current Class has subclasses, add its children to subClassesList and insert _children field (list of subclass objects) in the class JSON object
    var subClassesList = [];
    /*var thisIterator = evaluateXPathQuery("//rdfs:subClassOf[@rdf:resource='" + strClassId + "']/..", pXmlDoc, true);
while (thisResult = thisIterator.iterateNext()) {
var strSubClassLabel = evaluateXPathQuery("rdfs:label", thisResult, false);
subClassesList.push(buildTaxonomyOfClass(thisResult, pXmlDoc));
objClass["_children"] = subClassesList;
}*/
    var xpathQueryResultIterator = document.evaluate("//rdfs:subClassOf[@rdf:resource='" + strClassId + "']/..", pContextNode, nsResolver, XPathResult.ANY_TYPE, null);
    var thisResult = xpathQueryResultIterator.iterateNext();
    while (thisResult) {
//				var strSubClassId = evaluateXPathQuery("@rdf:resource", thisResult, false);
//				var strSubClassLabel = evaluateXPathQuery("rdfs:label", thisResult, false);
//				var strSubClassId = thisResult.textContent;
//				if (! strSubClassLabel) strSubClassLabel = strSubClassId.substring(strSubClassId.indexOf('#') + 1);
        subClassesList.push(buildTaxonomyOfClass(thisResult, pXmlDoc));
        objClass["_children"] = subClassesList;
        thisResult = xpathQueryResultIterator.iterateNext();
    }
    return objClass;
}