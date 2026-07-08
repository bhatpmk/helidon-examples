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
package io.helidon.examples.declarative.data.jdbc.streaming;

import java.math.BigDecimal;
import java.util.Map;

import io.helidon.json.binding.Json;

/**
 * Bounded application result produced by consuming a JDBC row stream.
 *
 * @param minimumId     requested minimum order identifier
 * @param orderCount    number of rows consumed
 * @param firstOrderId  first consumed order identifier, or {@code -1}
 * @param lastOrderId   last consumed order identifier, or {@code -1}
 * @param totalAmount   sum of consumed order amounts
 * @param ordersByRegion consumed order count by region
 * @param exhausted      whether traversal reached normal result-set exhaustion
 */
@Json.Entity
public record OrderSummary(long minimumId,
                           int orderCount,
                           long firstOrderId,
                           long lastOrderId,
                           BigDecimal totalAmount,
                           Map<String, Integer> ordersByRegion,
                           boolean exhausted) {
}
