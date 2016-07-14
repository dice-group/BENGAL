package org.aksw.simba.bengal.utils;

import java.util.List;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.MeaningSpan;
import org.aksw.gerbil.transfer.nif.Span;

/**
 * A simple utility class that should help to work with {@link Document}
 * instances.
 * 
 * @author Michael R&ouml;der (roeder@informatik.uni-leipzig.de)
 *
 */
public class DocumentHelper {

    /**
     * Searches the {@link MeaningSpan} that a) occurs first inside the document
     * and b) has the given URI as meaning.
     * 
     * @param uri
     *            the meaning the searched marking should have
     * @param document
     *            the document in which the marking should be searched
     * @return the first occurrence of a meaning with the given URI in the text
     *         or null if such a marking couldn't be found.
     */
    public static MeaningSpan searchFirstOccurrence(String uri, Document document) {
        List<MeaningSpan> entities = document.getMarkings(MeaningSpan.class);
        MeaningSpan result = null;
        for (MeaningSpan marking : entities) {
            if (marking.containsUri(uri)) {
                if ((result == null) || (marking.getStartPosition() < result.getStartPosition())) {
                    result = marking;
                }
            }
        }
        return result;
    }

    /**
     * This method replaces the part with the given start position and the given
     * length inside the text of the given document with the given replacement.
     * It updates the positions of markings that occur in the text behind the
     * replaced part. <b>Note</b> that this method does not take care of
     * markings that overlap with the replaced part!
     * 
     * @param document
     *            the document that should be updated
     * @param start
     *            the start position of the part that should be replaced
     * @param length
     *            the length of the part that should be replaced
     * @param replacement
     *            the replacement that should be inserted at the position of the
     *            deleted part
     */
    public static void replaceText(Document document, int start, int length, String replacement) {
        if (start < 0) {
            throw new IllegalArgumentException("Negative start position (s=" + start + ").");
        }
        if (length < 0) {
            throw new IllegalArgumentException("Negative length (l=" + length + ").");
        }
        // Calculate the difference between the old part of the text and the
        // replacement
        int diff = replacement.length() - length;
        int end = start + length;

        String text = document.getText();
        if (end > text.length()) {
            throw new IllegalArgumentException("The given Span(s=" + start + ",l=" + length
                    + ") is outside of the bounds of the document text (l=" + text.length() + ").");
        }

        if (diff != 0) {
            // Update all spans
            List<Span> spans = document.getMarkings(Span.class);
            for (Span span : spans) {
                if (span.getStartPosition() >= end) {
                    span.setStartPosition(span.getStartPosition() + diff);
                }
            }
        }

        // Create the new text
        StringBuilder builder = new StringBuilder(text.length() + diff);
        builder.append(text.substring(0, start));
        builder.append(replacement);
        builder.append(text.substring(end));
        document.setText(builder.toString());
    }

}
