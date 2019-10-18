/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/codegen/customScript")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class CustomScriptCodegenTestGenerated extends AbstractCustomScriptCodegenTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    public void testAllFilesPresentInCustomScript() throws Exception {
        KotlinTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/codegen/customScript"), Pattern.compile("^(.*)$"), true);
    }

    @TestMetadata("pathPattern5.kts")
    public void testPathPattern5_kts() throws Exception {
        runTest("compiler/testData/codegen/customScript/pathPattern5.kts");
    }

    @TestMetadata("simpleEnvVars.kts")
    public void testSimpleEnvVars_kts() throws Exception {
        runTest("compiler/testData/codegen/customScript/simpleEnvVars.kts");
    }

    @TestMetadata("simple.customext")
    public void testSimple_customext() throws Exception {
        runTest("compiler/testData/codegen/customScript/simple.customext");
    }

    @TestMetadata("stringReceiver.kts")
    public void testStringReceiver_kts() throws Exception {
        runTest("compiler/testData/codegen/customScript/stringReceiver.kts");
    }
}
