package fr.lasconic.nwc2musicxml.model;

public class Ending implements IElement {
	public String number; // 1 or 1,2 etc (including D for Default ending)
	public String type; // start, stop (closed) or discontinue (open)
	public boolean lastInSet = false;
	
	public String toString() {
		return "Ending Number: " + number + " Type: " + type;
	}
}
