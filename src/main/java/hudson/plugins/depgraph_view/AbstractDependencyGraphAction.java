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

import com.google.common.collect.ImmutableMap;
import hudson.Launcher;
import hudson.model.AbstractModelObject;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.DependencyGraph.Dependency;
import hudson.model.Hudson;
import hudson.plugins.depgraph_view.DependencyGraphProperty.DescriptorImpl;
import hudson.util.LogTaskListener;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic action for creating a Dot-Image of the DependencyGraph
 *
 * @author wolfs
 */
public abstract class AbstractDependencyGraphAction implements Action {
    private final Logger LOGGER = Logger.getLogger(Logger.class.getName());

    /**
     * Maps the extension of the requested file to the content type and the
     * argument for the -T option of the graphviz tools
     */
    protected static final ImmutableMap<String, SupportedImageType> extension2Type =
            ImmutableMap.of(
                    "png",SupportedImageType.of("image/png", "png"),
                    "svg",SupportedImageType.of("image/svg", "svg"),
                    "map",SupportedImageType.of("image/cmapx", "cmapx"),
                    "gv",SupportedImageType.of("text/plain", "gv") // Special case - do no processing
            );

    //  Lexicographic order of the dependencies
    private static final Comparator<Dependency> DEP_COMPARATOR = new Comparator<Dependency>() {
        @Override
        public int compare(Dependency o1, Dependency o2) {
            int down = (PROJECT_COMPARATOR.compare(o1.getDownstreamProject(),o2.getDownstreamProject()));
            return down != 0 ? down : PROJECT_COMPARATOR
                    .compare(o1.getUpstreamProject(),o2.getUpstreamProject());
        }
    };

    //  Lexicographic order of the dependencies
    private static final Comparator<Dependency> DEP_COMPARATOR_UPSTREAMFIRST = new Comparator<Dependency>() {
        @Override
        public int compare(Dependency o1, Dependency o2) {
            int up = (PROJECT_COMPARATOR.compare(o1.getUpstreamProject(),o2.getUpstreamProject()));
            return up != 0 ? up : PROJECT_COMPARATOR
                    .compare(o1.getDownstreamProject(),o2.getDownstreamProject());
        }
    };
    
    // Compares projects by name
    private static final Comparator<AbstractProject<?,?>> PROJECT_COMPARATOR = new Comparator<AbstractProject<?,?>>() {
        @Override
        public int compare(AbstractProject<?,?> o1, AbstractProject<?,?> o2) {
            return o1.getFullDisplayName().compareTo(o2.getFullDisplayName());
        }
    };

    // Data Structure to encode the content type and the -T argument for the graphviz tools
    protected static class SupportedImageType {
        final String contentType;
        final String dotType;

        private SupportedImageType(String contentType,
                                   String dotType) {
            this.contentType = contentType;
            this.dotType = dotType;
        }

        public static SupportedImageType of(String contentType, String dotType) {
            return new SupportedImageType(contentType, dotType);
        }

    }

    /**
     * graph.{png,gv,...} is mapped to the corresponding output
     */
    public void doDynamic(StaplerRequest req, StaplerResponse rsp)  throws IOException, ServletException, InterruptedException   {
        String path = req.getRestOfPath();
        if (path.startsWith("/graph.")) {
            String extension = path.substring("/graph.".length());            
            if (extension2Type.containsKey(extension.toLowerCase())) {            
                SupportedImageType imageType = extension2Type.get(extension.toLowerCase());
                CalculateDeps calculateDeps = new CalculateDeps(getProjectsForDepgraph());
                String graphDot = generateDotText(calculateDeps.getProjects(), 
                
                calculateDeps.getDependencies(), 
                calculateDeps.getSubJobs(), 
                calculateDeps.getCopiedArtifacts());
                
                rsp.setContentType(imageType.contentType);
                if ("gv".equalsIgnoreCase(extension)) {
                    rsp.getWriter().append(graphDot).close();
                } else {
                    runDot(rsp.getOutputStream(), new ByteArrayInputStream(graphDot.getBytes()), imageType.dotType);
                }
            }
        } else {
            rsp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }
    }
    
