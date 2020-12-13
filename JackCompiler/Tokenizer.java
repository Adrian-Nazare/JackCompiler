import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Tokenizer {

	FileReader reader = null;
	String inputFileName;
	
	String token; int tokenType;
	String token2; int token2Type;
	
	String word; //string to store parts of the code, if more tokens are written without white spaces in-between
	int current; //the last read character in numeric ASCII form, used as a variable and is to be appended to word before reading further 
	int next; //same as current, used to look ahead for block comments
	int line; //keeps track of the line we are currently parsing
	int index; //keeps track of the index at which a symbol is found within a word
	boolean isTokens, isWhitespace, isEOLine, isString, isComment, isEOF; //flags to keep track of what structure we are parsing
	char[] symbolList = {'{', '}', '(', ')', '[', ']', '.', ',', ';', '+', '-', '*', '/', '&', '|', '<', '>', '=', '~'};
	String [] KeywordList = {"class", "constructor", "function", "method", "field", "static", "var", "int", "char", "boolean", 
							 "void", "true", "false", "null", "this", "let", "do", "if", "else", "while", "return",};
	
	//constructor
	public Tokenizer(File inputFile) {
		try {
			reader = new FileReader (inputFile);
			current = reader.read();
			} catch (IOException e) { e.printStackTrace(); }
		
		inputFileName = inputFile.getName();
		line = 1;
		token = ""; token2 = ""; word = "";
		advance(); //token = token2, which is still "".  token2 = first token in the program
		advance(); //token = token 2, which is the first token in the program.  token 2 = the next token after the previous
	}
	
	public boolean hasMoreCommands() {
		if ((token == "") || (token == null)) {
			return false;
		}
		else {
			return true;
		}
	}
	
	/* Updates the tokens with the next ones: token becomes token2, 
	 * and the following token2 is computed, same for the labels */
	public void advance() {
		token = token2;
		tokenType = token2Type;
				
		if (word.length() > 0) {
			parseWord();
		}
		else {
			checkFlags();
			
			if (isTokens)
				processTokens();
			else if (isWhitespace)
				processWhitespace();
			else if (isEOLine)
				processEOLine();
			else if (isString)
				processString();
			else if (isComment)
				processComment();
			else if (isEOF)
				processEOF();
			else {
				System.out.println (String.format("Error: no flag raised when parsing line %d in file \"%s\"", line, inputFileName));
				try{current = reader.read();} catch (IOException e) { e.printStackTrace(); }  
				advance();
			}
		}
	}

	/* Method for parsing a word containing potentially multiple tokens */
	private void parseWord() {
		//First we check to see if the first char of the word is a symbol, in which case we process it directly
		for (char c: symbolList) {
			if (word.charAt(0) == c) {
				token2 = "" + c;
				token2Type = JackCompiler.SYMBOL;
				word = word.substring(1);
				return;
			}
		}
		//In case the symbol is not the first character, we call the processToken method to extract the token before the first symbol
		//If processToken successfully extracts a token, we delete that token from the tokens word and return
		for (index = 1; index < word.length(); index++) {
			for (char c: symbolList) {
				if (word.charAt(index) == c) {
					if (processToken(word.substring(0, index))) {
						word = word.substring(index);
						return;
					}
					else {
						//if an invalid token was found, we recursively call the parseWord method again to get the next token that will be processed
						word = word.substring(index);
						parseWord();
						return;
					}	
				}
			}
		}
		//If no symbols were found within the word, we process the word directly as a single token
		if (processToken(word)) {
			word = "";
		}
		//if not token is extracted from the word, we recursively call advance again
		else {
			word = "";
			advance();
		}
	}
	
	//Extracts a token from a string containing only one, and returns true if successfully extracted
	private boolean processToken(String wordSection) {
		for (String s: KeywordList) {
			if (wordSection.equals(s)) {
				token2 = s;
				token2Type = JackCompiler.KEYWORD;
				return true;
			}
		}
		if (isNumeric(wordSection)) {
			token2 = wordSection;
			token2Type = JackCompiler.INT_CONST;
			return true;
		}
		else if ((Character.isLetter(wordSection.charAt(0))) || (wordSection.charAt(0) == '_') ) {
			token2 = wordSection;
			token2Type = JackCompiler.IDENTIFIER;
			return true;
		}
		else {
			System.out.println (String.format("Invalid token encountered in file \"%s\" in line line %d", inputFileName, line));
			return false;
		}
	}
	
	//private method for checking if a string is a valid integer number
	private boolean isNumeric (String wordSection) {
		for (int i = 0; i < wordSection.length(); i++) {
			if ( !(Character.isDigit(wordSection.charAt(i))) ) {
				return false;
			}
		}
		return true;
	}
	
	//check the current character to see what structure we are dealing with
	private void checkFlags() {
		//in case we are dealing with a Slash/divide symbol that is not part of a comment declaration, the
		//comment-processing part of the method will be dealing with it and updating the isTokens Flag to true instead
		if ( (current == 38) || (current == 93) || (current == 95) || ((current > 39) && (current < 47))
				|| ((current > 47) && (current < 63)) || ((current > 64) && (current < 92)) || ((current > 96) && (current < 127)) ) {
			isTokens = true;
			return;
		}
		if ((current == ' ') || (current == '\t')) {
			isWhitespace = true;
			return;
		}
		if ((current == '\n') || (current == '\r')) {
			isEOLine = true;
			return;
		}
		if (current == '"') {
			isString = true;
			return;
		}
		if (current == '/') {
			try {
				//in case of a slash, we read in advance another character, 
				current = reader.read();
				if ((current == '/') || (current == '*')) {
					isComment = true;
					return;
				}
				//if it is not part of a comment declaration, we set isTokens flag to true 
				// and append the slash to the word here, so that it is not lost before processTokens() 
				// starts appending characters to the word starting with the character *after* the slash
				else {
					isTokens = true;
					word = "/";
				}					
			} catch (IOException e) { e.printStackTrace(); } 
		}
		if (current == -1) {
			isEOF = true;
		}
	}
	

	private void processTokens() {
		try {
			//we filter out invalid characters, numerical codes stand for ASCII characters
			while ( (current == 38) || (current == 93) || (current == 95) || ((current > 39) && (current < 63)) || ((current > 64) && (current < 92)) || ((current > 96) && (current < 127)) ) {	
				word += (char) current;
				current = reader.read();
			}
			isTokens = false; 
			advance(); //we recursively call advance() again, since this time it will have a word to parse for non-string tokens
		} catch (IOException e) { e.printStackTrace(); } 
	}
	
	private void processWhitespace() {
		try {
			while ((current == ' ') || (current == '\t')) {
				current = reader.read();
			}
			isWhitespace = false;
			advance();
		} catch (IOException e) { e.printStackTrace(); } 
	}
	
	private void processEOLine() {
		try {
			while ((current == '\n') || (current == '\r')) {
				if (current == '\n')
					line ++;
				current = reader.read();
			}
			isEOLine = false;
			advance();
		} catch (IOException e) { e.printStackTrace(); } 
	}
	
	private void processString() {
		try {
			token2 = "";
			token2Type = JackCompiler.STRING_CONST;
			current = reader.read();
			while ((current != '\n') && (current != '\r') && (current != '"')) {
				token2 += (char) current;
				current = reader.read();
			}
			isString = false;
			current = reader.read();
			//we do not call advance(), since the method would have successfully updated the token this time
		} catch (IOException e) { e.printStackTrace(); } 
	}
	
	private void processComment() {
		try {
			if (current == '/') { //if line comment, we continue to read until the end of line
				while ((current != '\n') && (current != '\r')) {
					current = reader.read();
				}				
			}
			else if ((current == '*')) {
				current = reader.read();
				next = reader.read();
				while (( !((current == '*') && (next == '/')) ) && (current != -1) ){
					current = next;
					next = reader.read();
					if  (current == '\n') 
						line ++;
				}
				current = reader.read();
			}
			else {
				System.out.println (String.format("Invalid comment encountered in file \"%s\" in line %d", inputFileName, line));
			}
			isComment = false;
			advance();
		} catch (IOException e) { e.printStackTrace(); } 
	}
	
	private void processEOF() {
		token2 = null;
		token2Type = -1;
	}
	
	
	public String getToken() {
		return token;
	}
	
	public String getToken2() {
		return token2;
	}
	
	public int getTokenType() {
		return tokenType;
	}
	
	public int getToken2Type() {
		return token2Type;
	}
	
	//Used for printing out the labels of the tokens by the CompilationEngine
	public String getTokenLabel() {
		switch (tokenType) {
		case JackCompiler.KEYWORD:
			return "keyword";
		case JackCompiler.SYMBOL:
			return "symbol";
		case JackCompiler.INT_CONST:
			return "integerConstant";
		case JackCompiler.STRING_CONST:
			return "stringConstant";
		case JackCompiler.IDENTIFIER:
			return "identifier";
		default:
			return "DEFAULT";
		}
	}
	
	public int getKeyword() {
		switch (token) {
		case "class": return JackCompiler.CLASS;
		case "constructor":	return JackCompiler.CONSTRUCTOR;
		case "function": return JackCompiler.FUNCTION;
		case "method": return JackCompiler.METHOD;
		case "field": return JackCompiler.FIELD;
		case "static": return JackCompiler.STATIC;
		case "var": return JackCompiler.VAR;
		case "int": return JackCompiler.INT;
		case "char": return JackCompiler.CHAR;
		case "boolean": return JackCompiler.BOOLEAN;
		case "void": return JackCompiler.VOID;
		case "true": return JackCompiler.TRUE;
		case "false": return JackCompiler.FALSE;
		case "null": return JackCompiler.NULL;
		case "this": return JackCompiler.THIS;
		case "let": return JackCompiler.LET;
		case "do": return JackCompiler.DO;
		case "if": return JackCompiler.IF;
		case "else": return JackCompiler.ELSE;
		case "while": return JackCompiler.WHILE;
		case "return": return JackCompiler.RETURN;
		default: return -1;
		}
	}

	public char getSymbol() {
		return token.charAt(0);
	}

	public String getIdentifier() {
		return token;
	}
	
	public int getIntVal() {
		return Integer.valueOf(token);
	}
	
	public String getStringVal() {
		return token;
	}
	
	public int getLine() {
		return line;
	}
	
    public void close(){
        try {
        	System.out.println(String.format("End of input file \"%s\" reached, attempting to close...", inputFileName));
        	reader.close();
        	System.out.println(String.format("Input file \"%s\" closed successfully", inputFileName));
        } catch (IOException logOrIgnore) {}
    }
}
