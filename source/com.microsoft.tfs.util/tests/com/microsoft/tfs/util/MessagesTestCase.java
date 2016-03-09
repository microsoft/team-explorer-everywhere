// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public abstract class MessagesTestCase extends TestCase {
    private final Hashtable<String, ArrayList<String>> mapClassNames = new Hashtable<String, ArrayList<String>>();
    private final Hashtable<String, Integer> mapParameterCount = new Hashtable<String, Integer>();
    private final Hashtable<String, Integer> mapArgumentsPassed = new Hashtable<String, Integer>();
    private final HashSet<String> hashsetPropertyNames = new HashSet<String>();
    private final List<String> allReferences = new ArrayList<String>();

    protected void validate() {
        try {
            final String propertiesPath = getDefaultPropertiesPath();
            assertNotNull("Path must not be null", propertiesPath); //$NON-NLS-1$
            assertTrue("Expected a .properties file", propertiesPath.endsWith(".properties")); //$NON-NLS-1$ //$NON-NLS-2$

            final Properties properties = new Properties();
            final InputStream propertiesStream = new FileInputStream(propertiesPath);
            properties.load(propertiesStream);
            propertiesStream.close();

            processProperties(properties);

            System.out.println("Processed properties file " + propertiesPath); //$NON-NLS-1$
            System.out.println("Found " + properties.size() + " properties."); //$NON-NLS-1$ //$NON-NLS-2$

            final List<File> javaFiles = new ArrayList<File>();
            for (final String srcDirName : getSourceDirectoryNames()) {
                getJavaFiles(new File(getSourcesRoot(srcDirName)), javaFiles);
            }
            processJavaFiles(javaFiles);

            System.out.println("Found " + javaFiles.size() + " java files."); //$NON-NLS-1$ //$NON-NLS-2$
            System.out.println("Found " + mapParameterCount.size() + " parameterized messages."); //$NON-NLS-1$ //$NON-NLS-2$
            System.out.println("Found " + allReferences.size() + " message references."); //$NON-NLS-1$ //$NON-NLS-2$

            verifyReferences();
            verifyParameterizedReferences();
        } catch (final IOException e) {
            assertTrue("Caught exception: " + e.getMessage(), false); //$NON-NLS-1$
        }
    }

    protected List<String> getSourceDirectoryNames() {
        return Arrays.asList("src"); //$NON-NLS-1$
    }

    private void verifyReferences() {
        final HashSet<String> undefinedPropertyNames = new HashSet<String>();
        final HashSet<String> unReferencedPropertyNames = new HashSet<String>(hashsetPropertyNames);

        /*
         * Remove all the DYNAMIC properties from the unreferenced set, because
         * they shouldn't be tested for references.
         */
        final List<String> dynamicPropertyNames = new ArrayList<String>();
        for (final Iterator<String> iterator = unReferencedPropertyNames.iterator(); iterator.hasNext();) {
            final String name = iterator.next();
            if (name.contains("DYNAMIC")) //$NON-NLS-1$
            {
                dynamicPropertyNames.add(name);
            }
        }
        unReferencedPropertyNames.removeAll(dynamicPropertyNames);

        // Test for undefined and unreferenced properties
        for (int i = 0; i < allReferences.size(); i++) {
            final String refName = allReferences.get(i);
            if (!hashsetPropertyNames.contains(refName)) {
                undefinedPropertyNames.add(refName);
            }

            if (unReferencedPropertyNames.contains(refName)) {
                unReferencedPropertyNames.remove(refName);
            }
        }

        for (final Iterator<String> it = undefinedPropertyNames.iterator(); it.hasNext();) {
            System.out.println("Undefined message=" + it.next()); //$NON-NLS-1$
        }

        for (final Iterator<String> it = unReferencedPropertyNames.iterator(); it.hasNext();) {
            System.out.println("Unreferenced message=" + it.next()); //$NON-NLS-1$
        }

        assertEquals("Undefined messages (see console output)", 0, undefinedPropertyNames.size()); //$NON-NLS-1$
        assertEquals("Unreferenced messages (see console output)", 0, unReferencedPropertyNames.size()); //$NON-NLS-1$
    }

    private void verifyParameterizedReferences() {
        final Enumeration<String> keys = mapArgumentsPassed.keys();
        while (keys.hasMoreElements()) {
            final String name = keys.nextElement();
            assertTrue("Expected '" + name + "' in parameter map", mapParameterCount.containsKey(name)); //$NON-NLS-1$ //$NON-NLS-2$
            assertTrue("Expected '" + name + "' in argument map", mapArgumentsPassed.containsKey(name)); //$NON-NLS-1$ //$NON-NLS-2$

            final int parameterCount = mapParameterCount.get(name).intValue();
            final int argumentCount = mapArgumentsPassed.get(name).intValue();
            assertEquals("Argument mismatch for '" + name + "'", parameterCount, argumentCount); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void processProperties(final Properties properties) {
        final Enumeration<Object> keyEnumerator = properties.keys();
        int unescapedQuoteCount = 0;

        while (keyEnumerator.hasMoreElements()) {
            final String key = (String) keyEnumerator.nextElement();

            final PropertyInfo property = new PropertyInfo(key, properties.getProperty(key));
            hashsetPropertyNames.add(property.name);

            // The message resource name should be of the format
            // <classname>.<unique-string>. This is the form generated by the
            // Eclipse wizard.

            final int dotIndex = property.name.indexOf('.');
            assertTrue("Expected '.' in '" + property.name + "'", dotIndex > 0); //$NON-NLS-1$ //$NON-NLS-2$

            final String className = property.name.substring(0, dotIndex);
            if (!mapClassNames.containsKey(className)) {
                mapClassNames.put(className, new ArrayList<String>());
            }

            final ArrayList<String> classStringIds = mapClassNames.get(className);
            classStringIds.add(property.name);

            // Get the parameter counts for each property value. Enforce a
            // 'Format' suffix on parameterized resource identifiers. Record the
            // parameter counts in a hash table to be used later to match up
            // with
            // the actual number of arguments passed.
            if (!property.name.contains("SKIPVALIDATE")) //$NON-NLS-1$
            {
                final int parameterCount = getParameterCount(property);
                if (parameterCount > 0) {
                    mapParameterCount.put(property.name, new Integer(parameterCount));

                    final boolean isFormat = property.name.endsWith("Format") || property.name.endsWith("FormatNOLOC"); //$NON-NLS-1$ //$NON-NLS-2$
                    assertTrue("Expected name '" + property.name + "' to end in 'Format'", isFormat); //$NON-NLS-1$ //$NON-NLS-2$

                    // Check for properly escaped single quotes in parameterized
                    // strings.
                    final String stripped = property.value.replaceAll("\'\'", ""); //$NON-NLS-1$ //$NON-NLS-2$
                    final int index = stripped.indexOf('\'');

                    if (index != -1) {
                        unescapedQuoteCount++;
                        System.out.println("Found unescaped single quote in " + property.name); //$NON-NLS-1$
                    }
                }
            }
        }

        if (unescapedQuoteCount > 0) {
            fail("Found " + unescapedQuoteCount + " unescaped single quotes, see console output for details"); //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private void processJavaFiles(final List<File> javaFiles) throws IOException {
        for (int i = 0; i < javaFiles.size(); i++) {
            final File javaFile = javaFiles.get(i);
            final String javaSourceCode = readFile(javaFile);

            final List<String> messageRefs = getMessageReferences(javaSourceCode);
            allReferences.addAll(messageRefs);

            final List<ArgumentInfo> argumentInfos = getMessageArguments(javaSourceCode);
            for (int j = 0; j < argumentInfos.size(); j++) {
                final ArgumentInfo argumentInfo = argumentInfos.get(j);
                final String name = argumentInfo.name;

                if (mapArgumentsPassed.contains(name)) {
                    final Integer other = mapArgumentsPassed.get(name);
                    assertEquals("Argument mismatch for id=" + name, argumentInfo.argCount, other.intValue()); //$NON-NLS-1$
                } else {
                    mapArgumentsPassed.put(name, new Integer(argumentInfo.argCount));
                }
            }
        }
    }

    private List<String> getMessageReferences(final String javaSourceCode) throws IOException {
        final String regex = "Messages.getString\\(\"([^\"]*)\"[^\\)]*\\)"; //$NON-NLS-1$
        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(javaSourceCode);

        final List<String> refs = new ArrayList<String>();
        while (matcher.find()) {
            final String sub = matcher.group(1);
            refs.add(sub);
        }

        return refs;
    }

    private List<ArgumentInfo> getMessageArguments(final String javaSourceCode) throws IOException {
        final String regex = "(?m)Messages.getString\\(\"[^\"]*\"\\).*$[^$]MessageFormat.format\\(.*;"; //$NON-NLS-1$
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(javaSourceCode);

        final List<ArgumentInfo> infos = new ArrayList<ArgumentInfo>();
        while (matcher.find()) {
            final String sub = javaSourceCode.substring(matcher.start(), matcher.end());

            final int startIndex = sub.indexOf("MessageFormat.format("); //$NON-NLS-1$
            final int endIndex = sub.indexOf(";", startIndex); //$NON-NLS-1$

            int argumentCount = 0;
            int parenNestingLevel = 0;
            for (int i = startIndex + 21; i < endIndex; i++) {
                final char ch = sub.charAt(i);
                if (ch == '(') {
                    parenNestingLevel++;
                } else if (ch == ')') {
                    parenNestingLevel--;
                } else if (ch == ',' && parenNestingLevel == 0) {
                    argumentCount++;
                }
            }

            final String id = sub.substring(20, sub.indexOf("\")")); //$NON-NLS-1$
            infos.add(new ArgumentInfo(id, argumentCount));
        }

        return infos;
    }

    private int getParameterCount(final PropertyInfo property) {
        final Pattern pattern = Pattern.compile("\\{[0-9]\\}"); //$NON-NLS-1$
        final Matcher matcher = pattern.matcher(property.value);

        /*
         * Check each parameter value against the expected index. A value must
         * be equal to or less than the previous value, or increase it by 1.
         */
        int parameterIndex = -1;
        while (matcher.find()) {
            final String sub = property.value.substring(matcher.start() + 1, matcher.end() - 1);
            final int subValue = Integer.valueOf(sub).intValue();

            if (subValue <= parameterIndex) {
                // Repeat of previous parameter; no increment.
            } else if (subValue == parameterIndex + 1) {
                parameterIndex++;
            } else {
                throw new AssertionFailedError("Parameter values must repeat the previous value or increase by 1: " //$NON-NLS-1$
                    + property.name
                    + " [" //$NON-NLS-1$
                    + property.value
                    + "]; '" //$NON-NLS-1$
                    + subValue
                    + "' must be " //$NON-NLS-1$
                    + parameterIndex
                    + " or " //$NON-NLS-1$
                    + (parameterIndex + 1));
            }
        }

        return parameterIndex + 1;
    }

    /**
     * Recursively traverse from the specified directory to locate .java files.
     * File information for java files are accumulated in the input list.
     *
     * @param directory
     * @param javaFiles
     */
    private void getJavaFiles(final File directory, final List<File> javaFiles) {
        assertNotNull("Directory must not be null", directory); //$NON-NLS-1$
        assertTrue("Directory does not exist", directory.exists()); //$NON-NLS-1$
        assertTrue("Expected a directory", directory.isDirectory()); //$NON-NLS-1$

        final File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            if (file.isDirectory()) {
                getJavaFiles(file, javaFiles);
            } else if (file.isFile() && file.getAbsolutePath().endsWith(".java")) //$NON-NLS-1$
            {
                javaFiles.add(file);
            }
        }
    }

    /**
     *
     * @param f
     * @return
     * @throws IOException
     */
    private String readFile(final File f) throws IOException {
        assertTrue("File does not exist=" + f.getAbsolutePath(), f.exists()); //$NON-NLS-1$

        final FileInputStream in = new FileInputStream(f);
        final byte[] bytes = new byte[(int) f.length()];
        final int bytesRead = in.read(bytes);

        assertEquals("Expected to read all bytes", f.length(), bytesRead); //$NON-NLS-1$

        in.close();
        return new String(bytes);
    }

    private String getDefaultPropertiesPath() {
        final String className = this.getClass().getName();
        final String[] dirs = className.split("\\."); //$NON-NLS-1$
        final StringBuffer sb = new StringBuffer(getSourcesRoot("src")); //$NON-NLS-1$

        for (int i = 0; i < dirs.length - 1; i++) {
            sb.append("/"); //$NON-NLS-1$
            sb.append(dirs[i]);
        }

        sb.append("/messages.properties"); //$NON-NLS-1$
        return sb.toString();
    }

    private String getSourcesRoot(final String srcDirectoryName) {
        final StringBuffer sb = new StringBuffer();

        final String sourcesRoot = System.getProperty("com.microsoft.tfs.util.MessagesTestCase.SourcesRoot"); //$NON-NLS-1$
        if (sourcesRoot != null) {
            sb.append(sourcesRoot);
            sb.append(File.separatorChar);
            sb.append("plugins"); //$NON-NLS-1$
            sb.append(File.separatorChar);
            sb.append(this.getClass().getPackage().getName());
            sb.append(File.separatorChar);
        }

        sb.append(srcDirectoryName);
        return sb.toString();
    }

    private class PropertyInfo {
        private final String name;
        private final String value;

        public PropertyInfo(final String name, final String value) {
            this.name = name;
            this.value = value;
        }
    }

    private class ArgumentInfo {
        private final String name;
        private final int argCount;

        public ArgumentInfo(final String name, final int argCount) {
            this.name = name;
            this.argCount = argCount;
        }
    }
}