    /**
     * Generates the graphviz code for the given projects and dependencies
     * @param projects the nodes of the graph
     * @param deps the edges of the graph
     * @return graphviz code
     */
    public String generateDotText(Set<AbstractProject<?,?>> projects, 
                                  Set<Dependency> deps, 
                                  Set<Dependency> subJobs, 
                                  Set<Dependency> copied) {
        
        /* Sort dependencies (by downstream task first) */
        List<Dependency> updownstreamDeps = new ArrayList<Dependency>(deps);
        Collections.sort(updownstreamDeps, DEP_COMPARATOR);
        
        /* create a list of all projects with up or downstream dependencies */
        List<AbstractProject<?, ?>> depProjects = listUniqueProjectsInDependencies(updownstreamDeps);
        Collections.sort(depProjects, PROJECT_COMPARATOR);
                
        /* Sort sub-job dependencies (by upstream task first) */
        List<Dependency> subjobDeps = new ArrayList<Dependency>(subJobs);
        Collections.sort(subjobDeps, DEP_COMPARATOR_UPSTREAMFIRST);        
        
        /* create a list of all subjobs and projects with subjobs  */
        List<AbstractProject<?, ?>> subJobProjects = listUniqueProjectsInDependencies(subjobDeps);        
        Collections.sort(subJobProjects, PROJECT_COMPARATOR);        
        
        /* Sort artifact-copy dependencies (by downstream task first) */
        List<Dependency> copiedArtifactDeps = new ArrayList<Dependency>(copied);
        Collections.sort(copiedArtifactDeps, DEP_COMPARATOR);
        /**/
                        
        /* create a list of all  copied artifacts and projects with copied artifacts  */
        List<AbstractProject<?, ?>> artifactsCopiedProjects = listUniqueProjectsInDependencies(copiedArtifactDeps);                
        Collections.sort(artifactsCopiedProjects, PROJECT_COMPARATOR);
        

        /* Find all projects wityhout dependencies or copied artifacts (stand-alone projects) */
        List<AbstractProject<?, ?>> standaloneProjects = new ArrayList<AbstractProject<?, ?>>(projects);
        standaloneProjects.removeAll(depProjects);
        standaloneProjects.removeAll(subJobProjects);         
        standaloneProjects.removeAll(artifactsCopiedProjects);
        Collections.sort(standaloneProjects, PROJECT_COMPARATOR);
       
        
        /**** Build the dot source file ****/       
        StringBuilder builder = new StringBuilder();
        
        CreateDigraphSourceCode(builder, standaloneProjects, depProjects, updownstreamDeps, subjobDeps, copiedArtifactDeps);
        
        return builder.toString();
    }
    
    private void  CreateDigraphSourceCode(StringBuilder builder,  
    List<AbstractProject<?, ?>> standaloneProjects, List<AbstractProject<?, ?>> depProjects,
    List<Dependency> updownstreamDeps, List<Dependency> subjobDeps, List<Dependency> copiedArtifactDeps)    
    {    
        builder.append("digraph {\n");
        builder.append("node [shape=box, style=rounded];\n");
                
        /**** First define all the objects and clusters ****/

        // Stuff not linked to other stuff
        for (AbstractProject<?, ?> proj : standaloneProjects) {
            builder.append(projectToNodeString(proj)).append(";\n");
        }
        
        // Sub jobs and their parents
        dependencyToSubjobClusters(builder, subjobDeps);        
        
        // up/downstream linked jobs 
        builder.append("subgraph clusterMain {\n");        
        for (AbstractProject<?, ?> proj : depProjects) {
            builder.append(projectToNodeString(proj)).append(";\n");
        }
        builder.append("color=white;\n}\n");

        /****Now define links between objects ****/
                
        // plain normal dependencies (up/downstream)
        for (Dependency dep : updownstreamDeps) {
            builder.append(dependencyToEdgeString(dep));
            builder.append(";\n");
        }
        
        // subjob dependencies
        for (Dependency dep : subjobDeps) {
            builder.append(dependencyToSubjobString(dep));
            builder.append(";\n");
        }
        
        //  copied artifact dependencies
        for (Dependency dep : copiedArtifactDeps) {
            builder.append(dependencyToCopiedArtifactString(dep));
            builder.append(";\n");
        }
        
        builder.append("}");           
    }
    
