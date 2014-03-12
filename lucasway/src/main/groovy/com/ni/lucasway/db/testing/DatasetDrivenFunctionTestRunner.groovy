package com.ni.lucasway.db.testing

import java.util.concurrent.Executors

import groovy.sql.Sql

import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier

public class DatasetDrivenFunctionTestRunner extends Runner
{
	public static def SQL_FUNCTIONS_BASE_DIR = new File('src/main/sql/functions')
	public static def SQL_FUNCTIONS_TESTS_BASE_DIR = new File('src/test/sql/functions')

	def sqlSource // LucaswayPlugin or other container will inject this.

	def functionSourceBaseDir
	def functionTestBaseDir
	def functionTests = [:]

	public DatasetDrivenFunctionTestRunner() {
		this(SQL_FUNCTIONS_BASE_DIR, SQL_FUNCTIONS_TESTS_BASE_DIR)
	}

	public DatasetDrivenFunctionTestRunner(functionSourceBaseDir, functionTestBaseDir) {
		this.functionSourceBaseDir = DirectoryScanUtils.ensureFileTyped(functionSourceBaseDir)
		this.functionTestBaseDir = DirectoryScanUtils.ensureFileTyped(functionTestBaseDir)
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
					functionTestsToExpect += functionTestName
				}
			}
		}

		functionTestBaseDir.eachDirRecurse { dir ->
			
			def relativePath = DirectoryScanUtils.getRelativePath(functionTestBaseDir, dir)
			
			if (functionTestsToExpect.contains(relativePath))
			{
				functionTests[relativePath] = findFunctionTestCases(dir)
				functionTests[relativePath].each {
					it.name = "${relativePath}::${it.name}".toString()
				}
			}
		}
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
    	return Description.createSuiteDescription(DatasetDrivenFunctionTestRunner.class)
    }

	/**
     * {@inheritDoc}
     */
    @Override
    public void run(RunNotifier notifier)
    {
    	notifier.fireTestRunStarted(getDescription())

    	if (functionTests.isEmpty()) { findFunctionTests() }

    	def dbAccessor = sqlSource()
    	def jdbcConnection = dbAccessor.createConnection()

    	try
    	{
    		def testFutures = []

	    	functionTests.each { testName, testCases ->

	    		testFutures.addAll(
	    			testCases.collect { testCase ->

	    				println "Running SQL Function Test Case: ${testCase.name}"
		    			notifier.fireTestStarted(testCase.description)
		    			
		    			try
		    			{
		    				testCase.jdbcConnection = jdbcConnection
		    				testCase.run()
		    				notifier.fireTestFinished(testCase.description)
		    			}
		    			catch (Throwable thrownByTestCase) {
		    				notifier.fireTestFailure(new Failure(testCase.description, thrownByTestCase))
		    			}
		    		})
	    	}
    	}
    	finally {
    		jdbcConnection.close()
    		notifier.fireTestRunFinished(null)
    	}
    }
}