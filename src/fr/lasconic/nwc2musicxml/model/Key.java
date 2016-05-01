package fr.lasconic.nwc2musicxml.model;

public class Key implements IElement{
	int[] keys;
	public static final String SHARPS = "FCGDAEB";
	public static final int NATURAL = 1000;
	public Key() {
		keys = new int[7]; // F C G D A E B
		for (int i = 0; i < keys.length; i++) {
			keys[i] = 0;
		}
	}

	public void parse(String k) {
		
		k = k.toUpperCase();
		String[] sArray = k.split(",");
		for (int i = 0; i < sArray.length; i++) {
			String key = sArray[i];
			if (key.length() == 2) {
				char c1 = key.charAt(0);
				char c2 = key.charAt(1);
				int index = SHARPS.indexOf(c1);
				if (index != -1) {
					if (c2 == '#') {
						keys[index] = 1;
					} else if (c2 == 'B') { // upper case
						keys[index] = -1;
					}
				}
			}
		}
	}

	public int getAlterForStep(String step){
		int result = 0;
		int index = SHARPS.indexOf(step);
		if (index != -1)
			result = keys[index];
		
		return result;
	}
	
	public static int getAlterForStep(String step, int[] noteKeys){
		int result = 0;
		int index = SHARPS.indexOf(step);
		if (index != -1)
			result = noteKeys[index];
		
		return result;
	}
	
	// mode is assumed to be major
	public int getFifth() {
		int fifth = 0;
		boolean found = false;
		for (int i = 0; i < keys.length; i++) {
			if (keys[i] != 0) {
				found = true;
				break;
			}
		}

		if (!found) {
			return 0;
		}
		found = false;

		int ref = keys[0];
		if (ref == 0) {
			// try flats
			int last = keys.length - 1;
			int ref2 = keys[last];
			if (ref2 == -1) {
				int i;
				for (i = last - 1; i > 0; i--) {
					if (keys[i] != -1) {
						break;
					}
				}
				// check the following // has to be 0
				for (int j = i; j > 0; j--) {
					if (keys[j] != 0) {
						found = true;
						break;
					}
				}
				if (!found)
					fifth = i - 6;
				else
					fifth = 0; // custom key signature
			}
		} else if (ref == 1) {
			int i;
			for (i = 1; i < keys.length; i++) {
				if (keys[i] != 1) {
					break;
				}
			}
			for (int j = i; j < keys.length; j++) {
				if (keys[j] != 0) {
					found = true;
					break;
				}
			}
			if (!found)
				fifth = i;
			else
				fifth = 0; // custom key signature
		} else if (ref == -1) {
			// All flats
			for (int i = 1; i < keys.length; i++) {
				if (keys[i] != -1) {
					found = true;
					break;
				}
			}
			if (!found) {
				fifth = -7;
			}
		}

		return fifth;
	}

	public String getMode() {
		return "major";
	}

	public static void main(String[] args) {
		Key key = new Key();
		key.parse("Bb,Eb");
		System.err.println(key.getFifth());
		// --- -2 ---/

		key = new Key();
		key.parse("Bb,Db");
		System.err.println(key.getFifth());
		// --- 0 ---/

		key = new Key();
		key.parse("F#,C#");
		System.err.println(key.getFifth());
		// --- 2 ---/

		key = new Key();
		key.parse("F#,D#");
		System.err.println(key.getFifth());
		// --- 0 ---/

		key = new Key();
		key.parse("Bb,Eb,Ab,Db,Gb,Cb,Fb");
		System.err.println(key.getFifth());
		// --- -7 ---/

		key = new Key();
		key.parse("F#,C#,G#,D#,A#,E#,B#");
		System.err.println(key.getFifth());
		// --- 7 ---/

	}

}
