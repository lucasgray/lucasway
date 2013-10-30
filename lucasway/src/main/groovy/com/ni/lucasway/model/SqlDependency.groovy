package com.ni.lucasway.model

/**
 * Sql Dependency Node in a Directed Acyclic Graph 
 */
class SqlDependency {
	
	List<SqlDependency> parents = []
	List<SqlDependency> children = []
	List<String> childNames = []
	boolean run = false
	boolean isEntity
	String name
	String version
	String timestamp
	Integer duration
	String sql
	
	List<SqlDependency> depthFirst() {
		def ret = []
		children = children.sort{it.children.size}.reverse()
		children.each {
			ret.addAll(it.depthFirst())
		}
		ret << this
		ret
	}
	
	def prettyPrint() {
		printMyselfAndChildren(1)
	}
	
	def printMyselfAndChildren(indent) {
		println "\t" * indent + name + ":" + version
		
		if (children.size() > 0) {
			children.each { childNode ->
				if (childNode != null) {
					childNode.printMyselfAndChildren(++indent)
				}
			}
		}
	}
	
}
