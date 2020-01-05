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

package com.codekutter.common.locking;

import com.codekutter.common.model.LockId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@Accessors(fluent = true)
public abstract class DistributedLock extends ReentrantLock implements Closeable {
    private static final long DEFAULT_LOCK_TIMEOUT = 15 * 60 * 1000; // 15 mins.

    private String instanceId;
    private LockId id;
    private long threadId;
    private long lockGetTimeout = DEFAULT_LOCK_TIMEOUT;
    private long lockExpiryTimeout = -1;

    public DistributedLock(@Nonnull String namespace, @Nonnull String name) {
        id = new LockId();
        id.setNamespace(namespace);
        id.setName(name);

        instanceId = UUID.randomUUID().toString();
        threadId = Thread.currentThread().getId();
    }

    public DistributedLock(@Nonnull LockId id) {
        this.id = id;

        instanceId = UUID.randomUUID().toString();
        threadId = Thread.currentThread().getId();
    }

    protected void checkThread() throws LockException {
        if (threadId != Thread.currentThread().getId()) {
            throw new LockException(String.format("Lock not owned by current thread. [owner thread id=%d]", threadId));
        }
    }

    public DistributedLock withLockGetTimeout(long lockGetTimeout) {
        if (lockGetTimeout > 0)
            this.lockGetTimeout = lockGetTimeout;
        return this;
    }

    public DistributedLock withLockExpiryTimeout(long lockExpiryTimeout) {
        if (lockExpiryTimeout > 0)
            this.lockExpiryTimeout = lockExpiryTimeout;
        return this;
    }

    @JsonIgnore
    public String getKey() {
        return String.format("%s::%s", id.getNamespace(), id.getName());
    }

    @Override
    public boolean isHeldByCurrentThread() {
        if (super.isHeldByCurrentThread()) {
            return (threadId == Thread.currentThread().getId());
        }
        return false;
    }
}
