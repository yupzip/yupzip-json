# Overview
A thin, fluent wrapper over [Jackson](https://github.com/FasterXML/jackson) for building, reading, and mapping JSON with the minimum amount of code.
## Why yupzip-json?
- **No annotations on your POJOs** — `Json` is a `Map`-backed value, not a code-generated class.
- **Fluent everywhere** — build, read, and map JSON in single expressions.
- **Caller-driven typing** — pick the accessor that matches the value (`string()`, `integer()`, `object()`, `array()`). The library doesn't second-guess you.
- **Spring-friendly** — works as `@RequestBody Json` and `ResponseEntity<Json>` out of the box.
# Status
![Build](https://github.com/yupzip/yupzip-json/actions/workflows/build.yml/badge.svg)
[![Coverage Status](https://coveralls.io/repos/github/yupzip/yupzip-json/badge.svg?branch=master)](https://coveralls.io/github/yupzip/yupzip-json?branch=master)
# Prerequisites
This library requires JDK 21+  
(yupzip-json with JDK 17 support is 2.4.0)  
(yupzip-json with JDK 1.8 support is 1.8.5)

# Getting started
### Maven
```xml
<dependency>
    <groupId>com.yupzip.json</groupId>
    <artifactId>yupzip-json</artifactId>
    <version>3.1.0</version>
</dependency>
```
### Gradle
```groovy
implementation group: 'com.yupzip.json', name: 'yupzip-json', version: '3.1.0'
```
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
Long orderId = order.longInt("orderId"); // longInt chosen as getter short name as 'long' being a keyword
BigDecimal price = order.bigDecimal("price");
List<BigDecimal> amounts = order.bigDecimals("amounts");
List<String> contactNumbers = person.strings("contactNumbers");
Json address = person.object("address");
Date dob = person.date("dob", "yyyy-MM-dd");
LocalDate today = person.localDate("today", "yyyy-MM-dd");

// default values and exception behavior
int qty = order.integerOr("qty", 1);                  // default if missing
String id = order.stringOrThrow("id");                // PropertyRequiredException if missing
String id = order.stringOrThrow("id", new MyApiError()); // custom exception

Company company = loadCompany();
List<String> employeeNames = Json.parse(company)
                      .stream("employees")
                      .map(employee -> employee.string("fullName"))
                      .collect(Collectors.toList());
```
Three variants per type — choose by what you want on absent/null values:

        | Variant | Behavior |
        |---|---|
        | `string(key)` | returns value, or `null` if missing |
        | `stringOr(key, default)` | returns default if missing |
        | `stringOrThrow(key)` | throws `PropertyRequiredException` if missing |
        | `stringOrThrow(key, ex)` | throws your exception if missing |

The same family exists for `integer`, `longInt`, `decimal`, `bigDecimal`, `bool`, `object`, `date`, `localDate`.
## 3. Utility
Helper methods:
```java
// boolean helpers: isTrue, isFalse, anyTrue, anyFalse allTrue, allFalse, valueEquals
user.isTrue("active"); // null-safe
user.isFalse("suspended");
user.allTrue("active", "verified");
user.anyFalse("active", "verified");
user.valueEquals("status", "OPEN");

// find() - deep lookup
String street = response.find("streetLine", String.class); // recurses nested objects/arrays

// static helper methods
Json.isValid(payload);
byte[] bytes = ...; 
Json json = Json.parse(bytes);
Person person = Json.parseAs(jsonString, Person.class);
String personJson = Json.asString(person);
Optional<Json> maybe = Json.from(maybeNullObject);
```

## 4. Mapping
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
## 5. Parsing/Converting
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
## 6. Removing
```java
person.remove("id");
person.remove("dob", "address", "employer");
person.remove(List.of("dob", "address", "employer"));
```
## 7. Spring REST
Works as `@RequestBody` because `Json` carries `@JsonAnySetter` / `@JsonAnyGetter`:
```java
@PutMapping("/v1/customers")
public void createCustomer(@RequestBody Json request) {
    customerService.createCustomer(request);
}
```
`RestTemplate` response body:
```java
public List<Product> getProducts(String url) {
    ResponseEntity<Json> responseEntity = restTemplate.getForEntity(url, Json.class);
    Json response = Objects.requireNonNull(responseEntity.getBody());
    return response.stream("data")
            .map(item -> Product.of(item.integer("id"))
                    .withName(item.string("name"))
                    .withPrice(item.decimal("price")))
            .toList();
}
```

# Configuration
### Jackson serialization/deserialization
Configuration via application.properties:
```properties
jackson.property-naming-strategy=UPPER_CAMEL_CASE
jackson.deserialization.fail-on-unknown-properties=false
jackson.serialization.fail-on-empty-beans=false
jackson.default-property-inclusion=ALWAYS
jackson.visibility.field=ANY
jackson.visibility.getter=NONE
jackson.visibility.is-getter=NONE
jackson.visibility.setter=NONE
jackson.disabled-features=WRITE_DATES_AS_TIMESTAMPS,FAIL_ON_EMPTY_BEANS
```
`disabled-features` / `enabled-features` accept any comma-separated values from `SerializationFeature`, `DeserializationFeature`, or `MapperFeature`.
### yupzip.Json collection type
JSON properties (keys/values) are stored in a java.util.Map `Map<String, Object> properties`.
This map is a HashMap by default, however this can be changed to LinkedHashMap if required via property:
```properties
yupzip.json.map-type=LINKED_HASH_MAP
```

# Contributing [![contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg?style=flat)](https://github.com/dwyl/esta/issues)

# License
This project is licensed under Apache License Version 2.0 - [LICENSE.md](LICENSE)
