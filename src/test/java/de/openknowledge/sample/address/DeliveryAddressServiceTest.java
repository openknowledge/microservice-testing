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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

import javax.inject.Inject;
import javax.validation.ValidationException;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MeecrowaveConfig;
import org.apache.meecrowave.testing.ConfigurationInject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.StateChangeAction;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import de.openknowledge.sample.address.domain.AddressValidationService;
import de.openknowledge.sample.infrastructure.H2Database;
import de.openknowledge.sample.infrastructure.H2TestData;
import rocks.limburg.cdimock.MockitoBeans;

@MockitoBeans(types = {AddressValidationService.class})
@Provider("delivery-service")
@PactFolder("src/test/pacts")
@MeecrowaveConfig
public class DeliveryAddressServiceTest {

    @ConfigurationInject
    private Meecrowave.Builder config;

    @RegisterExtension
    public static H2Database database = new H2Database("delivery").withInitScript("src/main/resources/sql/create.sql");

    @RegisterExtension
    public H2TestData testData = new H2TestData(database).withTestdataFile("src/test/sql/data.sql");

    @Inject
    private AddressValidationService addressValidationService;

    @BeforeEach
    public void setUp(PactVerificationContext context) {
        doThrow(new ValidationException("City not found")).when(addressValidationService).validate(argThat(a -> a.getCity().toString().contains("London")));
        context.setTarget(new HttpTestTarget("localhost", config.getHttpPort(), "/"));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
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

