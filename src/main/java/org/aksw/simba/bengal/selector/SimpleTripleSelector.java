/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.selector;

import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Implements a triple selector that describes a sinle entity in one sentence.
 * @author ngonga
 */
@Deprecated
public class SimpleTripleSelector implements TripleSelector{

    int benchmarkSize;
    int minNumberOfTriples;
    int maxNumberOfTriples;
    double starCoefficient;
    
    public SimpleTripleSelector(String endpoint, Set<String> classes)
    {
        benchmarkSize = 10;
        minNumberOfTriples = 1;
        maxNumberOfTriples = 3;
        starCoefficient = 0.5d;
    }
    
    public List<Statement> getNextStatements() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
