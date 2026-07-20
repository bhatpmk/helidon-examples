/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.examples.declarative.data.jdbc.mapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.data.DataException;
import io.helidon.data.jdbc.JdbcClient;
import io.helidon.examples.declarative.data.jdbc.mapping.model.Contact;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactCard;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactDetail;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactGraph;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactName;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactNameMapper;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactPhone;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactPhoneMapper;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactRowReducer;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactSummaryMapper;
import io.helidon.examples.declarative.data.jdbc.mapping.model.CustomContactGraph;
import io.helidon.examples.declarative.data.jdbc.mapping.model.CustomContactGraphReducer;
import io.helidon.examples.declarative.data.jdbc.mapping.model.PhoneGraph;
import io.helidon.examples.declarative.data.jdbc.mapping.model.TagGraph;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression tests for the generated mapping shapes used by the example.
 */
class MappingExampleTest {

    @Test
    void generatedSourceContainsEveryMappingPath() throws Exception {
        String source = generatedRepository();

        assertThat(source, containsString("Optional<JdbcClient.RowMapper<Contact>> contactRowMapper"));
        assertThat(source, containsString("DEFAULT_CONTACT_ROW_MAPPER = row -> new Contact"));
        assertThat(source, containsString(".map(contactRowMapper).list()"));
        assertThat(source, containsString(".map(contactRowMapper).optional()"));
        assertThat(source, containsString("ContactNameMapper contactNameMapper"));
        assertThat(source, containsString("ContactSummaryMapper contactSummaryMapper"));
        assertThat(source, containsString("JdbcClient.RowMapper<ContactName> contactNameMapper"));
        assertThat(source, containsString("JdbcClient.RowMapper<ContactName> contactSummaryMapper"));
        assertThat(source, containsString("JdbcClient.RowMapper<ContactPhone> contactPhoneRowMapper"));
        assertThat(source, not(containsString("new ContactNameMapper()")));
        assertThat(source, not(containsString("new ContactSummaryMapper()")));
        assertThat(source, containsString(".map(contactNameMapper).one()"));
        assertThat(source, containsString(".map(contactSummaryMapper).one()"));
        assertThat(source, containsString(".map(contactPhoneRowMapper).one()"));
        assertThat(source, containsString(".map(String.class).list()"));
        assertThat(source, containsString(".map(contactDetailRowMapper).list()"));
        assertThat(source, containsString(".map(contactCardRowMapper).list()"));
        assertThat(source, containsString(".reduce(new Reducer_ListGraphs())"));
        assertThat(source, containsString(".reduce(new CustomContactGraphReducer())"));
        assertThat(source, containsString(".reduce(new ContactRowReducer())"));
        assertThat(source, containsString("LinkedHashMap<Long, RootAccumulator> rootsByIdentity"));
        assertThat(source, containsString("LinkedHashMap<Long, PhonesAccumulator> phonesByIdentity"));
        assertThat(source, containsString("LinkedHashMap<Long, Phones_TagsAccumulator> tagsByIdentity"));
        assertThat(source, containsString("new ContactGraph(id, name, List.copyOf(phonesValues))"));
        assertThat(source, containsString("new PhoneGraph(id, type, phone, List.copyOf(tagsValues))"));
        assertThat(source, containsString("new TagGraph(id, name)"));
        assertThat(source, containsString("row.optional(\"phones.id\", Long.class).orElse(null)"));
        assertThat(source, containsString("row.optional(\"phones.tags.id\", Long.class).orElse(null)"));
        assertThat(source, containsString("if (phonesIdValue == null)"));
        assertThat(source, containsString("Graph scope 'phones.tags' has an identity while ancestor scope 'phones' is absent"));
        assertThat(source, containsString("Conflicting projected value for graph scope 'phones' property 'phone'"));
        assertThat(source, containsString("row.required(\"phoneCount\", Long.class)"));
        assertThat(source, containsString("row.required(\"tagCount\", Long.class)"));
    }

    @Test
    void generatedSourceUsesDirectPublicClientCalls() throws Exception {
        String source = generatedRepository();

        assertThat(source, containsString("private final JdbcClient jdbcClient"));
        assertThat(source, containsString("jdbcClient.create(SQL_LIST_CONTACTS)"));
        assertThat(source, containsString("@Service.Named(\"contacts\") @Data.ProviderType(\"jdbc\")"));
        assertThat(source, not(containsString("ResultSet")));
        assertThat(source, not(containsString("Class.forName")));
        assertThat(source, not(containsString("JdbcRunner")));
    }

