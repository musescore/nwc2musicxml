package fr.lasconic.nwc2musicxml.test;

import static org.junit.Assert.*;

import fr.lasconic.nwc2musicxml.convert.Nwc2MusicXML;
import fr.lasconic.nwc2musicxml.utils.IOUtils;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Test;

public class Nwc2MusicXMLTest_TempoVars {

	@Test
	public void testMain() {
		String[] args = new String[2];
		args[0] = "TestFiles/TempoVars.nwctxt";
		args[1] = "bin/TempoVars.xml";
		Nwc2MusicXML.main(args);
		
		String refFile = "TestFiles/TempoVars.xml";
		
		try {
			InputStream in1 = new FileInputStream(args[1]);
			InputStream in2 = new FileInputStream(refFile);
	
			if (!IOUtils.contentExceptEncodingDateEquals(in1, in2))
				fail("Files Different: TempoVars");
		} catch (IOException ioe) {
			fail("IOException " + ioe.getMessage());
		}
		System.out.println("Test Success: TempoVars");
	}
}
