package com.ni.lucasway.db.testing

import groovy.sql.Sql

import org.junit.Assert
import org.junit.Before
import org.junit.Test

import org.junit.runner.notification.RunNotifier

public class DatasetDrivenFunctionTestRunnerTest
{
	public static def SQL_FUNCTIONS_BASE_DIR = new File('src/test/resources/db-functions')
	public static def SQL_FUNCTIONS_TESTS_BASE_DIR = new File('src/test/resources/db-function-tests')
	public static def TEST_FUNCTION_PATH = '/com/ni/lucasway/db/testing/are_you_mocking_me.sql'

	@Before
	public void setupDatabase()
	{
		def config = ResourceBundle.getBundle('application')
    	def sqlHandle = Sql.newInstance(config.getString('jdbc.url.socialsense'), config.getString('jdbc.username.socialsense'), config.getString('jdbc.password.socialsense'), config.getString('jdbc.driver.socialsense'))
    	def jdbcConnection = sqlHandle.createConnection()
    	def sqlStmtExec = jdbcConnection.createStatement()
    	sqlStmtExec.execute('DROP TABLE IF EXISTS buzzlightyear')
    	sqlStmtExec.execute('CREATE TABLE buzzlightyear (id INTEGER, col1 TEXT, col2 TEXT)')
    	sqlStmtExec.execute(new File(SQL_FUNCTIONS_BASE_DIR, TEST_FUNCTION_PATH).getText())
    	sqlStmtExec.close()
    	jdbcConnection.close()
	}

	@Test
	public void testFindFunctions()
	{
		def testedObject = new DatasetDrivenFunctionTestRunner(SQL_FUNCTIONS_BASE_DIR, SQL_FUNCTIONS_TESTS_BASE_DIR)
		Assert.assertEquals(1, testedObject.functionTests.size())

		def testCases = testedObject.functionTests['/com/ni/lucasway/db/testing/are_you_mocking_me']
		Assert.assertNotNull(testCases)
		Assert.assertEquals(1, testCases.size())

		def testCase = testCases[0]
		Assert.assertEquals('are_you_mocking_me', testCase.functionName)
		Assert.assertEquals('/com/ni/lucasway/db/testing/are_you_mocking_me::main', testCase.name)
		Assert.assertEquals([ "foo", "boo", 1234, true ], testCase.parameters)
		Assert.assertEquals(1, testCase.expectedOutput.size())
		Assert.assertEquals([ 999, "abc", "def" ], testCase.expectedOutput[0])
	}

	@Test
	public void testRun()
	{
		def notifier = { println "Hello: ${it}" }
		def testedObject = new DatasetDrivenFunctionTestRunner(SQL_FUNCTIONS_BASE_DIR, SQL_FUNCTIONS_TESTS_BASE_DIR)
		testedObject.run(notifier as RunNotifier)
    }
}