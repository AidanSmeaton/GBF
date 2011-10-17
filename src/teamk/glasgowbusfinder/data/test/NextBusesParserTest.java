package teamk.glasgowbusfinder.data.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import teamk.glasgowbusfinder.data.Departure;
import teamk.glasgowbusfinder.data.NextBusesParser;

public class NextBusesParserTest {
	private NextBusesParser parser;
	
	@Before
	public void setUp() throws Exception {
		parser = new NextBusesParser();
	}

	@Test
	public void testCharacters() throws SAXException {
		/* Test case one: Scheduled time */
		char[] one = "<span id='departure-1'><b><a href=''>123</a></b>Glasgow Central at 10:20</span>".toCharArray();
		/* Test case two: Expected time */
		char[] two = "<span id='departure-2'><b><a href=''>123</a></b>Glasgow Central in 6 mins</span>".toCharArray();
		
		AttributesImpl attribOne = new AttributesImpl();
		attribOne.addAttribute("id", "id", "id", "id", "departure-1");
		
		AttributesImpl attribTwo = new AttributesImpl();
		attribTwo.addAttribute("id", "id", "id", "id", "departure-2");
		
		parser.startElement("", "span", "", attribOne);
		parser.startElement("", "b", "", attribOne);
		parser.startElement("", "a", "", attribOne);
		parser.characters(one, 37, 3);
		parser.endElement("", "a", "");
		parser.endElement("", "b", "");
		parser.characters(one, 48, "Glasgow Central at 10:20".length());
		parser.endElement("", "span", "");
		
		Departure departureOne = null;
		
		try {
			departureOne = parser.getDepartures().get(0);
		} catch (IndexOutOfBoundsException e) {
			fail("Departure 1 failed");
		}
		
		assertNotNull(departureOne);
		assertEquals(1, parser.getDepartures().size());
		assertEquals(departureOne.getDestination(), "Glasgow Central");
		assertEquals(departureOne.getService(), "123");
		assertEquals(departureOne.getTime(), "10:20");
		
		parser.startElement("", "span", "", attribTwo);
		parser.startElement("", "b", "", attribTwo);
		parser.startElement("", "a", "", attribTwo);
		parser.characters(two, 37, 3);
		parser.endElement("", "a", "");
		parser.endElement("", "b", "");
		parser.characters(two, 48, "Glasgow Central in 6 mins".length());
		parser.endElement("", "span", "");
		
		Departure departureTwo = null;
		
		try {
			departureTwo = parser.getDepartures().get(1);
		} catch (IndexOutOfBoundsException e) {
			fail("Departure 2 failed");
		}
		
		GregorianCalendar cal = new GregorianCalendar();
		cal.add(GregorianCalendar.MINUTE, 6);
		
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		
		assertNotNull(departureTwo);
		assertEquals(2, parser.getDepartures().size());
		assertEquals(departureTwo.getDestination(), "Glasgow Central");
		assertEquals(departureTwo.getService(), "123");
		assertEquals(departureTwo.getTime(), sdf.format(cal.getTime()));
	}
	
	@After
	public void tearDown() {
		System.out.println("Done");
	}
}