    @Test
    void generatedServiceDescriptorPreservesMapperTypes() throws Exception {
        String descriptor = Files.readString(Path.of(
                "target/generated-sources/annotations/io/helidon/examples/declarative/data/jdbc/mapping/model/"
                        + "ContactRepository__Jdbc__ServiceDescriptor.java"));

        assertThat(descriptor,
                   containsString("GenericType<Optional<JdbcClient.RowMapper<Contact>>>"));
        assertThat(descriptor, containsString("GenericType<JdbcClient.RowMapper<ContactPhone>>"));
        assertThat(descriptor, containsString("GenericType<ContactNameMapper>"));
        assertThat(descriptor, containsString("GenericType<ContactSummaryMapper>"));
    }

    @Test
    void flatProjectionPreservesNullOuterJoinChildren() {
        ContactDetail detail = new ContactDetail(1L, "Govind", null, null, null, null, null);

        assertThat(detail.contactId(), is(1L));
        assertThat(detail.phoneId(), nullValue());
        assertThat(detail.tagId(), nullValue());
    }

    @Test
    void graphModelRetainsOrderedChildren() {
        TagGraph tag = new TagGraph(1001L, "primary");
        PhoneGraph phone = new PhoneGraph(101L, "mobile", "+91-0101", List.of(tag));
        ContactGraph contact = new ContactGraph(1L, "Govind", List.of(phone));

        assertThat(contact.phones().stream().map(PhoneGraph::id).toList(), is(List.of(101L)));
        assertThat(contact.phones().getFirst().tags().getFirst().name(), is("primary"));
    }

    @Test
    void recordAndAggregateMappingsExposeExpectedValues() {
        assertThat(new Contact(1L, "Govind"), is(new Contact(1L, "Govind")));
        assertThat(new ContactName(1L, "GOVIND"), is(new ContactName(1L, "GOVIND")));
        assertThat(new ContactPhone(1L, "Govind", "mobile: +91-0101"),
                   is(new ContactPhone(1L, "Govind", "mobile: +91-0101")));
        assertThat(new ContactCard(1L, "Govind", "+91-0101", 2, 3).phoneCount(), is(2L));
    }

    @Test
    void exactMapperRenamesAndFormatsContactColumns() {
        ContactName mapped = new ContactNameMapper().map(row(Map.of("id", 1L, "name", "Govind")));

        assertThat(mapped, is(new ContactName(1L, "GOVIND")));
    }

    @Test
    void exactMapperSelectionSupportsDifferentInterpretationsOfOneResultType() {
        ContactName mapped = new ContactSummaryMapper().map(row(Map.of("id", 1L,
                                                                      "name", "Govind",
                                                                      "phoneCount", 2L,
                                                                      "tagCount", 3L)));

        assertThat(mapped, is(new ContactName(1L, "Govind (2 phones, 3 tags)")));
    }

    @Test
    void genericMapperBuildsApplicationSpecificPhoneLabel() {
        ContactPhone mapped = new ContactPhoneMapper().map(row(Map.of("contactId", 1L,
                                                                       "contactName", "Govind",
                                                                       "phoneType", "mobile",
                                                                       "phoneNumber", "+91-0101")));

        assertThat(mapped, is(new ContactPhone(1L, "Govind", "mobile: +91-0101")));
    }

    @Test
    void explicitReducerDeduplicatesRowsAndPreservesFirstSeenOrder() {
        ContactRowReducer reducer = new ContactRowReducer();

        reducer.accept(row(Map.of("id", 2L, "name", "Krishna")));
        reducer.accept(row(Map.of("id", 1L, "name", "Govind")));
        reducer.accept(row(Map.of("id", 2L, "name", "Krishna")));

        assertThat(reducer.finish(), is(List.of(new Contact(2L, "Krishna"), new Contact(1L, "Govind"))));
    }

