import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
//import java.util.HashMap;
import java.util.Map;
//import java.util.List;

public class CompilationEngine {
	//Declaring & initialising the constants 
    final int KEYWORD=0, SYMBOL=1, INT_CONST=2, STRING_CONST=3, IDENTIFIER=4;
    final int CLASS=5, CONSTRUCTOR=6, FUNCTION=7, METHOD=8, FIELD=9, STATIC=10, 
    		  VAR=11, INT=12, CHAR=13, BOOLEAN=14, VOID=15, TRUE=16, FALSE=17, 
    		  NULL=18, THIS=19, LET=20, DO=21, IF=22, ELSE=23, WHILE=24, RETURN=25,
    		  NONE=26, ARG=27;
    char[] op = {'+', '-', '*', '/', '&', '|', '<', '>', '='};
    
	Tokenizer tokenizer;
    PrintWriter writer;
    SymbolTable symbolTable;
    Map<Character, String> arithmeticOpMap;
    
    String inputFileName;
    int tokenNumber;
    
    String current; // variable to keep track of the current Token being processed
    int tokenType; //variable to keep track of the current Token type being processed
    
    int keyword; // the numeric code for the keyword, if token is of keyword type
    char symbol; //the char value of the symbol, it token is of symbol type
    //we do not use 'identifier' and 'stringVal' variables, as they are already handled by current
    
    String currentClassName;
    String currentSubroutineName;
    int currentSubroutineType;
    
    String currentSubroutineVarType;
	String currentSubroutineVarName;
	String currentSubroutineReturnType;
	int currentSubroutineArgs, currentSubroutineVars;
	int labelCounter;
    	
	//constructor
	public CompilationEngine(File inputFile, File outputFile) {
        try {
            writer = new PrintWriter(outputFile);    
    		tokenizer = new Tokenizer(inputFile);
    		symbolTable = new SymbolTable(inputFile);
    		
    		inputFileName = inputFile.getName();
    		arithmeticOpMap = Map.of('+', "add", 
    							  '-', "sub",
    							  '*', "call Math.multiply 2",
    							  '/', "call Math.divide 2",
    							  '=', "eq",
    							  '>', "gt", 
    							  '<', "lt", 
    							  '&', "and",
    							  '|', "or");
    		labelCounter = 0;
    		current = tokenizer.getToken();
    		tokenType = tokenizer.getTokenType();
    		if (tokenType == KEYWORD)
    			keyword = tokenizer.getKeyword();
    		else if (tokenType == SYMBOL)
    			symbol = tokenizer.getSymbol();
    		else {
    			System.out.println(String.format("Internal Error: invalid token parsed in file \"%s\" at line %d", inputFileName, tokenizer.getLine()));
    			System.exit(0);
    		}   		
    		
        } catch (IOException e) { e.printStackTrace(); }
	}
	
	/* The main CompilationEngine method */
	public void run() {
		while (tokenizer.hasMoreCommands()) { //checking if there is a valid current command
			//we always start with compiling the class, which handles the syntax analysis and calls other methods to handle, it as needed
			compileClass();
        }
		tokenizer.close();
	}
	
	private void compileClass() {
		symbolTable.startClass();
		
		eatKeyword(CLASS); //we check for the 'class' keyword
		currentClassName = inputFileName.substring(0, inputFileName.lastIndexOf(".vm"));
		eatIdentifier(currentClassName); //we check for and process an identifier for className
		eatSymbol('{');	//we check for and process the opening curly bracket
		
		//as long as we keep encountering 'static' or 'field', it means that we have a class variable declaration, and we keep invoking compileClassVarDec
		while ( (tokenType == KEYWORD) && ((keyword == STATIC) || (keyword == FIELD)) ) {
			compileClassVarDec();
		}
		
		//as long as we keep encountering 'constructor', 'function' or 'method', it means that we have a class variable declaration, and we keep invoking subroutineDec
		while ( (tokenType == KEYWORD) && ((keyword == CONSTRUCTOR) || (keyword == FUNCTION) || (keyword == METHOD)) ) {
			compileSubroutineDec();
		}
		
		eatSymbol('}');	//we check for and process the closing curly bracket

	}
	
