package com.ni.lucasway.db.testing

import groovy.io.FileType
import groovy.sql.Sql

import org.junit.Assert
import org.junit.Before
import org.junit.Test

import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier

import com.ni.lucasway.db.SqlMaker
import com.ni.lucasway.utils.ObjectNode

public class DatasetDrivenFunctionTestRunnerTest
{
	public static def SQL_FUNCTIONS_BASE_DIR = new File('src/test/resources/db-functions')
	public static def SQL_FUNCTIONS_TESTS_BASE_DIR = new File('src/test/resources/db-function-tests')
	public static def TEST_NAME = '/com/ni/lucasway/db/testing/are_you_mocking_me'
	public static Closure NODE_BY_TEST_NAME = { node -> node.value?.testName.equals(TEST_NAME) }
	public static def TEST_FUNCTION_NAME = 'are_you_mocking_me'
	public static def TEST_FUNCTION_PATH = '/com/ni/lucasway/db/testing/are_you_mocking_me.sql'

	def static JDBC_CONFIG = ResourceBundle.getBundle('migration-jdbc')

	def static sqlSource = SqlMaker.byBundle(JDBC_CONFIG)

	def testedObject

	@Before
	public void setupTestedObject()
	{
		testedObject = new DatasetDrivenFunctionTestRunner(SQL_FUNCTIONS_BASE_DIR, SQL_FUNCTIONS_TESTS_BASE_DIR)
		testedObject.sqlSource = sqlSource
	}

	@Before
	public void setupDatabase()
	{
    	def jdbcConnection = sqlSource().createConnection()
    	def sqlStmtExec = jdbcConnection.createStatement()
    	sqlStmtExec.execute('DROP TABLE IF EXISTS toyroom')
    	sqlStmtExec.execute('DROP TABLE IF EXISTS lucasway_test_table_1')
    	sqlStmtExec.execute('DROP TABLE IF EXISTS buzzlightyear')
    	sqlStmtExec.execute('CREATE TABLE toyroom (id INTEGER, leader TEXT, is_fun BOOLEAN)')
    	sqlStmtExec.execute('CREATE TABLE buzzlightyear (id INTEGER, col1 TEXT, col2 TEXT, expiresAt TIMESTAMP, toyroom_id INTEGER)')
    	sqlStmtExec.execute('CREATE TABLE lucasway_test_table_1 (id INTEGER, col1 TEXT, col2 TEXT)')
    	SQL_FUNCTIONS_BASE_DIR.eachFileRecurse(FileType.FILES) { file ->
    		println "DatasetDrivenFunctionTestRunnerTest: your mom is a whore: ${file}"
    		if (file.name.endsWith('.sql')) {
    			println "DatasetDrivenFunctionTestRunnerTest: Running SQL function definition: ${file}"
    			sqlStmtExec.execute(file.getText())
    		}
    	}
    	sqlStmtExec.close()
    	jdbcConnection.close()
	}

	@Test
	public void testScanFunctionTestDirectories()
	{
		testedObject.scanFunctionTestDirectories()

		def testCases = testedObject.functionTests.find(ObjectNode.depthFirst, NODE_BY_TEST_NAME)?.value?.testCases
		Assert.assertNotNull(testCases)
		Assert.assertEquals(1, testCases.size())

		def testCase = testCases[0]
		Assert.assertEquals(TEST_FUNCTION_NAME, testCase.functionName)
		Assert.assertEquals("${TEST_NAME}::main".toString(), testCase.name)
		Assert.assertEquals([ "foo", "boo", 1234, true ], testCase.invoke.arguments)
		Assert.assertEquals(1, testCase.expectedOutput.size())
		Assert.assertEquals([ 999, "abc", "def" ], testCase.expectedOutput[0])
	}

	@Test
	public void testRun()
	{
		def pattern = /common-(.+)(?:\.\w+)+/
		def patternMatch = 'common-dataset.xml' =~ pattern
		println "Common Test Context File Match: matched?=${patternMatch.size() == 1}; componentNmae=${patternMatch[0][1]}"
		def testTally = new TestResultAggregator()

		testedObject.scanFunctionTestDirectories()
		def testNode = testedObject.functionTests.find(ObjectNode.depthFirst, NODE_BY_TEST_NAME)
		println "Found example test: ${testNode}"

		def failingTestCase = testNode.value.testCases[0].copyOf()
		testNode.value.testCases += failingTestCase
		failingTestCase.expectedOutput[0][2] = "wrench-in-the-test".toString()
		failingTestCase.name = "${TEST_NAME}::failure".toString()
		
		testedObject.functionTests.find(ObjectNode.depthFirst, NODE_BY_TEST_NAME)?.value.testCases.each { testCase -> 
			println "Test runner will run: ${testCase.name}"
		}

		println 'INVOKING TEST RUN in testRun()'
		testedObject.run(testTally.asNotifier())
		testTally.reportResults()

		Assert.assertEquals(3, testTally.successful.size())
		Assert.assertEquals("${TEST_NAME}::main(com.ni.lucasway.db.testing.DatasetDrivenFunctionTestCase)".toString(), testTally.successful[0].displayName)

		Assert.assertEquals(1, testTally.failures.size())
		Assert.assertEquals("${TEST_NAME}::failure(com.ni.lucasway.db.testing.DatasetDrivenFunctionTestCase)".toString(), testTally.failures[0].description.displayName)
    }
}