package com.ni.lucasway.functions

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.ni.lucasway.model.SqlDependency


/**
 * Functions to build the dependency graphs
 */
class DependencyGraph {
	
	static Logger LOG = LoggerFactory.getLogger(DependencyGraph.class)
	
	static Set<SqlDependency> createGraph(Map<String,List<SqlDependency>> deps, List<SqlDependency> versions) {
		
		//for every dep, set the parent and child pointers correctly.
		//do not worry that the data structure mimics the real life DAG,
		//because we can easily find the top level nodes and quickly and
		//easily run through all the pointers to get what we need.
		
		//1. prune all entity migrations that have been run
		def migrations = deps['migrations'].findAll { toRun ->
			versions.findIndexOf {alreadyRun ->
				toRun.version.equals(alreadyRun.version) && 
				toRun.name.equals(alreadyRun.name)
			} == -1
		}
		
		def functions = deps['functions']
		
		//2. make the dependency tree for the migrations AND functions at the same time
		def allMigrations = []
		allMigrations.addAll(migrations)
		allMigrations.addAll(functions)
		
		def trees = buildTrees(allMigrations)
		
		trees
	}
	
	
	static Set<SqlDependency> buildTrees(List<SqlDependency> allMigrations) {
		
		Set<SqlDependency> fullTrees = [] as Set<SqlDependency>
		
		//set children and parent references for every SqlDependency
		allMigrations.each { migration ->
			if (migration.childNames != null && migration.childNames.size() > 0) {
				LOG.debug "Found childnames for ${migration.name}:${migration.version}"
				migration.childNames.each { childName ->
					SqlDependency childDep = allMigrations.find {it.name.equals(childName)}
					if (childDep != null) {
						migration.children << childDep
						childDep.parents << migration
					}
				}
			}
			fullTrees << migration
		}	
		fullTrees
	}
}
