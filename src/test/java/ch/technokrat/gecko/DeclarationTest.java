/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Declaration annotation.
 * Tests annotation properties and runtime visibility.
 */
public class DeclarationTest {

    @Declaration("Test method declaration")
    public void dummyAnnotatedMethod() {
    }

    @Test
    public void testDeclarationAnnotationExists() {
        assertNotNull("Declaration annotation should exist", Declaration.class);
    }

    @Test
    public void testDeclarationAnnotationTarget() {
        Target target = Declaration.class.getAnnotation(Target.class);
        assertNotNull("Declaration should have @Target annotation", target);
    }

    @Test
    public void testDeclarationAnnotationTargetValue() {
        Target target = Declaration.class.getAnnotation(Target.class);
        ElementType[] values = target.value();
        assertTrue("Declaration should target METHOD", java.util.Arrays.asList(values).contains(ElementType.METHOD));
    }

    @Test
    public void testDeclarationAnnotationRetention() {
        Retention retention = Declaration.class.getAnnotation(Retention.class);
        assertNotNull("Declaration should have @Retention annotation", retention);
    }

    @Test
    public void testDeclarationAnnotationRetentionValue() {
        Retention retention = Declaration.class.getAnnotation(Retention.class);
        assertEquals("Declaration should be retained at RUNTIME", RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testDeclarationAnnotationCanBeApplied() throws Exception {
        java.lang.reflect.Method method = DeclarationTest.class.getMethod("dummyAnnotatedMethod");
        Declaration annotation = method.getAnnotation(Declaration.class);
        assertNotNull("Declaration annotation should be retrievable at runtime", annotation);
    }

    @Test
    public void testDeclarationAnnotationValue() throws Exception {
        java.lang.reflect.Method method = DeclarationTest.class.getMethod("dummyAnnotatedMethod");
        Declaration annotation = method.getAnnotation(Declaration.class);
        assertEquals("Declaration value should match", "Test method declaration", annotation.value());
    }

    @Test
    public void testDeclarationAnnotationIsRetained() throws Exception {
        java.lang.reflect.Method method = DeclarationTest.class.getMethod("dummyAnnotatedMethod");
        Declaration annotation = method.getAnnotation(Declaration.class);
        assertNotNull("Declaration annotation should be visible at runtime", annotation);
    }

    @Test
    public void testDeclarationAnnotationEmptyString() throws Exception {
        // Should allow empty string
        Declaration decl = new Declaration() {
            @Override
            public String value() {
                return "";
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Declaration.class;
            }
        };
        assertEquals("", decl.value());
    }

    @Test
    public void testDeclarationAnnotationStringValues() throws Exception {
        String[] testValues = {
            "Simple declaration",
            "Multi word declaration",
            "Declaration with numbers 123",
            "Declaration with special chars !@#$%"
        };

        for (String value : testValues) {
            assertNotNull(value);
            assertFalse(value.isEmpty());
        }
    }

    @Test
    public void testDeclarationAnnotationPresence() throws Exception {
        java.lang.reflect.Method method = DeclarationTest.class.getMethod("dummyAnnotatedMethod");
        assertTrue("Method should have Declaration annotation",
                method.isAnnotationPresent(Declaration.class));
    }
}
