package com.ni.lucasway.db.testing

import java.util.concurrent.Executors

import groovy.io.FileType
import groovy.sql.Sql
import groovy.json.JsonSlurper

import org.dbunit.dataset.CompositeDataSet
import org.dbunit.dataset.xml.FlatXmlDataSet

import org.junit.runner.Description
import org.junit.runner.Runner
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier

import com.ni.lucasway.utils.DirectoryScanUtils
import com.ni.lucasway.utils.ObjectNode

public class DatasetDrivenFunctionTestRunner extends Runner
{
	public static def SQL_FUNCTIONS_BASE_DIR = new File('src/main/sql/functions')
	public static def SQL_FUNCTIONS_TESTS_BASE_DIR = new File('src/test/sql/functions')

	def sqlSource // LucaswayPlugin or other container will inject this.
	protected def dbAccessor
    protected def jdbcConnection

	def functionSourceBaseDir
	def functionTestBaseDir
	def functionTests
    
	public DatasetDrivenFunctionTestRunner() {
		this(SQL_FUNCTIONS_BASE_DIR, SQL_FUNCTIONS_TESTS_BASE_DIR)
	}

	public DatasetDrivenFunctionTestRunner(functionSourceBaseDir, functionTestBaseDir) {
		this.functionSourceBaseDir = DirectoryScanUtils.ensureFileTyped(functionSourceBaseDir)
		this.functionTestBaseDir = DirectoryScanUtils.ensureFileTyped(functionTestBaseDir)
	}

	def scanFunctionTestDirectories()
	{
		println "Scanning SQL and SQL Test Directories: sqlDirBase=${functionSourceBaseDir.path}; testDirBase=${functionTestBaseDir.path}"
		def functionTestsToExpect = []

		functionSourceBaseDir.eachDirRecurse { dir ->
			dir.eachFile { sqlFunctionFile ->
				if (sqlFunctionFile.name.endsWith('.sql')) {
					def functionTestName = DirectoryScanUtils.stripExtension(DirectoryScanUtils.getRelativePath(functionSourceBaseDir, sqlFunctionFile))
					functionTestsToExpect += functionTestName
				}
			}
		}

		functionTests = DirectoryScanUtils.buildAndProcessNodes(functionTestBaseDir) { treeDir, parentNode ->
			def functionPath = DirectoryScanUtils.getRelativePath(functionTestBaseDir, treeDir)
            if (functionTestsToExpect.contains(functionPath)) {
            	return [
            		testName: functionPath, is_test: true,
            		testCases: harvestFunctionTestCases(treeDir, functionPath, parentNode?.value?.testContext)
            	]
			}
			else {
				return [
					is_test: false,
				    testContext: gatherTestContext(treeDir, parentNode?.value?.testContext)
				]
			}
		}
	}

	def gatherTestContext(testDir, parentTestContext = null)
	{
		def testContext = [dataSet: null, invoke: [:]]

		testDir.eachFile(FileType.FILES) { file ->
			def configNameMatch = file.name =~ /common-(.+)(?:\.\w+)+/
			if (configNameMatch.size() == 1) {
				def configResource = configNameMatch[0][1]
				if (configResource.equals('dataset')) {
					testContext.dataSet = new FlatXmlDataSet(new FileInputStream(file))
				}
				else if (configResource.equals('invoke')) {
					file.withInputStream {
						testContext.invoke = new JsonSlurper().parse(it)
					}
				}
			}
		}

		mergeParentTestContext(testContext, parentTestContext)

		return testContext
	}

	def mergeParentTestContext(testContext, parentTestContext)
	{
        if (parentTestContext) {
            if (parentTestContext?.dataSet) {
            	println "Parent test context has a data set"
                testContext.dataSet = new CompositeDataSet(parentTestContext.dataSet, testContext.dataSet)
            }
            testContext.invoke.putAll(parentTestContext.invoke)
        }
    }
    
	def harvestFunctionTestCases(functionTestDir, functionPath, testContext)
	{
		def testCases = []
		functionTestDir.eachDir { testCaseDir ->
			testCases += new DatasetDrivenFunctionTestCase("${functionPath}::${testCaseDir.name}", testCaseDir, testContext)
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
        if (! functionTests) {
        	scanFunctionTestDirectories()
        }

    	try
    	{
    		dbAccessor = sqlSource()
    		jdbcConnection = dbAccessor.createConnection()

    		notifier.fireTestRunStarted(getDescription())

	    	functionTests.find(ObjectNode.breadthFirst) { node ->
	    		node.value.is_test
	    	}.each { testCaseNode ->
    			println "Running SQL Function Test Cases: functionName=${testCaseNode.name}; #cases=${testCaseNode.value.testCases.size()}"
    			testCaseNode.value.testCases.each { runTestCase(it, notifier) }
	    	}
    	}
    	finally {
    		jdbcConnection?.close()
    		notifier.fireTestRunFinished(null)
    	}
    }

    protected void runTestCase(testCase, notifier)
    {
		println "Running SQL Function Test Case: testCase=${testCase.description}"
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
    }
}