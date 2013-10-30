package com.ni.lucasway.functions

import org.gradle.api.file.ConfigurableFileTree
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.ni.lucasway.LucaswayPlugin
import com.ni.lucasway.model.SqlDependency


/**
 * Functions to parse the migration files
 */
class FileParsing {
	static Logger LOG = LoggerFactory.getLogger(LucaswayPlugin.class)
	
	static Map<String,List<SqlDependency>> parseFiles(ConfigurableFileTree fucnts, ConfigurableFileTree tables) {
		
		List<String> metaBuffer = []
		List<String> sqlBuffer = []
		
		List<SqlDependency> migrations = []
		List<SqlDependency> functions = []
		
		tables.each { file ->
			//bit to flip to indicate we are in a meta block or not
			boolean inMeta = false
			file.withReader { reader ->
				
				reader.eachLine { line ->
				
					LOG.debug "Line: ${line}"
					if (line.trim().matches('^-{2,}$')) {
						LOG.debug "Boundary found.  Switching context. Current inMeta: ${inMeta}"
						inMeta = !inMeta
						
						if (metaBuffer.size()>0 && sqlBuffer.size()>0) {
							migrations << flush(metaBuffer,sqlBuffer)
							
							metaBuffer = []
							sqlBuffer = []
						}
						
					} else {
						if (inMeta) {
							metaBuffer << line
						} else {
							sqlBuffer << line
						}
					}
				}
				if (metaBuffer.size()>0 && sqlBuffer.size()>0) {
					migrations << flush(metaBuffer,sqlBuffer)
					
					metaBuffer = []
					sqlBuffer = []
				}
			}
		}
		
		functions = parseFunctions(fucnts)
		
		return ["migrations":migrations, "functions":functions] as Map
	}
	
	static SqlDependency flush(List<String> metaBuffer, List<String> sqlBuffer) {
		LOG.info "Flush called"
		LOG.debug "State of meta buffer: ${metaBuffer}"
		LOG.debug "State of sql buffer: ${sqlBuffer}"
		
		SqlDependency migration = new SqlDependency()
		migration.isEntity = true
		migration.sql = sqlBuffer.join("\n")
		
		//--V201309140000_CreateTable
		migration.version = metaBuffer.collect {it.find('^--.?V[\\d]{12}_.*') }.find{it!=null}.find('V[\\d]{12}_.*')
		
		//--entity:dataview
		migration.name = metaBuffer.collect {it.find('^--.?entity:.*') }.find{it!=null}.find('entity:.*').replaceAll('entity:', '')
		
		migration.childNames = findChildren(metaBuffer.join("\n"))
		
		migration
	 }
	
	static List<SqlDependency> parseFunctions(ConfigurableFileTree functs) {
		
		def ret = []
		
		functs.each {file ->
			
			SqlDependency migration = new SqlDependency()
			migration.sql = file.text
			migration.isEntity = false
			migration.name = file.name
			migration.childNames = findChildren(file.text)
			
			ret << migration
		}
		
		ret
	}
	
	static List<String> findChildren(String text) {
		String depLine = text.split("\n").collect {it.find('^--dependsOn:.*') }.find{it!=null}
		
		if (depLine==null) {
			return []
		}
		
		List<String> children = []

		//This is the de-facto way to declare dependencies now, --dependsOn[a.sql,vw_b.sql,fnct_c.sql]
		//They can be functions or views or tables or anything.
		try {
			if(depLine.contains("dependsOn")) {
				children = depLine.substring(depLine.indexOf("[")+1, depLine.indexOf("]")).split(",")
			}
		} catch (Exception e) {
			//do nothing, just assume we dont have any dependencies
			println "Caught exception.  Assuming (hoping) no dependencies."
			LOG.error("Error:",e)
		}

		children
	}
}
