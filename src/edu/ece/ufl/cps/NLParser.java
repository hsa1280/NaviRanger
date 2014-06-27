package edu.ece.ufl.cps;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NLParser 
{
	String[] operations = {"show", "see", "tell", "how", "share", "post", "go"};
	String[] keywords = {"weather", "go", "traffic", "friends", "location"};
	String[] negations = {"no", "not", "remove", "hide"};
	
	public static NLResult parser(String userInput)
	{
		NLResult result = new NLResult();
		
		String [] inputTokens = userInput.split(" ");
		int inputLength = inputTokens.length;
		userInput.toLowerCase();
		
		if(userInput.contains("show") || userInput.contains("see") || userInput.contains("how"))
		{
			/* show me weather == showWeather(current) */
			if(userInput.contains("weather") && !(userInput.matches(".*[^A-Za-z](of|for|in|at)[^A-Za-z].*")))
			{
				result.functor = "showWeather";
				result.arg1 = "current";
				result.arg2 = null;
				
				return result;
			}
			/* show me weather of Boston== showWeather(Boston) */
			else if(userInput.contains("weather") && ((userInput.matches(".*[^A-Za-z](of|for|in|at)[^A-Za-z].*"))))
			{
				result.functor = "showWeather";
				result.arg1 = inputTokens[inputLength-1];
				result.arg2 = null;
				
				return result;
			}
			
			/* If user says - show me traffic, I want to see traffic then functor == showTraffic*/
			if(userInput.contains("traffic") && !((userInput.matches(".*[^A-Za-z](not|hide|remove)[^A-Za-z].*"))))
			{
				result.functor = "showTraffic";
				result.arg1 = null;
				result.arg2 = null;
				
				return result;
			}
			else if(userInput.contains("traffic") && ((userInput.matches(".*[^A-Za-z](not|hide|remove)[^A-Za-z].*"))))
			{
				result.functor = "removeTraffic";
				result.arg1 = null;
				result.arg2 = null;
				
				return result;
			}
		}
		
		/* Route pattern matching examples 
		 * show me how can i go from xxxx to yyyy
		 * i want to go to abcd
		 * i want to go from abcd to xyz  */
		
		/* Pattern: something something FROM xxxx TO yyyy
		 * Source comes before Destination*/
		if(userInput.matches(".*(route|go|way).*") && userInput.matches(".*(from).*(to).*"))
		{
			int goFromIndex = userInput.indexOf(" go from ");
			int toIndex = userInput.indexOf(" to ", goFromIndex);
			String source = userInput.substring(goFromIndex + 9, toIndex);
			String dest = userInput.substring(toIndex+4);
			result.functor = "drawRoute";
			
			if(source.contains("here")||source.contains("current")||source.contains("my"))
			{
				result.arg1 = "current location";
			}
			else
			{
				result.arg1 = source;
			}
			result.arg2 = dest;
			
			return result;
		}
		
		/* Route pattern matching examples 
		 * show me how can i go to xxxx from yyyy
		 * i want to go to abcd
		 * i want to go from abcd to xyz  */
		
		/* Pattern: something something TO xxxx FROM yyyy
		 * Destination comes before Source*/
		if(userInput.matches(".*(route|go|way).*") && userInput.matches(".*(to).*(from).*"))
		{
			int toIndex = userInput.indexOf(" to ");
			int fromIndex = userInput.indexOf(" from ", toIndex);
			String dest = userInput.substring(toIndex + 4, fromIndex);
			String source = userInput.substring(fromIndex+6);
			result.functor = "drawRoute";
			
			if(source.contains("here")||source.contains("current")||source.contains("my"))
			{
				result.arg2 = "current location";
			}
			else
			{
				result.arg2 = source;
			}
			result.arg1 = dest;
			
			return result;
		}
		
		if(userInput.matches(".*(route|go|way).*") && userInput.contains(" to ") && !userInput.contains(" from "))
		{
			int toIndex = userInput.indexOf(" to ");
			String source = "current location";
			String dest = userInput.substring(toIndex+4, userInput.length());
			result.functor = "drawRoute";
			result.arg1 = source;
			result.arg2 = dest;
			return result;
		}
		
		if(userInput.matches(".*(tell|share|post).*(where|location).*"))
		{
			result.functor = "fbShare";
			return result;
		}
		
		return result;	
	}
}