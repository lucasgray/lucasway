package com.ni.lucasway.model;

import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class SqlDependencyTest {

	SqlDependency parent
	
	// Parent
	// 			ChildOne
	//					GrandChildOne
	//								 GreatGrandChildOne
	//					GrandChildTwo
	//					GrandChildThree
	//			ChildTwo
	//			ChildTree
	
	@Before
	void setup() {
		
		parent = new SqlDependency()
		parent.name = "Parent"
		
		SqlDependency childOne = new SqlDependency()
		childOne.name = "ChildOne"
		
		SqlDependency childTwo = new SqlDependency()
		childTwo.name = "ChildTwo"
		
		SqlDependency childThree = new SqlDependency()
		childThree.name = "ChildThree"
		
		parent.children << childOne
		parent.children << childTwo
		parent.children << childThree
		
		SqlDependency grandChildOne = new SqlDependency()
		grandChildOne.name = "GrandChildOne"
		
		SqlDependency grandChildTwo = new SqlDependency()
		grandChildTwo.name = "GrandChildTwo"
		
		SqlDependency grandChildThree = new SqlDependency()
		grandChildThree.name = "GrandChildThree"
		
		childOne.children << grandChildOne
		childTwo.children << grandChildTwo
		childTwo.children << grandChildThree
		
		SqlDependency greatGrandChildOne = new SqlDependency()
		greatGrandChildOne.name = "GreatGrandChildOne"
		
		grandChildOne.children << greatGrandChildOne
		
		SqlDependency greatGrandChildTwo = new SqlDependency()
		greatGrandChildTwo.name = "GreatGrandChildTwo"
		
		grandChildTwo.children << greatGrandChildTwo
		
		SqlDependency greatGreatGrandChildTwo = new SqlDependency()
		greatGreatGrandChildTwo.name = "GreatGreatGrandChildTwo"
		
		greatGrandChildTwo.children << greatGreatGrandChildTwo
	}
	
	@Test
	void testDepthFirst() {
		def rslt = parent.depthFirst().collect{it.name}
		assert rslt[0].equals("GreatGrandChildOne")
		assert rslt[1].equals("GrandChildOne")
		assert rslt[rslt.size()-1].equals("Parent")
		
		println rslt
	}
	
	@Test
	@Ignore
	void TestDepthFirstClos() {
		def rslt = parent.depthFirst({it -> it.name = it.name + "Test!" }).collect{it.name}
		assert rslt[0].equals("GreatGrandChildOneTest!")
		assert rslt[1].equals("GrandChildOneTest!")
		assert rslt[rslt.size()-1].equals("ParentTest!")
	}
	
}
