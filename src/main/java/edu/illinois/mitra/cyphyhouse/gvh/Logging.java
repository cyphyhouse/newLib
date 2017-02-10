package edu.illinois.mitra.cyphyhouse.gvh;

public abstract class Logging {
	
	public abstract void e(String tag, String msg);
	
	public abstract void i(String tag, String msg);
	
	public abstract void d(String tag, String msg);

	public abstract String getLog();

	//TODO: we should save another log maybe?
    public void saveLogFile() {
    }
}
