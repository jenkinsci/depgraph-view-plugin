/*
 * Copyright (c) 2010 Stefan Wolf
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

package hudson.plugins.depgraph_view;

import com.google.common.collect.ListMultimap;
import com.google.inject.Injector;
import hudson.Launcher;
import hudson.model.AbstractModelObject;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.plugins.depgraph_view.DependencyGraphProperty.DescriptorImpl;
import hudson.plugins.depgraph_view.model.display.AbstractGraphStringGenerator;
import hudson.plugins.depgraph_view.model.display.DotGeneratorFactory;
import hudson.plugins.depgraph_view.model.display.GeneratorFactory;
import hudson.plugins.depgraph_view.model.display.JsonGeneratorFactory;
import hudson.plugins.depgraph_view.model.graph.DependencyGraph;
import hudson.plugins.depgraph_view.model.graph.GraphCalculator;
import hudson.plugins.depgraph_view.model.graph.ProjectNode;
import hudson.plugins.depgraph_view.model.graph.SubprojectCalculator;
import hudson.plugins.depgraph_view.model.operations.DeleteEdgeOperation;
import hudson.plugins.depgraph_view.model.operations.PutEdgeOperation;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import jenkins.model.ModelObjectWithContextMenu.ContextMenu;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic action for creating a Dot-Image of the DependencyGraph
 */
public abstract class AbstractDependencyGraphAction implements Action {
	
    private final Logger LOGGER = Logger.getLogger(Logger.class.getName());

    private static final Pattern EDGE_PATTERN = Pattern.compile("/(.*)/(.*[^/])(.*)");

    /**
     * This method is called via AJAX to obtain the context menu for this model object, but we don't have one...
     */
    public ContextMenu doContextMenu(StaplerRequest request, StaplerResponse response) throws Exception {
        return new ContextMenu();
    }

    public void doEdge(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
        String path = req.getRestOfPath();
        Matcher m = EDGE_PATTERN.matcher(path);
        if (m.find( )) {
          try {
            final String sourceJobName = m.group(1);
              final String targetJobName = m.group(2);
              if ("PUT".equalsIgnoreCase(req.getMethod())) {
                 new PutEdgeOperation(sourceJobName, targetJobName).perform();
              } else if ("DELETE".equalsIgnoreCase(req.getMethod())) {
                 new DeleteEdgeOperation(sourceJobName, targetJobName).perform();
              }
            } catch (Exception e) {
                rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        return;
    }

    /**
     * graph.{png,gv,...} is mapped to the corresponding output
     */
    public void doDynamic(StaplerRequest req, StaplerResponse rsp)  throws IOException, ServletException, InterruptedException {
        String path = req.getRestOfPath();
        SupportedImageType imageType = null;
        try {
            imageType = SupportedImageType.valueOf(path.substring(path.lastIndexOf('.')+1).toUpperCase());
        } catch (Exception e) {
            imageType = SupportedImageType.PNG;
        }

        GeneratorFactory generatorFactory = (imageType == SupportedImageType.JSON) ?
                new JsonGeneratorFactory() : new DotGeneratorFactory();
        AbstractGraphStringGenerator stringGenerator = null;
        if (path.startsWith("/graph.")) {
            Injector injector = Jenkins.lookup(Injector.class);
            GraphCalculator graphCalculator = injector.getInstance(GraphCalculator.class);
            DependencyGraph graph =
                    graphCalculator.generateGraph(GraphCalculator.abstractProjectSetToProjectNodeSet(getProjectsForDepgraph()));
            ListMultimap<ProjectNode, ProjectNode> projects2Subprojects =
                    injector.getInstance(SubprojectCalculator.class).generate(graph);
            stringGenerator = generatorFactory.newGenerator(graph, projects2Subprojects);
        } else if (path.startsWith("/legend.")) {
            stringGenerator = generatorFactory.newLegendGenerator();
        } else {
            rsp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }
        String graphString = stringGenerator.generate();

        rsp.setContentType(imageType.contentType);
        if (imageType.requiresProcessing) {
            runDot(rsp.getOutputStream(), new ByteArrayInputStream(graphString.getBytes(Charset.forName("UTF-8"))), imageType.dotType);
        } else {
            rsp.getWriter().append(graphString).close();
        }
    }


    /**
     * Execute the dot command with given input and output stream
     * @param type the parameter for the -T option of the graphviz tools
     */
    protected void runDot(OutputStream output, InputStream input, String type)
            throws IOException {
        DescriptorImpl descriptor = Hudson.getInstance().getDescriptorByType(DescriptorImpl.class);
        String dotPath = descriptor.getDotExeOrDefault();
        Launcher launcher = Hudson.getInstance().createLauncher(new LogTaskListener(LOGGER, Level.CONFIG));
        try {
            launcher.launch()
                    .cmds(dotPath,"-T" + type, "-Gcharset=UTF-8", "-q1")
                    .stdin(input)
                    .stdout(output)
                    .start().join();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Interrupted while waiting for dot-file to be created",e);
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }

    public boolean isGraphvizEnabled() {
        return Hudson.getInstance().getDescriptorByType(DescriptorImpl.class).isGraphvizEnabled();
    }

    public boolean isEditFunctionInJSViewEnabled() {
        return Hudson.getInstance().getDescriptorByType(DescriptorImpl.class).isEditFunctionInJSViewEnabled();
    }

    /**
     * @return projects for which the dependency graph should be calculated
     */
    protected abstract Collection<? extends AbstractProject<?, ?>> getProjectsForDepgraph();

    /**
     * @return title of the dependency graph page
     */
    public abstract String getTitle();

    /**
     * @return object for which the sidepanel.jelly will be shown
     */
    public abstract AbstractModelObject getParentObject();

    @Override
    public String getIconFileName() {
        return "graph.gif";
    }

    @Override
    public String getDisplayName() {
        return Messages.AbstractDependencyGraphAction_DependencyGraph();
    }

    @Override
    public String getUrlName() {
        return "depgraph-view";
    }

}
