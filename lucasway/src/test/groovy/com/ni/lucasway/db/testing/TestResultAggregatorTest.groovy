package com.ni.lucasway.db.testing

import org.junit.Assert
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.notification.Failure

public class TestResultAggregatorTest
{
	@Test
	public void testTestRunStarted()
	{
		def testedObject = new TestResultAggregator()
		def testSuiteDescription = Description.createSuiteDescription(TestResultAggregatorTest.class)
		testedObject.testRunStarted(testSuiteDescription)
		Assert.assertEquals(0, testedObject.allTests.size())
		Assert.assertEquals(0, testedObject.successful.size())
		Assert.assertEquals(0, testedObject.failures.size())
		Assert.assertEquals(0, testedObject.ignored.size())
		Assert.assertEquals(0, testedObject.countPendingTests())
		Assert.assertNotNull(testedObject.startTime)
		Assert.assertNull(testedObject.endTime)
	}

	@Test
	public void testTestRunFinished()
	{
		def testedObject = new TestResultAggregator()
		def testSuiteDescription = Description.createSuiteDescription(TestResultAggregatorTest.class)
		testedObject.testRunStarted(testSuiteDescription)
		Assert.assertEquals(0, testedObject.allTests.size())
		Assert.assertEquals(0, testedObject.successful.size())
		Assert.assertEquals(0, testedObject.failures.size())
		Assert.assertEquals(0, testedObject.ignored.size())
		Assert.assertEquals(0, testedObject.countPendingTests())
		Assert.assertNotNull(testedObject.startTime)
		Assert.assertNull(testedObject.endTime)
		testedObject.testRunFinished(null)
		Assert.assertNotNull(testedObject.endTime)
	}

	@Test
	public void testTestStarted()
	{
		def testedObject = new TestResultAggregator()
		def testSuiteDescription = Description.createSuiteDescription(TestResultAggregatorTest.class)
		testedObject.testRunStarted(testSuiteDescription)
		def startedTest = Description.createTestDescription(TestResultAggregatorTest.class, 'testMethodX')
		testedObject.testStarted(startedTest)
		Assert.assertEquals(1, testedObject.allTests.size())
		Assert.assertEquals(0, testedObject.successful.size())
		Assert.assertEquals(0, testedObject.failures.size())
		Assert.assertEquals(0, testedObject.ignored.size())
		Assert.assertEquals(1, testedObject.countPendingTests())
	}

	@Test
	public void testTestFinished()
	{
		def testedObject = new TestResultAggregator()
		def testSuiteDescription = Description.createSuiteDescription(TestResultAggregatorTest.class)
		testedObject.testRunStarted(testSuiteDescription)
		def successfulTest = Description.createTestDescription(TestResultAggregatorTest.class, 'testMethodX')
		testedObject.testStarted(successfulTest)
		Assert.assertEquals(1, testedObject.allTests.size())
		Assert.assertEquals(0, testedObject.successful.size())
		Assert.assertEquals(0, testedObject.failures.size())
		Assert.assertEquals(0, testedObject.ignored.size())
		Assert.assertEquals(1, testedObject.countPendingTests())
		testedObject.testFinished(successfulTest)
		Assert.assertEquals(1, testedObject.allTests.size())
		Assert.assertEquals(1, testedObject.successful.size())
		Assert.assertEquals(0, testedObject.countPendingTests())
	}

