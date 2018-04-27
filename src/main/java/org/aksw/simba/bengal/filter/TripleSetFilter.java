package org.aksw.simba.bengal.filter;

import java.util.List;

import org.apache.jena.rdf.model.Statement;

@FunctionalInterface
public interface TripleSetFilter {

    /**
     * Returns {@code true} if the triples are fulfilling the criteria of this filter and can be used for generating a document.
     * 
     * @param triples the triples that have been chosen for creating a document
     * @return {@code true} if the triples are fulfilling the criteria of this filter, else {@code false}
     */
    public boolean isGood(List<Statement> triples);
}
