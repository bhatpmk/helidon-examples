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
package io.helidon.examples.declarative.data.jdbc.mapping.model;

import io.helidon.data.Data;

/**
 * Declarative reducer contract for a joined contact graph with non-path SQL aliases.
 */
@Data.Mapper(target = Contact.class)
@Data.Map(source = "contact_key", target = "id")
@Data.Map(source = "contact_name", target = "name")
@Data.Map(source = "phone_key", target = "phones.id")
@Data.Map(source = "phone_kind", target = "phones.type")
@Data.Map(source = "phone_number", target = "phones.phone")
@Data.Map(source = "tag_key", target = "phones.tags.id")
@Data.Map(source = "tag_name", target = "phones.tags.name")
@Data.Key(source = "contact_key")
@Data.Key(source = "phone_key", target = "phones")
@Data.Key(source = "tag_key", target = "phones.tags")
interface ContactGraphMapping {
}
