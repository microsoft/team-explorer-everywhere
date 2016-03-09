// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Calendar;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.DestroyedContentUnavailableException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ItemNotFoundException;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.internal.DownloadURL;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadSpec;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.temp.TempStorageService;

import ms.tfs.versioncontrol.clientservices._03._Item;
import ms.tfs.versioncontrol.clientservices._03._PropertyValue;

/**
 * Contains information about a version control item.
 *
 * @since TEE-SDK-10.1
 */
public class Item extends WebServiceObjectWrapper implements Comparable<Item> {
    private DownloadURL downloadURLObject;

    public Item() {
        this(new _Item());
    }

    public Item(final ItemType itemType, final String serverItem, final int encoding) {
        this(
            new _Item(
                0,
                null,
                0,
                encoding,
                itemType.getWebServiceObject(),
                0,
                serverItem,
                null,
                null,
                null,
                0,
                null,
                false,
                null,
                null));
    }

    public Item(final _Item item) {
        super(item);

        // Update the wrapper and wrapped objects
        setDownloadURL(item.getDurl());
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Item getWebServiceObject() {
        return (_Item) webServiceObject;
    }

    @Override
    public int compareTo(final Item other) {
        // First we sort by path.
        final int res = ServerPath.compareTopDown(getServerItem(), other.getServerItem());
        if (res != 0) {
            return res;
        }

        /*
         * Now we compare by deletion ID. Visual Studio's code sorts this way to
         * give consistent order to matching paths. I'm not sure if this field
         * is special in any way, or if they just arbitrarily chose it for the
         * secondary sort.
         */
        if (getDeletionID() < other.getDeletionID()) {
            return -1;
        } else if (getDeletionID() > other.getDeletionID()) {
            return 1;
        } else {
            return 0;
        }
    }

    public int getChangeSetID() {
        return getWebServiceObject().getCs();
    }

    public void setChangeSetID(final int cs) {
        getWebServiceObject().setCs(cs);
    }

    public Calendar getCheckinDate() {
        return (Calendar) getWebServiceObject().getDate().clone();
    }

    public void setCheckinDate(final Calendar date) {
        getWebServiceObject().setDate((Calendar) date.clone());
    }

    public int getDeletionID() {
        return getWebServiceObject().getDid();
    }

    public void setDeletionID(final int did) {
        getWebServiceObject().setDid(did);
    }

    public FileEncoding getEncoding() {
        return new FileEncoding(getWebServiceObject().getEnc());
    }

    public void setEncoding(final FileEncoding encoding) {
        getWebServiceObject().setEnc(encoding.getCodePage());
    }

    public byte[] getContentHashValue() {
        return getWebServiceObject().getHash();
    }

    public void setContentHashValue(final byte[] hash) {
        getWebServiceObject().setHash(hash);
    }

    public String getServerItem() {
        return getWebServiceObject().getItem();
    }

    public void setServerItem(final String item) {
        getWebServiceObject().setItem(item);
    }

    public int getItemID() {
        return getWebServiceObject().getItemid();
    }

    public void setItemID(final int itemid) {
        getWebServiceObject().setItemid(itemid);
    }

    public long getContentLength() {
        return getWebServiceObject().getLen();
    }

    public void setContentLength(final long len) {
        getWebServiceObject().setLen(len);
    }

    public ItemType getItemType() {
        return ItemType.fromWebServiceObject(getWebServiceObject().getType());
    }

    public void setItemType(final ItemType type) {
        getWebServiceObject().setType(type.getWebServiceObject());
    }

    public void setDownloadURL(final String url) {
        getWebServiceObject().setDurl(url);
        downloadURLObject = new DownloadURL(url);
    }

    public String getDownloadURL() {
        return getWebServiceObject().getDurl();
    }

    public String getTimeZone() {
        return getWebServiceObject().getTz();
    }

    public String getTimeZoneO() {
        return getWebServiceObject().getTzo();
    }

    public PropertyValue[] getPropertyValues() {
        // TODO remove the selectUnique
        return PropertyUtils.selectUnique(
            (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getPropertyValues()));
    }

    public void setPropertyValues(final PropertyValue[] properties) {
        getWebServiceObject().setPropertyValues(
            (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, properties));
    }

    /**
     * This method always returns false for items in a TFS 2005 or TFS 2008
     * server.
     *
     * @return true if the item is a branch, false if it is not a branch
     * @since TFS2010
     */
    public boolean isBranch() {
        return getWebServiceObject().isIsbranch();
    }

    /**
     * Downloads the content for this version of the item to a temp file in a
     * new temp directory allocated with {@link TempStorageService}.
     *
     * @param fileName
     *        the file name (not full path) to give the temporary file
     * @return the temporary file created
     */
    public synchronized File downloadFileToTempLocation(final VersionControlClient client, final String fileName) {
        Check.notNullOrEmpty(fileName, "fileName"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$

        try {
            final File file = new File(TempStorageService.getInstance().createTempDirectory(), fileName);
            downloadFile(client, file.getAbsolutePath());
            return file;
        } catch (final IOException e) {
            throw new VersionControlException(e);
        }
    }

    /**
     * Downloads the content for this version of the item.
     *
     * @param filePath
     *        where to save the downloaded file contents (must not be
     *        <code>null</code>)
     */
    public synchronized void downloadFile(final VersionControlClient client, final String filePath) {
        Check.notNull(filePath, "filePath"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$

        prepareForDownload(client);

        client.downloadFile(new DownloadSpec(getDownloadURL()), new File(filePath), true);
    }

    /**
     * Makes sure we have all the information needed to download the file and
     * this item can be downloaded.
     */
    private void prepareForDownload(final VersionControlClient client) {
        Check.notNull(client, "client"); //$NON-NLS-1$

        // Make sure we have a file, not a directory
        if (getItemType() != ItemType.FILE) {
            throw new VersionControlException(
                MessageFormat.format(Messages.getString("PendingChange.PathIsNotAFileFormat"), getServerItem())); //$NON-NLS-1$
        }

        // Check for the case where the user attempts to download an item that
        // is in an indeterminate
        // state.
        if (getChangeSetID() == VersionControlConstants.INDETERMINATE_CHANGESET) {
            throw new VersionControlException(
                MessageFormat.format(Messages.getString("Item.ItemChangesetIndeterminateFormat"), getServerItem())); //$NON-NLS-1$
        }

        // For cases where the download URL wasn't requested, we get it here.
        if (getDownloadURL() == null || getDownloadURL().length() == 0) {
            final Item item = client.getItem(getItemID(), getChangeSetID(), true);
            if (item == null) {
                throw new ItemNotFoundException(
                    MessageFormat.format(
                        Messages.getString("Item.ItemNotFoundExceptionFormat"), //$NON-NLS-1$
                        getServerItem()));
            } else if (null == item.getDownloadURL()) {
                // In the case where the user has AdminProjectRigths but not
                // Read the Url will be null even though the caller will get the
                // item.
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("Item.ItemCannotBeDownloadReadPermissionRequiredFormat"), //$NON-NLS-1$
                        item.getServerItem()));
            }

            cloneDownloadURLFromItem(item);
        }

        // If the content is destroyed, we can't download it.
        if (isContentDestroyed(client)) {
            throw new DestroyedContentUnavailableException(
                MessageFormat.format(
                    Messages.getString("Item.DestroyedContentUnavailableExceptionFormat"), //$NON-NLS-1$
                    getChangeSetID(),
                    getServerItem()));
        }
    }

    /**
     * @return true if the content for this file has been destroyed, false if
     *         the content exists, or if the item is a directory.
     */
    public synchronized boolean isContentDestroyed(final VersionControlClient client) {
        Check.notNull(client, "client"); //$NON-NLS-1$

        // For cases where the download URL wasn't requested, we get it here.
        if (getDownloadURL() == null || getDownloadURL().length() == 0) {
            final Item item = client.getItem(getItemID(), getChangeSetID(), true);
            cloneDownloadURLFromItem(item);
        }

        return downloadURLObject.isContentDestroyed();
    }

    /**
     * Given another {@link Item} object, obtains the download URL information
     * from it and stuffs it in this object.
     */
    private void cloneDownloadURLFromItem(final Item item) {
        Check.notNull(item, "item"); //$NON-NLS-1$

        getWebServiceObject().setDurl(item.getDownloadURL());
        downloadURLObject = item.downloadURLObject;
    }
}
