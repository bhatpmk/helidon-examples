--
-- Copyright (c) 2026 Oracle and/or its affiliates.
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

DROP TABLE IF EXISTS SALES_ORDER;

CREATE TABLE SALES_ORDER (
    ID BIGINT PRIMARY KEY,
    CUSTOMER VARCHAR(80) NOT NULL,
    REGION VARCHAR(20) NOT NULL,
    AMOUNT DECIMAL(12, 2) NOT NULL
);

INSERT INTO SALES_ORDER (ID, CUSTOMER, REGION, AMOUNT) VALUES
    (1, 'Acme Labs', 'Americas', 1250.00),
    (2, 'Alpine Systems', 'Europe', 820.50),
    (3, 'Blue Ocean', 'Asia Pacific', 410.25),
    (4, 'Cedar Works', 'Americas', 2375.00),
    (5, 'Delta Retail', 'Europe', 915.75),
    (6, 'Evergreen Group', 'Americas', 150.00),
    (7, 'Fabrikam', 'Asia Pacific', 1120.40),
    (8, 'Granite Finance', 'Europe', 675.00),
    (9, 'Harbor Foods', 'Americas', 340.60),
    (10, 'Inland Energy', 'Asia Pacific', 1999.99),
    (11, 'Juniper Health', 'Europe', 760.00),
    (12, 'Keystone Media', 'Americas', 505.50);
