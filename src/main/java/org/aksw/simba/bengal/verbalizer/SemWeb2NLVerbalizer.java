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
import static com.hp.hpl.jena.vocabulary.RDFSyntax.doc;
import java.util.Collections;
import java.util.Comparator;

/**
 * A deterministic verbalizer which relies on the SemWeb2NL project.
 *
 * @author ngonga
 * @author roeder
 */
public class SemWeb2NLVerbalizer implements Verbalizer,Comparator<NamedEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SemWeb2NLVerbalizer.class);

    private TripleConverter converter;
    private SparqlEndpoint endpoint;
    int total = 0, total2 = 0;

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
            document = new DocumentImpl(converter.convert(t));
            System.out.println(document);
                      
           document = reduceEntityDensity(document, s);
            if (!annotateDocument(document, s)) {
                LOGGER.warn("One of the triples couldn't be translated. Returning null. triple={}", s);
                return null;
            }
            
            //else{
           
            //document = reduceEntityDensity(document, s);
            //}
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
            textBuilder.append(' ');
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

    public Document reduceEntityDensity(Document document, Statement s) {

        String gender = null;
        String tempSub = getEnglishLabel(s.getSubject().toString());
        String tempObj = getEnglishLabel(s.getObject().toString());
        String reduce = document.getText();

        if (tempSub == null || tempObj == null) {
            LOGGER.warn("Couldn't find an label for " + s.getSubject().toString() + ". Returning null.");
            return document;
        } else {

            if (reduce.contains(tempSub)) {
                total = total + 1;
            } 

            if (reduce.contains(tempObj)){
                total2 = total2 + 1;
            } 

            if (total > 1) {
                if (getType(s.getSubject().toString()).equals("http://dbpedia.org/ontology/PopulatedPlace")) {
                    //if (document.getText().contains(tempSub)) {
                    reduce = reduce.replace(tempSub+"'s", "Its");
                        total = 0;
                  //  }
                }
                if (getType(s.getSubject().toString()).equals("http://dbpedia.org/ontology/Organisation")) {
                   // if (document.getText().contains(tempSub)) {
                        reduce = reduce.replace(tempSub, "This organisation");
                        total = 0;
                   // }
                }
                if (getType(s.getSubject().toString()).equals("http://dbpedia.org/ontology/Person")) {
                    //if (document.getText().contains(tempSub)) {
                        if (getGender(s.getSubject().toString()).contains("he")) {
                            gender = "male";
                            reduce = reduce.replace(tempSub, "He");
                            total = 0;
                        }
                        if (getGender(s.getSubject().toString()).contains("she")) {
                            gender = "female";
                            reduce = reduce.replace(tempSub, "She");
                            total = 0;
                        }
                  //  }
                }
            }

            
            if (total2 > 4) {
                if (getType(s.getObject().toString()).equals("http://dbpedia.org/ontology/PopulatedPlace")) {
                    if (s.getObject().toString().equals(s.getSubject().toString())) {

                    } else {
                        reduce = reduce.replace(tempObj, "it");
                        total2 = 0;
                    }
                }
                if (getType(s.getObject().toString()).equals("http://dbpedia.org/ontology/Organisation")) {
                  //  if (document.getText().contains(tempObj)) {
                        reduce = reduce.replace(tempObj, "it");
                        total2 = 0;
                   // }
                }

                if (getType(s.getObject().toString()).equals("http://dbpedia.org/ontology/Person")) {
                  //  if (document.getText().contains(tempObj)) {
                        if (getGender(s.getObject().toString()).contains("he")) {
                            gender = "male";
                            reduce = reduce.replace(tempSub, "him");
                            total2 = 0;
                        }
                        if (getGender(s.getObject().toString()).contains("she")) {
                            gender = "female";
                            reduce = reduce.replace(tempSub, "her");
                            total2 = 0;
                        }
                  //  }
                }
            }
   
            
        Document newDoc = new DocumentImpl(reduce, document.getDocumentURI());
        
        //annotateDocument(newDoc, s);
        /*
        // find all named entities inside the new text
        // first sort them descending by their length
        List<NamedEntity> originalNes = document.getMarkings(NamedEntity.class);
        Collections.sort(originalNes, this);
        // Go through the list of named entities (starting with the longest) and
        // search for them inside the paraphrased text. Make sure that the
        // entities are not overlapping.
        
        BitSet blockedPositions = new BitSet(reduce.length());
        BitSet currentPositions = new BitSet(reduce.length());
        String label;
        int pos;
        for (NamedEntity ne : originalNes) {
            label = document.getText().substring(ne.getStartPosition(), ne.getStartPosition() + ne.getLength());
            pos = -ne.getLength();
            do {
                // search the position in the new text (make sure that we start
                // behind the position we might have found before)
                pos = reduce.indexOf(label, pos + ne.getLength());
                if (pos < 0) {
                    // the position search failed
                    LOGGER.warn(
                            "The paraphrasing changed one of the entities. Couldn't find the surface form \"{}\" in the text \"{}\". Returning the original document.",
                            label, reduce);
                    //return document;
                }
                else {
                currentPositions.clear();
                // check that this part of the String does not already have been
                // blocked
                currentPositions.set(pos, pos + ne.getLength());
                }
            } while (BitSet.intersectionCount(blockedPositions, currentPositions) > 0);
            // Update the position in the new text
            newDoc.addMarking(new NamedEntity(pos, ne.getLength(), ne.getUris()));
            blockedPositions.or(currentPositions);
        }
*/
        return newDoc;
        }
    }

        
    private String getType(String uri) {

        if (uri.equals(RDF.type.getURI())) {
            return "type";
        } else if (uri.equals(RDFS.label.getURI())) {
            return "label";
        }
        try {
            String labelQuery = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
                    + " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
                    + " PREFIX dbr: <http://dbpedia.org/resource/>"
                    + " PREFIX dbo: <http://dbpedia.org/ontology/>"
                    + " PREFIX owl: <http://www.w3.org/2002/07/owl#>"
                    + " SELECT  DISTINCT ?lcs WHERE {"
                    + " ?lcs ^rdf:type <" + uri + ">."
                    + "?lcs rdfs:subClassOf ?x."
                    + "?x rdfs:subClassOf owl:Thing."
                    + "}";

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
                    label = soln.getResource("lcs").toString();
                }
            }
            return label;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getGender(String uri) {

        if (uri.equals(RDF.type.getURI())) {
            return "type";
        } else if (uri.equals(RDFS.label.getURI())) {
            return "label";
        }
        try {
            String labelQuery = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
                    + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
                    + "PREFIX dbr: <http://dbpedia.org/resource/>"
                    + "PREFIX dbo: <http://dbpedia.org/ontology/>"
                    + "PREFIX owl: <http://www.w3.org/2002/07/owl#>"
                    + "SELECT  DISTINCT ?com WHERE {"
                    + " <" + uri + "> rdfs:comment ?com."
                    + "FILTER (lang(?com) = 'en')."
                    + "}";

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
                    label = soln.getLiteral("com").getLexicalForm();
                }
            }
            return label;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        System.out.println("LABEL AQUI");
        int pos = text.indexOf(label);
        if (pos < 0) {
            LOGGER.warn("Couldn't find the label \"{}\" inside the given text \"{}\". Returning false.", label, text);
            //return false;    
       } else{
        document.addMarking(new NamedEntity(pos, label.length(), resource.getURI()));
        }
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
        BitSet blockedPositions = new BitSet(text.length());
        int pos1 = text.indexOf(label1);
        if (pos1 < 0) {
            LOGGER.warn("Couldn't find the label \"{}\" inside the given text \"{}\". Returning false.", label1, text);
            //return false;
       } else {
        document.addMarking(new NamedEntity(pos1, label1.length(), r1.getURI()));
        blockedPositions.set(pos1, pos1 + label1.length());
        }
        // Let's find the smaller label inside the text. We have to make sure
        // that it is not inside of the larger label's marking.
        
        int pos2 = -label2.length();
        do {
            pos2 = text.indexOf(label2, pos2 + label2.length());
            document.addMarking(new NamedEntity(pos2, label2.length(), r2.getURI()));
            while (pos2 < 0) {
                LOGGER.warn("Couldn't find the label \"{}\" inside the given text \"{}\". Returning false.", label1,
                        text);
                return false;
           }
            // repeat while the position found is inside the marking of label1.
            // Since label1 is larger, we can simply check whether the beginning
            // or the end of label2 is blocked by the first label.
        } while (blockedPositions.get(pos2) || blockedPositions.get(pos2 + label2.length()));
        
        return true;
    }

    public static void main(String args[]) {
        Set<String> classes = new HashSet<>();
        classes.add("<http://dbpedia.org/ontology/Person>");
        classes.add("<http://dbpedia.org/ontology/Place>");
        classes.add("<http://dbpedia.org/ontology/Organisation>");
        TripleSelector ts = new SimpleSummarySelector(classes, classes, "http://dbpedia.org/sparql", null);
        
        for (int i = 0; i<=5; i++){
        List<Statement> stmts = ts.getNextStatements();
        Document doc = new SemWeb2NLVerbalizer(SparqlEndpoint.getEndpointDBpedia()).generateDocument(stmts);
        System.out.println(doc);
        }
    }

@Override
        public int compare(NamedEntity n1, NamedEntity n2) {
        int diff = n1.getLength() - n2.getLength();
        if (diff < 0) {
            // n1 is shorter
            return 1;
        } else if (diff > 0) {
            return -1;
        } else {
            return 0;
        }
    }
      
}