	@Test
	public void testTestFailure()
	{
		def testedObject = new TestResultAggregator()
		def testSuiteDescription = Description.createSuiteDescription(TestResultAggregatorTest.class)
		testedObject.testRunStarted(testSuiteDescription)
		def failingByAssertionTest = Description.createTestDescription(TestResultAggregatorTest.class, 'testMethodY')
		testedObject.testStarted(failingByAssertionTest)
		Assert.assertEquals(1, testedObject.allTests.size())
		Assert.assertEquals(0, testedObject.successful.size())
		Assert.assertEquals(0, testedObject.failures.size())
		Assert.assertEquals(0, testedObject.ignored.size())
		def failureByAssertion = new Failure(failingByAssertionTest, new AssertionError('You shall not pass'))
		testedObject.testFailure(failureByAssertion)
		Assert.assertEquals(1, testedObject.allTests.size())
		Assert.assertEquals(0, testedObject.successful.size())
		Assert.assertEquals(1, testedObject.failures.size())
		Assert.assertEquals(0, testedObject.ignored.size())
		Assert.assertEquals(1, testedObject.assertionFailures().size())
		Assert.assertEquals(0, testedObject.nonAssertionFailures().size())
		Assert.assertEquals(0, testedObject.countPendingTests())

		// call testFailure again and ensure testedObject does not count the failure twice
		testedObject.testFailure(failureByAssertion)
		Assert.assertEquals(1, testedObject.failures.size())

		def failingByNonAssertionTest = Description.createTestDescription(TestResultAggregatorTest.class, 'testMethodZ')
		testedObject.testStarted(failingByNonAssertionTest)
		def failureByNonAssertion = new Failure(failingByNonAssertionTest, new IOException('I expected to /dev/null to be readable; what gives?'))
		testedObject.testFailure(failureByNonAssertion)
		Assert.assertEquals(2, testedObject.allTests.size())
		Assert.assertEquals(0, testedObject.successful.size())
		Assert.assertEquals(2, testedObject.failures.size())
		Assert.assertEquals(0, testedObject.ignored.size())
		Assert.assertEquals(1, testedObject.assertionFailures().size())
		Assert.assertEquals(1, testedObject.nonAssertionFailures().size())
		Assert.assertEquals(0, testedObject.countPendingTests())
	}

	@Test
	public void testTestIgnored()
	{
		def testedObject = new TestResultAggregator()
		def testSuiteDescription = Description.createSuiteDescription(TestResultAggregatorTest.class)
		testedObject.testRunStarted(testSuiteDescription)
		def ignoredTest = Description.createTestDescription(TestResultAggregatorTest.class, 'testMethodA')
		testedObject.testStarted(ignoredTest)
		Assert.assertEquals(1, testedObject.allTests.size())
		Assert.assertEquals(0, testedObject.successful.size())
		Assert.assertEquals(0, testedObject.failures.size())
		Assert.assertEquals(0, testedObject.ignored.size())
		Assert.assertEquals(1, testedObject.countPendingTests())
		testedObject.testIgnored(ignoredTest)
		Assert.assertEquals(1, testedObject.allTests.size())
		Assert.assertEquals(0, testedObject.successful.size())
		Assert.assertEquals(0, testedObject.failures.size())
		Assert.assertEquals(1, testedObject.ignored.size())
		Assert.assertEquals(0, testedObject.countPendingTests())
	}

	@Test
	public void testReportResults()
	{
		def testedObject = new TestResultAggregator()
		testedObject.testRunStarted(describeSuite())
		simulateRun(testedObject, ranAsSuccess, describeTest('testMethodX'))
		simulateRun(testedObject, ranAsFailure(new AssertionError('You shall not pass')), describeTest('testMethodY'))
		simulateRun(testedObject, ranAsFailure(new IOException('I expected to /dev/null to be readable; what gives?')), describeTest('testMethodZ'))
		simulateRun(testedObject, ranAsIgnored, describeTest('testMethodA'))
		testedObject.testRunFinished(null)

		testedObject.reportResults()
	}

	def simulateRun(testedObject, notifyAfterStarted, description) {
		testedObject.testStarted(description)
		notifyAfterStarted(testedObject, description)
	}

	def ranAsSuccess = { testedObject, description ->
		testedObject.testFinished(description)
	}

	def ranAsFailure(throwable) {
		return { testedObject, description ->
			testedObject.testFailure(new Failure(description, throwable))
		}
	}

	def ranAsIgnored = { testedObject, description ->
		testedObject.testIgnored(description)
	}

	def describeSuite() {
		Description.createSuiteDescription(TestResultAggregatorTest.class)
	}

	def describeTest(testedMethodName) {
		Description.createTestDescription(TestResultAggregatorTest.class, testedMethodName)
	}
}