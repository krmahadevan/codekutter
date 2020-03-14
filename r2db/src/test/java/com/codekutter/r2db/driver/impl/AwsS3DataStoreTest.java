/*
 *  Copyright (2020) Subhabrata Ghosh (subho dot ghosh at outlook dot com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.codekutter.r2db.driver.impl;

import com.codekutter.common.ConfigTestConstants;
import com.codekutter.common.stores.model.User;
import com.codekutter.common.utils.LogUtils;
import com.codekutter.r2db.driver.EntityManager;
import com.codekutter.zconfig.common.ConfigProviderFactory;
import com.codekutter.zconfig.common.R2dbEnv;
import com.codekutter.zconfig.common.model.Version;
import com.google.common.base.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AwsS3DataStoreTest {

    private static final String BASE_PROPS_FILE =
            "src/test/resources/test-extended-env.properties";
    private static final User user = new User(UUID.randomUUID().toString());
    private static String encryptionKey = "21947a50-6755-47";
    private static String IV = "/NK/c+NKGUwMm0RF";
    private static EntityManager entityManager = null;

    @BeforeAll
    static void setup() throws Exception {
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(BASE_PROPS_FILE));
            String name = properties.getProperty(ConfigTestConstants.PROP_CONFIG_NAME);
            assertFalse(Strings.isNullOrEmpty(name));
            String filename = properties.getProperty(
                    ConfigTestConstants.PROP_CONFIG_FILE);
            assertFalse(Strings.isNullOrEmpty(filename));
            String vs = properties.getProperty(ConfigTestConstants.PROP_CONFIG_VERSION);
            assertFalse(Strings.isNullOrEmpty(vs));
            Version version = Version.parse(vs);
            assertNotNull(version);

            R2dbEnv.setup(R2dbEnv.class, name, filename, ConfigProviderFactory.EConfigType.XML, vs, encryptionKey);
            entityManager = R2dbEnv.env().getEntityManager();
            assertNotNull(entityManager);
        } catch (Throwable t) {
            LogUtils.error(AwsS3DataStoreTest.class, t);
            throw t;
        }
    }

    @AfterAll
    static void shutdown() {
        try {
            R2dbEnv.shutdown();
        } catch (Throwable t) {
            LogUtils.error(AwsS3DataStoreTest.class, t);
            throw t;
        }
    }

    @Test
    void move() {
    }

    @Test
    void create() {
    }

    @Test
    void update() {
    }

    @Test
    void delete() {
    }

    @Test
    void find() {
    }

    @Test
    void search() {
    }

    @Test
    void testSearch() {
    }
}