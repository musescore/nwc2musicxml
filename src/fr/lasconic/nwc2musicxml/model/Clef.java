package fr.lasconic.nwc2musicxml.model;

public class Clef implements IElement {
	public String type;
	public int octaveShift;
	
	public Clef(){
		this.type = "Treble";
		this.octaveShift = 0;
	}
	
	
	public String getSign(){
		String sign = "G";
		String t = type.toLowerCase();
		if (t.contains("treble")) {
			sign = "G";
		} else if (t.contains("bass")) {
			sign = "F";
		} else if (t.contains("alto")) {
			sign = "C";
		} else if (t.contains("tenor")) {
			sign = "C";
		} else if (t.contains("percussion")){
			sign = "percussion";
		}
		return sign;
	}
	
	public String getLine(){
		String line = "2";
		String t = type.toLowerCase();
		if (t.contains("treble")) {
			line = "2";
		} else if (t.contains("bass")) {
			line = "4";
		} else if (t.contains("alto")) {
			line = "3";
		} else if (t.contains("tenor")) {
			line = "4";
		} else if (t.contains("percussion")) {
			line = "3";
		}
			
		return line;
	}
	
}
