package com.ni.lucasway.db.testing

import java.text.DecimalFormat

import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier

public class TestResultAggregator extends RunListener
{
	protected static def DURATION_IN_SEC_FORMAT = new DecimalFormat('0.000')

	protected def startTime, endTime
	def duration = { endTime - startTime }

	protected def pending = []

	def allTests = []
	def successful = []
	def ignored = [] // aka skipped
	def failures = []
	def assertionFailures = { failures.findAll{ it.exception instanceof AssertionError } }
	def nonAssertionFailures = { failures.findAll{ ! (it.exception instanceof AssertionError) } }

	public int countPendingTests() { return pending.size() }

	@Override
	public void testRunStarted(Description description) throws Exception {
		endTime = null
		startTime = System.currentTimeMillis()
    }

	@Override
    public void testRunFinished(Result result) throws Exception {
    	endTime = System.currentTimeMillis()
    }

    @Override
    public void testStarted(Description someTest) throws Exception {
    	allTests += someTest
    	pending += someTest
    }

	@Override
	public void testFinished(Description successfulTest) throws Exception {
		if (pending.remove(successfulTest)) {
			successful += successfulTest
		}
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		if (pending.remove(failure.description)) {
			failures += failure
		}
	}

	@Override
	public void testIgnored(Description ignoredTest) throws Exception {
		if (pending.remove(ignoredTest)) {
			ignored += ignoredTest
		}
	}

	protected def printDurationInSeconds()
	{
		def durationInMilliseconds = duration()
		def durationInSeconds = (double) durationInMilliseconds / 1000.0D
		return DURATION_IN_SEC_FORMAT.format(durationInSeconds)
	}

	public RunNotifier asNotifier()
	{
		def notifier = new RunNotifier()
		notifier.addListener(this)
		return notifier
	}

	public void reportResults()
	{
		if (failures.size() > 0) {
			println "FAILURES!!!"
		}
		println ""
		println "Tests run: ${allTests.size()}, Failures: ${assertionFailures().size()}, Errors: ${nonAssertionFailures().size()}, Skipped: ${ignored.size()}, Time elapsed: ${printDurationInSeconds()} sec"
		println ""
		if (! failures.isEmpty()) {
			println "Tests in error:"
			println ""
			failures.each {
				println "${it} ${it.exception.class.name}"
				if (it.exception != null) {
					it.exception.printStackTrace(System.out)
				}
				println ""
			}
		}
	}
}