package jacoco_console_writer;

public enum Granularity {

	LINE("line"),
	METHOD("method"),
	CLASS("class"),
	PACKAGE("package");
	
	private String granularity;

	Granularity(String granularity) {
		this.granularity = granularity;
	}

	public String getGranularity() {
		return granularity;
	}
	
}
