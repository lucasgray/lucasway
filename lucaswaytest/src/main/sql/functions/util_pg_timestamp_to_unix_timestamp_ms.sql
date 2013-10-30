CREATE OR REPLACE FUNCTION util_pg_timestamp_to_unix_timestamp_ms(p_pg_timestamp timestamp with time zone)
 RETURNS bigint
 LANGUAGE plpgsql
 IMMUTABLE STRICT
AS $function$
declare
begin

    return round(date_part('epoch', current_timestamp)) * 1000;

end;
$function$
;