    @Test
    void explicitGraphReducerCreatesCustomRecordGraphWithCompositePhoneIdentity() {
        CustomContactGraphReducer reducer = new CustomContactGraphReducer();

        reducer.accept(row("contactId", 2L,
                           "contactName", "Krishna",
                           "phoneId", 201L,
                           "phoneType", "mobile",
                           "phoneNumber", "+91-080-5555-0201",
                           "tagId", 2001L,
                           "tagName", "primary"));
        reducer.accept(row("contactId", 1L,
                           "contactName", "Govind",
                           "phoneId", 101L,
                           "phoneType", "mobile",
                           "phoneNumber", "+91-080-5555-0101",
                           "tagId", 1001L,
                           "tagName", "primary"));
        reducer.accept(row("contactId", 1L,
                           "contactName", "Govind",
                           "phoneId", 101L,
                           "phoneType", "mobile",
                           "phoneNumber", "+91-080-5555-0101",
                           "tagId", 1002L,
                           "tagName", "sms"));
        reducer.accept(row("contactId", 1L,
                           "contactName", "Govind",
                           "phoneId", 101L,
                           "phoneType", "mobile",
                           "phoneNumber", "+91-080-5555-0101",
                           "tagId", 1002L,
                           "tagName", "sms"));
        reducer.accept(row("contactId", 1L,
                           "contactName", "Govind",
                           "phoneId", 102L,
                           "phoneType", "office",
                           "phoneNumber", "+91-080-5555-0102",
                           "tagId", null,
                           "tagName", null));
        reducer.accept(row("contactId", 3L,
                           "contactName", "Madhav",
                           "phoneId", null,
                           "phoneType", null,
                           "phoneNumber", null,
                           "tagId", null,
                           "tagName", null));

        List<CustomContactGraph> contacts = reducer.finish();
        assertThat(contacts.stream().map(CustomContactGraph::id).toList(), is(List.of(2L, 1L, 3L)));
        assertThat(contacts.get(1).phones().stream().map(phone -> phone.type()).toList(),
                   is(List.of("mobile", "office")));
        assertThat(contacts.get(1).phones().getFirst().tags().stream().map(tag -> tag.name()).toList(),
                   is(List.of("primary", "sms")));
        assertThat(contacts.get(2).phones().isEmpty(), is(true));
        assertThrows(UnsupportedOperationException.class, () -> contacts.add(contacts.getFirst()));
        assertThrows(UnsupportedOperationException.class,
                     () -> contacts.get(1).phones().add(contacts.get(1).phones().getFirst()));
    }

    @Test
    void explicitGraphReducerRejectsIncompleteOrConflictingRows() {
        CustomContactGraphReducer incompletePhone = new CustomContactGraphReducer();
        assertThrows(DataException.class,
                     () -> incompletePhone.accept(row("contactId", 1L,
                                                      "contactName", "Govind",
                                                      "phoneId", 101L,
                                                      "phoneType", "mobile",
                                                      "phoneNumber", null,
                                                      "tagId", null,
                                                      "tagName", null)));

        CustomContactGraphReducer orphanTag = new CustomContactGraphReducer();
        assertThrows(DataException.class,
                     () -> orphanTag.accept(row("contactId", 1L,
                                                "contactName", "Govind",
                                                "phoneId", null,
                                                "phoneType", null,
                                                "phoneNumber", null,
                                                "tagId", 1001L,
                                                "tagName", "primary")));

        CustomContactGraphReducer conflictingRoot = new CustomContactGraphReducer();
        conflictingRoot.accept(row("contactId", 1L,
                                   "contactName", "Govind",
                                   "phoneId", null,
                                   "phoneType", null,
                                   "phoneNumber", null,
                                   "tagId", null,
                                   "tagName", null));
        assertThrows(DataException.class,
                     () -> conflictingRoot.accept(row("contactId", 1L,
                                                       "contactName", "Different",
                                                       "phoneId", null,
                                                       "phoneType", null,
                                                       "phoneNumber", null,
                                                       "tagId", null,
                                                       "tagName", null)));
    }

    private static JdbcClient.Row row(Map<String, Object> values) {
        Map<String, Object> copy = new HashMap<>(values);
        return new JdbcClient.Row() {
            @Override
            public <T> Optional<T> optional(int index, Class<T> type) {
                throw new UnsupportedOperationException("The sample reducer uses column labels");
            }

            @Override
            public <T> Optional<T> optional(String label, Class<T> type) {
                return Optional.ofNullable(type.cast(copy.get(label)));
            }

            @Override
            public <T> T required(int index, Class<T> type) {
                throw new UnsupportedOperationException("The sample reducer uses column labels");
            }

            @Override
            public <T> T required(String label, Class<T> type) {
                return optional(label, type)
                        .orElseThrow(() -> new IllegalStateException("Missing sample value: " + label));
            }
        };
    }

    private static JdbcClient.Row row(Object... entries) {
        Map<String, Object> values = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            values.put((String) entries[i], entries[i + 1]);
        }
        return row(values);
    }

    private static String generatedRepository() throws Exception {
        return Files.readString(Path.of("target/generated-sources/annotations/io/helidon/examples/declarative/data/"
                                                 + "jdbc/mapping/model/ContactRepository__Jdbc.java"));
    }
}
