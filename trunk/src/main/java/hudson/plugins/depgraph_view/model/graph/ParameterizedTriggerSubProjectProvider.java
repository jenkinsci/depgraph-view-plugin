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

import com.google.common.base.Preconditions;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static hudson.plugins.depgraph_view.model.graph.ProjectNode.node;

/**
 * Provides subprojects given by the TriggerBuilder of the ParameterizedTriggerPlugin
 */
public class ParameterizedTriggerSubProjectProvider implements SubProjectProvider {

    private final boolean isParameterizedTriggerPluginInstalled;

    @Inject
    public ParameterizedTriggerSubProjectProvider(Jenkins jenkins) {
        isParameterizedTriggerPluginInstalled = jenkins.getPlugin("parameterized-trigger") != null;
    }

    @Override
    public Iterable<ProjectNode> getSubProjectsOf(AbstractProject<?, ?> project) {
        Preconditions.checkNotNull(project);
        if (!isParameterizedTriggerPluginInstalled) {
            return Collections.emptyList();
        }

        List<ProjectNode> subProjects = new ArrayList<ProjectNode>();
        if(project instanceof FreeStyleProject) {
            FreeStyleProject proj = (FreeStyleProject) project;
            List<Builder> builders = proj.getBuilders();
            for (Builder builder : builders) {
                if (builder instanceof TriggerBuilder) {
                    TriggerBuilder tBuilder = (TriggerBuilder) builder;
                    for (BlockableBuildTriggerConfig config : tBuilder.getConfigs()) {
                        for (AbstractProject<?,?> abstractProject : config.getProjectList(null)) {
                            subProjects.add(node(abstractProject) );
                        }
                    }
                }
            }
        }
        return subProjects;
    }
}
