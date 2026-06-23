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
 * Declarative mapper contract for a contact summary row with non-matching SQL aliases.
 */
@Data.Mapper(target = ContactCard.class)
@Data.Map(source = "contact_id", target = "id")
@Data.Map(source = "contact_display_name", target = "displayName")
@Data.Map(source = "primary_phone", target = "primaryPhone")
@Data.Map(source = "phone_count", target = "phoneCount")
@Data.Map(source = "tag_count", target = "tagCount")
interface ContactCardMapping {
}
