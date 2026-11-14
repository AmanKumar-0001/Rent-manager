package com.tenantmanager.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestAuditTest {

    @Test
    fun mainManifest_doesNotDeclareInternetPermission() {
        val mainManifest = File("src/main/AndroidManifest.xml")
        if (mainManifest.exists()) {
            val content = mainManifest.readText()
            assertFalse("main AndroidManifest.xml must not declare INTERNET permission", content.contains("android.permission.INTERNET"))
        }
    }

    @Test
    fun localFlavorManifest_removesInternetPermission() {
        val localManifest = File("src/local/AndroidManifest.xml")
        if (localManifest.exists()) {
            val content = localManifest.readText()
            assertTrue("local AndroidManifest.xml must explicitly remove INTERNET permission",
                content.contains("android.permission.INTERNET") && content.contains("tools:node=\"remove\""))
        }
    }

    @Test
    fun sheetsFlavorManifest_declaresInternetPermission() {
        val sheetsManifest = File("src/sheets/AndroidManifest.xml")
        if (sheetsManifest.exists()) {
            val content = sheetsManifest.readText()
            assertTrue("sheets AndroidManifest.xml must declare INTERNET permission",
                content.contains("android.permission.INTERNET"))
        }
    }

    @Test
    fun mergedManifest_ifGenerated_verifiesInternetSeparation() {
        val localMerged = File("build/intermediates/merged_manifests/localDebug/processLocalDebugManifest/AndroidManifest.xml")
        if (localMerged.exists()) {
            val content = localMerged.readText()
            assertFalse("localDebug merged manifest must NOT contain INTERNET permission",
                content.contains("android.permission.INTERNET"))
        }

        val sheetsMerged = File("build/intermediates/merged_manifests/sheetsDebug/processSheetsDebugManifest/AndroidManifest.xml")
        if (sheetsMerged.exists()) {
            val content = sheetsMerged.readText()
            assertTrue("sheetsDebug merged manifest MUST contain INTERNET permission",
                content.contains("android.permission.INTERNET"))
        }
    }
}

