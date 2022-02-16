/*
 * Copyright 2019 open knowledge GmbH
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
package de.openknowledge.sample.address.domain;


import static org.apache.commons.lang3.Validate.notNull;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTypeAdapter;

public class Address {
    private Recipient recipient;
    private Street street;
    private City city;

    @JsonbCreator
    public Address(@JsonbProperty("recipient") Recipient recipient) {
        this.recipient = notNull(recipient, "recipient may not be null");
    }

    public Address(Recipient recipient, Street street, City city) {
        this(recipient);
        setStreet(street);
        setCity(city);
    }

    @JsonbTypeAdapter(Recipient.Adapter.class)
    public Recipient getRecipient() {
        return recipient;
    }

    public Street getStreet() {
        return street;
    }

    public void setStreet(Street street) {
        this.street = street;
    }

    @JsonbTypeAdapter(City.Adapter.class)
    public City getCity() {
        return city;
    }

    public void setCity(City city) {
        this.city = city;
    }
}
