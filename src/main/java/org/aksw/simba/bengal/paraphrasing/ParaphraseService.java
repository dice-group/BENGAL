package org.aksw.simba.bengal.paraphrasing;

/**
 * This interface defines methods to paraphrase strings.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public interface ParaphraseService {

	/**
	 * This method paraphrases the given text. Note that it returns null if an
	 * error occurs.
	 * 
	 * 
	 * @param originalText
	 *            the text that should be paraphrased
	 * @return the paraphrased text or null if an error occurs.
	 */
	public String paraphrase(String originalText);
}