	private void compileClassVarDec() {
		int classVarKind = keyword;
		eatKeyword(STATIC, FIELD);

		String classVarType = current;
		eatType();
		
		String classVarName = current;
		eatIdentifier();
		
		symbolTable.define(classVarName, classVarType, classVarKind);
		//while encountering commas, we keep processing the comma, together with an expected variable name
		while ( (tokenType == SYMBOL) && (symbol == ',') ) {
			eatSymbol(',');
			classVarName = current;
			eatIdentifier();
			symbolTable.define(classVarName, classVarType, classVarKind);
		}
		
		eatSymbol(';');
	}
	
	private void compileSubroutineDec() { //WARNING: unfinished
		symbolTable.startSubroutine();
		currentSubroutineArgs = 0; currentSubroutineVars = 0;
		currentSubroutineType = keyword;
		eatKeyword(CONSTRUCTOR, FUNCTION, METHOD); //process the type of subroutine
		
		//process a void, or int/char/boolean/className returning type for the subroutine
		currentSubroutineReturnType = current;
		if ((tokenType == KEYWORD) && (keyword == VOID))
			eatKeyword(VOID);
		else
			eatType();
		
		currentSubroutineName = current;
		eatIdentifier(); //process an identifier for the subroutineName
		eatSymbol('(');
		
		if (currentSubroutineType == METHOD) {
			symbolTable.define("this", currentClassName, ARG);
			currentSubroutineArgs++;
		}
		compileParameterList();
		eatSymbol(')');
		
		
		compileSubroutineBody();	

	}	
	
	private void compileParameterList() {

		// ? -> if we have a token corresponding to a type declaration, the method starts processing the variable declarations
		currentSubroutineVarType = current;
		if (((tokenType == KEYWORD) && ((keyword == INT) || (keyword == CHAR) || (keyword == BOOLEAN))) ||
				(tokenType == IDENTIFIER) ) {
			eatType(); //process the type
			
			currentSubroutineVarName = current;
			eatIdentifier(); //process the variable name
			
			symbolTable.define(currentSubroutineVarName, currentSubroutineVarType, ARG);
			currentSubroutineArgs++;
			
			// * -> if we encounter a comma, it means we have multiple variables, and we keep processing them until no more commas are found
			while ( (tokenType == SYMBOL) && (symbol == ',') ) {
				eatSymbol(',');
				
				currentSubroutineVarType = current;
				eatType();
				currentSubroutineVarName = current;
				eatIdentifier();
				
				symbolTable.define(currentSubroutineVarName, currentSubroutineVarType, ARG);
				currentSubroutineArgs++;
			}
		}	
	}	
	
	private void compileSubroutineBody() {
		
		eatSymbol('{');
		while ((tokenType == KEYWORD) && (keyword == VAR)) {
			compileVarDec();
		}		
		compileStatements();
		eatSymbol('}');
		
		
	}	
	
	private void compileVarDec() {
		eatKeyword(VAR);
		
		currentSubroutineVarType = current;
		eatType();
		
		currentSubroutineVarName = current;
		eatIdentifier();
		
		symbolTable.define(currentSubroutineVarName, currentSubroutineVarType, VAR);
		currentSubroutineVars++;
		
		while ( (tokenType == SYMBOL) && (symbol == ',') ) {
			eatSymbol(',');
			
			currentSubroutineVarName = current;
			eatIdentifier();
			
			symbolTable.define(currentSubroutineVarName, currentSubroutineVarType, VAR);
			currentSubroutineVars++;
		}
		eatSymbol(';');
		
		
	}	
	
	private void compileStatements() {
		
		while ((tokenType == KEYWORD) && ((keyword == LET) || (keyword == IF)
				|| (keyword == WHILE) || (keyword == DO) || (keyword == RETURN))) {
			if (keyword == LET)
				compileLetStatement();
			else if (keyword == IF)
				compileIfStatement();
			else if (keyword == WHILE)
				compileWhileStatement();
			else if (keyword == DO)
				compileDoStatement();
			else //the last choice: if (keyword= = RETURN)
				compileReturnStatement();
		}

	}	
	
