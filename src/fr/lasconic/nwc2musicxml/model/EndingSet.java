package fr.lasconic.nwc2musicxml.model;

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Iterator;

public class EndingSet implements IElement {
	private LinkedHashMap<String, String> endings = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> endingCloses = new LinkedHashMap<String, String>();
	
	public void addEnding(int staffId, int measureId, String type) {
		String key = String.valueOf(staffId) + ":" + String.valueOf(measureId+1);
		endings.put(key, type);
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
	
	public String getEnding(int staffId, int measureId) {
		String key = staffId + ":" + measureId;
		if (endingCloses.containsKey(key)) {
			return endingCloses.get(key);
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
}
