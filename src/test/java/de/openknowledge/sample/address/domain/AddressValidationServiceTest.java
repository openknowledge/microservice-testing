package de.openknowledge.sample.address.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import javax.validation.ValidationException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRuleMk2;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.RequestResponsePact;

public class AddressValidationServiceTest {

    @Rule
    public PactProviderRuleMk2 mockProvider = new PactProviderRuleMk2("address-validation-service", "localhost", Integer.valueOf(System.getProperty("test.http.port", "6000")), this);

    private AddressValidationService service;

    @Pact(consumer = "delivery-service")
    public RequestResponsePact validateMaxsAddress(PactDslWithProvider builder) throws IOException {
        return builder
          .given("Three customers")
            .uponReceiving("POST request for Max's address")
            .path("/valid-addresses")
            .method("POST")
            .matchHeader("Content-Type", "application/json.*", "application/json")
            .body(new PactDslJsonBody()
              .stringValue("recipient", "Max Mustermann")
              .stringValue("city", "26122 Oldenburg")
              .object("street")
                .stringValue("name", "Poststrasse")
                .stringValue("number", "1")
              .closeObject())
            .willRespondWith()
              .status(200)
            .toPact();
    }

    @Pact(consumer = "delivery-service")
    public RequestResponsePact validateSherlocksAddress(PactDslWithProvider builder) throws IOException {
        return builder
          .given("Three customers")
            .uponReceiving("POST request for Sherlock's address")
            .path("/valid-addresses")
            .method("POST")
            .matchHeader("Content-Type", "application/json.*", "application/json")
            .body(new PactDslJsonBody()
                    .stringValue("recipient", "Sherlock Holmes")
                    .stringValue("city", "London NW1 6XE")
                    .object("street")
                        .stringValue("name", "Baker Street")
                        .stringValue("number", "221B")
                    .closeObject())
          .willRespondWith()
            .status(400)
            .matchHeader("Content-Type", "application/problem\\+json.*", "application/problem+json")
            .body(new PactDslJsonBody().stringMatcher("detail", ".*", "Addresses from UK are not supported for delivery"))
          .toPact();
    }

    @Before
    public void initializeService() {
        service = new AddressValidationService();
        service.addressValidationServiceUrl = "http://localhost:" + System.getProperty("test.http.port", "6000");
    }

    @Test
    @PactVerification(fragment = "validateMaxsAddress")
    public void validAddress() {
        service.validate(new Address(
                            new Recipient("Max Mustermann"),
                            new Street(new StreetName("Poststrasse"), new HouseNumber("1")),
                            new City("26122 Oldenburg")));
    }

    @Test
    @PactVerification(fragment = "validateSherlocksAddress")
    public void invalidAddress() {
        assertThatThrownBy(() -> 
            service.validate(new Address(
                    new Recipient("Sherlock Holmes"),
                    new Street(new StreetName("Baker Street"), new HouseNumber("221B")),
                    new City("London NW1 6XE"))))
            .isInstanceOf(ValidationException.class)
            .hasMessage("Addresses from UK are not supported for delivery");
    }
}
