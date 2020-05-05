# Overview
This project is a simple wrapper for [com.fasterxml.jackson](https://github.com/FasterXML/jackson) JSON parser.
It enables fluent building, reading, and mapping of JSON objects and arrays.

# Status
[![Build Status](https://travis-ci.com/yupzip/yupzip-json.svg?branch=master)](https://travis-ci.com/yupzip/yupzip-json)
[![Coverage Status](https://coveralls.io/repos/github/yupzip/yupzip-json/badge.svg?branch=master)](https://coveralls.io/github/yupzip/yupzip-json?branch=master)

# Getting started
### Maven
```xml
<dependency>
    <groupId>com.yupzip.json</groupId>
    <artifactId>yupzip-json</artifactId>
    <version>1.4.0</version>
</dependency>
```
### Gradle
```groovy
implementation group: 'com.yupzip.json', name: 'yupzip-json', version: '1.4.0'
```
# Prerequisites
This library requires JDK 1.8+

# Usage
## 1. Building
Fluent JSON object creation:
```java
Json person = Json.create()
                .put("id", 1)
                .put("name", "John Citizen")
                .add("gender", personEntity.getGender()) //adds property only if value is not null
                .put("weight", 90.1)
                .put("verified", true)
                .put("contactNumbers", List.of("0400000000", "0400000001"))
                .put("address", Json.create()
                        .put("addressLine", "100 George Street")
                        .put("postCode", "2000")
                        .put("state", "NSW")
                        .put("country", "Australia"))
                .put("dob", "1990-01-01");
```
## 2. Reading
```java
Json person = Json.create();

int id = person.integer("id");
String name = person.string("name");
String gender = person.stringOr("gender", "unknown"); //returns property value or default value if null
Double weight = person.decimal("weight");
List<String> contactNumbers = person.strings("contactNumbers");
Json address = person.object("address");
Date dob = person.date("dob", "yyyy-MM-dd");

Company company = loadCompany();
List<String> employeeNames = Json.parse(company)
                      .stream("employees")
                      .map(employee -> employee.string("fullName"))
                      .collect(Collectors.toList());
```
## 3. Mapping
Fluent mapping of JSON properties:
```java
Json response = Json.create(); //response payload
Person person = new Person();
Address address = new Address();

response.map("name", person::setName) // generic mapping of property value (type is defined by consumer)
        .integer("id", person::setId) // or mapping explicit types
        .decimal("weight", person::setWeight)
        .bool("verified", person::setVerified)
        .strings("contactNumbers", person::setContactNumbers)
        .integers("numbers", person::setNumbers)
        .decimals("scores", person::setScores)
        .object("address", addressJson -> addressJson // or mapping child json object
                .map("addressLine", address::setAddressLine)
                .map("postCode", address::setPostCode)
                .map("state", address::setState)
                .map("country", address::setCountry));
```
## 4. Parsing/Converting
Parsing JSON string:
```java
String personString = """
            {
               "id": 1,
               "name": "John Citizen"
            }
            """;
Json person = Json.parse(personString);
```
Converting from/to Java POJOs:
```java
Person person = new Person(1, "John Citizen");
Json json = Json.parse(person);
```
```java
Person person = Json.create()
                  .put("id", 1)
                  .put("name", "John Citizen")
                  .convertTo(Person.class);
```
## 5. Spring REST
As RestController request body:
```java
@PutMapping("/v1/customers")
public void createCustomer(@RequestBody Json request) {
    customerService.createCustomer(request);
}
```
As RestTemplate HttpEntity:
```java
public List<Product> getProducts(String url) {
    ResponseEntity<Json> responseEntity = restTemplate.getForEntity(url, Json.class);
    Json response = Objects.requireNonNull(responseEntity.getBody());
    return response.stream("data")
                .map(item -> Product.of(item.integer("id")
                                .withName(item.string("name")
                                .withPrice(item.decimal("price")))))
                .collect(Collectors.toList());
}
```

# Configuration
### Jackson serialization/deserialization
Configuration via application.properties:
```properties
jackson.deserialization.fail-on-unknown-properties=false
jackson.serialization.fail-on-empty-beans=false
jackson.default-property-inclusion=ALWAYS
jackson.visibility.field=ANY
jackson.visibility.getter=NONE
jackson.visibility.is-getter=NONE
jackson.visibility.setter=NONE
jackson.disabled-features=WRITE_DATES_AS_TIMESTAMPS,FAIL_ON_EMPTY_BEANS
```
### yupzip.Json collection type
JSON properties (keys/values) are stored in a java.util.Map `Map<String, Object> properties`.
This map is a HashMap by default, however this can be changed to LinkedHashMap if required via property:
```properties
yupzip.json.map-type=LINKED_HASH_MAP
```

# Contributing [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/dwyl/esta/issues)

# License
This project is licensed under Apache License Version 2.0 - [LICENSE.md](LICENSE)
