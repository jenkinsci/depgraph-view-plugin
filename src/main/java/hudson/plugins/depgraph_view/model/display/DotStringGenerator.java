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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ListMultimap;

import hudson.plugins.depgraph_view.DependencyGraphProperty.DescriptorImpl;
import hudson.plugins.depgraph_view.model.graph.DependencyGraph;
import hudson.plugins.depgraph_view.model.graph.ProjectNode;
import hudson.plugins.depgraph_view.model.graph.edge.Edge;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;
import static com.google.common.base.Functions.compose;
import static com.google.common.collect.Lists.transform;

/**
 * Generates a dot string representation of the graph
 */
public class DotStringGenerator extends AbstractDotStringGenerator {

    private String rankDirection;
    private static final Function<String, String> ESCAPE = new Function<String, String>() {
        @Override
        public String apply(String from) {
            return escapeString(from);
        }
    };

    /**
     * If the regex pattern matches, the given string will be striped to the given group. 
     */
    private static final class LabelProjectFunction implements Function<String, String>{

        private Pattern pattern;
        private int group;
        private int supergroup;

        public LabelProjectFunction(Pattern pattern, int group, int supergroup) {
            this.pattern = pattern;
            this.group = group;
            this.supergroup = supergroup;
        }

        @Override
        public String apply(String name) {
            return labelProjectName(name);
        }

        // label=<<FONT POINT-SIZE="10">superscript</FONT><BR />strippedname>
        private String labelProjectName(String name) {
            final Matcher matcher = pattern.matcher(name);
            if(matcher.matches() && matcher.groupCount() >= group) {
                final StringBuilder sb = new StringBuilder();
                if (supergroup > 0 && matcher.groupCount() >= supergroup) {
                    sb.append("<FONT POINT-SIZE=\"10\">").append(matcher.group(supergroup)).append("</FONT><BR />");
                }
                sb.append(matcher.group(group));
                return sb.toString();
            }
            return name;
        }
    }

    private final LabelProjectFunction stripFunction;


    public DotStringGenerator(Jenkins jenkins, DependencyGraph graph, ListMultimap<ProjectNode, ProjectNode> projects2Subprojects) {
        super(graph, projects2Subprojects);
        int projectNameStripRegexGroup = jenkins.getDescriptorByType(DescriptorImpl.class).getProjectNameStripRegexGroup();
        final String projectNameStripRegex = jenkins.getDescriptorByType(DescriptorImpl.class).getProjectNameStripRegex();
        int projectNameSuperscriptRegexGroup = jenkins.getDescriptorByType(DescriptorImpl.class).getProjectNameSuperscriptRegexGroup();
        this.rankDirection = jenkins.getDescriptorByType(DescriptorImpl.class).getGraphRankDirection();

        Pattern nameStripPattern = null;
        try {
            nameStripPattern = Pattern.compile(projectNameStripRegex);
        } catch (Exception e) {
            nameStripPattern = Pattern.compile(".*");
        }

        stripFunction = new LabelProjectFunction(nameStripPattern, projectNameStripRegexGroup, projectNameSuperscriptRegexGroup);
    }

    public DotStringGenerator(DependencyGraph graph, ListMultimap<ProjectNode, ProjectNode> projects2Subprojects) {
        this(Jenkins.get(), graph, projects2Subprojects);
    }

    /**
     * Generates the graphviz code for the given projects and dependencies
     * @return graphviz code
     */
    @Override
    public String generate() {
        /**** Build the dot source file ****/
        StringBuilder builder = new StringBuilder();

        builder.append("digraph {\n");
        builder.append("node [shape=box, style=rounded];\n");
        builder.append("rankdir=").append(rankDirection).append(";\n");

        /**** First define all the objects and clusters ****/

        // up/downstream linked jobs
        builder.append(cluster("Main", projectsInDependenciesNodes(), "color=invis;"));

        // Stuff not linked to other stuff
        List<String> standaloneNames = transform(standaloneProjects, compose(ESCAPE, PROJECT_NAME_FUNCTION));
        builder.append(cluster("Standalone", standaloneProjectNodes(standaloneNames),"color=invis;"));

        /****Now define links between objects ****/

        // edges
        for (Edge edge : edges) {
            builder.append(dependencyToEdgeString(edge));
            builder.append(";\n");
        }

        if (!standaloneNames.isEmpty()) {
            builder.append("edge[style=\"invisible\",dir=\"none\"];\n" + Joiner.on(" -> ").join(standaloneNames) + ";\n");
            builder.append("edge[style=\"invisible\",dir=\"none\"];\n" + standaloneNames.get(standaloneNames.size() - 1) + " -> \"Dependency Graph\"");
        }

        builder.append("}");

        return builder.toString();
    }

    private String standaloneProjectNodes(List<String> standaloneNames) {
        StringBuilder builder = new StringBuilder();
        for (ProjectNode proj : standaloneProjects) {
            builder.append(projectToNodeString(proj, subJobs.get(proj)));
            builder.append(";\n");
        }
        return builder.toString();
    }

    private String projectsInDependenciesNodes() {
        StringBuilder stringBuilder = new StringBuilder();
        for (ProjectNode proj : projectsInDeps) {
            if (subJobs.containsKey(proj)) {
                stringBuilder.append(projectToNodeString(proj, subJobs.get(proj)));
            }
            else {
                stringBuilder.append(projectToNodeString(proj));
            }
            stringBuilder.append(";\n");
        }
        return stringBuilder.toString();
    }

    private String projectToNodeString(ProjectNode proj) {
        return escapeString(proj.getName()) +
                " [label=<" + stripFunction.apply(proj.getName()) + "> " +
                " href=" + getEscapedProjectUrl(proj) + 
                " style=filled " +
                proj.getBuildingShape() +
                " tooltip=" + escapeString(proj.getToolTip()) +
                " fillcolor=" + escapeString(proj.getFill()) + "]";
    }

    private String projectToNodeString(ProjectNode proj, List<ProjectNode> subprojects) {
        StringBuilder builder = new StringBuilder();
        builder.append(escapeString(proj.getName()))
                .append(" [shape=\"Mrecord\" href=")
                .append(getEscapedProjectUrl(proj))
                .append(" label=<<table border=\"0\" cellborder=\"0\" cellpadding=\"3\" bgcolor=\"white\">\n");
        builder.append(getProjectRow(proj));
        for (ProjectNode subproject : subprojects) {
            builder.append(getProjectRow(subproject, "bgcolor=" + escapeString(subProjectColor))).append("\n");
        }
        builder.append("</table>>]");
        return builder.toString();
    }

    private String getProjectRow(ProjectNode project, String... extraColumnProperties) {
        return String.format("<tr><td align=\"center\" href=%s %s>%s</td></tr>", getEscapedProjectUrl(project), 
                Joiner.on(" ").join(extraColumnProperties), stripFunction.apply(project.getName()));
    }

    private String getEscapedProjectUrl(ProjectNode proj) {
        return escapeString(proj.getProject().getAbsoluteUrl());
    }

    private String dependencyToEdgeString(Edge edge, String... options) {
        return String.format("%s -> %s [ color=%s %s ] ", escapeString(edge.source.getName()), 
                escapeString(edge.target.getName()), edge.getColor(), Joiner.on(" ").join(options));
    }

}
