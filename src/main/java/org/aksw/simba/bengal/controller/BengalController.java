/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.controller;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.simba.bengal.paraphrasing.Paraphraser;
import org.aksw.simba.bengal.selector.TripleSelector;
import org.aksw.simba.bengal.verbalizer.Verbalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.aksw.simba.bengal.selector.SimpleSummarySelector;
import org.aksw.simba.bengal.verbalizer.SemWeb2NLVerbalizer;
import org.dllearner.kb.sparql.SparqlEndpoint;

/**
 *
 * @author ngonga
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 */
public class BengalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BengalController.class);
    private static final String NUMBEROFDOCS = "numberofdocs";

    public static void main(String args[]) throws Exception {
        BengalController.generateCorpus(new HashMap<String, String>(), "http://dbpedia.org/sparql",
                "test_output.bengal.txt");
    }

    public static void generateCorpus(Map<String, String> parameters, String endpoint, String corpusName) throws Exception {
        if (parameters == null)
            parameters = new HashMap<>();
        // TODO instantiate components;
        Set<String> classes = new HashSet<>();
        classes.add("<http://dbpedia.org/ontology/Person>");
        classes.add("<http://dbpedia.org/ontology/Place>");
        classes.add("<http://dbpedia.org/ontology/Organisation>");

        TripleSelector tripleSelector = new SimpleSummarySelector(classes, classes, endpoint, null);
        Verbalizer verbalizer = new SemWeb2NLVerbalizer(SparqlEndpoint.getEndpointDBpedia());
        Paraphraser paraphraser = null;

        // Get the number of documents from the parameters
        int numberOfDocuments = 10;
        if (parameters.containsKey(NUMBEROFDOCS)) {
            try {
                numberOfDocuments = Integer.parseInt(parameters.get(NUMBEROFDOCS));
            } catch (Exception e) {
                LOGGER.error("Could not parse number of documents");
            }
        }
        List<Statement> triples;
        Document document;
        List<Document> documents = new ArrayList<>();
        int counter = 0;
        while (documents.size() < numberOfDocuments) {
            // TODO select triples
            triples = tripleSelector.getNextStatements();
            // TODO create document
            document = verbalizer.generateDocument(triples);
            // TODO paraphrase document
            if (paraphraser != null) {
                document = paraphraser.getParaphrase(document);
            }
            // If the generation and paraphrasing were successful
            if (document != null) {
                document.setDocumentURI("http://aksw.org/generated/" + counter);
                counter++;
                documents.add(document);
            }
        }

        // TODO generate file name and path from corpus name
        String filePath = corpusName;
        // write the document
        NIFWriter writer = new TurtleNIFWriter();
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(filePath);
            writer.writeNIF(documents, fout);
        } catch (Exception e) {
            LOGGER.error("Error while writing the documents to file. Aborting.", e);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }
}
