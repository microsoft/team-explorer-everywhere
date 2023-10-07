// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.logging.adapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.spi.AbstractLoggerAdapter;
import org.apache.logging.log4j.spi.LoggerContext;

public class TEELogFactory extends LogFactory {

    private final AbstractLoggerAdapter<Log> adapter = new AbstractLoggerAdapter<Log>() {
        @Override
        protected LoggerContext getContext() {
            return getContext(TEELogFactory.this.getClass()); // unify context
        }
        @Override
        protected Log newLogger(final String name, final LoggerContext context) {
            return new LoggerAdapter(context.getLogger(name));
        }
    };

    @Override
    @SuppressWarnings("rawtypes")
    public Log getInstance(final Class clazz) throws LogConfigurationException {
        return getInstance(clazz.getName());
    }

    @Override
    public Log getInstance(final String name) throws LogConfigurationException {
        return adapter.getLogger(name);
    }

    @Override
    public void release() {
        adapter.close();
    }

    //--------------------------------------------------------------------------

    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

    @Override
    public String[] getAttributeNames() {
        return attributes.keySet().toArray(new String[0]);
    }

    @Override
    public Object getAttribute(final String name) {
        return attributes.get(name);
    }

    @Override
    public void removeAttribute(final String name) {
        attributes.remove(name);
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        if (value != null) {
            attributes.put(name, value);
        } else {
            removeAttribute(name);
        }
    }
}
