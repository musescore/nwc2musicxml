package fr.lasconic.nwc2musicxml.model;

public class Flow implements IElement {
	public String flow;
	
	public String getTag() {
		if (flow != null) {
			if (flow.startsWith("Coda")) {
				return "coda";
			} else if (flow.startsWith("Segno")) {
				return "segno";
			}
		}
		return "";
	}
	
	public String getWords() {
		if (flow != null) {
			if (flow.startsWith("DSalCoda")) {
				return "D.S. al Coda";
			} else if (flow.startsWith("DSalFine")) {
				return "D.S. al Fine";
			} else if (flow.startsWith("DCalCoda")) {
				return "D.C. al Coda";
			} else if (flow.startsWith("DCalFine")) {
				return "D.C. al Fine";
			} else if (flow.startsWith("Fine")) {
				return "Fine";
			} else if (flow.startsWith("ToCoda")) {
				return "To Coda";
			} else if (flow.startsWith("DalSegno")) {
				return "D.S.";
			} else if (flow.startsWith("DaCapo")) {
				return "D.C.";
			}
		}
		return "";
	}
	
	public String getSoundNodeNum() {
		if (flow != null) {
			if (flow.startsWith("Coda")) {
				return "coda";
			} else if (flow.startsWith("DSalCoda") || flow.startsWith("DSalFine")) {
				return "dalsegno";
			} else if (flow.startsWith("Fine")) {
				return "fine";
			} else if (flow.startsWith("Segno")) {
				return "segno";
			} else if (flow.startsWith("DalSegno")) {
				return "dalsegno";
			}
		}
		return "";
	}

	public String getSoundNodeYes() {
		if (flow != null) {
			if (flow.startsWith("DCalCoda") || flow.startsWith("DCalFine")) {
				return "dacapo";
			} 
		}
		return "";
	}
}

