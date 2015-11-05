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

import hudson.ExtensionPoint;
import hudson.model.AbstractProject;

/**
 * This is an extension point which makes it possible to subprojects
 * to the DependencyGraph which gets drawn. Note that in order to add your own
 * EdgeProvider you must not annotate the corresponding subclass with {@link hudson.Extension}
 * but instead add a {@link com.google.inject.Module} with a {@link com.google.inject.multibindings.Multibinder}
 * which has the {@link hudson.Extension} annotation. For example see {@link DependencyGraphModule}
 * and {@link ParameterizedTriggerSubProjectProvider}
 */
public interface SubProjectProvider extends ExtensionPoint {
    public Iterable<ProjectNode> getSubProjectsOf(AbstractProject<?,?> project);
}
