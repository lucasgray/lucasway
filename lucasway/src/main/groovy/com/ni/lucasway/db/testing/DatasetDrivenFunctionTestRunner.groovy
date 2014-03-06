package com.ni.lucasway.db.testing

import groovy.sql.Sql

import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier

public class DatasetDrivenFunctionTestRunner extends Runner
{
	public static def SQL_FUNCTIONS_BASE_DIR = new File('src/main/sql/functions')
	public static def SQL_FUNCTIONS_TESTS_BASE_DIR = new File('src/test/sql/functions')
	
	// map test names to test cases
	def functionSourceBaseDir
	def functionTestBaseDir
	def functionTests = [:]

	public DatasetDrivenFunctionTestRunner() {
		this(SQL_FUNCTIONS_BASE_DIR, SQL_FUNCTIONS_TESTS_BASE_DIR)
	}

	public DatasetDrivenFunctionTestRunner(functionSourceBaseDir, functionTestBaseDir) {
		this.functionSourceBaseDir = DirectoryScanUtils.ensureFileTyped(functionSourceBaseDir)
		this.functionTestBaseDir = DirectoryScanUtils.ensureFileTyped(functionTestBaseDir)
		findFunctionTests()
	}

	def findFunctionTests()
	{
		def functionTestsToExpect = []

		functionSourceBaseDir.eachDirRecurse { dir ->
			println "Inspecting SQL functions to test: baseDirectory=${dir}"
			dir.eachFile { sqlFunctionFile ->
				if (sqlFunctionFile.name.endsWith('.sql'))
				{
					def functionTestName = DirectoryScanUtils.stripExtension(DirectoryScanUtils.getRelativePath(functionSourceBaseDir, sqlFunctionFile))
					println "SQL file found: location=${sqlFunctionFile}; functionTestName=${functionTestName}"
					functionTestsToExpect += functionTestName
				}
			}
		}

		functionTestBaseDir.eachDirRecurse { dir ->
			def relativePath = DirectoryScanUtils.getRelativePath(functionTestBaseDir, dir)
			if (functionTestsToExpect.contains(relativePath)) {
				functionTests[relativePath] = findFunctionTestCases(dir)
				functionTests[relativePath].each {
					it.name = "${relativePath}::${it.name}".toString()
				}
			}
		}

		return functionTests
	}

	def findFunctionTestCases(functionTestDir)
	{
		def testCases = []
		functionTestDir.eachDir { testCaseDir ->
			testCases += new DatasetDrivenFunctionTestCase(functionTestDir.getName(), testCaseDir)
		}
		return testCases
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public Description getDescription() {
    	return Description.createSuiteDescription(DatasetDrivenFunctionRunner.class)
    }

	/**
     * {@inheritDoc}
     */
    @Override
    public void run(final RunNotifier notifier)
    {
    	def config = ResourceBundle.getBundle('application')
    	def sqlHandle = Sql.newInstance(config.getString('jdbc.url.socialsense'), config.getString('jdbc.username.socialsense'), config.getString('jdbc.password.socialsense'), config.getString('jdbc.driver.socialsense'))
    	def jdbcConnection = sqlHandle.createConnection()

    	functionTests.each { testName, testCases ->
    		testCases.each { testCase ->
    			def testCaseDescription = Description.createSuiteDescription(testCase.name, testCase.getClass())
    			
    			notifier.fireTestStarted(testCaseDescription)
    			
    			try
    			{
    				testCase.run(jdbcConnection)
    				notifier.fireTestFinished(testCaseDescription)
    			}
    			catch (Throwable caseThrown) {
    				notifier.fireTestFailure(new Failure(testCaseDescription, caseThrown))
    			}
    		}
    	}
    }
}