	private void compileLetStatement() {
		
		eatKeyword(LET);
		
		String assignmentVariable = current; //we keep track of the variable that is to be assigned
		eatIdentifier();
		
		if ((tokenType == SYMBOL) && (symbol == '[')) { //if an array assignment
			WritePushPop("push", assignmentVariable);//we push the address of the array
			
			eatSymbol('[');
			compileExpression(); //compile the expression1 in-between the brackets
			eatSymbol(']');
			
			writer.format("add\n"); //we add the resulting value to the address of the array
			
			eatSymbol('=');
			compileExpression(); //compile the expression2 on the right side of the equal sign
			
			writer.format("pop temp 0\n" //we pop the resulting value into a temporary value, in case expression 2 used an array
						+ "pop pointer1\n" //we pop the address of assignmentVariable[expression1] into pointer 1
						+ "push temp 0\n" // we again push the saved value of expression 2
						+ "pop that 0\n"); //we pop it into the RAM location that address assignmentVariable[expression1] points to
			
			eatSymbol(';');
		}
		
		else { //if a normal variable assignment
			eatSymbol('=');
			compileExpression();
			WritePushPop("pop", assignmentVariable);
		}
	}	
	
	private void compileIfStatement() {
		
		eatKeyword(IF);
		eatSymbol('(');
		compileExpression();
		eatSymbol(')');
		
		writer.print("not\n");
		writer.format("if-goto IF%d:L1\n", labelCounter);
		
		eatSymbol('{');
		compileStatements();
		eatSymbol('}');		
		
		if ((tokenType == KEYWORD) && (keyword == ELSE)) {
			writer.format("goto IF%d:L2\n", labelCounter);
			writer.format("label IF%d:L1\n", labelCounter);
			
			eatKeyword(ELSE);
			eatSymbol('{');
			compileStatements();
			eatSymbol('}');
			
			writer.format("label IF%d:L2\n", labelCounter);
		}
		else
			writer.format("label IF%d:L1\n", labelCounter);
		
		labelCounter++;
	}	
	
	private void compileWhileStatement() {
		eatKeyword(WHILE);
		writer.format("label WHILE%d:L1\n", labelCounter);
		
		eatSymbol('(');
		compileExpression();
		eatSymbol(')');
		
		writer.print("not\n");
		writer.format("if-goto WHILE%d:L2\n", labelCounter);
		
		eatSymbol('{');
		compileStatements();
		eatSymbol('}');
		
		writer.format("goto WHILE%d:L1\n", labelCounter);
		writer.format("label WHILE%d:L2\n", labelCounter);
		
		labelCounter++;
	}	
	
	private void compileDoStatement() {
		compileSubroutineCall();		
		writer.print("pop temp 0\n"); //we discard the return value of the function in the case of a DO statement
		eatSymbol(';');
		
	}
	
	private void compileSubroutineCall() { //WARNING UNFINISHED
		/*
		eatKeyword(DO);
		int nArgs;//to keep track of the number of arguments

		String className;
		String subroutineName;
		if ( (tokenizer.getToken2Type() == SYMBOL) && (tokenizer.getToken2().charAt(0) == '(') )
		

		if (symbolTable.KindOf(current) != NONE) { //if the current identifier is a variable
			WritePushPop("push", current); //we push variable's address as the first argument of the method
			nArgs++;
			
			className = symbolTable.TypeOf(current);
			
			eatSymbol('.');
			
			subroutineName = current;//the next identifier would be the method's name
			eatIdentifier();
			
			eatSymbol('(');
			nArgs = compileExpressionList(); //compileExpressionList returns the number of expressions in the list, which will become nArgs here
			eatSymbol(')');
			
			writer.format("call %s.%s %d\n", className, subroutineName, nArgs);
		}
		else { //if the current identifier is not a variable, it must be either a class name or a subroutine name;
			className = current; // we assume for now that the class name is the name of our class, and save it here
			eatIdentifier();
			
			if ( (tokenType == SYMBOL) && ((symbol == '(') || (symbol == '.')) ) {//check for correct syntax
				if (symbol == '(') {
					eatSymbol('(');
					nArgs = compileExpressionList(); 
					eatSymbol(')');
					
					writer.format("call %s.%s %d\n", currentClassName, classOrSubroutineName, nArgs);
				}
				else {
					eatSymbol('.');
					
					subroutineName = current;
					eatIdentifier();
					
					eatSymbol('(');
					nArgs = compileExpressionList();
					eatSymbol(')');
					
					writer.format("call %s.%s %d\n", classOrSubroutineName, subroutineName, nArgs);
				}
			}
			else {
				System.out.println(String.format("Syntax Error in file \"%s\" at line %d, expected symbol "
						+ "of type ( or . following subroutine call", inputFileName, tokenizer.getLine()));
				System.exit(0);
			}
			
		}*/
	}
	
