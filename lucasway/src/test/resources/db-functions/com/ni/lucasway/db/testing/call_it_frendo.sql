DROP FUNCTION IF EXISTS callitfrendo(TEXT);
CREATE OR REPLACE FUNCTION callitfrendo (
	param_1 TEXT
)
RETURNS
    TABLE (
    	id INTEGER,
    	col1 TEXT,
    	col2 TEXT
    )
AS
$$
BEGIN
	RETURN QUERY
	SELECT lucasway_test_table_1.id, lucasway_test_table_1.col1, lucasway_test_table_1.col2
	  FROM lucasway_test_table_1
	 WHERE lucasway_test_table_1.col2 = param_1;
END;
$$
LANGUAGE plpgsql;