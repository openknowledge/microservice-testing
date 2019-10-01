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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.openknowledge.sample.address.application.AddressApplication;
import de.openknowledge.sample.address.domain.Address;

@Ignore("TODO: configure arquillian provider")
@RunAsClient
@RunWith(Arquillian.class)
public class BillingAddressServiceTest {

    @ArquillianResource
    private URI uri;

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        PomEquippedResolveStage pomFile = Maven.resolver().loadPomFromFile("pom.xml");
        return ShrinkWrap.create(WebArchive.class)
                .addPackage(AddressApplication.class.getPackage())
                .addPackage(Address.class.getPackage())
                .addAsLibrary(pomFile.resolve("org.apache.commons:commons-lang3").withTransitivity().asSingleFile());
    }
    
    @Test
    public void getAddress() {
        JsonObject result = Json.createReader(new StringReader(ClientBuilder
                .newClient()
                .target(uri)
                .path("billing-addresses/0816")
                .request(MediaType.APPLICATION_JSON)
                .get()
                .readEntity(String.class)))
                .readObject();
        assertThat(result).is(sameAs(getClass().getResourceAsStream("0816.json")));
    }

    @Test
    public void setAddress() throws IOException {
        // test
        Response response = ClientBuilder
                .newClient()
                .target(uri)
                .path("billing-addresses/0815")
                .request(MediaType.APPLICATION_JSON)
                .post(entity(getClass().getResourceAsStream("0815-new.json"), MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        
        // post condition
        JsonObject result = Json.createReader(new StringReader(ClientBuilder
                .newClient()
                .target(uri)
                .path("billing-addresses/0815")
                .request(MediaType.APPLICATION_JSON)
                .get()
                .readEntity(String.class)))
                .readObject();
        assertThat(result).is(sameAs(getClass().getResourceAsStream("0815-new.json")));
    }

    @Test
    public void setNewAddress() {
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
                .path("billing-addresses/007")
                .request(MediaType.APPLICATION_JSON)
                .post(entity(getClass().getResourceAsStream("007.json"), MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());

        // post condition
        JsonObject result = Json.createReader(new StringReader(ClientBuilder
                .newClient()
                .target(uri)
                .path("billing-addresses/007")
                .request(MediaType.APPLICATION_JSON)
                .get()
                .readEntity(String.class)))
                .readObject();
        assertThat(result).is(sameAs(getClass().getResourceAsStream("007.json")));
    }
}
