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

import com.google.common.collect.Sets;
import edu.uci.ics.jung.algorithms.filters.VertexPredicateFilter;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import org.apache.commons.collections15.Predicate;

import java.util.Collection;
import java.util.Set;

/**
 * The dependency graph which should be drawn.
 */
public class DependencyGraph {

    private DirectedSparseMultigraph<ProjectNode, Edge> graph = new DirectedSparseMultigraph<ProjectNode, Edge>();

    public void addEdge(Edge edge) {
        graph.addEdge(edge, edge.getNodes());
    }

    public <T extends Edge> Set<ProjectNode> addEdgesWithNodes(Iterable<T> edges) {
        Set<ProjectNode> newNodes = Sets.newHashSet();
        for (T edge : edges) {
            for (ProjectNode node: edge.getNodes()) {
                if (!graph.containsVertex(node)) {
                    graph.addVertex(node);
                    newNodes.add(node);
                }
            }
            addEdge(edge);
        }
        return newNodes;
    }

    public <T extends ProjectNode> void addNodes(Iterable<T> nodes) {
        for (T node : nodes) {
            graph.addVertex(node);
        }
    }

    public Collection<Edge> getEdges() {
        return graph.getEdges();
    }

    public Collection<Edge> findEdgeSet(ProjectNode from, ProjectNode to) {
        return graph.findEdgeSet(from, to);
    }

    public Collection<ProjectNode> getNodes() {
        return graph.getVertices();
    }

    public Collection<ProjectNode> getIsolatedNodes() {
        return new VertexPredicateFilter<ProjectNode, Edge>(new Predicate<ProjectNode>() {
            @Override
            public boolean evaluate(ProjectNode projectNode) {
                return graph.degree(projectNode) == 0;
            }
        }).transform(graph).getVertices();
    }

    public Graph<ProjectNode, Edge> getGraph() {
        return graph;
    }

}
