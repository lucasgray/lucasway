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
	def dataSet
	def invoke = [:] // expect at least 'arguments' entry; optional are 'callAs' and 'schema'.
	def expectedOutput

	def jdbcConnection

	/**
	 * @param configDir has files the define the setup and result assertions of the test
	 */
	public DatasetDrivenFunctionTestCase(functionName, configDir, invokeCommonConfig = null)
	{
		this.functionName = functionName
		this.configDir = configDir
		name = configDir.name
		loadDataSet(new FileInputStream(new File(configDir, 'dataset.xml')))
		loadInvokeConfig(new FileInputStream(new File(configDir, 'invoke.json')), invokeCommonConfig)
		loadExpectedOutput(new FileInputStream(new File(configDir, 'expected-results.json')))
	}

	def copyOf() {
		return new DatasetDrivenFunctionTestCase(functionName, configDir, invoke)
	}

	def loadDataSet(testDataStream) {
		dataSet = new FlatXmlDataSet(testDataStream)
	}

	def loadInvokeConfig(testDataStream, commonConfig) {
		if (commonConfig) invoke.putAll(commonConfig)
		invoke.putAll(new JsonSlurper().parse(new InputStreamReader(testDataStream)))
	}

	def loadExpectedOutput(testDataStream) {
		expectedOutput = new JsonSlurper().parse(new InputStreamReader(testDataStream))
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

	def setupDataSet()
	{
		def dbunitConnection = new DatabaseConnection(jdbcConnection)
		println "Is DBUnit respecting schemas: schemaAware=${dbunitConnection.config.getProperty('http://www.dbunit.org/features/qualifiedTableNames')}"
		dbunitConnection.config.setProperty('http://www.dbunit.org/features/qualifiedTableNames', true)
		println "After updating config property:Is DBUnit respecting schemas: schemaAware=${dbunitConnection.config.getProperty('http://www.dbunit.org/features/qualifiedTableNames')}"
		new DeleteAllOperation().execute(dbunitConnection, dataSet)
		new InsertOperation().execute(dbunitConnection, dataSet)
	}

	def callAndTestFunction(assertResultSet)
	{
		def functionCall = null
		def functionResultSet = null

		try
		{
			functionCall = jdbcConnection.prepareCall(formatPreparedCall())
			setFunctionParameters(functionCall)

			functionResultSet = functionCall.executeQuery()
			assertResultSet(functionResultSet)
		}
		finally {
			functionResultSet?.close()
			functionCall?.close()
		}
	}

	def formatPreparedCall()
	{
		def actualFunctionName
		if (invoke.callAs) actualFunctionName = invoke.callAs
		else if (invoke.schema) actualFunctionName = "${invoke.schema}.${functionName}"
		else actualFunctionName = functionName
		return "{call ${actualFunctionName}(${invoke.arguments.collect{ '?' }.join(', ')})}"
	}

	def setFunctionParameters(jdbcStmt) {
		invoke.arguments.eachWithIndex { item, index ->
			jdbcStmt.setObject(index + 1, item)
		}
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