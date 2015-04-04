package fr.lasconic.nwc2musicxml.model2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import fr.lasconic.nwc2musicxml.utils.IOUtils;

public class NWCFile {

	private static final int ERROR_SUCCESS = 0;
	private static final int ERROR_INVALID_DATA = 1;
	public static String COMPRESS_HEADER = "[NWZ]";
	public static String NWC_HEADER = "[NoteWorthy ArtWare]\0\0\0[NoteWorthy Composer]"; // implies
																							// NUL
																							// at
																							// last

	public int load(InputStream is) {
		byte buffer[] = new byte[NWC_HEADER.length()];
		try {
			if (NWC_HEADER.length() == IOUtils.read(is, buffer, 0,
					NWC_HEADER.length())
					|| (!Arrays.equals(buffer, NWC_HEADER.getBytes()))) {
				String buf = new String(buffer);
				if (buf.startsWith(COMPRESS_HEADER)) {
					return loadCompressed(is);
				}
				System.err.println("invalid szNWCHeader");
                return ERROR_INVALID_DATA;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ERROR_SUCCESS;
	}

	private int loadCompressed(InputStream is) {
		// TODO Auto-generated method stub
		return ERROR_SUCCESS;
	}
}