	private void compileReturnStatement() {

		eatKeyword(RETURN);
		if (currentSubroutineReturnType == "void") {
			if (isExpression()) {
				System.out.println(String.format("Syntax Error in file \"%s\" at line %d, return value "
						+ "specified despite subroutine returning void", inputFileName, tokenizer.getLine()));
				System.exit(0);
			}
			else {
				writer.print("push constant 0\n"
						   + "return\n");
			}	
		}
		else {
			if (isExpression()) {
				compileExpression();
				writer.print("return\n");
			}
			else {
				System.out.println(String.format("Syntax Error in file \"%s\" at line %d, no return value "
						+ "specified for a non-void subroutine", inputFileName, tokenizer.getLine()));
				System.exit(0);
			}
		}
		
		eatSymbol(';');
		

	}	

	private void compileExpression() {
		
		compileTerm();
		while ((tokenType == SYMBOL) && contains(op, symbol) ) {
			char currentSymbol = symbol;
			
			eatSymbol(symbol);
			compileTerm();
			
			WriteArithmetic(currentSymbol);
		}
		
	}	
	
	private void compileTerm() {
		
		if (tokenType == INT_CONST) {
			String currentInt = current;
			eatIntegerConstant();
			writer.format("push constant %s\n", currentInt);
		}
		else if (tokenType == STRING_CONST) {
			writer.format("call String.new %d\n", current.length());
			for (char c: current.toCharArray()) {
				writer.format("call String.appendChar %d\n", c);
			}
			eatStringConstant();
		}
			
		//if it is a keyword constant:
		else if (tokenType == KEYWORD) {
			if (keyword == TRUE) {
				writer.print("push constant 0\n"
						   + "neg\n");
				eatKeyword(TRUE);
			}
			else if ((keyword == FALSE) || (keyword == NULL)) {
				writer.print("push constant 0\n");
				eatKeyword(FALSE, NULL);
			}
			else if (keyword == THIS) {
				writer.print("push this 0\n");
				eatKeyword(THIS);
			}
			else {
				System.out.println(String.format("Syntax Error in file \"%s\" at line %d, for term declaration "
						+ "as symbol: expected 'true', 'false', 'null' or 'this'", inputFileName, tokenizer.getLine()));
				System.exit(0); 		
			}
		}
		else if (tokenType == IDENTIFIER) {
			//if it is a variable array declaration:
			if ( (tokenizer.getToken2Type() == SYMBOL) && (tokenizer.getToken2().charAt(0) == '[') ) {
				WritePushPop("push", current);
				eatIdentifier();
				
				eatSymbol('[');
				compileExpression();
				eatSymbol(']');
				
				writer.print("add\n"
						   + "pop pointer 1\n"
						   + "push that 0");
			}
			//if it is a subroutine call from the current class, followed by parentheses:
			else if ( (tokenizer.getToken2Type() == SYMBOL) && (tokenizer.getToken2().charAt(0) == '(') ) {
				String subroutineName = current;
				eatIdentifier();
				
				eatSymbol('(');
				int nArgs = compileExpressionList();
				eatSymbol(')');
				
				writer.format("call %s.%s %d\n", currentClassName, subroutineName, nArgs);
			}
			//if it is a subroutine from another class, OR a subroutine called on another object
			else if ( (tokenizer.getToken2Type() == SYMBOL) && (tokenizer.getToken2().charAt(0) == '.') ) {
				if (symbolTable.KindOf(current) == NONE) { //if the identifier is not recognised, it is assumed to be a class name
					
					String className = current;
					eatIdentifier();
					eatSymbol('.');
					
					String subroutineName = current;
					eatIdentifier();
					eatSymbol('(');
					int nArgs  = compileExpressionList();
					eatSymbol(')');
					
					writer.format("call %s.%s %d\n", className, subroutineName, nArgs);
				}
				
				else { // the identifier is recognised as a variable name
					String varName = current;
					String className = symbolTable.TypeOf(current);
					
					WritePushPop("push", varName); // we push the variable itself as the first argument for the function call
					eatIdentifier();
					eatSymbol('.');
					
					String subroutineName = current;
					eatIdentifier();
					eatSymbol('(');
					int nArgs  = compileExpressionList();
					eatSymbol(')');
					
					writer.format("call %s.%s %d\n", className, subroutineName, nArgs + 1);
				}
			}
			//if it was just a variable:
			else {
				WritePushPop("push", current);
				eatIdentifier();
			}
		}
		//if it is another expression:
		else if ((tokenType == SYMBOL) && (symbol == '(')) {
			eatSymbol('(');
			compileExpression();
			eatSymbol(')');
		}
		//if it is a unaryOp term:
		else if ((tokenType == SYMBOL) && ((symbol == '-') || (symbol == '~')) ) {
			if (symbol == '-')
				writer.print("neg\n");
			else 
				writer.print("not\n");
			eatSymbol(symbol);
			compileTerm();
		}
		else {
			System.out.println(String.format("Syntax Error in file \"%s\" at line %d: invalid term declaration", inputFileName, tokenizer.getLine()));
			System.exit(0); 
		}

	}
	
