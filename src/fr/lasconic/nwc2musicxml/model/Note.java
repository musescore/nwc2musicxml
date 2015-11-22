package fr.lasconic.nwc2musicxml.model;

import fr.lasconic.nwc2musicxml.convert.IConstants;


public class Note implements IElement {
	
	public String dur;
	private String pos;
	private String opts;

	public boolean chord;
	public boolean firstInChord;
	public boolean rest;

	public int alt;
	public boolean accidental;
	public int rPos;
	public boolean startTie;
	public String stem;
	public String notehead;

	public Note() {
		chord = false;
		firstInChord = false;
		rest = false;
		stem = null;
		notehead = null;
	}

	public int dots() {
		int res = 0;
		if (dur.contains("DblDotted"))
			res = 2;
		else if (dur.contains("Dotted"))
			res = 1;

		return res;
	}

	public void setPos(String pos) {
		this.pos = pos;
		String tmpPos;
		tmpPos = pos;
		char[] c = pos.toCharArray();
		if (c[0] == '#' || c[0] == 'n' || c[0] == 'b' || c[0] == 'x'
				|| c[0] == 'v') {
			if (c[0] == '#')
				this.alt = 1;
			else if (c[0] == 'n')
				this.alt = 0;
			else if (c[0] == 'b')
				this.alt = -1;
			else if (c[0] == 'x')
				this.alt = 2;
			else if (c[0] == 'v')
				this.alt = -2;

			tmpPos = pos.substring(1);
			accidental = true;
		} else {
			accidental = false;
		}

		tmpPos = tmpPos.replaceAll("[^0-9\\-]", "");

		rPos = Integer.parseInt(tmpPos);
			
		int noteheadPos = c.length - 1;
		if (c[c.length - 1] == '^') {
			startTie = true;
			noteheadPos = c.length - 2;
		}
		if(noteheadPos != 0){
			char noteheadChar = c[noteheadPos];
			if(noteheadChar == 'x'){
				notehead = "x";
			}else if(noteheadChar == 'X'){
				notehead = "diamond";
			}else if(noteheadChar == 'z'){
				notehead = "none";
			}else if(noteheadChar == 'o'){
				notehead = "normal";
			}
		}
	}

	public void setOpts(String opts) {
		this.opts = opts;
		if (opts.contains("Stem=Up")) {
			stem = "up";
		} else if (opts.contains("Stem=Down")) {
			stem = "down";
		}
	}

	public boolean isLyricNever() {
		boolean res = false;
		if (opts != null && opts.contains("Lyric=Never"))
			res = true;
		return res;
	}

	public boolean isLyricAlways() {
		boolean res = false;
		if (opts != null && opts.contains("Lyric=Always"))
			res = true;
		return res;
	}
	
	public boolean isBeamed() {
		boolean res = false;
		if (opts != null && opts.contains("Beam"))
			res = true;
		return res;
	}

	public boolean isBeamEnd() {
		boolean res = false;
		if (opts != null && opts.contains("Beam=End"))
			res = true;
		return res;
	}

	public String getType() {
		String res = null;
		if (dur.contains("Whole"))
			res = "whole";
		else if (dur.contains("Half"))
			res = "half";
		else if (dur.contains("64th")) // before 4th...
			res = "64th";
		else if (dur.contains("4th"))
			res = "quarter";
		else if (dur.contains("8th"))
			res = "eighth";
		else if (dur.contains("16th"))
			res = "16th";
		else if (dur.contains("32nd"))
			res = "32nd";
		
		return res;
	}

	public boolean isPartOfTriplet() {
		boolean res = false;
		if (dur.contains("Triplet")) {
			res = true;
		}
		return res;
	}

	public boolean tripletStart() {
		boolean res = false;
		if (dur.contains("Triplet=First")) {
			res = true;
		}
		return res;
	}

	public boolean tripletEnd() {
		boolean res = false;
		if (dur.contains("Triplet=End")) {
			res = true;
		}
		return res;
	}

	public boolean slur() {
		boolean res = false;
		if (dur.contains("Slur")) {
			res = true;
		}
		return res;
	}

	public boolean grace() {
		boolean res = false;
		if (dur.contains("Grace")) {
			res = true;
		}
		return res;
	}

	public int getStemFlagCount() {
		int res = 0;
		
		if (rest)
			;
		else if (dur.contains("8th"))
			res = 1;
		else if (dur.contains("16th"))
			res = 2;
		else if (dur.contains("32nd"))
			res = 3;
		else if (dur.contains("64th")) //before 4th
			res = 4;

		return res;
	}

	public int getDuration() {
		int res = IConstants.DIVISIONS_PER_QUARTER_NOTE;
		if (dur.contains("Whole"))
			res = 4 * IConstants.DIVISIONS_PER_QUARTER_NOTE;
		else if (dur.contains("Half"))
			res = 2 * IConstants.DIVISIONS_PER_QUARTER_NOTE;
		else if (dur.contains("64th")) //before 4th
			res = IConstants.DIVISIONS_PER_QUARTER_NOTE / 16;
		else if (dur.contains("4th"))
			res = IConstants.DIVISIONS_PER_QUARTER_NOTE;
		else if (dur.contains("8th"))
			res = IConstants.DIVISIONS_PER_QUARTER_NOTE / 2;
		else if (dur.contains("16th"))
			res = IConstants.DIVISIONS_PER_QUARTER_NOTE / 4;
		else if (dur.contains("32nd"))
			res = IConstants.DIVISIONS_PER_QUARTER_NOTE / 8;


		int dots = dots();
		if (dots > 0) {
			int dotValue = res;
			for (int i = 0; i < dots(); i++) {
				dotValue /= 2;
				res += dotValue;
			}
		}
		if (isPartOfTriplet()) {
			res = (res * 2) / 3;
		}

		return res;
	}

	public String getStep(Clef clef) {
		String type = "treble";
		if (clef != null) {
			type = clef.type.toLowerCase();
		}
		int n = 6;
		if (type.contains("treble")) {
			n = 6;
		} else if (type.contains("bass")) {
			n = -6;
		} else if (type.contains("alto")) {
			n = 0;
		} else if (type.contains("tenor")) {
			n = 2;
		}
		n += rPos;// number extracted from pos
		char note = (char) ('A' + ((n + 2 + 70) % 7));

		return "" + note;
	}

	public int getOctave(Clef clef) {

		String type = "treble";
		if (clef != null) {
			type = clef.type.toLowerCase();
		}
		int n = 6;
		if (type.contains("treble")) {
			n = 6;
		} else if (type.contains("bass")) {
			n = -6;
		} else if (type.contains("alto")) {
			n = 0;
		} else if (type.contains("tenor")) {
			n = 2;
		}
		n += rPos;// number extracted from pos
		int octave = (70 + n) / 7 - 6;
		octave = octave + clef.octaveShift;
		return octave;
	}

	public boolean isAccent() {
		return dur.contains("Accent");

	}

	public boolean isStaccato() {
		return dur.contains("Staccato");
	}

	public boolean isTenuto() {
		return dur.contains("Tenuto");
	}
}
