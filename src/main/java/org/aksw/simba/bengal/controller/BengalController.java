/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.controller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.gerbil.io.nif.NIFParser;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.simba.bengal.paraphrasing.Paraphraser;
import org.aksw.simba.bengal.selector.PathBasedTripleSelector;
import org.aksw.simba.bengal.selector.SimpleSummarySelector;
import org.aksw.simba.bengal.selector.TripleSelector;
import org.aksw.simba.bengal.verbalizer.SemWeb2NLVerbalizer;
import org.aksw.simba.bengal.verbalizer.Verbalizer;
import org.apache.commons.io.IOUtils;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Statement;

/**
 *
 * @author ngonga
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 */
public class BengalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BengalController.class);
    private static final String NUMBEROFDOCS = "numberofdocs";

    private static final int DEFAULT_NUMBER_OF_DOCUMENTS = 100;
    private static final long SEED = 20;
    private static final int MAX_SENTENCE = 5;
    private static final boolean USE_PATH_PATTERN = true;
    private static final boolean USE_SYMMETRIC_CBD = false;
    private static final long WAITING_TIME_BETWEEN_DOCUMENTS = 1000;

    public static void main(String args[]) {
        String corpusName = "bengal_" + (USE_PATH_PATTERN ? "path" : (USE_SYMMETRIC_CBD ? "sym" : "star")) + "_"
                + Integer.toString(DEFAULT_NUMBER_OF_DOCUMENTS) + ".ttl";
        BengalController.generateCorpus(new HashMap<String, String>(), "http://dbpedia.org/sparql", corpusName);
        // This is just to check whether the created documents make sense
        // If the entities have a bad positioning inside the documents the
        // parser should print warn messages
        NIFParser parser = new TurtleNIFParser();
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(corpusName);
            parser.parseNIF(fin);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(fin);
        }
    }

    public static void generateCorpus(Map<String, String> parameters, String endpoint, String corpusName) {
        if (parameters == null)
            parameters = new HashMap<>();
        // TODO instantiate components;
        Set<String> classes = new HashSet<>();
        classes.add("<http://dbpedia.org/ontology/Person>");
        classes.add("<http://dbpedia.org/ontology/Place>");
        classes.add("<http://dbpedia.org/ontology/Organisation>");

        TripleSelector tripleSelector;
        if (USE_PATH_PATTERN) {
            tripleSelector = new PathBasedTripleSelector(classes, classes, endpoint, null, 1, MAX_SENTENCE, SEED,
                    false);
        } else {
            tripleSelector = new SimpleSummarySelector(classes, classes, endpoint, null, 1, MAX_SENTENCE, SEED,
                    USE_SYMMETRIC_CBD);
        }
        Verbalizer verbalizer = new SemWeb2NLVerbalizer(SparqlEndpoint.getEndpointDBpedia());
        Paraphraser paraphraser = null;

        // Get the number of documents from the parameters
        int numberOfDocuments = DEFAULT_NUMBER_OF_DOCUMENTS;
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
            // select triples
            triples = tripleSelector.getNextStatements();
            if (triples != null) {
                // create document
                document = verbalizer.generateDocument(triples);
                if (document != null) {
                    // TODO paraphrase document
                    if (paraphraser != null) {
                        try {
                            document = paraphraser.getParaphrase(document);
                        } catch (Exception e) {
                            LOGGER.error("Got exception from paraphraser. Using the original document.", e);
                        }
                    }
                    // If the generation and paraphrasing were successful
                    if (document != null) {
                        document.setDocumentURI("http://aksw.org/generated/" + counter);
                        counter++;
                        documents.add(document);
                    }
                }
            }
            try {
                Thread.sleep(WAITING_TIME_BETWEEN_DOCUMENTS);
            } catch (InterruptedException e) {
            }
        }

        // TODO generate file name and path from corpus name
        String filePath = corpusName;
        // write the documents
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
