package fr.lasconic.nwc2musicxml.model;

import java.util.ArrayList;

public class Chord implements IElement {
	public ArrayList<Note> notes;

	public Chord() {
		notes = new ArrayList<Note>();
	}

	public void addNote(Note note) {
		notes.add(note);
	}
}
