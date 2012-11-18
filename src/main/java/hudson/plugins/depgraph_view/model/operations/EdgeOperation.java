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

import hudson.model.AbstractProject;

import java.io.IOException;

import jenkins.model.Jenkins;

public abstract class EdgeOperation {
    protected final AbstractProject<?, ?> source;
    protected final AbstractProject<?, ?> target;

    public EdgeOperation(String sourceJobName, String targetJobName) {
        this.source = Jenkins.getInstance().getItemByFullName(sourceJobName.trim(), AbstractProject.class);
        this.target = Jenkins.getInstance().getItemByFullName(targetJobName, AbstractProject.class);
    }
    
    /**
     * Removes double commas and also trailing an leading commas.
     * @param actualValue the actual value to be normalized
     * @return the value with no unrequired commas
     */
    public static String normalizeChildProjectValue(String actualValue){
        actualValue = actualValue.replaceAll("(,[ ]*,)", ", ");
        actualValue = actualValue.replaceAll("(^,|,$)", "");
        return actualValue.trim();
    }
    
    public abstract void perform() throws IOException;
}
