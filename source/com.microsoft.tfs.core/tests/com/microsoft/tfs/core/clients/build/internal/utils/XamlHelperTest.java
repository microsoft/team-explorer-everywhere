// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.internal.utils;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import junit.framework.TestCase;

public class XamlHelperTest extends TestCase {
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    public void testSave() throws Exception {
        final String path = "$/TEE/main/build/teambuild" + (char) 0xc883; //$NON-NLS-1$

        final String expected =
            "<Dictionary x:TypeArguments=\"x:String, x:Object\" xmlns=\"clr-namespace:System.Collections.Generic;assembly=mscorlib\" xmlns:x=\"http://schemas.microsoft.com/winfx/2006/xaml\">" //$NON-NLS-1$
                + NEWLINE
                + "    <x:String x:Key=\"ConfigurationFolderPath\" xml:space=\"preserve\">" //$NON-NLS-1$
                + path
                + "</x:String>" //$NON-NLS-1$
                + NEWLINE
                + "</Dictionary>"; //$NON-NLS-1$

        final Properties properties = new Properties();
        properties.put("ConfigurationFolderPath", path); //$NON-NLS-1$

        final String actual = XamlHelper.save(properties);

        assertEquals(expected, actual);
    }

    public void testSerializeDeserialize() throws Exception {
        final Properties properties = new Properties();
        properties.put("ConfigurationFolderPath", "$/TEE/main/build/teambuild"); //$NON-NLS-1$ //$NON-NLS-2$
        properties.put("MSBuildArguments", "/p:IncrementalBuild=true"); //$NON-NLS-1$ //$NON-NLS-2$

        final String xaml = XamlHelper.save(properties);
        final Properties newProperties = XamlHelper.loadPartial(xaml);

        assertEquals(properties.size(), newProperties.size());

        for (final Iterator it = properties.entrySet().iterator(); it.hasNext();) {
            final Entry entry = (Entry) it.next();

            final String origKey = (String) entry.getKey();
            newProperties.containsKey(origKey);

            final String origValue = (String) entry.getValue();
            final String currValue = newProperties.getProperty(origKey);
            assertEquals(origValue, currValue);
        }
    }

