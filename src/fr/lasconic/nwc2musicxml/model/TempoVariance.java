package fr.lasconic.nwc2musicxml.model;

public class TempoVariance implements IElement {
	public String type;
	
	public boolean forNextNote() {
		if (type.equals("Fermata") || type.equals("Breath Mark")) {
			return true;
		} else {
			return false;
		}
	}
	
	public String getTempoVariance() {
		if (type.equals("Accelerando")) {
			return "accel.";
		} else if (type.equals("Allargando")) {
			return "allarg.";
		} else if (type.equals("Ritenuto")) {
			return "riten.";
		} else if (type.equals("Rallentando")) {
			return "rall.";
		} else if (type.equals("Ritardando")) {
			return "rit.";
		} else if (type.equals("Rubato")) {
			return "rubato";
		} else if (type.equals("Stringendo")) {
			return "string.";
		} else {
			return "";
		}
	}
}
