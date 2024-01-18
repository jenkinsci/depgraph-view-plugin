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

import static hudson.plugins.depgraph_view.model.graph.ProjectNode.node;

import java.util.Set;

import jakarta.inject.Inject;

import com.google.common.collect.Sets;

import hudson.model.Job;
import hudson.model.Project;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

/**
 * Provides {@link CopyArtifactEdge}s by inspecting the configuration of the {@link CopyArtifact} Plugin.
 */
public class CopyArtifactEdgeProvider implements EdgeProvider {

    private final Jenkins jenkins;
    private final boolean isPluginInstalled;

    @Inject
    public CopyArtifactEdgeProvider(Jenkins jenkins) {
        this.jenkins = jenkins;
        isPluginInstalled = jenkins.getPlugin("copyartifact") != null;
    }

    @Override
    public Iterable<Edge> getDownstreamEdgesIncidentWith(Job<?, ?> project) {
        Set<Edge> edges = Sets.newHashSet();
        if (!isPluginInstalled) {
            return edges;
        }
        for (Project<?, ?> downstream : jenkins.allItems(Project.class)) {
            for (Builder builder : downstream.getBuilders()) {
                if (builder instanceof CopyArtifact) {
                    Job<?,?> projectFromName = jenkins.getItem(((CopyArtifact) builder).getProjectName(),
                            downstream.getParent(), Job.class);
                    if (projectFromName == project) {
                        edges.add(new CopyArtifactEdge(node(project), node(downstream)));
                    }
                }
            }
        }
        return edges;
    }

    @Override
    public Iterable<Edge> getUpstreamEdgesIncidentWith(Job<?, ?> project) {
        Set<Edge> edges = Sets.newHashSet();
        if (!isPluginInstalled) {
            return edges;
        }
        if (project instanceof Project<?, ?>) {
            for (Builder builder : ((Project<?, ?>) project).getBuilders()) {
                if (builder instanceof CopyArtifact) {
                    Job<?,?> upstream = jenkins.getItem(((CopyArtifact) builder).getProjectName(),
                            project.getParent(), Job.class);
                    if (upstream != null) {
                        edges.add(new CopyArtifactEdge(node(upstream), node(project)));
                    }
                }
            }
        }
        return edges;
    }

}
