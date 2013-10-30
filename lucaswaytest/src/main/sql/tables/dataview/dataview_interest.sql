-----------------------------------
--entity:dataview_interest
--V201309140004_EditTable
-----------------------------------
ALTER TABLE dataview_interest ADD COLUMN foo text;
-----------------------------------
--entity:dataview_interest
--V201309140003_CreateTable
-----------------------------------
CREATE TABLE dataview_interest (
    dataview_id bigint,
    author_time_id bigint,
    feed_id bigint,
    interest_id bigint,
    interest_group_id bigint,
    sentiment_positive bigint,
    sentiment_negative bigint,
    sentiment_neutral bigint,
    imported_timestamp timestamp without time zone,
    post_count bigint
);