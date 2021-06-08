import java.io.*;
class StrCheck
{
public static void main(String args[]) throws IOException
{
StrCheck s= new StrCheck();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("enter string:");
	String exp = reader.readLine();
	
	try {
            reader.readLine();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
 System.out.println("output: " +s.replaceThresholds(exp));
}



public String replaceThresholds(String exp) {
		exp = " " + exp;
		int start = exp.indexOf('[');
		if(start == -1)
			return exp;

		while(start != -1)
		{	
			String var = exp.substring(exp.lastIndexOf(' ',start)+1,start);
			String expr = exp.substring(exp.lastIndexOf(' ',start),exp.indexOf(']')+1);
			String low = exp.substring(start+1, exp.indexOf('.'));
			String high = exp.substring(exp.indexOf('.')+2, exp.indexOf(']'));

			String code = " ( " + var + " >= " + low + " && " + var + " <= " + high + " )";
			String rep = exp.replace(expr,code);
			exp = rep;
			start = exp.indexOf('[');
			System.out.println(var);
		}

		/*while( ind != -1 || exp == "")
		{
			String var = exp.substring(start,exp.indexOf('['));		 
			String low = exp.substring(exp.indexOf('[')+1, exp.indexOf('.'));
			String high = exp.substring(exp.indexOf('.')+2, exp.indexOf(']'));

			code = code + "(" + var + " >= " + low + " && " + var + " <= " + high + ")";
			exp = exp.substring(exp.indexOf(']')+ 1);
 			System.out.println(exp);
			start = exp.indexOf(' ');
			ind = exp.indexOf('[');
			if(ind != -1)
				code = code + exp.substring(start, exp.indexOf(' '));
			 
		}*/
		return exp;
}
}
