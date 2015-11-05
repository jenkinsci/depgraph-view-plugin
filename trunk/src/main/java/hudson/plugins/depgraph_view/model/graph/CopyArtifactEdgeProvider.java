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
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static hudson.plugins.depgraph_view.model.graph.ProjectNode.node;

/**
 * Provides {@link CopyArtifactEdge}s by inspecting the configuration of the {@link CopyArtifact} Plugin.
 */
public class CopyArtifactEdgeProvider implements EdgeProvider {

    private boolean copyartifactIsInstalled;

    @Inject
    public CopyArtifactEdgeProvider(Jenkins jenkins) {
        copyartifactIsInstalled = jenkins.getPlugin("copyartifact") != null;
    }

    @Override
    public Iterable<Edge> getEdgesIncidentWith(AbstractProject<?, ?> project) {
        Set<Edge> artifactEdges = Sets.newHashSet();

        if (copyartifactIsInstalled) {
            if(project instanceof FreeStyleProject) {

                FreeStyleProject proj = (FreeStyleProject) project;
                List<Builder> builders = proj.getBuilders();

                for (Builder builder : builders) {

                    if (builder instanceof CopyArtifact) {

                        CopyArtifact caBuilder = (CopyArtifact) builder;
                        String projectName = caBuilder.getProjectName();
                        Jenkins jenkins = Jenkins.getInstance();
                        AbstractProject<?,?> projectFromName = jenkins.getItem(projectName, project.getParent(), AbstractProject.class);

                        if (projectFromName != null) {
                            artifactEdges.add(
                                    new CopyArtifactEdge(node(projectFromName), node(project)));
                        }
                    }
                }
            }
        }
        return artifactEdges;
    }
}