	private int compileExpressionList() {
		int noOfExpressions = 0;
		if (isExpression()) {
			compileExpression();
			noOfExpressions ++;
			
			while ( (tokenType == SYMBOL) && (symbol == ',') ) {
				eatSymbol(',');
				compileExpression();
				noOfExpressions ++;
			}
		}
		return noOfExpressions;
	}	
	
	private boolean eatKeyword(int... correctKeywords) {
		if (tokenType == KEYWORD) {
			for (int correctKeyword: correctKeywords) {
				if (keyword == correctKeyword) {
					advance();
					return true;
				}			
			}
			System.out.println(String.format("Syntax Error in file \"%s\" at line %d, incorrect keyword", inputFileName, tokenizer.getLine()));
			System.exit(0);
			return false;
		}
		System.out.println(String.format("Syntax Error in file \"%s\" at line %d, incorrect token, expected keyword", inputFileName, tokenizer.getLine()));
		System.exit(0);
		return false;
	}
	
	private boolean eatSymbol(char... correctSymbols) {
		if (tokenType == SYMBOL) {
			for (char correctSymbol : correctSymbols) {
				 if (symbol == correctSymbol) { 
					 advance();
					 return true;
				 }
			}
			System.out.println(String.format("Syntax Error: in file \"%s\" at line %d, incorrect symbol, expected one of: ", inputFileName, tokenizer.getLine()));
			System.out.println(correctSymbols);
			System.exit(0);
			return false;
		}
		System.out.println(String.format("Syntax Error: in file \"%s\" at line %d: incorrect token type, expected symbol, one of following: ", inputFileName, tokenizer.getLine(), symbol));
		System.out.println(correctSymbols);
		System.exit(0);
		return false;
	}
	
	private boolean eatIntegerConstant() {
		if (tokenType == INT_CONST) {
			advance();
			return true;}
		else {
			System.out.println(String.format("Syntax Error in file \"%s\" at line %d, expected integer constant", inputFileName, tokenizer.getLine()));
			System.exit(0);
			return false;
		}
	}
	
