/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.paraphrasing;


import java.util.List;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Span;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

/**
 * A paraphraser rewrites the content of a document to a semantically equivalent 
 * expression. The position of the annotations is to be updated. Returns null if
 * paraphrasing went wrong.
 * @author ngonga
 */
public class Paraphraser {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Paraphraser.class);
    
    public Document getParaphrase (Document doc) throws Exception{
        Translate.setClientId("Moussallem");
        Translate.setClientSecret("Sq2m3JmTIcdoA5diD2Mw4Clh6sNpoRp5e4uxcXpMwT8=");
        String text = doc.getText();
        
        
        String translatedText = Translate.execute(text, Language.ENGLISH, Language.ITALIAN);
        String paraphrases = Translate.execute(translatedText, Language.ITALIAN, Language.ENGLISH);

        doc.setText(paraphrases);
        Document newDoc = new DocumentImpl(paraphrases, doc.getDocumentURI(), doc.getMarkings());
        // Update named entities
        List<Span> spans = newDoc.getMarkings(Span.class);
        int pos;
        for (Span span : spans) {
            // TODO search the position in the old text
            // TODO Update the position in the new text
            // TODO if position search fails, return the original document and log a message like:
            LOGGER.warn("Paraphrasing changed one of the entities. Returning document without paraphrasing.");
            return doc;
        }
 
        return doc;
    
    }
    
    
    
}