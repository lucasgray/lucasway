package com.ni.lucasway.db.testing.dbunit

import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Timestamp
import java.sql.Types

import org.dbunit.dataset.datatype.AbstractDataType
import org.dbunit.dataset.datatype.DataType
import org.dbunit.dataset.datatype.DataTypeException
import org.dbunit.dataset.datatype.TypeCastException

import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory

public class PostgresqlCustomTimestampDataTypeFactory extends PostgresqlDataTypeFactory
{
	@Override
	public DataType createDataType(int sqlType, String sqlTypeName)
		throws DataTypeException
	{
		if (sqlTypeName.toLowerCase().equals("timestamp")) {
			return new CustomTimestampDataType()
		}
		else {
			return super.createDataType(sqlType, sqlTypeName)
		}
	}
	
	protected static class CustomTimestampDataType extends AbstractDataType
	{
		def stdType = DataType.forSqlType(Types.TIMESTAMP)

		public CustomTimestampDataType() {
			super("TIMESTAMP", Types.TIMESTAMP, Timestamp.class, false)
		}

	    @Override
	    public Object typeCast(Object value) throws TypeCastException {
	    	if (value instanceof String && value.equals("infinity")) {
	    		return new Timestamp()
	    	}
	    	else {
	    		stdType.typeCast(value)
	    	}
	    }

		@Override
		public void setSqlValue(
			Object value, int column, PreparedStatement statement)
            	throws SQLException, TypeCastException
        {
        	if (value instanceof String && value.equals("infinity")) {
        		statement.setObject(column, 'infinity', Types.TIMESTAMP, -1)
        	}
        	else {
        		stdType.setSqlValue(value, column, statement)
        	}
        }
	}
}