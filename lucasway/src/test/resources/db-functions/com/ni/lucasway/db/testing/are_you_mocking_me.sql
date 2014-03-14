DROP FUNCTION IF EXISTS are_you_mocking_me(TEXT, TEXT, INTEGER, BOOLEAN);
CREATE OR REPLACE FUNCTION are_you_mocking_me (
	param_1 TEXT, param_2 TEXT, param_3 INTEGER, param_4 BOOLEAN
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
	SELECT buzzlightyear.id, buzzlightyear.col1, buzzlightyear.col2
	  FROM buzzlightyear
	  JOIN toyroom ON buzzlightyear.toyroom_id = toyroom.id;
END;
$$
LANGUAGE plpgsql;