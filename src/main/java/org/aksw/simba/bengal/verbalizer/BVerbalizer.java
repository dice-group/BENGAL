/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.verbalizer;

import java.io.IOException;
import java.util.List;

import org.aksw.gerbil.transfer.nif.Document;
import org.apache.jena.rdf.model.Statement;

/**
 * Interface for verbalizer. The job of a verbalizer is to take a bunch of RDF
 * triples and generates natural-language text.
 * 
 * @author ngonga
 */
public interface BVerbalizer {
	public Document generateDocument(List<Statement> triples) throws IOException;
}
