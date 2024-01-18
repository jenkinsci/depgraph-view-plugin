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

package hudson.plugins.depgraph_view.model.graph;

import static hudson.plugins.depgraph_view.model.graph.ProjectNode.node;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import hudson.model.Item;
import hudson.model.Job;
import hudson.plugins.depgraph_view.model.graph.edge.Edge;
import hudson.plugins.depgraph_view.model.graph.edge.EdgeProvider;

/**
 * Generates the {@link DependencyGraph} given a set of {@link EdgeProvider}s.
 */
public class GraphCalculator {

    private Set<EdgeProvider> edgeProviders;

    @Inject
    public GraphCalculator(Set<EdgeProvider> edgeProviders) {
        this.edgeProviders = Sets.newHashSet(edgeProviders);
    }

    public DependencyGraph generateGraph(Iterable<ProjectNode> initialProjects) {
        DependencyGraph graph = new DependencyGraph();
        graph.addNodes(initialProjects);
        extendGraph(graph, initialProjects, e -> e::getUpstreamEdgesIncidentWith);
        extendGraph(graph, initialProjects, e -> e::getDownstreamEdgesIncidentWith);
        return graph;
    }
    
    private void extendGraph(DependencyGraph graph, Iterable<ProjectNode> fromProjects, 
            java.util.function.Function<EdgeProvider, java.util.function.Function<Job<?, ?>, Iterable<Edge>>> x) {
        List<Edge> newEdges = Lists.newArrayList();
        for (ProjectNode projectNode : fromProjects) {
            Job<?,?> project = projectNode.getProject();
            if (project.hasPermission(Item.READ)) {
                for (EdgeProvider edgeProvider : edgeProviders) {
                    Iterables.addAll(newEdges, x.apply(edgeProvider).apply(project));
                }
            }
        }
        Set<ProjectNode> newProj = graph.addEdgesWithNodes(newEdges);
        if (!newProj.isEmpty()) {
            extendGraph(graph, newProj, x);
        }
    }

    public static Iterable<ProjectNode> jobSetToProjectNodeSet(Iterable<? extends Job<?,?>> projects) {
        return Iterables.transform(projects, new Function<Job<?, ?>, ProjectNode>() {
            @Override
            public ProjectNode apply(Job<?, ?> input) {
                return input != null ? node(input) : null;
            }
        });
    }
}
