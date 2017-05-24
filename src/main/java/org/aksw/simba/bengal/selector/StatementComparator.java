package org.aksw.simba.bengal.selector;

import java.util.Comparator;

import org.apache.jena.rdf.model.Statement;

public class StatementComparator implements Comparator<Statement> {

	@Override
	public int compare(Statement s1, Statement s2) {
		int diff = s1.getSubject().getURI().compareTo(s2.getSubject().getURI().toString());
		if (diff == 0) {
			diff = s1.getPredicate().getURI().compareTo(s2.getPredicate().getURI().toString());
			if (diff == 0) {
				diff = s1.getObject().toString().compareTo(s2.getObject().toString());
			}
		}
		return diff;
	}

}
