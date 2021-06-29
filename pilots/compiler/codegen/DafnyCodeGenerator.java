package pilots.compiler.codegen;

import java.io.*;
import java.util.*;
import java.util.logging.*;

import pilots.compiler.parser.*;
import pilots.runtime.*;

import net.sourceforge.argparse4j.inf.Namespace;


public class DafnyCodeGenerator implements PilotsParserVisitor {
    private static Logger LOGGER = Logger.getLogger(PilotsCodeGenerator.class.getName());

    private static final String TAB = "    ";
    private static int indent = 0;

    private String appName = null;
    private List<InputStream> inputs = null;
    private List<String> constants = null;
	private String consts = "";
	private String constVars = "";
    private List<OutputStream> outputs = null;
    private List<OutputStream> errors = null;
    private List<Signature> sigs = null;
    private List<Mode> modes = null;
    private List<String> models = null;
    private Map<Integer, List<Correct>> corrects = null;    // key: mode, val: list of corrects
    private String code = null;
    private Map<String, String> varsMap = null;
    private Namespace opts = null;
    private int minInterval = Integer.MAX_VALUE;

    private static int depth = 0;
    
    public static void main(String[] args) {
        try {
            PilotsParser parser = new PilotsParser(new FileReader(args[0]));
            Node node = parser.Pilots();
            DafnyCodeGenerator visitor = new DafnyCodeGenerator();
            node.jjtAccept(visitor, null);
        } 
        catch (FileNotFoundException ex) {
            LOGGER.severe(ex.toString());
        }
        catch (TokenMgrError ex) {
            LOGGER.severe(ex.toString());            
        }
        catch (ParseException ex) {
            LOGGER.severe(ex.toString());                        
        }
    }

    public DafnyCodeGenerator() {
        inputs =  new ArrayList<>();
        constants = new ArrayList<>();
        outputs = new ArrayList<>();
        errors = new ArrayList<>();
        sigs = new  ArrayList<>();
        modes = new ArrayList<>();
	models = new ArrayList<>();
        corrects = new HashMap<>();
        code = new String();
        varsMap = new HashMap<>(); // Store variables in inputs
    }
	    public void setOptions(Namespace opts) {
        this.opts = opts;
    }

    private void goDown(String node) {
        /*
        String msg = "";
        for (int i = 0; i < depth; i++)
            msg += " ";
        LOGGER.finest(msg + node);
        */
        depth++;
    }

    private void goUp() {
        depth--;
    }

    private void incIndent() {
        indent++;
    }

    private void decIndent() {
        indent--;
    }

    private String insIndent() {
        String tab = "";
        for (int i = 0; i < indent; i++)
            tab += TAB;
        return tab;
    }

    private String incInsIndent() {
        incIndent();
        return insIndent();
    }

    private String decInsIndent() {
        decIndent();
        return insIndent();
    }

