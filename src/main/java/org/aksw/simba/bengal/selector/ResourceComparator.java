package org.aksw.simba.bengal.selector;

import java.util.Comparator;

import com.hp.hpl.jena.rdf.model.Resource;

public class ResourceComparator implements Comparator<Resource> {

    @Override
    public int compare(Resource r1, Resource r2) {
        return r1.getURI().compareTo(r2.getURI());
    }

}
