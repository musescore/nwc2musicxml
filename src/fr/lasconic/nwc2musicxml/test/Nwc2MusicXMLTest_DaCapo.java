package fr.lasconic.nwc2musicxml.test;

import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import fr.lasconic.nwc2musicxml.convert.Nwc2MusicXML;
import fr.lasconic.nwc2musicxml.utils.IOUtils;
import junit.framework.TestCase;

public class Nwc2MusicXMLTest_DaCapo extends TestCase {

	public void testMain() {
		String[] args = new String[2];
		args[0] = "TestFiles/Test DaCapo.nwctxt";
		args[1] = "bin/TestDaCapo.xml";
		Nwc2MusicXML.main(args);
		
		String refFile = "TestFiles/TestDaCapo.xml";
		
		try {
			InputStream in1 = new FileInputStream(args[1]);
			InputStream in2 = new FileInputStream(refFile);
	
			if (!IOUtils.contentExceptEncodingDateEquals(in1, in2))
				fail("Files Different: Da Capo");
		} catch (IOException ioe) {
			fail("IOException " + ioe.getMessage());
		}
		System.out.println("Test Success: Da Capo");
	}
}
