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

import io.helidon.json.binding.Json;

/**
 * Flat projection of one row returned by the contact, phone, and tag join.
 * <p>
 * The nullable child identifiers demonstrate generated mapping of left-joined columns without implying relationship
 * reduction. Repeated contact and phone values remain visible in separate rows.
 *
 * @param contactId contact identifier
 * @param contactName contact name
 * @param phoneId phone identifier, or {@code null} when the contact has no phone
 * @param phoneType phone type, or {@code null}
 * @param phoneNumber phone number, or {@code null}
 * @param tagId tag identifier, or {@code null} when the phone has no tag
 * @param tagName tag name, or {@code null}
 */
@Json.Entity
public record ContactDetail(Long contactId,
                            String contactName,
                            Long phoneId,
                            String phoneType,
                            String phoneNumber,
                            Long tagId,
                            String tagName) {
}
