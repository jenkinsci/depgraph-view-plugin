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

import hudson.model.AbstractProject;
import hudson.model.TaskListener;
import hudson.model.FreeStyleProject;
import hudson.model.DependencyGraph;
import hudson.model.DependencyGraph.Dependency;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.Plugin;
import hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig;
import hudson.plugins.parameterizedtrigger.TriggerBuilder;
import hudson.plugins.copyartifact.CopyArtifact;
import hudson.tasks.Builder;

import javax.servlet.ServletException;
import java.io.IOException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Class to calculate the connected components of the
 * dependency graph containing a set of projects.
 * After calculation one can obtain the set of dependencies and
 * the set of projects of the connected components.
 * Respects read permission of the projects.
 *
 * @author wolfs
 */
public class CalculateDeps {
    private static final Logger LOGGER = Logger.getLogger(CalculateDeps.class.getName());
    private final Set<Dependency> visitedDeps = new HashSet<Dependency>();
    private final Set<Dependency> subJobs = new HashSet<Dependency>();
    private final Set<Dependency> copiedArtifacts = new HashSet<Dependency>();
    private final Set<AbstractProject<?,?>> visitedProj = new HashSet<AbstractProject<?,?>>();
    private boolean calculated = false;
    private DependencyGraph dependencyGraph;

    public CalculateDeps(Collection<? extends AbstractProject<?, ?>> projects) {
        this.dependencyGraph = Hudson.getInstance().getDependencyGraph();
        visitedProj.addAll(projects);
    }

    public void calculateNodesAndDependencies()  throws IOException, ServletException, InterruptedException   {
        if (!calculated) {
            calculateNodesAndDependencies(visitedProj);
            calculated = true;
        }
    }
            
    List<AbstractProject<?,?>> getCopiedArtifacts(AbstractProject<?,?> project) {
    
        List<AbstractProject<?,?>> copiedArtifacts = new ArrayList<AbstractProject<?, ?>>();
                
        Plugin copyartifact = Hudson.getInstance().getPlugin("copyartifact");
        if (copyartifact != null) {                                                    
            if(project instanceof FreeStyleProject) {
            
                FreeStyleProject proj = (FreeStyleProject) project;
                List<Builder> builders = proj.getBuilders();
                
                for (Builder builder : builders) {
                
                    if (builder instanceof CopyArtifact) {
                    
                        CopyArtifact caBuilder = (CopyArtifact) builder;
                        String projectName = caBuilder.getProjectName();                    
                        Hudson hudson = Hudson.getInstance();
                        AbstractProject<?,?> projectFromName = hudson.getItemByFullName(projectName, AbstractProject.class);                                        
                        
                        copiedArtifacts.add( projectFromName );
                    }
                }
            }        
        }
        
        return copiedArtifacts;
    }
    
    List<AbstractProject<?,?>> getSubProjects(AbstractProject<?,?> project) throws IOException, ServletException, InterruptedException   {
        List<AbstractProject<?,?>> subProjects = new ArrayList<AbstractProject<?, ?>>();
                
        Plugin parameterizedTrigger = Hudson.getInstance().getPlugin("parameterized-trigger");
        if (parameterizedTrigger != null) {                                 
        
            if(project instanceof FreeStyleProject) {
            
                FreeStyleProject proj = (FreeStyleProject) project;
                List<Builder> builders = proj.getBuilders();
                
                for (Builder builder : builders) {
                
                    if (builder instanceof TriggerBuilder) {
                    
                        TriggerBuilder tBuilder = (TriggerBuilder) builder;
                        for (BlockableBuildTriggerConfig config : tBuilder.getConfigs()) {
                        
                            for (AbstractProject<?,?> abstractProject : config.getProjectList(null)) {
                                subProjects.add( abstractProject );                        
                            }
                        }
                    }
                }
            }
        }
        return subProjects;
    }
    
