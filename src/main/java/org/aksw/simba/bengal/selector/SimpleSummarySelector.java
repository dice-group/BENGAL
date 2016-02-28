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

/**
 *
 * @author ngonga
 */
public class SimpleSummarySelector extends AbstractSelector {

    Set<String> classes;
    String endpoint;
    List<Resource> resources;
    int counter = 0;

    public SimpleSummarySelector(Set<String> classes, String endpoint) {
        this.classes = classes;
        this.endpoint = endpoint;
        resources = null;
    }

    public List<Statement> getNextStatements() {
        if (resources == null) {
            resources = getResources(classes, endpoint);
        }
        List<Statement> result = getSummary(resources.get(counter % resources.size()));
        counter++;
        return result;
    }

    public List<Statement> getSummary(Resource r) {
        return null;
    }

    public static void main(String args[]) {
        Set<String> classes = new HashSet<>();
        classes.add("<http://dbpedia.org/ontology/Person>");
        SimpleSummarySelector sss = new SimpleSummarySelector(classes, "http://dbpedia.org/sparql");
        sss.getNextStatements();
    }
}
