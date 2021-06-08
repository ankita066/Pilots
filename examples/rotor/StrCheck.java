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
s.replaceThresholds(exp);
}



public void replaceThresholds(String exp) {
		 
		String low = exp.substring(exp.indexOf('[')+1, exp.indexOf('.'));
		String high = exp.substring(exp.indexOf('.')+2, exp.indexOf(']'));
System.out.println("low=" + low);
System.out.println("high=" + high);
}
}
