package com.plugin.tests;

import org.jetbrains.plugins.groovy.lang.resolve.api.GroovyMethodCallReference;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.plugin.AddGradleDependency;

/**
 * Unit tests for private utility methods in AddGradleDependency.
 * Reflection is used to access private methods so tests remain decoupled
 * from implementation visibility while verifying behavior.
 */
class AddGradleDependencyTest {
    // Constant inputs used across tests to avoid magic strings.
    private static final String EXAMPLE_PATH = "teamcode/src/main/java/org/firstinspires/ftc/teamcode/Robot.java";
    private static final String SIMPLE_PATH = "a/b/c";

    /**
     * Helper to invoke a private instance method by name.
     */
    private Object invokePrivate(String methodName, Class<?>[] parameterTypes, Object[] args) {
        try {
            Method m = AddGradleDependency.class.getDeclaredMethod(methodName, parameterTypes);
            m.setAccessible(true);
            return m.invoke(new AddGradleDependency(), args);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    void testRemoveBeginning_removesFirstSegment() {
        // When the first path segment is removed, result should start with a leading slash.
        String result = (String) invokePrivate("removeBeginning", new Class[]{String.class}, new Object[]{SIMPLE_PATH});
        assertEquals("/b/c", result, "First path segment should be replaced with an empty segment producing a leading slash");
    }

    @Test
    void testRemoveBeginning_preservesRestOfPath() {
        // Ensure complex paths preserve remainder and only the first segment is emptied.
        String result = (String) invokePrivate("removeBeginning", new Class[]{String.class}, new Object[]{EXAMPLE_PATH});
        // Expect leading slash and the rest of the path unchanged after first segment removed
        assertTrue(result.startsWith("/src/main/java/org/firstinspires/ftc/teamcode/Robot.java"),
                "Only the first path segment should be removed while the rest remains intact");
    }

    @Test
    void testGetReference_returnsMatchingReference() {
        // Create two mocked references with distinct method names.
        GroovyMethodCallReference refA = mock(GroovyMethodCallReference.class);
        when(refA.getMethodName()).thenReturn("repositories");
        GroovyMethodCallReference refB = mock(GroovyMethodCallReference.class);
        when(refB.getMethodName()).thenReturn("dependencies");

        ArrayList<GroovyMethodCallReference> refs = new ArrayList<>();
        refs.add(refA);
        refs.add(refB);

        Object found = invokePrivate("getReference",
                new Class[]{ArrayList.class, String.class},
                new Object[]{refs, "dependencies"});

        assertNotNull(found, "Expected to find a reference with the name 'dependencies'");
        assertSame(refB, found, "The returned reference must be the one that matches the requested name");
    }

    @Test
    void testGetReference_returnsNullWhenMissing() {
        GroovyMethodCallReference ref = mock(GroovyMethodCallReference.class);
        when(ref.getMethodName()).thenReturn("repositories");

        ArrayList<GroovyMethodCallReference> refs = new ArrayList<>();
        refs.add(ref);

        Object found = invokePrivate("getReference",
                new Class[]{ArrayList.class, String.class},
                new Object[]{refs, "nonexistent"});

        assertNull(found, "When no reference matches the name, method should return null");
    }
}
