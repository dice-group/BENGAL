/*
 * To change this license header, choose License Headers in Project Properties. To change this
 * template file, choose Tools | Templates and open the template in the editor.
 */
package org.aksw.simba.bengal.controller;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.gerbil.io.nif.NIFParser;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.simba.bengal.config.BengalRunConfig;
import org.aksw.simba.bengal.paraphrasing.ParaphraseService;
import org.aksw.simba.bengal.paraphrasing.Paraphraser;
import org.aksw.simba.bengal.paraphrasing.ParaphraserImpl;
import org.aksw.simba.bengal.paraphrasing.Paraphrasing;
import org.aksw.simba.bengal.selector.TripleSelector;
import org.aksw.simba.bengal.selector.TripleSelectorFactory;
import org.aksw.simba.bengal.verbalizer.AvatarVerbalizer;
import org.aksw.simba.bengal.verbalizer.BVerbalizer;
import org.aksw.simba.bengal.verbalizer.NumberOfVerbalizedTriples;
import org.aksw.simba.bengal.verbalizer.SemWeb2NLVerbalizer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Statement;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ngonga
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 * @author diegomoussallem
 */
public class BengalController {

	private static final Logger LOGGER = LoggerFactory.getLogger(BengalController.class);
	
	protected static final Option HELP_OPT = Option.builder("h")
	          .longOpt("help")
	          .required(false)
	          .hasArg(false)
	          .build();
	
	protected static final Options CLI_OPTS = new Options();
	protected static final HelpFormatter HELP_FORMATTER = new HelpFormatter();
	protected static final PrintWriter CONSOLE_WRITER = new PrintWriter(System.out);
	protected static final String APP_NAME = "Bengal";
	
	protected static final String PARAPHRASE_OPT = "pp";
	protected static final String PRONOUNS_OPT = "pr";
	protected static final String SURFACEFORMS_OPT = "sf";
	protected static final String ONLYOBJECTPROPS_OPT = "o";
	protected static final String NUMBEROFDOCUMENTS_OPT = "n";
	protected static final String SEEDS_OPT = "sd";
	protected static final String MINSENTENCE_OPT = "mns";
	protected static final String MAXSENTENCE_OPT = "mxs";
	protected static final String WAITTIME_OPT = "wt";
	protected static final String SPARQLENDPOINT_OPT = "se";
	protected static final String SELECTORTYPE_OPT = "st";

	static {
		CLI_OPTS.addOption(PARAPHRASE_OPT, "paraphrase", false, "Use Paraphrasing");
		CLI_OPTS.addOption(PRONOUNS_OPT, "pronouns", false, "Use Pronouns");
		CLI_OPTS.addOption(SURFACEFORMS_OPT, "surfaceforms", false, "Use Surface-Forms");
		CLI_OPTS.addOption(ONLYOBJECTPROPS_OPT, "onlyobjectprops", false, "Use only object properties");

		CLI_OPTS.addOption(NUMBEROFDOCUMENTS_OPT, "numberofdocuments", true, "Number of documents");
		CLI_OPTS.addOption(SEEDS_OPT, "seed", true, "Number of Seeds");
		CLI_OPTS.addOption(MINSENTENCE_OPT, "minsentence", true, "Minimum number of sentences");
		CLI_OPTS.addOption(MAXSENTENCE_OPT, "maxsentence", true, "Maximum number of sentences");
		CLI_OPTS.addOption(WAITTIME_OPT, "waittime", true, "Wait time between documents");
		CLI_OPTS.addOption(SPARQLENDPOINT_OPT, "sparqlendpoint", true, "Sparql Endpoint");
		CLI_OPTS.addRequiredOption(SELECTORTYPE_OPT, "selectortype", true, "Selector Type ('star', 'hybrid', 'path', 'sym' or 'summary')");
	}
	
	  

