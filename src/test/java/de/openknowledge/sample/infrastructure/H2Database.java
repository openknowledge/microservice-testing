/*
 * Copyright open knowledge GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.openknowledge.sample.infrastructure;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.h2.tools.RunScript;
import org.h2.tools.Server;
import org.junit.rules.ExternalResource;

public class H2Database extends ExternalResource {

    private Server database;
    private String initScript;
    private String databaseName;
    private String user;
    private String password;

    public H2Database(String databaseName) {
        this(databaseName, "sa", "sa"); 
    }

    public H2Database(String databaseName, String user, String password) {
        this.databaseName = databaseName;
        this.user = user;
        this.password = password;
    }

    public H2Database withInitScript(String file) {
        initScript = file;
        return this;
    }

    public String getDatabaseUrl() {
        return "jdbc:h2:tcp://localhost:" + database.getPort() + "/~/" + databaseName;
    }

    public void executeScript(String file) throws SQLException {
        RunScript.execute(getDatabaseUrl(), user, password, file, null, true);
    }

    public boolean execute(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(getDatabaseUrl(), user, password);
                Statement statement = connection.createStatement()) {
            return statement.execute(sql);
        }
    }

    public List<Object[]> select(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(getDatabaseUrl(), user, password);
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
            List<Object[]> rows = new ArrayList<>();
            ResultSet result = statement.getResultSet();
            int columnCount = result.getMetaData().getColumnCount();
            while (result.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    row[i] = result.getObject(i + 1);
                }
                rows.add(row);
            }
            return rows;
        }
    }

    protected void before() throws Throwable {
        database = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-ifNotExists");
        database.start();
        if (initScript != null) {
            executeScript(initScript);
        }
    }

    protected void after() {
        try {
            execute("DROP ALL OBJECTS");
        } catch (SQLException e) {
            // ignore
        }
        database.stop();
    }
}
