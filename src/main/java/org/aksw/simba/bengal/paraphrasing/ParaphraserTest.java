package org.aksw.simba.bengal.paraphrasing;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import org.aksw.simba.bengal.verbalizer.SemWeb2NLVerbalizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.aksw.simba.bengal.selector.SimpleSummarySelector;
import org.aksw.simba.bengal.selector.TripleSelector;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.kb.sparql.SparqlQuery;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

/**
 * A paraphraser rewrites the content of a document to a semantically equivalent 
 * expression. The position of the annotations is to be updated. Returns null if
 * paraphrasing went wrong.
 * @author ngonga
 * @author moussallem
 */
public class ParaphraserTest {
    
    
    
    SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Paraphraser.class);
    
    public Document getParaphrase (Document doc, List<Statement> triples) throws Exception{
        String paraphrase = "";
        Translate.setClientId("Moussallem");
        Translate.setClientSecret("1q2w3e4r5t5r4e3w2q11qazxsawq12");
        String text = doc.getText();
        
        
        String translatedText = Translate.execute(text, Language.ENGLISH, Language.ITALIAN);
        paraphrase = Translate.execute(translatedText, Language.ITALIAN, Language.ENGLISH);
       
        Document newDoc = new DocumentImpl(paraphrase);
       
        Set<Resource> resources = new HashSet<>();
        for (Statement s : triples) {
            resources.add(s.getSubject());
            if (s.getObject().isResource()) {
                resources.add(s.getObject().asResource());
            }
        }
        
         newDoc = annotateDocument(newDoc, resources);
       
        System.out.println("Old Text");
        System.out.println(text);
        System.out.println(doc.getMarkings());
        
        System.out.println("New Text");
        System.out.println(paraphrase);
        System.out.println(newDoc.getMarkings());
        
        // Update named entities

            // TODO search the position in the old text
            // TODO Update the position in the new text
            // TODO if position search fails, return the original document and log a message like:
            //LOGGER.warn("Paraphrasing changed one of the entities. Returning document without paraphrasing.");

        int count = 0;
        for (int i = 0; i < doc.getMarkings().size(); i++){
            for (int j = 0; j < newDoc.getMarkings().size(); j++){
            NamedEntity x = (NamedEntity) doc.getMarkings().get(i);
            NamedEntity y = (NamedEntity) newDoc.getMarkings().get(j);
            if (x.getUris().toString().equals(y.getUris().toString())){
            count = count + 1;}
            }
            //System.out.println("Consegui");
            //System.out.println(x.getUris());  
        }
        
       // ArrayList<NamedEntity> entities = new ArrayList<>();
        //Set<String> uris = new HashSet<>();
        //for (int i = 0; i <= doc.getMarkings().size(); i++) {
         //   entities.set(i, (NamedEntity) doc.getMarkings().get(i));
          //          uris.add(entities.get(i).getUris().toString());
           //         }
        
      //  System.out.println(uris.size());
        
        int similar = count - doc.getMarkings().size();
        int total = count - similar;
        System.out.println(similar);
        System.out.println(total);

        if(newDoc.getMarkings().size() == doc.getMarkings().size() && doc.getMarkings().size() == total){
            doc = newDoc;
            LOGGER.warn("Success"); }
            else { 
            doc = null;
           LOGGER.warn("Returning document without paraphrasing");}
        return doc;
           
    }

    private Document annotateDocument(Document document, Set<Resource> resources) {
        for (Resource r : resources) {
            document = annotateDocument(document, r);
        }
        return document;
    }

    private Document annotateDocument(Document document, Resource resource) {
        String label = getEnglishLabel(resource.getURI());
        String text = document.getText();

        // find all positions
        ArrayList<Integer> positions = new ArrayList<>();
        Pattern p = Pattern.compile(label); // insert your pattern here
        Matcher m = p.matcher(text);
        while (m.find()) {
            positions.add(m.start());
        }

        for (int index : positions) {
            document.addMarking(new NamedEntity(index, label.length(), resource.getURI()));
        }
        
        return document;
    }
    
    private String getEnglishLabel(String resource) {

        if (resource.equals(RDF.type.getURI())) {
            return "type";
        } else if (resource.equals(RDFS.label.getURI())) {
            return "label";
        }
        try {
            String labelQuery = "SELECT ?label WHERE {<" + resource + "> "
                    + "<http://www.w3.org/2000/01/rdf-schema#label> ?label. FILTER (lang(?label) = 'en')}";

            // take care of graph issues. Only takes one graph. Seems like some
            // sparql endpoint do
            // not like the FROM option.
            ResultSet results = new SparqlQuery(labelQuery, endpoint).send();

            // get label from knowledge base
            String label = null;
            QuerySolution soln;
            while (results.hasNext()) {
                soln = results.nextSolution();
                // process query here
                {
                    label = soln.getLiteral("label").getLexicalForm();
                }
            }
            return label;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
public static void main(String args[]) throws Exception {

Set<String> classes = new HashSet<>();
        classes.add("<http://dbpedia.org/ontology/Person>");
        classes.add("<http://dbpedia.org/ontology/Place>");
        classes.add("<http://dbpedia.org/ontology/Organisation>");
        TripleSelector ts = new SimpleSummarySelector(classes, classes, "http://dbpedia.org/sparql", null);
        List<Statement> stmts = ts.getNextStatements();
        Document doc = new SemWeb2NLVerbalizer(SparqlEndpoint.getEndpointDBpedia()).generateDocument(stmts);
        Document paraphrase = new ParaphraserTest().getParaphrase(doc, stmts);
        System.out.println(doc);
        System.out.println(paraphrase);
}

}