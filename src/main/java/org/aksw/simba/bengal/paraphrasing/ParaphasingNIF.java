package org.aksw.simba.bengal.paraphrasing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.aksw.gerbil.io.nif.NIFParser;
import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFParser;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.aksw.simba.bengal.utils.DocumentHelper;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.BitSet;

public class ParaphasingNIF {

	private static final Logger LOGGER = LoggerFactory.getLogger(ParaphasingNIF.class);
	// private static final String FILENAME =
	// "/Users/diegomoussallem/Desktop/BENGAL-master/bengal_datasets/B1_bengal_path_100.ttl";
	// private static final String FILENAME =
	// "/Users/diegomoussallem/Desktop/BENGAL-master/bengal_datasets/B12_bengal_hybrid_object_10.ttl";
	private static final String FILENAME = "/Users/diegomoussallem/Desktop/BENGAL-master/bengal_path_1.ttl";

	public void replaceSubjectWithPronoun(Document document, String subjectUri) {
		MeaningSpan marking = DocumentHelper.searchFirstOccurrence(subjectUri, document);
		if (marking == null) {
			return;
		}

		String documentText = document.getText();
		String pronoun = null;

		int start = marking.getStartPosition();
		int length = marking.getLength();
		int end = start + length;
		// FIXME check whether the entity is preceded by an article

		// Check whether we have to add a possessive pronoun (check whether it
		// has a trailing "'s")
		boolean possessiveForm = false;
		if ((documentText.charAt(end - 2) == '\'') && (documentText.charAt(end - 1) == 's')) {
			possessiveForm = true;
		} else if (((end + 1) < documentText.length()) && (documentText.charAt(end) == '\'')) {
			possessiveForm = true;
			// Check whether we have "<entity>'" or "<entity>'s"
			if (documentText.charAt(end + 1) == 's') {
				length += 2;
			} else {
				length += 1;
			}
		}

		// Choose a pronoun based on the type of the entity
		String type = getType(subjectUri);
		if (type != null) {
			if (type.equals("http://dbpedia.org/ontology/Person")) {
				// Get the comment text
				String commentString = getGender(subjectUri);
				// Search for a pronoun that identifies the person as man
				if (commentString.contains(" he ") || commentString.contains("He ")
						|| commentString.contains(" his ")) {
					if (possessiveForm) {
						pronoun = (start == 0) ? "His" : "his";
					} else {
						pronoun = (start == 0) ? "He" : "he";
					}
					// Ok, than search for a woman
				} else if (commentString.contains(" she ") || commentString.contains("She ")
						|| commentString.contains(" her ")) {
					if (possessiveForm) {
						pronoun = (start == 0) ? "Her" : "her";
					} else {
						pronoun = (start == 0) ? "She" : "she";
					}
				}
				// If we can not decide the gender we shouldn't insert a pronoun
				// (let it be null)
			} else {
				if (possessiveForm) {
					pronoun = (start == 0) ? "Its" : "its";
				} else {
					pronoun = (start == 0) ? "It" : "it";
				}
			}
		}

		// If we couldn't find a pronoun
		if (pronoun == null) {
			return;
		}

		// Remove the marking from the document
		document.getMarkings().remove(marking);
		// Replace the text
		DocumentHelper.replaceText(document, start, length, pronoun);
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
					+ " PREFIX dbr: <http://dbpedia.org/resource/>" + " PREFIX dbo: <http://dbpedia.org/ontology/>"
					+ " PREFIX owl: <http://www.w3.org/2002/07/owl#>" + " SELECT  DISTINCT ?lcs WHERE {"
					+ " ?lcs ^rdf:type <" + uri + ">." + "?lcs rdfs:subClassOf ?x." + "?x rdfs:subClassOf owl:Thing."
					+ "?lcs rdfs:label []." + "}";

			// take care of graph issues. Only takes one graph. Seems like some
			// sparql endpoint do
			// not like the FROM option.
			ResultSet results = new SparqlQuery(labelQuery, SparqlEndpoint.getEndpointDBpedia()).send();

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
					+ "PREFIX dbr: <http://dbpedia.org/resource/>" + "PREFIX dbo: <http://dbpedia.org/ontology/>"
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>" + "SELECT  DISTINCT ?com WHERE {" + " <" + uri
					+ "> rdfs:comment ?com." + "FILTER (lang(?com) = 'en')." + "}";

			// take care of graph issues. Only takes one graph. Seems like some
			// sparql endpoint do
			// not like the FROM option.
			ResultSet results = new SparqlQuery(labelQuery, SparqlEndpoint.getEndpointDBpedia()).send();

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

	public static Document replaceSubjectWithSurfaceForms(Document document) throws IOException {

		Document newDoc = null;
		Paraphrasing service = new Paraphrasing();
		List<NamedEntity> originalNes = document.getMarkings(NamedEntity.class);
		Collections.sort(originalNes, service);
		String text = document.getText();
		// String textSF = document.getText();

		List<MeaningSpan> oldEntities = document.getMarkings(MeaningSpan.class);
		Map<MeaningSpan, String> entities = new HashMap<MeaningSpan, String>();

		for (MeaningSpan meaning : oldEntities) {
			entities.put(meaning,
					text.substring(meaning.getStartPosition(), meaning.getStartPosition() + meaning.getLength()));
		}

		// Document newDoc = new DocumentImpl(textSF,
		// document.getDocumentURI());
		// Go through the list of named entities (starting with the longest)
		// and
		// search for them inside the paraphrased text. Make sure that the
		// entities are not overlapping.
		BitSet blockedPositions = new BitSet(text.length());
		// BitSet currentPositions = new BitSet(textSF.length());
		String label;
		String uri;
		int pos;
		int count = 0;
		for (NamedEntity ne : originalNes) {
			uri = ne.getUri().toString();
			label = text.substring(ne.getStartPosition(), ne.getStartPosition() + ne.getLength());
			pos = -ne.getLength();
			do {
				// search the position in the new text (make sure that we
				// start
				// behind the position we might have found before)
				pos = text.indexOf(label, pos + ne.getLength());
				if (pos < 0) {
					// the position search failed
					LOGGER.warn(
							"The SufaceForm replacement changed one of the entities. Couldn't find the surface form \"{}\" in the text \"{}\". Returning the original document.",
							label, text);
					// newDoc = null;
					break;
				} else if (pos > 0 && count == 1) {
					String newLabel = getSurfaceForm(uri);
					if (newLabel.equals("")) {
						// newDoc = null;
						break;
					} else {
						// System.out.println("newLabel: " + newLabel + " label:
						// "+ label);
					}
					String textSF = document.getText().replace(label, newLabel);
					newDoc = new DocumentImpl(textSF, document.getDocumentURI());
					BitSet currentPositions = new BitSet(textSF.length());
					// System.out.println("Old Text" + textSF);
					textSF = textSF.replace(label, newLabel);
					newDoc.setText(textSF);
					// System.out.println("new Text" + newDoc.getText());
					int newPos = textSF.indexOf(newLabel, ne.getStartPosition());
					if (pos >= 0) {
						currentPositions.clear();
						// check that this part of the String does not
						// already have been blocked
						currentPositions.set(pos, pos + label.length());
						// If this position is not intersecting with another
						// entity
						if (BitSet.intersectionCount(blockedPositions, currentPositions) == 0) {
							newDoc.addMarking(new NamedEntity(newPos, newLabel.length(), uri));
							blockedPositions.or(currentPositions);
							break;
						}
					}
					count = 0;
					break;
				} else {
					// currentPositions.clear();
					// check that this part of the String does not already
					// have
					// been
					// blocked
					// currentPositions.set(pos, pos + ne.getLength());
					count = 1;
				}
				// } while (BitSet.intersectionCount(blockedPositions,
				// currentPositions) > 0);
			} while (pos >= 0);

			// if (newDoc != null) {
			// Update the position in the new text
			// newDoc.addMarking(new NamedEntity(pos, ne.getLength(),
			// ne.getUris()));
			// blockedPositions.or(currentPositions);
			// }
		}
		/*
		 * int position = 0; Iterator<Entry<MeaningSpan, String>> it =
		 * entities.entrySet().iterator(); while (it.hasNext()) {
		 * 
		 * @SuppressWarnings("rawtypes") Map.Entry pair = (Map.Entry) it.next();
		 * MeaningSpan span = (MeaningSpan) pair.getKey(); String value =
		 * pair.getValue().toString(); position =
		 * newDoc.getText().indexOf(value); if (position != -1) {
		 * newDoc.addMarking(new NamedEntity(position, value.length(),
		 * span.getUri())); } it.remove(); // avoids a
		 * ConcurrentModificationException }
		 */
		// System.out.println(document.toString());
		// System.out.println(newDoc.toString());
		return newDoc;
	}

	public static String getSurfaceForm(String resource) throws IOException {
		try {
			Properties prop = new Properties();
			InputStream input = new FileInputStream(
					"/Users/diegomoussallem/Desktop/BENGAL-master/src/main/resources/config/bengal.properties");
			prop.load(input);

			String surfaceFormTSV = prop.getProperty("surfaceForms");
			// LOGGER.info("Getting surface forms from: " + surfaceFormTSV);
			File file = new File(surfaceFormTSV);
			String label = "";
			// LOGGER.info("Start parsing: " + file);
			BufferedReader br = new BufferedReader(new FileReader(file));
			while (br.ready()) {
				String[] line = br.readLine().split("\t");
				String subject = line[0];
				String object = line[1];
				if (subject.equals(resource)) {
					label = object;
					break;
				}
			}
			br.close();
			return label;
		} catch (Exception e) {
			// LOGGER.error("Got exception from SurfaceFormChanger. Using the
			// original document.", e);
			return "";
		}

	}

	public static Document paraphrasingNIF(Document doc) throws IOException {

		Paraphrasing service = new Paraphrasing();
		String text = doc.getText();
		System.out.println(text);
		String paraphrases = service.paraphrase(text);

		Document newDoc = new DocumentImpl(paraphrases, doc.getDocumentURI());

		// find all named entities inside the new text
		// first sort them descending by their length
		List<NamedEntity> originalNes = doc.getMarkings(NamedEntity.class);
		Collections.sort(originalNes, service);
		// Go through the list of named entities (starting with the longest)
		// and
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
				// search the position in the new text (make sure that we
				// start
				// behind the position we might have found before)
				pos = paraphrases.indexOf(label, pos + ne.getLength());
				if (pos < 0) {
					// the position search failed
					LOGGER.warn(
							"The paraphrasing changed one of the entities. Couldn't find the surface form \"{}\" in the text \"{}\". Returning the original document.",
							label, paraphrases);
					newDoc = null;
					break;
				} else {
					currentPositions.clear();
					// check that this part of the String does not already
					// have
					// been
					// blocked
					currentPositions.set(pos, pos + ne.getLength());
				}
			} while (BitSet.intersectionCount(blockedPositions, currentPositions) > 0);

			if (newDoc != null) {
				// Update the position in the new text
				newDoc.addMarking(new NamedEntity(pos, ne.getLength(), ne.getUris()));
				blockedPositions.or(currentPositions);
			}
		}

		return newDoc;

	}

