package org.jahia.modules.usercleanuptool;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UserTest {

    @Test
    public void constructorAndGettersRoundTrip() {
        // Arrange + Act
        User user = new User("alice", "/sites/x/acl/ace0", "jnt:ace");

        // Assert
        assertEquals("alice", user.getName());
        assertEquals("/sites/x/acl/ace0", user.getPath());
        assertEquals("jnt:ace", user.getType());
    }

    @Test
    public void allowsNullValues() {
        User user = new User(null, null, null);

        assertEquals(null, user.getName());
        assertEquals(null, user.getPath());
        assertEquals(null, user.getType());
    }
}
