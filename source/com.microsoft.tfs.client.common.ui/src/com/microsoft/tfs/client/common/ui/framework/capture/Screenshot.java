// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.capture;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * A utility class to take a screen shot and save it to a file. This is useful
 * in a bug capturing product. Uses AWT Robot class to take the screen shot.
 */
public class Screenshot {

    private static final String EXT_PNG = ".png"; //$NON-NLS-1$

    /**
     * Save a screen shot as a PNG file and return the file that was saved.
     *
     * @param path
     *        The path that you would like the file saved into i.e. c:/temp
     * @param fileName
     *        The prefix file name you would like i.e. &quot;myfile&quot; for
     *        &quot;myfile.png&quot; if a file alread exists with this name in
     *        this directory then a new one will be created with a number on the
     *        end, i.e. &quot;myfile-1.png&quot;
     * @param delayInMilliseconds
     *        delay in millis before the snapshot is taken - allows the users to
     *        get into position to take the screen shot.
     * @return File that has the saved screen shot in it.
     */
    public File saveScreenShot(final String path, final String fileName, final long delayInMilliseconds) {

        // Wait the specified ammount of time...
        try {
            Thread.sleep(delayInMilliseconds);
        } catch (final InterruptedException e) {
            // ignore.
        }

        // Determine the file name.
        String outPath;
        boolean fileExists = true;
        int attempt = 0;
        File saveFile = null;
        while (fileExists) {
            outPath = path + File.separator + fileName + (attempt == 0 ? "" : "-" + attempt) + EXT_PNG; //$NON-NLS-1$ //$NON-NLS-2$
            saveFile = new File(outPath);
            fileExists = saveFile.exists();
            attempt++;
        }

        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final Dimension screenSize = toolkit.getScreenSize();
        final Rectangle screenRect = new Rectangle(screenSize);
        // create screen shot
        try {
            final Robot robot = new Robot();
            final BufferedImage image = robot.createScreenCapture(screenRect);
            // save captured image to PNG file
            ImageIO.write(image, "png", saveFile); //$NON-NLS-1$
        } catch (final AWTException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return saveFile;
    }

}
