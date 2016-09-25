package fr.lasconic.nwc2musicxml.test;

import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import fr.lasconic.nwc2musicxml.convert.Nwc2MusicXML;
import fr.lasconic.nwc2musicxml.utils.IOUtils;

public class Nwc2MusicXMLTest_IncompleteVoice_24 {

	public void testNWCTXT(String name) {
		test(name, "nwctxt");
	}
	public void test(String name, String type) {
		String[] args = new String[2];
		args[0] = "TestFiles/" + name + "." + type;
		args[1] = "bin/" + name + ".xml";
		Nwc2MusicXML.main(args);
		
		String refFile = "TestFiles/" + name +".xml";
		
		try {
			InputStream in1 = new FileInputStream(args[1]);
			InputStream in2 = new FileInputStream(refFile);
	
			if (!IOUtils.contentExceptEncodingDateEquals(in1, in2))
				fail("Files Different: " + name);
		} catch (IOException ioe) {
			fail("IOException " + ioe.getMessage());
		}
		System.out.println("Test Success: " + name);
	}
	
	@Test
	public void test24_AmazingGrace() {
		test("24_AmazingGrace", "nwc");
	}
	@Test
	public void test24_IncompleteVoice_0() {
		testNWCTXT("24_IncompleteVoice_0");
	}
	@Test
	public void test24_IncompleteVoice_1() {
		testNWCTXT("24_IncompleteVoice_1");
	}
	@Test
	public void test24_IncompleteVoice_2staves() {
		testNWCTXT("24_IncompleteVoice_2staves");
	}
}
