// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.telemetry;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.context.ComponentContext;
import com.microsoft.applicationinsights.extensibility.context.ContextTagKeys;
import com.microsoft.applicationinsights.extensibility.context.DeviceContext;
import com.microsoft.applicationinsights.extensibility.context.UserContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.product.CoreVersionInfo;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.core.product.ProductName;
import com.microsoft.tfs.jni.RegistryKey;
import com.microsoft.tfs.jni.RegistryValue;
import com.microsoft.tfs.jni.RootKey;
import com.microsoft.tfs.jni.helpers.LocalHost;
import com.microsoft.tfs.util.ArrayUtils;
import com.microsoft.tfs.util.HashUtils;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

public class TfsTelemetryInitializer implements ContextInitializer {

    private static final String SQM_CLIENT_KEY_ROOT_PATH = "Software\\Microsoft\\SQMClient"; //$NON-NLS-1$
    private static final String SQM_USER_ID_VALUE_NAME = "UserId"; //$NON-NLS-1$
    private static final String DEFAULT_VERSION = "0"; //$NON-NLS-1$

    @Override
    public void initialize(final TelemetryContext context) {
        initializeInstrumentationKey(context);
        initializeProperties(context.getProperties());
        initializeUser(context.getUser());
        initializeComponent(context.getComponent());
        initializeDevice(context.getDevice());
        initializeTags(context.getTags());
    }

    private void initializeDevice(final DeviceContext device) {
        device.setOperatingSystem(getPlatformName());
        device.setOperatingSystemVersion(getPlatformVersion());
    }

    private void initializeInstrumentationKey(final TelemetryContext context) {
        if (ProductInformation.getCurrent() == ProductName.CLC) {
            context.setInstrumentationKey(TfsTelemetryInstrumentationInfo.getClcInstrumentationKey());
        } else if (ProductInformation.getCurrent() == ProductName.PLUGIN) {
            context.setInstrumentationKey(TfsTelemetryInstrumentationInfo.getTeeInstrumentationKey());
        }
    }

    private void initializeUser(final UserContext user) {
        user.setId(getUserId());
        user.setUserAgent(ProductInformation.getCurrent().getFamilyShortNameNOLOC());
    }

    private void initializeComponent(final ComponentContext component) {
        component.setVersion(getApplicationVersion());
    }

    private void initializeTags(final Map<String, String> tags) {
        tags.put(
            ContextTagKeys.getKeys().getApplicationId(),
            ProductInformation.getCurrent().getFamilyShortNameNOLOC());
        tags.put(ContextTagKeys.getKeys().getDeviceOS(), getPlatformName());
        tags.put(ContextTagKeys.getKeys().getDeviceOSVersion(), getPlatformVersion());
    }

