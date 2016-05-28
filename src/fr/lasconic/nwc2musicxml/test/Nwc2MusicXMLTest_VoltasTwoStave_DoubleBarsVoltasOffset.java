package fr.lasconic.nwc2musicxml.test;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import fr.lasconic.nwc2musicxml.convert.Nwc2MusicXML;
import fr.lasconic.nwc2musicxml.utils.IOUtils;

public class Nwc2MusicXMLTest_VoltasTwoStave_DoubleBarsVoltasOffset {

	@Test
	public void testMain() {
		String[] args = new String[2];
		args[0] = "TestFiles/Voltas Two Staves 2.nwctxt";
		args[1] = "bin/VoltasTwoStaves2.xml";
		Nwc2MusicXML.main(args);
		
		String refFile = "TestFiles/VoltasTwoStaves2.xml";
		
		try {
			InputStream in1 = new FileInputStream(args[1]);
			InputStream in2 = new FileInputStream(refFile);
	
			if (!IOUtils.contentExceptEncodingDateEquals(in1, in2))
				fail("Files Different: Voltas Two Staves 2");
		} catch (IOException ioe) {
			fail("IOException " + ioe.getMessage());
		}
		System.out.println("Test Success: Voltas Two Staves Double Bars & Voltas Offset");
	}
}
