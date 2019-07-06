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
public class AddressValidationServiceTest {

    @ArquillianResource
    private URI uri;

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        PomEquippedResolveStage pomFile = Maven.resolver().loadPomFromFile("pom.xml");
        return ShrinkWrap.create(WebArchive.class)
                .addPackage(AddressApplication.class.getPackage())
                .addPackage(Address.class.getPackage())
                .addAsLibrary(pomFile.resolve("org.apache.commons:commons-lang3").withTransitivity().asSingleFile())
                .addAsResource(AddressValidationServiceTest.class.getResource("/project-test.yml"), "project-defaults.yml")
                .addAsResource(Thread.currentThread().getContextClassLoader().getResource("plz.txt"), "plz.txt");
    }

    @Test
    public void validAddress() {
        Response response = ClientBuilder
                .newClient()
                .target(uri)
                .path("valid-addresses")
                .request(MediaType.APPLICATION_JSON)
                .post(entity(getClass().getResourceAsStream("valid-address.json"), MediaType.APPLICATION_JSON_TYPE));
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    }

    @Test
    public void missingAddress() throws IOException {
        JsonObject problem = Json.createReader(new StringReader(ClientBuilder
                .newClient()
                .target(uri)
                .path("valid-addresses")
                .request(MediaType.APPLICATION_JSON)
                .post(entity(getClass().getResourceAsStream("missing-address.json"), MediaType.APPLICATION_JSON_TYPE))
                .readEntity(String.class)))
                .readObject();
        assertThat(problem).containsEntry("status", Json.createValue(400));
        assertThat(problem).containsEntry("detail", Json.createValue("no matching city found"));
    }

    @Test
    public void misspelledAddress() {
        JsonObject problem = Json.createReader(new StringReader(ClientBuilder
                .newClient()
                .target(uri)
                .path("valid-addresses")
                .request(MediaType.APPLICATION_JSON)
                .post(entity(getClass().getResourceAsStream("misspelled-address.json"), MediaType.APPLICATION_JSON_TYPE))
                .readEntity(String.class)))
                .readObject();
        assertThat(problem).containsEntry("status", Json.createValue(400));
        assertThat(problem).containsEntry("detail", Json.createValue("Did you mean 26121 Oldenburg (Oldenburg)?"));
    }

    @Test
    public void ambiguousAddress() {
        JsonObject problem = Json.createReader(new StringReader(ClientBuilder
                .newClient()
                .target(uri)
                .path("valid-addresses")
                .request(MediaType.APPLICATION_JSON)
                .post(entity(getClass().getResourceAsStream("ambiguous-address.json"), MediaType.APPLICATION_JSON_TYPE))
                .readEntity(String.class)))
                .readObject();
        assertThat(problem).containsEntry("status", Json.createValue(400));
        assertThat(problem).hasEntrySatisfying("detail", v -> v.toString().startsWith("Did you mean one of"));
        assertThat(problem).hasEntrySatisfying("detail", v -> v.toString().contains("26909 Neubörger"));
        assertThat(problem).hasEntrySatisfying("detail", v -> v.toString().startsWith("26909 Neulehe"));
    }
}
