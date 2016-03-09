/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.verbalizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.aksw.simba.bengal.selector.SimpleSummarySelector;
import org.aksw.simba.bengal.selector.TripleSelector;
import org.aksw.triple2nl.TripleConverter;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.BitSet;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * A deterministic verbalizer which relies on the SemWeb2NL project.
 *
 * @author ngonga
 * @author roeder
 */
public class SemWeb2NLVerbalizer implements Verbalizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemWeb2NLVerbalizer.class);

    private TripleConverter converter;
    private SparqlEndpoint endpoint;

    public SemWeb2NLVerbalizer(SparqlEndpoint endpoint) {
        this.endpoint = endpoint;
        converter = new TripleConverter(this.endpoint);
    }

    public Document generateDocument(List<Statement> triples) {
        // generate sub documents
        List<Document> subDocs = new ArrayList<Document>(triples.size());
        Document document;
        for (Statement s : triples) {
            Triple t = Triple.create(s.getSubject().asNode(), s.getPredicate().asNode(), s.getObject().asNode());
            document = new DocumentImpl(converter.convertTripleToText(t));
            if (!annotateDocument(document, s)) {
                LOGGER.warn("One of the triples couldn't be translated. Returning null. triple={}", s);
                return null;
            }
            subDocs.add(document);
        }
        // generate the document containing all the sub documents
        document = new DocumentImpl();
        StringBuilder textBuilder = new StringBuilder();
        for (Document subDoc : subDocs) {
            if (textBuilder.length() > 0) {
                textBuilder.append(' ');
            }
            // add the entities
            for (NamedEntity ne : subDoc.getMarkings(NamedEntity.class)) {
                ne.setStartPosition(ne.getStartPosition() + textBuilder.length());
                document.addMarking(ne);
            }
            // append the text
            textBuilder.append(subDoc.getText());
            textBuilder.append('.');
        }
        document.setText(textBuilder.toString());
        return document;
    }

    private boolean annotateDocument(Document document, Statement s) {
        if (s.getObject().isResource()) {
            return annotateDocument(document, s.getSubject(), s.getObject().asResource());
        } else {
            return annotateDocument(document, s.getSubject());
        }
    }

    private String getEnglishLabel(String resource) {

        if (resource.equals(RDF.type.getURI())) {
            return "type";
        } else if (resource.equals(RDFS.label.getURI())) {
            return "label";
        }
        try {
            String labelQuery = "SELECT ?label WHERE {<" + resource + "> "
                    + "<http://www.w3.org/2000/01/rdf-schema#label> ?label. FILTER (lang(?label) = 'en')}";

            // take care of graph issues. Only takes one graph. Seems like some
            // sparql endpoint do
            // not like the FROM option.
            ResultSet results = new SparqlQuery(labelQuery, endpoint).send();

            // get label from knowledge base
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

    private boolean annotateDocument(Document document, Resource resource) {
        String label = getEnglishLabel(resource.getURI());
        if (label == null) {
            LOGGER.warn("Couldn't find an English label for " + resource.toString() + ". Returning null.");
            return false;
        }
        String text = document.getText();
        int pos = text.indexOf(label);
        if (pos < 0) {
            LOGGER.warn("Couldn't find the label \"{}\" inside the given text \"{}\". Returning false.", label, text);
            return false;
        }
        document.addMarking(new NamedEntity(pos, label.length(), resource.getURI()));
        return true;
    }

    private boolean annotateDocument(Document document, Resource subject, Resource object) {
        // we have to find out which label is the longer one and start with it
        String label1 = getEnglishLabel(subject.getURI());
        if (label1 == null) {
            LOGGER.warn("Couldn't find an English label for " + subject.toString() + ". Returning null.");
            return false;
        }
        String label2 = getEnglishLabel(object.getURI());
        if (label2 == null) {
            LOGGER.warn("Couldn't find an English label for " + object.toString() + ". Returning null.");
            return false;
        }
        Resource r1, r2;
        if (label1.length() > label2.length()) {
            r1 = subject;
            r2 = object;
        } else {
            String temp = label1;
            label1 = label2;
            label2 = temp;
            r2 = subject;
            r1 = object;
        }
        // Now, label1 is the longer label and r1 is its resource
        // Let's find the larger label inside the text
        String text = document.getText();
        int pos1 = text.indexOf(label1);
        if (pos1 < 0) {
            LOGGER.warn("Couldn't find the label \"{}\" inside the given text \"{}\". Returning false.", label1, text);
            return false;
        }
        document.addMarking(new NamedEntity(pos1, label1.length(), r1.getURI()));
        // Let's find the smaller label inside the text. We have to make sure
        // that it is not inside of the larger label's marking.
        BitSet blockedPositions = new BitSet(text.length());
        blockedPositions.set(pos1, pos1 + label1.length());
        int pos2 = -label2.length();
        do {
            pos2 = text.indexOf(label2, pos2 + label2.length());
            while (pos2 < 0) {
                LOGGER.warn("Couldn't find the label \"{}\" inside the given text \"{}\". Returning false.", label1,
                        text);
                return false;
            }
            // repeat while the position found is inside the marking of label1.
            // Since label1 is larger, we can simply check whether the beginning
            // or the end of label2 is blocked by the first label.
        } while (blockedPositions.get(pos2) || blockedPositions.get(pos2 + label2.length()));
        document.addMarking(new NamedEntity(pos2, label2.length(), r2.getURI()));
        return true;
    }

    public static void main(String args[]) {
        Set<String> classes = new HashSet<>();
        classes.add("<http://dbpedia.org/ontology/Person>");
        classes.add("<http://dbpedia.org/ontology/Place>");
        classes.add("<http://dbpedia.org/ontology/Organisation>");
        TripleSelector ts = new SimpleSummarySelector(classes, classes, "http://dbpedia.org/sparql", null);
        List<Statement> stmts = ts.getNextStatements();
        Document doc = new SemWeb2NLVerbalizer(SparqlEndpoint.getEndpointDBpedia()).generateDocument(stmts);
        System.out.println(doc);
    }
}
