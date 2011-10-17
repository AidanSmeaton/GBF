package teamk.glasgowbusfinder.data;

/**
 * Represents a departure for a bus. Used by the
 * NextBusesParser to create departure items.
 * 
 * @author Euan Freeman
 */
public class Departure {
	private String service;
	private String destination;
	private String time;
	
	public Departure(String service, String destination, String time) {
		super();
		this.service = service;
		this.destination = destination;
		this.time = time;
	}
	
	public String getService() {
		return service;
	}
	
	public void setService(String service) {
		this.service = service;
	}
	
	public String getDestination() {
		return destination;
	}
	
	public void setDestination(String destination) {
		this.destination = destination;
	}
	
	public void setTime(String time) {
		this.time = time;
	}
	
	public String getTime() {
		return time;
	}
}