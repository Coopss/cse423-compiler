package edu.nmt.frontend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.nmt.RuntimeSettings;
import edu.nmt.util.IOUtil;

/**
 * Scanner and tokenizer
 * @dated 01/22/20
 * @author Ryan
 *
 */
public class Scanner {
	
	// Class Variables
	private File finp; // input file object
	private static final String tokenOffloadFile = RuntimeSettings.buildDir + "/" + "tokens.txt"; // file to write tokens to
	private static String punctuation = "'!\"#$%&\\'()*+,-./:;<=>?@[\\\\]^_`{|}~"; // punctuation
	private List<Token> tokens; // list of tokens
	
	// Doublepunct cases since special characters have added spaces
	private static final String[][] doublePunctCases = {{"\\+\\s\\+", "\\+\\+"}, // ++
														{"\\-\\s\\-", "\\-\\-"}, // --
														{"\\&\\s\\&", "\\&\\&"}, // &&
														{"\\|\\s\\|", "\\|\\|"}, // ||
														{"\\+\\s\\=", "\\+\\="}, // +=
														{"\\-\\s\\=", "\\-\\="}, // -=
														{"\\*\\s\\=", "\\*\\="}, // *=
														{"\\/\\s\\=", "\\/\\="}, // /=
														{"\\&\\s\\=", "\\&\\="}, // &=
														{"\\%\\s\\=", "\\%\\="}, // %=
														{"\\|\\s\\=", "\\|\\="}, // |=
														{"\\^\\s\\=", "\\^\\="}, // ^=
														{"\\~\\s\\=", "\\~\\="}, // ~=
														{"\\=\\s\\=", "\\=\\="}, // ==
														{"\\>\\s\\>\\s\\=", "\\>\\>\\="}, // >>=
														{"\\<\\s\\<\\s\\=", "\\<\\<\\="}, // <<=
														{"\\<\\s\\=", "\\<\\="}, // <=
														{"\\>\\s\\=", "\\>\\="}, // >=
														{"\\!\\s\\=", "\\!\\="}, // !=
														{"\\-\\s\\>", "\\-\\>"}, // ->
														{"\\>\\s\\>", "\\>\\>"}, // >>
														{"\\<\\s\\<", "\\<\\<"}, // <<
													   };
														
	/**
	 * General constructor
	 * @param file File object to scan
	 * @throws FileNotFoundException
	 */
	public Scanner(File file) throws FileNotFoundException { 
		finp = file;
		if (!finp.exists()) {
			throw new FileNotFoundException();
		}
	}
	
	/**
	 * General constructor
	 * @param fileName Path to the file relative to the cwd
	 * @throws FileNotFoundException
	 */
	public Scanner(String fileName) throws FileNotFoundException { 
		finp = new File(fileName);
		if (!finp.exists()) {
			throw new FileNotFoundException();
		}
	}
	
