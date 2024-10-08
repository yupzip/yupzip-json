package com.yupzip.json.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.yupzip.json.Json;
import com.yupzip.json.JsonConfiguration;
import com.yupzip.json.JsonConfiguration.MapType;
import com.yupzip.json.JsonParseException;
import com.yupzip.json.PropertyRequiredException;
import com.yupzip.json.mock.Address;
import com.yupzip.json.mock.Person;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.yupzip.json.JsonConfiguration.MAP_TYPE;
import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.singletonList;

@ExtendWith(MockitoExtension.class)
class JJsonTest {

    private static final ObjectMapper JSON_PARSER = new ObjectMapper();
    private static final String MOCK_JSON_RESOURCE = "jsonapi-org-example.json";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'hh:mm:ss.SSSX";

    @Test
    void shouldParseJsonResource() throws IOException, ParseException {
        URL url = JJsonTest.class.getClassLoader().getResource(MOCK_JSON_RESOURCE);

        Json payload = JSON_PARSER.readValue(Objects.requireNonNull(url), JJson.class);
        Assertions.assertNotNull(payload);

        List<Json> dataList = payload.array("data");
        Assertions.assertFalse(dataList.isEmpty());

        Stream<Json> dataStream = payload.stream("data");
        Optional<Json> optionalData = dataStream.findFirst();
        Assertions.assertTrue(optionalData.isPresent());
        Assertions.assertEquals(optionalData.get(), dataList.getFirst());

        Json[] dataArray = payload.get("data", JJson[].class);
        Assertions.assertNotNull(dataArray);
        Assertions.assertEquals(1, dataArray.length);

        Json data = dataList.getFirst();
        Assertions.assertEquals("articles", data.string("type"));
        Assertions.assertEquals("1", data.string("id"));

        Json attributes = data.object("attributes");
        Assertions.assertEquals("JSON:API paints my bikeshed!", attributes.string("title"));
        Assertions.assertEquals("The shortest article. Ever.", attributes.string("body"));
        Assertions.assertEquals("2015-05-22T14:56:29.000Z", attributes.string("created"));
        Assertions.assertEquals("2015-05-22T14:56:28.000Z", attributes.string("updated"));

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
        Date createdDate = dateFormat.parse("2015-05-22T14:56:29.000Z");
        Assertions.assertEquals(createdDate, attributes.date("created", DATE_TIME_FORMAT));

        Json author = data.object("relationships").object("author");
        Assertions.assertEquals("people", author.object("data").string("type"));
        Assertions.assertEquals("42", author.object("data").string("id"));
    }

    @Test
    void shouldParseByteArray() {
        Json mock = Json.create()
                .put("id", 1)
                .put("name", "John")
                .put("weight", 90.1)
                .put("verified", true)
                .put("numbers", Arrays.asList(1, 2, 3));

        byte[] data = mock.toString().getBytes();

        Json parsedPerson = Json.parse(data);
        Assertions.assertEquals(1, parsedPerson.integer("id"));
        Assertions.assertEquals("John", parsedPerson.string("name"));
        Assertions.assertEquals(90.1, parsedPerson.decimal("weight"));
        Assertions.assertTrue(parsedPerson.bool("verified"));
        Assertions.assertEquals(3, parsedPerson.integers("numbers").size());

        Person person = Json.parseAs(data, Person.class);
        Assertions.assertEquals(1, person.getId());
        Assertions.assertEquals("John", person.getName());
        Assertions.assertEquals(90.1, person.getWeight());
        Assertions.assertTrue(person.getVerified());
        Assertions.assertEquals(3, person.getNumbers().size());
    }

