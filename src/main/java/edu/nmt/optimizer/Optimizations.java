package edu.nmt.optimizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import edu.nmt.frontend.Grammar;
import edu.nmt.frontend.Node;
import edu.nmt.frontend.parser.ASTParser;
import edu.nmt.frontend.parser.Parser;
import edu.nmt.frontend.scanner.Scanner;

public class Optimizations {
	
	// IR to optimize
	public IR target;
	enum Statement {
		invalid,
		basicStatement,
		returnStatement,
		conditionStatement,
		deScope,
	}
	
	/*
	 * Base constructor using formed IR
	 */
	public Optimizations(IR a) {
		target = a;
	}
	
	/**
	 * Applies constant propagation to the IR
	 * @return true if a change occurred
	 * @todo Scoping, adding more cases
	 */
	public Boolean constProp() {
		String splitres[];
		List<Instruction> instrList = target.getFunctionIRs().get("main");
		List<String> removalKeys = new ArrayList<String>();
		Map<String, Integer> varMap = new HashMap<String, Integer>();
		Map<String, Integer> copy = null;
		Stack<Map<String, Integer>> scoping = new Stack<Map<String, Integer>>();
		Boolean status = false;
		Integer counter = 0;
		Statement stattype;
		
		for (Instruction i : instrList) {
			splitres = i.toString().split(" ");
			
			// Temporary prints, delete later
			System.out.printf("Set of strings: {");
			for (String a : splitres) {
	            System.out.printf("%s|", a);
			}
			System.out.printf("}\n");
			
			// Set the types of the operation
			if(i.type.contentEquals("int") && splitres[1].equals("=")) {
				stattype = Statement.basicStatement;
			} else if(i.type.contentEquals("int") && splitres[0].equals("return")) {
				stattype = Statement.returnStatement;
			} else if(splitres[0].contentEquals("jump")) {
				stattype = Statement.conditionStatement;
			}  else if((splitres.length > 2) &&splitres[2].contains("endOf")) {
				System.out.println("WHOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
				stattype = Statement.deScope;
			} else {
				stattype = Statement.invalid;
			}
			
			// Attempt Propagation dependent on type
			if(stattype.equals(Statement.basicStatement)) {
				
				// Adding of entries goes here
				if(splitres.length == 3) {
					try {
						varMap.put(splitres[0], Integer.valueOf(splitres[2]));
					} catch (NumberFormatException e) {
						
					}
					System.out.println("Current mapping: "+varMap);
				}
				
				// Propagating goes here
				for(counter = 1; counter < splitres.length; counter++) {
					if(varMap.get(splitres[counter]) != null) {		
						try {
				    		i.setOperation("identifier");
				    		if(i.op1Name != null) {
				    			i.op1Name = i.op1Name.replace(splitres[counter], varMap.get(splitres[counter]).toString());
				    			System.out.println("***Replacing: " + splitres[counter] + " with: " + varMap.get(splitres[counter]).toString());
				    		} else {
				    			i.op1Name = opPrep(splitres, 2).replace(splitres[counter], varMap.get(splitres[counter]).toString());
				    			System.out.println("*****Replacing: " + splitres[counter] + " with: " + opPrep(splitres, 2).replaceAll(splitres[counter], varMap.get(splitres[counter]).toString()));
				    		}
				    		status = true;
				    	} catch (NullPointerException e) {
				    		i.op1Name = opPrep(splitres, 2).replace(splitres[counter], varMap.get(splitres[counter]).toString());
			    			System.out.println("*******************Replacing: " + splitres[counter] + " with: " + opPrep(splitres, 2).replaceAll(splitres[counter], varMap.get(splitres[counter]).toString()));
							status = true;
						}
						splitres = i.toString().split(" ");
						System.out.println("Now this is: " + i.toString());
					}
				}
			} else if (stattype.equals(Statement.returnStatement)) {
				// Propagating goes here
				for(counter = 1; counter < splitres.length; counter++) {
					if(varMap.get(splitres[counter]) != null) {		
						try {
				    		i.setOperation("identifier");
				    		if(i.op1Name != null) {
				    			i.op1Name = i.op1Name.replace(splitres[counter], varMap.get(splitres[counter]).toString());
				    			System.out.println("***Replacing: " + splitres[counter] + " with: " + varMap.get(splitres[counter]).toString());
				    		} else {
				    			i.op1Name = opPrep(splitres, 2).replace(splitres[counter], varMap.get(splitres[counter]).toString());
				    			System.out.println("*****Replacing: " + splitres[counter] + " with: " + opPrep(splitres, 2).replaceAll(splitres[counter], varMap.get(splitres[counter]).toString()));
				    		}
				    		status = true;
				    	} catch (NullPointerException e) {
				    		i.op1Name = opPrep(splitres, 2).replace(splitres[counter], varMap.get(splitres[counter]).toString());
			    			System.out.println("*******************Replacing: " + splitres[counter] + " with: " + opPrep(splitres, 2).replaceAll(splitres[counter], varMap.get(splitres[counter]).toString()));
							status = true;
						}
						splitres = i.toString().split(" ");
						System.out.println("Now this is: " + i.toString());
					}
				}
			} else if(stattype.equals(Statement.conditionStatement)) {
				// This set up a new scope
				scoping.push(varMap);
				copy = new HashMap<String, Integer>();
				for(Map.Entry<String, Integer> entry : varMap.entrySet()) {
					copy.put(entry.getKey(), entry.getValue());
				}
				varMap = copy;
			} else if(stattype.equals(Statement.deScope)) {
				copy = varMap;
				varMap = scoping.pop();
				
				for(Map.Entry<String, Integer> entry : varMap.entrySet()) {
					try {
						if (entry.getValue().equals(copy.get(entry.getKey())) == false) {
							removalKeys.add(entry.getKey());
						}
					} catch(NullPointerException e) {
						removalKeys.add(entry.getKey());
					}
					
				}
				
				for (int j = 0; j < removalKeys.size(); j++) {
					varMap.remove(removalKeys.get(j));
				}
			}
		}
		
		return status;
	}
	
	/**
	 * Applies constant folding to the 
	 * @return
	 */
	public Boolean constFold() {
		String splitres[];
		List<Instruction> instrList = target.getFunctionIRs().get("main");
		ExpressionEvaluator eval;
		Boolean status = false;
		
		for (Instruction i : instrList) {
			splitres = i.toString().split("=");
			if(splitres.length != 2) {
				
				eval = new ExpressionEvaluator(splitres[0]);
				
				if(eval.GetValue() != null) {
					try {
			    		i.setOperation("identifier");
			    		i.op1Name  = Integer.toString(eval.GetValue().intValue());
			    		
			    		if(Integer.parseInt(splitres[0]) != eval.GetValue().intValue()) {
			    			status = true;
			    		}
			    	} catch (NullPointerException e) {
			    		i.op1Name  = Integer.toString(eval.GetValue().intValue());
			    		if(Integer.parseInt(splitres[0]) != eval.GetValue().intValue()) {
			    			status = true;
			    		}
					} catch (NumberFormatException e) {
						status = false;
					}
				}
				continue;
			}
			splitres[0] = splitres[0].replaceAll("\\s+", "");
			splitres[1] = splitres[1].replaceAll("\\s+", "");
			
			eval = new ExpressionEvaluator(splitres[1]);
			
			if(eval.GetValue() != null) {
				try {
		    		i.setOperation("identifier");
		    		i.op1Name  = Integer.toString(eval.GetValue().intValue());
		    		
		    		if(Integer.parseInt(splitres[1]) != eval.GetValue().intValue()) {
		    			status = true;
		    		}
		    	} catch (NullPointerException e) {
		    		i.op1Name  = Integer.toString(eval.GetValue().intValue());
		    		if(Integer.parseInt(splitres[1]) != eval.GetValue().intValue()) {
		    			status = true;
		    		}
				} catch (NumberFormatException e) {
					status = false;
				}
			}
			
			
			
		}
		
		return status;
	}
	
	/**
	 * Remove dead lines from IR
	 */
	public void clean() {
		String splitres[];
		List<Instruction> instrList = target.getFunctionIRs().get("main");
		List<Instruction> deadLines = new ArrayList<Instruction>();
		List<String> liveVars = new ArrayList<String>();
		int count = 0;
		int j = 0;

		// Iterate through to find live variables
		for(Instruction i : instrList) {
			splitres = i.toString().split(" ");
			
			// Iterate through statement to find vars
			for(j = 1; j < splitres.length; j++) {
				try {
					Integer.parseInt(splitres[j]);
					continue;
				} catch (NumberFormatException e) {
					if(splitres[j].contentEquals("=") || splitres[j].contentEquals("null")) {
						continue;
					}
					liveVars.add(splitres[j]);
				}
			}
		}
		
		//Find lines that are built for unused variables
		for(Instruction i : instrList) {
			splitres = i.toString().split(" ");
			if(!liveVars.contains(splitres[0]) && splitres[1].contentEquals("=")) {
				deadLines.add(i);
			}
		}
		
		// Remove Dead lines
		instrList.removeAll(deadLines);
	}
	
	/**
	 * Wraps all Level 1 optimizations together
	 * @param target
	 * @return Optimized IR
	 */
	public static void l1Optimize(IR target) {
		Boolean status;
		Optimizations o1 = new Optimizations(target);

		status = true;
		while(status) {
			status = false;
			IR.printMain(target.getFunctionIRs());
			status |= o1.constProp();
			status |= o1.constFold();
		}
		o1.clean();
		
		return;
	}
	
	/**
	 * Formats a string to overwrite an op in the IR
	 * @param array is array of strings to format
	 * @param start is start of value
	 * @return
	 */
	public String opPrep(String array[], Integer start) {
		StringBuffer buffer = new StringBuffer();
		String str = null;
		
		for(int i = start; i < array.length; i++) {
			if(i == start.intValue()) {
				buffer.append(array[i]);
			} else {
				buffer.append(" " + array[i]);
			}
		}
		str = buffer.toString();
		
		return str;
	}

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Scanner scanner = new Scanner("test/optest.c");

		scanner.scan();
		Grammar g = new Grammar("config/grammar.cfg");
		g.loadGrammar();
		Parser p = new Parser(g, scanner, false);
		if (p.parse()) {
//			p.printParseTree();
		}
		
		ASTParser a = new ASTParser(p);
		if (a.parse()) {
			a.printAST();	
		}
		
		a.printSymbolTable();
		Node root = a.getRoot();
		root.recursiveSetDepth();
//		Node mainAST = root.getChildren().get(0).getChildren().get(0).getChildren().get(0);
		IR test = new IR(a);
		List<Instruction> mainList = test.getFunctionIRs().get("main");
//		System.out.println(mainList.get(0));
		IR.printMain(test.getFunctionIRs());
		
		//test.printIR();
		
		test.outputToFile();
		
		System.out.println("Pre-Optimization");
		IR.printMain(test.getFunctionIRs());
		Optimizations.l1Optimize(test); //  Example for calling L1 Opt
		System.out.println("\nPost-Optimization");
		IR.printMain(test.getFunctionIRs());
	}

}
