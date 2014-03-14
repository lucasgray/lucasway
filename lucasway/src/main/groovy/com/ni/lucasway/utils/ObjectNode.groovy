package com.ni.lucasway.utils

public class ObjectNode
{
	static enum ObjectNodeVisitState { HALT_FULL, HALT_CHILDREN, CONTINUE }

	/**
	 * Traverse the node tree in depth-first fashion.
	 * @param consumeNode Caller visits and consumes each node once the traversal
	 * arrives at the node; secondly, the caller can tell the traversal to stop
	 * by returning {@link ObjectNodeVisitState#HALT_FULL}.
	 */
	def final static depthFirst = { ObjectNode node, Closure consumeNode ->

		def visitState = consumeNode(node)
		if ((visitState != ObjectNodeVisitState.HALT_FULL) && (visitState != visitState != ObjectNodeVisitState.HALT_CHILDREN))
		{
			def childItr = node.children.iterator()
			while ((visitState != ObjectNodeVisitState.HALT_FULL) && childItr.hasNext()) {
				ObjectNode.depthFirst(childItr.next(), consumeNode)
			}
		}
	}

	/**
	 * Traverse the node tree in breadth-first fashion.
	 * @param consumeNode Caller visits and consumes each node once the traversal
	 * arrives at the node; secondly, the caller can tell the traversal to stop
	 * by returning {@link ObjectNodeVisitState#HALT_FULL}.
	 */
	def final static breadthFirst = { ObjectNode node, Closure consumeNode ->
		def visitState = consumeNode(node)
		if ((visitState != ObjectNodeVisitState.HALT_FULL) && (visitState != ObjectNodeVisitState.HALT_FULL)) {
			ObjectNode.breadthFirstChildren(node.children, consumeNode)
		}
	}

	private static final Closure breadthFirstChildren = { List children, Closure consumeNode ->
		def visitState
		def grandChildren = []
		for (int i = 0; i < children.size(); i++) {
			visitState = consumeNode(children[i])
			if (visitState == ObjectNodeVisitState.HALT_FULL) {
				return visitState
			}
			else if (visitState != ObjectNodeVisitState.HALT_CHILDREN) {
				grandChildren.addAll(children[i].children)
			}
		}
		if (visitState == ObjectNodeVisitState.HALT_FULL) {
			return visitState
		}
		else if (grandChildren.size() > 0) {
			return ObjectNode.breadthFirstChildren(grandChildren, consumeNode)
		}
		else {
			return ObjectNodeVisitState.CONTINUE
		}
	}

	private ObjectNode parent
	private List<ObjectNode> children = []
	def String name
	def Object value

	public ObjectNode() {
		
	}

	public ObjectNode(ObjectNode parent, String name, Object value) {
		setParent(parent)
		this.name = name
		this.value = value
	}

	/**
	 * Find and return the first node evaluated to <code>true</code> by calling
	 * <code>evaluateNode</code>.
	 * 
	 * @param traverseTree eg. the closures, <code>depthFirst</code> or <code>breadthFirst</code>.
	 * Your traverse method must accept two params: a node then a consumption closure.  In this case,
	 * the consumption closure evaluates each node visited/traversed as being the node we were
	 * looking for and will return in this method.
	 */
	def find(Closure traverseTree = ObjectNode.depthFirst, Closure evaluateNode) {
		def found = null
		traverseTree(this) { node ->
			if (evaluateNode(node)) {
				found = node
				return ObjectNodeVisitState.HALT_FULL
			}
	    }
		return found
	}

	/**
	 * Find all nodes using the tree traversal closure, <code>traverseTree</code>.
	 * Nodes that evaluate to <code>true</code> using <code>evaluateNode</code> are collected
	 * and returned after traversing the tree is complete or halted.
	 * 
	 * @param traverseTree eg. the closures, <code>depthFirst</code> or <code>breadthFirst</code>.
	 * Your traverse method must accept two params: a node then a consumption closure.  In this case,
	 * the consumption closure evaluates each node visited/traversed and adds to a collection
	 * of matching nodes
	 * 
	 * @return the collection of matching nodes
	 */
	def findAll(Closure traverseTree = ObjectNode.depthFirst, Closure evaluateNode) {
		def found = []
		traverseTree(this) { node ->
			if (evaluateNode(node)) found += node
		}
		return found
	}

	/**
	 * Visit all nodes using the closure, <code>traverseTree</code>, to coordinate
	 * the tree traversal order.
	 * 
	 * @param traverseTree eg. the closures, <code>depthFirst</code> or <code>breadthFirst</code>.
	 * @param consumeNode callback closure that gets to inspect each node; also, it can decide when
	 * to stop the tree traversal by returning {@link ObjectNodeVisitState#HALT_FULL}.
	 */
	def visit(Closure traverseTree = ObjectNode.depthFirst, Closure consumeNode) {
		traverseTree(this, consumeNode)
	}

	public ObjectNode getParent() {
		return parent
	}

	public void setParent(ObjectNode parent) {
		if (this.parent) parent.children -= this
		this.parent = parent
		if (this.parent) parent.children += this
	}

	public ObjectNode append(ObjectNode child) {
		child.setParent(this)
		return this
	}

	public void remove(ObjectNode child) {
		if (child.getParent() == this) child.setParent(null)
	}

	public void replaceInTree(ObjectNode replaceWith) {
		parent.children -= this
		parent.children += replaceWith
		children.each { child -> child.setParent(replaceWith) }
	}

	def printTree(indent = 0) {
		println "${' '.multiply(indent)}${name.padRight(70)}: ${value}"
		println "${' '.multiply(indent)}- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -"
		children.each {
			it.printTree(indent + 4)
		}
	}

	@Override
	public String toString() {
		return "${name}=${value}"
	}
}