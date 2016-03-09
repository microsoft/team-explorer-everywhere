// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.utils;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildAgent;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildController;
import com.microsoft.tfs.core.clients.build.internal.soapextensions.BuildServiceHost;

public class QueryResultHelper {
    public static Map<String, BuildController> match(
        final BuildServiceHost[] serviceHosts,
        final BuildController[] controllers,
        final BuildAgent[] agents) {
        final Map<String, BuildController> controllerMap = new HashMap<String, BuildController>();
        final Map<String, BuildServiceHost> serviceHostMap = new HashMap<String, BuildServiceHost>();

        for (final BuildServiceHost serviceHost : serviceHosts) {
            if (serviceHost != null) {
                serviceHostMap.put(serviceHost.getURI(), serviceHost);
            }
        }

        for (final BuildController controller : controllers) {
            if (controller != null) {
                if (serviceHostMap.containsKey(controller.getServiceHostURI())) {
                    final BuildServiceHost serviceHost = serviceHostMap.get(controller.getServiceHostURI());
                    serviceHost.setBuildController(controller);
                }

                controllerMap.put(controller.getURI(), controller);
            }
        }

        for (final BuildAgent agent : agents) {
            if (agent != null) {
                if (controllerMap.containsKey(agent.getControllerURI())) {
                    final BuildController controller = controllerMap.get(agent.getControllerURI());
                    agent.setController(controller);
                }

                if (serviceHostMap.containsKey(agent.getServiceHostURI())) {
                    final BuildServiceHost serviceHost = serviceHostMap.get(agent.getServiceHostURI());
                    serviceHost.addBuildAgent(agent);
                }
            }
        }

        return controllerMap;
    }
}
