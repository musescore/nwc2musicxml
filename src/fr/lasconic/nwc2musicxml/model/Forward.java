package fr.lasconic.nwc2musicxml.model;

public class Forward implements IElement{
	private int duration;
	
	public Forward(int duration){
		this.duration = duration;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	
}
