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

import org.apache.commons.text.StringEscapeUtils;

import com.google.common.base.Preconditions;
import hudson.model.Job;

/**
 * A Node in the DependencyGraph, which corresponds to a Project
 */
public class ProjectNode {
    private final Job<?,?> project;

    public static ProjectNode node(Job<?, ?> project) {
        return new ProjectNode(project);
    }

    public ProjectNode(Job<?, ?> project) {
        Preconditions.checkNotNull(project);
        this.project = project;
    }

    public String getBuildingShape() {
        if (project.isInQueue()) {
            return "shape=doubleoctagon";
        }
        else if (project.isBuilding()) {
            return "shape=tripleoctagon";
        }
        return "";
    }
    public String getFill() {
        return project.getIconColor().getHtmlBaseColor();
    }

    public String getName() {
        return StringEscapeUtils.escapeHtml4(project.getFullDisplayName());
    }

    public String getToolTip() {
        if (project.getLastBuild() != null) {
            StringBuilder sb = new StringBuilder();

            if (project.getLastCompletedBuild() != null) {
                sb.append("Last duration: ");
                sb.append(project.getLastCompletedBuild().getDurationString());
                sb.append(" . ");
            }
            if (project.isBuilding()) {
                sb.append("Current duration: ");
                sb.append(project.getLastBuild().getDurationString());
            }

            return sb.toString();
        }
        return getName();
    }

    public Job<?, ?> getProject() {
        return project;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProjectNode that = (ProjectNode) o;

        if (!project.equals(that.project)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return project.hashCode();
    }
}
