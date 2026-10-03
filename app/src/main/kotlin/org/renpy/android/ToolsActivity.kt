package org.renpy.android

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.NestedScrollView
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.renpy.android.databinding.ActivityToolsBinding
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.LinkedBlockingQueue

interface PythonStreamCallback {
    fun onOutput(text: String)
    fun onInputRequest(): String
}

class ToolsActivity : GameWindowActivity() {

    private lateinit var binding: ActivityToolsBinding

    private enum class InputType {
        NONE,
        APP_FILE,
        APP_FOLDER,
        SAF_FILES,
        SAF_FOLDER
    }

    private var currentInputType = InputType.NONE
    private var selectedAppPath: String? = null
    private var selectedSafUris: List<Uri> = emptyList()
    private var selectedSafTreeUri: Uri? = null

    private var customOutputDir: File? = null
    private var customOutputTreeUri: Uri? = null

    private var isDecompiling = false

    private val appFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data?.getStringExtra(FileExplorerActivity.EXTRA_SELECTED_PATH)
            if (!path.isNullOrBlank()) {
                currentInputType = InputType.APP_FILE
                selectedAppPath = path
                selectedSafUris = emptyList()
                selectedSafTreeUri = null
                updateInputUI()
            }
        }
    }

    private val appFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data?.getStringExtra(FileExplorerActivity.EXTRA_SELECTED_PATH)
            if (!path.isNullOrBlank()) {
                currentInputType = InputType.APP_FOLDER
                selectedAppPath = path
                selectedSafUris = emptyList()
                selectedSafTreeUri = null
                updateInputUI()
            }
        }
    }

    private val safFilesPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<Uri>()
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } ?: result.data?.data?.let { uri ->
                uris.add(uri)
            }

            if (uris.isNotEmpty()) {
                currentInputType = InputType.SAF_FILES
                selectedSafUris = uris
                selectedAppPath = null
                selectedSafTreeUri = null
                updateInputUI()
            }
        }
    }

    private val safFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val treeUri = result.data?.data
            if (treeUri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }

                currentInputType = InputType.SAF_FOLDER
                selectedSafTreeUri = treeUri
                selectedAppPath = null
                selectedSafUris = emptyList()
                updateInputUI()
            }
        }
    }

    private val customOutputFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data?.getStringExtra(FileExplorerActivity.EXTRA_SELECTED_PATH)
            if (!path.isNullOrBlank()) {
                customOutputDir = File(path)
                customOutputTreeUri = null
                updateOutputUI()
            }
        }
    }

    private val customOutputSafFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val treeUri = result.data?.data
            if (treeUri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
                customOutputTreeUri = treeUri
                customOutputDir = null
                updateOutputUI()
            }
        }
    }

    private var extractorInputType = InputType.NONE
    private var extractorAppPath: String? = null
    private var extractorSafUris: List<Uri> = emptyList()
    private var extractorSafTreeUri: Uri? = null

    private var extractorCustomOutputDir: File? = null
    private var extractorCustomOutputTreeUri: Uri? = null

    private var isExtracting = false

    private val extractorAppFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data?.getStringExtra(FileExplorerActivity.EXTRA_SELECTED_PATH)
            if (!path.isNullOrBlank()) {
                extractorInputType = InputType.APP_FILE
                extractorAppPath = path
                extractorSafUris = emptyList()
                extractorSafTreeUri = null
                updateExtractorInputUI()
            }
        }
    }

    private val extractorAppFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data?.getStringExtra(FileExplorerActivity.EXTRA_SELECTED_PATH)
            if (!path.isNullOrBlank()) {
                extractorInputType = InputType.APP_FOLDER
                extractorAppPath = path
                extractorSafUris = emptyList()
                extractorSafTreeUri = null
                updateExtractorInputUI()
            }
        }
    }

    private val extractorSafFilesPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uris = mutableListOf<Uri>()
            result.data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
            } ?: result.data?.data?.let { uri ->
                uris.add(uri)
            }

            val rpaUris = uris.filter { uri ->
                val name = getFileNameFromUri(uri)
                name == null || name.endsWith(".rpa", ignoreCase = true)
            }

            if (rpaUris.isNotEmpty()) {
                extractorInputType = InputType.SAF_FILES
                extractorSafUris = rpaUris
                extractorAppPath = null
                extractorSafTreeUri = null
                updateExtractorInputUI()
            }
        }
    }

    private val extractorSafFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val treeUri = result.data?.data
            if (treeUri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }

                extractorInputType = InputType.SAF_FOLDER
                extractorSafTreeUri = treeUri
                extractorAppPath = null
                extractorSafUris = emptyList()
                updateExtractorInputUI()
            }
        }
    }

    private val extractorCustomOutputFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data?.getStringExtra(FileExplorerActivity.EXTRA_SELECTED_PATH)
            if (!path.isNullOrBlank()) {
                extractorCustomOutputDir = File(path)
                extractorCustomOutputTreeUri = null
                updateExtractorOutputUI()
            }
        }
    }

    private val extractorCustomOutputSafFolderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val treeUri = result.data?.data
            if (treeUri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
                extractorCustomOutputTreeUri = treeUri
                extractorCustomOutputDir = null
                updateExtractorOutputUI()
            }
        }
    }

    private enum class PythonInputType {
        NONE,
        APP_FILE,
        SAF_FILE
    }

    private var pythonInputType = PythonInputType.NONE
    private var pythonAppPath: String? = null
    private var pythonSafUri: Uri? = null
    private var isRunningPython = false
    private val pythonInputQueue = LinkedBlockingQueue<String>()
    private var pythonLogBuffer: StringBuilder? = null

    private val pythonAppFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val path = result.data?.getStringExtra(FileExplorerActivity.EXTRA_SELECTED_PATH)
            if (!path.isNullOrBlank()) {
                pythonInputType = PythonInputType.APP_FILE
                pythonAppPath = path
                pythonSafUri = null
                updatePythonInputUI()
            }
        }
    }

    private val pythonSafFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }

                val name = getFileNameFromUri(uri)
                if (name != null && !name.endsWith(".py", ignoreCase = true)) {
                    Toast.makeText(this, getString(R.string.tools_python_select_script_required), Toast.LENGTH_SHORT)
                        .show()
                } else {
                    pythonInputType = PythonInputType.SAF_FILE
                    pythonSafUri = uri
                    pythonAppPath = null
                    updatePythonInputUI()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTitle(getString(R.string.title_tools))
        ensureToolsHomeDirectory()
        ensureExtractorHomeDirectory()
        ensurePythonRunnerHomeDirectory()

        setupToolsMenu()
        setupDecompilerView()
        setupExtractorView()
        setupPythonRunnerView()
    }

    private fun ensureToolsHomeDirectory(): File {
        val toolsDir = File(filesDir, "tools/unrpyc")
        if (!toolsDir.exists()) {
            toolsDir.mkdirs()
        }
        return toolsDir
    }

    private fun ensureExtractorHomeDirectory(): File {
        val toolsDir = File(filesDir, "tools/rpa_extractor")
        if (!toolsDir.exists()) {
            toolsDir.mkdirs()
        }
        return toolsDir
    }

    private fun ensurePythonRunnerHomeDirectory(): File {
        val toolsDir = File(filesDir, "tools/python_runner")
        if (!toolsDir.exists()) {
            toolsDir.mkdirs()
        }
        return toolsDir
    }

    private fun setupToolsMenu() {
        binding.cardRpycDecompiler.setOnClickListener {
            SoundEffects.playClick(this)
            showDecompilerView()
        }
        binding.cardRpaExtractor.setOnClickListener {
            SoundEffects.playClick(this)
            showExtractorView()
        }
        binding.cardPythonRunner.setOnClickListener {
            SoundEffects.playClick(this)
            showPythonRunnerView()
        }
    }

    private fun showDecompilerView() {
        binding.layoutToolsMenu.visibility = View.GONE
        binding.layoutExtractor.visibility = View.GONE
        binding.layoutPythonRunner.visibility = View.GONE
        binding.layoutDecompiler.visibility = View.VISIBLE
        setTitle(getString(R.string.tools_rpyc_decompiler))
        updateInputUI()
        updateOutputUI()
    }

    private fun hideDecompilerView() {
        if (isDecompiling) return
        binding.layoutDecompiler.visibility = View.GONE
        binding.layoutToolsMenu.visibility = View.VISIBLE
        setTitle(getString(R.string.title_tools))
    }

    private fun showExtractorView() {
        binding.layoutToolsMenu.visibility = View.GONE
        binding.layoutDecompiler.visibility = View.GONE
        binding.layoutPythonRunner.visibility = View.GONE
        binding.layoutExtractor.visibility = View.VISIBLE
        setTitle(getString(R.string.tools_rpa_extractor))
        updateExtractorInputUI()
        updateExtractorOutputUI()
    }

    private fun hideExtractorView() {
        if (isExtracting) return
        binding.layoutExtractor.visibility = View.GONE
        binding.layoutToolsMenu.visibility = View.VISIBLE
        setTitle(getString(R.string.title_tools))
    }

    private fun showPythonRunnerView() {
        binding.layoutToolsMenu.visibility = View.GONE
        binding.layoutDecompiler.visibility = View.GONE
        binding.layoutExtractor.visibility = View.GONE
        binding.layoutPythonRunner.visibility = View.VISIBLE
        setTitle(getString(R.string.tools_python_runner))
        updatePythonInputUI()
    }

    private fun hidePythonRunnerView() {
        if (isRunningPython) {
            pythonInputQueue.offer("\n")
            return
        }
        binding.layoutPythonRunner.visibility = View.GONE
        binding.layoutToolsMenu.visibility = View.VISIBLE
        setTitle(getString(R.string.title_tools))
    }

    private fun setupDecompilerView() {
        binding.btnBackToTools.setOnClickListener {
            SoundEffects.playClick(this)
            hideDecompilerView()
        }

        binding.btnSelectInputFile.setOnClickListener {
            SoundEffects.playClick(this)
            showStorageChoiceDialog(isFolder = false)
        }

        binding.btnSelectInputFolder.setOnClickListener {
            SoundEffects.playClick(this)
            showStorageChoiceDialog(isFolder = true)
        }

        binding.btnChangeOutputFolder.setOnClickListener {
            SoundEffects.playClick(this)
            showOutputStorageChoiceDialog()
        }

        binding.btnResetOutputFolder.setOnClickListener {
            SoundEffects.playClick(this)
            customOutputDir = null
            customOutputTreeUri = null
            updateOutputUI()
        }

        binding.btnStartDecompile.setOnClickListener {
            SoundEffects.playClick(this)
            startDecompilation()
        }
    }

    private fun showStorageChoiceDialog(isFolder: Boolean) {
        val options = arrayOf(
            getString(R.string.tools_app_files),
            getString(R.string.tools_external_storage)
        )

        GameDialogBuilder(this)
            .setTitle(getString(R.string.tools_storage_prompt))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, FileExplorerActivity::class.java).apply {
                            putExtra(FileExplorerActivity.EXTRA_PICKER_MODE, true)
                            putExtra(
                                FileExplorerActivity.EXTRA_PICKER_TYPE,
                                if (isFolder) FileExplorerActivity.PICKER_TYPE_FOLDER else FileExplorerActivity.PICKER_TYPE_FILE
                            )
                            if (!isFolder) {
                                putExtra(FileExplorerActivity.EXTRA_PICKER_EXTENSION, ".rpyc,.rpymc")
                            }
                            putExtra("startPath", filesDir.absolutePath)
                        }
                        if (isFolder) {
                            appFolderPickerLauncher.launch(intent)
                        } else {
                            appFilePickerLauncher.launch(intent)
                        }
                    }

                    1 -> {
                        if (isFolder) {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            safFolderPickerLauncher.launch(intent)
                        } else {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            }
                            safFilesPickerLauncher.launch(intent)
                        }
                    }
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }

    private fun showOutputStorageChoiceDialog() {
        val options = arrayOf(
            getString(R.string.tools_app_files),
            getString(R.string.tools_external_storage)
        )

        GameDialogBuilder(this)
            .setTitle(getString(R.string.tools_storage_prompt))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, FileExplorerActivity::class.java).apply {
                            putExtra(FileExplorerActivity.EXTRA_PICKER_MODE, true)
                            putExtra(FileExplorerActivity.EXTRA_PICKER_TYPE, FileExplorerActivity.PICKER_TYPE_FOLDER)
                            putExtra("startPath", filesDir.absolutePath)
                        }
                        customOutputFolderLauncher.launch(intent)
                    }

                    1 -> {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        customOutputSafFolderLauncher.launch(intent)
                    }
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }

    private fun updateInputUI() {
        val summary = when (currentInputType) {
            InputType.APP_FILE -> selectedAppPath ?: getString(R.string.tools_no_selection)
            InputType.APP_FOLDER -> selectedAppPath ?: getString(R.string.tools_no_selection)
            InputType.SAF_FILES -> {
                val count = selectedSafUris.size
                if (count == 1) {
                    selectedSafUris[0].lastPathSegment ?: getString(R.string.tools_files_selected, 1)
                } else {
                    getString(R.string.tools_files_selected, count)
                }
            }

            InputType.SAF_FOLDER -> selectedSafTreeUri?.lastPathSegment ?: getString(R.string.tools_external_folder)
            InputType.NONE -> getString(R.string.tools_no_selection)
        }
        binding.txtSelectedInput.text = summary
        updateOutputUI()
    }

    private fun updateOutputUI() {
        val outputText = when {
            customOutputDir != null -> customOutputDir!!.absolutePath
            customOutputTreeUri != null -> customOutputTreeUri!!.lastPathSegment
                ?: getString(R.string.tools_external_folder)

            currentInputType == InputType.SAF_FILES -> ensureToolsHomeDirectory().absolutePath
            else -> getString(R.string.tools_original_location)
        }
        binding.txtSelectedOutput.text = outputText
    }

    private fun setupExtractorView() {
        binding.btnBackToToolsExtractor.setOnClickListener {
            SoundEffects.playClick(this)
            hideExtractorView()
        }

        binding.btnSelectInputFileExtractor.setOnClickListener {
            SoundEffects.playClick(this)
            showExtractorStorageChoiceDialog(isFolder = false)
        }

        binding.btnSelectInputFolderExtractor.setOnClickListener {
            SoundEffects.playClick(this)
            showExtractorStorageChoiceDialog(isFolder = true)
        }

        binding.btnChangeOutputFolderExtractor.setOnClickListener {
            SoundEffects.playClick(this)
            showExtractorOutputStorageChoiceDialog()
        }

        binding.btnResetOutputFolderExtractor.setOnClickListener {
            SoundEffects.playClick(this)
            extractorCustomOutputDir = null
            extractorCustomOutputTreeUri = null
            updateExtractorOutputUI()
        }

        binding.btnStartExtract.setOnClickListener {
            SoundEffects.playClick(this)
            startExtraction()
        }
    }

    private fun showExtractorStorageChoiceDialog(isFolder: Boolean) {
        val options = arrayOf(
            getString(R.string.tools_app_files),
            getString(R.string.tools_external_storage)
        )

        GameDialogBuilder(this)
            .setTitle(getString(R.string.tools_storage_prompt))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, FileExplorerActivity::class.java).apply {
                            putExtra(FileExplorerActivity.EXTRA_PICKER_MODE, true)
                            putExtra(
                                FileExplorerActivity.EXTRA_PICKER_TYPE,
                                if (isFolder) FileExplorerActivity.PICKER_TYPE_FOLDER else FileExplorerActivity.PICKER_TYPE_FILE
                            )
                            if (!isFolder) {
                                putExtra(FileExplorerActivity.EXTRA_PICKER_EXTENSION, ".rpa")
                            }
                            putExtra("startPath", filesDir.absolutePath)
                        }
                        if (isFolder) {
                            extractorAppFolderPickerLauncher.launch(intent)
                        } else {
                            extractorAppFilePickerLauncher.launch(intent)
                        }
                    }

                    1 -> {
                        if (isFolder) {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            extractorSafFolderPickerLauncher.launch(intent)
                        } else {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            }
                            extractorSafFilesPickerLauncher.launch(intent)
                        }
                    }
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }

    private fun showExtractorOutputStorageChoiceDialog() {
        val options = arrayOf(
            getString(R.string.tools_app_files),
            getString(R.string.tools_external_storage)
        )

        GameDialogBuilder(this)
            .setTitle(getString(R.string.tools_storage_prompt))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, FileExplorerActivity::class.java).apply {
                            putExtra(FileExplorerActivity.EXTRA_PICKER_MODE, true)
                            putExtra(FileExplorerActivity.EXTRA_PICKER_TYPE, FileExplorerActivity.PICKER_TYPE_FOLDER)
                            putExtra("startPath", filesDir.absolutePath)
                        }
                        extractorCustomOutputFolderLauncher.launch(intent)
                    }

                    1 -> {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        extractorCustomOutputSafFolderLauncher.launch(intent)
                    }
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }

    private fun updateExtractorInputUI() {
        val summary = when (extractorInputType) {
            InputType.APP_FILE -> extractorAppPath ?: getString(R.string.tools_no_selection)
            InputType.APP_FOLDER -> extractorAppPath ?: getString(R.string.tools_no_selection)
            InputType.SAF_FILES -> {
                val count = extractorSafUris.size
                if (count == 1) {
                    getFileNameFromUri(extractorSafUris[0]) ?: extractorSafUris[0].lastPathSegment
                    ?: getString(R.string.tools_files_selected, 1)
                } else {
                    getString(R.string.tools_files_selected, count)
                }
            }

            InputType.SAF_FOLDER -> extractorSafTreeUri?.lastPathSegment ?: getString(R.string.tools_external_folder)
            InputType.NONE -> getString(R.string.tools_no_selection)
        }
        binding.txtSelectedInputExtractor.text = summary
        updateExtractorOutputUI()
    }

    private fun updateExtractorOutputUI() {
        val outputText = when {
            extractorCustomOutputDir != null -> extractorCustomOutputDir!!.absolutePath
            extractorCustomOutputTreeUri != null -> extractorCustomOutputTreeUri!!.lastPathSegment
                ?: getString(R.string.tools_external_folder)

            extractorInputType == InputType.SAF_FILES -> ensureExtractorHomeDirectory().absolutePath
            else -> getString(R.string.tools_original_location)
        }
        binding.txtSelectedOutputExtractor.text = outputText
    }

    private fun startDecompilation() {
        if (isDecompiling) return
        if (currentInputType == InputType.NONE) {
            binding.txtDecompileStatus.text = getString(R.string.tools_select_input_required)
            return
        }

        isDecompiling = true
        binding.btnStartDecompile.isEnabled = false
        binding.progressBarDecompile.progress = 0
        binding.txtDecompileStatus.text = getString(R.string.tools_decompiling)
        binding.txtDecompileLog.text = ""

        lifecycleScope.launch(Dispatchers.IO) {
            val logBuffer = StringBuilder()

            fun appendLog(line: String) {
                logBuffer.append(line).append("\n")
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.txtDecompileLog.text = logBuffer.toString()
                    binding.scrollConsole.fullScroll(View.FOCUS_DOWN)
                }
            }

            fun updateProgress(current: Int, total: Int, label: String) {
                lifecycleScope.launch(Dispatchers.Main) {
                    val pct = if (total > 0) ((current.toFloat() / total.toFloat()) * 100).toInt() else 0
                    binding.progressBarDecompile.progress = pct
                    binding.txtDecompileStatus.text = "$current / $total: $label"
                }
            }

            var successCount = 0
            var failCount = 0

            try {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(applicationContext))
                }
                val py = Python.getInstance()
                val bridge = py.getModule("decompiler_bridge")
                val overwrite = binding.cbOverwriteRpy.isChecked

                when (currentInputType) {
                    InputType.APP_FILE -> {
                        val inFile = File(selectedAppPath!!)
                        val outName = inFile.name.replace(".rpyc", ".rpy").replace(".rpymc", ".rpym")
                        updateProgress(0, 1, inFile.name)
                        appendLog("Decompiling ${inFile.name}...")

                        if (customOutputTreeUri != null) {
                            val targetDirDoc = DocumentFile.fromTreeUri(applicationContext, customOutputTreeUri!!)
                            val tempOut = File(cacheDir, outName)
                            val res =
                                bridge.callAttr("decompile_file", inFile.absolutePath, tempOut.absolutePath, overwrite)
                            if (res.callAttr("get", "success").toBoolean() && tempOut.exists()) {
                                if (targetDirDoc != null && writeTempFileToDocumentFile(
                                        tempOut,
                                        targetDirDoc,
                                        tempOut.name,
                                        overwrite
                                    )
                                ) {
                                    successCount++
                                    appendLog("Done: ${tempOut.name}")
                                } else {
                                    failCount++
                                    appendLog("Failed to write to external folder.")
                                }
                                tempOut.delete()
                            } else {
                                failCount++
                                val err = res.callAttr("get", "error")?.toString()
                                appendLog("Failed: $err")
                            }
                        } else {
                            val outDir = customOutputDir?.absolutePath
                            val res = bridge.callAttr("decompile_file", inFile.absolutePath, outDir, overwrite)
                            val ok = res.callAttr("get", "success").toBoolean()
                            val outPath = res.callAttr("get", "output_path")?.toString()
                            val err = res.callAttr("get", "error")?.toString()

                            if (ok) {
                                successCount++
                                appendLog("Done: $outPath")
                            } else {
                                failCount++
                                appendLog("Failed: $err")
                            }
                        }
                        updateProgress(1, 1, inFile.name)
                    }

                    InputType.APP_FOLDER -> {
                        val inFolder = File(selectedAppPath!!)
                        appendLog("Scanning folder for .rpyc files...")
                        val pyFileList = bridge.callAttr("find_rpyc_files", inFolder.absolutePath).asList()
                        val total = pyFileList.size
                        appendLog("Found $total .rpyc files.")

                        for ((index, item) in pyFileList.withIndex()) {
                            val filePath = item.toString()
                            val inFile = File(filePath)
                            val relativePath = inFile.relativeTo(inFolder).path
                            val outName = inFile.name.replace(".rpyc", ".rpy").replace(".rpymc", ".rpym")

                            updateProgress(index + 1, total, inFile.name)
                            appendLog("Decompiling: $relativePath")

                            if (customOutputTreeUri != null) {
                                val targetDirDoc = DocumentFile.fromTreeUri(applicationContext, customOutputTreeUri!!)
                                val tempOut = File(cacheDir, outName)
                                val res = bridge.callAttr("decompile_file", filePath, tempOut.absolutePath, overwrite)
                                if (res.callAttr("get", "success").toBoolean() && tempOut.exists()) {
                                    if (targetDirDoc != null && writeTempFileToDocumentFile(
                                            tempOut,
                                            targetDirDoc,
                                            tempOut.name,
                                            overwrite
                                        )
                                    ) {
                                        successCount++
                                    } else {
                                        failCount++
                                    }
                                    tempOut.delete()
                                } else {
                                    failCount++
                                }
                            } else {
                                val outPath = if (customOutputDir != null) {
                                    val target = File(customOutputDir, relativePath)
                                    target.parentFile?.mkdirs()
                                    target.path.replace(".rpyc", ".rpy").replace(".rpymc", ".rpym")
                                } else null

                                val res = bridge.callAttr("decompile_file", filePath, outPath, overwrite)
                                val ok = res.callAttr("get", "success").toBoolean()
                                if (ok) {
                                    successCount++
                                } else {
                                    failCount++
                                    val err = res.callAttr("get", "error")?.toString()
                                    appendLog("Error on ${inFile.name}: $err")
                                }
                            }
                        }
                    }

                    InputType.SAF_FILES -> {
                        val total = selectedSafUris.size
                        val targetDir = customOutputDir ?: ensureToolsHomeDirectory()
                        appendLog("Processing $total external file(s)...")

                        for ((index, uri) in selectedSafUris.withIndex()) {
                            val fileName = getFileNameFromUri(uri) ?: "script_$index.rpyc"
                            val outName = fileName.replace(".rpyc", ".rpy").replace(".rpymc", ".rpym")
                            updateProgress(index + 1, total, fileName)
                            appendLog("Reading $fileName from external storage...")

                            val tempFile = File(cacheDir, "temp_decompile_$fileName")
                            try {
                                contentResolver.openInputStream(uri)?.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                if (customOutputTreeUri != null) {
                                    val targetDirDoc =
                                        DocumentFile.fromTreeUri(applicationContext, customOutputTreeUri!!)
                                    val tempOut = File(cacheDir, outName)
                                    val res = bridge.callAttr(
                                        "decompile_file",
                                        tempFile.absolutePath,
                                        tempOut.absolutePath,
                                        overwrite
                                    )
                                    if (res.callAttr("get", "success").toBoolean() && tempOut.exists()) {
                                        if (targetDirDoc != null && writeTempFileToDocumentFile(
                                                tempOut,
                                                targetDirDoc,
                                                tempOut.name,
                                                overwrite
                                            )
                                        ) {
                                            successCount++
                                            appendLog("Done: ${tempOut.name}")
                                        } else {
                                            failCount++
                                            appendLog("Failed to write to external folder.")
                                        }
                                        tempOut.delete()
                                    } else {
                                        failCount++
                                        val err = res.callAttr("get", "error")?.toString()
                                        appendLog("Error: $err")
                                    }
                                } else {
                                    val targetDir = customOutputDir ?: ensureToolsHomeDirectory()
                                    val outTarget = File(targetDir, outName)
                                    val res = bridge.callAttr(
                                        "decompile_file",
                                        tempFile.absolutePath,
                                        outTarget.absolutePath,
                                        overwrite
                                    )
                                    val ok = res.callAttr("get", "success").toBoolean()
                                    if (ok) {
                                        successCount++
                                        appendLog("Done: ${outTarget.name}")
                                    } else {
                                        failCount++
                                        val err = res.callAttr("get", "error")?.toString()
                                        appendLog("Error: $err")
                                    }
                                }
                            } catch (e: Exception) {
                                failCount++
                                appendLog("Failed to read $fileName: ${e.message}")
                            } finally {
                                tempFile.delete()
                            }
                        }
                    }

                    InputType.SAF_FOLDER -> {
                        val treeUri = selectedSafTreeUri!!
                        appendLog("Scanning external tree for .rpyc files...")
                        val rootDoc = DocumentFile.fromTreeUri(applicationContext, treeUri)
                        if (rootDoc == null || !rootDoc.isDirectory) {
                            appendLog("Failed to access external directory.")
                        } else {
                            val rpycDocs = mutableListOf<DocumentFile>()
                            collectRpycDocuments(rootDoc, rpycDocs)
                            val total = rpycDocs.size
                            appendLog("Found $total .rpyc files in external folder.")

                            for ((index, doc) in rpycDocs.withIndex()) {
                                val name = doc.name ?: "script.rpyc"
                                val outName = name.replace(".rpyc", ".rpy").replace(".rpymc", ".rpym")
                                updateProgress(index + 1, total, name)
                                appendLog("Decompiling: $name")

                                val tempFile = File(cacheDir, "temp_$name")
                                try {
                                    contentResolver.openInputStream(doc.uri)?.use { input ->
                                        FileOutputStream(tempFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }

                                    if (customOutputDir != null) {
                                        val outTarget = File(customOutputDir, outName)
                                        val res = bridge.callAttr(
                                            "decompile_file",
                                            tempFile.absolutePath,
                                            outTarget.absolutePath,
                                            overwrite
                                        )
                                        if (res.callAttr("get", "success").toBoolean()) {
                                            successCount++
                                            appendLog("Done: ${outTarget.name}")
                                        } else {
                                            failCount++
                                            appendLog("Error: ${res.callAttr("get", "error")?.toString()}")
                                        }
                                    } else {
                                        val targetDirDoc = if (customOutputTreeUri != null) {
                                            DocumentFile.fromTreeUri(applicationContext, customOutputTreeUri!!)
                                                ?: rootDoc
                                        } else {
                                            doc.parentFile ?: rootDoc
                                        }

                                        val tempOut = File(cacheDir, outName)
                                        val res = bridge.callAttr(
                                            "decompile_file",
                                            tempFile.absolutePath,
                                            tempOut.absolutePath,
                                            overwrite
                                        )
                                        if (res.callAttr("get", "success").toBoolean() && tempOut.exists()) {
                                            if (writeTempFileToDocumentFile(
                                                    tempOut,
                                                    targetDirDoc,
                                                    tempOut.name,
                                                    overwrite
                                                )
                                            ) {
                                                successCount++
                                                appendLog("Done: ${tempOut.name}")
                                            } else {
                                                failCount++
                                                appendLog("Failed to write ${tempOut.name} to external storage.")
                                            }
                                            tempOut.delete()
                                        } else {
                                            failCount++
                                            val err = res.callAttr("get", "error")?.toString()
                                            appendLog("Error: $err")
                                        }
                                    }
                                } catch (e: Exception) {
                                    failCount++
                                    appendLog("Exception on $name: ${e.message}")
                                } finally {
                                    tempFile.delete()
                                }
                            }
                        }
                    }

                    InputType.NONE -> {}
                }
            } catch (e: Exception) {
                appendLog("Fatal error during decompilation: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    isDecompiling = false
                    binding.btnStartDecompile.isEnabled = true
                    val summary = getString(R.string.tools_finished_summary, successCount, failCount)
                    binding.txtDecompileStatus.text = summary
                    appendLog(summary)
                }
            }
        }
    }

    private fun writeTempFileToDocumentFile(
        sourceFile: File,
        targetDir: DocumentFile,
        displayName: String,
        overwrite: Boolean
    ): Boolean {
        val existing = targetDir.findFile(displayName)
        if (existing != null) {
            if (!overwrite) {
                return true
            }
            existing.delete()
        }
        val created = targetDir.createFile("application/octet-stream", displayName) ?: return false
        contentResolver.openOutputStream(created.uri)?.use { outStream ->
            sourceFile.inputStream().use { inStream ->
                inStream.copyTo(outStream)
            }
        }
        return true
    }

    private fun collectRpycDocuments(folder: DocumentFile, resultList: MutableList<DocumentFile>) {
        val files = folder.listFiles()
        for (f in files) {
            if (f.isDirectory) {
                collectRpycDocuments(f, resultList)
            } else if (f.name?.endsWith(".rpyc", ignoreCase = true) == true || f.name?.endsWith(
                    ".rpymc",
                    ignoreCase = true
                ) == true
            ) {
                resultList.add(f)
            }
        }
    }

    private fun startExtraction() {
        if (isExtracting) return
        if (extractorInputType == InputType.NONE) {
            binding.txtExtractStatus.text = getString(R.string.tools_select_input_required)
            return
        }

        isExtracting = true
        binding.btnStartExtract.isEnabled = false
        binding.progressBarExtract.progress = 0
        binding.txtExtractStatus.text = getString(R.string.tools_extracting)
        binding.txtExtractLog.text = ""

        lifecycleScope.launch(Dispatchers.IO) {
            val logBuffer = StringBuilder()

            fun appendLog(line: String) {
                logBuffer.append(line).append("\n")
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.txtExtractLog.text = logBuffer.toString()
                    binding.scrollConsoleExtractor.fullScroll(View.FOCUS_DOWN)
                }
            }

            fun updateProgress(current: Int, total: Int, label: String) {
                lifecycleScope.launch(Dispatchers.Main) {
                    val pct = if (total > 0) ((current.toFloat() / total.toFloat()) * 100).toInt() else 0
                    binding.progressBarExtract.progress = pct
                    binding.txtExtractStatus.text = "$current / $total: $label"
                }
            }

            var totalExtracted = 0
            var totalFailed = 0

            val overwrite = binding.cbExtractorOverwrite.isChecked
            val useSubfolder = binding.cbExtractorSubfolder.isChecked

            try {
                when (extractorInputType) {
                    InputType.APP_FILE -> {
                        val inFile = File(extractorAppPath!!)
                        appendLog("Extracting ${inFile.name}...")

                        val (extracted, failed) = if (extractorCustomOutputTreeUri != null) {
                            val targetDoc = DocumentFile.fromTreeUri(applicationContext, extractorCustomOutputTreeUri!!)
                            if (targetDoc == null) {
                                appendLog("Failed to access external storage output folder.")
                                Pair(0, 1)
                            } else {
                                extractRpaFileToDocument(
                                    inFile,
                                    targetDoc,
                                    useSubfolder,
                                    inFile.nameWithoutExtension,
                                    overwrite,
                                    ::updateProgress,
                                    ::appendLog
                                )
                            }
                        } else {
                            val targetDir =
                                extractorCustomOutputDir ?: inFile.parentFile ?: ensureExtractorHomeDirectory()
                            extractRpaFileToDirectory(
                                inFile,
                                targetDir,
                                useSubfolder,
                                inFile.nameWithoutExtension,
                                overwrite,
                                ::updateProgress,
                                ::appendLog
                            )
                        }

                        totalExtracted += extracted
                        totalFailed += failed
                        appendLog("Finished ${inFile.name}: $extracted extracted, $failed failed.")
                    }

                    InputType.APP_FOLDER -> {
                        val inFolder = File(extractorAppPath!!)
                        appendLog("Scanning folder for .rpa files...")
                        val rpaFiles = mutableListOf<File>()
                        collectRpaFiles(inFolder, rpaFiles)
                        val totalArchives = rpaFiles.size
                        appendLog("Found $totalArchives .rpa archive(s).")

                        for ((idx, rpaFile) in rpaFiles.withIndex()) {
                            appendLog("Archive [${idx + 1}/$totalArchives]: ${rpaFile.name}...")

                            val (extracted, failed) = if (extractorCustomOutputTreeUri != null) {
                                val targetDoc =
                                    DocumentFile.fromTreeUri(applicationContext, extractorCustomOutputTreeUri!!)
                                if (targetDoc == null) {
                                    appendLog("Failed to access external storage output folder.")
                                    Pair(0, 1)
                                } else {
                                    extractRpaFileToDocument(
                                        rpaFile,
                                        targetDoc,
                                        useSubfolder,
                                        rpaFile.nameWithoutExtension,
                                        overwrite,
                                        ::updateProgress,
                                        ::appendLog
                                    )
                                }
                            } else {
                                val targetDir =
                                    extractorCustomOutputDir ?: rpaFile.parentFile ?: ensureExtractorHomeDirectory()
                                extractRpaFileToDirectory(
                                    rpaFile,
                                    targetDir,
                                    useSubfolder,
                                    rpaFile.nameWithoutExtension,
                                    overwrite,
                                    ::updateProgress,
                                    ::appendLog
                                )
                            }

                            totalExtracted += extracted
                            totalFailed += failed
                            appendLog("Done ${rpaFile.name}: $extracted extracted, $failed failed.")
                        }
                    }

                    InputType.SAF_FILES -> {
                        val totalArchives = extractorSafUris.size
                        appendLog("Processing $totalArchives external .rpa file(s)...")

                        for ((idx, uri) in extractorSafUris.withIndex()) {
                            val fileName = getFileNameFromUri(uri) ?: "archive_$idx.rpa"
                            val archiveBaseName = File(fileName).nameWithoutExtension
                            appendLog("Archive [${idx + 1}/$totalArchives]: $fileName...")

                            val tempFile = File(cacheDir, "temp_extract_$fileName")
                            try {
                                contentResolver.openInputStream(uri)?.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                val (extracted, failed) = if (extractorCustomOutputTreeUri != null) {
                                    val targetDoc =
                                        DocumentFile.fromTreeUri(applicationContext, extractorCustomOutputTreeUri!!)
                                    if (targetDoc == null) {
                                        appendLog("Failed to access external storage output folder.")
                                        Pair(0, 1)
                                    } else {
                                        extractRpaFileToDocument(
                                            tempFile,
                                            targetDoc,
                                            useSubfolder,
                                            archiveBaseName,
                                            overwrite,
                                            ::updateProgress,
                                            ::appendLog
                                        )
                                    }
                                } else {
                                    val targetDir = extractorCustomOutputDir ?: ensureExtractorHomeDirectory()
                                    extractRpaFileToDirectory(
                                        tempFile,
                                        targetDir,
                                        useSubfolder,
                                        archiveBaseName,
                                        overwrite,
                                        ::updateProgress,
                                        ::appendLog
                                    )
                                }

                                totalExtracted += extracted
                                totalFailed += failed
                                appendLog("Done $fileName: $extracted extracted, $failed failed.")
                            } catch (e: Exception) {
                                totalFailed++
                                appendLog("Failed reading $fileName: ${e.message}")
                            } finally {
                                tempFile.delete()
                            }
                        }
                    }

                    InputType.SAF_FOLDER -> {
                        val treeUri = extractorSafTreeUri!!
                        appendLog("Scanning external folder for .rpa files...")
                        val rootDoc = DocumentFile.fromTreeUri(applicationContext, treeUri)
                        if (rootDoc == null || !rootDoc.isDirectory) {
                            appendLog("Failed to access external directory.")
                        } else {
                            val rpaDocs = mutableListOf<DocumentFile>()
                            collectRpaDocuments(rootDoc, rpaDocs)
                            val totalArchives = rpaDocs.size
                            appendLog("Found $totalArchives .rpa archive(s) in external folder.")

                            for ((idx, doc) in rpaDocs.withIndex()) {
                                val name = doc.name ?: "archive_$idx.rpa"
                                val archiveBaseName = File(name).nameWithoutExtension
                                appendLog("Archive [${idx + 1}/$totalArchives]: $name...")

                                val tempFile = File(cacheDir, "temp_extract_$name")
                                try {
                                    contentResolver.openInputStream(doc.uri)?.use { input ->
                                        FileOutputStream(tempFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }

                                    val (extracted, failed) = if (extractorCustomOutputDir != null) {
                                        extractRpaFileToDirectory(
                                            tempFile,
                                            extractorCustomOutputDir!!,
                                            useSubfolder,
                                            archiveBaseName,
                                            overwrite,
                                            ::updateProgress,
                                            ::appendLog
                                        )
                                    } else {
                                        val targetDoc = if (extractorCustomOutputTreeUri != null) {
                                            DocumentFile.fromTreeUri(applicationContext, extractorCustomOutputTreeUri!!)
                                                ?: rootDoc
                                        } else {
                                            doc.parentFile ?: rootDoc
                                        }
                                        extractRpaFileToDocument(
                                            tempFile,
                                            targetDoc,
                                            useSubfolder,
                                            archiveBaseName,
                                            overwrite,
                                            ::updateProgress,
                                            ::appendLog
                                        )
                                    }

                                    totalExtracted += extracted
                                    totalFailed += failed
                                    appendLog("Done $name: $extracted extracted, $failed failed.")
                                } catch (e: Exception) {
                                    totalFailed++
                                    appendLog("Failed reading $name: ${e.message}")
                                } finally {
                                    tempFile.delete()
                                }
                            }
                        }
                    }

                    InputType.NONE -> {}
                }
            } catch (e: Exception) {
                appendLog("Fatal error during extraction: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    isExtracting = false
                    binding.btnStartExtract.isEnabled = true
                    val summary = getString(R.string.tools_extract_finished_summary, totalExtracted, totalFailed)
                    binding.txtExtractStatus.text = summary
                    appendLog(summary)
                }
            }
        }
    }

    private fun extractRpaFileToDirectory(
        rpaFile: File,
        targetDir: File,
        useSubfolder: Boolean,
        subfolderName: String = rpaFile.nameWithoutExtension,
        overwrite: Boolean,
        onProgress: (current: Int, total: Int, name: String) -> Unit,
        onLog: (String) -> Unit
    ): Pair<Int, Int> {
        val effectiveDir = if (useSubfolder) {
            File(targetDir, subfolderName).apply { mkdirs() }
        } else {
            targetDir.apply { mkdirs() }
        }

        var extracted = 0
        var failed = 0

        try {
            RpaReader(rpaFile).use { reader ->
                val fileList = reader.getFileList()
                val total = fileList.size
                for ((index, entryKey) in fileList.withIndex()) {
                    val normalizedName = entryKey.replace('\\', '/')
                    val destFile = File(effectiveDir, normalizedName)
                    onProgress(index + 1, total, destFile.name)

                    if (destFile.exists() && !overwrite) {
                        extracted++
                        continue
                    }

                    try {
                        destFile.parentFile?.mkdirs()
                        destFile.outputStream().use { fos ->
                            reader.extractFile(entryKey, fos)
                        }
                        extracted++
                    } catch (e: Exception) {
                        failed++
                        onLog("Failed: $normalizedName (${e.message})")
                    }
                }
            }
        } catch (e: Exception) {
            failed++
            onLog("Failed to read ${rpaFile.name}: ${e.message}")
        }

        return Pair(extracted, failed)
    }

    private fun extractRpaFileToDocument(
        rpaFile: File,
        targetDoc: DocumentFile,
        useSubfolder: Boolean,
        subfolderName: String = rpaFile.nameWithoutExtension,
        overwrite: Boolean,
        onProgress: (current: Int, total: Int, name: String) -> Unit,
        onLog: (String) -> Unit
    ): Pair<Int, Int> {
        val docCache = mutableMapOf<String, DocumentFile>()
        val effectiveDoc = if (useSubfolder) {
            getOrCreateDocumentSubdir(targetDoc, subfolderName, docCache) ?: targetDoc
        } else {
            targetDoc
        }

        var extracted = 0
        var failed = 0

        try {
            RpaReader(rpaFile).use { reader ->
                val fileList = reader.getFileList()
                val total = fileList.size
                for ((index, entryKey) in fileList.withIndex()) {
                    val normalizedName = entryKey.replace('\\', '/')
                    val relativeParent = File(normalizedName).parent ?: ""
                    val entryFileName = File(normalizedName).name
                    onProgress(index + 1, total, entryFileName)

                    try {
                        val parentDoc = getOrCreateDocumentSubdir(effectiveDoc, relativeParent, docCache)
                        if (parentDoc != null) {
                            val ok = writeEntryToDocumentFile(reader, entryKey, parentDoc, entryFileName, overwrite)
                            if (ok) extracted++ else {
                                failed++
                                onLog("Failed writing: $normalizedName")
                            }
                        } else {
                            failed++
                            onLog("Failed creating folder for: $normalizedName")
                        }
                    } catch (e: Exception) {
                        failed++
                        onLog("Failed: $normalizedName (${e.message})")
                    }
                }
            }
        } catch (e: Exception) {
            failed++
            onLog("Failed to read ${rpaFile.name}: ${e.message}")
        }

        return Pair(extracted, failed)
    }

    private fun getOrCreateDocumentSubdir(
        rootDir: DocumentFile,
        relativeSubdirPath: String,
        cache: MutableMap<String, DocumentFile>? = null
    ): DocumentFile? {
        val normalized = relativeSubdirPath.replace('\\', '/').trim('/').trim()
        if (normalized.isEmpty() || normalized == ".") {
            return rootDir
        }
        if (cache != null && cache.containsKey(normalized)) {
            return cache[normalized]
        }

        val segments = normalized.split('/').filter { it.isNotBlank() }
        var current = rootDir
        var currentPath = ""
        for (segment in segments) {
            currentPath = if (currentPath.isEmpty()) segment else "$currentPath/$segment"
            if (cache != null && cache.containsKey(currentPath)) {
                current = cache[currentPath]!!
                continue
            }
            val found = current.findFile(segment)
            current = if (found != null && found.isDirectory) {
                found
            } else {
                current.createDirectory(segment) ?: return null
            }
            cache?.put(currentPath, current)
        }
        return current
    }

    private fun writeEntryToDocumentFile(
        reader: RpaReader,
        entryKey: String,
        targetDir: DocumentFile,
        displayName: String,
        overwrite: Boolean
    ): Boolean {
        val existing = targetDir.findFile(displayName)
        if (existing != null) {
            if (!overwrite) {
                return true
            }
            existing.delete()
        }
        val created = targetDir.createFile("application/octet-stream", displayName) ?: return false
        contentResolver.openOutputStream(created.uri)?.use { outStream ->
            reader.extractFile(entryKey, outStream)
        }
        return true
    }

    private fun collectRpaFiles(folder: File, resultList: MutableList<File>) {
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                collectRpaFiles(file, resultList)
            } else if (file.name.endsWith(".rpa", ignoreCase = true)) {
                resultList.add(file)
            }
        }
    }

    private fun collectRpaDocuments(folder: DocumentFile, resultList: MutableList<DocumentFile>) {
        val files = folder.listFiles()
        for (f in files) {
            if (f.isDirectory) {
                collectRpaDocuments(f, resultList)
            } else if (f.name?.endsWith(".rpa", ignoreCase = true) == true) {
                resultList.add(f)
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (index != -1) {
                    name = cursor.getString(index)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }

    private fun setupPythonRunnerView() {
        binding.btnBackToToolsPython.setOnClickListener {
            SoundEffects.playClick(this)
            hidePythonRunnerView()
        }

        binding.btnSelectInputFilePython.setOnClickListener {
            SoundEffects.playClick(this)
            showPythonStorageChoiceDialog()
        }

        binding.btnStartPython.setOnClickListener {
            SoundEffects.playClick(this)
            startPythonExecution()
        }

        binding.btnCopyPythonLog.setOnClickListener {
            SoundEffects.playClick(this)
            copyPythonLogToClipboard()
        }

        binding.btnClearPythonLog.setOnClickListener {
            SoundEffects.playClick(this)
            clearPythonLog()
        }

        binding.btnSendPythonStdin.setOnClickListener {
            SoundEffects.playClick(this)
            submitPythonInput()
        }

        binding.editPythonStdin.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                submitPythonInput()
                true
            } else {
                false
            }
        }
    }

    private fun submitPythonInput() {
        val input = binding.editPythonStdin.text?.toString() ?: ""
        binding.editPythonStdin.setText("")
        appendPythonLog(input + "\n")
        binding.txtPythonStatus.text = getString(R.string.tools_python_running)
        pythonInputQueue.offer(input + "\n")
    }

    private fun appendPythonLog(chunk: String) {
        val buf = pythonLogBuffer ?: return
        buf.append(chunk)
        lifecycleScope.launch(Dispatchers.Main) {
            binding.txtPythonLog.text = buf.toString()
            binding.scrollConsolePython.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showPythonStorageChoiceDialog() {
        val options = arrayOf(
            getString(R.string.tools_app_files),
            getString(R.string.tools_external_storage)
        )

        GameDialogBuilder(this)
            .setTitle(getString(R.string.tools_storage_prompt))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, FileExplorerActivity::class.java).apply {
                            putExtra(FileExplorerActivity.EXTRA_PICKER_MODE, true)
                            putExtra(FileExplorerActivity.EXTRA_PICKER_TYPE, FileExplorerActivity.PICKER_TYPE_FILE)
                            putExtra(FileExplorerActivity.EXTRA_PICKER_EXTENSION, ".py")
                            putExtra("startPath", filesDir.absolutePath)
                        }
                        pythonAppFilePickerLauncher.launch(intent)
                    }

                    1 -> {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        pythonSafFilePickerLauncher.launch(intent)
                    }
                }
            }
            .setNegativeButton(getString(android.R.string.cancel), null)
            .show()
    }

    private fun updatePythonInputUI() {
        val text = when (pythonInputType) {
            PythonInputType.APP_FILE -> pythonAppPath ?: getString(R.string.tools_no_selection)
            PythonInputType.SAF_FILE -> {
                val name = pythonSafUri?.let { getFileNameFromUri(it) } ?: pythonSafUri?.lastPathSegment
                name ?: getString(R.string.tools_no_selection)
            }

            PythonInputType.NONE -> getString(R.string.tools_no_selection)
        }
        binding.txtSelectedInputPython.text = text
    }

    private fun startPythonExecution() {
        if (isRunningPython) return
        if (pythonInputType == PythonInputType.NONE) {
            binding.txtPythonStatus.text = getString(R.string.tools_python_select_script_required)
            return
        }

        isRunningPython = true
        binding.btnStartPython.isEnabled = false
        binding.progressBarPython.visibility = View.VISIBLE
        binding.txtPythonStatus.text = getString(R.string.tools_python_running)
        binding.txtPythonLog.text = ""
        binding.layoutPythonInputBar.visibility = View.GONE
        binding.editPythonStdin.setText("")
        pythonInputQueue.clear()

        val logBuffer = StringBuilder()
        pythonLogBuffer = logBuffer

        val argsStr = binding.editPythonArgs.text?.toString()?.trim() ?: ""

        lifecycleScope.launch(Dispatchers.IO) {
            var exitCode = 0
            var success = false

            try {
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(applicationContext))
                }
                val py = Python.getInstance()
                val runner = py.getModule("script_runner")

                val scriptFile: File = when (pythonInputType) {
                    PythonInputType.APP_FILE -> File(pythonAppPath!!)
                    PythonInputType.SAF_FILE -> {
                        val originalName = getFileNameFromUri(pythonSafUri!!) ?: "script.py"
                        val runnerDir = ensurePythonRunnerHomeDirectory()
                        val cachedFile = File(runnerDir, originalName)
                        appendPythonLog("[Preparing $originalName...]\n")
                        contentResolver.openInputStream(pythonSafUri!!).use { inStream ->
                            if (inStream != null) {
                                FileOutputStream(cachedFile).use { outStream ->
                                    inStream.copyTo(outStream)
                                }
                            } else {
                                throw IllegalStateException("Cannot read file from external storage.")
                            }
                        }
                        cachedFile
                    }

                    PythonInputType.NONE -> throw IllegalStateException("No script selected.")
                }

                appendPythonLog("[Running ${scriptFile.name}${if (argsStr.isNotEmpty()) " with args: $argsStr" else ""}]\n----------------------------------------\n")

                val callback = object : PythonStreamCallback {
                    override fun onOutput(text: String) {
                        appendPythonLog(text)
                    }

                    override fun onInputRequest(): String {
                        lifecycleScope.launch(Dispatchers.Main) {
                            binding.layoutPythonInputBar.visibility = View.VISIBLE
                            binding.txtPythonStatus.text = getString(R.string.tools_python_waiting_input)
                            binding.editPythonStdin.requestFocus()
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showSoftInput(binding.editPythonStdin, InputMethodManager.SHOW_IMPLICIT)
                            binding.scrollConsolePython.fullScroll(View.FOCUS_DOWN)
                        }
                        return pythonInputQueue.take()
                    }
                }

                val result = runner.callAttr("run_script", scriptFile.absolutePath, argsStr, callback)
                success = result.callAttr("get", "success").toBoolean()
                exitCode = result.callAttr("get", "exit_code").toInt()
                val err = result.callAttr("get", "error")?.toString()
                if (!success && !err.isNullOrBlank() && !logBuffer.contains(err)) {
                    appendPythonLog("\n$err\n")
                }
            } catch (e: Exception) {
                success = false
                exitCode = -1
                appendPythonLog("\nExecution error: ${e.message ?: e.javaClass.simpleName}\n")
            } finally {
                pythonInputQueue.offer("\n")
                withContext(Dispatchers.Main) {
                    isRunningPython = false
                    binding.layoutPythonInputBar.visibility = View.GONE
                    binding.btnStartPython.isEnabled = true
                    binding.progressBarPython.visibility = View.GONE
                    if (success) {
                        binding.txtPythonStatus.text = getString(R.string.tools_python_finished_success, exitCode)
                    } else {
                        binding.txtPythonStatus.text = getString(R.string.tools_python_finished_error, exitCode)
                    }
                }
            }
        }
    }

    private fun copyPythonLogToClipboard() {
        val logText = binding.txtPythonLog.text?.toString() ?: ""
        if (logText.isNotEmpty() && logText != getString(R.string.tools_ready)) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Python Log", logText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, R.string.tools_python_log_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearPythonLog() {
        pythonLogBuffer?.setLength(0)
        binding.txtPythonLog.text = getString(R.string.tools_ready)
        binding.txtPythonStatus.text = getString(R.string.tools_ready)
        binding.layoutPythonInputBar.visibility = View.GONE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isDecompiling || isExtracting || isRunningPython) return
        if (binding.layoutDecompiler.visibility == View.VISIBLE) {
            hideDecompilerView()
        } else if (binding.layoutExtractor.visibility == View.VISIBLE) {
            hideExtractorView()
        } else if (binding.layoutPythonRunner.visibility == View.VISIBLE) {
            hidePythonRunnerView()
        } else {
            super.onBackPressed()
        }
    }
}
