// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.type;

import com.microsoft.tfs.core.clients.workitem.internal.fields.PSFieldDefinitionTypeEnum;

/**
 * Used to obtain WITypeConverter instances.
 */
public class WITypeConverterFactory {
    // TODO use different converters for large vs. small field type flags?
    private static final WITypeConverter STRING_CONVERTER = new WIStringTypeConverter(true, true);
    private static final WITypeConverter HTML_CONVERTER = new WIStringTypeConverter(true, false);
    private static final WITypeConverter IDENTITY_CONVERTER = new WIIdentityTypeConverter();
    private static final WITypeConverter INT_CONVERTER = new WIIntegerTypeConverter();
    private static final WITypeConverter DOUBLE_CONVERTER = new WIDoubleTypeConverter();
    private static final WITypeConverter DATE_CONVERTER = new WIDateTypeConverter();
    private static final WITypeConverter GUID_CONVERTER = new WIGUIDTypeConverter();

    public static WITypeConverter getTypeConverter(final int psType) {
        /*
         * The design of this method, as well as the WITypeConverter
         * implementations, is based on:
         *
         * Microsoft.TeamFoundation.WorkItemTracking.Client.Field#TranslateValue
         * (in the Microsoft.TeamFoundation.WorkItemTracking.Client assembly)
         */

        if (psType <= PSFieldDefinitionTypeEnum.SINGLE_VALUED_DOUBLE) {
            if (psType <= PSFieldDefinitionTypeEnum.SINGLE_VALUED_DATE_TIME) {
                switch (psType) {
                    case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD:
                        return STRING_CONVERTER;
                    case PSFieldDefinitionTypeEnum.SINGLE_VALUED_INTEGER:
                        return INT_CONVERTER;
                    case PSFieldDefinitionTypeEnum.SINGLE_VALUED_DATE_TIME:
                        return DATE_CONVERTER;
                }

                return IDENTITY_CONVERTER;
            }

            switch (psType) {
                case PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_PLAINTEXT:
                case PSFieldDefinitionTypeEnum.TREE_NODE:
                    return STRING_CONVERTER;

                case PSFieldDefinitionTypeEnum.SINGLE_VALUED_GUID:
                    return GUID_CONVERTER;
            }

            if (psType == PSFieldDefinitionTypeEnum.SINGLE_VALUED_DOUBLE) {
                return DOUBLE_CONVERTER;
            }

            return IDENTITY_CONVERTER;
        }

        if (psType <= PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_HISTORY) {
            switch (psType) {
                case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREEPATH:
                case PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_HISTORY:
                    return STRING_CONVERTER;

                case PSFieldDefinitionTypeEnum.SINGLE_VALUED_INTEGER_TREEID:
                    return INT_CONVERTER;
            }

            return IDENTITY_CONVERTER;
        }

        if ((psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREE_NODE_NAME)
            && (psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_HTML)
            && (psType != PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREE_NODE_TYPE)) {
            return IDENTITY_CONVERTER;
        }

        if (psType == PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_HTML) {
            return HTML_CONVERTER;
        }

        return STRING_CONVERTER;
    }
}