	public static void main(String args[]) throws Exception {

		FileInputStream nif = new FileInputStream(FILENAME);
		NIFParser parser = new TurtleNIFParser();
		Iterator<Document> iterator = parser.parseNIF(nif).listIterator();
		List<Document> documents = new ArrayList<Document>();
		List<Document> documents2 = new ArrayList<>();
		List<Document> documents3 = new ArrayList<>();

		while (iterator.hasNext()) {
			documents.add(iterator.next());
		}
		System.out.println(documents.size());

		int count = 0;
		for (Document doc : documents) {

			Document newDoc = paraphrasingNIF(doc);
			count++;
			// If the generation and paraphrasing were successful
			if (newDoc != null) {
				LOGGER.info("Created document #" + count);
				newDoc.setDocumentURI("http://aksw.org/generated/" + count);
				documents2.add(newDoc);
			} else {
				LOGGER.info("Created document without paraphrasing #" + count);
				doc.setDocumentURI("http://aksw.org/generated/" + count);
				documents2.add(doc);
			}
		}

		//for (Document doc : documents2) {
		//	Document newDoc = replaceSubjectWithSurfaceForms(doc);
		//	LOGGER.info("Created document without paraphrasing #" + count);
		//	newDoc.setDocumentURI("http://aksw.org/generated/" + count);
		//	documents3.add(newDoc);
		//}

		// generate file name and path from corpus name
		String filePath = "B12_bengal_hybrid_object_10_paraphrased.ttl";
		// write the documents
		NIFWriter writer = new TurtleNIFWriter();
		FileOutputStream fout = null;
		int i = 0;
		try {
			fout = new FileOutputStream(filePath);
			for (; i < documents3.size(); ++i) {
				writer.writeNIF(documents3.subList(i, i + 1), fout);
			}
			// writer.writeNIF(documents, fout);
		} catch (Exception e) {
			System.out.println(documents3.get(i));
			LOGGER.error("Error while writing the documents to file. Aborting.", e);
			System.out.println(documents3.get(i));
		} finally {
			if (fout != null) {
				try {
					fout.close();
				} catch (Exception e) {
					// nothing to do
				}
			}
		}
		System.out.println("Finished");
	}

}
