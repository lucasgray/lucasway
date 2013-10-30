-----------------------------------
--entity:dataview
--V201309150000_AddDisplayName
-----------------------------------
ALTER TABLE dataview ADD COLUMN display_name text;
-----------------------------------
--entity:dataview
--V201309140000_CreateTable
--dependsOn:[c.sql]
-----------------------------------
CREATE TABLE dataview (
    id bigint, 
    version_id bigint, 
    version_from_time timestamp without time zone, 
    version_to_time timestamp without time zone, 
    native_dataview_id bigint, 
    native_update_timestamp timestamp without time zone, 
    name text
);