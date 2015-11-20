package fr.lasconic.nwc2musicxml.model;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Tempo implements IElement {

	private int tempo;
	private String base;
	private String text;
	private float pos;

	public Tempo() {
		tempo = 120;
		base = "Quarter";
		text = "";
		pos = 0;
	}

	private static final Map<String, Double> baseLength;
	static {
		Map<String, Double> aMap = new HashMap<String, Double>();
		aMap.put("Eighth", 0.5);
		aMap.put("Eighth Dotted", 0.75);
		aMap.put("Quarter", 1.0);
		aMap.put("Quarter Dotted", 1.5);
		aMap.put("Half", 2.0);
		aMap.put("Half Dotted", 3.0);
		baseLength = Collections.unmodifiableMap(aMap);
	}

	public void parse(String[] sArray) {
		for (int i = 2; i < sArray.length; i++) {
			String sA = sArray[i];
			String[] sArray2;
			if (sA.startsWith("Tempo")) {
				sArray2 = sA.split(":");
				tempo = Integer.parseInt(sArray2[1]);
			} else if (sA.startsWith("Base")) {
				sArray2 = sA.split(":");
				base = sArray2[1];
			} else if (sA.startsWith("Text")) {
				sArray2 = sA.split(":");
				text = sArray2[1].substring(1, sArray2[1].length() - 1);
			} else if (sA.startsWith("Pos")) {
				sArray2 = sA.split(":");
				pos = Float.parseFloat(sArray2[1]);
			}
		}
	}

	public int getTempo() {
		return tempo;
	}

	public void setTempo(int tempo) {
		this.tempo = tempo;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getAbsoluteTempo() {
		DecimalFormat df = new DecimalFormat("#.##");
		return df.format(tempo * baseLength.get(base));
	}

	public boolean isDotted() {
		return base.contains("Dotted");
	}

	public boolean hasText() {
		return !text.isEmpty();
	}

	public String getBaseLen() {
		String result = "quarter";
		if (base.contains("Quarter")) {
			result = "quarter";
		} else if (base.contains("Eighth")) {
			result = "eighth";
		} else if (base.contains("Half")) {
			result = "half";
		}
		return result;
	}
	
	public String getPlacement() {
		if (pos >= 0)
			return "above";
		else
			return "below";
			
		
	}
}
