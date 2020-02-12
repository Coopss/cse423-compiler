package edu.nmt.frontend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public class Action {

	private ActionType type;
	private Goto state;
	private Stack<Goto> stack;
	private Stack<String> goals;
	
	Action(String goal) {
		this.type = ActionType.SHIFT;
		this.state = null;
		this.stack = new Stack<Goto>();
		this.goals = new Stack<String>();
		this.goals.push(goal);
	}
	
	public ActionType getType() {
		return this.type;
	}
	
	public void setType(ActionType type) {
		this.type = type;
	}
	
	public void shift(Token token, Token lookahead) {
		
		System.out.println("Shifting to " + token + " from state " + this.state + "\n");
		
		if (token == null && lookahead == null) {
			if (this.state.terminateTo() != null) {
				this.setType(ActionType.REDUCE);
				return;
			} else {
				this.setType(ActionType.REJECT);
			}
		/* if state is null, begin there */
		} else if (this.state == null) {
			
			if (!stack.isEmpty()) {
				this.setType(ActionType.REJECT);
				return;
			}
			
			this.state = Goto.gotoTable.get(token.getTokenLabel());
			
			/* push current state to stack */
			this.stack.push(this.state);
			
			/* 
			 * check if this state can transition to the lookahead 
			 * if it can transition, add it to stack
			 * else, set the reduce flag
			 */
			if (this.state.canTransition(lookahead.getTokenLabel())) {
				System.out.println("Adding state \"" + this.state + "\" to the stack\n");
			} else {
				this.setType(ActionType.REDUCE);	
			}
		} else if (!this.state.canTransition(token.getTokenLabel()) && this.state.terminateTo() != null) {
			System.out.println("reduce");
			this.setType(ActionType.REDUCE);
		} else {
			/* else first check if current state can transition to token */
			
			System.out.println("Attempting to shift from " + this.state + " to " + token.getTokenLabel());
			
			Goto nextState = this.state.makeTransition(token.getTokenLabel());
			
			System.out.println("nextState is " + nextState);
			
			/* if next state is null, there is no direct path to token */
			if (nextState == null) {
				/* 
				 * check if there are any non-terminals to transition to 
				 * if not empty, add current state to stack and lock it
				 * else, reject
				 */
				
				System.out.println(this.state.getNonTerminalTransitions().isEmpty());
				
				if (!this.state.getNonTerminalTransitions().isEmpty() || this.state.canRepeat()) {	
					
					Goto nextNT = null;
					
					if (this.state.canRepeat())
						nextNT = this.state;
					else
						nextNT = this.state.getNonTerminalTransitions().get(0);
					
					stack.push(nextNT);
					goals.push(nextNT.toString());
					nextState = Goto.gotoTable.get(token.getTokenLabel());
	
					/* lock it */
					this.stack.push(new Goto(new Token(null, "$", null, null)));
					
					this.stack.push(nextState);
	
					System.out.println(stack);
					
					this.state = nextState;
					this.setType(ActionType.SHIFT);
				} else {
					System.out.println("Rejected");
					this.setType(ActionType.REJECT);
				}
			} else {
				/* add nextState to the stack */
				System.out.println("Adding state \"" + nextState + "\" to the stack\n");
				this.state = nextState;
				this.stack.push(nextState);
				this.setType(ActionType.SHIFT);
			}
		}
	}
	
	public Node reduce() {
		Goto parent = this.state.terminateTo();
		List<Node> children = new ArrayList<Node>();
		
		System.out.println("state \"" + this.state + "\" wants to reduce to \"" + parent + "\"\n");
		
		if (parent.toString().equals(this.goals.peek())) {
			String goal = this.goals.pop();
			
			System.out.println("choice 1\n");
			
			if (this.stack.size() > 1) {
				while (!this.stack.isEmpty()) {
					Goto g = this.stack.pop();
					
					System.out.println("Popped \"" + g + "\" off the stack!");
					
					if (g.toString().equals("$")) {
						break;
					}
					
					children.add(g.getToken());
				}
				
				parent = this.stack.pop();
				
				// add all nodes to parent
				for (Node child : children) {
					parent.getToken().addChild(child);
				}
			} else {
				System.out.println("choice 2\n");
				parent = new Goto(new Token(null, goal, null, null));
				children.add(this.state.getToken());
			}			
			
			// add all nodes to parent
			for (Node child : children) {
				parent.getToken().addChild(child);
			}
			
			this.state = parent;
		} else {
			System.out.println("choice 3\n");
			while (!this.stack.isEmpty()) {
				Goto g = this.stack.pop();
				
				System.out.println("Popped \"" + g + "\" off the stack!");
				
				if (g.toString().equals("$")) {
					break;
				}
				
				children.add(g.getToken());
			}
			
			parent = Goto.gotoTable.get(parent.toString());
			
			// add all nodes to parent
			for (Node child : children) {
				parent.getToken().addChild(child);
			}
			
			this.state = parent;
			
			if (!this.stack.isEmpty())
				stack.push(new Goto(new Token(null, "$", null, null)));
			
			stack.push(this.state);			
		}
		
		System.out.println(stack);
		System.out.println(goals);
		
		if (parent.toString().equals("program"))
			this.setType(ActionType.ACCEPT);
		else
			this.setType(ActionType.REPEAT);	
		
		return parent.getToken();
	}
}