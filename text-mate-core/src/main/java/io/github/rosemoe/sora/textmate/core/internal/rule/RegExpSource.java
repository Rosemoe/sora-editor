/**
 * Copyright (c) 2015-2017 Angelo ZERR.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 * <p>
 * Initial code from https://github.com/Microsoft/vscode-textmate/
 * Initial copyright Copyright (C) Microsoft Corporation. All rights reserved.
 * Initial license: MIT
 * <p>
 * Contributors:
 * - Microsoft Corporation: Initial code, written in TypeScript, licensed under MIT license
 * - Angelo Zerr <angelo.zerr@gmail.com> - translation and adaptation to Java
 */
package io.github.rosemoe.sora.textmate.core.internal.rule;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.rosemoe.sora.textmate.core.internal.oniguruma.IOnigCaptureIndex;

/**
 *
 * @see https://github.com/Microsoft/vscode-textmate/blob/master/src/rule.ts
 *
 */
public class RegExpSource {

    private static final Pattern HAS_BACK_REFERENCES = Pattern.compile("\\\\(\\d+)");
    private static final Pattern BACK_REFERENCING_END = Pattern.compile("\\\\(\\d+)");
    private static final Pattern REGEXP_CHARACTERS = Pattern
            .compile("[\\-\\\\\\{\\}\\*\\+\\?\\|\\^\\$\\.\\,\\[\\]\\(\\)\\#\\s]");

    private int ruleId;
    private boolean _hasAnchor;
    private boolean _hasBackReferences;
    private IRegExpSourceAnchorCache anchorCache;
    private String source;

    public RegExpSource(String regExpSource, int ruleId) {
        this(regExpSource, ruleId, true);
    }

    public RegExpSource(String regExpSource, int ruleId, boolean handleAnchors) {
        if (handleAnchors) {
            this._handleAnchors(regExpSource);
        } else {
            this.source = regExpSource;
            this._hasAnchor = false;
        }

        if (this._hasAnchor) {
            this.anchorCache = this._buildAnchorCache();
        }

        this.ruleId = ruleId;
        this._hasBackReferences = HAS_BACK_REFERENCES.matcher(this.source).find();

        // console.log('input: ' + regExpSource + ' => ' + this.source + ', ' +
        // this.hasAnchor);
    }

    @Override
    public RegExpSource clone() {
        return new RegExpSource(this.source, this.ruleId, true);
    }

    private void _handleAnchors(String regExpSource) {
        if (regExpSource != null) {
            int len = regExpSource.length();
            char ch;
            char nextCh;
            int lastPushedPos = 0;
            StringBuilder output = new StringBuilder();

            boolean hasAnchor = false;
            for (int pos = 0; pos < len; pos++) {
                ch = regExpSource.charAt(pos);

                if (ch == '\\') {
                    if (pos + 1 < len) {
                        nextCh = regExpSource.charAt(pos + 1);
                        if (nextCh == 'z') {
                            output.append(regExpSource.substring(lastPushedPos, pos));
                            output.append("$(?!\\n)(?<!\\n)");
                            lastPushedPos = pos + 2;
                        } else if (nextCh == 'A' || nextCh == 'G') {
                            hasAnchor = true;
                        }
                        pos++;
                    }
                }
            }

            this._hasAnchor = hasAnchor;
            if (lastPushedPos == 0) {
                // No \z hit
                this.source = regExpSource;
            } else {
                output.append(regExpSource.substring(lastPushedPos, len));
                this.source = output.toString(); // join('');
            }
        } else {
            this._hasAnchor = false;
            this.source = regExpSource;
        }
    }

    public String resolveBackReferences(String lineText, IOnigCaptureIndex[] captureIndices) {
        try {
            List<String> capturedValues = Arrays.stream(captureIndices)
                    .map(capture -> lineText.substring(capture.getStart(), capture.getEnd())).collect(Collectors.toList());
            Matcher m = BACK_REFERENCING_END.matcher(this.source);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String g1 = m.group();
                int index = Integer.parseInt(g1.substring(1, g1.length()));
                String replacement = escapeRegExpCharacters(capturedValues.size() > index ? capturedValues.get(index) : "");
                m.appendReplacement(sb, replacement);
            }
            m.appendTail(sb);
            return sb.toString();
        } catch (Throwable e) {
            //e.printStackTrace();
        }

        return lineText;
    }

    private String escapeRegExpCharacters(String value) {
        Matcher m = REGEXP_CHARACTERS.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, "\\\\\\\\" + m.group());
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private IRegExpSourceAnchorCache _buildAnchorCache() {

        // Collection<String> A0_G0_result=new ArrayList<Character>();
        // Collection<String> A0_G1_result=new ArrayList<String>();
        // Collection<String> A1_G0_result=new ArrayList<String>();
        // Collection<String> A1_G1_result=new ArrayList<String>();

        StringBuilder A0_G0_result = new StringBuilder();
        StringBuilder A0_G1_result = new StringBuilder();
        StringBuilder A1_G0_result = new StringBuilder();
        StringBuilder A1_G1_result = new StringBuilder();
        int pos;
        int len;
        char ch;
        char nextCh;

        for (pos = 0, len = this.source.length(); pos < len; pos++) {
            ch = this.source.charAt(pos);
            A0_G0_result.append(ch);
            A0_G1_result.append(ch);
            A1_G0_result.append(ch);
            A1_G1_result.append(ch);

            if (ch == '\\') {
                if (pos + 1 < len) {
                    nextCh = this.source.charAt(pos + 1);
                    if (nextCh == 'A') {
                        A0_G0_result.append('\uFFFF');
                        A0_G1_result.append('\uFFFF');
                        A1_G0_result.append('A');
                        A1_G1_result.append('A');
                    } else if (nextCh == 'G') {
                        A0_G0_result.append('\uFFFF');
                        A0_G1_result.append('G');
                        A1_G0_result.append('\uFFFF');
                        A1_G1_result.append('G');
                    } else {
                        A0_G0_result.append(nextCh);
                        A0_G1_result.append(nextCh);
                        A1_G0_result.append(nextCh);
                        A1_G1_result.append(nextCh);
                    }
                    pos++;
                }
            }
        }

        return new IRegExpSourceAnchorCache(A0_G0_result.toString(), A0_G1_result.toString(), A1_G0_result.toString(),
                A1_G1_result.toString()
                // StringUtils.join(A0_G0_result, ""),
                // StringUtils.join(A0_G1_result, ""),
                // StringUtils.join(A1_G0_result, ""),
                // StringUtils.join(A1_G1_result, "")
        );
    }

    public String resolveAnchors(boolean allowA, boolean allowG) {
        if (!this._hasAnchor) {
            return this.source;
        }

        if (allowA) {
            if (allowG) {
                return this.anchorCache.A1_G1;
            } else {
                return this.anchorCache.A1_G0;
            }
        } else {
            if (allowG) {
                return this.anchorCache.A0_G1;
            } else {
                return this.anchorCache.A0_G0;
            }
        }
    }

    public boolean hasAnchor() {
        return this._hasAnchor;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String newSource) {
        if (this.source.equals(newSource)) {
            return;
        }
        this.source = newSource;

        if (this._hasAnchor) {
            this.anchorCache = this._buildAnchorCache();
        }
    }

    public Integer getRuleId() {
        return this.ruleId;
    }

    public boolean hasBackReferences() {
        return this._hasBackReferences;
    }

}
