package com.ni.lucasway.db

import groovy.sql.Sql

class SqlMaker {
    
    static Sql byProperties(Map properties) {
		
		return {
			SqlMaker.makeSql(
				properties.url, properties.driver,
				properties.username, properties.password
			)
		}
    }

    static Sql makeSql(String url, String driver, String username, String password) {

        return Sql.newInstance(driver:driver,url:url,user:username,password:password)
    }
	
	static void loadClasspathWithSqlDriver(project) {
		try {
			project.configurations.sql.resolve().each {
				project.gradle.class.classLoader.addURL(it.toURI().toURL())
			}
			
			Class.forName(project.lucasway.driver)
		} catch (Exception e) {
			println "ERROR: Problem loading database driver. Cannot continue"
			throw new RuntimeException(e)
		}
	}

}

