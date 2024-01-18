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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.Run;
import hudson.tasks.BuildTrigger;
import jenkins.model.Jenkins;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.support.steps.build.BuildUpstreamNodeAction;

/**
 * {@link EdgeProvider} yielding the dependencies of the Jenkins {@link BuildTrigger} publisher.
 */
public class BuildTriggerEdgeProvider implements EdgeProvider {

	private final Jenkins jenkins;
	private final Logger LOGGER = Logger.getLogger(BuildTriggerEdgeProvider.class.getName());

	@Inject
	public BuildTriggerEdgeProvider(Jenkins jenkins) {
		this.jenkins = jenkins;
	}

	@Override
	public Iterable<Edge> getUpstreamEdgesIncidentWith(Job<?, ?> project) {
		List<Edge> edges = new ArrayList<>();
		for (AbstractProject<?, ?> upstream : jenkins.allItems(AbstractProject.class)) {
			BuildTrigger buildTrigger = upstream.getPublishersList().get(BuildTrigger.class);
			if (buildTrigger != null
					&& Items.fromNameList(upstream.getParent(), buildTrigger.getChildProjectsValue(), Job.class)
							.contains(project)) {
				edges.add(new BuildTriggerEdge(upstream, project));
			}
		}
		return edges;
	}

	@Override
	public Iterable<Edge> getDownstreamEdgesIncidentWith(Job<?, ?> project) {
		List<Edge> edges = new ArrayList<>();
		if (project instanceof AbstractProject<?, ?>) {
			BuildTrigger buildTrigger = ((AbstractProject<?, ?>) project).getPublishersList().get(BuildTrigger.class);
			if (buildTrigger != null) {
				for (Job<?, ?> downstream : Items.fromNameList(project.getParent(),
						buildTrigger.getChildProjectsValue(), Job.class)) {
					edges.add(new BuildTriggerEdge(project, downstream));
				}
			}
		} else if (project instanceof WorkflowJob) {
			Run<?,?> lastRun = project.getLastSuccessfulBuild();
			if (lastRun == null) {
				lastRun = project.getLastBuild();
			}
			if (lastRun != null) {
				for (Action action : lastRun.getAllActions()) {
					if (action instanceof BuildUpstreamNodeAction) {
						BuildUpstreamNodeAction buna = (BuildUpstreamNodeAction) action;
						Run<?,?> upstreamRun = Run.fromExternalizableId(buna.getUpstreamRunId());
						if (upstreamRun != null) {
							edges.add(new BuildTriggerEdge(upstreamRun.getParent(), project));
						}
					}
				}
			} else {
				LOGGER.log(Level.FINE, "Not graphing edges for project " + project.getFullName() + " because it has not been built yet.");
			}
		}
		return edges;
	}

}
