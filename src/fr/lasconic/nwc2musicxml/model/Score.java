package fr.lasconic.nwc2musicxml.model;

import java.util.ArrayList;

public class Score {
	public ArrayList<Part> parts;
	public Metadata metadata;

	public Score() {
		parts = new ArrayList<Part>();
		metadata = null;
	}

	public void addPart(Part part) {
		parts.add(part);
	}

	public Part find(String name) {
		Part res = null;
		/*
		 * for (Part part : parts) { if (part.name.compareTo(name) == 0) { res =
		 * part; break; } }
		 */
		return res;
	}

	public boolean checkCoherence() {
		boolean res = true;
		if (parts.size() > 0) {
			Part part = parts.get(0);
			if (part.staves.size() > 0) {
				int length = part.staves.get(0).length();
				for (Part p : parts) {
					for (Staff staff : p.staves) {
						if (length != staff.length()) {
							res = false;
							break;
						}
					}
				}
			} else {
				res = false;
			}
		} else {
			res = false;
		}
		return res;
	}
}
