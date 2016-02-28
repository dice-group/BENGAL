/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

/**
 * Gets the CBD of a resource
 *
 * @author ngonga
 */
public abstract class AbstractSelector implements TripleSelector {

    /**
     * Returns list of triples for a given resource and data source
     *
     * @param res
     * @param endpoint
     * @param graph
     * @return CBD of res
     */
    List<Statement> getCBD(Resource res, String endpoint, String graph) {
        List<Statement> statements = new ArrayList<>();
        Model m = ModelFactory.createDefaultModel();
        try {
            String sparqlQueryString = "SELECT ?p ?o WHERE { <" + res + "> ?p ?o. FILTER isBlank(?o) }";
            QueryFactory.create(sparqlQueryString);
            QueryExecution qexec = QueryExecutionFactory.sparqlService(
                    endpoint, sparqlQueryString, graph);
            ResultSet cbd = qexec.execSelect();
            qexec.close();
            while (cbd.hasNext()) {
                QuerySolution qs = cbd.nextSolution();
//                RDFNode p =  qs.get("p");
//                m.add(res, , qs.get("o"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<Integer, Statement> map = new HashMap<>();
        for (Statement s : statements) {
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

    public List<Resource> getResources(Set<String> classes, String endpoint) {
        String query = "";
        if (classes != null) {
            if (classes.isEmpty()) {
                query = "SELECT ?s WHERE {?s a ?x} ORDER BY DESC(?s)";
            } else {
                query = "SELECT ?s WHERE {";
                for (String c : classes) {
                    query = query + "{ ?x a " + c + ". } UNION ";
                }
                query = query.substring(0, query.length() - 6);
                query = query + " } LIMIT 10";
            }
        }
        System.out.println(query);
        // create the Jena query using the ARQ syntax (has additional support for SPARQL federated queries)
        Query sparqlQuery = QueryFactory.create(query, Syntax.syntaxARQ);
    // we want to bind the ?uniprotAccession variable in the query
        // to the URI for Q16850 which is http://purl.uniprot.org/uniprot/Q16850

        QueryEngineHTTP httpQuery = new QueryEngineHTTP(endpoint, sparqlQuery);
        // execute a Select query
        ResultSet results = httpQuery.execSelect();
        List<Resource> result = new ArrayList<>();
        while (results.hasNext()) {
            QuerySolution solution = results.next();
            System.out.println(solution);
            // get the value of the variables in the select clause
            try
            {
                Resource r = solution.getResource("x");
                result.add(r);
                System.out.println(r);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        return result;
    }
}