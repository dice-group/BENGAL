/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.selector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;

/**
 *
 * @author ngonga
 */
public class SimpleSummarySelector extends AbstractSelector {

    Set<String> classes;
    String endpoint;
    String graph;
    List<Resource> resources;
    Random r = new Random(20);
    int minSize = 1;
    int maxSize = 5;
    
    public SimpleSummarySelector(Set<String> classes, String endpoint, String graph, int minSize, int maxSize) {
        this.classes = classes;
        this.endpoint = endpoint;
        this.graph = graph;
        resources = null;
        this.minSize = minSize;
        if(maxSize < minSize) maxSize = minSize + 1;
        this.maxSize = maxSize;                
    }
    
    public SimpleSummarySelector(Set<String> classes, String endpoint, String graph) {
        this.classes = classes;
        this.endpoint = endpoint;
        this.graph = graph;
        resources = null;
    }

    public List<Statement> getNextStatements() {
        if (resources == null) {
            resources = getResources(classes, endpoint, graph);
        }
        int counter = Math.abs(r.nextInt()%resources.size());
        //get symmetric CBD
        List<Statement> statements = getSummary(resources.get(counter));    
        
        //now pick random statements
        Set<Statement> result = new HashSet<>();
        int size = minSize + r.nextInt(maxSize - minSize + 1);
        while(result.size() < size)
        {
            counter = Math.abs(r.nextInt()%resources.size());
            result.add(statements.get(counter));
        }
        System.out.println(result);
        return sortStatementsByHash(result);
    }

    public List<Statement> getSummary(Resource r) {
        return getSymmetricCBD(r, classes, endpoint, graph);
    }

    public static void main(String args[]) {
        Set<String> classes = new HashSet<>();
        classes.add("<http://dbpedia.org/ontology/Person>");
        classes.add("<http://dbpedia.org/ontology/Place>");
        classes.add("<http://dbpedia.org/ontology/Organisation>");
        SimpleSummarySelector sss = new SimpleSummarySelector(classes, "http://dbpedia.org/sparql", null);
        sss.getNextStatements();
    }
}
