package teamk.glasgowbusfinder.data;

/**
 * Represents a title and an entry.
 * 
 * @author Aidan Smeaton
 */
public class StopInformationPair {
	private String title;
	private String entry;
	
	public StopInformationPair(String title, String entry) {
		this.title = title;
		this.entry = entry;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getEntry() {
		return entry;
	}
	
	public void setEntry(String entry) {
		this.entry = entry;
	}
}