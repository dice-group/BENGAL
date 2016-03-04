/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.paraphrasing;


import org.aksw.gerbil.transfer.nif.Document;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.aksw.gerbil.io.nif.NIFWriter;
import org.aksw.gerbil.io.nif.impl.TurtleNIFWriter;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.simba.bengal.paraphrasing.Paraphraser;
import org.aksw.simba.bengal.selector.TripleSelector;
import org.aksw.simba.bengal.verbalizer.Verbalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Statement;
import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.aksw.simba.bengal.selector.SimpleSummarySelector;
import org.aksw.simba.bengal.verbalizer.SemWeb2NLVerbalizer;
import org.dllearner.kb.sparql.SparqlEndpoint;

/**
 * A paraphraser rewrites the content of a document to a semantically equivalent 
 * expression. The position of the annotations is to be updated. Returns null if
 * paraphrasing went wrong.
 * @author ngonga
 */
public class Paraphraser {
    public Document getParaphrase (Document doc) throws Exception{
        Translate.setClientId("Moussallem");
        Translate.setClientSecret("Sq2m3JmTIcdoA5diD2Mw4Clh6sNpoRp5e4uxcXpMwT8=");
        String text = doc.getText();
        
        
        String translatedText = Translate.execute(text, Language.ENGLISH, Language.ITALIAN);
        String paraphrases = Translate.execute(translatedText, Language.ITALIAN, Language.ENGLISH);
 
        doc.setText(paraphrases);
        return doc;
    
    }
    
    
    
}