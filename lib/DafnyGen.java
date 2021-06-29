import java.io.*;
import java.util.*;
class DafnyGen
{
public static void main(String args[]) throws IOException
{
DafnyGen s= new DafnyGen();
List<String> constants = new ArrayList<String>();
	
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    System.out.println("enter list:");
	
	while (reader.readLine() != null) {
		constants.add(reader.readLine());
		}
	
	try {
            reader.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
 System.out.println("output code: " +s.getConstants(constants));
}



public String getConstants(List<String> constants) {
	String code = "";
		 for (int i = 0; i < constants.size()-1; i++) {			 
			code += constants.get(i) + ": real, ";
		 }
        code += constants.get(constants.size()-1) + ":real ; \n"; 
		return code;
}


public String getErrors(List<String> constants) {
	String code = "";
		 for (int i = 0; i < constants.size()-1; i++) {			 
			code += constants.get(i) + ": real, ";
		 }
        code += constants.get(constants.size()-1) + ":real ; \n"; 
		return code;
}
}
