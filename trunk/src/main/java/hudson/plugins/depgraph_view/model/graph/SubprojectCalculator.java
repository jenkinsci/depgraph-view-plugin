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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;

/**
 * Used to calculate the subprojects of a project given a set of providers.
 */
public class SubprojectCalculator {

    private Set<SubProjectProvider> providers;

    @Inject
    public SubprojectCalculator(Set<SubProjectProvider> providers) {
        this.providers = providers;
    }

    public ListMultimap<ProjectNode, ProjectNode> generate(DependencyGraph graph) {
        ListMultimap<ProjectNode, ProjectNode> project2Subprojects = ArrayListMultimap.create();
        Collection<ProjectNode> nodes = graph.getNodes();
        for (ProjectNode node : nodes) {
            for (SubProjectProvider provider : providers) {
                project2Subprojects.putAll(node, provider.getSubProjectsOf(node.getProject()));
            }
        }
        return project2Subprojects;
    }
}
