package org.aksw.simba.bengal.selector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * This triple selector is a hybrid approach based on the
 * {@link SimpleSummarySelector} and the {@link PathBasedTripleSelector}. It
 * randomly selects whether it should follow the star or the path pattern to
 * select the next statement that is added to the selected triples.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class HybridTripleSelector extends AbstractSelector {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HybridTripleSelector.class);

    private Set<String> sourceClasses;
    private List<Resource> resources;
    private Random r = new Random(20);
    private int minSize = 1;
    private int maxSize = 5;

    /**
     * Constructor
     * 
     * @param sourceClasses
     *            Classes for subjects
     * @param targetClasses
     *            Classes for objects
     * @param endpoint
     *            SPARQL endpoint
     * @param graph
     *            Graph to query (null if none)
     * @param minSize
     *            Minimal size of summary
     * @param maxSize
     *            Maximal size of summary
     */
    public HybridTripleSelector(Set<String> sourceClasses, Set<String> targetClasses, String endpoint, String graph,
            int minSize, int maxSize, long seed) {
        super(targetClasses, endpoint, graph);
        this.sourceClasses = sourceClasses;
        resources = null;
        this.minSize = minSize;
        if (maxSize < minSize) {
            maxSize = minSize + 1;
        }
        this.maxSize = maxSize;
        this.r = new Random(seed);
    }

    /**
     * Constructor
     * 
     * @param sourceClasses
     *            Classes for subjects
     * @param targetClasses
     *            Classes for objects
     * @param endpoint
     *            SPARQL endpoint
     * @param graph
     *            Graph to query (null if none)
     */
    public HybridTripleSelector(Set<String> sourceClasses, Set<String> targetClasses, String endpoint, String graph) {
        super(targetClasses, endpoint, graph);
        this.sourceClasses = sourceClasses;
        resources = null;
    }

    /**
     * Returns the next set of statements generated by this selector
     * 
     * @return Set of statements
     */
    public List<Statement> getNextStatements() {
        if (resources == null) {
            resources = getResources(sourceClasses);
        }
        // pick a random length for the result list
        int size = minSize + r.nextInt(maxSize - minSize + 1);
        List<Statement> result = new ArrayList<>(size);
        Set<Resource> visitedResources = new HashSet<Resource>();

        // Choose the first resource randomly
        int counter = Math.abs(r.nextInt() % resources.size());
        Resource currentResource = resources.get(counter);
        visitedResources.add(currentResource);

        List<Statement> statements = null;
        Statement statement;
        boolean resourceChanged = true;
        int retries = 0, maxRetries = 10;
        while (result.size() < size) {
            // get symmetric CBD
            if (resourceChanged) {
                statements = getSummary(currentResource);
                if (statements == null) {
                    // there was an error
                    return null;
                }
                if (statements.size() == 0) {
                    System.out.println(result);
                    return result;
                }
                resourceChanged = false;
            }

            // now pick a random statement
            counter = Math.abs(r.nextInt() % statements.size());
            statement = statements.get(counter);
            // if this is the last statement of this path
            if (result.size() == (size - 1)) {
                result.add(statement);
            } else {
                // we have to make sure that the object is a resource that we
                // have not seen before
                if (statement.getObject().isResource()
                        && (!visitedResources.contains(statement.getObject().asResource()))) {
                    result.add(statement);
                    retries = 0;
                    visitedResources.add(statement.getObject().asResource());
                    // Choose whether we should go on with the path pattern
                    if (r.nextBoolean()) {
                        currentResource = statement.getObject().asResource();
                        resourceChanged = true;
                    }
                } else {
                    ++retries;
                    if(retries > maxRetries) {
                        LOGGER.warn("After {} retries I couldn't select a matching statement. Returning the statements I selected so far.", maxRetries);
                        System.out.println(result);
                        return result;
                    }
                }
            }
        }
        System.out.println(result);
        return result;
    }
}