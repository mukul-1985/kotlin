/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.loader

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptConfigurationManager
import org.jetbrains.kotlin.idea.core.script.configuration.utils.getKtFile
import org.jetbrains.kotlin.idea.core.script.debug
import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.LegacyResolverWrapper
import org.jetbrains.kotlin.scripting.resolve.refineScriptCompilationConfiguration
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver

internal class DefaultScriptConfigurationLoader(val project: Project) :
    ScriptConfigurationLoader {
    override fun isAsync(scriptDefinition: ScriptDefinition): Boolean =
        scriptDefinition
            .asLegacyOrNull<KotlinScriptDefinition>()
            ?.dependencyResolver
            ?.let { it is AsyncDependenciesResolver || it is LegacyResolverWrapper }
            ?: false

    override fun loadDependencies(
        isFirstLoad: Boolean,
        virtualFile: VirtualFile,
        scriptDefinition: ScriptDefinition,
        context: ScriptConfigurationLoadingContext
    ): Boolean {
        val file = project.getKtFile(virtualFile) ?: return false

        debug(file) { "start dependencies loading" }

        val result = refineScriptCompilationConfiguration(
            KtFileScriptSource(file), scriptDefinition, file.project
        )
        context.saveConfiguration(virtualFile, result, false)

        debug(file) { "finish dependencies loading" }

        return true
    }
}