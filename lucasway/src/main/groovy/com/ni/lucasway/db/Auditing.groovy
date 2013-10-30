package com.ni.lucasway.db

class Auditing {
    
    static String audittable = "lucasway_audit"
    
    static void doAuditing(sql, username) {
        if (!exists(sql)) {
            createtable(sql)
        }
        dolog(sql, username)
    }
    
    static exists(sql) {
        def sqlstr = "select exists(select * from information_schema.tables where table_name='${audittable}') as e"
        def rslt = sql.firstRow(sqlstr.toString())
        rslt.e
    }
    
    static dolog(sql,username) {
        
        def proc = "svn st".execute()
        proc.waitFor()
        def svnstrslt = proc.in.text

        def sqlstr = "insert into ${audittable}(name, time, svn_st) values ('${username}', now(), '${svnstrslt}')"
        sql.execute(sqlstr.toString())
    }
    
    static createtable(sql) {
        def sqlstr = "create table ${audittable} (name text, time timestamp, svn_st text)"
        sql.execute(sqlstr.toString())
    }
}
