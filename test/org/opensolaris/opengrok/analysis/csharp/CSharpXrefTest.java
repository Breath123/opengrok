/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017, Chris Fraire <cfraire@me.com>.
 */

package org.opensolaris.opengrok.analysis.csharp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;

import org.opensolaris.opengrok.analysis.CtagsReader;
import org.opensolaris.opengrok.analysis.Definitions;
import org.opensolaris.opengrok.analysis.FileAnalyzer;
import org.opensolaris.opengrok.analysis.WriteXrefArgs;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.opensolaris.opengrok.analysis.Xrefer;
import static org.opensolaris.opengrok.util.CustomAssertions.assertLinesEqual;
import static org.opensolaris.opengrok.util.StreamUtils.copyStream;

/**
 * Tests the {@link CSharpXref} class.
 */
public class CSharpXrefTest {

    @Test
    public void sampleTest() throws IOException {
        writeAndCompare("org/opensolaris/opengrok/analysis/csharp/sample.cs",
            "org/opensolaris/opengrok/analysis/csharp/sample_xref.html",
            getTagsDefinitions(), 209);
    }

    @Test
    public void shouldCloseTruncatedStringSpan() throws IOException {
        writeAndCompare("org/opensolaris/opengrok/analysis/csharp/truncated.cs",
            "org/opensolaris/opengrok/analysis/csharp/truncated_xref.html",
            null, 1);
    }

    private void writeAndCompare(String sourceResource, String resultResource,
        Definitions defs, int expLOC) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        InputStream res = getClass().getClassLoader().getResourceAsStream(
            sourceResource);
        assertNotNull(sourceResource + " should get-as-stream", res);
        int actLOC = writeCsharpXref(new PrintStream(baos), res, defs);
        res.close();

        InputStream exp = getClass().getClassLoader().getResourceAsStream(
            resultResource);
        assertNotNull(resultResource + " should get-as-stream", exp);
        byte[] expbytes = copyStream(exp);
        exp.close();
        baos.close();

        String ostr = new String(baos.toByteArray(), "UTF-8");
        String gotten[] = ostr.split("\n");

        String estr = new String(expbytes, "UTF-8");
        String expected[] = estr.split("\n");

        assertLinesEqual("CSharp xref", expected, gotten);
        assertEquals("CSharp LOC", expLOC, actLOC);
    }

    private int writeCsharpXref(PrintStream oss, InputStream iss,
        Definitions defs) throws IOException {

        oss.print(getHtmlBegin());

        Writer sw = new StringWriter();
        CSharpAnalyzerFactory fac = new CSharpAnalyzerFactory();
        FileAnalyzer analyzer = fac.getAnalyzer();
        analyzer.setScopesEnabled(true);
        analyzer.setFoldingEnabled(true);
        WriteXrefArgs wargs = new WriteXrefArgs(
            new InputStreamReader(iss, "UTF-8"), sw);
        wargs.setDefs(defs);
        Xrefer xref = analyzer.writeXref(wargs);
        oss.print(sw.toString());

        oss.print(getHtmlEnd());
        return xref.getLOC();
    }

    private Definitions getTagsDefinitions() throws IOException {
        InputStream res = getClass().getClassLoader().getResourceAsStream(
            "org/opensolaris/opengrok/analysis/csharp/sampletags");
        assertNotNull("though sampletags should stream,", res);

        BufferedReader in = new BufferedReader(new InputStreamReader(
            res, "UTF-8"));

        CtagsReader rdr = new CtagsReader();
        String line;
        while ((line = in.readLine()) != null) {
            rdr.readLine(line);
        }
        return rdr.getDefinitions();
    }

    private static String getHtmlBegin() {
        return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "<meta charset=\"UTF-8\">\n" +
            "<title>sampleFile - OpenGrok cross reference" +
            " for /sampleFile</title></head><body>\n";
    }

    private static String getHtmlEnd() {
        return "</body>\n" +
            "</html>\n";
    }
}
