// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.io.File;
import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ChangeRequestValidationException;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.Wildcard;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.core.util.FileEncodingDetector;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._ChangeRequest;
import ms.tfs.versioncontrol.clientservices._03._ItemType;
import ms.tfs.versioncontrol.clientservices._03._PropertyValue;

/**
 * Contains information about an change a user is making to a version control
 * resource.
 *
 * @since TEE-SDK-10.1
 */
public class ChangeRequest extends WebServiceObjectWrapper {
    /**
     * Construct a {@link ChangeRequest}.
     *
     * @param request
     *        the request object to use as default values. All local paths in
     *        this request object must be absolute.
     * @throws ChangeRequestValidationException
     *         if this request fails validation.
     */
    public ChangeRequest(final _ChangeRequest request) throws ChangeRequestValidationException {
        this(request, true);
    }

    /**
     * Creates a {@link ChangeRequest} for the given item at the given version.
     *
     * @param item
     *        the item to change (must not be <code>null</code>)
     * @param version
     *        the version of the item to change (may be <code>null</code> to let
     *        the server apply the default version appropriate for the path)
     * @param requestType
     *        the type of change requested (must not be <code>null</code>)
     * @param itemType
     *        the type of item (may be <code>null</code> if detectTargetItemType
     *        is true, otherwise not <code>null</code>)
     * @param encoding
     *        the item's encoding (must not be <code>null</code>)
     * @param lockLevel
     *        the lock level desired (must not be <code>null</code>)
     * @param deletionID
     *        the deletion ID of the item
     * @param targetItem
     *        the target item (may be null if the change does not require a
     *        different target item)
     * @param detectTargetItemType
     *        true to examine the item's local representation to determine the
     *        type (file or folder), false to use the given item type (which
     *        must not be null)
     * @throws ChangeRequestValidationException
     *         if the change request is not valid after parameters are
     *         interpreted
     */
    public ChangeRequest(
        final ItemSpec item,
        final VersionSpec version,
        final RequestType requestType,
        final ItemType itemType,
        final int encoding,
        final LockLevel lockLevel,
        final int deletionID,
        final String targetItem,
        final boolean detectTargetItemType) throws ChangeRequestValidationException {
        this(
            new _ChangeRequest(
                requestType.getWebServiceObject(),
                deletionID,
                encoding,
                (itemType == null) ? null : itemType.getWebServiceObject(),
                lockLevel.getWebServiceObject(),
                targetItem,
                _ItemType.Any,
                item.getWebServiceObject(),
                version != null ? version.getWebServiceObject() : null,
                null /* use setProperties() */),
            detectTargetItemType);
    }