    private void initializeProperties(final Map<String, String> properties) {
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_USER_ID, getUserId());

        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_MAJOR_VERSION, getApplicationMajorVersion());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_MINOR_VERSION, getApplicationMinorVersion());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_SERVICEPACK, getApplicationServiceVersion());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_BUILD_NUMBER, getApplicationBuildVersion());

        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_EXE_NAME, getExeName());

        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_PROCESSOR_ARCHITECTURE, getProcessorArchitecture());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_LOCALE_NAME, getLocaleName());

        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_OS_MAJOR_VERSION, getPlatformMajorVersion());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_OS_MINOR_VERSION, getPlatformMinorVersion());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_OS_NAME, getPlatformName());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_OS_SHORT_NAME, getPlatformShortName());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_OS_FULL_NAME, getPlatformFullName());

        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_JAVA_RUNTIME_NAME, getJavaName());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_JAVA_RUNTIME_VERSION, getJavaVersion());

        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_FRAMEWORK_NAME, getFrameworkName());
        properties.put(TfsTelemetryConstants.CONTEXT_PROPERTY_FRAMEWORK_VERSION, getFrameworkVersion());
    }

    private String getSystemProperty(final String propertyName) {
        return System.getProperty(propertyName, StringUtil.EMPTY);
    }

    private String getUserId() {
        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            final RegistryKey sqmClientRegistryKey =
                new RegistryKey(RootKey.HKEY_CURRENT_USER, SQM_CLIENT_KEY_ROOT_PATH);
            if (sqmClientRegistryKey != null) {
                final RegistryValue userIdRegistryValue = sqmClientRegistryKey.getValue(SQM_USER_ID_VALUE_NAME);
                if (userIdRegistryValue != null) {
                    return userIdRegistryValue.getStringValue();
                }
            }

        }

        final String hostName = LocalHost.getShortName();
        final String userName = getSystemProperty("user.name"); //$NON-NLS-1$
        final String fakeUserId = MessageFormat.format("{0}@{1}", userName, hostName); //$NON-NLS-1$

        final byte[] hash = HashUtils.hashString(fakeUserId, "UTF-8", HashUtils.ALGORITHM_SHA_1); //$NON-NLS-1$

        return ArrayUtils.byteArrayToHexString(hash);
    }

    private String getExeName() {
        final String exeFile = System.getProperty("eclipse.launcher"); //$NON-NLS-1$
        if (StringUtil.isNullOrEmpty(exeFile)) {
            return ProductInformation.getCurrent().getProductShortNameNOLOC();
        } else {
            return LocalPath.getFileName(exeFile);
        }

    }

    private String getPlatformName() {
        return getSystemProperty("os.name"); //$NON-NLS-1$
    }

    private String getPlatformShortName() {
        final String osName = getSystemProperty("os.name"); //$NON-NLS-1$
        final String shortName;

        if (StringUtil.isNullOrEmpty(osName)) {
            shortName = StringUtil.EMPTY;
        } else {
            final String[] nameParts = osName.trim().split(" ", 2); //$NON-NLS-1$
            shortName = nameParts[0];
        }

        return shortName;
    }

    private String getPlatformVersion() {
        return getSystemProperty("os.version"); //$NON-NLS-1$
    }

    private String getPlatformMajorVersion() {
        final String osVersion = getSystemProperty("os.version"); //$NON-NLS-1$

        if (osVersion.indexOf('.') < 0) {
            return osVersion;
        } else {
            return osVersion.split("\\.", 2)[0]; //$NON-NLS-1$
        }
    }

    private String getPlatformMinorVersion() {
        final String osVersion = getSystemProperty("os.version"); //$NON-NLS-1$

        if (osVersion.indexOf('.') < 0) {
            return StringUtil.EMPTY;
        } else {
            return osVersion.split("\\.", 2)[1]; //$NON-NLS-1$
        }
    }

    private String getPlatformFullName() {
        return MessageFormat.format("{0} ({1})", getSystemProperty("os.name"), getSystemProperty("os.version")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private String getProcessorArchitecture() {
        return getSystemProperty("os.arch").toUpperCase(); //$NON-NLS-1$
    }

    private String getLocaleName() {
        return Locale.getDefault().getDisplayName();
    }

    private String getJavaName() {
        return getSystemProperty("java.runtime.name"); //$NON-NLS-1$
    }

    private String getJavaVersion() {
        return getSystemProperty("java.version"); //$NON-NLS-1$
    }

    private String getFrameworkName() {
        return getSystemProperty("eclipse.launcher.name"); //$NON-NLS-1$
    }

    private String getFrameworkVersion() {
        String version = getSystemProperty("eclipse.buildId"); //$NON-NLS-1$
        if (!StringUtil.isNullOrEmpty(version)) {
            return version;
        } else {
            final String osgiVersion = getSystemProperty("osgi.framework.version"); //$NON-NLS-1$
            if (osgiVersion != null && osgiVersion.length() > 4) {
                version = osgiVersion.substring(0, 5);

                if (version.startsWith("3.9.")) { //$NON-NLS-1$
                    return version.replace("3.9.", "4.3."); //$NON-NLS-1$ //$NON-NLS-2$
                } else if (version.startsWith("3.8.")) { //$NON-NLS-1$
                    return version.replace("3.8.", "4.2."); //$NON-NLS-1$ //$NON-NLS-2$
                } else {
                    return version;
                }
            }

            return osgiVersion;
        }
    }

    private String getApplicationMajorVersion() {
        final String v = CoreVersionInfo.getMajorVersion();
        return !StringUtil.isNullOrEmpty(v) ? v : DEFAULT_VERSION;
    }

    private String getApplicationMinorVersion() {
        final String v = CoreVersionInfo.getMinorVersion();
        return !StringUtil.isNullOrEmpty(v) ? v : DEFAULT_VERSION;
    }

    private String getApplicationServiceVersion() {
        final String v = CoreVersionInfo.getServiceVersion();
        return !StringUtil.isNullOrEmpty(v) ? v : DEFAULT_VERSION;
    }

    private String getApplicationBuildVersion() {
        final String v = CoreVersionInfo.getBuildVersion();
        return !StringUtil.isNullOrEmpty(v) ? v : DEFAULT_VERSION;
    }

    private String getApplicationVersion() {
        return StringUtil.join(new String[] {
            getApplicationMajorVersion(),
            getApplicationMinorVersion(),
            getApplicationServiceVersion(),
            getApplicationBuildVersion()
        }, "."); //$NON-NLS-1$
    }
}