	private boolean eatStringConstant() {
		if (tokenType == STRING_CONST) {
				advance(); 
				return true;
			}
		else {
			System.out.println(String.format("Syntax Error in file \"%s\" at line %d, expected string constant", inputFileName, tokenizer.getLine()));
			System.exit(0);
			return false;
		}
	}
	
	private boolean eatIdentifier(String... correctIdentifiers) {
		if (tokenType == IDENTIFIER) {
			if (correctIdentifiers.length == 0) {
				advance();	
				return true;
			}
			else if (correctIdentifiers.length != 0) {
				for (String correctIdentifier : correctIdentifiers) {
					if (current == correctIdentifier) {
						advance();	
						return true;
					}
				}
				System.out.println(String.format("Syntax Error in file \"%s\" at line %d, expected one of following identifiers:", inputFileName, tokenizer.getLine()));
				System.out.println(correctIdentifiers);
				return false;
			}
		}
		System.out.println(String.format("Syntax Error in file \"%s\" at line %d, expected identifier", inputFileName, tokenizer.getLine()));
		System.exit(0);
		return false;
	}
	
	private boolean eatType() {
		if (tokenType == KEYWORD)
			return eatKeyword(INT, CHAR, BOOLEAN);
		else if (tokenType == IDENTIFIER)
			return eatIdentifier();
		else {
			System.out.println(String.format("Syntax Error in file \"%s\" at line %d, expected a type "
					+ "declaration (int, char, boolean, or a className)", inputFileName, tokenizer.getLine()));
			System.exit(0);
			return false;
		}
	}
	

	private boolean isExpression() {
		if (tokenType == INT_CONST)
			return true;
		else if (tokenType == STRING_CONST)
			return true;
		else if (
				  (tokenType == KEYWORD) && 
				  ((keyword == TRUE) || (keyword == FALSE) || (keyword == NULL) || (keyword == THIS)) 
				 )
			return true;
		else if (tokenType == IDENTIFIER)
			return true;
		else if (
				 (tokenType == SYMBOL) &&
		  		 ((symbol == '(') || (symbol == '-') || (symbol == '~'))
		  		)
			return true;
		else
			return false;
	}
	
	private boolean contains(char[] opList, char symbolToCheck) {
		for (char c: opList)
			if (symbolToCheck == c)
				return true;
		return false;
	}
	
/*	private void printBody(String input) {

		writer.format(input);
		System.out.print(input);
	}*/
	
/*	private void printToken() {

		writer.format("<%s> %s </%s>\n", tokenizer.getTokenLabel(), tokenizer.getToken(), tokenizer.getTokenLabel());
		System.out.print(String.format("<%s> %s </%s>\n", tokenizer.getTokenLabel(), tokenizer.getToken(), tokenizer.getTokenLabel()));
	}*/
	
	//advances the token offered by the tokenizer, and updates associated variables
	private void advance() {
		tokenizer.advance();
		current = tokenizer.getToken();
		tokenType = tokenizer.getTokenType();
		if (tokenType == KEYWORD)
			keyword = tokenizer.getKeyword();
		else if (tokenType == SYMBOL)
			symbol = tokenizer.getSymbol();
	}
	
	private void WritePushPop(String pushPop, String varName) {
		switch (symbolTable.KindOf(varName)) {
		case STATIC:
			writer.format("%s %s.static %d\n", pushPop, currentClassName, symbolTable.IndexOf(varName));
			return;
		case FIELD:
			writer.format("%s this %d\n", pushPop, symbolTable.IndexOf(varName));
			return;
		case ARG:
			writer.format("%s argument %d\n", pushPop, symbolTable.IndexOf(varName));
			return;
		case VAR:
			writer.format("%s local %d\n", pushPop, symbolTable.IndexOf(varName));
			return;
		}
	}
	
	private void WriteArithmetic(char expressionSymbol) {
		writer.format("%s\n", arithmeticOpMap.get(expressionSymbol));
	}
	
	public void close() {
		writer.close();
	}
}
