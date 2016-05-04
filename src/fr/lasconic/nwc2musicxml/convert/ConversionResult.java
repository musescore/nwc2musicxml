package fr.lasconic.nwc2musicxml.convert;

public class ConversionResult {
	public static final int ERROR_OLD_VERSION = -2;
	public static final int ERROR = -1;
	public static final int CONTINUE = 0;
	public static final int END_OF_FILE = 1;
	
	int errorCode;
	String title;
	
	public ConversionResult() {
		super();
		title = null;
		errorCode = ERROR;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public boolean isError() {
		return errorCode < CONTINUE;
	}
	
}
