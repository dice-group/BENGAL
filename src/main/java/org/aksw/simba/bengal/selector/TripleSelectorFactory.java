/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.selector;

import java.util.Set;

/**
 * Factory for generating triple selectors from names
 * 
 * @author ngonga
 */
public class TripleSelectorFactory {

    public enum SelectorType {
        STAR, SIM_STAR, PATH, HYBRID
    };

    public TripleSelector create(SelectorType type, Set<String> sourceClasses, Set<String> targetClasses,
            String endpoint, String graph, int minSize, int maxSize, long seed) {
        switch (type) {
        case STAR:
            return new SimpleSummarySelector(sourceClasses, targetClasses, endpoint, graph, minSize, maxSize, seed,
                    false);
        case SIM_STAR:
            return new SimpleSummarySelector(sourceClasses, targetClasses, endpoint, graph, minSize, maxSize, seed,
                    true);
        case PATH:
            return new PathBasedTripleSelector(sourceClasses, targetClasses, endpoint, graph, minSize, maxSize, seed);
        case HYBRID:
            return new HybridTripleSelector(sourceClasses, targetClasses, endpoint, graph, minSize, maxSize, seed);
        }
        return null;
    }

}
