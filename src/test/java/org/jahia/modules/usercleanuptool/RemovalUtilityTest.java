package org.jahia.modules.usercleanuptool;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.RepositoryException;

import org.jahia.services.content.JCRNodeWrapper;
import org.junit.Test;

public class RemovalUtilityTest {

    @Test
    public void aceNodeIsCleanable() throws RepositoryException {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.isNodeType("jnt:ace")).thenReturn(true);

        assertTrue(RemovalUtility.isCleanableType(node));
    }

    @Test
    public void memberNodeIsCleanable() throws RepositoryException {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.isNodeType("jnt:ace")).thenReturn(false);
        when(node.isNodeType("jnt:member")).thenReturn(true);

        assertTrue(RemovalUtility.isCleanableType(node));
    }

    @Test
    public void otherNodeTypeIsNotCleanable() throws RepositoryException {
        JCRNodeWrapper node = mock(JCRNodeWrapper.class);
        when(node.isNodeType("jnt:ace")).thenReturn(false);
        when(node.isNodeType("jnt:member")).thenReturn(false);

        assertFalse(RemovalUtility.isCleanableType(node));
    }

    @Test
    public void nullNodeIsNotCleanable() throws RepositoryException {
        assertFalse(RemovalUtility.isCleanableType(null));
    }
}
