package com.ni.lucasway.db.testing

import groovy.json.JsonSlurper

import org.dbunit.database.DatabaseConnection

import org.dbunit.operation.DeleteAllOperation
import org.dbunit.operation.InsertOperation

import org.dbunit.dataset.xml.FlatXmlDataSet

import org.junit.Assert

/**
 * A test case to call a function and assert its results
 */
public class DatasetDrivenFunctionTestCase
{
	def functionName
	def name

	/**
	 * The database will be 
	 */
	def preDataSet
	def parameters
	def expectedOutput

	/**
	 * @param configDir has files the define the setup and result assertions of the test
	 */
	public DatasetDrivenFunctionTestCase(functionName, configDir)
	{
		this.functionName = functionName
		this.name = configDir.name
		preDataSet = new FlatXmlDataSet(new FileInputStream(new File(configDir, 'dataset.xml')))
		parameters = readFunctionParameters(new FileInputStream(new File(configDir, 'params.json')))
		expectedOutput = readExpectedOutput(new FileInputStream(new File(configDir, 'expected-results.json')))
	}

	def readFunctionParameters(confInputStream) {
		return new JsonSlurper().parse(new InputStreamReader(confInputStream))
	}

	def readExpectedOutput(confOutputStream) {
		return new JsonSlurper().parse(new InputStreamReader(confOutputStream))
	}

	def run(jdbcConnection) {
		setupDataSet(jdbcConnection)
		callAndTestFunction(jdbcConnection, matchResultSetToExpectedOutput)
	}

	def setupDataSet(jdbcConnection) {
		def dbunitConnection = new DatabaseConnection(jdbcConnection)
		new DeleteAllOperation().execute(dbunitConnection, preDataSet)
		new InsertOperation().execute(dbunitConnection, preDataSet)
	}

	def callAndTestFunction(jdbcConnection, assertResultSet)
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
			functionCall?.close()
			functionResultSet?.close()
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