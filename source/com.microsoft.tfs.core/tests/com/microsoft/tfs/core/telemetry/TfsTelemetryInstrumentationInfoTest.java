// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.telemetry;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

public class TfsTelemetryInstrumentationInfoTest extends TestCase {

    private static final String TEE_PROD_KEY = "1c226251-b2f0-4f4d-b610-4a43af091919"; //$NON-NLS-1$
    private static final String TEE_TEST_KEY = "4478152a-35b4-4f8f-a8ad-a8c599c9b41f"; //$NON-NLS-1$
    private static final String CLC_PROD_KEY = "c8e8c8fa-2343-4d3a-89a5-7f0b6205de85"; //$NON-NLS-1$
    private static final String CLC_TEST_KEY = "de5cacf3-8c9a-4255-9338-9a7c60469b10"; //$NON-NLS-1$

    private static final String PROPERTY_FILE_FORMAT =
        "telemetry.instrumentation.is_test_environment=%s%ntelemetry.instrumentation.is_developer_mode=%s"; //$NON-NLS-1$

    public void testEnvShouldHonorDeveloperModeFlag() {
        final String testEnvWithDeveloperMode = String.format(PROPERTY_FILE_FORMAT, true, true);
        TfsTelemetryInstrumentationInfo.initialize(new ByteArrayInputStream(testEnvWithDeveloperMode.getBytes()));

        // flag is true
        assertTrue(TfsTelemetryInstrumentationInfo.isDeveloperMode());

        // return test key in test env
        assertEquals(TfsTelemetryInstrumentationInfo.getClcInstrumentationKey(), CLC_TEST_KEY);
        assertEquals(TfsTelemetryInstrumentationInfo.getTeeInstrumentationKey(), TEE_TEST_KEY);
    }

    public void testEnvShouldHonorDeveloperModeFlag2() {
        final String testEnvWithDeveloperMode = String.format(PROPERTY_FILE_FORMAT, true, false);
        TfsTelemetryInstrumentationInfo.initialize(new ByteArrayInputStream(testEnvWithDeveloperMode.getBytes()));

        // flag is false
        assertFalse(TfsTelemetryInstrumentationInfo.isDeveloperMode());

        // return test key in test env
        assertEquals(TfsTelemetryInstrumentationInfo.getClcInstrumentationKey(), CLC_TEST_KEY);
        assertEquals(TfsTelemetryInstrumentationInfo.getTeeInstrumentationKey(), TEE_TEST_KEY);
    }

    public void testProdModeShouldIgnoreDeveloperMode() {
        final String testEnvWithDeveloperMode = String.format(PROPERTY_FILE_FORMAT, false, true);
        TfsTelemetryInstrumentationInfo.initialize(new ByteArrayInputStream(testEnvWithDeveloperMode.getBytes()));

        // ignored developer mode flag in prod env
        assertFalse(TfsTelemetryInstrumentationInfo.isDeveloperMode());

        // return prod key in prod env
        assertEquals(TfsTelemetryInstrumentationInfo.getClcInstrumentationKey(), CLC_PROD_KEY);
        assertEquals(TfsTelemetryInstrumentationInfo.getTeeInstrumentationKey(), TEE_PROD_KEY);
    }

    public void testProdModeShouldIgnoreDeveloperMode2() {
        // developer mode flag should be ignored
        final String testEnvWithDeveloperMode = String.format(PROPERTY_FILE_FORMAT, false, false);
        TfsTelemetryInstrumentationInfo.initialize(new ByteArrayInputStream(testEnvWithDeveloperMode.getBytes()));

        // ignored developer mode flag in prod env
        assertFalse(TfsTelemetryInstrumentationInfo.isDeveloperMode());

        // return prod key in prod env
        assertEquals(TfsTelemetryInstrumentationInfo.getClcInstrumentationKey(), CLC_PROD_KEY);
        assertEquals(TfsTelemetryInstrumentationInfo.getTeeInstrumentationKey(), TEE_PROD_KEY);
    }

    public void testNullPropertyShouldDefaultToProd() {
        // should got IOException with null InputStream
        TfsTelemetryInstrumentationInfo.initialize(null);

        // default to non-developer mode
        assertFalse(TfsTelemetryInstrumentationInfo.isDeveloperMode());

        // return prod key in prod env
        assertEquals(TfsTelemetryInstrumentationInfo.getClcInstrumentationKey(), CLC_PROD_KEY);
        assertEquals(TfsTelemetryInstrumentationInfo.getTeeInstrumentationKey(), TEE_PROD_KEY);
    }

    public void testInvalidPropertyShouldDefaultToProd() {
        // should got empty strings for the properties
        TfsTelemetryInstrumentationInfo.initialize(new ByteArrayInputStream("abc=def".getBytes())); //$NON-NLS-1$

        // default to non-developer mode
        assertFalse(TfsTelemetryInstrumentationInfo.isDeveloperMode());

        // return prod key in prod env
        assertEquals(TfsTelemetryInstrumentationInfo.getClcInstrumentationKey(), CLC_PROD_KEY);
        assertEquals(TfsTelemetryInstrumentationInfo.getTeeInstrumentationKey(), TEE_PROD_KEY);
    }
}
