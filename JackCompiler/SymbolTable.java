import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SymbolTable {
	
	final int NONE=26, STATIC=10, FIELD=9, ARG=27, VAR=11;
	String inputFileName;
	
	//List<Object> classListEntries;
	//List<Object> subroutineListEntries;
	int staticIndex, fieldIndex, argIndex, localIndex;
	
	HashMap<String, List<Object>> classTable;// = new HashMap<String, List<Object>>();
	HashMap<String, List<Object>> subroutineTable;// = new HashMap<String, List<Object>>();
	
	public SymbolTable (File inputFile) {
		inputFileName = inputFile.getName();
	}
	
	public void startClass() {
		classTable = new HashMap<String, List<Object>>();
		staticIndex = 0;
		fieldIndex = 0;
	}
	
	public void startSubroutine() {
		subroutineTable = new HashMap<String, List<Object>>();
		argIndex = 0;
		localIndex = 0;
	}
	
	public void define (String name, String type, int kind) {
		if (kind == STATIC){
			classTable.put(name, Arrays.asList(type, kind, staticIndex));
			staticIndex ++;
		}
		else if (kind == FIELD) {
			classTable.put(name, Arrays.asList(type, kind, fieldIndex));
			fieldIndex ++;
		}
		else if (kind == ARG){
			subroutineTable.put(name, Arrays.asList(type, kind, argIndex));
			argIndex ++;
		}
		else if (kind == VAR) {
			subroutineTable.put(name, Arrays.asList(type, kind, localIndex));
			localIndex ++;
		}
		else {
			System.out.println("Invalid kind of variable provided in SymbolTable.define");
			System.exit(0); 
		}
	}
	
	public int VarCount (int kind) {	
		if 		(kind == STATIC) return staticIndex;
		else if (kind == FIELD)  return fieldIndex;
		else if (kind == ARG)    return argIndex;
		else if (kind == VAR)    return localIndex;
		else {
			System.out.println("Invalid kind of variable provided in SymbolTable.VarCount, returning -1");
			return -1;
		}
	}
	
	
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
	
	
	public int KindOf (String name) {
		if (subroutineTable.get(name) == null) {
			if (classTable.get(name) == null)
				return NONE;
			return (int) classTable.get(name).get(1);
		}
		return (int) subroutineTable.get(name).get(1);
	}
	
	
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
