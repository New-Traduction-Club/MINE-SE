package org.renpy.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileExplorerFilterTest {

    @Test
    fun testHiddenSystemFilesIdentification() {
        assertTrue(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/chaquopy")))
        assertTrue(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/generated.lock")))
        assertTrue(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/profileinstalled")))
        assertTrue(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/PersistedInstallation.xml")))
        assertTrue(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/PersistedInstallation.temp")))
        assertTrue(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/PersistedInstallation.")))
    }

    @Test
    fun testRegularFilesAreNotHidden() {
        assertFalse(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/game")))
        assertFalse(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/script.rpy")))
        assertFalse(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/archive.rpa")))
        assertFalse(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/chaquopy_other")))
        assertFalse(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/other.generated.lock")))
        assertFalse(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/PersistedInstallation")))
    }

    @Test
    fun testDotFilesAreNotHiddenBySystemFilter() {
        assertFalse(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/.nomedia")))
        assertFalse(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/.runtime_841.version")))
        assertFalse(FileExplorerViewModel.isHiddenSystemItem(File("/mock/path/.hidden_folder")))
    }
}
