package fr.lasconic.nwc2musicxml.convert;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import fr.lasconic.nwc2musicxml.model2.NWCFile;

public class Nwc2Txt {
	public static void main(String[] args) throws Exception {
		Nwc2Txt nwc2Txt = new Nwc2Txt();
		nwc2Txt.go2();
	}

	
	
	private void go2() {
		File f = new File("moonlite.nwc");
		try {
			InputStream in
			   = new FileInputStream(f);
			NWCFile file = new NWCFile();
			file.load(in);
		
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void useGoToKillCompilerWarning() throws Exception {
		go();
	}


	private void go() throws Exception {

		File f = new File("moonlite.nwc");
		int l = (int) f.length();
		RandomAccessFile raf = new RandomAccessFile(f, "r");
		byte[] nwcData = new byte[l];
		raf.readFully(nwcData);
		
		ByteBuffer bf = ByteBuffer.wrap(nwcData);
		byte[] fb = new byte[5];
		bf.get(fb);
		String format = new String(fb);
		System.err.println(format);
		System.err.println();

		if ("[NWZ]".compareTo(format) == 0) {
			System.err.println("Compressed NWC detected!");
			System.err
					.println("Dumping to uncompressed NWC format and attemping conversion soon...");
			bf.get();

			byte[] content = new byte[l - 6];
			bf.get(content);
			//String s = new String(content);
			Inflater decompressor = new Inflater();
			decompressor.setInput(content);
			// Create an expandable byte array to hold the decompressed data
			ByteArrayOutputStream bos = new ByteArrayOutputStream(
					content.length);

			// Decompress the data
			byte[] buf = new byte[1024];
			while (!decompressor.finished()) {
				try {
					int count = decompressor.inflate(buf);
					// System.err.println(new String(buf));
					bos.write(buf, 0, count);
				} catch (DataFormatException e) {
					System.err.println("DATA FORMAT EXCEPTION");
					return;
				}
			}
			try {
				bos.close();
			} catch (IOException e) {
			}

			// Get the decompressed data
			nwcData = bos.toByteArray();
		} else if ("[Note]".compareTo(format) != 0) {
			System.err.println("Unknown format");
			raf.close();
			return;
		}

		BufferedWriter out = new BufferedWriter(new FileWriter("test.txt"));
		for (int i = 0; i < nwcData.length; i++) {
			String s = Integer.toHexString(nwcData[i]);
			if(s.length() == 1){
				s = "0"+s;
			}
			//System.err.print(s);
			out.write(s);
			if(nwcData[i] == 0){
				out.write("\n");	
			}
		}
		out.close();
		
		// deal with compiler warning
		raf.close();
		
		ByteBuffer bb = ByteBuffer.wrap(nwcData);
		
		System.err.println(getString(bb,23));
		System.err.println(getString(bb,23));
		byte[] versionBytes = new byte[5];
		bb.get(versionBytes);
		String version = versionBytes[1] +"."+ versionBytes[1];
		System.err.println(version);
		System.err.println(getString(bb,19));
		System.err.println(bb.get()); //?
		System.err.println(bb.get()); //?
		System.err.println("Title : " + getString(bb)); //Title
		System.err.println("Author : " + getString(bb)); //Author
		System.err.println("Lyricist : " + getString(bb)); //Lyricist
		System.err.println("Copyright1 : " + getString(bb)); //Copyright1
		System.err.println("Copyright2 : " + getString(bb)); //Copyright2
		System.err.println("Comments : " + getString(bb)); //Comments
		
		System.err.println(getString(bb)); //Comments
		System.err.println(getString(bb)); //Comments
		System.err.println(getString(bb)); //Comments
		System.err.println(getString(bb)); //Comments
		System.err.println(getString(bb)); //Comments
		System.err.println(getString(bb)); //Comments
		System.err.println(getString(bb)); //Comments
		System.err.println(getString(bb)); //Comments
		System.err.println(getString(bb)); //Comments
		System.err.println(getString(bb)); //Comments
		
	}
	
	
	public String getString(ByteBuffer bb, int count){
		byte[] b = new byte[count];
		bb.get(b);
		
		
		int len = 0;
		int i = 0;
		while(i < b.length && b[i] != 0 ) {
		    len++;
		    i++;
		}

		String result = new String(b, 0, len);
		return result;
	}
	
	public String getString(ByteBuffer bb){
		byte b;
		StringBuilder sb = new StringBuilder();
		while( (b = bb.get()) != 0){
			sb.append((char)b);
		}
		return sb.toString();
	}
}
