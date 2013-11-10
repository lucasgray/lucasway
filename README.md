#lucasway

###Background

lucasway is a database migration framework that takes a structural approach to database migrations.  Traditional frameworks allow for a folder of migration files that are run sequentially, and are marked off never to be run again.  The aim of this project is to structure database resources like code and gain the benefits of extra flexibility, maintainability, and readability.

The name is a nod to flyway and timsort.

###Statement of purpose

Splitting the structure of migrations out by logical namespace instead of version timestamp has the following benefits:  

+   It is a more logical grouping of data constructs
+   It allows easy diffs in your version control system, and a straightforward way to track how your table has changed over time
+   It allows a structure that makes sense to your project’s database.

Functions, triggers, views, etc - everything not directly structured data in your database, should be treated as code.  Migrations should be treated as a deploy.  Deploying code to a server refreshes all methods, so deploying code to a database should refresh all functions.

Tables should be grouped logically, and all changes to a table should occur close to each other on the file system.

The drawback to this approach is that the resources may need structure.  In your project’s database, you may have a hard dependency for another resource to exist before the dependent resource can execute successfully.  Think of a data migration that requires a function to exist.  

All resources in lucasway allow dependencies via a directed acyclic graph. http://www.gradle.org/docs/current/userguide/go01.html#dag

##How it works

lucasway is designed as a gradle plugin.  Simply add the plugin to your project and apply the necessary configurations.

###Typical project layout

    /src
    ----/main
    --------/sql
    ------------/tables
    ----------------/users
    --------------------user.sql
    --------------------user_company.sql
    --------------------company.sql
    ----------------/functions
    --------------------/common
    ------------------------unix_timestamp_to_pg_timestamp.sql
    ------------------------increment.sql
    --------------------/user
    ------------------------load_user.sql
    ----------------/triggers
    --------------------sample_trigger.sql
    ----------------/views
    --------------------sample_view.sql
    ----------------/misc
    --------------------create_a_schema.sql
    --------------------do_a_barrel_roll.sql

lucasway treats all files under /src/main/sql as your migration resources.  Every file under /src/main/sql/tables is treated as versioned migrations and will be tracked like usual.  Files under /src/main/sql/* otherwise not in the tables folder are treated as code and will be applied to the target database every time lucaswayMigrate runs.

##How to add it to your project

####Add lucasway's maven repo and plugin artifact to your build script

    buildscript {
        repositories {
            maven { url "https://github.com/lucasgray/maven-repository/raw/master/" }
        }
        
        dependencies {
            classpath group: 'com.ni', name: 'lucasway', version: '0.1'
        }
    }
    
####Apply the plugin

    apply plugin: 'lucasway-migrations'
    
####Denote the sql dependency needed to run the migrations

    dependencies {
        sql group:"postgresql", name:"postgresql", version:"9.1-901-1.jdbc4"
    }
    
####Configure lucasway

    lucasway {
        url="jdbc:postgresql://localhost:5432/lucasway"
        driver="org.postgresql.Driver"
        username="user"
        password="pass"
        auditing=true
    }

##How to structure your files

###Functions, etc.

Functions are tracked uniquely via their file name.  A good practice is naming the file the exact same as the function.  The file MUST be idempotent for your database of choice, meaning, it should be able to run over and over independently.  For postgres, this means the first line should be DROP FUNCTION foo or CREATE OR REPLACE FUNCTION foo.

__increment.sql__

    CREATE OR REPLACE FUNCTION increment(i integer) RETURNS integer AS $$
        BEGIN
                RETURN i + 1;
        END;
      $$ LANGUAGE plpgsql;

###Tables, etc.

Tables are tracked via a block of meta information, then the sql to perform the necessary create or alter statements.  One table file can optimally track all changes to that table, in ascending or descending order (we prefer descending).

Table migrations can also be used to perform oneoff data migrations into those tables, as shown in the example.  Table migrations can pull in functions they depend on to be run first.


__user.sql__

    -----------------------------------
    --entity:user
    --dependsOn:[new_adhoc_user_load_function.sql]
    --V201311081234_MigrateOldUsersOver
    -----------------------------------
    SELECT adhoc_user_load_function();
    -----------------------------------
    --entity:user
    --V201311071234_AddAgeColumn
    -----------------------------------
    ALTER TABLE user ADD COLUMN age text;
    -----------------------------------
    --entity:user
    --V201310281234_CreateTable
    -----------------------------------
    CREATE TABLE user (
        author_id bigint,
        native_author_id text,
        feed_id bigint,
        interval_id bigint,
        interval_type_id bigint,
        gender_id bigint,
        post_count bigint
    );

##Running lucasway

    $ gradle lucaswayMigration

lucasway inspects all table and function files, then runs top level table migrations (and any functions they depend on first).  It marks each as run as it runs them.  If any fail, the migrations up to that point succeed and do not roll back.

Auditing can be turned on or off.  If on, it will record who ran migrations when, and a snapshot of svn st at that time (note that this is very environment specific - if you're not running on svn, you should probably turn auditing off)