    @Test
    void shouldBuildJsonResource() throws IOException {
        URL url = JJsonTest.class.getClassLoader().getResource(MOCK_JSON_RESOURCE);
        Json resource = JSON_PARSER.readValue(Objects.requireNonNull(url), JJson.class);

        Json payload = Json.create()
                .put("data", singletonList(Json.create()
                        .put("type", "articles")
                        .put("id", "1")
                        .put("attributes", Json.create()
                                .put("title", "JSON:API paints my bikeshed!")
                                .put("body", "The shortest article. Ever.")
                                .put("created", "2015-05-22T14:56:29.000Z")
                                .put("updated", "2015-05-22T14:56:28.000Z"))
                        .put("relationships", Json.create()
                                .put("author", Json.create()
                                        .put("data", Json.create()
                                                .put("id", "42")
                                                .put("type", "people"))))))
                .put("included", singletonList(Json.create()
                        .put("type", "people")
                        .put("id", "42")
                        .put("attributes", Json.create()
                                .put("name", "John")
                                .put("age", 80)
                                .put("gender", "male")))
                );

        Assertions.assertEquals(resource, payload);
        if (MAP_TYPE == MapType.LINKED_HASH_MAP) {
            Assertions.assertEquals(resource.toString(), payload.toString());
        }
    }

    @Test
    void shouldPutNullAndOnlyAddNotNull() {
        Json person = Json.create()
                .put("id", (Integer) null)
                .put("licences", (List<Json>) null)
                .add("name", (String) null)
                .add("address", (Json) null)
                .add("licence", Json.create())
                .add("addresses", (List<Json>) null)
                .add("employers", Arrays.asList(Json.create(), Json.create()))
                .add("age", 30);

        Assertions.assertTrue(person.hasKey("id"));
        Assertions.assertFalse(person.hasValueFor("id"));
        Assertions.assertTrue(person.hasKey("licences"));
        Assertions.assertFalse(person.hasKey("name"));
        Assertions.assertNull(person.string("id"));
        Assertions.assertFalse(person.hasKey("address"));
        Assertions.assertTrue(person.hasKey("licence"));
        Assertions.assertFalse(person.hasKey("addresses"));
        Assertions.assertTrue(person.hasKey("employers"));
        Assertions.assertNotNull(person.integer("age"));
        Assertions.assertEquals(30, person.integer("age"));
    }

    @Test
    void shouldTestValidJson() {
        Assertions.assertFalse(Json.isValid("text"));
        Assertions.assertFalse(Json.isValid(""));
        Assertions.assertTrue(Json.isValid(null));
        Assertions.assertTrue(Json.isValid("{}"));
        Assertions.assertTrue(Json.isValid("""
                {"id": 1}"""));
    }

    @Test
    void shouldPutMap() {
        Json person = Json.create()
                .put("id", 1)
                .put("name", "John");

        Json empty = Json.create();
        Assertions.assertTrue(empty.isEmpty());
        empty.put(person.asMap());

        Assertions.assertEquals(1, empty.integer("id"));
        Assertions.assertEquals("John", empty.string("name"));
    }

    @Test
    void shouldReturnObjectOrDefault() {
        Json data = Json.create()
                .put("employee", Json.create()
                        .put("id", 1)
                        .put("name", "John"));

        Json defaultAddress = Json.create().put("address1", "100 Pitt Street");
        Json defaultEmployee = Json.create()
                .put("id", 2)
                .put("name", "Jack");

        Assertions.assertNull(data.object("address"));
        Assertions.assertNotNull(data.object("employee"));
        Assertions.assertNotNull(data.objectOr("address", defaultAddress));
        Assertions.assertNotNull(data.objectOr("employee", defaultEmployee));
        Assertions.assertEquals(data.objectOr("employee", defaultEmployee).integer("id"), 1);
        Assertions.assertEquals(data.objectOr("address", defaultAddress).string("address1"), "100 Pitt Street");
    }

    @Test
    void shouldAppendValueToCollection() {
        Json person = Json.create()
                .append("contactNumbers", "0400000000")
                .append("ids", 1)
                .append("scores", 91.1);

        Assertions.assertEquals(1, person.strings("contactNumbers").size());
        Assertions.assertEquals(1, person.integers("ids").size());
        Assertions.assertEquals(1, person.decimals("scores").size());

        person.append("contactNumbers", "0400000001");
        person.append("ids", 2);
        person.append("scores", 95.3);

        Assertions.assertEquals(2, person.strings("contactNumbers").size());
        Assertions.assertEquals(2, person.integers("ids").size());
        Assertions.assertEquals(2, person.decimals("scores").size());

        Json city1 = Json.create().put("city", "Sydney");
        Json city2 = Json.create().put("city", "Melbourne");

        person.append("cities", city1);
        person.append("cities", city2);

        Assertions.assertEquals(2, person.array("cities").size());
    }

