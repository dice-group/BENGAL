package org.aksw.simba.bengal.utils;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.aksw.gerbil.dataset.Dataset;
import org.aksw.gerbil.dataset.DatasetConfiguration;
import org.aksw.gerbil.dataset.impl.nif.NIFFileDatasetConfig;
import org.aksw.gerbil.datatypes.ExperimentType;
import org.aksw.gerbil.exceptions.GerbilException;
import org.aksw.gerbil.transfer.nif.Document;
import org.aksw.gerbil.web.config.AdapterList;
import org.aksw.gerbil.web.config.DatasetsConfig;
import org.apache.commons.io.IOUtils;

import au.com.bytecode.opencsv.CSVWriter;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class PosTagAnalyzer {

	public static void main(String[] args) {
		List<DatasetConfiguration> configs = new ArrayList<>();

		AdapterList<DatasetConfiguration> datasets = DatasetsConfig.datasets(null, null);
		configs.addAll(datasets.getAdaptersForName("ACE2004"));
		configs.addAll(datasets.getAdaptersForName("AIDA/CoNLL-Complete"));
		configs.addAll(datasets.getAdaptersForName("AIDA/CoNLL-Test A"));
		configs.addAll(datasets.getAdaptersForName("AIDA/CoNLL-Test B"));
		configs.addAll(datasets.getAdaptersForName("AIDA/CoNLL-Training"));
		configs.addAll(datasets.getAdaptersForName("AQUAINT"));
		configs.addAll(datasets.getAdaptersForName("DBpediaSpotlight"));
		configs.addAll(datasets.getAdaptersForName("IITB"));
		configs.addAll(datasets.getAdaptersForName("KORE50"));
		configs.addAll(datasets.getAdaptersForName("Microposts2014-Test"));
		configs.addAll(datasets.getAdaptersForName("Microposts2014-Train"));
		configs.addAll(datasets.getAdaptersForName("MSNBC"));
		configs.addAll(datasets.getAdaptersForName("N3-Reuters-128"));
		configs.addAll(datasets.getAdaptersForName("N3-RSS-500"));
		configs.addAll(datasets.getAdaptersForName("OKE 2015 Task 1 evaluation dataset"));
		configs.addAll(datasets.getAdaptersForName("OKE 2015 Task 1 gold standard Sample"));

		configs.add(new NIFFileDatasetConfig("B1", "datasets/B1_bengal_path_100.ttl", true, ExperimentType.A2KB, null,
				null));
		configs.add(new NIFFileDatasetConfig("B2", "datasets/B2_bengal_path_para_100.ttl", true, ExperimentType.A2KB,
				null, null));
		configs.add(new NIFFileDatasetConfig("B3", "datasets/B3_bengal_star_100.ttl", true, ExperimentType.A2KB, null,
				null));
		configs.add(new NIFFileDatasetConfig("B4", "datasets/B4_bengal_star_para_100.ttl", true, ExperimentType.A2KB,
				null, null));
		configs.add(new NIFFileDatasetConfig("B5", "datasets/B5_bengal_sym_100.ttl", true, ExperimentType.A2KB, null,
				null));
		configs.add(new NIFFileDatasetConfig("B6", "datasets/B6_bengal_sym_para_100.ttl", true, ExperimentType.A2KB,
				null, null));
		configs.add(new NIFFileDatasetConfig("B7", "datasets/B7_bengal_hybrid_100.ttl", true, ExperimentType.A2KB, null,
				null));
		configs.add(new NIFFileDatasetConfig("B8", "datasets/B8_bengal_hybrid_para_100.ttl", true, ExperimentType.A2KB,
				null, null));
		configs.add(new NIFFileDatasetConfig("B9", "datasets/B9_bengal_summary_100.ttl", true, ExperimentType.A2KB,
				null, null));
		configs.add(new NIFFileDatasetConfig("B10", "datasets/B10_bengal_summary_para_100.ttl", true,
				ExperimentType.A2KB, null, null));
		configs.add(new NIFFileDatasetConfig("B11", "datasets/B11_bengal_hybrid_10000.ttl", true, ExperimentType.A2KB,
				null, null));
		configs.add(new NIFFileDatasetConfig("B12", "datasets/B12_bengal_hybrid_object_10.ttl", true,
				ExperimentType.A2KB, null, null));
		configs.add(new NIFFileDatasetConfig("B13", "datasets/B13_bengal_star_data_10_70+_sen.ttl", true,
				ExperimentType.A2KB, null, null));

		PosTagAnalyzer analyzer = new PosTagAnalyzer();
		analyzer.analyze(configs, "analyzation.csv");
	}

	protected StanfordCoreNLP pipeline;

	public PosTagAnalyzer() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos");
		pipeline = new StanfordCoreNLP(props);
	}

	public void analyze(List<DatasetConfiguration> configs, String outputFile) {
		List<Object2IntOpenHashMap<String>> results = new ArrayList<>(configs.size());
		for (DatasetConfiguration config : configs) {
			Dataset dataset;
			try {
				dataset = config.getDataset(ExperimentType.A2KB);
				results.add(analyzeDocs(dataset.getInstances()));
			} catch (GerbilException e) {
				e.printStackTrace();
				results.add(null);
			}
		}
		writeOutput(outputFile, configs, results);
	}

	private Object2IntOpenHashMap<String> analyzeDocs(List<Document> documents) {
		Object2IntOpenHashMap<String> result = new Object2IntOpenHashMap<>();
		Annotation annotation;
		for (Document document : documents) {
			annotation = new Annotation(document.getText());
			pipeline.annotate(annotation);
			List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
			for (CoreMap sentence : sentences) {
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					result.addTo(token.get(PartOfSpeechAnnotation.class), 1);
				}
			}
		}
		return result;
	}

	private void writeOutput(String outputFile, List<DatasetConfiguration> configs,
			List<Object2IntOpenHashMap<String>> results) {
		Set<String> posTags = new HashSet<String>();
		for (Object2IntOpenHashMap<String> result : results) {
			if (result != null) {
				posTags.addAll(result.keySet());
			}
		}
		String keys[] = posTags.toArray(new String[posTags.size()]);
		Arrays.sort(keys);
		FileWriter fWriter = null;
		CSVWriter writer = null;
		try {
			fWriter = new FileWriter(outputFile);
			writer = new CSVWriter(fWriter);
			String line[] = new String[configs.size() + 1];
			line[0] = "pos tags";
			for (int i = 0; i < configs.size(); ++i) {
				line[i + 1] = configs.get(i).getName();
			}
			writer.writeNext(line);
			Object2IntOpenHashMap<String> result;
			for (int i = 0; i < keys.length; ++i) {
				line[0] = keys[i];
				for (int j = 0; j < results.size(); ++j) {
					result = results.get(j);
					if (result != null) {
						line[j + 1] = Integer.toString(result.getInt(line[0]));
					} else {
						line[j + 1] = "";
					}
				}
				writer.writeNext(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(writer);
			IOUtils.closeQuietly(fWriter);
		}
	}

}
