package teamk.glasgowbusfinder.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class provides utility functions for formatting
 * time strings, such as ISO-8601 format into a
 * "human readable" format, and vice versa.
 * 
 * @author Aidan Smeaton
 */
public class TimeFormatter {
	/**
	 * Method for changing the format of a string from "HH:mm (Day)" to
	 * a datestring suitable for database, relative to the current date.
	 * 
	 * @param time String representation of time as "HH:mm (Day)" where
	 * Day is a 3-letter abbreviation of a day of the week, e.g. Mon, Wed.
	 * Inclusion of the "Day" is optional.
	 * 
	 * @return Date from time formatted as "yyyy-MM-dd HH:mm" (ISO-8601),
	 * relative from current time.
	 */
	public static String formatTime(String time) {
		/* Create date objects */
		Date now = new Date(System.currentTimeMillis());
		Date busArrival = new Date();
	    
		/* Parse hours and minutes from 'HH:mm' */
		busArrival.setMinutes(Integer.parseInt(time.substring(3,5)));
		busArrival.setHours(Integer.parseInt(time.substring(0,2)));
		busArrival.setDate(now.getDate());
		busArrival.setMonth(now.getMonth());
		busArrival.setYear(now.getYear());
		
		/* If length is greater than 5 ("HH:mm"), a
		 * day is appended to this date - meaning it
		 * is "tomorrow".
		 */
		if (time.trim().length() > 5){
			busArrival.setDate(busArrival.getDate() + 1); // add a day
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		return sdf.format(busArrival);
	}
	
	/**
	 * Extracts the hour and minute fields from
	 * ISO-8601 formatted date, e.g. "15:47"
	 * from "2010-02-14 15:47:20".
	 * 
	 * @param original  Date as 'yy-mm-dddd hh:mm:ss'
	 * 
	 * @return Time as 'hh:mm'
	 */
	public static String getHoursAndMins(String original) {
		return original.substring(11,16);
	}

	/**
	 * Takes a given time string and returns the number of days, hours and minutes
	 * from the current time, and whether or not it preceded the current time
	 * 
	 * @param time  Time as 'yy-mm-dddd hh:mm:ss:
	 * 
	 * @return Components of time as [ days_ago, hours, minutes, was_in_past ].
	 */
	public static long[] getTimeComponents(String time) {

		// create 2 dates - a date for now, and an empty date
		Date now = new Date(System.currentTimeMillis());
		Date alertTime = formattedTimeToDate(time);

		// initialise variables
		long diff = 0;
		long isOld = 0;

		// if the alert time is in the future
		if (now.before(alertTime)){
			diff = alertTime.getTime() - now.getTime();                             
		}
		// if the alert time is in the past
		else {
			diff = now.getTime() - alertTime.getTime();
			isOld = 1;
		}

		// calculate the difference as days, minutes and hours
		long days = (diff / (1000 * 60 * 60 * 24));
		long hours = (diff / (1000 * 60 * 60)) - ((diff / (1000 * 60 * 60 * 24)) * 24);
		long minutes = (diff / ((1000 * 60))) - ((diff / (1000 * 60 * 60)) * 60);

		return new long[] {days, hours, minutes, isOld};
	}

	/**
	 * Constructs a "count-down" until time string from
	 * the number of days, hours and minutes.
	 * 
	 * @param days Number of days
	 * @param hours Number of hours
	 * @param minutes number of minutes
	 * 
	 * @return Time as "x days, y hours, z minutes"
	 */
	public static String writeCountDownString(long days, long hours, long minutes) {

		boolean thereAreDays = (days != 0);
		boolean thereAreHours = (hours != 0);
		boolean thereAreMinutes = (minutes != 0);

		String countDownString = "";

		if (thereAreDays){
			if (days == 1){
				countDownString += (days + " day");
			}
			else {
				countDownString += (days + " days");
			}
			if (thereAreHours || thereAreMinutes){
				countDownString += (", ");
			}
		}
		if (thereAreHours){
			if (hours == 1){
				countDownString += (hours + " hour");
			}
			else {
				countDownString += (hours + " hours");
			}
			if (thereAreMinutes){
				countDownString += (", ");
			}
		}
		if (thereAreMinutes){
			if (minutes == 1){
				countDownString += (minutes + " minute");
			}
			else {
				countDownString += (minutes + " minutes");
			}
		}
		return countDownString;                 
	}
	
	/**
	 * Constructs a new Date using a string
	 * that represents a date using the
	 * date format "yyyy-MM-dd HH:mm"
	 * 
	 * @param time The time represented as a string
	 * 
	 * @return new Date representing the time
	 */
	public static Date formattedTimeToDate(String time) {

		Date dateTime = new Date();
		SimpleDateFormat toFullDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		try {
			dateTime = toFullDate.parse(time);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return dateTime;
		
	}
	
}
