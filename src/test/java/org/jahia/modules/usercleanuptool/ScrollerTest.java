package org.jahia.modules.usercleanuptool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.jahia.services.content.JCRNodeWrapper;
import org.junit.Test;

public class ScrollerTest {

    /**
     * Test subclass exposing the protected {@code setStepResult} so we can feed a mocked
     * {@link QueryResult} into the real {@link Scroller#scroll()} algorithm.
     */
    private static class TestableScroller extends Scroller {
        TestableScroller(Function<JCRNodeWrapper, Boolean> predicate, List<JCRNodeWrapper> tray,
                         int capacity, int offset) {
            super(predicate, tray, capacity, offset);
        }

        void feed(QueryResult result) {
            setStepResult(result);
        }
    }

    /** Builds a QueryResult whose getNodes() returns an iterator over the given nodes. */
    private static QueryResult resultOf(List<JCRNodeWrapper> nodes) throws RepositoryException {
        NodeIterator it = mock(NodeIterator.class);
        // hasNext() returns true once per node, then false.
        Boolean[] hasNext = new Boolean[nodes.size() + 1];
        for (int i = 0; i < nodes.size(); i++) {
            hasNext[i] = Boolean.TRUE;
        }
        hasNext[nodes.size()] = Boolean.FALSE;
        if (nodes.isEmpty()) {
            when(it.hasNext()).thenReturn(false);
        } else {
            Boolean[] rest = new Boolean[nodes.size()];
            System.arraycopy(hasNext, 1, rest, 0, nodes.size());
            when(it.hasNext()).thenReturn(hasNext[0], rest);
            JCRNodeWrapper first = nodes.get(0);
            JCRNodeWrapper[] more = new JCRNodeWrapper[nodes.size() - 1];
            for (int i = 1; i < nodes.size(); i++) {
                more[i - 1] = nodes.get(i);
            }
            if (more.length == 0) {
                when(it.nextNode()).thenReturn(first);
            } else {
                when(it.nextNode()).thenReturn(first, more);
            }
        }
        QueryResult result = mock(QueryResult.class);
        when(result.getNodes()).thenReturn(it);
        return result;
    }

    private static JCRNodeWrapper node() {
        return mock(JCRNodeWrapper.class);
    }

    @Test
    public void predicateTrueNodesFillTrayUpToCapacity() throws RepositoryException {
        // Arrange: 5 matching nodes, capacity 3.
        List<JCRNodeWrapper> nodes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            nodes.add(node());
        }
        List<JCRNodeWrapper> tray = new ArrayList<>();
        Function<JCRNodeWrapper, Boolean> alwaysTrue = n -> true;
        TestableScroller scroller = new TestableScroller(alwaysTrue, tray, 3, 0);
        scroller.feed(resultOf(nodes));

        // Act
        boolean keepGoing = scroller.scroll();

        // Assert: tray filled to capacity, scroll signals stop (false).
        assertEquals(3, tray.size());
        assertFalse(keepGoing);
    }

    @Test
    public void predicateFalseNodesAreSkipped() throws RepositoryException {
        // Arrange: 4 nodes, none match the predicate.
        List<JCRNodeWrapper> nodes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            nodes.add(node());
        }
        List<JCRNodeWrapper> tray = new ArrayList<>();
        Function<JCRNodeWrapper, Boolean> alwaysFalse = n -> false;
        TestableScroller scroller = new TestableScroller(alwaysFalse, tray, 3, 0);
        scroller.feed(resultOf(nodes));

        // Act
        boolean keepGoing = scroller.scroll();

        // Assert: nothing added; not yet at capacity so continue (true).
        assertEquals(0, tray.size());
        assertTrue(keepGoing);
    }

    @Test
    public void offsetSkipsFirstMatchingNodes() throws RepositoryException {
        // Arrange: 5 matching nodes, capacity 10, offset 2.
        List<JCRNodeWrapper> nodes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            nodes.add(node());
        }
        List<JCRNodeWrapper> tray = new ArrayList<>();
        Function<JCRNodeWrapper, Boolean> alwaysTrue = n -> true;
        TestableScroller scroller = new TestableScroller(alwaysTrue, tray, 10, 2);
        scroller.feed(resultOf(nodes));

        // Act
        boolean keepGoing = scroller.scroll();

        // Assert: first 2 matches skipped, remaining 3 collected; below capacity -> continue.
        assertEquals(3, tray.size());
        assertEquals(nodes.get(2), tray.get(0));
        assertEquals(nodes.get(3), tray.get(1));
        assertEquals(nodes.get(4), tray.get(2));
        assertTrue(keepGoing);
    }

    @Test
    public void returnsTrueToContinueWhenBelowCapacity() throws RepositoryException {
        // Arrange: 2 matching nodes, capacity 5.
        List<JCRNodeWrapper> nodes = new ArrayList<>();
        nodes.add(node());
        nodes.add(node());
        List<JCRNodeWrapper> tray = new ArrayList<>();
        TestableScroller scroller = new TestableScroller(n -> true, tray, 5, 0);
        scroller.feed(resultOf(nodes));

        // Act
        boolean keepGoing = scroller.scroll();

        // Assert
        assertEquals(2, tray.size());
        assertTrue(keepGoing);
    }
}
