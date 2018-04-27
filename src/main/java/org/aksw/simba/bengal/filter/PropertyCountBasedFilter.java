package org.aksw.simba.bengal.filter;

import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Statement;

public class PropertyCountBasedFilter implements TripleSetFilter {

    protected Set<String> properties;
    protected int minCount;

    public PropertyCountBasedFilter(Set<String> properties, int minCount) {
        super();
        this.properties = properties;
        this.minCount = minCount;
    }

    @Override
    public boolean isGood(List<Statement> triples) {
        return triples.stream().parallel().filter(s -> properties.contains(s.getPredicate().getURI())).count() >= minCount;
    }

}
