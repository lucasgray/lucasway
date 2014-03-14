package com.ni.lucasway.utils

import org.junit.Assert
import org.junit.Test

public class ObjectNodeTest
{
	def tree = new ObjectNode(null, 'foo', 'bar')
						.append(new ObjectNode(null, 'x', 'y')
							.append(new ObjectNode(null, 'c', 'd')))
						.append(new ObjectNode(null, 'a', 'b'))

	@Test
	public void testFind()
	{
		Assert.assertEquals('bar', tree.find(ObjectNode.depthFirst) { it.name.equals('foo')}?.value)
		Assert.assertEquals('y', tree.find(ObjectNode.depthFirst) { it.name.equals('x')}?.value)
		Assert.assertEquals('b', tree.find(ObjectNode.depthFirst) { it.name.equals('a')}?.value)
		Assert.assertEquals('d', tree.find(ObjectNode.depthFirst) { it.name.equals('c')}?.value)
	}

	@Test
	public void testBreadthFirst()
	{
		runVisitAllTest(ObjectNode.breadthFirst, ['foo', 'x', 'a', 'c'])
	}

	@Test
	public void testDepthFirst()
	{
		runVisitAllTest(ObjectNode.depthFirst, ['foo', 'x', 'c', 'a'])
	}

	protected void runVisitAllTest(traverseMethodToTest, expectedVisitOrder = null)
	{
		def visited = [:]
		def expectedVisited = [foo: 'bar', x: 'y', a: 'b', c: 'd']
		def visitOrder = []
		tree.visit(traverseMethodToTest) { node ->
			if (visited[node.name]) {
				throw new AssertionError("Already visited node! ${node}")
			}
			else {
				visitOrder += node.name
				visited[node.name] = node.value
			}
		}

		Assert.assertEquals(expectedVisited, visited)
		if (expectedVisitOrder) Assert.assertEquals(expectedVisitOrder, visitOrder)
	}
}