    @Test
    void shouldParseValues() {
        Json person = Json.create()
                .put("id", 1)
                .put("name", "John")
                .put("weight", 90.1)
                .put("verified", true)
                .put("sports", Arrays.asList("NFL", "NHL", "NBA"))
                .put("numbers", Arrays.asList(1, 2, 3))
                .put("scores", Arrays.asList(100.0, 97.3, 89.1));

        Assertions.assertEquals(1, person.integer("id"));
        Assertions.assertEquals("John", person.string("name"));
        Assertions.assertEquals(90.1, person.decimal("weight"));
        Assertions.assertTrue(person.bool("verified"));
        Assertions.assertEquals(Arrays.asList("NFL", "NHL", "NBA"), person.strings("sports"));
        Assertions.assertEquals(Arrays.asList(1, 2, 3), person.integers("numbers"));
        Assertions.assertEquals(Arrays.asList(100.0, 97.3, 89.1), person.decimals("scores"));
    }

    @Test
    void shouldParseDateValues() {
        Json dates = Json.create()
                .put("zonedDateTime", "2020-01-25T09:00:00+00:00")
                .put("dateTime", "2020-01-25T09:00:00")
                .put("date", "2020-01-25")
                .put("time", "09:00:00");

        Date date1 = Date.from(ZonedDateTime.of(2020, 1, 25, 9, 0, 0, 0, ZoneId.of("UTC")).toInstant());
        Date date2 = Date.from(ZonedDateTime.of(2020, 1, 25, 9, 0, 0, 0, ZoneId.systemDefault()).toInstant());
        Date date3 = Date.from(LocalDateTime.of(2020, 1, 25, 9, 0, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
        Assertions.assertEquals(date1, dates.date("zonedDateTime", "yyyy-MM-dd'T'hh:mm:ssXXX"));
        Assertions.assertEquals(date2, dates.date("dateTime", "yyyy-MM-dd'T'hh:mm:ss", ZoneId.systemDefault().toString()));
        Assertions.assertEquals(date3, dates.date("date", "time", " ", "yyyy-MM-dd hh:mm:ss"));

        LocalDateTime parsed = LocalDateTime.ofInstant(dates.dateOrNow("nullDate", "yyyy-MM-dd hh:mm:ss").toInstant(), ZoneId.systemDefault());
        Assertions.assertEquals(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), parsed.truncatedTo(ChronoUnit.SECONDS));
        Assertions.assertEquals(date1, dates.dateOrNow("zonedDateTime", "yyyy-MM-dd'T'hh:mm:ssXXX"));
        Assertions.assertThrows(JsonParseException.class, () -> dates.date("date", "yyyy/MM/dd"));
    }

    @Test
    void shouldThrowJsonParseExceptionForInvalidDataTypes() {
        Json person = Json.create()
                .put("id", 1)
                .put("name", "John")
                .put("weight", 90.1)
                .put("verified", true)
                .put("sports", Arrays.asList("NFL", "NHL", "NBA"))
                .put("numbers", Arrays.asList(1, 2, 3))
                .put("scores", Arrays.asList(100.0, 97.3, 89.1));

        Assertions.assertThrows(JsonParseException.class, () -> person.string("id"));
        Assertions.assertThrows(JsonParseException.class, () -> person.integer("name"));
        Assertions.assertThrows(JsonParseException.class, () -> person.integers("sports"));
        Assertions.assertThrows(JsonParseException.class, () -> person.decimal("verified"));
        Assertions.assertThrows(JsonParseException.class, () -> person.decimals("sports"));
        Assertions.assertThrows(JsonParseException.class, () -> person.bool("weight"));
        Assertions.assertThrows(JsonParseException.class, () -> Json.array(new Person()));
        Assertions.assertThrows(JsonParseException.class, () -> person.convertTo(JsonNodeCreator.class));
    }

    @Test
    void shouldParseValueOrReturnDefault() throws IOException {
        URL url = JJsonTest.class.getClassLoader().getResource(MOCK_JSON_RESOURCE);
        Json payload = JSON_PARSER.readValue(Objects.requireNonNull(url), JJson.class);

        Json data = payload.array("data").getFirst();
        Assertions.assertEquals("1", data.stringOr("id", "2"));
        Assertions.assertEquals("2", data.stringOr("missingId", "2"));

        payload.put("flag", true);
        payload.put("integer", 1);
        payload.put("decimal", 1.0);

        Assertions.assertTrue(payload.boolOr("flag", false));
        Assertions.assertFalse(payload.boolOr("missingFlag", false));
        Assertions.assertEquals(1, payload.integerOr("integer", 2));
        Assertions.assertEquals(2, payload.integerOr("missingInteger", 2));
        Assertions.assertEquals(1.0, payload.decimalOr("decimal", 2.0));
        Assertions.assertEquals(2.0, payload.decimalOr("missingDecimal", 2.0));
    }

    @Test
    void shouldReturnOptionalForSeek() throws IOException {
        URL url = JJsonTest.class.getClassLoader().getResource(MOCK_JSON_RESOURCE);
        Json payload = JSON_PARSER.readValue(Objects.requireNonNull(url), JJson.class);

        Assertions.assertTrue(payload.seekArray("data").isPresent());
        Assertions.assertFalse(payload.seekArray("missingData").isPresent());

        Json data = payload.array("data").getFirst();
        Assertions.assertTrue(data.seek("attributes").isPresent());
        Assertions.assertFalse(data.seek("missingAttributes").isPresent());
    }

    @Test
    void shouldConvertToPojo() {
        Json person = Json.create()
                .put("id", 1)
                .put("name", "John")
                .put("weight", 90.1)
                .put("verified", true)
                .put("unknownProperty", "something");

        Person mockPerson = person.convertTo(Person.class);

        Assertions.assertNotNull(mockPerson);
        Assertions.assertEquals(1, mockPerson.getId());
        Assertions.assertEquals("John", mockPerson.getName());
        Assertions.assertEquals(90.1, mockPerson.getWeight());
        Assertions.assertTrue(mockPerson.getVerified());
    }

    @Test
    void shouldThrowJsonExceptionWhenFailOnUnknownPropertiesFeatureIsTrue() {
        Properties props = JacksonConfiguration.loadProperties();
        if (parseBoolean(props.getProperty("jackson.deserialization.fail-on-unknown-properties", "false"))) {
            Assertions.assertThrows(JsonParseException.class, () -> Json.create().put("unknownProperty", "something").convertTo(Person.class));
        }
    }

    @Test
    void shouldParseJsonString() {
        String jsonString = "{\"id\":1,\"name\":\"John\",\"weight\":90.1,\"verified\":true, \"contactNumbers\":[\"0400000000\",\"0400000001\"]}";

        Json person = Json.parse(jsonString);

        Assertions.assertEquals(1, person.integer("id"));
        Assertions.assertEquals("John", person.string("name"));
        Assertions.assertEquals(90.1, person.decimal("weight"));
        Assertions.assertTrue(person.bool("verified"));
        Assertions.assertEquals(Arrays.asList("0400000000", "0400000001"), person.strings("contactNumbers"));
        Assertions.assertEquals("{\"id\":1}", Json.create().put("id", 1).toString());

        Person parsedPerson = Json.parseAs(jsonString, Person.class);
        Assertions.assertNotNull(parsedPerson);
        Assertions.assertEquals(1, parsedPerson.getId());

        String objectAsString = Json.asString(parsedPerson);
        Assertions.assertEquals(parsedPerson.getId(), Json.parseAs(objectAsString, Person.class).getId());

        Assertions.assertThrows(JsonParseException.class, () -> Json.parseAs(jsonString, JsonNodeCreator.class));
    }

    @Test
    void shouldThrowParseExceptionForInvalidJson() {
        Assertions.assertThrows(JsonParseException.class, () -> Json.parse(JsonConfiguration.class));
        Assertions.assertThrows(JsonParseException.class, () -> Json.parse("{\"id\":1,\"name\":\"John\",\"weight\":90.1,\"verified\":true"));
    }

    @Test
    void shouldParsePojo() {
        Person mockPerson = new Person();
        mockPerson.setId(1);
        mockPerson.setName("John");
        mockPerson.setWeight(90.1);
        mockPerson.setVerified(true);
        mockPerson.setContactNumbers(Arrays.asList("0400000000", "0400000001"));

        Json person = Json.parse(mockPerson);

        Assertions.assertEquals(1, person.integer("id"));
        Assertions.assertEquals("John", person.string("name"));
        Assertions.assertEquals(90.1, person.decimal("weight"));
        Assertions.assertTrue(person.bool("verified"));
        Assertions.assertEquals(Arrays.asList("0400000000", "0400000001"), person.strings("contactNumbers"));

        Optional<Json> optionalPerson = Json.from(mockPerson);
        Assertions.assertTrue(optionalPerson.isPresent());
        Assertions.assertEquals(1, optionalPerson.get().integer("id"));

        List<Json> persons = Json.array(Arrays.asList(mockPerson, new Person()));
        Assertions.assertEquals(2, persons.size());
    }

    @Test
    void shouldMapValues() {
        Json person = Json.create()
                .put("id", 1)
                .put("name", "John")
                .put("weight", 90.1)
                .put("verified", true)
                .put("contactNumbers", Arrays.asList("0400000000", "0400000001"))
                .put("numbers", Arrays.asList(1, 2, 3))
                .put("scores", Arrays.asList(100.0, 97.3, 89.1))
                .put("address", Json.create()
                        .put("addressLine", "100 George Street")
                        .put("postCode", "2000")
                        .put("state", "NSW")
                        .put("country", "Australia"))
                .put("addresses", Arrays.asList(
                        Json.create()
                                .put("addressLine", "100 George Street")
                                .put("postCode", "2000")
                                .put("state", "NSW")
                                .put("country", "Australia"),
                        Json.create()
                                .put("addressLine", "100 Pitt Street")
                                .put("postCode", "2000")
                                .put("state", "NSW")
                                .put("country", "Australia"))
                );

        Person mockPerson = new Person();
        Address mockAddress = new Address();

        person.integer("id", mockPerson::setId)
                .map("name", mockPerson::setName)
                .decimal("weight", mockPerson::setWeight)
                .bool("verified", mockPerson::setVerified)
                .strings("contactNumbers", mockPerson::setContactNumbers)
                .integers("numbers", mockPerson::setNumbers)
                .decimals("scores", mockPerson::setScores)
                .object("address", address -> address
                        .string("addressLine", mockAddress::setAddressLine)
                        .string("postCode", mockAddress::setPostCode)
                        .string("state", mockAddress::setState)
                        .string("country", mockAddress::setCountry))
                .array("addresses", jsonArray -> mockPerson.setAddresses(jsonArray
                        .stream()
                        .map(jsonObject -> jsonObject.convertTo(Address.class))
                        .collect(Collectors.toList())));

        Assertions.assertEquals(1, mockPerson.getId());
        Assertions.assertEquals("John", mockPerson.getName());
        Assertions.assertEquals(90.1, mockPerson.getWeight());
        Assertions.assertTrue(mockPerson.getVerified());
        Assertions.assertEquals(Arrays.asList("0400000000", "0400000001"), mockPerson.getContactNumbers());
        Assertions.assertEquals(Arrays.asList(1, 2, 3), mockPerson.getNumbers());
        Assertions.assertEquals(Arrays.asList(100.0, 97.3, 89.1), mockPerson.getScores());
        Assertions.assertEquals("100 George Street", mockAddress.getAddressLine());
        Assertions.assertEquals("2000", mockAddress.getPostCode());
        Assertions.assertEquals("NSW", mockAddress.getState());
        Assertions.assertEquals("Australia", mockAddress.getCountry());
        Assertions.assertEquals(2, mockPerson.getAddresses().size());
        Assertions.assertEquals("100 Pitt Street", mockPerson.getAddresses().get(1).getAddressLine());
    }

    @Test
    void shouldFindValues() {
        Json person = Json.create()
                .put("id", 1)
                .put("name", "John")
                .put("weight", 90.1)
                .put("verified", true)
                .put("car", Collections.singletonMap("make", "vw"))
                .put("numbers", Arrays.asList(1, 2, 3))
                .put("scores", Arrays.asList(100.0, 97.3, 89.1))
                .put("salary", Json.create()
                        .put("type", "weekly")
                        .put("amount", "2000"))
                .put("friend", Collections.singletonMap("", ""))
                .put("addresses", Arrays.asList(
                        Json.create()
                                .put("addressLine", "100 George Street")
                                .put("postCode", "2000")
                                .put("state", "NSW")
                                .put("country", "Australia"),
                        Json.create()
                                .put("addressLine", "100 Pitt Street")
                                .put("postCode", "2000")
                                .put("state", "NSW")
                                .put("country", "Australia")))
                .put("addressMap", Arrays.asList(
                        Collections.singletonMap("streetLine", "100 George Street"),
                        Collections.singletonMap("streetLine", "100 Pitt Street"))
                );

        Assertions.assertEquals(1, person.find("id", Integer.class));
        Assertions.assertEquals("John", person.find("name", String.class));
        Assertions.assertTrue(person.find("verified", Boolean.class));
        Assertions.assertEquals("vw", person.find("make", String.class));
        Assertions.assertEquals(3, person.find("numbers", Integer[].class).length);
        Assertions.assertEquals(1, person.find("numbers", Integer[].class)[0]);
        Assertions.assertEquals("weekly", person.find("type", String.class));
        Assertions.assertEquals("100 George Street", person.find("addressLine", String.class));
        Assertions.assertEquals("100 George Street", person.find("streetLine", String.class));
    }

    @Test
    void shouldThrowPropertyRequiredException() {
        Json person = Json.create()
                .put("id", 1)
                .put("name", "John")
                .put("weight", 90.1)
                .put("verified", true)
                .put("dob", "1990-01-01")
                .put("address", Json.create().put("addressLine", "100 George Street"));

        Assertions.assertNotNull(person.objectOrThrow("address"));
        Assertions.assertNotNull(person.objectOrThrow("address", new PropertyRequiredException()));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.objectOrThrow("company"));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.objectOrThrow("company", new PropertyRequiredException()));

