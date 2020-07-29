/*
 *   Copyright 2020 Rosemoe
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.github.rosemoe.editor.langs.s5droid;

import java.util.List;
import java.util.ArrayList;

/**
 * S5droid code block tree
 * This is used to analyze auto complete
 *
 * @author Rose
 */
public class S5droidTree {
    public final Node root;

    private Node curr;

    /**
     * Create a new S5droid code block tree
     */
    public S5droidTree() {
        root = new Node();
        root.startLine = 0;
        root.endLine = Integer.MAX_VALUE;
        root.isBlock = true;
        root.children = new ArrayList<>(256);
        curr = root;
    }

    /**
     * Enter a new code block
     *
     * @param line start line of code block entering
     */
    public void enterCodeBlock(int line) {
        Node sub = new Node();
        sub.startLine = line;
        sub.endLine = 0;
        if (sub.children == null) {
            sub.children = new ArrayList<>(16);
        }
        sub.isBlock = true;
        sub.parent = curr;
        curr.children.add(sub);
        curr = sub;
    }

    /**
     * Exit current code block
     *
     * @param line end line of code block exiting
     * @return The node of code block exited
     */
    public Node exitCodeBlock(int line) {
        curr.endLine = line;
        Node block = curr;
        boolean empty = curr.children.isEmpty();
        curr = curr.parent;
        if (curr == null) {
            curr = root;
        }
        if (empty) {
            curr.children.remove(block);
        }
        return block;
    }

    /**
     * Add a new variant in current code block
     *
     * @param line The line position of variant
     * @param name The name of this variant
     * @param type The type of this variant
     */
    public void addVariant(int line, String name, String type) {
        Node var = new Node();
        var.startLine = line;
        var.endLine = line;
        var.isBlock = false;
        var.varName = name;
        var.varType = type;
        curr.children.add(var);
    }

    /**
     * The model of code block and variant
     */
    public static class Node {

        public int startLine = 0;
        public int endLine = 0;
        public List<Node> children = null;
        public boolean isBlock = false;
        public String varName = null;
        public String varType = null;
        public Node parent = null;

    }

}

