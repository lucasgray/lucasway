package com.ni.lucasway.db

import groovy.sql.Sql

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlMaker {
    
    private static final Logger LOG = LoggerFactory.getLogger(SqlMaker.class)

    /**
     * Provide a Sql instance whose properties come from a bundle;
     * Expect 'url', 'driver', 'username', and 'password'.
     * 
     * The unit test properties will come from a bundled properties file.
     */
    def static byBundle(ResourceBundle bundle)
    {
		return {
			LOG.info "Connecting to database: url=${bundle.getString('url')}; driver=${bundle.getString('driver')}"
			SqlMaker.makeSql(
				bundle.getString('url'), bundle.getString('driver'),
				bundle.getString('username'), bundle.getString('password')
			)
		}
    }

    /**
     * Provide a Sql instance whose properties come from a the containing
     * project which applied this Lucasway plugin.
     * 
     * Expect 'url', 'driver', 'username', and 'password'.
     * 
     * The actual migration properties will come by the containing project which
     * sets the properties in the lucasway plugin configuration.
     */
    def static byProperties(Map properties)
    {
		return {
			SqlMaker.makeSql(
				properties.url, properties.driver,
				properties.username, properties.password
			)
		}
    }

    static Sql makeSql(String url, String driver, String username, String password)
    {
        def sql = Sql.newInstance(driver:driver, url:url, user:username, password:password)
        sql.metaClass.url = url
        return sql
    }
	
	static void loadClasspathWithSqlDriver(project, driver = null) {
		try {
			project.configurations.sql.resolve().each {
				project.gradle.class.classLoader.addURL(it.toURI().toURL())
			}
			
			Class.forName(driver ?: project.lucasway.driver)
		} catch (Exception e) {
			println "ERROR: Problem loading database driver. Cannot continue"
			throw new RuntimeException(e)
		}
	}

}