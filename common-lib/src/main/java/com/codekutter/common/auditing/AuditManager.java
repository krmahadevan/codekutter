package com.codekutter.common.auditing;

import com.codekutter.common.model.AuditRecord;
import com.codekutter.common.model.EAuditType;
import com.codekutter.common.model.IKeyed;
import com.codekutter.common.stores.AbstractDataStore;
import com.codekutter.common.stores.DataStoreManager;
import com.codekutter.common.utils.ConfigUtils;
import com.codekutter.common.utils.LogUtils;
import com.codekutter.zconfig.common.ConfigurationException;
import com.codekutter.zconfig.common.IConfigurable;
import com.codekutter.zconfig.common.model.annotations.ConfigPath;
import com.codekutter.zconfig.common.model.nodes.*;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@Accessors(fluent = true)
@ConfigPath(path = "audit-manager")
public class AuditManager implements IConfigurable, Closeable {
    @Setter(AccessLevel.NONE)
    private Map<String, AbstractAuditLogger> loggers = new ConcurrentHashMap<>();
    @Setter(AccessLevel.NONE)
    private Map<Class<? extends IKeyed>, AbstractAuditLogger> entityIndex = new HashMap<>();
    @Setter(AccessLevel.NONE)
    private AbstractAuditLogger defaultLogger = null;
    @Setter(AccessLevel.NONE)
    private DataStoreManager dataStoreManager;

    public AuditManager withDataStoreManager(@Nonnull DataStoreManager dataStoreManager) {
        this.dataStoreManager = dataStoreManager;
        return this;
    }

    public AbstractAuditLogger getLogger(@Nonnull String name) {
        return loggers.get(name);
    }

    public <T extends IKeyed> AuditRecord audit(@Nonnull EAuditType type,
                                                @Nonnull T entity,
                                                String changeDelta,
                                                @Nonnull Principal user) throws AuditException {
        AbstractAuditLogger logger = getLogger(entity.getClass());
        if (logger != null) {
            return logger.write(type, entity, entity.getClass(), changeDelta, user);
        }
        return null;
    }

    public <T extends IKeyed> AuditRecord audit(@Nonnull EAuditType type,
                                                @Nonnull T entity,
                                                String changeDelta,
                                                @Nonnull Principal user,
                                                @Nonnull IAuditSerDe serializer) throws AuditException {
        AbstractAuditLogger logger = getLogger(entity.getClass());
        if (logger != null) {
            return logger.write(type, entity, entity.getClass(), changeDelta,user, serializer);
        }
        return null;
    }

    public <T extends IKeyed> AuditRecord audit(@Nonnull String logger,
                                                @Nonnull EAuditType type,
                                                @Nonnull T entity,
                                                String changeDelta,
                                                @Nonnull Principal user) throws AuditException {
        if (loggers.containsKey(logger)) {
            return loggers.get(logger).write(type, entity, entity.getClass(), changeDelta, user);
        }
        return null;
    }

    public <T extends IKeyed> AuditRecord audit(@Nonnull String logger,
                                                @Nonnull EAuditType type,
                                                @Nonnull T entity,
                                                String changeDelta,
                                                @Nonnull IAuditSerDe serializer,
                                                @Nonnull Principal user) throws AuditException {
        if (loggers.containsKey(logger)) {
            return loggers.get(logger).write(type, entity, entity.getClass(), changeDelta, user, serializer);
        }
        return null;
    }

    public AbstractAuditLogger getLogger(Class<? extends IKeyed> type) {
        if (entityIndex.containsKey(type)) {
            return entityIndex.get(type);
        } else if (type.isAnnotationPresent(Audited.class)) {
            Audited a = type.getAnnotation(Audited.class);
            if (loggers.containsKey(a.logger())) {
                return loggers.get(a.logger());
            }
        }
        return defaultLogger;
    }

    /**
     * Configure this type instance.
     *
     * @param node - Handle to the configuration node.
     * @throws ConfigurationException
     */
    @Override
    public void configure(@Nonnull AbstractConfigNode node) throws ConfigurationException {
        Preconditions.checkArgument(node instanceof ConfigPathNode);
        Preconditions.checkArgument(dataStoreManager != null);
        try {
            AbstractConfigNode pnode = ConfigUtils.getPathNode(getClass(), (ConfigPathNode) node);
            if (pnode != null) {
                if (pnode instanceof ConfigPathNode) {
                    pnode = ConfigUtils.getPathNode(AbstractAuditLogger.class, (ConfigPathNode) pnode);
                    if (pnode instanceof ConfigPathNode) {
                        readLoggerConfig((ConfigPathNode) pnode);
                    }
                } else if (pnode instanceof ConfigListElementNode) {
                    List<ConfigElementNode> nodes = ((ConfigListElementNode) pnode).getValues();
                    if (nodes != null && !nodes.isEmpty()) {
                        for (ConfigElementNode en : nodes) {
                            readLoggerConfig((ConfigPathNode) en);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LogUtils.error(getClass(), t);
            throw new ConfigurationException(t);
        }
    }

    @SuppressWarnings("unchecked")
    private void readLoggerConfig(ConfigPathNode node) throws ConfigurationException {
        try {
            String cname = ConfigUtils.getClassAttribute(node);
            if (Strings.isNullOrEmpty(cname)) {
                throw new ConfigurationException(
                        String.format("Invalid Audit Logger configuration: Logger class not specified. [node=%s]",
                                node.getAbsolutePath()));
            }
            Class<? extends AbstractAuditLogger> cls = (Class<? extends AbstractAuditLogger>) Class.forName(cname);
            AbstractAuditLogger logger = cls.newInstance();
            logger.configure(node);
            AbstractDataStore dataStore = dataStoreManager.getDataStore(logger.dataStoreName(), logger.dataStoreType());
            if (dataStore == null) {
                throw new ConfigurationException(String.format("[logger=%s] Data Store not found. [name=%s][type=%s]",
                        logger.name(), logger.dataStoreName(), logger.dataStoreType().getCanonicalName()));
            }
            logger.withDataStore(dataStore);
            loggers.put(logger.name(), logger);
            if (logger.defaultLogger()) {
                defaultLogger = logger;
            }
            List<String> classes = logger.classes();
            if (classes != null && !classes.isEmpty()) {
                for (String c : classes) {
                    Class<? extends IKeyed> type = (Class<? extends IKeyed>) Class.forName(c);
                    entityIndex.put(type, logger);
                }
            }
            LogUtils.info(getClass(), String.format("Configured logger. [name=%s][type=%s][default=%s]",
                    logger.name(), logger.getClass().getCanonicalName(), logger.defaultLogger()));
        } catch (Exception ex) {
            throw new ConfigurationException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        if (!loggers.isEmpty()) {
            for (String key : loggers.keySet()) {
                loggers.get(key).close();
            }
            loggers.clear();
        }
    }

    private static final AuditManager __instance = new AuditManager();

    public static void init(@Nonnull AbstractConfigNode node) throws ConfigurationException {
        __instance.configure(node);
    }

    public static AuditManager get() {
        return __instance;
    }
}
