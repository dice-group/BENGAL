/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.verbalizer;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import java.util.List;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;

import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.aksw.simba.bengal.selector.SimpleSummarySelector;
import org.aksw.simba.bengal.selector.TripleSelector;
import org.aksw.triple2nl.TripleConverter;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;

/**
 * A deterministic verbalizer which relies on the SemWeb2NL project.
 *
 * @author ngonga
 * @author roeder
 */
public class SemWeb2NLVerbalizer implements Verbalizer {

    public Document generateDocument(List<Statement> triples) {

        SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
        TripleConverter converter = new TripleConverter(endpoint);
        String text = "";

        //generate text
        for (Statement s : triples) {
            Triple t = Triple.create(s.getSubject().asNode(), s.getPredicate().asNode(), s.getObject().asNode());
            text = text + " " + converter.convertTripleToText(t);
        }
        text = text.substring(1);

        //annotate entities
        Document document = new DocumentImpl(text);
        Set<Resource> resources = new HashSet<>();
        for (Statement s : triples) {
            resources.add(s.getSubject());
            if (s.getObject().isResource()) {
                resources.add(s.getObject().asResource());
            }
        }
        return annotateDocument(document, resources);
    }

    private String getEnglishLabel(String resource) {
        SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
        if (resource.equals(RDF.type.getURI())) {
            return "type";
        } else if (resource.equals(RDFS.label.getURI())) {
            return "label";
        }
        try {
            String labelQuery = "SELECT ?label WHERE {<" + resource + "> "
                    + "<http://www.w3.org/2000/01/rdf-schema#label> ?label. FILTER (lang(?label) = 'en')}";

            // take care of graph issues. Only takes one graph. Seems like some sparql endpoint do
            // not like the FROM option.
            ResultSet results = new SparqlQuery(labelQuery, endpoint).send();

            //get label from knowledge base
            String label = null;
            QuerySolution soln;
            while (results.hasNext()) {
                soln = results.nextSolution();
                // process query here
                {
                    label = soln.getLiteral("label").getLexicalForm();
                }
            }
            return label;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Document annotateDocument(Document document, Set<Resource> resources) {
        for(Resource r: resources)
        {
            document = annotateDocument(document, r);
        }
        return document;
    }

    private Document annotateDocument(Document document, Resource resource) {
        String label = getEnglishLabel(resource.getURI());
        String text = document.getText();

        //find all positions
        ArrayList<Integer> positions = new ArrayList();
        Pattern p = Pattern.compile(label);  // insert your pattern here
        Matcher m = p.matcher(text);
        while (m.find()) {
            positions.add(m.start());
        }

        for (int index : positions) {
            document.addMarking(new NamedEntity(index, label.length(), resource.getURI()));
        }

        return document;
    }

    public static void main(String args[]) {
        Set<String> classes = new HashSet<>();
        classes.add("<http://dbpedia.org/ontology/Person>");
        classes.add("<http://dbpedia.org/ontology/Place>");
        classes.add("<http://dbpedia.org/ontology/Organisation>");
        TripleSelector ts = new SimpleSummarySelector(classes, classes, "http://dbpedia.org/sparql", null);
        List<Statement> stmts = ts.getNextStatements();
        Document doc = new SemWeb2NLVerbalizer().generateDocument(stmts);
        System.out.println(doc);
    }
}
