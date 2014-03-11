package com.ni.lucasway.db.testing

import groovy.json.JsonSlurper

import org.dbunit.database.DatabaseConnection

import org.dbunit.operation.DeleteAllOperation
import org.dbunit.operation.InsertOperation

import org.dbunit.dataset.xml.FlatXmlDataSet

import org.junit.Assert

import org.junit.runner.Description

/**
 * A test case to call a function and assert its results
 */
public class DatasetDrivenFunctionTestCase implements Runnable
{
	def functionName
	def configDir
	def name

	/**
	 * The database will be 
	 */
	def preDataSet
	def parameters
	def expectedOutput

	def jdbcConnection

	/**
	 * @param configDir has files the define the setup and result assertions of the test
	 */
	public DatasetDrivenFunctionTestCase(functionName, configDir)
	{
		this.functionName = functionName
		this.configDir = configDir
		name = configDir.name
		preDataSet = new FlatXmlDataSet(new FileInputStream(new File(configDir, 'dataset.xml')))
		parameters = readFunctionParameters(new FileInputStream(new File(configDir, 'params.json')))
		expectedOutput = readExpectedOutput(new FileInputStream(new File(configDir, 'expected-results.json')))
	}

	def copyOf() {
		return new DatasetDrivenFunctionTestCase(functionName, configDir)
	}

	def readFunctionParameters(confInputStream) {
		return new JsonSlurper().parse(new InputStreamReader(confInputStream))
	}

	def readExpectedOutput(confOutputStream) {
		return new JsonSlurper().parse(new InputStreamReader(confOutputStream))
	}

	@Override
	public void run()
	{
		setupDataSet()
		callAndTestFunction(matchResultSetToExpectedOutput)
	}

	def getDescription() {
		Description.createTestDescription(getClass(), name)
	}

	def setupDataSet() {
		println 'your mom is a whore'
		def dbunitConnection = new DatabaseConnection(jdbcConnection)
		println "Is DBUnit respecting schemas: schemaAware=${dbunitConnection.config.getProperty('http://www.dbunit.org/features/qualifiedTableNames')}"
		dbunitConnection.config.setProperty('http://www.dbunit.org/features/qualifiedTableNames', true)
		println "After updating config property:Is DBUnit respecting schemas: schemaAware=${dbunitConnection.config.getProperty('http://www.dbunit.org/features/qualifiedTableNames')}"
		new DeleteAllOperation().execute(dbunitConnection, preDataSet)
		new InsertOperation().execute(dbunitConnection, preDataSet)
	}

	def callAndTestFunction(assertResultSet)
	{
		def functionCall = null
		def functionResultSet = null

		try
		{
			functionCall = jdbcConnection.prepareCall(printCallableStatement())
			setFunctionParameters(functionCall)

			functionResultSet = functionCall.executeQuery()
			assertResultSet(functionResultSet)
		}
		finally {
			functionResultSet?.close()
			functionCall?.close()
		}
	}

	def printCallableStatement() {
		"{call ${functionName}(${parameters.collect{ '?' }.join(', ')})}"
	}

	def setFunctionParameters(jdbcStmt) {
		parameters.eachWithIndex { item, index -> jdbcStmt.setObject(index + 1, item) }
	}

	def matchResultSetToExpectedOutput = { resultSet ->

		def numResultSetRows = 0
		expectedOutput.each { expectedRow ->
			
			def hasMatchingResultRow = resultSet.next()
			Assert.assertTrue("Function output # rows is incorrect: # rows found was ${numResultSetRows}", hasMatchingResultRow)
			numResultSetRows++

			def columnCount = resultSet.metaData.columnCount
			Assert.assertEquals("Function output row has incorrect # columns: expected=${columnCount}; actual=${expectedRow.size()}", expectedRow.size(), columnCount)
			expectedRow.eachWithIndex { expectedColumn, columnIndex ->
				Assert.assertEquals("Function output column is not expected: expectedValue=${expectedColumn}; actualValue=${resultSet.getObject(columnIndex + 1)}", expectedColumn, resultSet.getObject(columnIndex + 1))
			}
		}
	}
}