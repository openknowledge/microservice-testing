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

import org.junit.jupiter.api.extension.*;

import java.security.cert.Extension;
import java.sql.SQLException;


public class H2TestData implements BeforeAllCallback, AfterAllCallback {

    private H2Database database;
    private String testDataFile;

    public H2TestData(H2Database database) {
        this.database = database;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        if (testDataFile != null) {
            executeScript(testDataFile);
        }
    }


    public H2TestData withTestdataFile(String testDataFile) {
        this.testDataFile = testDataFile;
        return this;
    }

    public void executeScript(String file) {
        try {
            database.executeScript(file);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean execute(String sql) {
        try {
            return database.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void after() {
        try {
            database.execute("SET REFERENTIAL_INTEGRITY FALSE");
            database.select("SHOW TABLES").stream().map(tables -> tables[0].toString()).forEach(table -> execute("TRUNCATE TABLE " + table));
            database.execute("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {

    }
}
