// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.internal.db.dao;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.internal.db.ConnectionPool;
import com.microsoft.tfs.core.internal.db.DBConnection;

public abstract class BaseDAOFactory {
    private final Map implementationClasses = new HashMap();
    private final Map daoObjects = new HashMap();
    private boolean initialized = false;
    private final ConnectionPool connectionPool;

    protected abstract void doAddImplementationMappings();

    protected abstract void doInitializeDAOImplementation(Object implementaion);

    public BaseDAOFactory(final ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public synchronized Object getDAO(final Class daoInterfaceClass) {
        if (daoObjects.containsKey(daoInterfaceClass)) {
            return daoObjects.get(daoInterfaceClass);
        }

        if (!initialized) {
            initialize();
        }

        final Class implementationClass = (Class) implementationClasses.get(daoInterfaceClass);
        if (implementationClass == null) {
            throw new IllegalArgumentException(
                MessageFormat.format(
                    "the dao interface [{0}] does not have an implementation registered with this factory", //$NON-NLS-1$
                    daoInterfaceClass.getName()));
        }

        InitializableDAO implementaion;
        try {
            implementaion = (InitializableDAO) instantiate(implementationClass);
        } catch (final Exception e) {
            throw new RuntimeException(MessageFormat.format(
                "unable to instantiate dao implementation [{0}]", //$NON-NLS-1$
                implementationClass.getName()), e);
        }

        final TLSConnectionSource connectionSource = new TLSConnectionSource();

        implementaion.initialize(connectionSource);
        doInitializeDAOImplementation(implementaion);

        final Object dao = Proxy.newProxyInstance(daoInterfaceClass.getClassLoader(), new Class[] {
            daoInterfaceClass
        }, new DAOProxy(implementaion, connectionSource, connectionPool));

        daoObjects.put(daoInterfaceClass, dao);

        return dao;
    }

    protected Object instantiate(final Class cls) throws InstantiationException, IllegalAccessException {
        return cls.newInstance();
    }

    private void initialize() {
        initialized = true;
        doAddImplementationMappings();
    }

    protected void addImplementation(final Class interfaceClass, final Class implementationClass) {
        implementationClasses.put(interfaceClass, implementationClass);
    }

    private static class DAOProxy implements InvocationHandler {
        private final Object delegate;
        private final TLSConnectionSource connectionSource;
        private final ConnectionPool connectionPool;

        public DAOProxy(
            final Object delegate,
            final TLSConnectionSource connectionSource,
            final ConnectionPool connectionPool) {
            this.delegate = delegate;
            this.connectionSource = connectionSource;
            this.connectionPool = connectionPool;
        }

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            final DBConnection connection = connectionPool.getConnection();
            connectionSource.setConnection(connection);
            try {
                return method.invoke(delegate, args);
            } catch (final InvocationTargetException ex) {
                throw ex.getCause();
            } finally {
                connectionSource.clearConnection();
                connectionPool.releaseConnection(connection);
            }
        }
    }

    private static class TLSConnectionSource implements DBConnectionSource {
        private final ThreadLocal connectionTLS = new ThreadLocal();

        public void setConnection(final DBConnection connection) {
            connectionTLS.set(connection);
        }

        public void clearConnection() {
            connectionTLS.set(null);
        }

        @Override
        public DBConnection getConnection() {
            return (DBConnection) connectionTLS.get();
        }
    }
}
