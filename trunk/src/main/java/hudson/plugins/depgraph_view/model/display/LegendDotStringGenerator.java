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

import com.google.common.collect.ArrayListMultimap;
import hudson.plugins.depgraph_view.model.graph.DependencyGraph;
import hudson.plugins.depgraph_view.model.graph.ProjectNode;

/**
 * Generates the legend in dot format.
 */
public class LegendDotStringGenerator extends AbstractDotStringGenerator {

    public LegendDotStringGenerator() {
        super(new DependencyGraph(), ArrayListMultimap.<ProjectNode, ProjectNode>create());
    }

    @Override
    public String generate() {
            /**** Build the dot source file ****/
            StringBuilder builder = new StringBuilder();

            builder.append("digraph {\n");
            builder.append("node [shape=box, style=rounded];\n");

            builder.append(cluster("Legend", legend()));

            builder.append("}");
            return builder.toString();
    }

    private String legend() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("label=\"Legend:\" labelloc=t centered=false color=black node [shape=plaintext]")
                .append("\"Dependency Graph\"\n")
                .append("\"Copy Artifact\"\n")
                .append("\"Sub-Project\"\n")
                .append("node [style=invis]\n")
                .append("a [label=\"\"] b [label=\"\"]")
                .append(" c [fillcolor=" + escapeString(subProjectColor) + " style=filled fontcolor="
                        + escapeString(subProjectColor) + "]\n")
                .append("a -> b [style=invis]\n")
                .append("{rank=same a -> \"Dependency Graph\" [color=black style=bold minlen=2]}\n")
                .append("{rank=same b -> \"Copy Artifact\" [color=lightblue minlen=2]}\n")
                .append("{rank=same c -> \"Sub-Project\" [ style=invis]}\n");
        return stringBuilder.toString();
    }

}