    /**
     * @param request
     *        the request object to use as default values. All local paths in
     *        this request object must be absolute.
     * @param detectTargetItemType
     *        whether to validate the item type in the request by checking for
     *        an existing target.
     * @throws ChangeRequestValidationException
     *         if this request fails validation.
     */
    private ChangeRequest(final _ChangeRequest request, final boolean detectTargetItemType)
        throws ChangeRequestValidationException {
        super(request);

        /*
         * We accept local paths in the change request, and they may be
         * relative. We have to convert them to their canonical path before we
         * continue, or the server won't be happy.
         */
        if (getTargetItem() != null && !ServerPath.isServerPath(getTargetItem())) {
            setTargetItem(LocalPath.canonicalize(getTargetItem()));
        }

        /*
         * We need some kind of change. A good sanity check.
         */
        Check.isTrue(getRequestType() != RequestType.NONE, "getRequestType() != RequestType.NONE"); //$NON-NLS-1$

        if (detectTargetItemType) {
            /*
             * For branch, rename, and undelete, make sure there is not a
             * writable file at the target. Also, check for the target being a
             * directory and let the server know that fact.
             */
            if (getRequestType() == RequestType.BRANCH
                || getRequestType() == RequestType.RENAME
                || getRequestType() == RequestType.UNDELETE) {
                String targetItem = getTargetItem();
                boolean directoryAtTarget = false;

                if (targetItem != null
                    && Wildcard.isWildcard(targetItem) == false
                    && ServerPath.isServerPath(targetItem) == false) {
                    File targetItemFile = new File(targetItem);
                    final FileSystemAttributes targetItemAttributes =
                        FileSystemUtils.getInstance().getAttributes(targetItem);

                    if (targetItemFile.exists()) {
                        if (targetItemFile.isDirectory()) {
                            /*
                             * The new target is currently a directory, so the
                             * new item should be a child inside that directory.
                             * The server will create any needed repository
                             * folders that are required.
                             */

                            directoryAtTarget = true;

                            /*
                             * Make sure there won't be a writable file in the
                             * way when the item is moved.
                             */
                            if (ServerPath.isServerPath(getItemSpec().getItem()) == false
                                && Wildcard.isWildcard(getItemSpec().getItem()) == false) {
                                targetItem =
                                    LocalPath.combine(targetItem, LocalPath.getLastComponent(getItemSpec().getItem()));
                                targetItemFile = new File(targetItem);
                            }
                        }

                        /*
                         * Detect a case-changing rename, so we can skip the
                         * exception in this case.
                         */
                        final boolean caseChangingRename = (ServerPath.isServerPath(getItemSpec().getItem()) == false
                            && LocalPath.isWildcard(getItemSpec().getItem()) == false
                            && getItemSpec().getItem().equalsIgnoreCase(targetItem)
                            && getItemSpec().getItem().equals(targetItem) == false);

                        /*
                         * Check for file existence again because the path to
                         * check may be changed in the if above.
                         */
                        if (caseChangingRename == false
                            && targetItemFile.exists()
                            && targetItemFile.isFile()
                            && targetItemAttributes.isReadOnly() == false) {
                            throw new ChangeRequestValidationException(MessageFormat.format(
                                //@formatter:off
                                Messages.getString("ChangeRequest.AWritableFileExistsAtThePathWhichIsInTheWayOfTheChangeRequestForItemFormat"), //$NON-NLS-1$
                                //@formatter:on
                                targetItem,
                                getItemSpec().getItem()));
                        }
                    }
                }

                if (directoryAtTarget) {
                    setTargetItemType(ItemType.FOLDER);
                }
            }
        }
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ChangeRequest getWebServiceObject() {
        return (_ChangeRequest) webServiceObject;
    }

    public int getDeletionID() {
        return getWebServiceObject().getDid();
    }

    public int getEncoding() {
        return getWebServiceObject().getEnc();
    }

    public ItemSpec getItemSpec() {
        return new ItemSpec(getWebServiceObject().getItem());
    }

    public LockLevel getLockLevel() {
        return LockLevel.fromWebServiceObject(getWebServiceObject().getLock());
    }

    public RequestType getRequestType() {
        return RequestType.fromWebServiceObject(getWebServiceObject().getReq());
    }

    public String getTargetItem() {
        final String target = getWebServiceObject().getTarget();

        if (target != null && ServerPath.isServerPath(target) == false) {
            return LocalPath.tfsToNative(target);
        }

        return getWebServiceObject().getTarget();
    }

    public ItemType getTargetItemType() {
        return ItemType.fromWebServiceObject(getWebServiceObject().getTargettype());
    }

    public ItemType getItemType() {
        return ItemType.fromWebServiceObject(getWebServiceObject().getType());
    }

    public VersionSpec getVersionSpec() {
        return VersionSpec.fromWebServiceObject(getWebServiceObject().getVspec());
    }

    public void setDeletionID(final int did) {
        getWebServiceObject().setDid(did);
    }

    public void setEncoding(final int enc) {
        getWebServiceObject().setEnc(enc);
    }

    public void setItemSpec(final ItemSpec item) {
        getWebServiceObject().setItem(item.getWebServiceObject());
    }

    public void setLockLevel(final LockLevel lock) {
        getWebServiceObject().setLock(lock.getWebServiceObject());
    }

    public void setRequestType(final RequestType req) {
        getWebServiceObject().setReq(req.getWebServiceObject());
    }

    public void setTargetItem(String target) {
        if (ServerPath.isServerPath(target) == false) {
            target = LocalPath.nativeToTFS(target);
        }

        getWebServiceObject().setTarget(target);
    }

    public void setTargetItemType(final ItemType targettype) {
        getWebServiceObject().setTargettype(targettype.getWebServiceObject());
    }

    public void setItemType(final ItemType type) {
        getWebServiceObject().setType(type.getWebServiceObject());
    }

    public void setVersionSpec(final VersionSpec vspec) {
        getWebServiceObject().setVspec(vspec.getWebServiceObject());
    }

    public PropertyValue[] getProperties() {
        return (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getProperties());
    }

    public void setProperties(final PropertyValue[] properties) {
        getWebServiceObject().setProperties((_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, properties));
    }

    /**
     * Create change requests from an array of string paths. The encodings of
     * disk files are automatically detected.
     *
     * @param paths
     *        the local disk or server paths of files or folders to create a
     *        change request for (must not be <code>null</code>) Local paths
     *        will be canonicalized (therefore made absolute). Any server paths
     *        will not have their encoding automatically detected. (must not be
     *        <code>null</code>)
     * @param requestType
     *        the type of change that is being requested.
     * @param lockLevel
     *        an array containing the lock level for each file.
     * @param recursionType
     *        what kind of recursion to perform on the paths supplied as the
     *        first argument.
     * @param fileEncoding
     *        an array containing the encoding for each item spec, or null to
     *        indicate that the encoding is unchanged. If the array is null,
     *        {@link VersionControlConstants#ENCODING_UNCHANGED} is used for
     *        all.
     * @return a new array of AChangeRequest instances.
     * @throws ChangeRequestValidationException
     *         if an error occurred constructing an AChangeRequest because a
     *         local disk file could not be examined.
     */
    public static ChangeRequest[] fromStrings(
        final String[] paths,
        final RequestType requestType,
        final LockLevel[] lockLevel,
        final RecursionType recursionType,
        final FileEncoding[] fileEncoding) throws ChangeRequestValidationException {
        Check.notNull(paths, "paths"); //$NON-NLS-1$

        final ItemSpec[] specs = new ItemSpec[paths.length];

        for (int i = 0; i < paths.length; i++) {
            String path = paths[i];

            /*
             * If the path is a local path, canonicalize it except for symlinks
             * (making it absolute if it was relative).
             */
            if (ServerPath.isServerPath(path) == false) {
                path = LocalPath.canonicalize(path);
            }

            specs[i] = new ItemSpec(path, recursionType);
        }

        return fromSpecs(specs, requestType, lockLevel, fileEncoding);
    }

    /**
     * Create change requests from an array of specs. The encodings of disk
     * files are automatically detected.
     *
     * @param specs
     *        the items specs to create requests for. Local paths will be
     *        canonicalized (therefore made absolute). Any server paths will not
     *        have their encoding automatically detected. (must not be
     *        <code>null</code>)
     * @param requestType
     *        the type of change that is being requested.
     * @param lockLevel
     *        an array containing the lock level for each item spec.
     * @param fileEncoding
     *        an array containing the encoding for each item spec, or null to
     *        indicate that the encoding is unchanged. If the array is null,
     *        {@link VersionControlConstants#ENCODING_UNCHANGED} is used for
     *        all.
     * @return a new array of AChangeRequest instances.
     * @throws ChangeRequestValidationException
     *         if an error occurred constructing an AChangeRequest because a
     *         local disk file could not be examined.
     */
    public static ChangeRequest[] fromSpecs(
        final ItemSpec[] specs,
        final RequestType requestType,
        final LockLevel[] lockLevel,
        final FileEncoding[] fileEncoding) throws ChangeRequestValidationException {
        Check.notNull(specs, "specs"); //$NON-NLS-1$

        final ChangeRequest[] ret = new ChangeRequest[specs.length];

        for (int i = 0; i < specs.length; i++) {
            final ItemSpec itemSpec = specs[i];

            final String path = itemSpec.getItem();

            /*
             * Default to not changing the encoding.
             */
            int codePage = VersionControlConstants.ENCODING_UNCHANGED;

            /*
             * If an encoding was specified, use that as a hint to the detector.
             * The detector accepts server and local paths, including those with
             * wildcards.
             */
            if (fileEncoding != null && fileEncoding[i] != null) {
                try {
                    codePage = FileEncodingDetector.detectEncoding(path, fileEncoding[i]).getCodePage();
                } catch (final TECoreException e) {
                    throw new ChangeRequestValidationException(
                        MessageFormat.format(
                            Messages.getString("ChangeRequest.ItemWasSkippedBecauseEncodingDetectionFailedFormat"), //$NON-NLS-1$
                            path,
                            e.getMessage()));
                }
            }

            ret[i] = new ChangeRequest(
                itemSpec,
                null,
                requestType,
                ItemType.ANY,
                codePage,
                lockLevel[i],
                itemSpec.getDeletionID(),
                null,
                true);
        }
        return ret;
    }

    @Override
    public String toString() {
        final _ChangeRequest changeRequest = getWebServiceObject();

        return ChangeRequest.class.getName()
            + " " //$NON-NLS-1$
            + changeRequest.getItem().getItem()
            + ":" //$NON-NLS-1$
            + (changeRequest.getItem().getRecurse() != null ? changeRequest.getItem().getRecurse().getName() : "null") //$NON-NLS-1$
            + " " //$NON-NLS-1$
            + changeRequest.getReq().getName()
            + (changeRequest.getTarget() != null ? " -> (" + changeRequest.getTarget() + ")" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
