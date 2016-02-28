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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 *
 * @author ngonga
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 */
public class BengalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BengalController.class);

    public void generateCorpus(Map<String, String> parameters, Model data, String corpusName) {
        // TODO instantiate components;
        TripleSelector tripleSelector = null;
        Verbalizer verbalizer = null;
        Paraphraser paraphraser = null;

        // TODO get the number of documents from the parameters
        int numberOfDocuments = 0;

        List<Statement> triples;
        Document document;
        List<Document> documents = new ArrayList<Document>();
        while (documents.size() < numberOfDocuments) {
            // TODO select triples
            triples = tripleSelector.getNextStatements();
            // TODO create document
            document = verbalizer.generateDocument(triples);
            // TODO paraphrase document
            document = paraphraser.getParaphrase(document);
            // If the generation and paraphrasing were successful
            if (document != null) {
                documents.add(document);
            }
        }

        // TODO generate file name and path from corpus name
        String filePath = null;
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

        return;
    }
}
