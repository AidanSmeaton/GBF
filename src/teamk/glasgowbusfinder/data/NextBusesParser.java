package teamk.glasgowbusfinder.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Handle SAX events and parse departure information
 * from nextbuses.mobi
 * 
 * @author Euan Freeman
 */
public class NextBusesParser extends DefaultHandler {
	private boolean inDeparture;
	private boolean inB;
	private boolean inA;
	private boolean haveRouteNumber;
	private String routeNumber;
	private ArrayList<Departure> departures;
	
	public NextBusesParser() {
		super();
		
		prepare();
	}
	
	public ArrayList<Departure> getDepartures() {
		return departures;
	}
	
	public void prepare() {
		inDeparture = false;
		inB = false;
		inA = false;
		haveRouteNumber = false;
		routeNumber = "";
		
		departures = new ArrayList<Departure>(10);
	}
	
	/**
	 * Called when element starts: <foo>
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (localName.equals("span") && attributes != null && attributes.getIndex("id") != -1) {
			inDeparture = true;
		} else if (inDeparture && localName.equals("b")) {
			inB = true;
		} else if (inDeparture && inB && localName.equals("a")) {
			inA = true;
			
			haveRouteNumber = false;
		}
	}

	/**
	 * Called when element closing: </foo>
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (localName.equals("span")) {
			inDeparture = false;
			haveRouteNumber = false;
		} else if (inDeparture && localName.equals("b")) {
			inB = false;
		} else if (inDeparture && inB && localName.equals("a")) {
			inA = false;
		}
	}

	/**
	 * Called to get element contents: 'bar' in <foo>bar</foo>
	 */
	@Override
	public void characters(char[] ch, int start, int length)
	throws SAXException {
		if (inDeparture) {
			String foo = (new String(ch, start, length)).trim();
			
			if (foo.length() != 0) {
				if (inB && inA) {
					haveRouteNumber = true;
					
					routeNumber = foo;
				} else if (haveRouteNumber) {
					Departure newDeparture;
				    
				    if (foo.contains(" at ")) {
				        String[] routeInfo = foo.split(" at ");
				        
				        newDeparture = new Departure(routeNumber, routeInfo[0].trim(), routeInfo[1].trim());
					} else if (foo.contains(" in ")) {
						/* ["Stop", "X mins"] */
					    String[] routeInfo = foo.split(" in ");
					    
					    /* ["X", "mins"] */
					    String[] timeExploded = routeInfo[1].trim().split(" ");
					    
					    int minutesRemaining = Integer.parseInt(timeExploded[0]);
					    
					    GregorianCalendar cal = new GregorianCalendar();
					    cal.add(GregorianCalendar.MINUTE, minutesRemaining);
					    
					    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
					    String time = sdf.format(cal.getTime());
					    
					    newDeparture = new Departure(routeNumber, routeInfo[0].trim(), time);
					} else {
					    return; /* Unexpected sequence */
					}
					
					departures.add(newDeparture);
				}
			}
		}			
	}
}