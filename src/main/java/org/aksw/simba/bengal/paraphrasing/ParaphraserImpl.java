package org.aksw.simba.bengal.paraphrasing;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.carrotsearch.hppc.BitSet;

public class ParaphraserImpl implements Paraphraser, Comparator<NamedEntity> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Paraphraser.class);

	protected ParaphraseService service;

	public ParaphraserImpl(ParaphraseService service) {
		this.service = service;
	}

	@Override
	public Document getParaphrase(Document doc, String dictPath) {
		String text = doc.getText();
		String paraphrases = service.paraphrase(text, dictPath);
		if (paraphrases == null) {
			return doc;
		}

		Document newDoc = new DocumentImpl(paraphrases, doc.getDocumentURI());

		// find all named entities inside the new text
		// first sort them descending by their length
		List<NamedEntity> originalNes = doc.getMarkings(NamedEntity.class);
		Collections.sort(originalNes, this);
		// Go through the list of named entities (starting with the longest) and
		// search for them inside the paraphrased text. Make sure that the
		// entities are not overlapping.
		BitSet blockedPositions = new BitSet(paraphrases.length());
		BitSet currentPositions = new BitSet(paraphrases.length());
		String label;
		int pos;
		for (NamedEntity ne : originalNes) {
			label = text.substring(ne.getStartPosition(), ne.getStartPosition() + ne.getLength());
			pos = -ne.getLength();
			do {
				// search the position in the new text (make sure that we start
				// behind the position we might have found before)
				pos = paraphrases.indexOf(label, pos + ne.getLength());
				if (pos < 0) {
					// the position search failed
					LOGGER.warn(
							"The paraphrasing changed one of the entities. Couldn't find the surface form \"{}\" in the text \"{}\". Returning the original document.",
							label, paraphrases);
					return doc;
				}
				currentPositions.clear();
				// check that this part of the String does not already have been
				// blocked
				currentPositions.set(pos, pos + ne.getLength());
			} while (BitSet.intersectionCount(blockedPositions, currentPositions) > 0);
			// Update the position in the new text
			newDoc.addMarking(new NamedEntity(pos, ne.getLength(), ne.getUris()));
			blockedPositions.or(currentPositions);
		}
		return newDoc;
	}

	/**
	 * Sorts Named entities descending by their length.
	 */
	@Override
	public int compare(NamedEntity n1, NamedEntity n2) {
		int diff = n1.getLength() - n2.getLength();
		if (diff < 0) {
			// n1 is shorter
			return 1;
		} else if (diff > 0) {
			return -1;
		} else {
			return 0;
		}
	}
}