package project;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Handles file name parsing
 * @author Alex Guglielmino 20933584
 * @author Dominic Cockman 20927611
 */

public class GetFileNames {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String str = "-a \"Two Against One.mp3\" \"my file here\"";
		System.out.println("String in testing: " + str);
		
		try {
			String[] filenames = parseString(str);
			for(String s: filenames) {
				System.out.println("Returned filenames: <" + s + ">");
			}
			
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		}
	}
	
	// method here
	public static String[] parseString(String str) throws IllegalArgumentException {
		// Tokens are split using quotation marks
		StringTokenizer strTok = new StringTokenizer(str, "\"");
		// An arraylist is used to store the tokens
		ArrayList<String> tokens = new ArrayList<String>();

		// 2 or 4 tokens required for legal commands
		if(strTok.countTokens() != 4 && strTok.countTokens() != 2)
			throw new IllegalArgumentException("ERROR: Wrong number of tokens");
		
		// add the tokens
		while(strTok.hasMoreTokens()) {
			tokens.add(strTok.nextToken());
		}
		
		// add legal tokens to return string array
		String[] filenames = null;
		// two filenames
		if(tokens.size() == 4) {
			filenames = new String[2];
			filenames[0] = tokens.get(1);
			filenames[1] = tokens.get(3);
			// only one filename
		} else if (tokens.size() == 2) {
			filenames = new String[1];
			filenames[0] = tokens.get(1);
			// wrong number of arguments --> error
		}
		
		return filenames;
	}

}
