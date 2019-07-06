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
import static org.mockito.Mockito.mock;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.validation.ValidationException;

import de.openknowledge.sample.address.domain.AddressValidationService;
import de.openknowledge.sample.infrastructure.CdiMock;

@ApplicationScoped
public class AddressValidationServiceMock {

    @Produces
    @CdiMock
    @ApplicationScoped
    public AddressValidationService createValidationServiceMock() {
        AddressValidationService service = mock(AddressValidationService.class);
        doThrow(new ValidationException("City not found")).when(service).validate(argThat(a -> a.getCity().toString().contains("London")));
        return service;
    }
}
