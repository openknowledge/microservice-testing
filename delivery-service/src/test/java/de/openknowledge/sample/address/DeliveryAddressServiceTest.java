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
import static org.transactionunit.TransactionUnitProvider.PERSISTENCE_PROVIDER_PROPERTY;
import static space.testflight.DatabaseInstanceScope.PER_TEST_SUITE;

import java.io.InputStream;
import java.util.Map;

import javax.enterprise.inject.Specializes;
import javax.json.bind.JsonbBuilder;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceProperty;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.apache.meecrowave.testing.ConfigurationInject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.transactionunit.RollbackAfterTest;

import de.openknowledge.sample.address.domain.Address;
import de.openknowledge.sample.address.domain.AddressValidationService;
import rocks.limburg.cdimock.MockitoBeans;
import space.testflight.ConfigProperty;
import space.testflight.Flyway;

@Flyway(databaseInstance = PER_TEST_SUITE, configuration = {
        @ConfigProperty(key = "space.testflight.jdbc.url.property", value = "javax.persistence.jdbc.url"),
        @ConfigProperty(key = "space.testflight.jdbc.username.property", value = "javax.persistence.jdbc.user"),
        @ConfigProperty(key = "space.testflight.jdbc.password.property", value = "javax.persistence.jdbc.password"), })
@MockitoBeans(types = { AddressValidationService.class })
@MonoMeecrowaveConfig
@RollbackAfterTest
@PersistenceContext(unitName = "delivery-service", properties = {
        @PersistenceProperty(name = "javax.persistence.provider", value = "org.transactionunit.TransactionUnitProvider"),
        @PersistenceProperty(name = PERSISTENCE_PROVIDER_PROPERTY, value = "org.hibernate.jpa.HibernatePersistenceProvider"), })
public class DeliveryAddressServiceTest {

    @ConfigurationInject
    private Meecrowave.Builder config;
    private WebTarget addressTarget;

    @BeforeEach
    public void setUp() {
        addressTarget = newClient().target("http://localhost:" + config.getHttpPort() + "/delivery-addresses");
    }

    @Test
    public void createAddress() {
        Response response = addressTarget.path("0815").request()
                .post(Entity.entity(getResource("0815.json"), MediaType.APPLICATION_JSON));
        assertThat(response.getStatus()).isEqualTo(200);

        Map<?, ?> expected = JsonbBuilder.create().fromJson(getResource("0815.json"), Map.class);
        assertThat(addressTarget.path("0815").request().get(Map.class)).containsAllEntriesOf(expected);
    }

    private InputStream getResource(String name) {
        return DeliveryAddressServiceTest.class.getResourceAsStream(name);
    }
}
