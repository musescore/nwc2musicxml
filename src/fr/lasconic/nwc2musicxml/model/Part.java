package fr.lasconic.nwc2musicxml.model;

import java.util.ArrayList;

public class Part {
	public String name;
	public String channel;
	public int program;
	public int trans;

	public boolean endRepeat;
	public String barLineStyle; 
	
	public ArrayList<Staff> staves;

	public TimeSig currentTimeSig;
	
	public Part() {
		this.staves = new ArrayList<Staff>();
		this.trans = 0;
		this.program = -1;
		this.endRepeat = false;
		this.barLineStyle = "light-heavy";
		this.currentTimeSig = new TimeSig();
	}

	public void addStaff(Staff staff) {
		staves.add(staff);
	}

	public boolean isVisible() {
		boolean res = true;
		for (Staff staff : staves) {
			if (!staff.visible) {
				res = false;
				break;
			}
		}
		return res;
	}

	public boolean containsStaffForGroup(String group) {
		for (Staff staff : staves) {
			if (staff.group.compareTo(group) == 0)
				return true;
		}
		return false;
	}
}
