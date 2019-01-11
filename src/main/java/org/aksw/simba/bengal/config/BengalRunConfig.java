package org.aksw.simba.bengal.config;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.aksw.simba.bengal.selector.TripleSelectorFactory.SelectorType;

public class BengalRunConfig {
	
	public static final Properties PROP = new Properties();
	static {
		InputStream input;
		try {
			input = new FileInputStream("src/main/resources/config/bengal.properties");
			PROP.load(input);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static final int DEFAULT_NUMBER_OF_DOCUMENTS = 1;
	public static final long DEF_SEED = System.currentTimeMillis();
	public static final int DEF_MIN_SENTENCE = 3;
	public static final int DEF_MAX_SENTENCE = 10;
	public static final long DEF_WAITING_TIME_BETWEEN_DOCUMENTS = 500;
	public static final String DEF_SPARQL_EP = "http://dbpedia.org/sparql";
	
	public static final String DEF_DICT_PATH = PROP.getProperty("dict");
	public static final String DEF_SURFACEFORM_PATH = PROP.getProperty("surfaceForms");

	private int numberOfDocs = DEFAULT_NUMBER_OF_DOCUMENTS;
	private long seed = DEF_SEED;
	private int minSentence = DEF_MIN_SENTENCE;
	private int maxSentence = DEF_MAX_SENTENCE;
	private long waitTime = DEF_WAITING_TIME_BETWEEN_DOCUMENTS;
	private String sqparqlEndPoint = DEF_SPARQL_EP;
	private String selectorType;

	private boolean useParaphrasing = false;
	private boolean usePronouns = false;
	private boolean useSurfaceForms = false;
	private boolean useAvatars = false;
	private boolean useOnlyObjectProps = false;
	
	private String dictDirPath = DEF_DICT_PATH;
	private String surfaceFormFilePath = DEF_SURFACEFORM_PATH;

	public int getNumberOfDocs() {
		return numberOfDocs;
	}

	public void setNumberOfDocs(int defNumberOf) {
		this.numberOfDocs = defNumberOf;
	}

	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	public int getMinSentence() {
		return minSentence;
	}

	public void setMinSentence(int minSentence) {
		this.minSentence = minSentence;
	}

	public int getMaxSentence() {
		return maxSentence;
	}

	public void setMaxSentence(int maxSentence) {
		this.maxSentence = maxSentence;
	}

	public String getSelectorType() {
		return selectorType;
	}

	public void setSelectorType(String selectorType) {
		this.selectorType = selectorType;
	}

	public boolean isUseParaphrasing() {
		return useParaphrasing;
	}

	public void setUseParaphrasing(boolean useParaphrasing) {
		this.useParaphrasing = useParaphrasing;
	}

	public boolean isUsePronouns() {
		return usePronouns;
	}

	public void setUsePronouns(boolean usePronouns) {
		this.usePronouns = usePronouns;
	}

	public boolean isUseSurfaceForms() {
		return useSurfaceForms;
	}

	public void setUseSurfaceForms(boolean useSurfaceForms) {
		this.useSurfaceForms = useSurfaceForms;
	}

	public boolean isUseAvatars() {
		return useAvatars;
	}

	public void setUseAvatars(boolean useAvatars) {
		this.useAvatars = useAvatars;
	}

	public boolean isUseOnlyObjectProps() {
		return useOnlyObjectProps;
	}

	public void setUseOnlyObjectProps(boolean useOnlyObjectProps) {
		this.useOnlyObjectProps = useOnlyObjectProps;
	}

	public long getWaitTime() {
		return waitTime;
	}

	public void setWaitTime(long waitTime) {
		this.waitTime = waitTime;
	}

	public SelectorType getSelectorTypeEnum() {
		SelectorType res = null;
		if (selectorType.matches("star")) {
			res = SelectorType.STAR;
		} else if (selectorType.matches("hybrid")) {
			res = SelectorType.HYBRID;
		} else if (selectorType.matches("path")) {
			res = SelectorType.PATH;
		} else if (selectorType.matches("sym")) {
			res = SelectorType.SIM_STAR;
		}
		return res;
	}

	public String getSqparqlEndPoint() {
		return sqparqlEndPoint;
	}

	public void setSparqlEndPoint(String sqparqlEndPoint) {
		this.sqparqlEndPoint = sqparqlEndPoint;
	}

	public String getDictDirPath() {
		return dictDirPath;
	}

	public void setDictDirPath(String dictDirPath) {
		this.dictDirPath = dictDirPath;
	}

	public String getSurfaceFormFilePath() {
		return surfaceFormFilePath;
	}

	public void setSurfaceFormFilePath(String surfaceFormFilePath) {
		this.surfaceFormFilePath = surfaceFormFilePath;
	}

	@Override
	public String toString() {
		return "BengalRunConfig [numberOfDocs=" + numberOfDocs + ", seed=" + seed + ", minSentence=" + minSentence
				+ ", maxSentence=" + maxSentence + ", waitTime=" + waitTime + ", sqparqlEndPoint=" + sqparqlEndPoint
				+ ", selectorType=" + selectorType + ", useParaphrasing=" + useParaphrasing + ", usePronouns="
				+ usePronouns + ", useSurfaceForms=" + useSurfaceForms + ", useAvatars=" + useAvatars
				+ ", useOnlyObjectProps=" + useOnlyObjectProps + ", dictDirPath=" + dictDirPath
				+ ", surfaceFormFilePath=" + surfaceFormFilePath + "]";
	}

}
