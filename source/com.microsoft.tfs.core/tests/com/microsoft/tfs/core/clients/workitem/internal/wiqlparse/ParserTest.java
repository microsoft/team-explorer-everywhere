// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class ParserTest extends TestCase {

    // --- TEST LEXEMS --
    public void testParseLexemsCharNumber() {
        final List list = Parser.parseLexems("a123"); //$NON-NLS-1$
        assertEquals("|a123", joinValues(list)); //$NON-NLS-1$
    }

    public void testParseLexems() {
        final String s = " 0 00 -1 0.2 0.e-10 3.14e10  " //$NON-NLS-1$
            + "'test' 'test''test' 'test\"test' \"test'test\" \"test\"\"test\" [test test]" //$NON-NLS-1$
            + "test a123 [123] [1 2 3] @aBc@a12@12A@123@@" //$NON-NLS-1$
            + "()[-]{}<>||&&|&--++<==>=<==!==!.,"; //$NON-NLS-1$

        final String expected = "|0|00|-1|0.2|0.e-10|3.14e10|" //$NON-NLS-1$
            + "test|test'test|test\"test|test'test|test\"test|test test|" //$NON-NLS-1$
            + "test|a123|123|1 2 3|aBc|a12|12A|123|||" //$NON-NLS-1$
            + "(|)|-|{|}|<>||||&&|||&|-|-|+|+|<=|=>|=<|==|!=|=|!|.|,"; //$NON-NLS-1$

        final List a = Parser.parseLexems(s);
        assertEquals(expected, joinValues(a));

        // Verify toString
        for (final Iterator it = a.iterator(); it.hasNext();) {
            final NodeItem n = (NodeItem) it.next();

            // Try to parse back toString representation to ensure it is the
            // same
            List list = Parser.parseLexems(n.toString());
            assertEquals(list.size(), 1);
            assertTrue(list.get(0) instanceof NodeItem);
            NodeItem n2 = (NodeItem) list.get(0);
            assertEquals(n.toString(), n2.toString());
            assertEquals(n.getValue(), n2.getValue());

            // Verify string offsets
            assertTrue(n.getStartOffset() != -1);
            final String substr = s.substring(n.getStartOffset(), n.getEndOffset());

            list = Parser.parseLexems(substr);
            assertEquals(list.size(), 1);
            assertTrue(list.get(0) instanceof NodeItem);
            n2 = (NodeItem) list.get(0);
            assertEquals(n.toString(), n2.toString());
            assertEquals(n.getValue(), n2.getValue());
        }

        checkLexemError(" 'test", SyntaxError.EXPECTING_CLOSING_QUOTE, "'test'"); //$NON-NLS-1$ //$NON-NLS-2$
        checkLexemError("\"test", SyntaxError.EXPECTING_CLOSING_QUOTE, "'test'"); //$NON-NLS-1$ //$NON-NLS-2$
        checkLexemError("[ 1 2 3 ", SyntaxError.EXPECTING_CLOSING_SQUARE_BRACKET, "[ 1 2 3 ]"); //$NON-NLS-1$ //$NON-NLS-2$

    }

    // --- TEST SYNTAX ---

    public void testParseSyntax() {
        checkSyntax(
            "select A from B where C=0", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}{FieldCondition{FieldName C}{Number 0}}{}{}{}{}}", //$NON-NLS-1$
            "select A from B where C = 0"); //$NON-NLS-1$

        checkSyntax(
            "select A from B where C=1+2", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}{FieldCondition{FieldName C}{Arithmetic{Number 1}{Number 2}}}{}{}{}{}}", //$NON-NLS-1$
            "select A from B where C = 1 + 2"); //$NON-NLS-1$

        checkSyntax(
            "select A,B,C,D,[1],[2] from B order by AA,BB asc,[-] desc", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}{FieldName B}{FieldName C}{FieldName D}{FieldName [1]}{FieldName [2]}}" //$NON-NLS-1$
                + "{TableName B}{}{}{OrderFieldList{FieldName AA}{FieldName BB asc}{FieldName [-] desc}}{}{}}", //$NON-NLS-1$
            "select A, B, C, D, [1], [2] from B order by AA, BB asc, [-] desc"); //$NON-NLS-1$

        checkSyntax(
            "select A from B where C=1 and C<>2 or C>-1 and C<=2 and not D under '/' and D not under '/'", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}" //$NON-NLS-1$
                + "{Or{And{FieldCondition{FieldName C}{Number 1}}{FieldCondition{FieldName C}{Number 2}}}" //$NON-NLS-1$
                + "{And{FieldCondition{FieldName C}{Number -1}}{FieldCondition{FieldName C}{Number 2}}" //$NON-NLS-1$
                + "{Not{FieldCondition{FieldName D}{String '/'}}}" //$NON-NLS-1$
                + "{Not{FieldCondition{FieldName D}{String '/'}}}}}{}{}{}{}}", //$NON-NLS-1$
            "select A from B where C = 1 and C <> 2 or C > -1 and C <= 2 and not D under '/' and not D under '/'"); //$NON-NLS-1$

        checkSyntax(
            "select A from B where C=1 and (C=2 or C=3) or C in (1,2,3,4,@a,@b) asof '1/1/1 GMT'", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}" //$NON-NLS-1$
                + "{Or{And{FieldCondition{FieldName C}{Number 1}}{Or{FieldCondition{FieldName C}{Number 2}}" //$NON-NLS-1$
                + "{FieldCondition{FieldName C}{Number 3}}}}{FieldCondition{FieldName C}" //$NON-NLS-1$
                + "{ValueList{Number 1}{Number 2}{Number 3}{Number 4}{Variable @a}{Variable @b}}}}{}{}{String '1/1/1 GMT'}{}}", //$NON-NLS-1$
            "select A from B where C = 1 and (C = 2 or C = 3) or C in (1, 2, 3, 4, @a, @b) asof '1/1/1 GMT'"); //$NON-NLS-1$

        checkSyntax(
            "SELECT A FROM B WHERE [1]<>'Test' AND EVER(C=2 OR C=3) or C ever IN ('A','B','C')", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}" //$NON-NLS-1$
                + "{Or{And{FieldCondition{FieldName [1]}{String 'Test'}}" //$NON-NLS-1$
                + "{Ever{Or{FieldCondition{FieldName C}{Number 2}}{FieldCondition{FieldName C}{Number 3}}}}}" //$NON-NLS-1$
                + "{Ever{FieldCondition{FieldName C}{ValueList{String 'A'}{String 'B'}{String 'C'}}}}}{}{}{}{}}", //$NON-NLS-1$
            "select A from B where [1] <> 'Test' and ever (C = 2 or C = 3) or ever C in ('A', 'B', 'C')"); //$NON-NLS-1$

        checkSyntax("select A from B", "{Select{FieldList{FieldName A}}{TableName B}{}{}{}{}{}}", "select A from B"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

        checkSyntax(
            "select A from B mode(C,D)", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}{}{}{}{}{Mode{Name C}{Name D}}}", //$NON-NLS-1$
            "select A from B mode (C, D)"); //$NON-NLS-1$

        checkSyntax(
            "select A from B where X=='X' mode(C)", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}{FieldCondition{FieldName X}{String 'X'}}{}{}{}{Mode{Name C}}}", //$NON-NLS-1$
            "select A from B where X = 'X' mode (C)"); //$NON-NLS-1$

        checkSyntax(
            "select A from B where X=='X' mode(C,D)", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}{FieldCondition{FieldName X}{String 'X'}}{}{}{}{Mode{Name C}{Name D}}}", //$NON-NLS-1$
            "select A from B where X = 'X' mode (C, D)"); //$NON-NLS-1$

        checkSyntax(
            "select A from B where X==true", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}{FieldCondition{FieldName X}{BoolValue True}}{}{}{}{}}", //$NON-NLS-1$
            "select A from B where X = True"); //$NON-NLS-1$

        checkSyntax(
            "select A from B where X==false", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}{FieldCondition{FieldName X}{BoolValue False}}{}{}{}{}}", //$NON-NLS-1$
            "select A from B where X = False"); //$NON-NLS-1$

        checkSyntax(
            "select A from B where C = D", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}{FieldCondition{FieldName C}{FieldName D}}{}{}{}{}}", //$NON-NLS-1$
            "select A from B where C = D"); //$NON-NLS-1$

        checkSyntax(
            "select A from B where X in (@Today, @Today - 1)", //$NON-NLS-1$
            "{Select{FieldList{FieldName A}}{TableName B}{FieldCondition{FieldName X}{ValueList{Variable @Today}{Arithmetic{Variable @Today}{Number 1}}}}{}{}{}{}}", //$NON-NLS-1$
            "select A from B where X in (@Today, @Today - 1)"); //$NON-NLS-1$

        // Test Errors
        checkSyntaxError("A from B", SyntaxError.EXPECTING_SELECT, "A"); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B orderby C", SyntaxError.EXPECTING_END_OF_STRING, "orderby"); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B order C", SyntaxError.EXPECTING_BY, "C"); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B from C", SyntaxError.DUPLICATE_FROM, ""); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B where C=1 where D=2", SyntaxError.DUPLICATE_WHERE, ""); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B where C=1 order by A order by B", SyntaxError.DUPLICATE_ORDER_BY, ""); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B where C=1 D", SyntaxError.EXPECTING_END_OF_STRING, "D"); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B where C", SyntaxError.EXPECTING_COMPARISON_OPERATOR, ""); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B where not ever C", SyntaxError.EXPECTING_COMPARISON_OPERATOR, ""); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B where C not under", SyntaxError.EXPECTING_VALUE, ""); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B where A=0 order by A acs", SyntaxError.EXPECTING_END_OF_STRING, "acs"); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A where A=0", SyntaxError.FROM_IS_NOT_SPECIFIED, ""); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B mode A", SyntaxError.EXPECTING_LEFT_BRACKET, ""); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B mode()", SyntaxError.EXPECTING_MODE, ""); //$NON-NLS-1$ //$NON-NLS-2$
        checkSyntaxError("select A from B mode(123)", SyntaxError.EXPECTING_MODE, ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    // --- Helper Methods ----

    private String nodeDump(final Node n) {
        String s = "{"; //$NON-NLS-1$
        if (n != null) {
            s = s + n.getNodeType().toString();
            if (n instanceof NodeItem) {
                s = s + " " + n.toString(); //$NON-NLS-1$
            }
            for (int i = 0; i < n.getCount(); i++) {
                s = s + nodeDump(n.getItem(i));
            }
        }
        return s + "}"; //$NON-NLS-1$
    }

    private String joinValues(final List a) {
        final StringBuffer sb = new StringBuffer();
        for (final Iterator it = a.iterator(); it.hasNext();) {
            final NodeItem i = (NodeItem) it.next();
            sb.append("|"); //$NON-NLS-1$
            sb.append(i.getValue());
        }
        return sb.toString();
    }

    private void checkLexemError(final String s, final SyntaxError errCode, final String errNode) {
        try {
            Parser.parseLexems(s);
            fail("Parser does not throw syntax error."); //$NON-NLS-1$
        } catch (final SyntaxException x) {
            assertTrue("Error code not as expected", x.getSyntaxError() == errCode); //$NON-NLS-1$
            assertTrue("Error node not as expected", errNode.equals(x.getNode().toString())); //$NON-NLS-1$
        }
    }

    public void checkSyntax(final String s, final String dump, final String res) {
        final Node node = Parser.parseSyntax(s);
        final String tmp = nodeDump(node);

        assertEquals("Node dumps not as expected", dump, tmp); //$NON-NLS-1$

        final String s2 = node.toString();
        assertEquals("Failed before bind", res, s2); //$NON-NLS-1$

        // verify Bind
        node.bind(null, null, null);
        assertEquals("Failed after bind", res, node.toString()); //$NON-NLS-1$

        // check toString() is equivalent
        final Node node2 = Parser.parseSyntax(s2);
        assertEquals(s2, node2.toString());

        // check node by offset
        for (int i = 0; i < s.length(); i++) {
            // TODO
        }
    }

    private void checkSyntaxError(final String s, final SyntaxError errCode, final String errNode) {
        try {
            final Node node = Parser.parseSyntax(s);
            node.bind(null, null, null);
            fail("No syntax error raised when expected"); //$NON-NLS-1$
        } catch (final SyntaxException x) {
            assertTrue("Error code not as expected", x.getSyntaxError() == errCode); //$NON-NLS-1$
            if (errNode != "") //$NON-NLS-1$
            {
                assertTrue("Error node not as expected", x.getNode().toString().equals(errNode)); //$NON-NLS-1$
            }
        }
    }

}
