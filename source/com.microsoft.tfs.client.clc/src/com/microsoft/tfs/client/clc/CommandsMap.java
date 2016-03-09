// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.util.Check;

public abstract class CommandsMap {
    public abstract Class[] getGlobalOptions();

    /**
     * Maps a name string to a Class instance, so we can look up commands by
     * name. The key is a String, and the value is a Class.
     */
    private final Map _aliasesToCommands = new HashMap();

    /**
     * Maps a command to its canonical name, so we can print a nice list out for
     * help text with the correct names.
     */
    private final Map _commandsToCanonicalNames = new HashMap();

    /**
     * Add a command in the hash map.
     *
     * @param commandClass
     *        the command class.
     * @param names
     *        all names the command can be known by.
     */
    protected void putCommand(final Class commandClass, final String[] names) {
        Check.isTrue(names.length > 0, "names.length > 0"); //$NON-NLS-1$

        _commandsToCanonicalNames.put(commandClass, names[0]);

        for (int i = 0; i < names.length; i++) {
            _aliasesToCommands.put(names[i], commandClass);
        }
    }

    /**
     * Creates an instance of the command class that matches the supplied
     * command name.
     *
     * @param commandName
     *        the name (or partial name) of the command to create.
     * @return a new Command instance that matches the given command name, null
     *         if no matches found.
     */
    public Command findCommand(final String commandName) {
        Check.notNullOrEmpty(commandName, "commandName"); //$NON-NLS-1$

        for (final Iterator i = _aliasesToCommands.keySet().iterator(); i.hasNext();) {
            final Object key = i.next();

            final String alias = (String) key;

            // TODO Support partial matches.
            if (alias.compareToIgnoreCase(commandName) == 0) {
                // We found it. Create an instance.
                final Class klass = (Class) _aliasesToCommands.get(key);
                Command c;
                try {
                    c = (Command) klass.newInstance();
                } catch (final Exception e) {
                    return null;
                }

                c.setCanonicalName((String) _commandsToCanonicalNames.get(klass));
                c.setMatchedAlias(alias);
                c.setUserText(commandName);

                return c;
            }
        }

        return null;
    }

    /**
     * Gets all the aliases (including canonical name) for the given command.
     *
     * @param command
     *        the command to get all aliases for (not null).
     * @return all the aliases (including canonical name) for the given command.
     */
    public String[] getAliasesForCommand(final Command command) {
        Check.notNull(command, "command"); //$NON-NLS-1$

        final ArrayList ret = new ArrayList();

        for (final Iterator i = _aliasesToCommands.entrySet().iterator(); i.hasNext();) {
            final Entry entry = (Entry) i.next();

            if (entry.getValue().equals(command.getClass())) {
                ret.add(entry.getKey());
            }
        }

        return (String[]) ret.toArray(new String[ret.size()]);
    }

    /**
     * Gets the map used to map commands to canonical names.
     *
     * @return a new mapping of command to the the command's canonical name.
     */
    public Map getCommandsToCanonicalNamesMap() {
        return (Map) ((HashMap) _commandsToCanonicalNames).clone();
    }
}
