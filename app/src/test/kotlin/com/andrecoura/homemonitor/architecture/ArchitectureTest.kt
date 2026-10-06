package com.andrecoura.homemonitor.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/** The dependency rule of the architecture, checked on the source code. A violation fails the build. */
class ArchitectureTest {
    private val files = Konsist.scopeFromProduction().files
    private val base = "com.andrecoura.homemonitor"

    private fun importsOf(layer: String) = files.filter { it.packagee?.name?.startsWith("$base.$layer") == true }

    @Test
    fun `the three layers exist`() {
        listOf("domain", "data", "ui").forEach { org.junit.Assert.assertFalse("no files in $it", importsOf(it).isEmpty()) }
    }

    @Test
    fun `domain depends on nothing but itself and the JDK and coroutines`() {
        importsOf("domain").assertTrue { file ->
            file.imports.all { import ->
                val name = import.name
                name.startsWith("$base.domain") || name.startsWith("java.") || name.startsWith("kotlinx.coroutines") ||
                    name.startsWith("kotlin.")
            }
        }
    }

    @Test
    fun `data does not know the ui`() {
        importsOf("data").assertFalse { file -> file.imports.any { it.name.startsWith("$base.ui") } }
    }

    @Test
    fun `ui reaches data only through repositories and ports, never through Room or simulators`() {
        importsOf("ui").assertFalse { file -> file.imports.any { it.name.startsWith("$base.data") } }
    }

    @Test
    fun `view models never touch Android UI or Room types`() {
        Konsist.scopeFromProduction().classes().withNameEndingWith("ViewModel").assertTrue { vm ->
            vm.containingFile.imports.none {
                it.name.startsWith("androidx.compose") || it.name.startsWith("androidx.room") ||
                    it.name.startsWith("android.")
            }
        }
    }

    @Test
    fun `only the di package and the application class see simulators`() {
        files.filter { it.imports.any { import -> import.name.startsWith("$base.data.simulation") } }.assertTrue { file ->
            val pkg = file.packagee?.name.orEmpty()
            pkg.startsWith("$base.di") || pkg.startsWith("$base.data.simulation")
        }
    }
}