    public void testUpdateProperties() throws Exception {

        /*
         * IBM's Java libraries format the XML differently from Sun's
         * (newlines?). Skipping this test for non-Sun, non-Oracle (post Sun
         * acquisition) because it's not that critical.
         */
        if (System.getProperty("java.vm.vendor").contains("Sun") == false //$NON-NLS-1$//$NON-NLS-2$
            && System.getProperty("java.vm.vendor").contains("Oracle") == false) //$NON-NLS-1$//$NON-NLS-2$
        {
            return;
        }

        final Properties properties = new Properties();
        properties.put("ConfigurationFolderPath", "$/TEE/main/build/teambuild"); //$NON-NLS-1$ //$NON-NLS-2$
        properties.put("MSBuildArguments", "/p:IncrementalBuild=true"); //$NON-NLS-1$ //$NON-NLS-2$

        final String startXaml =
            "<Dictionary x:TypeArguments=\"x:String, x:Object\" xmlns=\"clr-namespace:System.Collections.Generic;assembly=mscorlib\" xmlns:mtbwa=\"clr-namespace:Microsoft.TeamFoundation.Build.Workflow.Activities;assembly=Microsoft.TeamFoundation.Build.Workflow\" xmlns:x=\"http://schemas.microsoft.com/winfx/2006/xaml\">" //$NON-NLS-1$
                + NEWLINE
                + "  <mtbwa:BuildSettings x:Key=\"BuildSettings\" ProjectsToBuild=\"$/TEE/PowerTools/main/TeamBuildExtensions/TeamBuildExtensions.sln,$/tee/PowerTools/main/TeamBuildExtensions/setup/BuildExtensionsSetup.sln\">" //$NON-NLS-1$
                + NEWLINE
                + "    <mtbwa:BuildSettings.PlatformConfigurations>" //$NON-NLS-1$
                + NEWLINE
                + "      <mtbwa:PlatformConfigurationList Capacity=\"1\">" //$NON-NLS-1$
                + NEWLINE
                + "        <mtbwa:PlatformConfiguration Configuration=\"Release\" Platform=\"Any CPU\" />" //$NON-NLS-1$
                + NEWLINE
                + "      </mtbwa:PlatformConfigurationList>" //$NON-NLS-1$
                + NEWLINE
                + "    </mtbwa:BuildSettings.PlatformConfigurations>" //$NON-NLS-1$
                + NEWLINE
                + "  </mtbwa:BuildSettings>" //$NON-NLS-1$
                + NEWLINE
                + "  <mtbwa:TestSpecList x:Key=\"TestSpecs\" Capacity=\"4\">" //$NON-NLS-1$
                + NEWLINE
                + "    <mtbwa:TestAssemblySpec CategoryFilter=\"{x:Null}\" MSTestCommandLineArgs=\"{x:Null}\" AssemblyFileSpec=\"**\\*test*.dll\" TestSettingsFileName=\"$/TEE/PowerTools/main/TeamBuildExtensions/BuildServer.testsettings\" />" //$NON-NLS-1$
                + NEWLINE
                + "  </mtbwa:TestSpecList>" //$NON-NLS-1$
                + NEWLINE
                + "  <x:String x:Key=\"BuildNumberFormat\">$(BuildDefinitionName)_V2.0.0$(Rev:.r)</x:String>" //$NON-NLS-1$
                + NEWLINE
                + "  <x:String x:Key=\"MSBuildArguments\">/p:UsingTeamBuild=true</x:String>" //$NON-NLS-1$
                + NEWLINE
                + "</Dictionary>" //$NON-NLS-1$
                + NEWLINE;

        final String editedXaml = XamlHelper.updateProperties(startXaml, properties);

        final String expectedXaml =
            "<Dictionary xmlns=\"clr-namespace:System.Collections.Generic;assembly=mscorlib\" xmlns:mtbwa=\"clr-namespace:Microsoft.TeamFoundation.Build.Workflow.Activities;assembly=Microsoft.TeamFoundation.Build.Workflow\" xmlns:x=\"http://schemas.microsoft.com/winfx/2006/xaml\" x:TypeArguments=\"x:String, x:Object\">" //$NON-NLS-1$
                + NEWLINE
                + "  <mtbwa:BuildSettings ProjectsToBuild=\"$/TEE/PowerTools/main/TeamBuildExtensions/TeamBuildExtensions.sln,$/tee/PowerTools/main/TeamBuildExtensions/setup/BuildExtensionsSetup.sln\" x:Key=\"BuildSettings\">" //$NON-NLS-1$
                + NEWLINE
                + "    <mtbwa:BuildSettings.PlatformConfigurations>" //$NON-NLS-1$
                + NEWLINE
                + "      <mtbwa:PlatformConfigurationList Capacity=\"1\">" //$NON-NLS-1$
                + NEWLINE
                + "        <mtbwa:PlatformConfiguration Configuration=\"Release\" Platform=\"Any CPU\"/>" //$NON-NLS-1$
                + NEWLINE
                + "      </mtbwa:PlatformConfigurationList>" //$NON-NLS-1$
                + NEWLINE
                + "    </mtbwa:BuildSettings.PlatformConfigurations>" //$NON-NLS-1$
                + NEWLINE
                + "  </mtbwa:BuildSettings>" //$NON-NLS-1$
                + NEWLINE
                + "  <mtbwa:TestSpecList Capacity=\"4\" x:Key=\"TestSpecs\">" //$NON-NLS-1$
                + NEWLINE
                + "    <mtbwa:TestAssemblySpec AssemblyFileSpec=\"**\\*test*.dll\" CategoryFilter=\"{x:Null}\" MSTestCommandLineArgs=\"{x:Null}\" TestSettingsFileName=\"$/TEE/PowerTools/main/TeamBuildExtensions/BuildServer.testsettings\"/>" //$NON-NLS-1$
                + NEWLINE
                + "  </mtbwa:TestSpecList>" //$NON-NLS-1$
                + NEWLINE
                + "  <x:String x:Key=\"BuildNumberFormat\">$(BuildDefinitionName)_V2.0.0$(Rev:.r)</x:String>" //$NON-NLS-1$
                + NEWLINE
                + "  <x:String x:Key=\"MSBuildArguments\">/p:IncrementalBuild=true</x:String>" //$NON-NLS-1$
                + NEWLINE
                + "<x:String x:Key=\"ConfigurationFolderPath\" xml:space=\"preserve\">$/TEE/main/build/teambuild</x:String>" //$NON-NLS-1$
                + NEWLINE
                + "</Dictionary>"; //$NON-NLS-1$

        assertEquals(expectedXaml, editedXaml);
    }
}
