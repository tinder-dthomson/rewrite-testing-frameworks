/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.testing.junit5

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class JUnitParamsRunnerToParameterizedTest : JavaRecipeTest {
    override val parser: JavaParser = JavaParser.fromJavaVersion()
        .classpath("junit", "JUnitParams")
        .build()

    override val recipe: Recipe
        get() = JUnitParamsRunnerToParameterized()

    @Test
    fun testHasAssociatedMethodSource() = assertChanged(
        before = """
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import junitparams.JUnitParamsRunner;
            import junitparams.Parameters;
        
            @RunWith(JUnitParamsRunner.class)
            public class PersonTests {
            
                @Test
                @Parameters
                public void personIsAdult(int age, boolean valid) {
                }
            
                private Object[] parametersForPersonIsAdult() {
                    return new Object[]{new Object[]{13, false}, new Object[]{17, false}};
                }
            
                @Test
                @Parameters
                public void personIsChild(int age, boolean valid) {
                }
            
                private Object[] parametersForPersonIsChild() {
                    return new Object[]{new Object[]{3, false}, new Object[]{7, false}};
                }
            }
        """,
        after = """
            import org.junit.jupiter.params.ParameterizedTest;
            import org.junit.jupiter.params.provider.MethodSource;
            
            public class PersonTests {
            
                @ParameterizedTest
                @MethodSource("parametersForPersonIsAdult")
                public void personIsAdult(int age, boolean valid) {
                }
            
                private static Object[] parametersForPersonIsAdult() {
                    return new Object[]{new Object[]{13, false}, new Object[]{17, false}};
                }
            
                @ParameterizedTest
                @MethodSource("parametersForPersonIsChild")
                public void personIsChild(int age, boolean valid) {
                }
            
                private static Object[] parametersForPersonIsChild() {
                    return new Object[]{new Object[]{3, false}, new Object[]{7, false}};
                }
            }
        """
    )

    @Test
    fun testHasSpecifiedMethodSource() = assertChanged(
        before = """
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import junitparams.JUnitParamsRunner;
            import junitparams.Parameters;
            import junitparams.NamedParameters;
            import junitparams.naming.TestCaseName;
        
            @RunWith(JUnitParamsRunner.class)
            public class PersonTests {
            
                @Test
                @Parameters(method = "youngAdultPersonParams")
                @TestCaseName("persons-age: {0} is-young-adult: {1}")
                public void personIsAdult(int age, boolean valid) {
                }
            
                private Object[] youngAdultPersonParams() {
                    return new Object[]{new Object[]{13, false}, new Object[]{17, false}};
                }
            
                @Test
                @Parameters(named = "named-params")
                @TestCaseName("persons-age: {0} is-young-adult: {1}")
                public void personIsNamedAdult(int age, boolean valid) {
                }
            
                @NamedParameters("named-params")
                private Object[] namedPeopleParams() {
                    return new Object[]{new Object[]{13, false}, new Object[]{17, false}};
                }
            
                @Test
                @Parameters(method = "named2,named3")
                public void paramsInMultipleMethods(String p1, Integer p2) { }
                
                private Object named2() {
                    return new Object[]{"AAA", 1};
                }
                
                private Object named3() {
                    return new Object[]{"BBB", 2};
                }
            }
        """,
        after = """
            import org.junit.jupiter.params.ParameterizedTest;
            import org.junit.jupiter.params.provider.MethodSource;
            
            public class PersonTests {
            
                @ParameterizedTest(name = "persons-age: {0} is-young-adult: {1}")
                @MethodSource("youngAdultPersonParams")
                public void personIsAdult(int age, boolean valid) {
                }
            
                private static Object[] youngAdultPersonParams() {
                    return new Object[]{new Object[]{13, false}, new Object[]{17, false}};
                }
            
                @ParameterizedTest(name = "persons-age: {0} is-young-adult: {1}")
                @MethodSource("namedPeopleParams")
                public void personIsNamedAdult(int age, boolean valid) {
                }
            
            
                private static Object[] namedPeopleParams() {
                    return new Object[]{new Object[]{13, false}, new Object[]{17, false}};
                }
            
                @ParameterizedTest
                @MethodSource({"named2", "named3"})
                public void paramsInMultipleMethods(String p1, Integer p2) { }
            
                private static Object named2() {
                    return new Object[]{"AAA", 1};
                }
            
                private static Object named3() {
                    return new Object[]{"BBB", 2};
                }
            }
        """
    )

    @Test
    fun enumSourceNotConverted() = assertChanged (
        before = """
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import junitparams.JUnitParamsRunner;
            import junitparams.Parameters;
            
            enum PersonType {MALE, FEMALE}
            
            @RunWith(JUnitParamsRunner.class)
            class enumSourceTests {
                @Test
                @Parameters(source = PersonType.class)
                public void enumsAsParamsInMethod(PersonType person) { }
            }
        """,
        after = """
            import org.junit.Test;
            import org.junit.runner.RunWith;
            import junitparams.JUnitParamsRunner;
            import junitparams.Parameters;
            
            enum PersonType {MALE, FEMALE}
            
            @RunWith(JUnitParamsRunner.class)
            class enumSourceTests {
                @Test
                // JunitParamsRunnerToParameterized conversion not supported
                @Parameters(source = PersonType.class)
                public void enumsAsParamsInMethod(PersonType person) { }
            }
        """
    );
}
