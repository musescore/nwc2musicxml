package fr.lasconic.nwc2musicxml.model;

import java.util.ArrayList;

public class Lyrics {

	public ArrayList<Lyric> lyrics;
	public int index;

	public Lyrics() {
		this.lyrics = new ArrayList<Lyric>();
		index = 0;
	}

	public Lyric next() {

		Lyric res = null;
		if (index < lyrics.size()) {
			res = lyrics.get(index);
			index++;
		}
		return res;
	}

	public void parse(String in) {
		String[] sArray = in.split("\\\\r\\\\n|\\\\n");
		for (int i = 0; i < sArray.length; i++) {
			String[] sArray2 = sArray[i].split("\\s");
			for (int j = 0; j < sArray2.length; j++) {
				if (sArray2[j].length() > 0) {
					String[] sArray3 = sArray2[j].split("-");
					if (sArray3.length > 1) {
						for (int k = 0; k < sArray3.length; k++) {
							Lyric lyric = new Lyric(sArray3[k]);
							if (k == 0) {
								lyric.syllabic = "begin";
							} else if (k == sArray3.length - 1) {
								lyric.syllabic = "end";
							} else {
								lyric.syllabic = "middle";
							}
							lyrics.add(lyric);
						}
					} else {
						Lyric lyric = new Lyric(sArray2[j]);
						lyric.syllabic = "single";
						lyrics.add(lyric);
					}
				}
			}
			if (i != sArray.length - 1) {
				Lyric l = lyrics.get(lyrics.size() - 1);
				if (l != null) {
					if (l.endLine) {
						l.endLine = false;
						l.endParagraph = true;
					} else {
						l.endLine = true;
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		String test = "The first Now-ell the an-gel did say\r\nWas__to cer-tain poor shep-herds in fields as they lay;\r\nIn fields where they lay\r\nkeep-ing their sheep,\r\non__a cold win-ter\'s night that was so deep.\r\n\r\nNow-ell, Now-ell, Now-ell, Now-ell,\r\nBorn is the King of Is-ra-el.\r\n";
		Lyrics lyrics = new Lyrics();
		lyrics.parse(test);
		for (Lyric lyric : lyrics.lyrics) {
			System.err.println(lyric);
		}

	}
}
