package com.ni.lucasway.db

import com.ni.lucasway.model.SqlDependency

class Versioning {
    
    static String versiontable = "lucasway_version"
    
    static exists(sql) {
        def sqlstr = "select exists(select * from information_schema.tables where table_name='${versiontable}') as e"
        def rslt = sql.firstRow(sqlstr.toString())
        rslt.e
    }
	
	static List<SqlDependency> versions(sql) {
		def sqlstr = "select version,entity from ${versiontable}"
		List<String> rslt = []
		sql.eachRow(sqlstr.toString()) { rslt << new SqlDependency(version:it.version, isEntity:true, name:it.entity) }
		rslt 
	}
    
    static dolog(sql, version, applied_on, entity, duration) {
        
        def sqlstr = "insert into ${versiontable}(version,applied_on,entity,duration) values ('${version}', '${applied_on}'::timestamp, '${entity}', '${duration}')"
        sql.execute(sqlstr.toString())
    }
    
    static createtable(sql) {
        def sqlstr = "create table ${versiontable} (version text, applied_on timestamp, entity text, duration integer)"
        sql.execute(sqlstr.toString())
    }
    
    boolean hasRun(version,entity,sql) {
        def sqlstr = "select exists(select * from ${versiontable} where version = '${version}' and entity = '${entity}') as e"
        def rslt = sql.firstRow(sqlstr.toString())
        rslt.e
    }
}
