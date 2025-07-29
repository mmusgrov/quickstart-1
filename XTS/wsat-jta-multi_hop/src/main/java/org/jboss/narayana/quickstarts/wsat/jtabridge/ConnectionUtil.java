package org.jboss.narayana.quickstarts.wsat.jtabridge;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ConnectionUtil {
    private static final String JNDI_NAME = "java:jboss/datasources/H2XADS1";

    public static DataSource lookupDataSource(String jndiName) throws NamingException {
        Context ctx = new InitialContext();
        return (DataSource) ctx.lookup(jndiName);
    }

    public static DataSource lookupDataSource() throws NamingException {
        return lookupDataSource(JNDI_NAME);
    }
}