	/**
	 * Scans the input file into a list of tokens, side effect.
	 * @throws IOException
	 */
	public void scan() throws IOException {
		String fcontents = IOUtil.readFileToString(finp);
		String fcontent_orig = fcontents;
		
		/* Preprocess input prior to tokenization */
			
		// Handle strings
		 Map<String, String> stringLiteralID = new HashMap<String, String>();
		 
		 Matcher m = Pattern.compile("(?s)\\\"[^\\n]*?\\\"").matcher(fcontents);
		 while (m.find()) {
			 String uuid = UUID.randomUUID().toString().replaceAll("-", "");
			 String stringLiteral = m.group();
			 fcontents = fcontents.replace(stringLiteral, uuid);
			 stringLiteralID.put(uuid, stringLiteral);
		 }
		 
		// Handle characters
		 Map<String, String> charLiteralID = new HashMap<String, String>();
		 
		 Matcher m2 = Pattern.compile("(?s)\\\'[^\\n]*?\\\'").matcher(fcontents);
		 while (m2.find()) {
			 String uuid = UUID.randomUUID().toString().replaceAll("-", "");
			 String charConstant = m2.group();
			 fcontents = fcontents.replace(charConstant, uuid);
			 charLiteralID.put(uuid, charConstant);
		 }

		 
//		 Matcher m3 = Pattern.compile("(?s)\\\'[^\\n]*?\\\'").matcher(fcontents);
//		 while (m3.find()) {
//			 String uuid = UUID.randomUUID().toString().replaceAll("-", "");
//			 String stringLiteral = m.group();
//			 fcontents = fcontents.replace(stringLiteral, uuid);
//			 stringLiteralID.put(uuid, stringLiteral);
//		 }
		 
//		 Pattern word = Pattern.compile("\n");
//		 Matcher match = word.matcher(fcontents);
//		 Integer linenum = 0;
//		 Map<String, Integer> lineNumberID = new HashMap<String, Integer>();
//		 
//		 while (match.find()) {
//			 String uuid = UUID.randomUUID().toString().replaceAll("-", "");
//			 fcontents = match.replaceFirst(" " + uuid + " ");
//			 lineNumberID.put(uuid, linenum++);
//			 match = word.matcher(fcontents);
//			 
//		 }
		 
		// Remove single line comments
		fcontents = fcontents.replaceAll("//.*\n", " ");
		
		// Remove multiline comments (non-greedy search, otherwise we might delete code)
		fcontents = fcontents.replaceAll("(?s)/\\*.*?\\*/", " ");
		
		// Add whitespace around punctuation characters
		for (char c : punctuation.toCharArray()) {
			String cs = Character.toString(c);
			fcontents = fcontents.replaceAll("\\" + cs, " " + "\\" + cs + " ");
		}
		
		// Remove repeated whitespace
		fcontents = fcontents.replaceAll("\\s+", " ");

		// Remove whitespace for special punctuation tokens (*=, +=, &&, etc)
		for (int i = 0; i < doublePunctCases.length; i++) {
			fcontents = fcontents.replaceAll(doublePunctCases[i][0], doublePunctCases[i][1]);
		}
		
		// Send processed code to tokenizer
		tokens = tokenize(fcontents);
		
		// Convert id placeholders back to unmodified literals
		for (Token tok : tokens) {
			// Case where the current token is a string literal placeholder
			if (stringLiteralID.containsKey(tok.getTokenString())) {
				tok.setTokenString(stringLiteralID.get(tok.getTokenString()));
				tok.setTokenLabel("string_literal");
			}
			
			// Case where the current token is a char_constant placeholder
			if (charLiteralID.containsKey(tok.getTokenString())) {
				tok.setTokenString(charLiteralID.get(tok.getTokenString()));
				tok.setTokenLabel("char_constant");
			}
			
		}
	}
	
	/**
	 * Scans in a token array from a <>.token file
	 * @param verbose specify verbose printing of matches
	 * @throws IOException
	 */
	public static List<Token> scanfromfile(String fileName) throws IOException {
		String fcontents = IOUtil.readFileToString(new File(fileName));
		ArrayList<Token> tokens = new ArrayList<Token>();
		
		//Set up matching for 2 token fields
		Matcher m = Pattern.compile("(.*) '(.*)'").matcher(fcontents);
		 while (m.find()) {
			 //Add relevant token to list, re-check the token label
			 Token tmp = new Token(m.group(2), null, null);
			 tokens.add(tmp);
		 }
		 
		 for (Token tok : tokens) {
		    	//System.out.println(tok);
		 }
		 
		 return tokens;
	}
	
	/**
	 * Tokenizes a preprocessed code string
	 * @param s string to tokenize
	 * @return A list of token objects
	 */
	private static List<Token> tokenize(String s) {
		ArrayList<Token> tokens = new ArrayList<Token>();
		StringTokenizer st = new StringTokenizer(s);
        
		while (st.hasMoreTokens()) 
        	tokens.add(new Token(st.nextToken(), null, null));
        
        return tokens;
	}
	
	/**
	 * Optionally offload to file
	 * @throws IOException
	 */
	public void offloadToFile() throws IOException {
	    BufferedWriter writer = new BufferedWriter(new FileWriter(tokenOffloadFile));
	    for (Token tok : tokens) {
//	    	System.out.println(tok);
	    	writer.write(tok.toString() + '\n');
	    }     
	    writer.close();
	}
	
	public List<Token> getTokens() {
		return this.tokens;
	}
	
    public static void main(String[] args) throws IOException {
        Scanner s = new Scanner("test/min.c");
        s.scan();
        for (Token tok : s.getTokens()) {
        	System.out.println(tok);
        }
    }
	
	
}