// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.email;

import java.text.MessageFormat;

import com.microsoft.tfs.util.Messages;
import com.microsoft.tfs.util.StringUtil;

public class EmailAddressParser {

    public static final char BACKSLASH = '\\';
    public static final char QUOTE = '\"';
    public static final char DOT = '.';
    public static final char AT = '@';
    public static final char HYPHEN = '-';
    public static final String ALLOWED_SPECIALS = "!#$%&'*+-/=?^_`.{|}~"; //$NON-NLS-1$
    public static final String ALLOWED_QUOTED_SPECIALS = "(),:;<> "; //$NON-NLS-1$
    public static final String REQUIRE_QUOTING = "\",[]@\\"; //$NON-NLS-1$
    public static final String MANY_DOTS = ".."; //$NON-NLS-1$

    public static final char BOL = (char) -2;
    public static final char EOL = (char) -1;

    private String buff;
    private String emailAddress;

    private String errorMessage = null;

    private boolean inQuote = false;

    private int idx;
    private int lastQuoteIdx;
    private char curRawChar;
    private char prevRawChar;
    private char curChar;
    private char prevChar;

    public EmailAddressParser() {
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean parse(final String emailAddress) {
        this.emailAddress = StringUtil.isNullOrEmpty(emailAddress) ? StringUtil.EMPTY : emailAddress.trim();
        this.buff = this.emailAddress + EOL;

        this.errorMessage = null;

        this.inQuote = false;

        this.idx = -1;
        this.lastQuoteIdx = -1;
        this.curRawChar = BOL;
        this.prevRawChar = BOL;
        this.curChar = BOL;
        this.prevChar = BOL;

        if (this.emailAddress.length() == 0) {
            return true;
        }

        if (this.emailAddress.length() > 256) {
            errorMessage = Messages.getString("EmailAddressParser.AddressTooLong"); //$NON-NLS-1$
            return false;
        }

        if (parseLocalPart()) {
            if (curChar == QUOTE) {
                if (getRawChar() == EOL) {
                    final String errorMessageFormat =
                        Messages.getString("EmailAddressParser.UnexpectedEolInQuoteFormat"); //$NON-NLS-1$
                    errorMessage = MessageFormat.format(errorMessageFormat, curChar, lastQuoteIdx + 1);
                    return false;
                }
                ;
            }

            if (curRawChar == EOL) {
                errorMessage = Messages.getString("EmailAddressParser.MissingDomainpart"); //$NON-NLS-1$
                return false;
            }

            if (curRawChar != AT) {
                final String errorMessageFormat = Messages.getString("EmailAddressParser.UnallowedCharacterFormat"); //$NON-NLS-1$
                errorMessage = MessageFormat.format(errorMessageFormat, curRawChar, idx + 1);
                return false;
            }

            return parseDomainPart();
        }

        return false;
    }

    private boolean parseLocalPart() {
        /*
         * Some additional validation will be required after all quotes/escapes
         * are handled. Let's collect the transformed local part in SB and check
         * it before return.
         */
        final StringBuilder sb = new StringBuilder(buff.length());

        do {
            if (getChar() == EOL) {
                if (inQuote) {
                    final String errorMessageFormat =
                        Messages.getString("EmailAddressParser.UnexpectedEolInQuoteFormat"); //$NON-NLS-1$
                    errorMessage = MessageFormat.format(errorMessageFormat, lastQuoteIdx + 1);
                } else {
                    errorMessage = Messages.getString("EmailAddressParser.UnexpectedEol"); //$NON-NLS-1$
                }

                return false;
            }

            if (curChar == QUOTE && idx == 0) {
                inQuote = !inQuote;
                lastQuoteIdx = idx;
                continue;
            }

            if (isEndOfLocalPart()) {
                break;
            }

            if (idx > 64) {
                errorMessage = Messages.getString("EmailAddressParser.LocalPartTooLong"); //$NON-NLS-1$
                return false;
            }

            if (isAllowed(curChar)) {
                sb.append(curChar);
                continue;
            }

            final String errorMessageFormat = Messages.getString("EmailAddressParser.UnallowedCharacterFormat"); //$NON-NLS-1$
            errorMessage = MessageFormat.format(errorMessageFormat, curChar, idx + 1);
            return false;

        } while (true);

        if (sb.length() == 0) {
            errorMessage = Messages.getString("EmailAddressParser.EmptyLocalpart"); //$NON-NLS-1$
            return false;
        }

        if (sb.charAt(0) == DOT) {
            errorMessage = Messages.getString("EmailAddressParser.DotAtBeginning"); //$NON-NLS-1$
            return false;
        }

        if (sb.charAt(sb.length() - 1) == DOT) {
            errorMessage = Messages.getString("EmailAddressParser.DotAtEnd"); //$NON-NLS-1$
            return false;
        }

        if (sb.indexOf(MANY_DOTS) > -1) {
            errorMessage = Messages.getString("EmailAddressParser.ManyDots"); //$NON-NLS-1$
            return false;
        }

        return true;
    }

    private boolean parseDomainPart() {

        if (getRawChar() == EOL) {
            errorMessage = Messages.getString("EmailAddressParser.MissingDomainpart"); //$NON-NLS-1$
            return false;
        }

        final String domainPart = emailAddress.toLowerCase().substring(idx);

        if (domainPart.length() > 253) {
            errorMessage = Messages.getString("EmailAddressParser.DomainTooLong"); //$NON-NLS-1$
            return false;
        }

        final String[] labels = domainPart.split("\\.", -1); //$NON-NLS-1$

        if (labels.length > 127) {
            errorMessage = Messages.getString("EmailAddressParser.TooManyDomainLevels"); //$NON-NLS-1$
            return false;
        }

        if (StringUtil.isNullOrEmpty(labels[0])) {
            errorMessage = Messages.getString("EmailAddressParser.DomainCannotStartWithDot"); //$NON-NLS-1$
            return false;
        }

        if (StringUtil.isNullOrEmpty(labels[labels.length - 1])) {
            errorMessage = Messages.getString("EmailAddressParser.DomainCannotEndWithDot"); //$NON-NLS-1$
            return false;
        }

        for (final String label : labels) {
            if (StringUtil.isNullOrEmpty(label)) {
                errorMessage = Messages.getString("EmailAddressParser.SubdomainCannotBeEmpty"); //$NON-NLS-1$
                return false;
            }

            if (label.length() > 63) {
                errorMessage = Messages.getString("EmailAddressParser.SubdomainTooLong"); //$NON-NLS-1$
                return false;
            }

            if (label.charAt(0) == HYPHEN) {
                errorMessage = Messages.getString("EmailAddressParser.SubdomainCannotStartWithHyphen"); //$NON-NLS-1$
                return false;
            }

            if (label.charAt(label.length() - 1) == HYPHEN) {
                errorMessage = Messages.getString("EmailAddressParser.SubdomainCannotEndWithHyphen"); //$NON-NLS-1$
                return false;
            }

            for (char c : label.toCharArray()) {
                if (!Character.isLetter(c) && !Character.isDigit(c) && c != HYPHEN) {
                    errorMessage = Messages.getString("EmailAddressParser.WrongCharacterInSubdomain"); //$NON-NLS-1$
                    return false;
                }
            }
        }

        return true;
    }

    private char getRawChar() {
        if (curRawChar == EOL) {
            return EOL;
        }

        prevRawChar = curRawChar;
        curRawChar = buff.charAt(++idx);

        return curRawChar;
    }

    private char getChar() {
        if (curChar == EOL) {
            return EOL;
        }

        prevChar = curChar;

        if (getRawChar() == EOL) {
            return curChar = EOL;
        }

        if (curRawChar == BACKSLASH && inQuote) {
            return curChar = getRawChar();
        }

        return curChar = curRawChar;
    }

    private boolean isAllowed(final char c) {
        if (isAlphaDigit(c)) {
            return true;
        }

        if (ALLOWED_SPECIALS.indexOf(c) > -1) {
            return true;
        }

        if (inQuote) {
            if (ALLOWED_QUOTED_SPECIALS.indexOf(c) > -1) {
                return true;
            }

            if (REQUIRE_QUOTING.indexOf(c) > -1 && prevRawChar == BACKSLASH) {
                return true;
            }
        }

        return false;
    }

    private boolean isAlphaDigit(final char c) {
        return '0' <= c && c <= '9' || 'a' <= c && c <= 'z' || 'A' <= c && c <= 'Z';
    }

    private boolean isQuotedChar() {
        return prevRawChar == BACKSLASH;
    }

    private boolean isEndOfLocalPart() {
        if (inQuote) {
            return curChar == QUOTE && !isQuotedChar();
        } else {
            return curChar == AT && !isQuotedChar();
        }
    }
}
