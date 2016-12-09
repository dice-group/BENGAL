/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.selector;

import java.util.List;

import org.apache.jena.rdf.model.Statement;

/**
 * Gathers a set of triples for verbalization
 * @author ngonga
 */
public interface TripleSelector {
    public List<Statement> getNextStatements();
}