	public static void main(final String args[]) throws ParseException {
		//Check if help queried
		if(checkForHelp(args)) {
			HELP_FORMATTER.printHelp(APP_NAME, CLI_OPTS);
		}else {
			//***Parsing Stage***
			//Create a parser
			CommandLineParser parser = new DefaultParser();
			try {
				//parse the options passed as command line arguments
				CommandLine cmd = parser.parse( CLI_OPTS, args);
				//***Interrogation Stage***
				BengalRunConfig runConfig = generateConfigBean(cmd);
				process(runConfig);
			} catch(MissingOptionException | MissingArgumentException  me  ) {
				CONSOLE_WRITER.write(me.getMessage()+"\n");
				HELP_FORMATTER.printUsage(CONSOLE_WRITER,80,APP_NAME, CLI_OPTS);
				CONSOLE_WRITER.flush();
			}
		}
	}
	
	protected static BengalRunConfig generateConfigBean(CommandLine cmd) {
		BengalRunConfig runConfig = new BengalRunConfig();
		
		if(cmd.hasOption(PARAPHRASE_OPT)) {
			runConfig.setUseParaphrasing(true);
		}
		if(cmd.hasOption(PRONOUNS_OPT)) {
			runConfig.setUsePronouns(true);
		}
		if(cmd.hasOption(SURFACEFORMS_OPT)) {
			runConfig.setUseSurfaceForms(true);
		}
		
		if(cmd.hasOption(ONLYOBJECTPROPS_OPT)) {
			runConfig.setUseOnlyObjectProps(true);
		}
		
		if(cmd.hasOption(NUMBEROFDOCUMENTS_OPT)) {
			runConfig.setNumberOfDocs(Integer.parseInt(cmd.getOptionValue(NUMBEROFDOCUMENTS_OPT)));
		}
		if(cmd.hasOption(SEEDS_OPT)) {
			runConfig.setSeed(Integer.parseInt(cmd.getOptionValue(SEEDS_OPT)));
		}
		if(cmd.hasOption(MINSENTENCE_OPT)) {
			runConfig.setMinSentence(Integer.parseInt(cmd.getOptionValue(MINSENTENCE_OPT)));
		}
		if(cmd.hasOption(MAXSENTENCE_OPT)) {
			runConfig.setMaxSentence(Integer.parseInt(cmd.getOptionValue(MAXSENTENCE_OPT)));
		}
		if(cmd.hasOption(WAITTIME_OPT)) {
			runConfig.setWaitTime(Long.parseLong(cmd.getOptionValue(WAITTIME_OPT)));
		}
		if(cmd.hasOption(SPARQLENDPOINT_OPT)) {
			runConfig.setSparqlEndPoint(cmd.getOptionValue(SPARQLENDPOINT_OPT));
		}
		if(cmd.hasOption(SELECTORTYPE_OPT)) {
			String selectorType = cmd.getOptionValue(SELECTORTYPE_OPT);
			if(selectorType.equals("summary")) {
				runConfig.setUseAvatars(true);
			}
			runConfig.setSelectorType(selectorType);
		}
		
		return runConfig;
	}
	
	
	protected static boolean checkForHelp(String[] args) throws ParseException  { 
		boolean hasHelp = false;
		Options options = new Options();
		try {
			options.addOption(HELP_OPT);

			CommandLineParser parser = new DefaultParser();

			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption(HELP_OPT.getOpt())) {
				hasHelp = true;
			}

		}
		catch (ParseException e) {
			// do nothing
		}

