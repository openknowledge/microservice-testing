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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.Specializes;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.johnzon.jaxrs.jsonb.jaxrs.JsonbJaxrsProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.PostgreSQLContainer;

import de.openknowledge.sample.address.application.AddressesApplication;
import de.openknowledge.sample.address.domain.Address;
import de.openknowledge.sample.address.domain.AddressValidationService;

@RunAsClient
@RunWith(Arquillian.class)
public class DeliveryAddressServiceTest {

    private static final Logger LOG = Logger.getLogger(DeliveryAddressServiceTest.class.getName());
    @ClassRule
    public static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:9.6.24");
    
    @Deployment
    public static WebArchive createDeployment() {
        PomEquippedResolveStage pomFile = Maven.resolver().loadPomFromFile("pom.xml");

        System.setProperty("javax.persistence.jdbc.url", postgresql.getJdbcUrl());
        System.setProperty("javax.persistence.jdbc.user", postgresql.getUsername());
        System.setProperty("javax.persistence.jdbc.password", postgresql.getPassword());
        System.setProperty("javax.persistence.schema-generation.database.action", "drop-and-create");
        WebArchive archive = ShrinkWrap.create(WebArchive.class)
                .addAsLibraries(pomFile.resolve("org.apache.commons:commons-lang3").withTransitivity().asFile())
                .addAsLibraries(pomFile.resolve("org.microjpa:microjpa").withTransitivity().asFile())
                .addAsLibraries(pomFile.resolve("org.postgresql:postgresql").withTransitivity().asFile())
                .addPackage(AddressesApplication.class.getPackage())
                .addClass(AddressValidationServiceMock.class)
//                .addPackage(Address.class.getPackage())
//                .addPackage(ValidationExceptionHandler.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

        LOG.log(Level.FINE, () -> archive.toString(true));
        return archive;
    }

    @ArquillianResource
    private URL baseURI;
    private WebTarget addressTarget;

    @Before
    public void initializeClient() {
        addressTarget = ClientBuilder.newClient()
                .register(JsonbJaxrsProvider.class)
                .target(baseURI.toString())
                .path("delivery-addresses");
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

