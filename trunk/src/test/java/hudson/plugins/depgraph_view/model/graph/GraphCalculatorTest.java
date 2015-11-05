/*
 * Copyright (c) 2010 Stefan Wolf
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

package hudson.plugins.depgraph_view.model.graph;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildTrigger;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static hudson.plugins.depgraph_view.model.graph.ProjectNode.node;
import static org.junit.Assert.assertTrue;

public class GraphCalculatorTest {
    private FreeStyleProject project1;
    private FreeStyleProject project2;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testCalculateDepsDownstream() throws IOException {
        createProjects();
        addDependency(project1, project2);
        j.jenkins.rebuildDependencyGraph();
        DependencyGraph graph = generateGraph(project1);
        assertGraphContainsProjects(graph, project1, project2);
        assertTrue(graph.findEdgeSet(node(project1), node(project2)).size() == 1);
    }

    private void assertGraphContainsProjects(DependencyGraph graph, AbstractProject<?,?>... projects) {
        Collection<ProjectNode> projectNodes = Lists.newArrayList(GraphCalculator.abstractProjectSetToProjectNodeSet(Arrays.asList(projects)));
        assertTrue(graph.getNodes().containsAll(projectNodes));
    }

    private DependencyGraph generateGraph(AbstractProject<?,?> from) {
        return new GraphCalculator(getDependencyGraphEdgeProviders()).generateGraph(Collections.singleton(node(from)));
    }

    @Test
    public void testCalculateDepsUpstream() throws IOException {
        createProjects();
        addDependency(project1, project2);
        j.getInstance().rebuildDependencyGraph();
        DependencyGraph graph = generateGraph(project2);
        assertGraphContainsProjects(graph, project1, project2);
        assertTrue(graph.findEdgeSet(node(project1), node(project2)).size() == 1);
    }

    private void assertHasOneDependencyEdge(DependencyGraph graph, AbstractProject<?,?> from, AbstractProject<?,?> to) {
        assertTrue(graph.findEdgeSet(node(from), node(to)).size() == 1);
    }

    @Test
    public void testCalculateDepsTransitive() throws IOException {
        createProjects();
        FreeStyleProject project3 = j.createFreeStyleProject();
        addDependency(project2, project1);
        addDependency(project2, project3);
        j.getInstance().rebuildDependencyGraph();
        DependencyGraph graph = generateGraph(project1);
        assertGraphContainsProjects(graph, project1, project2, project3);
        assertHasOneDependencyEdge(graph, project2, project1);
        assertHasOneDependencyEdge(graph, project2, project3);
    }

    private void createProjects() throws IOException {
        project1 = j.createFreeStyleProject();
        project2 = j.createFreeStyleProject();
    }

    private ImmutableSet<EdgeProvider> getDependencyGraphEdgeProviders() {
        return ImmutableSet.<EdgeProvider>of(new DependencyGraphEdgeProvider(j.getInstance()));
    }

    private void addDependency(AbstractProject<?,?> project1, AbstractProject<?,?> project2) throws IOException {
        project1.getPublishersList().add(new BuildTrigger(project2.getName(), false));
    }
}
