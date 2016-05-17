package fr.lasconic.nwc2musicxml.model;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Iterator;

public class EndingSet implements IElement {
	private LinkedHashMap<String, String> endings = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> endingCloses = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> segnos = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> doubleBarlines = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> staveLength = new LinkedHashMap<String, String>();
	private int maxStaves = 0;
	private int maxMeasures = 0;
	
	public void addEnding(int staffId, int measureId, String type) {
		String key = String.valueOf(staffId) + ":" + String.valueOf(measureId+1);
		endings.put(key, type);
		if (staffId > maxStaves) {
			maxStaves = staffId;
		}
		if (measureId > maxMeasures) {
			maxMeasures = measureId;
		}
	}
	
	public void addDoubleBarline(int staffId, int measureId, String type) {
		String key = String.valueOf(staffId) + ":" + String.valueOf(measureId+1);
		doubleBarlines.put(key, type);
		if (staffId > maxStaves) {
			maxStaves = staffId;
		}
		if (measureId > maxMeasures) {
			maxMeasures = measureId;
		}
	}
	
	public void addFlow(int staffId, int measureId, String type) {
		String key = String.valueOf(staffId) + ":" + String.valueOf(measureId+1);
		segnos.put(key, type);
		if (staffId > maxStaves) {
			maxStaves = staffId;
		}
		if (measureId > maxMeasures) {
			maxMeasures = measureId;
		}
	}
	
	public void addLastBar(int staffId, int measureId) {
		staveLength.put(String.valueOf(staffId), String.valueOf(measureId+1));
	}
	
	public void prepareEndings() {
		Set<String> keys = endings.keySet();
		Iterator<String> iterator = keys.iterator();
		String lastKey = null;
		String[] split;
		while (iterator.hasNext()) {
			String key = (String)iterator.next();
			// because each staff will start with ending 1, this code should work across staves
			if (lastKey != null) {
				String ending = endings.get(key);
				if (ending.startsWith("1")) {
					// new Ending Set - so close the last one - by adding discontinue in same bar ending started
					split = lastKey.split(":");
					int lastBar = Integer.valueOf(split[1]);
					String newKey = split[0] + ":" + String.valueOf(lastBar);
					endingCloses.put(newKey, endings.get(lastKey) + ":" + "discontinue");
				} else {
					// within Ending Set - close at the bar before the next ending with stop
					split = key.split(":");
					int previousBar = Integer.valueOf(split[1])-1;
					String newKey = split[0] + ":" + String.valueOf(previousBar);
					endingCloses.put(newKey, endings.get(lastKey) + ":" + "stop");
				}
				lastKey = key;
			} else {
				lastKey = key;
				// do nothing - the ending is added when the next one is encountered
			}
		}
		// deal with final ending
		endingCloses.put(lastKey, endings.get(lastKey) + ":" + "discontinue");			
	}
	
	public boolean compareStaves() {
		boolean validFile = true;
		// having stored the instances of voltas, segnos and double bar lines for all staves in the EndingSet LinkedHashMaps
		// go through and ensure the staves aren't different from a flow control perspective.  Rules  are:
		// 	bar lines should be the same
		//	voltas must be the same
		//	segnos must be the same or missing from subsequent staves
		if (maxStaves < 2) {
			// nothing to do - only one stave
		} else {
			String firstStaff;
			String secondStaff;
			for (int s = 2; s <= maxStaves; s++) {
				if (getLastBar(1).compareTo(getLastBar(s)) != 0) {
					System.err.println("   Stave " + s + " (" + getLastBar(s) + ") has a different length to Stave 1 (" + getLastBar(1) + ")");
					validFile = false;
				}
				for (int m = 1; m <= maxMeasures; m++) {
					// check varlines
					firstStaff = getDoubleBar(1, m);
					secondStaff = getDoubleBar(s, m);
					if (hasValue(firstStaff) && !hasValue(secondStaff)) {
						System.err.println("   Barline Error Bar " + m + ": staff 1 has Style|" + firstStaff + " and staff " + s + " does not");
						validFile = false;
					} else if (!hasValue(firstStaff) && hasValue(secondStaff)) {
						System.err.println("   Barline Error Bar " + m + ": staff " + s + " has Style|" + secondStaff + " and staff 1 does not");
						validFile = false;
					} else if (hasValue(firstStaff) && hasValue(secondStaff)) {
						if (firstStaff.compareTo(secondStaff) != 0) {
							System.err.println("   Barline Error Bar " + m + ": staff 1 has Style|" + firstStaff + " and staff " + s + " has Style|" + secondStaff);
							validFile = false;							
						}
					}
					
					// check segnos
					firstStaff = getFlow(1, m);
					secondStaff = getFlow(s, m);
					if (hasValue(firstStaff) && !hasValue(secondStaff)) {
						// valid - ok if the segnos only appear on the first staff
					} else if (!hasValue(firstStaff) && hasValue(secondStaff)) {
						System.err.println("   Flow Error Bar " + m + ": staff " + s + " has " + secondStaff + " and staff 1 does not");
						validFile = false;
					} else if (hasValue(firstStaff) && hasValue(secondStaff)) {
						if (firstStaff.compareTo(secondStaff) != 0) {
							System.err.println("   Flow Error Bar " + m + ": staff 1 has " + firstStaff + " at bar " + m + " has " + secondStaff);
							validFile = false;							
						}
					}
					
					// check endings
					firstStaff = getNWCEnding(1, m);
					secondStaff = getNWCEnding(s, m);
					if (hasValue(firstStaff) && !hasValue(secondStaff)) {
						// valid - ok if the voltas only appear on the first staff
					} else if (!hasValue(firstStaff) && hasValue(secondStaff)) {
						System.err.println("   Volta Error Bar + " + m + ": staff " + s + " has Ending|" + secondStaff + " and staff 1 does not");
						validFile = false;
					} else if (hasValue(firstStaff) && hasValue(secondStaff)) {
						if (firstStaff.compareTo(secondStaff) != 0) {
							System.err.println("   Volta Error Bar " + m + ": staff 1 has Ending|" + firstStaff + " and staff " + s + " has Ending|" + secondStaff);
							validFile = false;					
						}
					}					
				}
			}
		}
		return validFile;
	}
	
	public String getEnding(int staffId, int measureId) {
		String key = staffId + ":" + measureId;
		if (endingCloses.containsKey(key)) {
			return endingCloses.get(key);
		} else {
			return "";
		}
	}
	
	private String getNWCEnding(int staffId, int measureId) {
		String key = staffId + ":" + measureId;
		if (endings.containsKey(key)) {
			return endings.get(key);
		} else {
			return "";
		}
		
	}
	
	public String getEndingNumber(int staffId, int measureId) {
		String[] split = getEnding(staffId, measureId).split(":");
		return split[0];
	}
	
	public String getEndingType(int staffId, int measureId) {
		String[] split = getEnding(staffId, measureId).split(":");
		return split[1];		
	}
	
	private String getFlow(int staffId, int measureId) {
		String key = staffId + ":" + measureId;
		if (segnos.containsKey(key)) {
			return segnos.get(key);
		} else {
			return "";
		}
	}
	
	private String getDoubleBar(int staffId, int measureId) {
		String key = staffId + ":" + measureId;
		if (doubleBarlines.containsKey(key)) {
			return doubleBarlines.get(key);
		} else {
			return "";
		}
	}
	
	private boolean hasValue(String value) {
		if (value.compareTo("") == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	private String getLastBar(int staffId) {
		return staveLength.get(String.valueOf(staffId));
	}
}
