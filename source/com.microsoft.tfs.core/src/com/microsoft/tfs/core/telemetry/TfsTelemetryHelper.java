// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.telemetry;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.logger.InternalLogger.LoggerOutputType;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.SessionState;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.product.CoreVersionInfo;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.core.product.ProductName;
import com.microsoft.tfs.util.GUID;

public class TfsTelemetryHelper {
    private static final Log log = LogFactory.getLog(TfsTelemetryHelper.class);

    private static TelemetryClient aiClient;

    static {
        final Map<String, String> loggerData = new HashMap<String, String>();

        loggerData.put("Level", InternalLogger.LoggingLevel.ERROR.toString()); //$NON-NLS-1$
        loggerData.put("UniquePrefix", "ai-log"); //$NON-NLS-1$ //$NON-NLS-2$
        loggerData.put("BaseFolder", "AppInsights"); //$NON-NLS-1$ //$NON-NLS-2$

        InternalLogger.INSTANCE.initialize(LoggerOutputType.FILE.toString(), loggerData);

        TelemetryConfiguration.getActive().getContextInitializers().add(new TfsTelemetryInitializer());
        TelemetryConfiguration.getActive().getChannel().setDeveloperMode(
            TfsTelemetryInstrumentationInfo.isDeveloperMode());
    }

    public synchronized static TelemetryClient getTelemetryClient() {
        if (aiClient == null) {
            log.info(ProductInformation.getCurrent().getProductFullNameNOLOC()
                + " v." //$NON-NLS-1$
                + CoreVersionInfo.getVersion());

            if (ProductInformation.getCurrent() == ProductName.SDK) {
                log.info("AppInsights telemetry disabled for SDK product"); //$NON-NLS-1$

                final TelemetryConfiguration disabledConfiguration = TelemetryConfiguration.getActive();
                disabledConfiguration.setTrackingIsDisabled(true);

                aiClient = new TelemetryClient(disabledConfiguration);
            } else {
                log.info("AppInsights telemetry initialized"); //$NON-NLS-1$
                log.info(
                    MessageFormat.format("    Developer Mode: {0}", TfsTelemetryInstrumentationInfo.isDeveloperMode())); //$NON-NLS-1$
                log.info(MessageFormat.format(
                    "    Production Environment: {0}", //$NON-NLS-1$
                    !TfsTelemetryInstrumentationInfo.isTestKey()));

                aiClient = new TelemetryClient();
            }

            aiClient.getContext().getSession().setId(UUID.randomUUID().toString());
        }

        return aiClient;
    }

    public static void sendMetric(final String name, final double value) {
        getTelemetryClient().trackMetric(name, value);
    }

    public static void sendEvent(final String name) {
        getTelemetryClient().trackEvent(name, null, null);
    }

    public static void sendEvent(final String name, final Map<String, String> properties) {
        getTelemetryClient().trackEvent(name, properties, null);
    }

    public static void sendPageView(final String pageName) {
        getTelemetryClient().trackPageView(pageName);
    }

    public static void sendPageView(final String pageName, final Map<String, String> properties) {
        final PageViewTelemetry telemetry = new PageViewTelemetry(pageName);

        if (properties != null) {
            telemetry.getProperties().putAll(properties);
        }

        getTelemetryClient().trackPageView(telemetry);
    }

    public static void sendSessionBegins() {
        getTelemetryClient().trackSessionState(SessionState.Start);
    }

    public static void sendSessionEnds() {
        getTelemetryClient().trackSessionState(SessionState.End);
    }

    public static void sendException(final Exception exception) {
        getTelemetryClient().trackException(exception);
    }

    public static String getName(final Object o) {
        if (o == null) {
            return "**UNKNOWN**"; //$NON-NLS-1$
        } else {
            return o.getClass().getSimpleName();
        }
    }

    public static void addContextProperties(final Map<String, String> properties, final TFSConnection connection) {
        if (connection != null && connection.hasAuthenticated()) {
            final boolean isHosted = connection.isHosted();
            properties.put(TfsTelemetryConstants.SHARED_PROPERTY_IS_HOSTED, Boolean.toString(isHosted));

            final GUID connectionId = connection.getInstanceID();
            properties.put(TfsTelemetryConstants.SHARED_PROPERTY_SERVER_ID, connectionId.toString());

            if (connection instanceof TFSTeamProjectCollection) {
                final TFSTeamProjectCollection collection = (TFSTeamProjectCollection) connection;
                final TFSConfigurationServer configurationServer = collection.getConfigurationServer();

                if (configurationServer != null) {
                    final GUID serverId = configurationServer.getInstanceID();

                    properties.put(TfsTelemetryConstants.SHARED_PROPERTY_SERVER_ID, serverId.toString());
                    properties.put(TfsTelemetryConstants.SHARED_PROPERTY_COLLECTION_ID, connectionId.toString());
                }
            }
        }
    }
}
