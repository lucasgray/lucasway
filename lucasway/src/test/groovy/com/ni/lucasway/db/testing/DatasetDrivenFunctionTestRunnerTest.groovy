package com.ni.lucasway.db.testing

import groovy.sql.Sql

import org.junit.Assert
import org.junit.Before
import org.junit.Test

import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier

import com.ni.lucasway.db.SqlMaker

public class DatasetDrivenFunctionTestRunnerTest
{
	public static def SQL_FUNCTIONS_BASE_DIR = new File('src/test/resources/db-functions')
	public static def SQL_FUNCTIONS_TESTS_BASE_DIR = new File('src/test/resources/db-function-tests')
	public static def TEST_FUNCTION_NAME = '/com/ni/lucasway/db/testing/are_you_mocking_me'
	public static def TEST_FUNCTION_PATH = '/com/ni/lucasway/db/testing/are_you_mocking_me.sql'

	def static JDBC_CONFIG = ResourceBundle.getBundle('unittest-jdbc')

	def static sqlSource = {
		SqlMaker.makeSql(
			JDBC_CONFIG.getString('url'),
			JDBC_CONFIG.getString('driver'),
			JDBC_CONFIG.getString('username'),
			JDBC_CONFIG.getString('password')
		)
	}

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
    	sqlStmtExec.execute('DROP TABLE IF EXISTS buzzlightyear')
    	sqlStmtExec.execute('CREATE TABLE buzzlightyear (id INTEGER, col1 TEXT, col2 TEXT)')
    	sqlStmtExec.execute(new File(SQL_FUNCTIONS_BASE_DIR, TEST_FUNCTION_PATH).getText())
    	sqlStmtExec.executeQuery('select 1 from buzzlightyear')
    	sqlStmtExec.close()
    	jdbcConnection.close()
	}

	@Test
	public void testFindFunctionTests()
	{
		testedObject.findFunctionTests()

		Assert.assertEquals(1, testedObject.functionTests.size())

		def testCases = testedObject.functionTests[TEST_FUNCTION_NAME]
		Assert.assertNotNull(testCases)
		Assert.assertEquals(1, testCases.size())

		def testCase = testCases[0]
		Assert.assertEquals('are_you_mocking_me', testCase.functionName)
		Assert.assertEquals("${TEST_FUNCTION_NAME}::main".toString(), testCase.name)
		Assert.assertEquals([ "foo", "boo", 1234, true ], testCase.invoke.arguments)
		Assert.assertEquals(1, testCase.expectedOutput.size())
		Assert.assertEquals([ 999, "abc", "def" ], testCase.expectedOutput[0])
	}

	@Test
	public void testRun()
	{
		def testTally = new TestResultAggregator()

		testedObject.findFunctionTests()
		testedObject.functionTests[TEST_FUNCTION_NAME] += testedObject.functionTests[TEST_FUNCTION_NAME][0].copyOf()
		testedObject.functionTests[TEST_FUNCTION_NAME][1].expectedOutput[0][2] = "wrench-in-the-test".toString()
		testedObject.functionTests[TEST_FUNCTION_NAME][1].name = "${TEST_FUNCTION_NAME}::failure".toString()
		testedObject.run(testTally.asNotifier())

		//testTally.reportResults()

		Assert.assertEquals(1, testTally.successful.size())
		Assert.assertEquals("${TEST_FUNCTION_NAME}::main(com.ni.lucasway.db.testing.DatasetDrivenFunctionTestCase)".toString(), testTally.successful[0].displayName)

		Assert.assertEquals(1, testTally.failures.size())
		Assert.assertEquals("${TEST_FUNCTION_NAME}::failure(com.ni.lucasway.db.testing.DatasetDrivenFunctionTestCase)".toString(), testTally.failures[0].description.displayName)
    }
}