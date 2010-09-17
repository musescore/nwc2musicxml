package fr.lasconic.nwc2musicxml.model;

import java.util.ArrayList;
import java.util.HashMap;

public class Measure {
	public ArrayList<IElement> voice1;
	public HashMap<Integer, ArrayList<IElement>> voices;
	public boolean lineBreak;
	public String leftBarType;
	public String rightBarType;
	public boolean leftRepeat;
	public boolean rightRepeat;
	
	public boolean wholeRest; // if we got a whole rest in the measure
	public int notesCount;
	
	public int measureOffset; //keep track of advancement to add forward on voice > 1

	public Measure() {
		voices = new HashMap<Integer, ArrayList<IElement>>();

		voice1 = new ArrayList<IElement>();
		voices.put(1, voice1);
		this.lineBreak = false;
		measureOffset = 0;
		leftRepeat = false;
		rightRepeat = false;
		wholeRest = false;
		notesCount = 0;
	}

	public boolean isFullRest(){
		return wholeRest && notesCount == 1;
	}
	
	public void addElement(IElement element, int voiceNumber) {
		ArrayList<IElement> voice = voices.get(voiceNumber);
		if (voice == null) {
			voice = new ArrayList<IElement>();
			voices.put(voiceNumber, voice);
		}
		
		if(element instanceof Note){
			Note note = (Note)element;
			if(note.firstInChord || note.rest){
				measureOffset += note.getDuration();
			}
		}
		
		voice.add(element);
	}
	
	public boolean isEmpty(){
		boolean result = true;
		for(Integer voiceId: voices.keySet()){
			if (voices.get(voiceId).size()>0){
				result = false;
				break;
			}
		}
		return result;
	}
	
	public boolean isFirstVoice(int voiceId){
		return (voiceId%4 ==1);
	}
}
