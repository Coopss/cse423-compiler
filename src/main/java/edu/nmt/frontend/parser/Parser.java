package edu.nmt.frontend.parser;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import edu.nmt.RuntimeSettings;
import edu.nmt.frontend.Grammar;
import edu.nmt.frontend.Node;
import edu.nmt.frontend.Token;
import edu.nmt.frontend.scanner.Scanner;
import edu.nmt.util.Debugger;

/**
 * Converts a token stream into a parse tree
 * @author Terence
 * 
 */
public class Parser {
	
	private Grammar grammar;
	private List<Token> tokens;
	private Node parseTree;
	private Action action;
	private Debugger debugger;
	private String filename;
	
	public Parser(Grammar g, Scanner scanner) {
		this.grammar = g;
		this.tokens = scanner.getTokens();
		this.filename = scanner.getFile().getName();
		this.debugger = new Debugger(false);
		this.action = new Action(this.debugger);
		
		try {
			grammar.loadGrammar();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Goto.init(g);
	}
	
	public Parser(Grammar g, Scanner scanner, boolean debug) {
		this.grammar = g;
		this.tokens = scanner.getTokens();
		this.filename = scanner.getFile().getName();
		this.debugger = new Debugger(debug);
		this.debugger.print(scanner.getTokens());
		this.action = new Action(this.debugger);
		
		try {
			grammar.loadGrammar();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Goto.init(g);
	}
	
	/**
	 * prints the parse tree to console
	 */
	public void printParseTree() {
		System.out.println(Node.printTree(this.parseTree, " ", false));
		Node.recrusiveChildReversal(this.parseTree);
	}
	
	public String getParseTreeString() {
		String s = Node.printTree(this.parseTree, " ", false);
		Node.recrusiveChildReversal(this.parseTree);
		return s;
	}
	
	public void testParse(Node node) {
		
		for (Node n : node.getChildren()) {
			testParse(n);
		}
		
		if (node.getChildren().isEmpty()) {
			System.out.println(node.getToken().getTokenString());
		}
	}

	/**
	 * iterate through stream of tokens and parse them
	 * at each iteration, perform an action based on action type
	 * @return true if successful parse, false else
	 */
	public boolean parse() {
		Iterator<Token> tokenIt = tokens.iterator();
		Token token = null;
		
		while (true) {
			switch (this.action.getType()) {
			case ACCEPT:
				this.parseTree = action.getRoot();
				return true;
			case REJECT:
				this.printError(action.getError(), action.getExpected(), token);
				return false;
			case SHIFT:		
				try {
					debugger.print(token);
					token = tokenIt.next();
				} catch (NoSuchElementException nse) {
					token = null;
				}
			case REPEAT:
				debugger.printPhase(ActionType.SHIFT);
				
				try {
					action.setType(this.action.shift(token));
				} catch (NullPointerException npe) {
					debugger.printStackTrace(npe);
					return false;
				}
				
				break;
			case REDUCE:
				debugger.printPhase(ActionType.REDUCE);
				
				try {
					action.setType(this.action.reduce());
				} catch (NullPointerException npe) {
					debugger.printStackTrace(npe);
					return false;
				}
				break;
			}
		}
	}
	
	private void printError(int err, String token, Token lookahead) {
		switch (err) {
		case 1:
			System.err.println(String.format("%s:%d:%d: error: expected '%s' before '%s' token", this.filename, lookahead.getLineNum(), 
					lookahead.getCharPos(), token, lookahead.getTokenString()));
			break;
		}
	}
	
	public static void main(String argv[]) throws IOException {
		Scanner scanner = new Scanner("test/test.c");
		scanner.scan();
		Parser p = new Parser(new Grammar("config/grammar.cfg"), scanner, true);
		p.grammar.loadGrammar();
		p.parse();
		//p.testParse(p.parseTree);
		//System.out.println("Output of parse(): " + p.parse() + "\n");
		p.printParseTree();
		
		ASTParser a = new ASTParser(p);
		a.parse();
		a.printAST();
	}

	public Node getParseTree() {
		return this.parseTree;
	}
	
	public String getFilename() {
		return this.filename;
	}

	public void writeParseTree(String writeParseFile) {
		try {
			  FileWriter myWriter = new FileWriter(writeParseFile);
		      myWriter.write(this.getParseTreeString());
		      myWriter.close();
		} catch (IOException e) {
			  System.out.println("An error occurred writing the parse tree to file.");
			  e.printStackTrace();
		}    			
		
	}
}