    private void calculateNodesAndDependencies(Set<AbstractProject<?, ?>> fromProjects)  throws IOException, ServletException, InterruptedException   {
        Set<AbstractProject<?,?>> newProj = new HashSet<AbstractProject<?, ?>>();
        for (AbstractProject<?,?> project : fromProjects) {           
            if (project.hasPermission(Item.READ)) {
                // dependencies
                newProj.addAll(
                        addNewDependencies(dependencyGraph.getUpstreamDependencies(project),true));
                newProj.addAll(
                        addNewDependencies(dependencyGraph.getDownstreamDependencies(project),false));
                
                Set<Dependency> subProjectDeps  = new HashSet<Dependency>();
                
                for(AbstractProject<?,?> subProject : getSubProjects(project)) {
                    subProjectDeps.add(new Dependency(project, subProject));                                
                }
                 
                // Sub-project dependencies
                newProj.addAll(
                        addNewSubJobs(subProjectDeps,true));
                newProj.addAll(
                        addNewSubJobs(subProjectDeps,false));      
                
                List<AbstractProject<?,?>> copiedArtifactsTemp = getCopiedArtifacts(project);
                for(AbstractProject<?,?> copiedProject : copiedArtifactsTemp) {
                    if(copiedProject == null || project == null) {
                        continue;
                    }
                    copiedArtifacts.add(new Dependency(project, copiedProject));                                
                }                                        
            }
        }
        visitedProj.addAll(newProj);
        if (!newProj.isEmpty()) {
            calculateNodesAndDependencies(newProj);
        }
    }

    private Set<AbstractProject<?, ?>> addNewSubJobs(Collection<Dependency> dependencies, boolean isUpstream) {
        
        Set<AbstractProject<?,?>> newProj = new HashSet<AbstractProject<?, ?>>();
        
        for (Dependency dep : dependencies) {
            AbstractProject<?,?> projectToAdd = isUpstream ? dep.getUpstreamProject() : dep.getDownstreamProject();
            if (projectToAdd.hasPermission(Item.READ) && !subJobs.contains(dep)) {
                subJobs.add(dep);
                if (!visitedProj.contains(projectToAdd)) {
                    newProj.add(projectToAdd);
                }
            }
        }
        return newProj;
    }

    private Set<AbstractProject<?, ?>> addNewDependencies(Collection<Dependency> dependencies, boolean isUpstream) {
        
        Set<AbstractProject<?,?>> newProj = new HashSet<AbstractProject<?, ?>>();
        
        for (Dependency dep : dependencies) {
            AbstractProject<?,?> projectToAdd = isUpstream ? dep.getUpstreamProject() : dep.getDownstreamProject();
            if (projectToAdd.hasPermission(Item.READ) && !visitedDeps.contains(dep)) {
                visitedDeps.add(dep);
                if (!visitedProj.contains(projectToAdd)) {
                    newProj.add(projectToAdd);
                }
            }
        }
        return newProj;
    }

    /**
     * Calculates the connected components if necessary
     * @return projects (nodes) in the connected components
     */
    public Set<AbstractProject<?, ?>> getProjects()  throws IOException, ServletException, InterruptedException   {
        if (!calculated) {
            calculateNodesAndDependencies();
        }
        return Collections.unmodifiableSet(visitedProj);
    }

    /**
     * Calculates the connected components if necessary
     * @return dependencies (edges) in the connected components
     */
    public Set<Dependency> getDependencies()  throws IOException, ServletException, InterruptedException   {
        if (!calculated) {
            calculateNodesAndDependencies();
        }
        return Collections.unmodifiableSet(visitedDeps);
    }

    public Set<Dependency> getSubJobs()  throws IOException, ServletException, InterruptedException   {
        if (!calculated) {
            calculateNodesAndDependencies();
        }
        return Collections.unmodifiableSet(subJobs);
    }
    
    public Set<Dependency> getCopiedArtifacts()  throws IOException, ServletException, InterruptedException   {
        if (!calculated) {
            calculateNodesAndDependencies();
        }
        return Collections.unmodifiableSet(copiedArtifacts);
    }                   
}
