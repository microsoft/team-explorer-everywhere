// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

import com.microsoft.tfs.util.Check;

public class PolygonUtils {
    /**
     * Given a list of coordinates for a polygon, this returns a rectangle
     * describing the bounds. The x/y describes the origin of the rectangle, the
     * width/height describes the width and height. (Thus the size of the
     * rectangle required to draw is width + x, height + y, since x and y may
     * not be zeros.)
     *
     * @param polygonPoints
     * @return
     */
    public static Rectangle getBounds(final int[] polygonPoints) {
        Check.notNull(polygonPoints, "polygon points"); //$NON-NLS-1$
        Check.isTrue((polygonPoints.length > 0), "polygon points not empty"); //$NON-NLS-1$
        Check.isTrue((polygonPoints.length % 2) == 0, "even number of polygon points"); //$NON-NLS-1$

        /* Loop once to determine the origin - ie the smallest x and y values */
        int originX = Integer.MAX_VALUE, originY = Integer.MAX_VALUE;
        for (int i = 0; i < polygonPoints.length; i += 2) {
            final int x = polygonPoints[i];
            final int y = polygonPoints[i + 1];

            if (x < originX) {
                originX = x;
            }
            if (y < originY) {
                originY = y;
            }
        }

        /* Loop again to determine the width and height from origin */
        int width = 0, height = 0;
        for (int i = 0; i < polygonPoints.length; i += 2) {
            final int x = polygonPoints[i];
            final int y = polygonPoints[i + 1];

            if (width < (x - originX)) {
                width = x - originX;
            }
            if (height < (y - originY)) {
                height = y - originY;
            }
        }

        return new Rectangle(originX, originX, width, height);
    }

    /**
     * Given the polygon described by polygonPoints, transforms the position of
     * the polygon by the x/y described by the Point. That is, the polygon will
     * be shifted by x to the left and y to the bottom. Useful for remapping a
     * polygon into a new space.
     *
     * @param newOrigin
     *        The new origin of the polygon
     * @param polygonPoints
     *        The points describing the polygon
     * @return Points describing the same polygon, shifted in the x/y space
     */
    public static int[] transformPosition(final Point newOrigin, final int[] polygonPoints) {
        Check.notNull(newOrigin, "newOrigin"); //$NON-NLS-1$
        Check.notNull(polygonPoints, "polygon points"); //$NON-NLS-1$
        Check.isTrue((polygonPoints.length > 0), "polygon points not empty"); //$NON-NLS-1$
        Check.isTrue((polygonPoints.length % 2) == 0, "even number of polygon points"); //$NON-NLS-1$

        final int[] newPolygonPoints = new int[polygonPoints.length];

        for (int i = 0; i < polygonPoints.length; i += 2) {
            newPolygonPoints[i] = polygonPoints[i] + newOrigin.x;
            newPolygonPoints[i + 1] = polygonPoints[i + 1] + newOrigin.y;
        }

        return newPolygonPoints;
    }
}
