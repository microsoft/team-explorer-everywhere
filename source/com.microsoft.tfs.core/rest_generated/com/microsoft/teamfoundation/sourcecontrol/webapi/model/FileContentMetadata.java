// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

 /*
* ---------------------------------------------------------
* Generated file, DO NOT EDIT
* ---------------------------------------------------------
*
* See following wiki page for instructions on how to regenerate:
*   https://vsowiki.com/index.php?title=Rest_Client_Generation
*/

package com.microsoft.teamfoundation.sourcecontrol.webapi.model;


/** 
 */
public class FileContentMetadata {

    private String contentType;
    private int encoding;
    private String extension;
    private String fileName;
    private boolean isBinary;
    private boolean isImage;
    private String vsLink;

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public int getEncoding() {
        return encoding;
    }

    public void setEncoding(final int encoding) {
        this.encoding = encoding;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public boolean getIsBinary() {
        return isBinary;
    }

    public void setIsBinary(final boolean isBinary) {
        this.isBinary = isBinary;
    }

    public boolean getIsImage() {
        return isImage;
    }

    public void setIsImage(final boolean isImage) {
        this.isImage = isImage;
    }

    public String getVsLink() {
        return vsLink;
    }

    public void setVsLink(final String vsLink) {
        this.vsLink = vsLink;
    }
}
