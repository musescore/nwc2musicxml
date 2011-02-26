package fr.lasconic.nwc2musicxml.model;

public class Lyric {
	public String text;
	public String syllabic; // "single", "begin", "end", or "middle"
	public boolean endLine;
	public boolean endParagraph;

	public Lyric() {
		this.endLine = false;
		this.endParagraph = false;
	}
	
	public Lyric(String text) {
		this();
		this.text = text.replaceAll("_", " ");
		this.text = this.text.replaceAll("\\\\'", "'");
		this.text = this.text.replaceAll("\\\\\"", "\"");
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return text + " syllabic:" + syllabic + " " + endLine + " - "
				+ endParagraph;
	}
}
