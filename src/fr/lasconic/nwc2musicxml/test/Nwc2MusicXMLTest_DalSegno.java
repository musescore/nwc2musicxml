package fr.lasconic.nwc2musicxml.test;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import fr.lasconic.nwc2musicxml.convert.Nwc2MusicXML;
import fr.lasconic.nwc2musicxml.utils.IOUtils;

public class Nwc2MusicXMLTest_DalSegno {

	@Test
	public void testMain() {
		String[] args = new String[2];
		args[0] = "TestFiles/Test DalSegno.nwctxt";
		args[1] = "bin/TestDalSegno.xml";
		Nwc2MusicXML.main(args);
		
		String refFile = "TestFiles/TestDalSegno.xml";
		
		try {
			InputStream in1 = new FileInputStream(args[1]);
			InputStream in2 = new FileInputStream(refFile);
	
			if (!IOUtils.contentExceptEncodingDateEquals(in1, in2))
				fail("Files Different: DalSegno");
		} catch (IOException ioe) {
			fail("IOException " + ioe.getMessage());
		}
		System.out.println("Test Success: DalSegno");
	}
}
