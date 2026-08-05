package hudson.plugins.depgraph_view.model.graph;

import static hudson.plugins.depgraph_view.model.graph.ProjectNode.node;
import static org.junit.Assert.assertTrue;
import hudson.model.AbstractProject;
import hudson.model.BallColor;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.depgraph_view.DependencyGraphProperty.DescriptorImpl;
import hudson.plugins.depgraph_view.model.graph.edge.DependencyGraphEdgeProvider;
import hudson.plugins.depgraph_view.model.graph.edge.EdgeProvider;
import hudson.plugins.depgraph_view.model.graph.project.SubProjectProvider;
import hudson.plugins.depgraph_view.model.display.DotStringGenerator;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.model.JenkinsLocationConfiguration;
import jenkins.triggers.ReverseBuildTrigger;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;

public class ProjectNodeAttributesTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void nodeShowsStatusAndTooltip() throws Exception {
        JenkinsLocationConfiguration.get().setUrl("http://localhost");
        j.getInstance().getDescriptorByType(DescriptorImpl.class).setProjectNameStripRegex("com.comp.(.*)");
        j.getInstance().getDescriptorByType(DescriptorImpl.class).setProjectNameStripRegexGroup(1);

        final FreeStyleProject myJob = j.createFreeStyleProject("com.comp.myJob");
        final FreeStyleProject myJob2 = j.createFreeStyleProject("com.comp.myJob2");
        final ReverseBuildTrigger rbt = new ReverseBuildTrigger("com.comp.myJob2");
        myJob.addTrigger(rbt);
        j.getInstance().rebuildDependencyGraph();
        j.buildAndAssertStatus(Result.SUCCESS, myJob);
        DependencyGraph graph = generateGraph(myJob);
        final SubprojectCalculator subprojectCalculator = new SubprojectCalculator(Collections.<SubProjectProvider>emptySet());
        final ListMultimap<ProjectNode, ProjectNode> subProjects = subprojectCalculator.generate(graph);
        final DotStringGenerator dotStringGenerator = new DotStringGenerator(j.getInstance(), graph, subProjects);
        final String dotString = dotStringGenerator.generate();
        System.err.println(dotString);

        Pattern p = Pattern.compile(".*<myJob>.* fillcolor=\"" + BallColor.BLUE.getHtmlBaseColor() + "\".*");
        Matcher m = p.matcher(dotString);
        assertTrue("node label should contain fill color for job success",
            m.find());

        p = Pattern.compile(".*<myJob2>.* fillcolor=\"" + BallColor.NOTBUILT.getHtmlBaseColor() + "\".*");
        m = p.matcher(dotString);
        assertTrue("node label should contain fill color for job no result",
            m.find());

        p = Pattern.compile(".*<myJob>.* tooltip=\"Last duration: .*");
        m = p.matcher(dotString);
        assertTrue("node label should contain tooltip with job duration",
            m.find());
    }

    private DependencyGraph generateGraph(AbstractProject<?,?> from) {
        return new GraphCalculator(getDependencyGraphEdgeProviders()).generateGraph(Collections.singleton(node(from)));
    }
    
    private ImmutableSet<EdgeProvider> getDependencyGraphEdgeProviders() {
        return ImmutableSet.<EdgeProvider>of(new DependencyGraphEdgeProvider(j.getInstance()));
    }
}
