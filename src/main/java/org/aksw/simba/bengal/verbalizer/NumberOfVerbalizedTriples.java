package org.aksw.simba.bengal.verbalizer;

import org.aksw.gerbil.transfer.nif.Marking;

public class NumberOfVerbalizedTriples implements Marking {

	private int numberOfTriples;

	public NumberOfVerbalizedTriples(int numberOfTriples) {
		super();
		this.numberOfTriples = numberOfTriples;
	}

	public int getNumberOfTriples() {
		return numberOfTriples;
	}

	public void setNumberOfTriples(int numberOfTriples) {
		this.numberOfTriples = numberOfTriples;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO hack to avoid error if this method is deleted @MichaelRoeder
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + numberOfTriples;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NumberOfVerbalizedTriples other = (NumberOfVerbalizedTriples) obj;
		if (numberOfTriples != other.numberOfTriples)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("NumberOfVerbalizedTriples [numberOfTriples=");
		builder.append(numberOfTriples);
		builder.append("]");
		return builder.toString();
	};

}
