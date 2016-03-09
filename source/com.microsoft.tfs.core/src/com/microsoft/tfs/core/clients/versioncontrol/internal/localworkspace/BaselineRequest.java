// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

public class BaselineRequest {
    /**
     * The baseline file GUID which was generated and already persisted in the
     * local version table with the entry.
     */
    private byte[] baselineFileGUID;

    /**
     * This is only used in combination with the baseline file GUID to figure
     * out what baseline directory to put the file into. (There is one per
     * partition.) If you don't know what partition the baseline file should
     * live on, it's OK for this value to be null. The baseline will be stored
     * in an arbitrarily chosen partition's baseline folder.
     */
    private String baselinePartitionLocalItem;

    /**
     * If set, then this path will be used as the baseline content. As the
     * content is gzipped, it will be hashed and that hash will be compared at
     * completion with the value stored in HashValue. If they differ, then the
     * baseline request will try to fall back to the DownloadUrl, if available.
     */
    private String sourceLocalItem;

    /**
     * The download URL to use to fetch the baseline content. Used if LocalItem
     * is null or if the local item's hash value does not match our own
     * HashValue.
     */
    private String downloadURL;

    /**
     * The hash value of the baseline content. (Optional.)
     */
    private byte[] hashValue;

    public static BaselineRequest fromDisk(
        final byte[] baselineFileGUID,
        final String baselinePartitionLocalItem,
        final String sourceLocalItem,
        final byte[] hashValue) {
        final BaselineRequest request = new BaselineRequest();

        request.baselineFileGUID = baselineFileGUID;
        request.baselinePartitionLocalItem = baselinePartitionLocalItem;
        request.sourceLocalItem = sourceLocalItem;
        request.hashValue = hashValue;

        return request;
    }

    public static BaselineRequest fromDiskAndDownloadUrl(
        final byte[] baselineFileGUID,
        final String baselinePartitionLocalItem,
        final String sourceLocalItem,
        final String downloadURL,
        final byte[] hashValue) {
        final BaselineRequest request = new BaselineRequest();

        request.baselineFileGUID = baselineFileGUID;
        request.baselinePartitionLocalItem = baselinePartitionLocalItem;
        request.sourceLocalItem = sourceLocalItem;
        request.downloadURL = downloadURL;
        request.hashValue = hashValue;

        return request;
    }

    public static BaselineRequest fromDownloadUrl(
        final byte[] baselineFileGUID,
        final String baselinePartitionLocalItem,
        final String downloadURL,
        final byte[] hashValue) {
        final BaselineRequest request = new BaselineRequest();

        request.baselineFileGUID = baselineFileGUID;
        request.baselinePartitionLocalItem = baselinePartitionLocalItem;
        request.downloadURL = downloadURL;
        request.hashValue = hashValue;

        return request;
    }

    public static BaselineRequest makeRemoveRequest(final byte[] baselineFileGUID) {
        final BaselineRequest request = new BaselineRequest();

        request.baselineFileGUID = baselineFileGUID;

        return request;
    }

    public BaselineRequest() {
    }

    public byte[] getBaselineFileGUID() {
        return baselineFileGUID;
    }

    public void setBaselineFileGUID(final byte[] baselineFileGUID) {
        this.baselineFileGUID = baselineFileGUID;
    }

    public String getBaselinePartitionLocalItem() {
        return baselinePartitionLocalItem;
    }

    public void setBaselinePartitionLocalItem(final String baselinePartitionLocalItem) {
        this.baselinePartitionLocalItem = baselinePartitionLocalItem;
    }

    public String getSourceLocalItem() {
        return sourceLocalItem;
    }

    public void setSourceLocalItem(final String sourceLocalItem) {
        this.sourceLocalItem = sourceLocalItem;
    }

    public String getDownloadURL() {
        return downloadURL;
    }

    public void setDownloadURL(final String downloadURL) {
        this.downloadURL = downloadURL;
    }

    public byte[] getHashValue() {
        return hashValue;
    }

    public void setHashValue(final byte[] hashValue) {
        this.hashValue = hashValue;
    }
}