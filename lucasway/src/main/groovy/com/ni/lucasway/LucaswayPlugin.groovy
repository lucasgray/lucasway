package com.ni.lucasway

import groovy.sql.Sql

import groovy.transform.Canonical

import org.gradle.api.Plugin
import org.gradle.api.Project

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.ni.lucasway.db.MigrationRunner
import com.ni.lucasway.db.SqlMaker
import com.ni.lucasway.db.testing.DatasetDrivenFunctionTestRunner
import com.ni.lucasway.db.testing.TestResultAggregator

/**
 * Front gate for Lucasway Plugin.
 * Defines tasks: 'lucaswayMigrate', 'newMigration'
 * OutOfOrder is true by default. //TODO implement running not out of order.
 */
class LucaswayPlugin implements Plugin<Project> {
	Logger LOG = LoggerFactory.getLogger(LucaswayPlugin.class)
	
	void apply(Project project) {
		
		SqlMaker.loadClasspathWithSqlDriver(project)

		project.extensions.create("lucasway", LucaswayPluginExtension)

		project.configurations.create('sql')
		project.configurations.sql.transitive = false
		project.task('newMigration') <<  {
			java.text.DateFormat df = new java.text.SimpleDateFormat('yyyyMMddSSSS')
			println "use V"+df.format(new Date())
		}

		project.task('test') << {
			println ""
			println "--------------------------------------------------------"
			println "Lucasway: Testing Migrations"
			println "--------------------------------------------------------"
			println ""
			println "\tTest Database Url: ${project.lucasway.test.url}"
			println "\tTest Database Driver: ${project.lucasway.test.driver}"
			println "\tTest Database Username: ${project.lucasway.test.username}"
			println "\tTest Sql File Base: ${project.lucasway.sqlFiles}"
			println ""

		    def migrationRunner = new MigrationRunner(project: project, sqlSource: SqlMaker.byProperties(project.lucasway.test))
		    migrationRunner.run()

		    def testSuiteRunner = new DatasetDrivenFunctionTestRunner(sqlSource: SqlMaker.byProperties(project.lucasway.test))
		    def testResults = new TestResultAggregator()
		    testSuiteRunner.run(testResults.asNotifier())
		    testResults.reportResults()
		    if (! testResults.failures.isEmpty()) {
		    	throw new RuntimeException('Migrations must pass all unit tests.')
		    }
		}

		project.task('lucaswayMigrate') << {

			println ""
			println "--------------------------------------------------------"
			println "Lucasway Configuration:"
			println "--------------------------------------------------------"
			println ""
			println "\tDatabase Url: ${project.lucasway.url}"
			println "\tDatabase Driver: ${project.lucasway.driver}"
			println "\tDatabase Username: ${project.lucasway.username}"
			println "\tAuditing: ${project.lucasway.auditing}"
			println "\tSql File Base: ${project.lucasway.sqlFiles}"
			println ""
			
			new MigrationRunner(sqlSource: SqlMaker.byProperties(project.lucasway)).run()
			runFunctionTests(testSql)
		}

		project.getTasks().getByName('lucaswayMigrate').dependsOn(project.getTasks().getByName('test'))
	}
}

@Canonical
class LucaswayPluginExtension {
	String url
	String driver
	String username
	String password

	Boolean auditing
	String sqlFiles

	Map test = [:]
}