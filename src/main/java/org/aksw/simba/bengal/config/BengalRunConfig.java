package org.aksw.simba.bengal.config;

import org.aksw.simba.bengal.selector.TripleSelectorFactory.SelectorType;

public class BengalRunConfig {

	private static final int DEFAULT_NUMBER_OF_DOCUMENTS = 1;
	private static final long SEED = 21;
	private static final int MIN_SENTENCE = 3;
	private static final int MAX_SENTENCE = 10;
	private static final long WAITING_TIME_BETWEEN_DOCUMENTS = 500;

	private int numberOfDocs = DEFAULT_NUMBER_OF_DOCUMENTS;
	private long seed = SEED;
	private int minSentence = MIN_SENTENCE;
	private int maxSentence = MAX_SENTENCE;
	private long waitTime = WAITING_TIME_BETWEEN_DOCUMENTS;
	private String selectorType;

	private boolean useParaphrasing = false;
	private boolean usePronouns = false;
	private boolean useSurfaceForms = false;
	private boolean useAvatars = false;
	private boolean useOnlyObjectProps = false;

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
		if(selectorType.matches("star")) {
			res = SelectorType.STAR;
		} else if(selectorType.matches("hybrid")) {
			res = SelectorType.HYBRID;
		} else if(selectorType.matches("path")) {
			res = SelectorType.PATH;
		} else if(selectorType.matches("sym")) {
			res = SelectorType.SIM_STAR;
		}
		return res;
	}

	@Override
	public String toString() {
		return "BengalRunConfig [defNumberOf=" + numberOfDocs + ", seed=" + seed + ", minSentence=" + minSentence
				+ ", maxSentence=" + maxSentence + ", selectorType=" + selectorType + ", useParaphrasing="
				+ useParaphrasing + ", usePronouns=" + usePronouns + ", useSurfaceForms=" + useSurfaceForms
				+ ", useAvatars=" + useAvatars + ", useOnlyObjectProps=" + useOnlyObjectProps + ", waitTime=" + waitTime
				+ "]";
	}

}
