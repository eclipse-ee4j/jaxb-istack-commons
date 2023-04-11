/*
 * Copyright (c) 1997, 2023 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.istack;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.lang.ref.WeakReference;

/**
 * Pool of reusable objects that are indistinguishable from each other,
 * such as JAXB marshallers.
 *
 * @author Kohsuke Kawaguchi
 * @param <T> type
 */
public interface Pool<T> {

    /**
     * Gets a new object from the pool.
     *
     * <p>
     * If no object is available in the pool, this method creates a new one.
     * @return an object from the pool
     */
    @NotNull T take();

    /**
     * Returns an object back to the pool.
     * @param t object to put back to pool
     */
    void recycle(@NotNull T t);

    /**
     * Default implementation that uses {@link ConcurrentLinkedQueue}
     * as the data store.
     *
     * <h2>Note for Implementors</h2>
     * <p>
     * Don't rely on the fact that this class extends from {@link ConcurrentLinkedQueue}.
     * @param <T> type
     */
    abstract class Impl<T> implements Pool<T> {

        private volatile WeakReference<ConcurrentLinkedQueue<T>> queue;

        /**
         * Create new Impl
         */
        protected Impl() {
        }

        /**
         * Gets a new object from the pool.
         *
         * <p>
         * If no object is available in the pool, this method creates a new one.
         *
         * @return
         *      always non-null.
         */
        @Override
        public final @NotNull T take() {
            T t = getQueue().poll();
            if(t==null) {
                return create();
            }
            return t;
        }

        /**
         * Returns an object back to the pool.
         * @param t object to put back to the pool
         */
        @Override
        public final void recycle(T t) {
            getQueue().offer(t);
        }

        private ConcurrentLinkedQueue<T> getQueue() {
            WeakReference<ConcurrentLinkedQueue<T>> q = queue;
            if (q != null) {
                ConcurrentLinkedQueue<T> d = q.get();
                if (d != null) {
                    return d;
                }
            }
            // overwrite the queue
            ConcurrentLinkedQueue<T> d = new ConcurrentLinkedQueue<>();
            queue = new WeakReference<>(d);

            return d;
        }

        /**
         * Creates a new instance of object.
         *
         * <p>
         * This method is used when someone wants to
         * {@link #take() take} an object from an empty pool.
         *
         * <p>
         * Also note that multiple threads may call this method
         * concurrently.
         * @return an object from an empty pool
         */
        protected abstract @NotNull T create();
    }
}
