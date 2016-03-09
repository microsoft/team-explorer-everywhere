// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.checkinpolicies;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.core.memento.MementoException;
import com.microsoft.tfs.core.memento.XMLMemento;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * A {@link PolicyAnnotation} contains one or more {@link PolicyDefinition}s,
 * and can be serialized to and from a TFS annotation on a single team project.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class PolicyAnnotation {
    /**
     * The annotation name used in TFS. The annotation name was chosen for
     * Teampr1se 3.0, and is retained in Team Explorer Everywhere for
     * compatibility.
     */
    public static final String CHECKIN_POLICY_ANNOTATION_NAME = "TeampriseCheckinPolicies"; //$NON-NLS-1$

    /**
     * The single data version we read and write.
     */
    public static final int SUPPORTED_VERSION = 1;

    /**
     * The character set used when serializing to a TFS annotation.
     */
    private static final String ANNOTATION_CHARSET = "UTF-8"; //$NON-NLS-1$

    private final static String ANNOTATION_MEMENTO_NAME = "policy-annotation"; //$NON-NLS-1$
    private final static String VERSION_ATTRIBUTE_NAME = "version"; //$NON-NLS-1$

    private final static String POLICY_DEFINITION_MEMENTO_NAME = "policy-definition"; //$NON-NLS-1$

    private final PolicyDefinition[] definitions;

    /**
     * Creates an annotation that holds the given definitions. An annotation can
     * not contain zero definitions (the annotation should be deleted entirely
     * by higher-level code in this case).
     *
     * @param definitions
     *        an array of at least one definition (must not be <code>null</code>
     *        )
     */
    public PolicyAnnotation(final PolicyDefinition[] definitions) {
        Check.notNull(definitions, "definitions"); //$NON-NLS-1$
        Check.isTrue(definitions.length > 0, "definitions array cannot be empty"); //$NON-NLS-1$

        this.definitions = definitions;
    }

    /**
     * Reads a {@link PolicyAnnotation} from the given annotation value string.
     *
     * @param annotationValue
     *        the annotation value string to read from (must not be
     *        <code>null</code>)
     * @return the new {@link PolicyAnnotation} containing all
     *         {@link PolicyDefinition}s loaded from the annotation.
     * @throws PolicySerializationException
     *         if an error occurred reading the annotation value.
     */
    public static PolicyAnnotation fromAnnotation(final String annotationValue) throws PolicySerializationException {
        Check.notNull(annotationValue, "annotationValue"); //$NON-NLS-1$

        /*
         * The annotation value is XML parsable by XMLMemento. The root node is
         * simply our PolicyAnnotation memento.
         */
        XMLMemento annotationMemento;
        try {
            annotationMemento = XMLMemento.read(
                new ByteArrayInputStream(annotationValue.getBytes(ANNOTATION_CHARSET)),
                ANNOTATION_CHARSET);
        } catch (final MementoException e) {
            throw new PolicySerializationException("Memento exception", e); //$NON-NLS-1$
        } catch (final UnsupportedEncodingException e) {
            throw new PolicySerializationException("Encoding not supported", e); //$NON-NLS-1$
        }

        /*
         * Not all memento deserializers will check that they were given the
         * correct kind of memento (they should not care what the element was
         * named), but this is the root, so it's a worthwile check.
         */
        if (annotationMemento.getName() != ANNOTATION_MEMENTO_NAME) {
            throw new PolicySerializationException(
                MessageFormat.format(
                    "Got unexpected root element {0} instead of expected {1}", //$NON-NLS-1$
                    annotationMemento.getName(),
                    ANNOTATION_MEMENTO_NAME));
        }

        final Integer schemaVersion = annotationMemento.getInteger(VERSION_ATTRIBUTE_NAME);

        if (schemaVersion == null) {
            throw new PolicySerializationException("The policy annotation did not specify a schema version"); //$NON-NLS-1$
        }

        if (schemaVersion.intValue() != SUPPORTED_VERSION) {
            throw new PolicySerializationException(
                MessageFormat.format(
                    "Policy annotation data version {0} can not be read by this annotation serializer.", //$NON-NLS-1$
                    schemaVersion.toString()));
        }

        /*
         * Read all children that are definitions.
         */
        final Memento[] definitionMementos = annotationMemento.getChildren(POLICY_DEFINITION_MEMENTO_NAME);

        if (definitionMementos.length == 0) {
            throw new PolicySerializationException(
                "Read 0 policy definitions from the annotation. This annotation should be deleted because annotations cannot contain zero policy definitions."); //$NON-NLS-1$
        }

        final PolicyDefinition[] definitions = new PolicyDefinition[definitionMementos.length];
        for (int i = 0; i < definitionMementos.length; i++) {
            definitions[i] = PolicyDefinition.fromMemento(definitionMementos[i]);
        }

        return new PolicyAnnotation(definitions);
    }

    /**
     * Encodes this {@link PolicyAnnotation} into a string suitable for storage
     * in a TFS annotation value.
     *
     * @return a TFS annotation value containing this {@link PolicyAnnotation}'s
     *         data.
     */
    public String toAnnotationValue() {
        final Memento annotationMemento = new XMLMemento(ANNOTATION_MEMENTO_NAME);

        annotationMemento.putInteger(VERSION_ATTRIBUTE_NAME, SUPPORTED_VERSION);

        // Write all the children into new nodes.
        for (int i = 0; i < definitions.length; i++) {
            final Memento definitionMemento = annotationMemento.createChild(POLICY_DEFINITION_MEMENTO_NAME);
            definitions[i].toMemento(definitionMemento);
        }

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ((XMLMemento) annotationMemento).write(os, ANNOTATION_CHARSET);
            return os.toString(ANNOTATION_CHARSET);
        } catch (final IOException e) {
            throw new PolicySerializationException("Error serializing definition memento", e); //$NON-NLS-1$
        }
    }

    /**
     * @return the definitions defined in this annotation.
     */
    public PolicyDefinition[] getDefinitions() {
        return definitions;
    }
}
