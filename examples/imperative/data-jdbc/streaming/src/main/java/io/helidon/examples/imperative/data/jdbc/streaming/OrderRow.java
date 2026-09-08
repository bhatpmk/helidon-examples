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
package io.helidon.examples.imperative.data.jdbc.streaming;

import java.math.BigDecimal;

/**
 * One sales order mapped while a provider-owned JDBC cursor is open.
 *
 * @param id order identifier
 * @param customer customer name
 * @param region sales region
 * @param amount order amount
 */
public record OrderRow(long id, String customer, String region, BigDecimal amount) {
}
