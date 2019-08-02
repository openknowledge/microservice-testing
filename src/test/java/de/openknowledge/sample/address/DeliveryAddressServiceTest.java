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

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.ArquillianTest;
import org.jboss.arquillian.junit.ArquillianTestClass;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.beans11.BeansDescriptor;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.StateChangeAction;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import de.openknowledge.sample.address.application.AddressesApplication;
import de.openknowledge.sample.address.domain.Address;
import de.openknowledge.sample.address.infrastructure.ValidationExceptionHandler;
import de.openknowledge.sample.infrastructure.CdiMock;
import de.openknowledge.sample.infrastructure.H2Database;
import de.openknowledge.sample.infrastructure.H2TestData;

@RunAsClient
@RunWith(PactRunner.class)
@Provider("delivery-service")
@PactFolder("src/test/pacts")
public class DeliveryAddressServiceTest {

    @ClassRule
    public static H2Database database = new H2Database("delivery").withInitScript("src/main/resources/sql/create.sql");
    @ClassRule
    public static ArquillianTestClass arquillianTestClass = new ArquillianTestClass();
    @Rule
    public ArquillianTest arquillianTest = new ArquillianTest();
    @Rule
    public H2TestData testData = new H2TestData(database).withTestdataFile("src/test/sql/data.sql");

    @TestTarget
    public Target target = new HttpTarget("http", "localhost", Integer.valueOf(System.getProperty("test.http.port", "6002")), "/");

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        PomEquippedResolveStage pomFile = Maven.resolver().loadPomFromFile("pom.xml");
        return ShrinkWrap.create(WebArchive.class).addPackage(AddressesApplication.class.getPackage())
                .addPackage(Address.class.getPackage()).addPackage(ValidationExceptionHandler.class.getPackage())
                .addClasses(CdiMock.class, AddressValidationServiceMock.class)
                .addAsLibraries(pomFile.resolve("org.apache.commons:commons-lang3").withoutTransitivity().asFile())
                .addAsLibraries(pomFile.resolve("com.h2database:h2").withoutTransitivity().asFile())
                .addAsLibraries(pomFile.resolve("org.mockito:mockito-core").withTransitivity().asFile())
                .addAsResource(Address.class.getResource("/META-INF/persistence.xml"), "META-INF/persistence.xml")
                .addAsManifestResource(Address.class.getResource("/META-INF/microprofile-config.properties"), "microprofile-config.properties")
                .addAsManifestResource(
                        new StringAsset(
                                IOUtils.toString(DeliveryAddressServiceTest.class.getResource("/h2-ds.xml"),
                                StandardCharsets.UTF_8).replace("${database.url}", database.getDatabaseUrl())),
                        "wildfly-ds.xml")
                .addAsWebInfResource(new StringAsset(Descriptors.create(BeansDescriptor.class)
                        .createAlternatives().stereotype(CdiMock.class.getName()).up().exportAsString()), "beans.xml")
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/jboss-web.xml"), "jboss-web.xml");
    }

    @State("Three customers")
    public void setThreeCustomers() {
        testData.executeScript("src/test/sql/data.sql");
    }

    @State(value = "Three customers", action = StateChangeAction.TEARDOWN)
    public void cleanupThreeCustomers() {
        testData.after();
    }
}
