package fr.lasconic.nwc2musicxml.model;

public class TimeSig implements IElement{

	private int beats;
	private int beatType;
	private String symbol;
	
	public TimeSig(){
		beats = 4;
		beatType = 4;
	}
	
	public void parse(String signature) {
		beats = 4;
		beatType = 4;
		symbol = null;
		if (signature.contains("Common")) {
			beats = 4;
			beatType = 4;
			symbol = "common";
		} else if (signature.contains("AllaBreve")) {
			beats = 2;
			beatType = 2;
			symbol = "cut";
		} else {
			String[] sArray = signature.split("/");
			if (sArray.length == 2) {
				try {
					beats = Integer.parseInt(sArray[0]);
					beatType = Integer.parseInt(sArray[1]);
				} catch (NumberFormatException e) {
					beats = 4;
					beatType = 4;
				}
			}
		}
	}

	public int getBeats() {
		return beats;
	}

	public void setBeats(int beats) {
		this.beats = beats;
	}

	public int getBeatType() {
		return beatType;
	}

	public void setBeatType(int beatType) {
		this.beatType = beatType;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	
	
	
	
}
