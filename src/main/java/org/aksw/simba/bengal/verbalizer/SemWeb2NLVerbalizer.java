/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.verbalizer;

import java.util.List;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.apache.jena.rdf.model.Statement;

/**
 * A deterministic verbalizer which relies on the SemWeb2NL project.
 * 
 * @author ngonga
 * @author roeder
 */
public class SemWeb2NLVerbalizer implements Verbalizer {

    public Document generateDocument(List<Statement> triples) {

        // This is a simple example that shows how a document can be created
        Document document = new DocumentImpl("PLEASE GIVE ME A URI!!!");
        int startPosition = 0;
        int length = 10;
        document.addMarking(new NamedEntity(startPosition, length, "ENTITY URI"));
        return document;
    }
}
