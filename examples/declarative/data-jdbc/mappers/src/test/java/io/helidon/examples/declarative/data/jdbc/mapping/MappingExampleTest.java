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

import io.helidon.data.DataException;
import io.helidon.data.jdbc.JdbcClient;
import io.helidon.examples.declarative.data.jdbc.mapping.model.Contact;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactCard;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactDetail;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactGraph;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactRowReducer;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ImmutableContactGraph;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ImmutableContactGraphReducer;
import io.helidon.examples.declarative.data.jdbc.mapping.model.PhoneGraph;
import io.helidon.examples.declarative.data.jdbc.mapping.model.TagGraph;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the generated mapping shapes used by the example.
 */
class MappingExampleTest {

    @Test
    void generatedSourceContainsEveryMappingPath() throws Exception {
        String source = generatedRepository();

        assertTrue(source.contains(".map(MAPPER_LIST_CONTACTS).list()"), source);
        assertTrue(source.contains(".map(MAPPER_FIND_CONTACT).optional()"), source);
        assertTrue(source.contains("new ContactNameMapper()"), source);
        assertTrue(source.contains(".map(MAPPER_MAPPED_CONTACT).one()"), source);
        assertTrue(source.contains(".map(String.class).list()"), source);
        assertTrue(source.contains(".map(MAPPER_LIST_DETAILS).list()"), source);
        assertTrue(source.contains(".map(MAPPER_LIST_CARDS).list()"), source);
        assertTrue(source.contains(".reduce(new Reducer_ListGraphs())"), source);
        assertTrue(source.contains(".reduce(new ImmutableContactGraphReducer())"), source);
        assertTrue(source.contains(".reduce(new ContactRowReducer())"), source);
        assertTrue(source.contains("IdentityHashMap<ContactGraph, LinkedHashMap<Long, PhoneGraph>>"), source);
        assertTrue(source.contains("IdentityHashMap<PhoneGraph, LinkedHashMap<Long, TagGraph>>"), source);
        assertTrue(source.contains("if (phonesTagsId != null)"), source);
        assertTrue(source.contains("row.get(\"phoneId\", Long.class)"), source);
        assertTrue(source.contains("row.get(\"tagId\", Long.class)"), source);
        assertTrue(source.contains("row.required(\"phoneCount\", Long.class)"), source);
        assertTrue(source.contains("row.required(\"tagCount\", Long.class)"), source);
    }

    @Test
    void generatedSourceUsesDirectPublicClientCalls() throws Exception {
        String source = generatedRepository();

        assertTrue(source.contains("private final JdbcClient jdbcClient"), source);
        assertTrue(source.contains("jdbcClient.create(SQL_LIST_CONTACTS)"), source);
        assertTrue(source.contains("@Service.Named(\"contacts\") @Data.ProviderType(\"jdbc\")"), source);
        assertFalse(source.contains("ResultSet"), source);
        assertFalse(source.contains("Class.forName"), source);
        assertFalse(source.contains("JdbcRunner"), source);
    }

    @Test
    void flatProjectionPreservesNullOuterJoinChildren() {
        ContactDetail detail = new ContactDetail(1L, "Govind", null, null, null, null, null);

        assertEquals(1L, detail.contactId());
        assertNull(detail.phoneId());
        assertNull(detail.tagId());
    }

    @Test
    void graphModelRetainsOrderedChildren() {
        ContactGraph contact = new ContactGraph();
        contact.setId(1L);
        contact.setName("Govind");

        PhoneGraph phone = new PhoneGraph();
        phone.setId(101L);
        TagGraph tag = new TagGraph();
        tag.setId(1001L);
        tag.setName("primary");
        phone.setTags(List.of(tag));
        contact.setPhones(List.of(phone));

        assertEquals(List.of(101L), contact.getPhones().stream().map(PhoneGraph::getId).toList());
        assertEquals("primary", contact.getPhones().getFirst().getTags().getFirst().getName());
    }

    @Test
    void recordAndAggregateMappingsExposeExpectedValues() {
        assertEquals(new Contact(1L, "Govind"), new Contact(1L, "Govind"));
        assertEquals(2L, new ContactCard(1L, "Govind", "+91-0101", 2, 3).phoneCount());
    }

    @Test
    void explicitReducerDeduplicatesRowsAndPreservesFirstSeenOrder() {
        ContactRowReducer reducer = new ContactRowReducer();

        reducer.accept(row(Map.of("id", 2L, "name", "Krishna")));
        reducer.accept(row(Map.of("id", 1L, "name", "Govind")));
        reducer.accept(row(Map.of("id", 2L, "name", "Krishna")));

        assertEquals(List.of(new Contact(2L, "Krishna"), new Contact(1L, "Govind")), reducer.finish());
    }

    @Test
    void explicitGraphReducerCreatesImmutableGraphWithCompositePhoneIdentity() {
        ImmutableContactGraphReducer reducer = new ImmutableContactGraphReducer();

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

        List<ImmutableContactGraph> contacts = reducer.finish();
        assertEquals(List.of(2L, 1L, 3L), contacts.stream().map(ImmutableContactGraph::id).toList());
        assertEquals(List.of("mobile", "office"),
                     contacts.get(1).phones().stream().map(phone -> phone.type()).toList());
        assertEquals(List.of("primary", "sms"),
                     contacts.get(1).phones().getFirst().tags().stream().map(tag -> tag.name()).toList());
        assertTrue(contacts.get(2).phones().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> contacts.add(contacts.getFirst()));
        assertThrows(UnsupportedOperationException.class,
                     () -> contacts.get(1).phones().add(contacts.get(1).phones().getFirst()));
    }

    @Test
    void explicitGraphReducerRejectsIncompleteOrConflictingRows() {
        ImmutableContactGraphReducer incompletePhone = new ImmutableContactGraphReducer();
        assertThrows(DataException.class,
                     () -> incompletePhone.accept(row("contactId", 1L,
                                                      "contactName", "Govind",
                                                      "phoneId", 101L,
                                                      "phoneType", "mobile",
                                                      "phoneNumber", null,
                                                      "tagId", null,
                                                      "tagName", null)));

        ImmutableContactGraphReducer orphanTag = new ImmutableContactGraphReducer();
        assertThrows(DataException.class,
                     () -> orphanTag.accept(row("contactId", 1L,
                                                "contactName", "Govind",
                                                "phoneId", null,
                                                "phoneType", null,
                                                "phoneNumber", null,
                                                "tagId", 1001L,
                                                "tagName", "primary")));

        ImmutableContactGraphReducer conflictingRoot = new ImmutableContactGraphReducer();
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
            public <T> T get(int index, Class<T> type) {
                throw new UnsupportedOperationException("The sample reducer uses column labels");
            }

            @Override
            public <T> T get(String label, Class<T> type) {
                return type.cast(copy.get(label));
            }

            @Override
            public <T> T required(int index, Class<T> type) {
                throw new UnsupportedOperationException("The sample reducer uses column labels");
            }

            @Override
            public <T> T required(String label, Class<T> type) {
                T value = get(label, type);
                if (value == null) {
                    throw new IllegalStateException("Missing sample value: " + label);
                }
                return value;
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
