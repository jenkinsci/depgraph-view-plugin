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

package hudson.plugins.depgraph_view.model.operations;

import hudson.model.Result;
import hudson.tasks.BuildTrigger;
import jenkins.model.Jenkins;

import java.io.IOException;

public class DeleteEdgeOperation extends EdgeOperation {
    
    public DeleteEdgeOperation(String sourceJobName, String targetJobName) {
        super(sourceJobName, targetJobName);
    }

    public void perform() throws IOException {
        if (source != null && target != null) {
            final BuildTrigger buildTrigger = source.getPublishersList().get(BuildTrigger.class);
            if (buildTrigger != null) {
                final String childProjectsValue = normalizeChildProjectValue(buildTrigger.getChildProjectsValue().replace(target.getName(), ""));
                final Result threshold = buildTrigger.getThreshold();
                source.getPublishersList().remove(buildTrigger);
                if (!childProjectsValue.isEmpty()) {
                    source.getPublishersList().add(new BuildTrigger(childProjectsValue, threshold));
                }
                source.save();
                target.save();
                Jenkins.get().rebuildDependencyGraph();
            }
        }
    }
}