    private void acceptChildren(SimpleNode node, Object data) {
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            node.jjtGetChild(i).jjtAccept(this, data);
        }
    }
	   private void generateConstantsList() {
		 for (int i = 0; i < constants.size()-1; i++) {			 
			consts += constants.get(i) + ": real, ";
			constVars +=  constants.get(i) + ", ";
		 }
        consts += constants.get(constants.size()-1) + ": real ";
		constVars += constants.get(constants.size()-1); 
   
    }
	
	    public void generateErrors() {

        code += insIndent() + "datatype Error = ErrorVal ("; 
		
        for (int i = 0; i < errors.size()-1; i++) {			 
			code += insIndent() + (errors.get(i)).getVarNames()[0] + ": real, ";
			varsMap.put((errors.get(i)).getVarNames()[0], "e." + (errors.get(i)).getVarNames()[0]);
		 }
        code += insIndent() + (errors.get(errors.size()-1)).getVarNames()[0] + ":real) //Error functions \n"; 
		varsMap.put((errors.get(errors.size()-1)).getVarNames()[0], "e." + (errors.get(errors.size()-1)).getVarNames()[0]);

    }
	
	
	 private void generateModesErrorDetection() {
        code += "\n \n/*Method to calculate the mode from a given input value \nInput: Value of the error function and thresholds \nOutput: Mode of the system at that point */";
        code += "\nmethod ModeAnalyzer(e: Error, ";
		code += consts + ") returns (mode: int) \n";
		code += "ensures modeCorrect(e, " + constVars + ",mode) \n { \n ";
		code += insIndent() + "mode := -1; \n ";

        for (int i = 0; i < modes.size(); i++) {
            Mode mode = modes.get(i);
            if (i == 0)
                code += insIndent() + "if (";
            else
                code += insIndent() + "} \n" + insIndent() + "else if (";

			code += replaceVar(replaceMathFuncs(replaceThresholds(mode.getCondition())), varsMap) + ") {\n";
            code += incInsIndent() + "mode := " + mode.getId() + ";\t// " + mode.getDesc() + "\n";
            decIndent();
        }
        code += insIndent() + "}\n ";
		code += "}\n";
    }
	
	
	 private void generateModeCorrectness() {
		String conditions = "";
        code += insIndent() + "\n//Predicate to check if the mode of a particular input is consistent \n \n";
        code += insIndent() + "predicate modeCorrect(e: Error, ";
		code += consts + ", m: int) { \n \n";

        for (int i = 0; i < modes.size(); i++) {
            Mode mode = modes.get(i);
            if (i == 0)
			{
				code += insIndent() + "/*If condition*/ \n";
				code += "(" + replaceVar(replaceMathFuncs(replaceThresholds(mode.getCondition())), varsMap);
				code += " ==> m == " + mode.getId() + ")\n \n";
				conditions += "!(" + replaceVar(replaceMathFuncs(replaceThresholds(mode.getCondition())), varsMap) + ")";
			}
			else
			{
				code += incInsIndent() + "/* Else if condition reached when earlier conditions are not satisfied*/ \n";
				code += insIndent() + " && (" + conditions + " && " + replaceVar(replaceMathFuncs(replaceThresholds(mode.getCondition())), varsMap);
				code += " ==> m == " + mode.getId() + ")\n \n";
				conditions += " && !(" + replaceVar(replaceMathFuncs(replaceThresholds(mode.getCondition())), varsMap) + ")";
			}
        }
		incInsIndent();
		code += insIndent() + "/* Default condition for Unknown mode*/ \n";
		code += insIndent() + " && (" + conditions + " ==> m == -1 )\n";
        code += "}\n ";
		decIndent();
    }
	
	
	private void generateSystemCheck() {
		code += insIndent() + "\n//Predicate to check if the stream of values is consistent with model - specification to be proved\n";
		code += insIndent() + "\npredicate SystemCheck(start: int, end: int, dataStream: array?<Error>, modeStream: array?<int>, " + consts + ") \n";
		code += insIndent() + "requires dataStream != null && modeStream != null && dataStream.Length >= end  && modeStream.Length == dataStream.Length \n";
		code += insIndent() + "reads dataStream, modeStream\n";
		code += insIndent() + "requires end >= start && start >= 0\n" + insIndent() + "{ \n";
		code += insIndent() + "forall i :: start <= i < end ==> modeCorrect(dataStream[i], " + constVars + " , modeStream[i])\n" + insIndent() + "} \n";
		decIndent();
    }
	
	private void generateInductiveProof() {
		code += insIndent() + "\n//Method to calculate the mode of every incoming value and prove it consistent with existing values using induction\n";
		code += "\nmethod inductiveProof(start: int, end: int, dataStream: array<Error>, " + consts + ")  returns (modeStream: array<int>) \n";
		code += "requires end >= start && start >= 0\n";
		code += "requires dataStream.Length > 0 ==> dataStream.Length - 1  >= end \n";
		code += "requires dataStream.Length == 0 ==> dataStream.Length >= end \n";
		code += "ensures modeStream.Length == dataStream.Length  \n";
		code += "ensures SystemCheck(start, end, dataStream, modeStream, " + constVars + ") \n" + insIndent() + "{ \n";
		code += insIndent() + "modeStream := new int[dataStream.Length]; \n";
		code += insIndent() + "if(dataStream.Length == 0) //When do data is available \n" + insIndent() + "{ \n " + insIndent() + "{assert SystemCheck(0, 0, dataStream, modeStream, " +constVars+ ");\n";
		code += insIndent() + "} \n " + insIndent() + "{else {\n " + insIndent() + "var idx := start; \nwhile(idx < end) \ninvariant idx <= end // property is consistent over all values in the time window \n";
		code += insIndent() + "invariant SystemCheck(start, idx, dataStream, modeStream," + constVars + ") \n";
		code += insIndent() + "decreases end - idx \n{ \nvar m := ModeAnalyzer(dataStream[idx], " + constVars + "); \n";
		code += insIndent() +  "modeStream[idx] := m; \n  idx := idx + 1; \n } \n } \n }";
    }
	
	private String replaceVar(String exp, Map<String, String> map) {
        // Replace all variables in exp using entires in map
        // E.g. exp: "a + b" ==> "data.get("a") + data.get("b")"
        String newExp = "";
        StringTokenizer tokenizer = new StringTokenizer(exp, "()/*+-<>= &|", true);

        while (tokenizer.hasMoreElements()) {
            String var = (String)tokenizer.nextElement();

            // Special case for the mode keyword
            // Use the raw mode variable unless it is explicity defined by the user
            if (map.get("mode") == null && var.equals("mode")) {
                newExp += var;
                continue;
            }

            // Special case for power: a^n, n is integer
            int powerOpIndex = var.indexOf("^");
            String exponent = null;
            if (0 < powerOpIndex) {
                newExp += "Math.pow(";
                exponent = var.substring(powerOpIndex + 1);
                var = var.substring(0, powerOpIndex);
            }

            String hashVar = map.get(var);
            // System.out.println(var + " --> " + hashVar);
            if (hashVar != null)
                newExp += hashVar;
            else 
                newExp += var;

            if (0 < powerOpIndex)
                newExp += ", " + exponent + ")";
        }

        return newExp;
    }
	
	 public String replaceMathFuncs(String exp) {
        String[] funcs1 = {"asin", "acos", "atan"};
        String[] funcs2 = {"sqrt", "sin", "cos", "abs"};
        String[] funcs3 = {"arcs", "arcc", "arct"};

        for (int i = 0; i < funcs1.length; i++)
            exp = exp.replaceAll(funcs1[i], funcs3[i]);

        for (int i = 0; i < funcs2.length; i++)
            exp = exp.replaceAll(funcs2[i], "Math." + funcs2[i]);

        for (int i = 0; i < funcs3.length; i++) 
            exp = exp.replaceAll(funcs3[i], "Math." + funcs1[i]);

        return exp;
    }
	
	/*Ankita added: To replace threshold checks by operators */
	
	 public String replaceThresholds(String exp) {

		String[] thresh = {"..", "[", "]", "{", "}"};
        String[] threshTargets = {" && ", " >= ", " <= ", " > ", " < "};

        for (int i = 0; i < thresh.length; i++) {
            exp = exp.replace(thresh[i], threshTargets[i]);
        }
		
		return exp;
        
    }
	
	
	  public String replaceLogicalOps(String exp) {
        String[] opsSources = {"and", "or", "xor", "not"};
        String[] opsTargets = {" && ", " || ", " ^ ", " ! "};

        for (int i = 0; i < opsSources.length; i++) {
            exp = exp.replaceAll(opsSources[i], opsTargets[i]);
        }
        
        return exp;
    }
	
	 private void generateCode() {

        generateConstantsList();
		//generateInputs();
        generateErrors();
		generateModeCorrectness();
		generateModesErrorDetection();
		generateSystemCheck();
		generateInductiveProof();
       // generateLoop();
        // generateModeCountFunctions();
       // generateMain();
    }

    private void outputCode() {
        if (opts.get("stdout")) {
            System.out.println(code);
        }
        else {
            try {
                File file = new File(appName + ".dfy");
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                pw.print(code);
                pw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

 public Object visit(SimpleNode node, Object data) {
        return null;
    }

    public Object visit(ASTPilots node, Object data) {
        goDown("Pilots");

        appName = (String) node.jjtGetValue();
        appName = appName.substring(0, 1).toUpperCase() + appName.substring(1);
        acceptChildren(node, null);
        generateCode();
        outputCode();

        goUp();

        return null;
    }

    public Object visit(ASTInput node, Object data) {
        LOGGER.finest("ASTInput: value=" + node.jjtGetValue()
                     + ", #children=" + node.jjtGetNumChildren()
                     + ", data=" + data);

        goDown("Input");

        InputStream input = new InputStream();
        String[] varNames = ((String)node.jjtGetValue()).split(",");
        input.setVarNames(varNames);
        inputs.add(input);
        acceptChildren(node, input);

        goUp();

        return null;
    }

    public Object visit(ASTConstant node, Object data) {
        LOGGER.finest("ASTConstant: value=" + node.jjtGetValue()
                     + ", #children=" + node.jjtGetNumChildren()
                     + ", data=" + data);
        
        goDown("Constant");

        String[] vals = ((String)node.jjtGetValue()).split(":");
        constants.add(vals[0]);

        goUp();
        
        return null;
    }
    
    public Object visit(ASTOutput node, Object data) {
        LOGGER.finest("ASTOutput: value=" + node.jjtGetValue()
                     + ", #children=" + node.jjtGetNumChildren()
                     + ", data=" + data);
        
        goDown("Output");

        OutputStream output = new OutputStream();
        output.setOutputType(OutputType.Output);
        String[] vals = ((String)node.jjtGetValue()).split(":");

        String[] varNames = vals[0].split(",");
        output.setVarNames(varNames);
        output.setExp(vals[1]);

        int unit = 0;
        if (vals[3].equalsIgnoreCase("nsec") || vals[3].equalsIgnoreCase("usec")) {
            unit = 0;
        }
        else if (vals[3].equalsIgnoreCase("msec")) {
            unit = 1;
        }
        else if (vals[3].equalsIgnoreCase("sec")) {
            unit = 1000;
        }
        else if (vals[3].equalsIgnoreCase("min")) {
            unit = 60 * 1000;
        }
        else if (vals[3].equalsIgnoreCase("hour")) {
            unit = 60 * 60 * 1000;
        }
        else if (vals[3].equalsIgnoreCase("day")) {
            unit = 24 * 60 * 60 * 1000;
        }
        output.setInterval((int)(Double.parseDouble(vals[2]) * unit)); // msec
        outputs.add(output);

        node.jjtGetChild(1).jjtAccept(this, output); // accept Exps() only

        goUp();

        return null;
    }

    public Object visit(ASTError node, Object data) {
        LOGGER.finest("ASTError: value=" + node.jjtGetValue()
                     + ", #children=" + node.jjtGetNumChildren()
                     + ", data=" + data);
        
        goDown("Error");

        OutputStream output = new OutputStream();
        output.setOutputType(OutputType.Error);
        String[] vals = ((String) node.jjtGetValue()).split(":");

        String[] varNames = vals[0].split(",");
        output.setVarNames(varNames);
        output.setExp(vals[1]);
        output.setInterval(-1); // No interval for error
        errors.add(output);

        node.jjtGetChild(1).jjtAccept(this, output); // accept Exps() only

        goUp();

        return null;
    }

    public Object visit(ASTSignature node, Object data) {
        LOGGER.finest("ASTSignature: value=" + node.jjtGetValue()
                     + ", #children=" + node.jjtGetNumChildren()
                     + ", data=" + data);
        
        goDown("Signature");
        String[] vals = ((String) node.jjtGetValue()).split(":");
        // format = id:constant:arg:exps
        Signature sig = new Signature(vals[0], vals[1], vals[3], vals[4]);
        sigs.add(sig);
        
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            LOGGER.finest("ASTSignature: child=" + node.jjtGetChild(i));            
        }

        // First child is Exp. Multiple Estimates can follow that.
        if (1 < node.jjtGetNumChildren()) {
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                LOGGER.finest("ASTSignature: child=" + node.jjtGetChild(i));
                node.jjtGetChild(i).jjtAccept(this, sig);
            }
        }        
        goUp();

        return null;
    }

    public Object visit(ASTMode node, Object data) {
        LOGGER.finest("ASTMode: value=" + node.jjtGetValue()
                     + ", #children=" + node.jjtGetNumChildren()
                     + ", data=" + data);
        
        goDown("Mode");
        String[] vals = ((String)node.jjtGetValue()).split(":");
        Mode mode = new Mode(vals[0], replaceLogicalOps(vals[1]), vals[2]);
        modes.add(mode);

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            LOGGER.finest("ASTMode: child=" + node.jjtGetChild(i));
        }

        // First child is Exp. Multiple Estimates can follow that.
        if (1 < node.jjtGetNumChildren()) {
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                LOGGER.finest("ASTMode: child=" + node.jjtGetChild(i));
                node.jjtGetChild(i).jjtAccept(this, mode);
            }
        }

        goUp();
        
        return null;
    }
    
    public Object visit(ASTEstimate node, Object data){
        LOGGER.finest("ASTEstimate: value=" + node.jjtGetValue()
                     + ", #children=" + node.jjtGetNumChildren()
                     + ", data=" + data);
        
        goDown("estimate");
        
        String[] vals = ((String) node.jjtGetValue()).split(":");
        String var = vals[0]; 
        String when = vals[1];
        String times = vals[2];
        String exp = vals[3];

        int modeId = -1;
        String name = null, arg = null;
        if (data instanceof Mode) {
            Mode mode = (Mode)data;
            modeId = mode.getId();
        } else {
            // it must be Signature
            Signature sig = (Signature)data;
            modeId = sig.getId();
            name = sig.getName();
            arg = sig.getArg();
        }

        Correct correct = new Correct(modeId, name, arg, var, exp);

        if (!when.equals("null")) {
            correct.saveState = true;
            correct.saveStateTriggerModeCount = 1;
            if (!times.equals("null")){
                correct.saveStateTriggerModeCount = Integer.parseInt(times);
            }
        }

        // Since now a mode can have multiple estimates, they are chained by a list
        List<Correct> correctsList = corrects.getOrDefault(modeId, new ArrayList<>());
        correctsList.add(correct);
        corrects.put(modeId, correctsList);
        
        goUp();
        
        return null;
    }

    public Object visit(ASTCorrect node, Object data) {
        LOGGER.finest("ASTCorrect: value=" + node.jjtGetValue()
                     + ", #children=" + node.jjtGetNumChildren()
                     + ", data=" + data);
        
        goDown("Correct");

        String[] vals = ((String) node.jjtGetValue()).split(":");

        int modeId = -1;
        String id = vals[0];
        if (id.charAt(0) == 's' || id.charAt(0) == 'S') {
            // node is for signature
            int parenIndex = id.indexOf("(");
            String integerIdStr = (0 < parenIndex) ? id.substring(1, parenIndex) : id.substring(1);
            modeId = Integer.parseInt(integerIdStr);            
        } else if (id.charAt(0) == 'm' || id.charAt(0) == 'M') {
            // node is for mode
            modeId = Integer.parseInt(id.substring(1));            
        } else {
            System.err.println("Illegel start of signature identifier: " + id.charAt(0));
            return null;
        }

        Correct correct = new Correct(modeId, vals[0], vals[1], vals[2], vals[3]);
        List<Correct> correctsList = corrects.getOrDefault(modeId, new ArrayList<>());
        correctsList.add(correct);

        goUp();

        return null;
    }

    public Object visit(ASTVars node, Object data) {
        goDown("Vars");  
        goUp();
        return null;
    }

    public Object visit(ASTConstInSignature node, Object data) {
        goDown("Const");        
        goUp();
        return null;
    }

    public Object visit(ASTDim node, Object data) {
        goDown("Dim");
        goUp();
        return null;
    }

    public Object visit(ASTMethod node, Object data) {
        goDown("Method");

        InputStream input = (InputStream)data;
        String[] vals = ((String) node.jjtGetValue()).split(":");
        String[] args = vals[1].split(",");

        int id;
        if (vals[0].equalsIgnoreCase("closest")) {
            id = Method.CLOSEST;
        } else if (vals[0].equalsIgnoreCase("euclidean")) {
            id = Method.EUCLIDEAN;
        } else if (vals[0].equalsIgnoreCase("interpolate")) {
            id = Method.INTERPOLATE;
        } else if (vals[0].equalsIgnoreCase("model")){
            id = Method.MODEL;
	    models.add( vals[1].split(",")[0] );
        } else {
            System.err.println("Invalid method: " + vals[0]);
            return null;
        }
        
        input.addMethod(id, args);
        goUp();

        return null;
    }

    public Object visit(ASTMethods node, Object data) {
        goDown("Methods");
        acceptChildren(node, data);
        goUp();
        return null;
    }

    public Object visit(ASTTime node, Object data) {
        goDown("Time");
        goUp();
        return null;
    }

    public Object visit(ASTExps node, Object data) {
        goDown("Exps");
        acceptChildren(node, data);
        goUp();
        return null;
    }

    public Object visit(ASTExp node, Object data) {
        goDown("Exp");
        acceptChildren(node, data);
        goUp();
        return null;
    }

    public Object visit(ASTExp2 node, Object data) {
        goDown("Exp2");
        acceptChildren(node, data);
        goUp();
        return null;
    }
	
	  public Object visit(ASTIntervalExp  node, Object data) {
        goDown("IntervalExp ");
        acceptChildren(node, data);
        goUp();
        return null;
    }

    public Object visit(ASTFunc node, Object data) {
        goDown("Func");
        goUp();
        return null;
    }

    public Object visit(ASTNumber node, Object data) {
        goDown("Number");
        goUp();
        return null;
    }

    public Object visit(ASTValue node, Object data) {
        goDown("Value");
        if (data instanceof OutputStream && (node.jjtGetValue() != null)) {
            OutputStream output = (OutputStream)data;
            output.addDeclaredVarNames((String)node.jjtGetValue());
        }
        goUp();
        return null;
    }
}

	
	
	
	
