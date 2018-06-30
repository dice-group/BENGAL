package org.aksw.simba.bengal.verbalizer;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.simba.bengal.utils.DocumentHelper;
import org.aksw.simba.bengal.verbalizer.VerbalizerSelectorFactory.SelectorLanguage;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplaceSubjectWithPronoun {
	
	private static final Logger logger = LoggerFactory.getLogger(ReplaceSubjectWithPronoun.class);
	
	public void replaceSubjectWithPronoun(final Document document, final String subjectUri, final SelectorLanguage language, final SparqlEndpoint endpoint) {
		final MeaningSpan marking = DocumentHelper.searchFirstOccurrence(subjectUri, document);
		if (marking == null) {
			return;
		}

		final String documentText = document.getText();
		String pronoun = null;

		final int start = marking.getStartPosition();
		int length = marking.getLength();
		final int end = start + length;
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

		switch (language) {
		case EN: {
		// Choose a pronoun based on the type of the entity
		final String type = getType(subjectUri, endpoint);
		if (type != null) {
			if (type.equals("http://dbpedia.org/ontology/Person")) {
				// Get the comment text
				final String commentString = getGender(subjectUri, endpoint);
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

		}
		case ES: {
			// Choose a pronoun based on the type of the entity
			String type = getType(subjectUri, endpoint);
			if (type != null) {
				if (type.equals("http://dbpedia.org/ontology/Person")) {
					// Get the comment text
					final String commentString = getGender(subjectUri, endpoint);
					// Search for a pronoun that identifies the person as man
					if (commentString.contains(" él ") || commentString.contains("Él ")
							|| commentString.contains(" su ")) {
						if (possessiveForm) {
							pronoun = (start == 0) ? "Su" : "su";
						} else {
							pronoun = (start == 0) ? "Él" : "él";
						}
						// Ok, than search for a woman
					} else if (commentString.contains(" ella ") || commentString.contains("Ella ")
							|| commentString.contains(" su ")) {
						if (possessiveForm) {
							pronoun = (start == 0) ? "Su" : "su";
						} else {
							pronoun = (start == 0) ? "Ella" : "ella";
						}
					}
					// If we can not decide the gender we shouldn't insert a pronoun
					// (let it be null)
				} else {
					if (possessiveForm) {
						pronoun = (start == 0) ? "Su" : "su";
					} else {
						pronoun = (start == 0) ? "Ello" : "ello";
					}
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
	}
	
	private String getType(final String uri, final SparqlEndpoint endpoint) {

		if (uri.equals(RDF.type.getURI())) {
			return "type";
		} else if (uri.equals(RDFS.label.getURI())) {
			return "label";
		}
		try {
			final String labelQuery = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
					+ " PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
					+ " PREFIX dbr: <http://es.dbpedia.org/resource/>" + " PREFIX dbo: <http://dbpedia.org/ontology/>"
					+ " PREFIX owl: <http://www.w3.org/2002/07/owl#>" + " SELECT  DISTINCT ?lcs WHERE {"
					+ " ?lcs ^rdf:type <" + uri + ">." + "?lcs rdfs:subClassOf ?x." + "?x rdfs:subClassOf owl:Thing."
					+ "?lcs rdfs:label []." + "}";

			// take care of graph issues. Only takes one graph. Seems like some
			// sparql endpoint do
			// not like the FROM option.
			final ResultSet results = new SparqlQuery(labelQuery, endpoint).send();

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
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String getGender(final String uri, final SparqlEndpoint endpoint) {

		if (uri.equals(RDF.type.getURI())) {
			return "type";
		} else if (uri.equals(RDFS.label.getURI())) {
			return "label";
		}
		try {
			final String labelQuery = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
					+ "PREFIX dbr: <http://es.dbpedia.org/resource/>" + "PREFIX dbo: <http://dbpedia.org/ontology/>"
					+ "PREFIX owl: <http://www.w3.org/2002/07/owl#>" + "SELECT  DISTINCT ?com WHERE {" + " <" + uri
					+ "> rdfs:comment ?com." + "FILTER (lang(?com) = 'es')." + "}";

			// take care of graph issues. Only takes one graph. Seems like some
			// sparql endpoint do
			// not like the FROM option.
			final ResultSet results = new SparqlQuery(labelQuery, endpoint).send();

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
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
