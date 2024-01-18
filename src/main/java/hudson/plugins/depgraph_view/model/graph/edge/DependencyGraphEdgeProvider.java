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

package hudson.plugins.depgraph_view.model.graph.edge;

import hudson.model.AbstractProject;
import hudson.model.DependencyGraph;
import hudson.model.Job;
import jenkins.model.Jenkins;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link EdgeProvider} yielding the dependencies of the Jenkins dependency graph.
 */
public class DependencyGraphEdgeProvider implements EdgeProvider {

    private DependencyGraph dependencyGraph;

    @Inject
    public DependencyGraphEdgeProvider(Jenkins jenkins) {
        dependencyGraph = jenkins.getDependencyGraph();
    }

    @Override
    public Iterable<Edge> getUpstreamEdgesIncidentWith(Job<?, ?> project) {
        List<DependencyGraph.Dependency> dependencies = new ArrayList<DependencyGraph.Dependency>();
        if (project instanceof AbstractProject<?, ?>) {
            dependencies.addAll(dependencyGraph.getUpstreamDependencies((AbstractProject<?, ?>)project));
        }
        return getEdges(dependencies);
    }

    @Override
    public Iterable<Edge> getDownstreamEdgesIncidentWith(Job<?, ?> project) {
        List<DependencyGraph.Dependency> dependencies = new ArrayList<DependencyGraph.Dependency>();
        if (project instanceof AbstractProject<?, ?>) {
            dependencies.addAll(dependencyGraph.getDownstreamDependencies((AbstractProject<?, ?>)project));
        }
        return getEdges(dependencies);
    }

    private List<Edge> getEdges(List<DependencyGraph.Dependency> dependencies) {
        List<Edge> edges = new ArrayList<Edge>();
        for (DependencyGraph.Dependency dependency : dependencies) {
            edges.add(new BuildTriggerEdge(dependency.getUpstreamProject(), dependency.getDownstreamProject()));
        }
        return edges;
    }

}
