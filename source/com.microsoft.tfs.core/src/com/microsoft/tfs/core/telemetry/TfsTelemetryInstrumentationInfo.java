// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.telemetry;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.StringUtil;

/**
 * Get Telemetry information
 * <p>
 * This class is for internal use only.
 *
 * @threadsafety thread-safe
 */
public abstract class TfsTelemetryInstrumentationInfo {

    private final static Log log = LogFactory.getLog(TfsTelemetryInstrumentationInfo.class);

    /**
     * This resource should contain all telemetry information.
     * <p>
     * There are two properties: telemetry.instrumentation.is_test_environment
     * telemetry.instrumentation.is_developer_mode
     * <p>
     * If "is_test_environment" resolves to false, "is_developer_mode" will be
     * ignored
     * <p>
     * Default to "is_test_environment" to false in case this file does not
     * exist
     */
    public static final String TELEMETRY_INSTRUMENTATION_PROPERTIES_RESOURCE =
        "/com.microsoft.tfs.core-telemetry.properties"; //$NON-NLS-1$

    private static final String TEE_PROD_KEY = "1c226251-b2f0-4f4d-b610-4a43af091919"; //$NON-NLS-1$
    private static final String TEE_TEST_KEY = "4478152a-35b4-4f8f-a8ad-a8c599c9b41f"; //$NON-NLS-1$
    private static final String CLC_PROD_KEY = "c8e8c8fa-2343-4d3a-89a5-7f0b6205de85"; //$NON-NLS-1$
    private static final String CLC_TEST_KEY = "de5cacf3-8c9a-4255-9338-9a7c60469b10"; //$NON-NLS-1$

    private static boolean isTestEnv;
    private static boolean isDeveloperMode;

    static {
        // final InputStream in =
        // TfsTelemetryInstrumentationInfo.class.getResourceAsStream(TELEMETRY_INSTRUMENTATION_PROPERTIES_RESOURCE);
        // Now we always use AI telemetry in the production mode.
        final InputStream in = null;
        initialize(in);
    }

    static void initialize(final InputStream in) {
        // Default to production environment with batch uploading
        isTestEnv = false;
        isDeveloperMode = false;

        if (in != null) {
            try {
                final Properties props = new Properties();
                try {
                    props.load(in);
                    final String isTestEnvProperty = props.getProperty("telemetry.instrumentation.is_test_environment"); //$NON-NLS-1$
                    final String isDeveloperModeProperty =
                        props.getProperty("telemetry.instrumentation.is_developer_mode"); //$NON-NLS-1$

                    // Default to production environment, all invalid inputs
                    // will be resolved as "false"
                    if (!StringUtil.isNullOrEmpty(isTestEnvProperty) && Boolean.parseBoolean(isTestEnvProperty)) {
                        isTestEnv = true;
                        if (!StringUtil.isNullOrEmpty(isDeveloperModeProperty)
                            && Boolean.parseBoolean(isDeveloperModeProperty)) {
                            isDeveloperMode = true;
                        }
                    }
                } catch (final IOException e) {
                    log.warn(MessageFormat.format(
                        "Unable to load property resource {0} with exception {1}", //$NON-NLS-1$
                        TELEMETRY_INSTRUMENTATION_PROPERTIES_RESOURCE,
                        e));
                    // suppressing exception
                }
            } finally {
                try {
                    in.close();
                } catch (final IOException e) {
                    log.warn(MessageFormat.format(
                        "Unable to close property resource {0} with exception {1}", //$NON-NLS-1$
                        TELEMETRY_INSTRUMENTATION_PROPERTIES_RESOURCE,
                        e));
                    // suppressing exception
                }
            }
        }
    }

    public static boolean isDeveloperMode() {
        return isDeveloperMode;
    }

    public static boolean isTestKey() {
        return isTestEnv;
    }

    public static String getTeeInstrumentationKey() {
        return isTestKey() ? TEE_TEST_KEY : TEE_PROD_KEY;
    }

    public static String getClcInstrumentationKey() {
        return isTestKey() ? CLC_TEST_KEY : CLC_PROD_KEY;
    }
}