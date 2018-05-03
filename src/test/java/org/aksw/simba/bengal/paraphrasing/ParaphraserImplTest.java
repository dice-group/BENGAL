package org.aksw.simba.bengal.paraphrasing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.transfer.nif.Marking;
import org.aksw.gerbil.transfer.nif.data.DocumentImpl;
import org.aksw.gerbil.transfer.nif.data.NamedEntity;
import org.aksw.gerbil.transfer.nif.data.StartPosBasedComparator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParaphraserImplTest implements ParaphraseService {

	public static void main(String[] args) {
		System.out.println((new Paraphrasing())
				.paraphrase("Tartar's mouth country is Azerbaijan." + " Azerbaijan's leader is Ilham Aliyev."
						+ " Ilham Aliyev's successor is Artur Rasizade. Artur Rasizade's party is New Azerbaijan Party. New Azerbaijan Party's headquarter is Baku."
						+ ""));
	}

	@Parameters
	public static Collection<Object[]> data() {
		List<Object[]> testConfigs = new ArrayList<Object[]>();
		// The extractor returns nothing
		testConfigs.add(new Object[] {
				new DocumentImpl("The senator received a Bachelor of Laws from the Columbia University.",
						"http://example.org/doc1",
						Arrays.asList((Marking) new NamedEntity(4, 7, "http://example.org/Senator"),
								(Marking) new NamedEntity(49, 19, "http://example.org/Columbia_University"))),
				new DocumentImpl("The Columbia University awarded the senator a Bachelor of Laws.",
						"http://example.org/doc1",
						Arrays.asList((Marking) new NamedEntity(36, 7, "http://example.org/Senator"),
								(Marking) new NamedEntity(4, 19, "http://example.org/Columbia_University"))),
				"The Columbia University awarded the senator a Bachelor of Laws." });
		testConfigs
				.add(new Object[] {
						new DocumentImpl(
								"Tartar's mouth country is Azerbaijan. Azerbaijan's leader is Ilham Aliyev. Ilham Aliyev's successor is Artur Rasizade. Artur Rasizade's party is New Azerbaijan Party. New Azerbaijan Party's headquarter is Baku.",
								"http://example.org/doc1",
								Arrays.asList(
										(Marking) new NamedEntity(167, 20,
												"http://dbpedia.org/resource/New_Azerbaijan_Party"),
										(Marking) new NamedEntity(26, 10, "http://dbpedia.org/resource/Azerbaijan"),
										(Marking) new NamedEntity(61, 12, "http://dbpedia.org/resource/Ilham_Aliyev"),
										(Marking) new NamedEntity(103, 14,
												"http://dbpedia.org/resource/Artur_Rasizade"),
										(Marking) new NamedEntity(38, 10, "http://dbpedia.org/resource/Azerbaijan"),
										(Marking) new NamedEntity(119, 14,
												"http://dbpedia.org/resource/Artur_Rasizade"),
										(Marking) new NamedEntity(75, 12, "http://dbpedia.org/resource/Ilham_Aliyev"),
										(Marking) new NamedEntity(145,
												20, "http://dbpedia.org/resource/New_Azerbaijan_Party"),
										(Marking) new NamedEntity(205, 4, "http://dbpedia.org/resource/Baku"))),
						new DocumentImpl(
								"Country of mouth of tartar is Azerbaijan. Azerbaijan's leader Ilham Aliyev. Successor of Ilham Aliyev is Artur Rasizade. Party of Artur Rasizade's New Azerbaijan Party. Home of the New Azerbaijan Party is Baku.",
								"http://example.org/doc1",
								Arrays.asList(
										(Marking) new NamedEntity(147, 20,
												"http://dbpedia.org/resource/New_Azerbaijan_Party"),
										(Marking) new NamedEntity(30, 10, "http://dbpedia.org/resource/Azerbaijan"),
										(Marking) new NamedEntity(62, 12, "http://dbpedia.org/resource/Ilham_Aliyev"),
										(Marking) new NamedEntity(105, 14,
												"http://dbpedia.org/resource/Artur_Rasizade"),
										(Marking) new NamedEntity(42, 10, "http://dbpedia.org/resource/Azerbaijan"),
										(Marking) new NamedEntity(130, 14,
												"http://dbpedia.org/resource/Artur_Rasizade"),
										(Marking) new NamedEntity(89, 12, "http://dbpedia.org/resource/Ilham_Aliyev"),
										(Marking) new NamedEntity(181,
												20, "http://dbpedia.org/resource/New_Azerbaijan_Party"),
										(Marking) new NamedEntity(205, 4, "http://dbpedia.org/resource/Baku"))),
						"Country of mouth of tartar is Azerbaijan. Azerbaijan's leader Ilham Aliyev. Successor of Ilham Aliyev is Artur Rasizade. Party of Artur Rasizade's New Azerbaijan Party. Home of the New Azerbaijan Party is Baku." });
		// In the following test case, the paraphrasing changed one of the named
		// entities surface forms. Thus, the original document should be
		// returned.
		testConfigs
				.add(new Object[] {
						new DocumentImpl("New Azerbaijan Party's headquarter is Baku.", "http://example.org/doc1",
								Arrays.asList(
										(Marking) new NamedEntity(0, 20,
												"http://dbpedia.org/resource/New_Azerbaijan_Party"),
										(Marking) new NamedEntity(38, 4, "http://dbpedia.org/resource/Baku"))),
						new DocumentImpl("New Azerbaijan Party's headquarter is Baku.", "http://example.org/doc1",
								Arrays.asList(
										(Marking) new NamedEntity(0, 20,
												"http://dbpedia.org/resource/New_Azerbaijan_Party"),
										(Marking) new NamedEntity(38, 4, "http://dbpedia.org/resource/Baku"))),
						"Baku is the headquarter of the party of New Azerbaijan." });
		return testConfigs;
	}

	private Document document;
	private Document expectedDocument;
	private String paraphrasedText;

	public ParaphraserImplTest(Document document, Document expectedDocument, String paraphrasedText) {
		super();
		this.document = document;
		this.expectedDocument = expectedDocument;
		this.paraphrasedText = paraphrasedText;
	}

	@Test
	public void test() {
		ParaphraserImpl paraphraser = new ParaphraserImpl(this);
		Document newDocument = paraphraser.getParaphrase(document);

		Assert.assertEquals(expectedDocument.getDocumentURI(), newDocument.getDocumentURI());
		Assert.assertEquals(expectedDocument.getText(), newDocument.getText());
		StartPosBasedComparator comparator = new StartPosBasedComparator();
		List<NamedEntity> expectedNes = expectedDocument.getMarkings(NamedEntity.class);
		List<NamedEntity> nes = newDocument.getMarkings(NamedEntity.class);
		Collections.sort(expectedNes, comparator);
		Collections.sort(nes, comparator);
		Assert.assertArrayEquals(nes.toArray(), expectedNes.toArray());
	}

	@Override
	public String paraphrase(String originalText) {
		return paraphrasedText;
	}

}
