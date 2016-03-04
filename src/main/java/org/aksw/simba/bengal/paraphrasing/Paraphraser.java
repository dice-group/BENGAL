/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.paraphrasing;


import org.aksw.gerbil.transfer.nif.Document;

import com.memetix.mst.language.Language;
import com.memetix.mst.translate.Translate;

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