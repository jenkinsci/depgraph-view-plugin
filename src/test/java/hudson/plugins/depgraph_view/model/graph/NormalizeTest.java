package hudson.plugins.depgraph_view.model.graph;

import static org.junit.Assert.*;

import java.io.IOException;

import hudson.plugins.depgraph_view.model.operations.EdgeOperation;

import org.junit.Test;

public class NormalizeTest {

    @Test
    public void test() {
        String childProjectsValue = "abc, ,ccc";
        childProjectsValue = EdgeOperation.normalizeChildProjectValue(childProjectsValue);
        assertEquals("abc, ccc", childProjectsValue);
    }
    @Test
    public void test1() {
        String childProjectsValue = "abc,   ,ccc";
        childProjectsValue = EdgeOperation.normalizeChildProjectValue(childProjectsValue);
        assertEquals("abc, ccc", childProjectsValue);
    }
    @Test
    public void test2() {
        String childProjectsValue = "abc, ccc";
        childProjectsValue = EdgeOperation.normalizeChildProjectValue(childProjectsValue);
        assertEquals("abc, ccc", childProjectsValue);
    }
    
    @Test
    public void test3() {
        String childProjectsValue = ",abc, ,ccc,";
        childProjectsValue = EdgeOperation.normalizeChildProjectValue(childProjectsValue);
        assertEquals("abc, ccc", childProjectsValue);
    }
    
    @Test
    public void test4() {
        String childProjectsValue = ",abc,,ccc,";
        childProjectsValue = EdgeOperation.normalizeChildProjectValue(childProjectsValue);
        assertEquals("abc, ccc", childProjectsValue);
    }

}
