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

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;

import java.net.URI;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.ArquillianTest;
import org.jboss.arquillian.junit.ArquillianTestClass;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;

import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.State;
import au.com.dius.pact.provider.junit.loader.PactFolder;
import au.com.dius.pact.provider.junit.target.HttpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import de.openknowledge.sample.address.application.AddressApplication;
import de.openknowledge.sample.address.domain.Address;

@RunAsClient
@RunWith(PactRunner.class)
@Provider("billing-service")
@PactFolder("src/test/pacts")
public class BillingAddressServiceTest {

    @ClassRule
    public static ArquillianTestClass arquillianTestClass = new ArquillianTestClass();
    @Rule
    public ArquillianTest arquillianTest = new ArquillianTest();
    @TestTarget
    public final Target target = new HttpTarget("http", "localhost", valueOf(getProperty("test.http.port", "6001")), "/billing");
    @ArquillianResource
    private URI uri;

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        PomEquippedResolveStage pomFile = Maven.resolver().loadPomFromFile("pom.xml");
        return ShrinkWrap.create(WebArchive.class, "billing.war")
                .addPackage(AddressApplication.class.getPackage())
                .addPackage(Address.class.getPackage())
                .addAsLibrary(pomFile.resolve("org.apache.commons:commons-lang3").withTransitivity().asSingleFile());
    }

    @State("Three customers")
    public void setThreeCustomers() {
        // nothing to do
    }
}
