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

import static de.openknowledge.sample.address.JsonObjectComparision.sameAs;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.beans11.BeansDescriptor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.openknowledge.sample.address.application.AddressesApplication;
import de.openknowledge.sample.address.domain.Address;
import de.openknowledge.sample.address.infrastructure.ValidationExceptionHandler;
import de.openknowledge.sample.infrastructure.CdiMock;
import de.openknowledge.sample.infrastructure.H2Database;
import de.openknowledge.sample.infrastructure.H2TestData;

@Ignore("TODO: configure arquillian provider")
@RunAsClient
@RunWith(Arquillian.class)
public class DeliveryAddressServiceTest {

    @ClassRule
    public static H2Database database = new H2Database("delivery").withInitScript("src/main/resources/sql/create.sql");
    @Rule
    public H2TestData testdata = new H2TestData(database).withTestdataFile("src/test/sql/data.sql");

    @ArquillianResource
    private URI uri;

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        PomEquippedResolveStage pomFile = Maven.resolver().loadPomFromFile("pom.xml");
        return ShrinkWrap.create(WebArchive.class)
                .addPackage(AddressesApplication.class.getPackage())
                .addPackage(Address.class.getPackage())
                .addPackage(ValidationExceptionHandler.class.getPackage())
                .addClasses(CdiMock.class, AddressValidationServiceMock.class)
                .addAsLibraries(pomFile.resolve("org.apache.commons:commons-lang3").withoutTransitivity().asFile())
                .addAsLibraries(pomFile.resolve("com.h2database:h2").withoutTransitivity().asFile())
                .addAsLibraries(pomFile.resolve("org.mockito:mockito-core").withTransitivity().asFile())
                .addAsManifestResource(Address.class.getResource("/META-INF/microprofile-config.properties"), "microprofile-config.properties")
                .addAsResource(Address.class.getResource("/META-INF/persistence.xml"), "META-INF/persistence.xml")
                .addAsWebInfResource(new StringAsset(Descriptors.create(BeansDescriptor.class)
                        .createAlternatives().stereotype(CdiMock.class.getName()).up()
                        .exportAsString()), "beans.xml");
    }
    
    @Test
    public void getAddress() throws URISyntaxException, SQLException {
        database.executeScript("src/test/sql/data.sql");
        JsonObject result = Json.createReader(new StringReader(ClientBuilder
                .newClient()
                .target(uri)
                .path("delivery-addresses/0816")
                .request(MediaType.APPLICATION_JSON)
                .get()
                .readEntity(String.class)))
                .readObject();
        assertThat(result).is(sameAs(getClass().getResourceAsStream("0816.json")));
    }

    @Test
    public void setValidAddress() throws URISyntaxException, IOException {
        // test
        Response response = ClientBuilder
                .newClient()
                .target(uri)
                .path("delivery-addresses/0815")
                .request(MediaType.APPLICATION_JSON)
                .post(entity(getClass().getResourceAsStream("0815-new.json"), MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        
        // post condition
        JsonObject result = Json.createReader(new StringReader(ClientBuilder
                .newClient()
                .target(uri)
                .path("delivery-addresses/0815")
                .request(MediaType.APPLICATION_JSON)
                .get()
                .readEntity(String.class)))
                .readObject();
        assertThat(result).is(sameAs(getClass().getResourceAsStream("0815-new.json")));
    }

    @Test
    public void dontSetInvalidAddress() throws URISyntaxException {
        // test
        JsonObject problem = Json.createReader(new StringReader(ClientBuilder
                .newClient()
                .target(uri)
                .path("delivery-addresses/0816")
                .request(MediaType.APPLICATION_JSON)
                .post(entity(getClass().getResourceAsStream("0816-invalid.json"), MediaType.APPLICATION_JSON_TYPE))
                .readEntity(String.class)))
                .readObject();
        assertThat(problem).containsEntry("status", Json.createValue(400));
        assertThat(problem).containsEntry("detail", Json.createValue("City not found"));
        
        // post condition
        JsonObject result = Json.createReader(new StringReader(ClientBuilder
                .newClient()
                .target(uri)
                .path("delivery-addresses/0816")
                .request(MediaType.APPLICATION_JSON)
                .get()
                .readEntity(String.class)))
                .readObject();
        assertThat(result).is(sameAs(getClass().getResourceAsStream("0816.json")));
    }

    @Test
    public void setNewAddress() throws URISyntaxException {
        // pre condition
        Response notFoundResponse = ClientBuilder
                .newClient()
                .target(uri)
                .path("delivery-addresses/0817")
                .request(MediaType.APPLICATION_JSON)
                .get();
        assertThat(notFoundResponse.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());

        // test
        Response response = ClientBuilder
                .newClient()
                .target(uri)
                .path("delivery-addresses/0817")
                .request(MediaType.APPLICATION_JSON)
                .post(entity(getClass().getResourceAsStream("0817.json"), MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        // post condition
        JsonObject result = Json.createReader(new StringReader(ClientBuilder
                .newClient()
                .target(uri)
                .path("delivery-addresses/0817")
                .request(MediaType.APPLICATION_JSON)
                .get()
                .readEntity(String.class)))
                .readObject();
        assertThat(result).is(sameAs(getClass().getResourceAsStream("0817.json")));
    }

    @Test
    public void dontSetInvalidNewAddress() throws URISyntaxException {
        // pre condition
        Response notFoundResponse = ClientBuilder
                .newClient()
                .target(uri)
                .path("delivery-addresses/007")
                .request(MediaType.APPLICATION_JSON)
                .get();
        assertThat(notFoundResponse.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());

        // test
        Response response = ClientBuilder
                .newClient()
                .target(uri)
                .path("delivery-addresses/007")
                .request(MediaType.APPLICATION_JSON)
                .post(entity(getClass().getResourceAsStream("007-invalid.json"), MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        
        // post condition
        notFoundResponse = ClientBuilder
                .newClient()
                .target(uri)
                .path("delivery-addresses/007")
                .request(MediaType.APPLICATION_JSON)
                .get();
        assertThat(notFoundResponse.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }
}
