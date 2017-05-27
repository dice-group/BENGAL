/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
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
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.IntObjectOpenHashMap;

/**
 * Gets the CBD of a resource
 *
 * @author ngonga
 */
public abstract class AbstractSelector implements TripleSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSelector.class);

    private Set<String> targetClasses;
    private String endpoint;
    private String graph;
    private boolean useSymmetricCbd = false;

    public AbstractSelector(Set<String> targetClasses, String endpoint, String graph) {
        this.targetClasses = targetClasses;
        this.endpoint = endpoint;
        this.graph = graph;
    }

    public AbstractSelector(Set<String> targetClasses, String endpoint, String graph, boolean useSymmetricCbd) {
        this.useSymmetricCbd = useSymmetricCbd;
        this.targetClasses = targetClasses;
        this.endpoint = endpoint;
        this.graph = graph;
    }

    /**
     * Returns list of triples for a given resource and data source
     *
     * @param res
     * @param endpoint
     * @param graph
     * @return CBD of res
     */
    protected List<Statement> getSymmetricCBD(Resource res, Set<String> targetClasses, String endpoint, String graph) {
        Model m = ModelFactory.createDefaultModel();
        String sparqlQueryString = "";
        QueryExecution qexec = null;
        try {
            if (targetClasses != null) {
                if (targetClasses.isEmpty()) {
                    sparqlQueryString = "SELECT ?p ?o WHERE {<" + res
                            + "> ?p ?o. ?o <http://www.w3.org/2000/01/rdf-schema#label> [].}";
                    // sparqlQueryString = "SELECT ?p ?o WHERE {<" + res + "> ?p
                    // ?o.}";
                } else {
                    sparqlQueryString = "SELECT ?p ?o WHERE {";
                    sparqlQueryString = sparqlQueryString + " {<" + res
                            + "> ?p ?o. ?o  <http://www.w3.org/2000/01/rdf-schema#label> [].}";
                    // sparqlQueryString = sparqlQueryString + " {<" + res + ">
                    // ?p ?o.}";
                    for (String c : targetClasses) {
                        sparqlQueryString = sparqlQueryString + "{ ?o a <" + c
                                + ">. ?o  <http://www.w3.org/2000/01/rdf-schema#label> []. } UNION ";
                    }
                    sparqlQueryString = sparqlQueryString.substring(0, sparqlQueryString.length() - 6);
                    sparqlQueryString = sparqlQueryString + " }";
                }
            }

            QueryFactory.create(sparqlQueryString);
            if (graph != null) {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQueryString, graph);
            } else {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQueryString);
            }
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
                    // sparqlQueryString = "SELECT ?p ?o WHERE {<" + res + "> ?p
                    // ?o.}";
                } else {
                    sparqlQueryString = "SELECT ?p ?o WHERE {";
                    sparqlQueryString = sparqlQueryString + " {?o ?p <" + res
                            + ">. ?o  <http://www.w3.org/2000/01/rdf-schema#label> [].}";
                    // sparqlQueryString = sparqlQueryString + " {?o ?p <" + res
                    // + ">.}";
                    for (String c : targetClasses) {
                        sparqlQueryString = sparqlQueryString + "{ ?o a <" + c
                                + ">. ?o  <http://www.w3.org/2000/01/rdf-schema#label> []. } UNION ";
                    }
                    sparqlQueryString = sparqlQueryString.substring(0, sparqlQueryString.length() - 6);
                    sparqlQueryString = sparqlQueryString + " }";
                }
            }

            QueryFactory.create(sparqlQueryString);
            if (graph != null) {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQueryString, graph);
            } else {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQueryString);
            }
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
            qexec.close();
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
     * @param endpoint
     * @param graph
     * @return CBD of res
     */
    protected List<Statement> getCBD(Resource res, Set<String> targetClasses, String endpoint, String graph) {
        Model m = ModelFactory.createDefaultModel();
        String sparqlQueryString = "";
        QueryExecution qexec = null;
        try {
            if (targetClasses != null) {
                if (targetClasses.isEmpty()) {
                    sparqlQueryString = "SELECT ?p ?o WHERE {<" + res
                            + "> ?p ?o. ?o  <http://www.w3.org/2000/01/rdf-schema#label> [].}";
                    // sparqlQueryString = "SELECT ?p ?o WHERE {<" + res + "> ?p
                    // ?o.}";
                } else {
                    sparqlQueryString = "SELECT ?p ?o WHERE {";
                    sparqlQueryString = sparqlQueryString + " {<" + res
                            + "> ?p ?o. ?o  <http://www.w3.org/2000/01/rdf-schema#label> [].}";
                    // sparqlQueryString = sparqlQueryString + " {<" + res + ">
                    // ?p ?o.}";
                    for (String c : targetClasses) {
                        sparqlQueryString = sparqlQueryString + "{ ?o a <" + c
                                + ">. ?o  <http://www.w3.org/2000/01/rdf-schema#label> []. } UNION ";
                    }
                    sparqlQueryString = sparqlQueryString.substring(0, sparqlQueryString.length() - 6);
                    sparqlQueryString = sparqlQueryString + " }";
                }
            }

            QueryFactory.create(sparqlQueryString);
            if (graph != null) {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQueryString, graph);
            } else {
                qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQueryString);
            }
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
            qexec.close();
        }

        return sortStatements(m.listStatements());
    }

    /**
     * Sort statements by hash
     * 
     * @param stmtIterator
     *            Iterator which is used to get the statements
     * @return List of statements sorted by hash
     */
    @Deprecated
    protected List<Statement> sortStatementsByHash(StmtIterator stmtIterator) {
        IntObjectOpenHashMap<Statement> map = new IntObjectOpenHashMap<Statement>();
        Statement s;
        while (stmtIterator.hasNext()) {
            s = stmtIterator.next();
            map.put(s.hashCode(), s);
        }
        int keys[] = new int[map.assigned];
        int pos = 0;
        for (int i = 0; i < map.allocated.length; ++i) {
            if (map.allocated[i]) {
                keys[pos] = map.keys[i];
                ++pos;
            }
        }
        Arrays.sort(keys);
        List<Statement> result = new ArrayList<Statement>(keys.length);
        for (int i = 0; i < keys.length; ++i) {
            result.add(map.get(keys[i]));
        }
        return result;
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
     * Sort statements by hash
     * 
     * @param statements
     *            Set of statements
     * @return List of statements sorted by hash
     */
    @Deprecated
    protected List<Statement> sortStatementsByHash(Set<Statement> statements) {
        IntObjectOpenHashMap<Statement> map = new IntObjectOpenHashMap<Statement>(2 * statements.size());
        for (Statement s : statements) {
            map.put(s.hashCode(), s);
        }
        int keys[] = new int[map.assigned];
        int pos = 0;
        for (int i = 0; i < map.allocated.length; ++i) {
            if (map.allocated[i]) {
                keys[pos] = map.keys[i];
                ++pos;
            }
        }
        Arrays.sort(keys);
        List<Statement> result = new ArrayList<Statement>(keys.length);
        for (int i = 0; i < keys.length; ++i) {
            result.add(map.get(keys[i]));
        }
        return result;
    }

    /**
     * Get all resources that belong to the union of classes and sort them by
     * URI in desceding order
     *
     * @param classes
     *            Set of classes for resources
     * @param endpoint
     *            Endpoint from which the data is to be selected
     * @param graph
     *            Graph for the endpoint
     * @return Sorted list of resources from the classes
     */
    protected List<Resource> getResources(Set<String> classes, Map<String, Set<String>> resourceTypeMapping) {
        String query = "";
        List<Resource> result = new ArrayList<>();
        if (classes == null || classes.isEmpty()) {
            query = "SELECT ?s ?t WHERE {?s a ?t} ORDER BY DESC(?s)";
            Query sparqlQuery = QueryFactory.create(query, Syntax.syntaxARQ);

            QueryEngineHTTP httpQuery = new QueryEngineHTTP(endpoint, sparqlQuery);
            // execute a Select query
            try {
                ResultSet results = httpQuery.execSelect();
                QuerySolution solution;
                Resource r, t;
                Set<String> types;
                while (results.hasNext()) {
                    solution = results.next();
                    // get the value of the variables in the select clause
                    try {
                        r = solution.getResource("s");
                        result.add(r);
                        t = solution.getResource("t");
                        types = new HashSet<>();
                        types.add(t.getURI());
                        resourceTypeMapping.put(r.getURI(), types);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                httpQuery.close();
            }
        } else {
            for (String c : classes) {
                query = "SELECT ?s WHERE {?s a <" + c + ">. } ";
                Query sparqlQuery = QueryFactory.create(query, Syntax.syntaxARQ);

                QueryEngineHTTP httpQuery = new QueryEngineHTTP(endpoint, sparqlQuery);
                // execute a Select query
                try {
                    ResultSet results = httpQuery.execSelect();
                    QuerySolution solution;
                    Resource r;
                    Set<String> types;
                    while (results.hasNext()) {
                        solution = results.next();
                        // get the value of the variables in the select
                        // clause
                        try {
                            r = solution.getResource("s");
                            result.add(r);
                            if (resourceTypeMapping.containsKey(r.getURI())) {
                                types = resourceTypeMapping.get(r.getURI());
                            } else {
                                types = new HashSet<>();
                                resourceTypeMapping.put(r.getURI(), types);
                            }
                            types.add(c);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } finally {
                    httpQuery.close();
                }
            }
        }
        sortResources(result);
        return result;
    }

    @Deprecated
    protected void sortResourcesByHash(List<Resource> resources) {
        IntObjectOpenHashMap<Resource> map = new IntObjectOpenHashMap<Resource>(2 * resources.size());
        for (Resource r : resources) {
            map.put(r.hashCode(), r);
        }
        int keys[] = new int[map.assigned];
        int pos = 0;
        for (int i = 0; i < map.allocated.length; ++i) {
            if (map.allocated[i]) {
                keys[pos] = map.keys[i];
                ++pos;
            }
        }
        Arrays.sort(keys);
        resources.clear();
        for (int i = 0; i < keys.length; ++i) {
            resources.add(map.get(keys[i]));
        }
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
    protected List<Statement> getSummary(Resource r) {
        // one can use symmetric cbds here as well
        if (useSymmetricCbd) {
            return getSymmetricCBD(r, targetClasses, endpoint, graph);
        } else {
            return getCBD(r, targetClasses, endpoint, graph);
        }
    }
}
