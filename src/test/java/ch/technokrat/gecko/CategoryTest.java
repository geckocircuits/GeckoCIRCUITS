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
 * Unit tests for Category annotation.
 * Tests annotation properties and runtime visibility.
 */
public class CategoryTest {

    @Category(MethodCategory.SIMULATION_START)
    public void dummyAnnotatedMethod() {
    }

    @Test
    public void testCategoryAnnotationExists() {
        assertNotNull("Category annotation should exist", Category.class);
    }

    @Test
    public void testCategoryAnnotationTarget() {
        Target target = Category.class.getAnnotation(Target.class);
        assertNotNull("Category should have @Target annotation", target);
    }

    @Test
    public void testCategoryAnnotationTargetValue() {
        Target target = Category.class.getAnnotation(Target.class);
        ElementType[] values = target.value();
        assertTrue("Category should target METHOD", java.util.Arrays.asList(values).contains(ElementType.METHOD));
    }

    @Test
    public void testCategoryAnnotationRetention() {
        Retention retention = Category.class.getAnnotation(Retention.class);
        assertNotNull("Category should have @Retention annotation", retention);
    }

    @Test
    public void testCategoryAnnotationRetentionValue() {
        Retention retention = Category.class.getAnnotation(Retention.class);
        assertEquals("Category should be retained at RUNTIME", RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testCategoryAnnotationCanBeApplied() throws Exception {
        java.lang.reflect.Method method = CategoryTest.class.getMethod("dummyAnnotatedMethod");
        Category annotation = method.getAnnotation(Category.class);
        assertNotNull("Category annotation should be retrievable at runtime", annotation);
    }

    @Test
    public void testCategoryAnnotationValue() throws Exception {
        java.lang.reflect.Method method = CategoryTest.class.getMethod("dummyAnnotatedMethod");
        Category annotation = method.getAnnotation(Category.class);
        assertEquals("Category value should be SIMULATION_START", MethodCategory.SIMULATION_START, annotation.value());
    }

    @Test
    public void testCategoryAnnotationIsRetained() throws Exception {
        java.lang.reflect.Method method = CategoryTest.class.getMethod("dummyAnnotatedMethod");
        Category annotation = method.getAnnotation(Category.class);
        assertNotNull("Category annotation should be visible at runtime", annotation);
    }

    @Test
    public void testCategoryAnnotationMultipleValues() throws Exception {
        for (MethodCategory category : MethodCategory.values()) {
            assertNotNull("All method categories should be valid annotation values", category);
        }
    }
}
