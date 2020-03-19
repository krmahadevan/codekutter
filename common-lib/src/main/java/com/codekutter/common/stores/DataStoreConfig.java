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

package com.codekutter.common.stores;

import com.codekutter.common.auditing.IAuditContextGenerator;
import com.codekutter.zconfig.common.model.annotations.ConfigAttribute;
import com.codekutter.zconfig.common.model.annotations.ConfigPath;
import com.codekutter.zconfig.common.model.annotations.ConfigValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@Getter
@Setter
@Accessors(fluent = true)
@ConfigPath(path = "dataStore")
@SuppressWarnings("rawtypes")
@MappedSuperclass
public class DataStoreConfig {
    @ConfigAttribute(name = "dataStoreClass", required = true)
    @SuppressWarnings("rawtypes")
    @Transient
    private Class<? extends AbstractDataStore> dataStoreClass;
    @Column(name = "data_store_class")
    private String dataStoreClassString;
    @ConfigAttribute(name = "name", required = true)
    @Column(name = "name")
    private String name;
    @ConfigValue(name = "description")
    @Column(name = "description")
    private String description;
    @ConfigAttribute(name = "connection", required = true)
    @Column(name = "connection")
    private String connectionName;
    @ConfigAttribute(name = "connectionType", required = true)
    @Transient
    private Class<?> connectionType;
    @Column(name = "connection_type")
    private String connectionTypeString;
    @ConfigAttribute(name = "audited")
    @Column(name = "audited")
    private boolean audited = false;
    @ConfigAttribute(name = "auditLogger")
    @Column(name = "audit_logger_name")
    private String auditLogger;
    @ConfigAttribute(name = "auditContextProvider")
    @Transient
    private Class<? extends IAuditContextGenerator> auditContextProvider;
    @Column(name = "audit_context_provider_type")
    private String auditContextProviderClass;
}
