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

import jakarta.inject.Inject;

import hudson.model.Items;
import hudson.model.Job;
import hudson.model.Project;
import hudson.plugins.parameterizedtrigger.BuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

/**
 * {@link EdgeProvider} yielding the dependencies of the Parameterized Trigger Plugin {@link TriggerBuilder} builder.
 */
public class ParameterizedTriggerBuilderEdgeProvider implements EdgeProvider {

	private final Jenkins jenkins;
	private final boolean isPluginInstalled;

	@Inject
	public ParameterizedTriggerBuilderEdgeProvider(Jenkins jenkins) {
		this.jenkins = jenkins;
		isPluginInstalled = jenkins.getPlugin("parameterized-trigger") != null;
	}

	@Override
	public Iterable<Edge> getUpstreamEdgesIncidentWith(Job<?, ?> project) {
		List<Edge> edges = new ArrayList<>();
		if (!isPluginInstalled) {
			return edges;
		}
		for (Project<?, ?> upstream : jenkins.allItems(Project.class)) {
			for (Builder builder : upstream.getBuilders()) {
				if (builder instanceof TriggerBuilder) {
					for (BuildTriggerConfig config : ((TriggerBuilder) builder).getConfigs()) {
						if (Items.fromNameList(upstream.getParent(), config.getProjects(), Job.class)
								.contains(project)) {
							edges.add(new ParameterizedTriggerEdge(upstream, project));
						}
					}
				}
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
		if (project instanceof Project<?, ?>) {
			for (Builder builder : ((Project<?, ?>) project).getBuilders()) {
				if (builder instanceof TriggerBuilder) {
					for (BuildTriggerConfig config : ((TriggerBuilder) builder).getConfigs()) {
						for (Job<?, ?> downstream : Items.fromNameList(project.getParent(), config.getProjects(),
								Job.class)) {
							edges.add(new ParameterizedTriggerEdge(project, downstream));
						}
					}
				}
			}
		}
		return edges;
	}

}
