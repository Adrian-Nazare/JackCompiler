import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SymbolTable {
	
	String inputFileName;
	
	//will be used to keep track of the variable's indices in the symbol table
	int staticIndex, fieldIndex, argIndex, localIndex;
	
	//hash maps that point to a 3-item list of type, kind and index for the variables in the symbol tables
	HashMap<String, List<Object>> classTable;
	HashMap<String, List<Object>> subroutineTable;
	
	public SymbolTable (File inputFile) {
		inputFileName = inputFile.getName();
	}
	
	/* creates a new class symbol table each time we start compiling a new class */
	public void startClass() {
		classTable = new HashMap<String, List<Object>>();
		staticIndex = 0;
		fieldIndex = 0;
	}
	
	/* creates a new subroutine symbol table each time we start compiling a new subroutine */
	public void startSubroutine() {
		subroutineTable = new HashMap<String, List<Object>>();
		argIndex = 0;
		localIndex = 0;
	}
	
	/*function that adds a new variable entry to the symbol table */
	public void define (String name, String type, int kind) {
		if (kind == JackCompiler.STATIC){
			classTable.put(name, Arrays.asList(type, kind, staticIndex));
			staticIndex ++;
		}
		else if (kind == JackCompiler.FIELD) {
			classTable.put(name, Arrays.asList(type, kind, fieldIndex));
			fieldIndex ++;
		}
		else if (kind == JackCompiler.ARG){
			subroutineTable.put(name, Arrays.asList(type, kind, argIndex));
			argIndex ++;
		}
		else if (kind == JackCompiler.VAR) {
			subroutineTable.put(name, Arrays.asList(type, kind, localIndex));
			localIndex ++;
		}
		else {
			System.out.println("Invalid kind of variable provided in SymbolTable.define");
			System.exit(0); 
		}
	}
	
	/* returns the number of variables of that kind in the current symbol table */
	public int VarCount (int kind) {	
		if 		(kind == JackCompiler.STATIC) return staticIndex;
		else if (kind == JackCompiler.FIELD)  return fieldIndex;
		else if (kind == JackCompiler.ARG)    return argIndex;
		else if (kind == JackCompiler.VAR)    return localIndex;
		else {
			System.out.println("Invalid kind of variable provided in SymbolTable.VarCount, returning -1");
			return -1;
		}
	}
	
	/*returns the type of the variable names [name] in the current symbol table */
	public String TypeOf (String name) {
		if (subroutineTable.get(name) == null) {
			if (classTable.get(name) == null) {
				System.out.println(String.format("No variable named %s was found in the scope "
						+ "of class %s, returning empty string;", name, inputFileName));
				return "";
			}
			return (String) classTable.get(name).get(0);
		}
		return (String) subroutineTable.get(name).get(0);
	}
	
	/*returns the kind (static, field, argument, local/var) of the variable named [name] in the current symbol table */
	public int KindOf (String name) {
		if (subroutineTable.get(name) == null) {
			if (classTable.get(name) == null)
				return JackCompiler.NONE;
			return (int) classTable.get(name).get(1);
		}
		return (int) subroutineTable.get(name).get(1);
	}
	
	/*returns the index of the variable named [name] in the current symbol table */
	public int IndexOf (String name) {
		if (subroutineTable.get(name) == null) {
			if (classTable.get(name) == null)
			{
				System.out.println(String.format("No variable named %s was found in the scope "
						+ "of class %s, returning -1;", name, inputFileName));
				return -1;
			}
			return (int) classTable.get(name).get(2);
		}
		return (int) subroutineTable.get(name).get(2);
	}
}