        Assertions.assertEquals(1, person.integerOrThrow("id"));
        Assertions.assertEquals(1, person.integerOrThrow("id", new PropertyRequiredException()));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.integerOrThrow("identifier"));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.integerOrThrow("identifier", new PropertyRequiredException()));

        Assertions.assertEquals("John", person.stringOrThrow("name"));
        Assertions.assertEquals("John", person.stringOrThrow("name", new PropertyRequiredException()));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.stringOrThrow("firstName"));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.stringOrThrow("firstName", new PropertyRequiredException()));

        Assertions.assertEquals(90.1, person.decimalOrThrow("weight"));
        Assertions.assertEquals(90.1, person.decimalOrThrow("weight", new PropertyRequiredException()));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.decimalOrThrow("height"));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.decimalOrThrow("height", new PropertyRequiredException()));

        Assertions.assertEquals(true, person.boolOrThrow("verified"));
        Assertions.assertEquals(true, person.boolOrThrow("verified", new PropertyRequiredException()));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.boolOrThrow("hasLicence"));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.boolOrThrow("hasLicence", new PropertyRequiredException()));

        Date dob = Date.from(LocalDate.of(1990, 1, 1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        Assertions.assertEquals(dob, person.dateOrThrow("dob", "yyyy-MM-dd"));
        Assertions.assertEquals(dob, person.dateOrThrow("dob", "yyyy-MM-dd", new PropertyRequiredException()));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.dateOrThrow("dateOfBirth", "yyyy-MM-dd"));
        Assertions.assertThrows(PropertyRequiredException.class, () -> person.dateOrThrow("dateOfBirth", "yyyy-MM-dd", new PropertyRequiredException()));

    }

    @Test
    void shouldParseStringAsNumberOrBoolean() {
        Json person = Json.create()
                .put("id", "1")
                .put("name", "John")
                .put("weight", "90.1")
                .put("verified", "true");

        Assertions.assertEquals(1, person.integer("id"));
        Assertions.assertEquals(1.0, person.decimal("id"));
        Assertions.assertEquals(90.1, person.decimal("weight"));
        Assertions.assertEquals(true, person.bool("verified"));
    }

    @Test
    void shouldCheckBooleanValue() {
        Json person = Json.create()
                .put("id", "true")
                .put("name", "John")
                .put("registered", true)
                .put("verified", false);

        Assertions.assertTrue(person.isTrue("registered"));
        Assertions.assertTrue(person.isFalse("verified"));
        Assertions.assertTrue(person.isTrue("id"));
        Assertions.assertFalse(person.isFalse("age"));
    }

    @Test
    void shouldCheckMultipleBooleanValues() {
        Json person = Json.create()
                .put("id", "1")
                .put("name", "John")
                .put("registered", true)
                .put("verified", true);

        Assertions.assertTrue(person.anyTrue("registered", "verified"));
        Assertions.assertFalse(person.anyFalse("registered", "verified"));
        Assertions.assertTrue(person.allTrue("registered", "verified"));
        Assertions.assertFalse(person.allFalse("registered", "verified"));
    }

    @Test
    void shouldCheckValueEquals() {
        Json person = Json.create()
                .put("id", "1")
                .put("name", "John")
                .put("registered", true)
                .put("verified", true);

        Assertions.assertTrue(person.valueEquals("id", "1"));
        Assertions.assertFalse(person.valueEquals("id", "2"));
        Assertions.assertFalse(person.valueEquals("age", "30"));
    }

    @Test
    void shouldRemove() {
        Json person = Json.create()
                .put("id", "1")
                .put("name", "John")
                .put("registered", true)
                .put("verified", true);

        Assertions.assertTrue(person.remove("id"));
        Assertions.assertFalse(person.hasKey("id"));
        Assertions.assertFalse(person.hasValueFor("id"));
        Assertions.assertFalse(person.remove("age"));
    }

    @Test
    void shouldRemoveAll() {
        Json person = Json.create()
                .put("id", "1")
                .put("name", "John")
                .put("registered", true)
                .put("verified", true);

        Assertions.assertTrue(person.remove("id", "name"));
        Assertions.assertFalse(person.hasKey("id"));
        Assertions.assertFalse(person.hasKey("name"));
        Assertions.assertFalse(person.hasValueFor("id"));
        Assertions.assertFalse(person.hasValueFor("name"));

        Assertions.assertFalse(person.remove((String) null));

        Assertions.assertFalse(person.remove("registered2"));
        Assertions.assertFalse(person.hasKey("registered2"));
        Assertions.assertFalse(person.hasKey("registered2"));
        Assertions.assertTrue(person.hasValueFor("registered"));
        Assertions.assertTrue(person.hasValueFor("registered"));

        Assertions.assertTrue(person.remove(List.of("verified", "verified2")));
        Assertions.assertFalse(person.hasKey("verified"));
        Assertions.assertFalse(person.hasKey("verified2"));

        Assertions.assertFalse(person.remove((List<String>) null));
    }

    @Test
    void shouldParseLocalDate() {
        Json payload = Json.create()
                .put("date", "2024-10-01")
                .put("today", DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now()));

        Assertions.assertEquals(LocalDate.of(2024, 10, 1), payload.localDate("date", "yyyy-MM-dd"));
        Assertions.assertThrows(DateTimeParseException.class, () -> payload.localDate("date", "yyyy-MMM-dd"));

        Assertions.assertEquals(LocalDate.now(), payload.localDateOrToday("today", "yyyy-MM-dd"));
        Assertions.assertEquals(LocalDate.now(), payload.localDateOrToday("today", "yyyy-MM-dd"));
        Assertions.assertThrows(DateTimeParseException.class, () -> payload.localDateOrToday("today", "yyyy-MMM-dd"));

        Assertions.assertEquals(LocalDate.of(2024, 10, 1), payload.localDateOr("date", "yyyy-MM-dd", LocalDate.of(2024, 10, 2)));
        Assertions.assertEquals(LocalDate.of(2024, 10, 2), payload.localDateOr("date2", "yyyy-MM-dd", LocalDate.of(2024, 10, 2)));
        Assertions.assertThrows(DateTimeParseException.class, () -> payload.localDateOr("today", "yyyy-MMM-dd", LocalDate.now()));

        Assertions.assertEquals(LocalDate.of(2024, 10, 1), payload.localDateOrThrow("date", "yyyy-MM-dd", new RuntimeException()));
        Assertions.assertThrows(RuntimeException.class, () -> payload.localDate("date2", "yyyy-MMM-dd"));
    }
}
