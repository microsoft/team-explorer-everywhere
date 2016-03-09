// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.teamfoundation.distributedtask.webapi.model;

import java.io.IOException;
import java.text.MessageFormat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.microsoft.tfs.core.Messages;

/**
 *
 *
 * @threadsafety unknown
 */
public class JobEventSerializer extends JsonSerializer<JobEvent> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void serialize(final JobEvent value, final JsonGenerator writer, final SerializerProvider provider)
        throws IOException,
            JsonProcessingException {
        if (value instanceof JobAssignedEvent) {
            final JobAssignedEvent v = (JobAssignedEvent) value;
            writer.writeObject(v);
        } else if (value instanceof JobCompletedEvent) {
            final JobCompletedEvent v = (JobCompletedEvent) value;
            writer.writeObject(v);
        } else {
            throw new UnsupportedOperationException(
                MessageFormat.format(
                    Messages.getString("Serializer.NotImplementedFormat"), //$NON-NLS-1$
                    value.getClass().getName()));
        }
    }

}
