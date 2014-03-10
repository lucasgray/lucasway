package com.ni.lucasway.db.testing

import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier

public class TestResultAggregator extends RunListener
{
	protected def startTime, endTime
	def duration { endTime - startTime }

	def allTests = []
	def successes = []
	def skipped = []
	def failures = []
	def assertionFailures = { failures.retainAll{ it.exception instanceof AssertionError } }
	def nonAssertionFailures = { failures.retainAll{ ! (it.exception instanceof AssertionError) } }

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
	public void testFinished(Description success) throws Exception {
		successes += success
	}

	@Override
	public void testFailure(Failure failure) throws Exception {
		failures += failure
	}

	protected def printDuration() {

	}

	public RunNotifier asNotifier()
	{
		def notifier = new RunNotifier()
		notifier.addListener(this)
		return notifier
	}

	public void reportResults()
	{
		println ""
		println "Tests run: ${allTests.size()}, Failures: ${assertionFailures.size()}, Errors: ${nonAssertionFailures.size()}, Skipped: ${skipped.size()}, Time elapsed: 2.318 sec"
		println ""
		println "Results:"
		println ""
		println "Tests in error:"
		println ""
		failures.each {
			println "${it.toString()}"
		}
	}
}