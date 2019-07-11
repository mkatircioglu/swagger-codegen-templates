# Swagger Codegen Templates [![Build Status](https://travis-ci.com/mkatircioglu/swagger-codegen-templates.svg?token=8GAU4h1fx7G5QDqGjD51&branch=master)](https://travis-ci.com/mkatircioglu/swagger-codegen-templates)
This project provides tooling for Maven projects to generate code from swagger-definitions (yaml or json). Code will be generated at every build to make sure your implementation matches your api.

It only provides for Spring Boot & Spring Cloud Template for code-generation currently.

## Installation

Installation with maven is easy. Just add the following snippet into your pom.xml:

*Example 1: Plugin configuration in pom.xml*

```xml
            <plugin>
                <groupId>io.swagger</groupId>
                <artifactId>swagger-codegen-maven-plugin</artifactId>
                <version>2.3.1</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                        <configuration>
                            <!-- specify the swagger yaml -->
                            <inputSpec>${project.basedir}/swagger.yml</inputSpec>
                            <language>springInterfaces</language>
                            <generateModels>false</generateModels>
                            <!-- pass any necessary config options -->
                            <configOptions>
                            	<apiPackage>com.yourcompany.api</apiPackage>
                              <modelPackage>com.yourcompany.model</modelPackage>
                              <import-mappings>${swagger-import-mappings}</import-mappings>
                            </configOptions>
                        </configuration>
                    </execution>
                </executions>
                <dependencies>
                    <dependency>
                        <groupId>io.github.mkatircioglu</groupId>
                        <artifactId>swagger-codegen-templates</artifactId>
                        <version>${template.version}</version>
                    </dependency>
                </dependencies>
            </plugin>
```

## Templates
*Table 1: Overview of  templates*

| Template             | Status  |
| -------------------- | ------- |
| springInterfaces     | Done    |
| typescriptInterfaces | Ongoing |

The following examples are generated from the official pet-store-example by swagger.

### springInterfaces Template

When using springInterfaces as template then the generated code looks like this:

*Example 2: Generated code*

```java
package io.github.mkatircioglu.controller;

import io.github.mkatircioglu.model.ModelApiResponse;
import io.github.mkatircioglu.model.Pet;
import org.springframework.core.io.Resource;
import java.util.stream.Collectors;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import org.springframework.validation.annotation.Validated;

import java.util.*;

import javax.validation.constraints.*;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;

@Validated
@RestController
public class PetApiController {

    private final PetApiDelegate delegate;

    @Autowired
    public PetApiController(PetApiDelegate delegate) {
        this.delegate = delegate;
    }

    
    @RequestMapping(value = "/pet", method = RequestMethod.POST)
    public ResponseEntity<Void> addPet( @Valid @RequestBody Pet body) {
        return delegate.addPet(body);
    }

    
    @RequestMapping(value = "/pet/{petId}", method = RequestMethod.DELETE)
    public void deletePet(@PathVariable Long petId) {
        delegate.deletePet(petId, apiKey);
    }

    
    @RequestMapping(value = "/pet/findByStatus", method = RequestMethod.GET)
    public List<Pet> findPetsByStatus( @NotNull @RequestParam(value = "status", required = true) List<String> status) {
        return delegate.findPetsByStatus(status);
    }

    
    @RequestMapping(value = "/pet/findByTags", method = RequestMethod.GET)
    public List<Pet> findPetsByTags( @NotNull
   @RequestParam(value = "tags", required = true) List<String> tags) {
        return delegate.findPetsByTags(tags);
    }

    
    @RequestMapping(value = "/pet/{petId}", method = RequestMethod.GET)
    public Pet getPetById(@PathVariable Long petId) {
        return delegate.getPetById(petId);
    }

    
    @RequestMapping(value = "/pet", method = RequestMethod.PUT)
    public void updatePet( @Valid @RequestBody Pet body) {
        delegate.updatePet(body);
    }

    
    @RequestMapping(value = "/pet/{petId}", method = RequestMethod.POST)
    public void updatePetWithForm(@PathVariable Long petId) {
        delegate.updatePetWithForm(petId, name, status);
    }

    
    @RequestMapping(value = "/pet/{petId}/uploadImage", method = RequestMethod.POST)
    public ModelApiResponse uploadFile(@PathVariable Long petId) {
        return delegate.uploadFile(petId, additionalMetadata, file);
    }
}
```

## Configuration Options
There are some more configuration options for the maven-plugin.

*Table 2: Overview of configuration options*

| Configuration Option | Description                            |
| -------------------- | -------------------------------------- |
| inputSpec` | OpenAPI Spec file path |
| language | target generation language |
| generateModels       | generate the models (`true` by default) |
| generateApis         | generate the apis (`true` by default) |
| useDelegatePattern   | enable delegation services for maps Data Transfer Objects to Entities |
| useBeanValidation    | enable javax validation on requests objects/classes |
| apiPackage           | the package to use for generated api objects/classes |
| modelPackage         | the package to use for generated model objects/classes |
| feignName           | target feign client name ( `generates Feign clients if exist`) |
| feignUrl            | target feign client url  ( `generates Feign clients if exist`) |
| configOptions | a map of language-specific parameters |
| type-mappings        | a map of project specific types |
| import-mappings      | a list of project specific imports |
| dateLibrary          | change the default dateLibrary (`legacy` by default) |


*Table 3: Vendor Extensions*

| Parameter Name     | Where to use            | Description                                                  | Example                                                      |
| ------------------ | ----------------------- | :----------------------------------------------------------- | ------------------------------------------------------------ |
| x-ignore-param     | Parameter               | Will not include the parameter in the generated Java classes | x-ignore-param:true                                          |
| x-encrypted-id     | Parameter               | Generates the parameter as EntityId encrypted data object rather than String | x-encrypted-id:true                                          |
| x-validation-class | Parameter               | Adds validation to parameter or definition  that value must be a valid string according to what class is provided. Such validation annotations can be found in friends-commons. Please also make sure to add the import-mapping to the contract project where you intent to use such functionality. | x-validation-class:ValidCurrencyCode                         |
| x-change-reference | Definition or Parameter | There are scenarios where we want to use other objects instead of just primitive ones that swagger provides | x-change-reference:Country                                   |
| x-method-security  | Operation               | Add Spring Method Security on operation. Values are annotation and expressison. | x-method-security:<br/>&nbsp;&nbsp;&nbsp;&nbsp;annotation:"Secured"<br/>&nbsp;&nbsp;&nbsp;&nbsp;expression:{\"ROLE_VIEWER\",\"ROLE_EDITOR\"\} |
| x-payload-class    | Operation               | Will override the default return type List with the one provided. Generally the choice is between List, Set, Page. | x-payload-class: Page                                        |

*Table 4: Additional Notes*

| Note                               | Description                                                  |
| ---------------------------------- | ------------------------------------------------------------ |
| Swagger generation import mappings | Please make sure that `${swagger-import-mappings}` is added to each swagger plugin under each project sub-module. Failing to do this will result in for example EntityId being imported as <br/><br/>`import com.company.dto.EntityId;`<br/><br/><br/>&nbsp;instead of <br/>```import com.company.commons.model.EntityId;``` |

## License

This software is licensed under the terms in the file named “LICENSE” in the root directory of this project.

## Author

Mete Alpaslan KATIRCIOGLU <mete[at]katircioglu.net>
