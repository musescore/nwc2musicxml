package fr.lasconic.nwc2musicxml.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Staff {
	public String name;
	public String group;
	public ArrayList<Measure> measures;
	public boolean visible;
	public ArrayList<Lyrics> lyricsLine;
	
	// state
	public Clef currentClef;
	public Key currentKey;
	public Set<Integer> tieList;
	public int[] noteKeys;
	public Note slurStarted;
	public int currentBeamCount;

	public Staff() {
		this.measures = new ArrayList<Measure>();
		this.lyricsLine = new ArrayList<Lyrics>();
		this.currentClef = new Clef();
		this.currentKey = new Key();
		this.tieList = new HashSet<Integer>();
		noteKeys = new int[7];
		this.visible = true;
		this.slurStarted = null;
		this.currentBeamCount = 0;
		this.name = "";
		this.group = "";
	}

	public void addMeasure(Measure measure) {
		measures.add(measure);
	}

	public int length() {
		return measures.size();
	}

	public int[] transformKeyListForTie() {
		int[] noteKeyTmp = new int[7];
		for (int s : tieList) {
			char note = "CDEFGAB".charAt(s);
			int index = Key.SHARPS.indexOf(note);
			if (noteKeys[index] == +1) {
				noteKeyTmp[index] = 1;
			}
			if (noteKeys[index] == -1) {
				noteKeyTmp[index] = -1;
			}
			if (noteKeys[index] == Key.NATURAL) {
				noteKeyTmp[index] = Key.NATURAL;
			}

		}
		noteKeys = noteKeyTmp;
		return noteKeys;
	}

	public static void main(String[] args) {
		Staff staff = new Staff();
		int[] array = { 1, 1, 0, 0, 0, 0, 0 };
		staff.noteKeys = array;
		staff.tieList.add(16);

		for (int i = 0; i < staff.noteKeys.length; i++) {
			System.err.print(staff.noteKeys[i] + " ");
		}
		System.err.println();
		staff.transformKeyListForTie();
		for (int i = 0; i < staff.noteKeys.length; i++) {
			System.err.print(staff.noteKeys[i] + " ");
		}

		System.err.println();
		System.err.println();

		int[] array2 = { 1, 1, 1, 0, 0, 0, 0 };
		staff.noteKeys = array2;
		staff.tieList.add(13);
		staff.tieList.add(18);

		for (int i = 0; i < staff.noteKeys.length; i++) {
			System.err.print(staff.noteKeys[i] + " ");
		}
		System.err.println();
		staff.transformKeyListForTie();
		for (int i = 0; i < staff.noteKeys.length; i++) {
			System.err.print(staff.noteKeys[i] + " ");
		}

		System.err.println();
		System.err.println();

		int[] array3 = { 1, 0, -1, 0, 1, 0, 1 };
		staff.noteKeys = array3;
		staff.tieList.add(13);
		staff.tieList.add(18);

		for (int i = 0; i < staff.noteKeys.length; i++) {
			System.err.print(staff.noteKeys[i] + " ");
		}
		System.err.println();
		staff.transformKeyListForTie();
		for (int i = 0; i < staff.noteKeys.length; i++) {
			System.err.print(staff.noteKeys[i] + " ");
		}

	}
}
