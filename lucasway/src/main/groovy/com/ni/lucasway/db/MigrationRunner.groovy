package com.ni.lucasway.db

import groovy.sql.Sql

import org.gradle.api.file.ConfigurableFileTree

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.ni.lucasway.LucaswayPlugin
import com.ni.lucasway.functions.DependencyGraph
import com.ni.lucasway.functions.FileParsing
import com.ni.lucasway.model.SqlDependency

public class MigrationRunner
{
	def static Logger LOG = LoggerFactory.getLogger("${LucaswayPlugin.class.name}::MigrationRunner")

	def project
	def sqlSource = { SqlMaker.byProperties(project.lucasway) }

	def run()
	{
		def sql = sqlSource()

		//TODO these should be configs
		ConfigurableFileTree functs =
				project.fileTree(dir:'src/main/sql', excludes:['tables/*'], includes:['**/*.sql'])
		ConfigurableFileTree tables =
				project.fileTree(dir:'src/main/sql/tables', includes:['**/*.sql'])

		LOG.info "Functs: ${functs}" 
		LOG.info "Tables: ${tables}" 

		if (!Versioning.exists(sql)) {
			println "Versioning table does not exist.  Creating it now."
			Versioning.createtable(sql)
		}

		List<SqlDependency> versions = Versioning.versions(sql)

		LOG.debug "Versions: ${versions}"

		def filesAsDeps = FileParsing.parseFiles(functs,tables)

		LOG.debug "Result of file parsing: ${filesAsDeps}"

		Set<SqlDependency> depTrees = DependencyGraph.createGraph(filesAsDeps, versions)

		//print all the top level nodes.
		Set<SqlDependency> parentNodes = depTrees.findAll { it.parents.size == 0}

//			println ""
//			println "--------------------------------------------------------"
//			println "Lucasway has discovered the following migrations to run:"
//			println "--------------------------------------------------------"
//			println ""
//			parentNodes.each {it.prettyPrint()}
//			println ""

		//TODO: detect a cycle and shut. down. everything.

		Set<SqlDependency> entityParents = parentNodes.findAll {it.isEntity}

		if (entityParents != null && entityParents.size() > 0) {
			List<SqlDependency> sortedEntityParents = entityParents.sort(false) { it.version }
			
			println ""
			println "--------------------------------------------------------"
			println "Running Entity Migrations"
			println "--------------------------------------------------------"
			println ""
			sortedEntityParents.each {it.prettyPrint()}
			println ""
			
			runDepthFirst(sortedEntityParents, sql)
		}

		Set<SqlDependency> functionParents = parentNodes.findAll{!it.isEntity && !it.run}

		println ""
		println "--------------------------------------------------------"
		println "Refreshing Functions"
		println "--------------------------------------------------------"
		println ""
		functionParents.each {it.prettyPrint()}
		println ""
		
		println "--------------------------------------------------------"
		println "--------------------------------------------------------"
		println ""
		
		runDepthFirst(functionParents, sql)

		if (project.lucasway.auditing) {
			LOG.info "Logging success to audit table"
			Auditing.doAuditing(sql, System.getProperty("user.name"))
		}
	}

	void runDepthFirst(trees, sql) {
		trees.each { node ->
			//run entire tree starting from leaves and going up
			node.depthFirst().each { child ->
				println "Running ${child.name} because ${node.name} depends on it"
				runIfNecessaryAndUpdateGraph(child, sql)
			}
		}
	}

	void runIfNecessaryAndUpdateGraph(SqlDependency node, Sql sql) {

		if (!node.run) {
			Long before = System.currentTimeMillis()
			sql.execute(node.sql)
			Long after = System.currentTimeMillis()
			println "Took: ${after-before}ms"

			if (node.isEntity) {
				LOG.info "Updating schema_version for: ${node.name}:${node.version}"
				Versioning.dolog(sql, node.version, new Date(), node.name, after-before)
			}

			node.run = true

		} else {
			println 'Already run, ignoring'
		}
	}
}