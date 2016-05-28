package fr.lasconic.nwc2musicxml.test;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import fr.lasconic.nwc2musicxml.convert.Nwc2MusicXML;
import fr.lasconic.nwc2musicxml.utils.IOUtils;

public class Nwc2MusicXMLTest_TwoStaves_SegnosOffset {

	@Test
	public void test() {
		String[] args = new String[2];
		args[0] = "TestFiles/TestSegnos2.nwctxt";
		args[1] = "bin/TestSegnos2.xml";
		Nwc2MusicXML.main(args);
		
		String refFile = "TestFiles/TestSegnos2.xml";
		
		try {
			InputStream in1 = new FileInputStream(args[1]);
			InputStream in2 = new FileInputStream(refFile);
	
			if (!IOUtils.contentExceptEncodingDateEquals(in1, in2))
				fail("Files Different: Two Staves Segnos Offset");
		} catch (IOException ioe) {
			fail("IOException " + ioe.getMessage());
		}
		System.out.println("Test Success: Two Staves Segnos Offset");
	}
}
