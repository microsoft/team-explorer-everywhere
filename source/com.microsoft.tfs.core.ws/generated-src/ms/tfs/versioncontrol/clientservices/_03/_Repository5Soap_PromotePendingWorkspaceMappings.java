// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package ms.tfs.versioncontrol.clientservices._03;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.microsoft.tfs.core.ws.runtime.serialization.ElementSerializable;
import com.microsoft.tfs.core.ws.runtime.xml.XMLStreamWriterHelper;

public class _Repository5Soap_PromotePendingWorkspaceMappings
    implements ElementSerializable
{
    // No attributes    

    // Elements
    protected String workspaceName;
    protected String ownerName;
    protected int projectNotificationId;

    public _Repository5Soap_PromotePendingWorkspaceMappings()
    {
        super();
    }

    public _Repository5Soap_PromotePendingWorkspaceMappings(
        final String workspaceName,
        final String ownerName,
        final int projectNotificationId)
    {
        // TODO : Call super() instead of setting all fields directly?
        setWorkspaceName(workspaceName);
        setOwnerName(ownerName);
        setProjectNotificationId(projectNotificationId);
    }

    public String getWorkspaceName()
    {
        return this.workspaceName;
    }

    public void setWorkspaceName(String value)
    {
        this.workspaceName = value;
    }

    public String getOwnerName()
    {
        return this.ownerName;
    }

    public void setOwnerName(String value)
    {
        this.ownerName = value;
    }

    public int getProjectNotificationId()
    {
        return this.projectNotificationId;
    }

    public void setProjectNotificationId(int value)
    {
        this.projectNotificationId = value;
    }

    @Override
    public void writeAsElement(XMLStreamWriter writer, String name)
        throws XMLStreamException
    {
        writer.writeStartElement(name);

        // Elements
        XMLStreamWriterHelper.writeElement(
            writer,
            "workspaceName",
            this.workspaceName);
        XMLStreamWriterHelper.writeElement(
            writer,
            "ownerName",
            this.ownerName);
        XMLStreamWriterHelper.writeElement(
            writer,
            "projectNotificationId",
            this.projectNotificationId);

        writer.writeEndElement();
    }

}
