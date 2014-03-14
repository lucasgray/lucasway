package com.ni.lucasway.utils

import org.junit.Assert
import org.junit.Test

import groovy.util.NodePrinter

public class DirectoryScanUtilsTest
{
	public static def TESTS_DIR = new File('src/test/resources/db-function-tests')

	@Test
	public void testBuildAndProcessNodes()
	{
		def rootNode = DirectoryScanUtils.buildAndProcessNodes(TESTS_DIR) { dir, parentNode ->
			return [dir: dir]
		}

		rootNode.visit(ObjectNode.depthFirst) { node ->
			Assert.assertNotNull(node.name)
			Assert.assertNotNull(node.value)
			Assert.assertNotNull(node.value.dir)
			Assert.assertEquals(node.value.dir.name, node.name)
			println "${node.name.padRight(80)} : value=${node.value}"
		}
	}
}