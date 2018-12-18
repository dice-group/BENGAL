package org.aksw.simba.bengal.verbalizer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.simba.bengal.selector.ResourceComparator;
import org.aksw.simba.bengal.selector.StatementComparator;
import org.aksw.simba.bengal.triple2nl.converter.DefaultIRIConverter;
import org.aksw.simba.bengal.triple2nl.converter.IRIConverter;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.BitSet;

import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

public class AvatarVerbalizer implements Comparator<String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AvatarVerbalizer.class);

	/**
	 * The delay that the system will have between sending two queries.
	 */
	private static final int DELAY = 200;

	/**
	 * The lifetime of the cache.
	 */
	@SuppressWarnings("unused")
	private static final long CACHE_TIME_TO_LIVE = 31l * 24l * 60l * 60l * 1000l; // 1month

	private static final String CACHE_DIRECTORY = "./cache";

	public static AvatarVerbalizer create(Set<String> sourceClasses, Set<String> targetClasses, String endpoint,
			String graph, long seed, boolean useSymmetricCbd) {
		try {
			QueryExecutionFactory qef = initQueryExecution(endpoint, graph, CACHE_DIRECTORY);
			return new AvatarVerbalizer(sourceClasses, targetClasses, endpoint, graph, seed, useSymmetricCbd, qef);
		} catch (Exception e) {
			LOGGER.error("Couldn't create SPARQL query factory. Returning null.", e);
			return null;
		}
	}

	private Set<String> targetClasses;
	// private String endpoint;
	// private String graph;
	private boolean useSymmetricCbd = false;
	private Set<String> sourceClasses;
	private List<Resource> resources;
	private Random r;
	private IRIConverter uriConverter;
	protected Verbalizer verbalizer;
	protected QueryExecutionFactory qef;

	protected AvatarVerbalizer(Set<String> sourceClasses, Set<String> targetClasses, String endpoint, String graph,
			long seed, boolean useSymmetricCbd, QueryExecutionFactory qef) {
		this.useSymmetricCbd = useSymmetricCbd;
		this.targetClasses = targetClasses;
		// this.endpoint = endpoint;
		// this.graph = graph;
		this.sourceClasses = sourceClasses;
		resources = null;
		this.r = new Random(seed);
		verbalizer = new Verbalizer(SparqlEndpoint.getEndpointDBpedia(), CACHE_DIRECTORY);
		this.qef = qef;
		uriConverter = new DefaultIRIConverter(qef, CACHE_DIRECTORY);
	}

	/**
	 * Returns list of triples for a given resource and data source
	 *
	 * @param res
	 * @param targetClasses
	 * @return CBD of res
	 */
	protected List<Statement> getSymmetricCBD(Resource res, Set<String> targetClasses) {
		Model m = ModelFactory.createDefaultModel();
		String sparqlQueryString = "";
		QueryExecution qexec = null;
		try {
			if (targetClasses != null) {
				if (targetClasses.isEmpty()) {
					sparqlQueryString = "SELECT ?p ?o WHERE {<" + res
							+ "> ?p ?o. ?o <http://www.w3.org/2000/01/rdf-schema#label> []. }";
				} else {
					sparqlQueryString = "SELECT ?p ?o WHERE {";
					sparqlQueryString = sparqlQueryString + " {<" + res
							+ "> ?p ?o. ?o <http://www.w3.org/2000/01/rdf-schema#label> [].}";
					for (String c : targetClasses) {
						sparqlQueryString = sparqlQueryString + "{ ?o a " + c
								+ ". ?o <http://www.w3.org/2000/01/rdf-schema#label> [].} UNION ";
					}
					sparqlQueryString = sparqlQueryString.substring(0, sparqlQueryString.length() - 6);
					sparqlQueryString = sparqlQueryString + " }";
				}
			}

			qexec = qef.createQueryExecution(sparqlQueryString);
			ResultSet cbd = qexec.execSelect();
			while (cbd.hasNext()) {
				QuerySolution qs = cbd.nextSolution();
				Property p = m.createProperty(qs.get("p").asResource().getURI());
				if (qs.get("o").isLiteral()) {
					m.add(res, p, qs.getLiteral("o"));
				} else {
					m.add(res, p, qs.getResource("o"));
				}
			}
			qexec.close();

			if (targetClasses != null) {
				if (targetClasses.isEmpty()) {
					sparqlQueryString = "SELECT ?p ?o WHERE {<" + res
							+ "> ?p ?o. ?o <http://www.w3.org/2000/01/rdf-schema#label> [].}";
				} else {
					sparqlQueryString = "SELECT ?p ?o WHERE {";
					sparqlQueryString = sparqlQueryString + " {?o ?p <" + res
							+ ">. ?o <http://www.w3.org/2000/01/rdf-schema#label> [].}";
					for (String c : targetClasses) {
						sparqlQueryString = sparqlQueryString + "{ ?o a " + c
								+ ". ?o <http://www.w3.org/2000/01/rdf-schema#label> [].} UNION ";
					}
					sparqlQueryString = sparqlQueryString.substring(0, sparqlQueryString.length() - 6);
					sparqlQueryString = sparqlQueryString + " }";
				}
			}

			qexec = qef.createQueryExecution(sparqlQueryString);
			cbd = qexec.execSelect();
			while (cbd.hasNext()) {
				QuerySolution qs = cbd.nextSolution();
				Property p = m.createProperty(qs.get("p").asResource().getURI());

				m.add(qs.getResource("o"), p, res);
			}
		} catch (Exception e) {
			LOGGER.error("Exception while requesting CBD. Returning null.", e);
			return null;
		} finally {
			if (qexec != null) {
				qexec.close();
			}
		}

		// sort the statements
		// Map<Integer, Statement> map = new HashMap<>();
		// StmtIterator iter = m.listStatements();
		// while (iter.hasNext()) {
		// Statement s = iter.next();
		// map.put(s.hashCode(), s);
		// }
		// List<Integer> keys = new ArrayList<>(map.keySet());
		// Collections.sort(keys);
		// List<Statement> result = new ArrayList<>();
		// for (int k : keys) {
		// result.add(map.get(k));
		// }
		// return result;
		return sortStatements(m.listStatements());
	}

	/**
	 * Returns list of triples for a given resource and data source
	 *
	 * @param res
	 * @param targetClasses
	 * @return CBD of res
	 */
	protected List<Statement> getCBD(Resource res, Set<String> targetClasses) {
		Model m = ModelFactory.createDefaultModel();
		String sparqlQueryString = "";
		QueryExecution qexec = null;
		try {
			if (targetClasses != null) {
				if (targetClasses.isEmpty()) {
					sparqlQueryString = "SELECT ?p ?o WHERE {<" + res
							+ "> ?p ?o. ?o <http://www.w3.org/2000/01/rdf-schema#label> [].}";
				} else {
					sparqlQueryString = "SELECT ?p ?o WHERE {";
					sparqlQueryString = sparqlQueryString + " {<" + res
							+ "> ?p ?o. ?o <http://www.w3.org/2000/01/rdf-schema#label> [].}";
					for (String c : targetClasses) {
						sparqlQueryString = sparqlQueryString + "{ ?o a " + c
								+ ". ?o <http://www.w3.org/2000/01/rdf-schema#label> [].} UNION ";
					}
					sparqlQueryString = sparqlQueryString.substring(0, sparqlQueryString.length() - 6);
					sparqlQueryString = sparqlQueryString + " }";
				}
			}
			qexec = qef.createQueryExecution(sparqlQueryString);
			ResultSet cbd = qexec.execSelect();
			while (cbd.hasNext()) {
				QuerySolution qs = cbd.nextSolution();
				Property p = m.createProperty(qs.get("p").asResource().getURI());
				if (qs.get("o").isLiteral()) {
					m.add(res, p, qs.getLiteral("o"));
				} else {
					m.add(res, p, qs.getResource("o"));
				}
			}
		} catch (Exception e) {
			LOGGER.error("Exception while requesting CBD. Returning null.", e);
			return null;
		} finally {
			if (qexec != null) {
				qexec.close();
			}
		}

		return sortStatements(m.listStatements());
	}

	protected List<Statement> sortStatements(StmtIterator stmtIterator) {
		List<Statement> result = new ArrayList<Statement>();
		while (stmtIterator.hasNext()) {
			result.add(stmtIterator.next());
		}
		Collections.sort(result, new StatementComparator());
		return result;
	}

	protected List<Statement> sortStatements(Set<Statement> statements) {
		List<Statement> result = new ArrayList<Statement>(statements);
		Collections.sort(result, new StatementComparator());
		return result;
	}

	/**
	 * Get all resources that belong to the union of classes and sort them by
	 * URI in desceding order
	 *
	 * @param classes
	 *            Set of classes for resources
	 * @return Sorted list of resources from the classes
	 */
	protected List<Resource> getResources(Set<String> classes) {
		String query = "";
		if (classes != null) {
			if (classes.isEmpty()) {
				query = "SELECT ?s WHERE {?s a ?x.} ORDER BY DESC(?s)";
			} else {
				query = "SELECT ?x WHERE {";
				for (String c : classes) {
					query = query + "{ ?x a " + c + ". } UNION ";
				}
				query = query.substring(0, query.length() - 6);
				query = query + " {?x ?p ?y} ";
				for (String c : classes) {
					query = query + "{ ?y a " + c + ". } UNION ";
				}
				query = query.substring(0, query.length() - 6);
				query = query + " }";
			}
		}
		Query sparqlQuery = QueryFactory.create(query, Syntax.syntaxARQ);

		QueryExecution qexec = null;
		List<Resource> result = new ArrayList<>();
		// execute a Select query
		try {
			qexec = qef.createQueryExecution(sparqlQuery);
			ResultSet results = qexec.execSelect();
			QuerySolution solution;
			Resource r;
			while (results.hasNext()) {
				solution = results.next();
				// get the value of the variables in the select clause
				try {
					r = solution.getResource("x");
					result.add(r);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} finally {
			if (qexec != null) {
				qexec.close();
			}
		}
		sortResources(result);
		return result;
	}

	protected void sortResources(List<Resource> resources) {
		Collections.sort(resources, new ResourceComparator());
	}

	/**
	 * Gets a set of statements that summarize a resource r
	 * 
	 * @param r
	 *            A resource
	 * @return Summary (some CBD)
	 */
	protected List<Statement> getTriples(Resource r) {
		// one can use symmetric cbds here as well
		if (useSymmetricCbd) {
			return getSymmetricCBD(r, targetClasses);
		} else {
			return getCBD(r, targetClasses);
		}
	}

	public Document nextDocument() {
		if (resources == null) {
			resources = getResources(sourceClasses);
		}
		Document document = null;
		while (document == null) {
			int counter = Math.abs(r.nextInt() % resources.size());
			OWLIndividual ind = new OWLNamedIndividualImpl(IRI.create(resources.get(counter).getURI()));
			String text = verbalizer.summarize(ind);
			// get CBD
			List<Statement> statements = getTriples(resources.get(counter));
			// there was no error
			if (statements != null) {
				document = new DocumentImpl(text);
				Map<String, String> labelsToUris = getLabelsForResources(statements);
				String labels[] = labelsToUris.keySet().toArray(new String[labelsToUris.size()]);
				Arrays.sort(labels, this);
				// Go through the array of labels (starting with the
				// longest) and search for them inside the paraphrased text.
				// Make sure that the entities are not overlapping.
				BitSet blockedPositions = new BitSet(text.length());
				BitSet currentPositions = new BitSet(text.length());
				String label, uri;
				int pos;
				for (int i = 0; i < labels.length; ++i) {
					label = labels[i];
					uri = labelsToUris.get(label);
					pos = -label.length();
					do {
						// search the position in the text (make sure that
						// we start behind the position we might have found
						// before)
						pos = text.indexOf(label, pos + label.length());
						if (pos >= 0) {
							currentPositions.clear();
							// check that this part of the String does not
							// already have been blocked
							currentPositions.set(pos, pos + label.length());
							// If this position is not intersecting with another
							// entity
							if (BitSet.intersectionCount(blockedPositions, currentPositions) == 0) {
								document.addMarking(new NamedEntity(pos, label.length(), uri));
								blockedPositions.or(currentPositions);
							}
						}
					} while (pos >= 0);
				}
			}
		}
		return document;
	}

	private Map<String, String> getLabelsForResources(List<Statement> statements) {
		Map<String, String> labelsToUris = new HashMap<String, String>();
		String uri, label;
		for (Statement statement : statements) {
			uri = statement.getSubject().getURI();
			label = getEnglishLabel(uri);
			if (label != null) {
				labelsToUris.put(label, uri);
			}
			label = uriConverter.convert(uri);
			if (label != null) {
				labelsToUris.put(label, uri);
			}
			if (statement.getObject().isResource()) {
				// &&
				// !statement.getObject().asResource().getURI().contains("http://commons.wikimedia.org/wiki/"))
				// {
				uri = statement.getObject().asResource().getURI();
				label = getEnglishLabel(uri);
				if (label != null) {
					labelsToUris.put(label, uri);
				}
				label = uriConverter.convert(uri);
				if (label != null) {
					labelsToUris.put(label, uri);
				}
			}
		}
		return labelsToUris;
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
			ResultSet results = new SparqlQuery(labelQuery, SparqlEndpoint.getEndpointDBpedia()).send();

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

	/**
	 * Sorts Named entities descending by their length.
	 */
	@Override
	public int compare(String s1, String s2) {
		int diff = s1.length() - s2.length();
		if (diff < 0) {
			// n1 is shorter
			return 1;
		} else if (diff > 0) {
			return -1;
		} else {
			return 0;
		}
	}

	protected static QueryExecutionFactory initQueryExecution(String endpoint, String graph, String cacheDirectory)
			throws ClassNotFoundException, SQLException {
		QueryExecutionFactory qef;
		if (graph != null) {
			qef = new QueryExecutionFactoryHttp(endpoint, graph);
		} else {
			qef = new QueryExecutionFactoryHttp(endpoint);
		}

		qef = new QueryExecutionFactoryDelay(qef, DELAY);

		return qef;
	}

}