    private List<AbstractProject<?, ?>> listUniqueProjectsInDependencies(List<Dependency> dependencies)
    {
        List<AbstractProject<?, ?>> listTemp = new ArrayList<AbstractProject<?, ?>>();
        for (Dependency dependency : dependencies) 
        {
            listTemp.add(dependency.getUpstreamProject());
            listTemp.add(dependency.getDownstreamProject());
        }
         /* remove duplicates by passing through a hashset */
        Set<AbstractProject<?, ?>> tempSet = new HashSet<AbstractProject<?, ?>>(listTemp);
        return new ArrayList<AbstractProject<?, ?>>(tempSet);
    }
    
    
    private String projectToNodeString(AbstractProject<?, ?> proj) {
        return escapeString(proj.getFullDisplayName()) +
                " [href=" +
                escapeString(Hudson.getInstance().getRootUrlFromRequest() + proj.getUrl()) + "]";
    }

    private void dependencyToSubjobClusters(StringBuilder builder, List<Dependency> sortedSubjobs)
    {
        int subjobcount = 1;
        
        builder.append("\n/** Subjob clusters **/\n");
        
        AbstractProject<?,?> currentUpstream = null;
        for(Dependency subJob : sortedSubjobs)
        {
            if(currentUpstream != subJob.getUpstreamProject())
            {
                if(currentUpstream != null)
                { 
                    // not the first upstream (master) job
                    builder.append("}\n\n");
                }
                builder.append("subgraph cluster_subjobs" + subjobcount++ + " {\n");
                builder.append(projectToNodeString(subJob.getUpstreamProject()) + "\n");
            }
            builder.append("    " + projectToNodeString(subJob.getDownstreamProject()) + ";\n");
            currentUpstream = subJob.getUpstreamProject();
        }
        if(currentUpstream != null)
        { 
            //there were at least one upstream (master) jobs
            builder.append("}\n\n");
        }
        
        builder.append("\n");        
    }
    
    private String dependencyToEdgeString(Dependency dep) {
        return escapeString(dep.getUpstreamProject().getFullDisplayName()) + " -> " +
                escapeString(dep.getDownstreamProject().getFullDisplayName()) + " [ style=bold ] ";
    }
    
    private String dependencyToCopiedArtifactString(Dependency dep) {
        return escapeString(dep.getDownstreamProject().getFullDisplayName()) + " -> " +
                escapeString(dep.getUpstreamProject().getFullDisplayName()) + " [color=lightblue, dir=back]";
    }

    private String dependencyToSubjobString(Dependency dep) {
        return escapeString(dep.getUpstreamProject().getFullDisplayName()) + " -> " +
                escapeString(dep.getDownstreamProject().getFullDisplayName()) + "  ";
    }

    private String escapeString(String toEscape) {
        return "\"" + toEscape + "\"";
    }

    /**
     * Execute the dot commando with given input and output stream
     * @param type the parameter for the -T option of the graphviz tools
     */
    protected void runDot(OutputStream output, InputStream input, String type)
            throws IOException {
        DescriptorImpl descriptor = Hudson.getInstance().getDescriptorByType(DependencyGraphProperty.DescriptorImpl.class);
        String dotPath = descriptor.getDotExeOrDefault();
        Launcher launcher = Hudson.getInstance().createLauncher(new LogTaskListener(LOGGER, Level.CONFIG));
        try {
            launcher.launch()
                    .cmds(dotPath,"-T" + type)
                    .stdin(input)
                    .stdout(output).start().join();
        } catch (InterruptedException e) {
            LOGGER.severe("Interrupted while waiting for dot-file to be created:" + e);
            e.printStackTrace();
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
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
