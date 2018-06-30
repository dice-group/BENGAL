package org.aksw.simba.bengal.verbalizer;

import java.io.IOException;
import java.util.Set;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.rdf2en.triple2nl.TripleConverter;
import org.aksw.rdf2es.triple2nl.TripleConverterSpanish;
import org.aksw.rdf2pt.triple2nl.TripleConverterPortuguese;
import org.apache.jena.graph.Triple;
import org.dllearner.kb.sparql.SparqlEndpoint;


public class VerbalizerSelectorFactory {

	public enum SelectorLanguage {
		EN,ES,PT,FR,DE,IT
	};

	public Document create(SelectorLanguage language, Triple t, SparqlEndpoint endpoint) throws IOException {
		switch (language) {
		case ES:
			return new DocumentImpl(new TripleConverterSpanish(endpoint).convert(t).toString());
		case EN:
			return new DocumentImpl(new TripleConverter(endpoint).convert(t).toString());
		case PT:
			return new DocumentImpl(new TripleConverterPortuguese(endpoint).convert(t).toString());
		case DE:
			break;
		case FR:
			break;
		case IT:
			break;
		default:
			break;
	}
		return null;
	}
}
