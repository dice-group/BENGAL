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
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.gerbil.transfer.nif.data.TypedNamedEntity;
import org.aksw.simba.bengal.paraphrasing.ParaphraseService;
import org.aksw.simba.bengal.paraphrasing.Paraphraser;
import org.aksw.simba.bengal.paraphrasing.ParaphraserImpl;
import org.aksw.simba.bengal.paraphrasing.Paraphrasing;
import org.aksw.simba.bengal.selector.TripleSelector;
import org.aksw.simba.bengal.selector.TripleSelectorFactory;
import org.aksw.simba.bengal.selector.TripleSelectorFactory.SelectorType;
import org.aksw.simba.bengal.verbalizer.AvatarVerbalizer;
import org.aksw.simba.bengal.verbalizer.BVerbalizer;
import org.aksw.simba.bengal.verbalizer.NumberOfVerbalizedTriples;
import org.aksw.simba.bengal.verbalizer.SemWeb2NLVerbalizer;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 *
 * @author ngonga
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 */
public class BengalController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BengalController.class);
    // private static final String NUMBEROFDOCS = "numberofdocs";

    private static final int DEFAULT_NUMBER_OF_DOCUMENTS = 500;
    private static final long SEED = 21;
    private static final int MIN_SENTENCE = 20;
    private static final int MAX_SENTENCE = 50;
    private static final SelectorType SELECTOR_TYPE = SelectorType.STAR;
    private static final boolean USE_PARAPHRASING = true;
    private static final boolean USE_PRONOUNS = false;
    private static final boolean USE_SURFACEFORMS = true;
    private static final boolean USE_AVATAR = false;
    private static final boolean USE_ONLY_OBJECT_PROPERTIES = false;
    private static final long WAITING_TIME_BETWEEN_DOCUMENTS = 500;

    public static void main(String args[]) {
        String typeSubString = "";
        String endpoint;
        Set<String> classes = new HashSet<>();
        SelectorType selectorType;
        long seed;
        int numberOfDocuments;
        int minSentences;
        int maxSentences;
        long waitingTime;
        String outputDir = "";
        if (args.length > 0) {
            if (args.length < 3) {
                System.err.println(
                        "Wrong number of arguments. Arguments: <endpoint-URL> [star|hybrid|path|sim_star] <output-dir> <seed> <number-of-documents> <waiting-time> <class>+");
            }
            endpoint = args[0];
            typeSubString = args[1];
            selectorType = SelectorType.valueOf(typeSubString.toUpperCase());
            outputDir = args[2];
            if ((outputDir.length() > 0) && (!outputDir.endsWith("/"))) {
                outputDir += "/";
            }
            seed = Long.parseLong(args[3]);
            numberOfDocuments = Integer.parseInt(args[4]);
            minSentences = MIN_SENTENCE;
            maxSentences = MAX_SENTENCE;
            waitingTime = Long.parseLong(args[5]);
            for (int i = 6; i < args.length; ++i) {
                classes.add(args[i]);
            }
        } else {
            if (USE_AVATAR) {
                typeSubString = "summary";
            } else {
                switch (SELECTOR_TYPE) {
                case STAR: {
                    typeSubString = "star";
                    break;
                }
                case HYBRID: {
                    typeSubString = "hybrid";
                    break;
                }
                case PATH: {
                    typeSubString = "path";
                    break;
                }
                case SIM_STAR: {
                    typeSubString = "sym";
                    break;
                }
                }
            }
            selectorType = SELECTOR_TYPE;
            endpoint = "http://dbpedia.org/sparql";
            classes.add("http://dbpedia.org/ontology/Person");
            classes.add("http://dbpedia.org/ontology/Place");
            classes.add("http://dbpedia.org/ontology/Organisation");
            seed = SEED;
            numberOfDocuments = DEFAULT_NUMBER_OF_DOCUMENTS;
            minSentences = MIN_SENTENCE;
            maxSentences = MAX_SENTENCE;
            waitingTime = WAITING_TIME_BETWEEN_DOCUMENTS;
        }
        String corpusName = outputDir + "bengal_" + typeSubString + "_" + (USE_PRONOUNS ? "pronoun_" : "")
                + (USE_SURFACEFORMS ? "surface_" : "") + (USE_PARAPHRASING ? "para_" : "")
                + numberOfDocuments + ".ttl";
        LOGGER.info("Will create {} using classes {}", corpusName, classes);

        BengalController.generateCorpus(endpoint, corpusName, classes, selectorType, seed, numberOfDocuments,
                minSentences, maxSentences, USE_PARAPHRASING, USE_PRONOUNS, USE_SURFACEFORMS, USE_AVATAR,
                USE_ONLY_OBJECT_PROPERTIES, waitingTime);
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

    public static void generateCorpus(String endpoint, String corpusName, Set<String> classes,
            SelectorType selectorType, long seed, int numberOfDocuments, int minSentences, int maxSentences,
            boolean useParaphrasing, boolean usePronouns, boolean useSurfaceforms, boolean useAvatar,
            boolean useOnlyObjectProps, long waitingTime) {
        // if (parameters == null) {
        // parameters = new HashMap<>();
        // }

        // instantiate components;
        TripleSelectorFactory factory = new TripleSelectorFactory();
        TripleSelector tripleSelector = null;
        BVerbalizer verbalizer = null;
        AvatarVerbalizer alernativeVerbalizer = null;
        if (useAvatar) {
            alernativeVerbalizer = AvatarVerbalizer.create(classes, useOnlyObjectProps ? classes : new HashSet<>(),
                    endpoint, null, seed, false);
            if (alernativeVerbalizer == null) {
                return;
            }
        } else {
            tripleSelector = factory.create(selectorType, classes, useOnlyObjectProps ? classes : new HashSet<>(),
                    endpoint, null, minSentences, maxSentences, seed);
            verbalizer = new SemWeb2NLVerbalizer(SparqlEndpoint.getEndpointDBpedia(), usePronouns, useSurfaceforms);
        }
        Paraphraser paraphraser = null;
        if (useParaphrasing) {
            ParaphraseService paraService = Paraphrasing.create();
            if (paraService != null) {
                paraphraser = new ParaphraserImpl(paraService);
            } else {
                LOGGER.error("Couldn't create paraphrasing service. Aborting.");
                return;
            }
        }

        // Get the number of documents from the parameters
        // int numberOfDocuments = DEFAULT_NUMBER_OF_DOCUMENTS;
        // if (parameters.containsKey(NUMBEROFDOCS)) {
        // try {
        // numberOfDocuments = Integer.parseInt(parameters.get(NUMBEROFDOCS));
        // } catch (Exception e) {
        // LOGGER.error("Could not parse number of documents");
        // }
        // }
        List<Statement> triples;
        Document document = null;
        List<Document> documents = new ArrayList<>();
        int counter = 0;
        Map<String, Set<String>> resourceTypeMapping = new HashMap<>();
        while (documents.size() < numberOfDocuments) {
            if (useAvatar) {
                document = alernativeVerbalizer.nextDocument();
            } else {
                // select triples
                triples = tripleSelector.getNextStatements(resourceTypeMapping);
                if ((triples != null) && (triples.size() >= minSentences)) {
                    // create document
                    document = verbalizer.generateDocument(triples);
                    if (document != null) {
                        List<NumberOfVerbalizedTriples> tripleCounts = document
                                .getMarkings(NumberOfVerbalizedTriples.class);
                        if ((tripleCounts.size() > 0) && (tripleCounts.get(0).getNumberOfTriples() < MIN_SENTENCE)) {
                            LOGGER.error(
                                    "The generated document does not have enough verbalized triples. It will be discarded.");
                            document = null;
                        }
                    }
                    if (document != null) {
                        // Add types of NEs
                        filterAndAddNETypes(document, classes, resourceTypeMapping, endpoint, waitingTime);
                        // paraphrase document
                        if (paraphraser != null) {
                            try {
                                document = paraphraser.getParaphrase(document);
                            } catch (Exception e) {
                                LOGGER.error("Got exception from paraphraser. Using the original document.", e);
                            }
                        }
                    }
                }
            }
            // If the generation and paraphrasing were successful
            if (document != null) {
                LOGGER.info("Created document #" + counter);
                document.setDocumentURI("http://aksw.org/generated/" + counter);
                counter++;
                documents.add(document);
                document = null;
            }
            try {
                if (!useAvatar) {
                    Thread.sleep(waitingTime);
                }
            } catch (InterruptedException e) {
            }
        }

        // generate file name and path from corpus name
        String filePath = corpusName;
        // write the documents
        NIFWriter writer = new TurtleNIFWriter();
        FileOutputStream fout = null;
        int i = 0;
        try {
            fout = new FileOutputStream(filePath);
            for (; i < documents.size(); ++i) {
                writer.writeNIF(documents.subList(i, i + 1), fout);
            }
            // writer.writeNIF(documents, fout);
        } catch (Exception e) {
            System.out.println(documents.get(i));
            LOGGER.error("Error while writing the documents to file. Aborting.", e);
            System.out.println(documents.get(i));
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

    private static void filterAndAddNETypes(Document document, Set<String> classes,
            Map<String, Set<String>> resourceTypeMapping, String endpoint, long waitingTime) {
        // add the types to all named entities for which there are types
        // available
        if ((resourceTypeMapping != null) && (resourceTypeMapping.size() > 0)) {
            List<Marking> markings = document.getMarkings();
            List<Marking> newMarkings = new ArrayList<Marking>();
            MeaningSpan ne;
            Set<String> types;
            for (Marking marking : markings) {
                if (marking instanceof MeaningSpan) {
                    ne = (MeaningSpan) marking;
                    // if there is a type for this entity
                    if (resourceTypeMapping.containsKey(ne.getUri())) {
                        types = resourceTypeMapping.get(ne.getUri());
                    } else {
                        // get the type of this ne
                        types = getTypes(ne.getUri(), endpoint, waitingTime);
                    }
                    // NEs without one of the given types should be removed
                    types = Sets.intersection(classes, types);
                    if (!types.isEmpty()) {
                        newMarkings
                                .add(new TypedNamedEntity(ne.getStartPosition(), ne.getLength(), ne.getUris(), types));
                    }
                } else {
                    newMarkings.add(marking);
                }
            }
            document.setMarkings(newMarkings);
        }
    }

    private static Set<String> getTypes(String uri, String endpoint, long waitingTime) {
        String query = "SELECT ?t WHERE {<" + uri + "> a ?t. } ";
        Query sparqlQuery = QueryFactory.create(query, Syntax.syntaxARQ);

        QueryEngineHTTP httpQuery = new QueryEngineHTTP(endpoint, sparqlQuery);
        // execute a Select query
        Set<String> types = new HashSet<String>();
        try {
            ResultSet results = httpQuery.execSelect();
            QuerySolution solution;
            while (results.hasNext()) {
                solution = results.next();
                types.add(solution.getResource("t").getURI());
            }
        } catch (Exception e) {
            types.clear();
        } finally {
            httpQuery.close();
        }
        // Let's wait some time, before going on to be gentile to the endpoint
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return types;
    }
}
