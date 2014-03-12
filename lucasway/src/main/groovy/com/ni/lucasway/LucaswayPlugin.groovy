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

		project.extensions.create("lucasway", LucaswayPluginExtension)

		project.configurations.create('sql')
		project.configurations.sql.transitive = false
		project.task('newMigration') <<  {
			java.text.DateFormat df = new java.text.SimpleDateFormat('yyyyMMddSSSS')
			println "use V${df.format(new Date())}"
		}

		project.task('test') << {
			
			if (project.lucasway.skipTests) {
				println "Lucasway: Skipping Migration Tests"
				return
			}

			def testJdbcProperties = new Properties()
			new FileInputStream(new File('src/test/resources/migration-jdbc.properties')).withStream {
				testJdbcProperties.load(it)
			}

			println ""
			println "--------------------------------------------------------"
			println "Lucasway: Testing Migrations"
			println "--------------------------------------------------------"
			println ""
			println "\tTest Database Url: ${testJdbcProperties.url}"
			println "\tTest Database Driver: ${testJdbcProperties.driver}"
			println "\tTest Database Username: ${testJdbcProperties.username}"
			println "\tTest SQL File Base: ${project.lucasway.sqlFiles}"
			println ""

			SqlMaker.loadClasspathWithSqlDriver(project, testJdbcProperties.driver)
			
		    def migrationRunner = new MigrationRunner(project: project, sqlSource: SqlMaker.byProperties(testJdbcProperties))
		    migrationRunner.run()

		    def testSuiteRunner = new DatasetDrivenFunctionTestRunner(sqlSource: SqlMaker.byProperties(testJdbcProperties))
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
			
			SqlMaker.loadClasspathWithSqlDriver(project)

			new MigrationRunner(project: project, sqlSource: SqlMaker.byProperties(project.lucasway)).run()
		}

		project.tasks.getByName('lucaswayMigrate').dependsOn(project.tasks.getByName('test'))
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

	Boolean skipTests
}