/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.selector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

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
                    sparqlQueryString = "SELECT ?p ?o WHERE {<" + res + "> ?p ?o} ORDER BY DESC(?s)";
                } else {
                    sparqlQueryString = "SELECT ?p ?o WHERE {";
                    sparqlQueryString = sparqlQueryString + " {<" + res + "> ?p ?o}";
                    for (String c : targetClasses) {
                        sparqlQueryString = sparqlQueryString + "{ ?o a " + c + ". } UNION ";
                    }
                    sparqlQueryString = sparqlQueryString.substring(0, sparqlQueryString.length() - 6);
                    sparqlQueryString = sparqlQueryString + " }";
                }
            }

            System.out.println(sparqlQueryString);
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
                    sparqlQueryString = "SELECT ?p ?o WHERE {<" + res + "> ?p ?o} ORDER BY DESC(?s)";
                } else {
                    sparqlQueryString = "SELECT ?p ?o WHERE {";
                    sparqlQueryString = sparqlQueryString + " {?o ?p <" + res + ">}";
                    for (String c : targetClasses) {
                        sparqlQueryString = sparqlQueryString + "{ ?o a " + c + ". } UNION ";
                    }
                    sparqlQueryString = sparqlQueryString.substring(0, sparqlQueryString.length() - 6);
                    sparqlQueryString = sparqlQueryString + " }";
                }
            }

            System.out.println(sparqlQueryString);
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
        Map<Integer, Statement> map = new HashMap<>();
        StmtIterator iter = m.listStatements();
        while (iter.hasNext()) {
            Statement s = iter.next();
            map.put(s.hashCode(), s);
        }
        List<Integer> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        List<Statement> result = new ArrayList<>();
        for (int k : keys) {
            result.add(map.get(k));
        }
        return result;
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
                    sparqlQueryString = "SELECT ?p ?o WHERE {<" + res + "> ?p ?o} ORDER BY DESC(?s)";
                } else {
                    sparqlQueryString = "SELECT ?p ?o WHERE {";
                    sparqlQueryString = sparqlQueryString + " {<" + res + "> ?p ?o}";
                    for (String c : targetClasses) {
                        sparqlQueryString = sparqlQueryString + "{ ?o a " + c + ". } UNION ";
                    }
                    sparqlQueryString = sparqlQueryString.substring(0, sparqlQueryString.length() - 6);
                    sparqlQueryString = sparqlQueryString + " }";
                }
            }

            System.out.println(sparqlQueryString);
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

        return sortStatementsByHash(m.listStatements());
    }

    /**
     * Sort statements by hash
     * 
     * @param stmtIterator
     *            Iterator which is used to get the statements
     * @return List of statements sorted by hash
     */
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

    /**
     * Sort statements by hash
     * 
     * @param statements
     *            Set of statements
     * @return List of statements sorted by hash
     */
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
    protected List<Resource> getResources(Set<String> classes) {
        String query = "";
        if (classes != null) {
            if (classes.isEmpty()) {
                query = "SELECT ?s WHERE {?s a ?x} ORDER BY DESC(?s)";
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
        System.out.println(query);
        Query sparqlQuery = QueryFactory.create(query, Syntax.syntaxARQ);

        QueryEngineHTTP httpQuery = new QueryEngineHTTP(endpoint, sparqlQuery);
        List<Resource> result = new ArrayList<>();
        // execute a Select query
        try {
            ResultSet results = httpQuery.execSelect();
            while (results.hasNext()) {
                QuerySolution solution = results.next();
                System.out.println(solution);
                // get the value of the variables in the select clause
                try {
                    Resource r = solution.getResource("x");
                    result.add(r);
                    System.out.println(r);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            httpQuery.close();
        }
        sortResourcesByHash(result);
        return result;
    }

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
