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

package de.openknowledge.sample.address;

import static javax.ws.rs.client.ClientBuilder.newClient;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;

import javax.enterprise.inject.Specializes;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MeecrowaveConfig;
import org.apache.meecrowave.testing.ConfigurationInject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.openknowledge.sample.address.domain.Address;
import de.openknowledge.sample.address.domain.AddressValidationService;
import rocks.limburg.cdimock.MockitoBeans;

@Testcontainers
@MockitoBeans(types = {AddressValidationService.class})
@MeecrowaveConfig
public class DeliveryAddressServiceTest {

    @Container
    public static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:9.6.24");

    @ConfigurationInject
    private Meecrowave.Builder config;
    private WebTarget addressTarget;
    
    @BeforeAll
    public static void setJdbcUrl() {
        System.setProperty("javax.persistence.jdbc.url", postgresql.getJdbcUrl());
        System.setProperty("javax.persistence.jdbc.user", postgresql.getUsername());
        System.setProperty("javax.persistence.jdbc.password", postgresql.getPassword());
        System.setProperty("javax.persistence.schema-generation.database.action", "drop-and-create");
    }

    @BeforeEach
    public void setUp() {
        addressTarget = newClient().target("http://localhost:" + config.getHttpPort() + "/delivery-addresses");
    }

    @Test
    public void createAddress() {
        Response response = addressTarget
                .path("0815")
                .request()
                .post(Entity.entity(getResource("0815.json"), MediaType.APPLICATION_JSON));
        assertThat(response.getStatus()).isEqualTo(200);

        Map<?, ?> expected = JsonbBuilder.create().fromJson(getResource("0815.json"), Map.class);
        assertThat(addressTarget.path("0815").request().get(Map.class)).containsAllEntriesOf(expected);
    }

    private InputStream getResource(String name) {
        return DeliveryAddressServiceTest.class.getResourceAsStream(name);
    }

    @Specializes
    public static class AddressValidationServiceMock extends AddressValidationService {
        public void validate(Address address) {
            // do nothing
        }
    }
}

