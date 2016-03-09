// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.product;

/**
 * Tracks the current running product. Call {@link #initialize(ProductName)}
 * once early in application startup.
 * <p>
 * This class is for internal use only.
 *
 * @threadsafety thread-safe
 */
public class ProductInformation {
    private static volatile ProductName currentProduct = ProductName.SDK;

    public static void initialize(final ProductName product) {
        currentProduct = product;
    }

    public static ProductName getCurrent() {
        return currentProduct;
    }
}