		return hasHelp;
	  }
	
	
	protected static void process(BengalRunConfig runConfig) {
		String typeSubString = runConfig.getSelectorType();
		
		final String corpusName = "bengal_" + typeSubString + "_" + (runConfig.isUsePronouns() ? "pronoun_" : "")
				+ (runConfig.isUseSurfaceForms() ? "surface_" : "") + (runConfig.isUseParaphrasing() ? "para_" : "")
				+ Integer.toString(runConfig.getNumberOfDocs()) + ".ttl";
		BengalController.generateCorpus(runConfig, corpusName);
		// This is just to check whether the created documents make sense
		// If the entities have a bad positioning inside the documents the
		// parser should print warn messages
		final NIFParser parser = new TurtleNIFParser();
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(corpusName);
			parser.parseNIF(fin);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(fin);
		}
	}

	public static void generateCorpus(BengalRunConfig runConfig, final String corpusName) {
		String endpoint = runConfig.getSqparqlEndPoint();

		final Set<String> classes = new HashSet<>();
		classes.add("<http://dbpedia.org/ontology/Person>");
		classes.add("<http://dbpedia.org/ontology/Place>");
		classes.add("<http://dbpedia.org/ontology/Organisation>");

		// instantiate components;
		final TripleSelectorFactory factory = new TripleSelectorFactory();
		TripleSelector tripleSelector = null;
		BVerbalizer verbalizer = null;
		AvatarVerbalizer alernativeVerbalizer = null;
		if (runConfig.isUseAvatars()) {
			alernativeVerbalizer = AvatarVerbalizer.create(classes,
					runConfig.isUseOnlyObjectProps() ? classes : new HashSet<>(), endpoint, null, runConfig.getSeed(), false);
			if (alernativeVerbalizer == null) {
				return;
			}
		} else {
			tripleSelector = factory.create(runConfig.getSelectorTypeEnum(), classes,
					runConfig.isUseOnlyObjectProps() ? classes : new HashSet<>(), endpoint, null, runConfig.getMinSentence(), runConfig.getMaxSentence(),
					runConfig.getSeed());
			verbalizer = new SemWeb2NLVerbalizer(SparqlEndpoint.getEndpointDBpedia(), runConfig.isUsePronouns(), runConfig.isUseSurfaceForms());
		}
		Paraphraser paraphraser = null;
		if (runConfig.isUseParaphrasing()) {
			final ParaphraseService paraService = Paraphrasing.create();
			if (paraService != null) {
				paraphraser = new ParaphraserImpl(paraService);
			} else {
				LOGGER.error("Couldn't create paraphrasing service. Aborting.");
				return;
			}
		}

		// Get the number of documents from the parameters
		int numberOfDocuments = runConfig.getNumberOfDocs();
		List<Statement> triples;
		Document document = null;
		final List<Document> documents = new ArrayList<>();
		int counter = 1;
		while (documents.size() < numberOfDocuments) {
			if (runConfig.isUseAvatars()) {
				document = alernativeVerbalizer.nextDocument();
			} else {
				// select triples
				triples = tripleSelector.getNextStatements();
				if ((triples != null) && (triples.size() >= runConfig.getMinSentence())) {
					// create document
					document = verbalizer.generateDocument(triples);
					if (document != null) {
						final List<NumberOfVerbalizedTriples> tripleCounts = document
								.getMarkings(NumberOfVerbalizedTriples.class);
						if ((tripleCounts.size() > 0) && (tripleCounts.get(0).getNumberOfTriples() < runConfig.getMinSentence())) {
							LOGGER.error(
									"The generated document does not have enough verbalized triples. It will be discarded.");
							document = null;
						}
					}
					if (document != null) {
						// paraphrase document
						if (paraphraser != null) {
							try {
								document = paraphraser.getParaphrase(document);
							} catch (final Exception e) {
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
				if (!runConfig.isUseAvatars()) {
					Thread.sleep(runConfig.getWaitTime());
				}
			} catch (final InterruptedException e) {
			}
		}

		// generate file name and path from corpus name
		final String filePath = corpusName;
		// write the documents
		final NIFWriter writer = new TurtleNIFWriter();
		FileOutputStream fout = null;
		int i = 0;
		try {
			fout = new FileOutputStream(filePath);
			for (; i < documents.size(); ++i) {
				writer.writeNIF(documents.subList(i, i + 1), fout);
			}
			// writer.writeNIF(documents, fout);
		} catch (final Exception e) {
			System.out.println(documents.get(i));
			LOGGER.error("Error while writing the documents to file. Aborting.", e);
			System.out.println(documents.get(i));
		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (final Exception e) {
					// nothing to do
				}
			}
		}
	}
}
