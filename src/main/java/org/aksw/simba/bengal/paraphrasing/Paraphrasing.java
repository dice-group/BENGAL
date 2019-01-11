/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.paraphrasing;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.aksw.simba.bengal.selector.SimpleSummarySelector;
import org.aksw.simba.bengal.selector.TripleSelector;
import org.aksw.simba.bengal.verbalizer.SemWeb2NLVerbalizer;
import org.apache.jena.rdf.model.Statement;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.BitSet;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import simplenlg.features.Feature;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;

/**
 *
 * @author DiegoMoussallem
 */
public class Paraphrasing implements ParaphraseService, Comparator<NamedEntity> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Paraphrasing.class);

	// loads config file
	static String file = "/config/bengal.properties";
	public static Properties prop = new Properties();
	static {
		try {
			prop.load(Paraphrasing.class.getResourceAsStream(file));
		} catch (final IOException e) {
			LOGGER.warn("Couldn't load config file".concat(file));
		}
	}

	public static Paraphrasing create() {
		Paraphrasing service = null;
		service = new Paraphrasing();
		return service;
	}

	protected Paraphrasing() {
	}

	@Override
	public String paraphrase(String originalText, String dictPath) {

		try {
			Properties prop = new Properties();
			InputStream input = Paraphrasing.class.getResourceAsStream("/config/bengal.properties");
			prop.load(input);

			// String wordnetPath = prop.getProperty("dict");
			String wordnetPath = dictPath;
			// System.out.println("Original text: " + originalText);
			Lexicon lexicon = Lexicon.getDefaultLexicon();
			NLGFactory nlgFactory = new NLGFactory(lexicon);
			Realiser realiser = new Realiser(lexicon);
			MaxentTagger tagger = new MaxentTagger(
					"edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger");
			SPhraseSpec s = nlgFactory.createClause();

			String[] sentences = originalText.split(Pattern.quote(". "));

			for (int k = 0; k < sentences.length; k++) {
				sentences[k] = sentences[k].replace(".", "");
				// System.out.println(sentences[k]);
			}

			int vSentences = 0;
			for (int i = 0; i < sentences.length; i++) {

				try {
					vSentences = vSentences + 1;
					String[] Phrase = new String[100];
					// String[] Phrase = sentences[i].split("[,; ]+");
					Phrase = sentences[i].split("\\s+");
					// String[] Pos = sentences[i].split("[,; ]+");
					String[] Pos = sentences[i].split("\\s+");

					for (int k = 0; k < Pos.length; k++) {
						Pos[k] = tagger.tagString(Pos[k]);
					}

					int value = 0;
					int value2 = 0;
					for (int k = 0; k < Pos.length; k++) {
						if (Pos[k].contains("_V") && value == 0) {
							value = k;
						}
						if (Pos[k].contains("_NN ") && value2 == 0) {
							value2 = k;
						}
					}

					if (value2 > 0) {
						Phrase[value2] = synWN(Phrase[value2], Pos[value2], wordnetPath);
					}
					s.setVerb(Phrase[value]);

					String inf_verb = s.getVerb().toString().replace("WordElement[", "").replace(":VERB]", "");

					if (!inf_verb.equals("be")) {
						s.setVerb(synWN(s.getVerb().toString().replace("WordElement[", "").replace(":VERB]", ""),
								Pos[value], wordnetPath));
					}

					StringBuilder sb = new StringBuilder();
					if (Phrase.length > 1) {
						sb.append(Phrase[0]);
						for (int w = 1; w < value; w++) {
							sb.append(" ").append(Phrase[w]);
						}
					}

					s.setSubject(sb.toString());

					StringBuilder sc = new StringBuilder();
					if (Phrase.length > 1) {
						sc.append(Phrase[value + 1]);
						for (int z = value + 2; z < Phrase.length; z++) {
							sc.append(" ").append(Phrase[z]);
						}
					}

					s.setObject(sc.toString());

					if (!inf_verb.equals("be")) {
						s.setFeature(Feature.PERFECT, true);
					}

					if (!Pos[value].equals(Pos.length) && !Pos[value + 1].contains("JJ")) {
						s.setFeature(Feature.PASSIVE, true);
					}
					String output = realiser.realiseSentence(s); // + " .";
					String[] paraphraser = output.split("\\s+");
					int find = 0;
					int found = 0;
					StringBuilder end = new StringBuilder();
					if (paraphraser.length > 1) {
						end.append(paraphraser[0]);
						for (int z = 1; z < paraphraser.length; z++) {
							if (z == found) {
								continue;
							}
							if (paraphraser[z].equals("been")) {
								find++;
								if (inf_verb.equals("be") && find > 0) {
									continue;
								} else if ((!inf_verb.equals("be") && find > 1)) {
									continue;
								}
							}
							if (paraphraser[z].equals("by")) {
								continue;
							}
							end.append(" ").append(paraphraser[z]);
						}
					}
					output = end.toString();
					sentences[i] = output;
					value = 0;
				} catch (Exception e) {
					System.out.println("Paraphrasing failed, error" + e);
				}
			}

			StringBuilder paraphrased = new StringBuilder();
			if (sentences.length >= 1) {
				paraphrased.append(sentences[0]);
				for (int z = 1; z < sentences.length; z++) {
					paraphrased.append(" ").append(sentences[z]);
				}
			}
			String output = paraphrased.toString();
			System.out.println("Paraphrased text:  " + output);
			return output;
		} catch (Exception e) {
			LOGGER.error("Exception from NLG. Returning null.", e);
			return originalText;
		}
	}

	public String synWN(String word, String tag, String wordnetPath) {

		System.setProperty("wordnet.database.dir", wordnetPath);
		// setting path for the WordNet Directory
		// System.out.println("word: " + word);
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		Synset[] synsets = null;
		if (tag.contains("_V")) {
			synsets = database.getSynsets(word, SynsetType.VERB);
		} else if (tag.contains("_NN")) {
			synsets = database.getSynsets(word, SynsetType.NOUN);
		}
		// Display the word forms and definitions for synsets retrieved
		String synonym = "";
		if (synsets.length > 0) {
			ArrayList<String> al = new ArrayList<String>();
			// add elements to al, including duplicates
			HashSet<String> hs = new HashSet<String>();
			for (int i = 0; i < synsets.length; i++) {
				String[] wordForms = synsets[i].getWordForms();
				// System.out.println(wordForms.length);
				// synonym = wordForms[0].toString();
				for (int j = 0; j < wordForms.length; j++) {
					al.add(wordForms[j]);
				}
			}
			// removing duplicates
			hs.addAll(al);
			al.clear();
			al.addAll(hs);

			// showing all synsets
			// System.out.println("Quantity of synonyms " + al.size());
			for (int j = 0; j < al.size(); j++) {
				// System.out.println(al.get(j));
				if (!al.get(j).equals(word)) {
					synonym = al.get(j);
					break;
				}
			}
			if (synonym.equals("")) {
				return word;
			} else {
				return synonym;
			}
		} else {
			// System.err.println("No synsets exist that contain the word form
			// '" + word + "'");
			return word;
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

	public static void main(String args[]) throws Exception {

		Set<String> classes = new HashSet<>();
		classes.add("<http://dbpedia.org/ontology/Person>");
		classes.add("<http://dbpedia.org/ontology/Place>");
		classes.add("<http://dbpedia.org/ontology/Organisation>");
		TripleSelector ts = new SimpleSummarySelector(classes, classes, "http://dbpedia.org/sparql", null);
		List<Statement> stmts = ts.getNextStatements();
		Document doc = new SemWeb2NLVerbalizer(SparqlEndpoint.getEndpointDBpedia()).generateDocument(stmts, prop.getProperty("surfaceForms"));

		Paraphrasing service = new Paraphrasing();

		String text = doc.getText();
		String paraphrases = service.paraphrase(text, prop.getProperty("dict"));

		Document newDoc = new DocumentImpl(paraphrases, doc.getDocumentURI());

		// find all named entities inside the new text
		// first sort them descending by their length
		List<NamedEntity> originalNes = doc.getMarkings(NamedEntity.class);
		Collections.sort(originalNes, service);
		// Go through the list of named entities (starting with the longest) and
		// search for them inside the paraphrased text. Make sure that the
		// entities are not overlapping.
		BitSet blockedPositions = new BitSet(paraphrases.length());
		BitSet currentPositions = new BitSet(paraphrases.length());
		String label;
		int pos;
		for (NamedEntity ne : originalNes) {
			label = text.substring(ne.getStartPosition(), ne.getStartPosition() + ne.getLength());
			pos = -ne.getLength();
			do {
				// search the position in the new text (make sure that we start
				// behind the position we might have found before)
				pos = paraphrases.indexOf(label, pos + ne.getLength());
				if (pos < 0) {
					// the position search failed
					LOGGER.warn(
							"The paraphrasing changed one of the entities. Couldn't find the surface form \"{}\" in the text \"{}\". Returning the original document.",
							label, paraphrases);
				}
				currentPositions.clear();
				// check that this part of the String does not already have been
				// blocked
				try {
					currentPositions.set(pos, pos + ne.getLength());
				} catch (Exception e) {

				}

			} while (BitSet.intersectionCount(blockedPositions, currentPositions) > 0);
			// Update the position in the new text
			newDoc.addMarking(new NamedEntity(pos, ne.getLength(), ne.getUris()));
			blockedPositions.or(currentPositions);
		}

		List<Document> documents = new ArrayList<>();
		// If the generation and paraphrasing were successful
		if (newDoc != null) {
			LOGGER.info("Created document #" + 1);
			newDoc.setDocumentURI("http://aksw.org/generated/" + 1);
			documents.add(newDoc);
		}

		// generate file name and path from corpus name
		String filePath = "Pharaphrasetest.ttl";
		// write the documents
		NIFWriter writer = new TurtleNIFWriter();
		FileOutputStream fout = null;
		int i = 0;
		try {
			fout = new FileOutputStream(filePath);
			writer.writeNIF(documents.subList(i, i + 1), fout);
			// writer.writeNIF(documents, fout);
		} catch (Exception e) {
			// System.out.println(documents.get(i));
			LOGGER.error("Error while writing the documents to file. Aborting.", e);
			// System.out.println(documents.get(i));
		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (Exception e) {
					// nothing to do
				}
			}
		}
		System.out.println("Doc" + doc.toString());
		System.out.println("Paraphrased doc" + newDoc.toString());
	}
}