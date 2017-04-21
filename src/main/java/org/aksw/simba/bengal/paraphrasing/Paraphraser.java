/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.simba.bengal.paraphrasing;

import org.aksw.gerbil.transfer.nif.Document;

/**
 * A paraphraser rewrites the content of a document to a semantically equivalent
 * expression. The position of the annotations is to be updated. Returns null if
 * paraphrasing went wrong.
 * 
 * @author ngonga
 */
public interface Paraphraser {

	/**
	 * This method paraphrases the given document and returns a document with
	 * the paraphrased text and updated named entities. This method should never
	 * return null. If the paraphrasing encounters a problem the original
	 * document should be returned.
	 * 
	 * @param doc
	 *            the {@link Document} that should be paraphrased
	 * @return the paraphrased document or the original document if a problem
	 *         occurs.
	 */
	public Document getParaphrase(Document doc);
}
