/*
 * Copyright (c) 2019 Guido Grazioli
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.jenkinsci.plugins.pipeline.maven.GlobalPipelineMavenConfig;
import org.jenkinsci.plugins.pipeline.maven.dao.PipelineMavenPluginDao;
import org.jenkinsci.plugins.pipeline.maven.trigger.WorkflowJobDependencyTrigger;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.ExtensionList;
import hudson.model.Job;
import jenkins.model.Jenkins;


/**
 * {@link EdgeProvider} yielding the dependencies of the Jenkins {@link WorkflowJobDependencyTrigger} trigger.
 */
public class PipelineGraphPublisherEdgeProvider implements EdgeProvider {

    private final Jenkins jenkins;
    private final boolean isPluginInstalled;
    private final static Logger LOGGER = Logger.getLogger(PipelineGraphPublisherEdgeProvider.class.getName());

    @Inject
    public PipelineGraphPublisherEdgeProvider(Jenkins jenkins) {
        this.jenkins = jenkins;
        this.isPluginInstalled = jenkins.getPlugin("pipeline-maven") != null;
    }

    @Override
    public Iterable<Edge> getUpstreamEdgesIncidentWith(Job<?, ?> project) {
        List<Edge> edges = new ArrayList<>();
        if (!isPluginInstalled) {
            return edges;
        }
        GlobalPipelineMavenConfig globalPMConfig = ExtensionList.lookupSingleton(GlobalPipelineMavenConfig.class);
        if (globalPMConfig != null && project instanceof WorkflowJob) {
            PipelineMavenPluginDao dao = globalPMConfig.getDao();
            if (project.getLastSuccessfulBuild() != null) {
                LOGGER.log(Level.FINEST, "Project" + project.getFullName() + ", build: " + project.getLastSuccessfulBuild().getNumber());
                Map<String, Integer> upstreams = dao.listUpstreamJobs(project.getFullName(), project.getLastSuccessfulBuild().getNumber());
                for (String upstreamName : upstreams.keySet()) {
                    Job<?, ?> upstream = jenkins.getItemByFullName(upstreamName, Job.class);
                    if (upstream != null) edges.add(new MavenDependencyEdge(upstream, project));
                }
            } else {
                LOGGER.log(Level.FINE, "Project " + project.getFullName() + ": not graphing edges because it was not built yet");
            }
        }
        return edges;
    }

    @Override
    public Iterable<Edge> getDownstreamEdgesIncidentWith(Job<?, ?> project) {
        List<Edge> edges = new ArrayList<>();
        if (!isPluginInstalled) {
            return edges;
        }   
        GlobalPipelineMavenConfig globalPMConfig = ExtensionList.lookupSingleton(GlobalPipelineMavenConfig.class);
        if (globalPMConfig != null && project instanceof WorkflowJob) {
            PipelineMavenPluginDao dao = globalPMConfig.getDao();
            if (project.getLastSuccessfulBuild() != null) {
                LOGGER.log(Level.FINEST, "Project" + project.getFullName() + ", build: " + project.getLastSuccessfulBuild().getNumber());
                List<String> downstreams = dao.listDownstreamJobs(project.getFullName(), project.getLastSuccessfulBuild().getNumber());
                for (String downstreamName : downstreams) {
                    Job<?, ?> downstream = jenkins.getItemByFullName(downstreamName, Job.class);
                    if (downstream != null) edges.add(new MavenDependencyEdge(project, downstream));
                }
            } else {
                LOGGER.log(Level.FINE, "Project " + project.getFullName() + ": not graphing edges because it was not built yet");
            }
        }
        return edges;
    }

}
