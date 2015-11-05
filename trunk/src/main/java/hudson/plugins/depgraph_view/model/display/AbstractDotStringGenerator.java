/*
 * Copyright (c) 2012 Stefan Wolf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.depgraph_view.model.display;

import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;
import hudson.plugins.depgraph_view.model.graph.DependencyGraph;
import hudson.plugins.depgraph_view.model.graph.ProjectNode;

/**
 * Base class for generating dot representations of the graph.
 */
public abstract class AbstractDotStringGenerator extends AbstractGraphStringGenerator {
    
	protected String subProjectColor = "#F0F0F0";

    protected AbstractDotStringGenerator(DependencyGraph graph, ListMultimap<ProjectNode, ProjectNode> projects2Subprojects) {
        super(graph, projects2Subprojects);
    }

    protected static String escapeString(String toEscape) {
        return "\"" + toEscape + "\"";
    }

    protected String cluster(String name, String contents, String... options) {
        StringBuilder builder = new StringBuilder();
        builder.append("subgraph cluster" + name + " {\n");
        builder.append(contents);
        builder.append(Joiner.on("\n").join(options) + "}\n");
        return builder.toString();
    }
}
