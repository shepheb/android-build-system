/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.build.gradle.internal.res.namespaced

import com.android.build.gradle.internal.fixtures.FakeLogger
import com.android.ide.common.symbols.Symbol
import com.android.resources.ResourceType
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.artifacts.result.ComponentSelectionReason
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import java.io.File
import javax.tools.ToolProvider

/**
 * Creates a mock [ResolvedDependencyResult] with the given ID and immediate children.
 */
fun createDependency(
    id: String,
    children: MutableSet<DependencyResult> = ImmutableSet.of()
): MockResolvedDependencyResult = MockResolvedDependencyResult(
        MockResolvedComponentResult(
                MockComponentIdentifier(id),
                children
        )
)

/**
 * Compiles given Java sources and outputs them into the given java output directory.
 */
fun compileSources(sources: ImmutableList<File>, javacOutput: File) {
    val javac = ToolProvider.getSystemJavaCompiler()
    val manager = javac.getStandardFileManager(null, null, null)

    javac.getTask(
            null,
            manager, null,
            ImmutableList.of("-d", javacOutput.absolutePath), null,
            manager.getJavaFileObjectsFromFiles(sources)
    )
        .call()
}

/**
 * Creates a test only symbol with the given type and name, with a default value (empty list for
 * declare-styleables, '0' for other types). Does not verify the correctness of the resource name.
 */
fun symbol(type: String, name: String): Symbol {
    val resType = ResourceType.getEnum(type)!!
    if (resType == ResourceType.DECLARE_STYLEABLE) {
        return Symbol.StyleableSymbol(name, ImmutableList.of(), ImmutableList.of())
    }
    return Symbol.NormalSymbol(resType, name, 0)
}

class MockResolvedDependencyResult(
    private val selected: ResolvedComponentResult
) : ResolvedDependencyResult {
    override fun getSelected(): ResolvedComponentResult = selected
    override fun getFrom(): ResolvedComponentResult = error("not implemented")
    override fun getRequested(): ComponentSelector = error("not implemented")
}

class MockResolvedComponentResult(
    private val id: ComponentIdentifier,
    private val dependencies: MutableSet<DependencyResult>
) : ResolvedComponentResult {
    override fun getId(): ComponentIdentifier = id
    override fun getDependencies(): MutableSet<DependencyResult> = dependencies
    override fun getDependents(): MutableSet<ResolvedDependencyResult> = error("not implemented")
    override fun getSelectionReason(): ComponentSelectionReason = error("not implemented")
    override fun getModuleVersion(): ModuleVersionIdentifier? = error("not implemented")
    override fun getVariant(): ResolvedVariantResult = error("not implemented")
}

private class MockComponentIdentifier(val name: String) : ComponentIdentifier {
    override fun getDisplayName(): String = name
}

class MockLogger : FakeLogger() {
    val warnings = ArrayList<String>()
    val infos = ArrayList<String>()

    override fun warn(p0: String?) {
        warnings.add(p0!!)
    }

    override fun info(p0: String?) {
        infos.add(p0!!)
    }
}