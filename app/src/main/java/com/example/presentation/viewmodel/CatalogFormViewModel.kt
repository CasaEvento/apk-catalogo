package com.example.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CatalogItem
import com.example.data.model.CompanyConfig
import com.example.data.repository.CatalogRepository
import com.example.data.repository.CompanyConfigRepository
import com.example.data.service.GeminiEnrichmentService
import com.example.data.service.GoogleWorkspaceSyncService
import com.example.ui.canvas.CatalogCanvasRenderer
import com.example.ui.image_editor.BitmapEditorUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

class CatalogFormViewModel(
    application: Application,
    private val catalogRepository: CatalogRepository,
    private val configRepository: CompanyConfigRepository
) : AndroidViewModel(application) {

    private val workspaceService = GoogleWorkspaceSyncService(application)
    private val geminiService = GeminiEnrichmentService()

    // Persistent items list from Room
    val savedItems: StateFlow<List<CatalogItem>> = catalogRepository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Corporate Configuration state
    val companyBranding: StateFlow<CompanyConfig> = configRepository.config

    // Workspace Sync Settings
    var workspaceAccessToken by mutableStateOf("")
    var workspaceSpreadsheetId by mutableStateOf("")
    var workspaceFolderId by mutableStateOf("")
    var simulateWorkspaceFlow by mutableStateOf(true) // Default true for sandbox streaming ease of use

    // Form states
    var productName by mutableStateOf("")
    var productCode by mutableStateOf("")
    var dimHeight by mutableStateOf("15") // Defaults
    var dimWidth by mutableStateOf("15")   // Defaults
    var dimDepth by mutableStateOf("5")    // Defaults
    var material by mutableStateOf("")
    var finish by mutableStateOf("Mate")
    var presentation by mutableStateOf("Unidad")

    // Image/editor states
    var rawCapturedBitmap by mutableStateOf<Bitmap?>(null)
    var processedBitmap by mutableStateOf<Bitmap?>(null)
    var isErasingMode by mutableStateOf(false)
    var brushSize by mutableStateOf(30f)
    var magicWandTolerance by mutableStateOf(0.18f)
    var lastTouchedColor by mutableStateOf<Int?>(null)

    // Action execution logs/states
    var isAnalyzingWithGemini by mutableStateOf(false)
    var isSyncingWithWorkspace by mutableStateOf(false)
    var syncLog by mutableStateOf("Formulario en blanco listo.")

    init {
        // Load default Workspace tokens if saved locally previously
        val sharedPrefs = application.getSharedPreferences("workspace_credentials_pref", Application.MODE_PRIVATE)
        workspaceAccessToken = sharedPrefs.getString("access_token", "") ?: ""
        workspaceSpreadsheetId = sharedPrefs.getString("spreadsheet_id", "") ?: ""
        workspaceFolderId = sharedPrefs.getString("folder_id", "") ?: ""
        simulateWorkspaceFlow = sharedPrefs.getBoolean("simulate_workspace_v2", true)
    }

    /**
     * Set the raw camera/gallery asset and duplicate it to processedBitmap to prepare for cutting out
     */
    fun setCapturedImageUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream: InputStream? = getApplication<Application>().contentResolver.openInputStream(uri)
                val original = BitmapFactory.decodeStream(inputStream)
                if (original != null) {
                    // Resize to a maximum of 800px to maintain local high frames processing performance
                    val maxDim = 800
                    val scaled = if (original.width > maxDim || original.height > maxDim) {
                        val aspect = original.width.toFloat() / original.height.toFloat()
                        if (aspect > 1f) {
                            Bitmap.createScaledBitmap(original, maxDim, (maxDim / aspect).toInt(), true)
                        } else {
                            Bitmap.createScaledBitmap(original, (maxDim * aspect).toInt(), maxDim, true)
                        }
                    } else {
                        original
                    }
                    rawCapturedBitmap = scaled
                    processedBitmap = scaled.copy(Bitmap.Config.ARGB_8888, true)
                    lastTouchedColor = null
                    syncLog = "Fotografía cargada. Toque una sección del fondo para removerla."
                }
            } catch (e: Exception) {
                Log.e("CatalogVM", "Error loading image resource", e)
                syncLog = "Error al abrir la imagen: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Executes the magic wand sweep to replace similar pixels with transparency
     */
    fun applyMagicWand(targetColor: Int) {
        val original = rawCapturedBitmap ?: return
        lastTouchedColor = targetColor
        viewModelScope.launch {
            syncLog = "Limpiando fondo..."
            val result = BitmapEditorUtils.removeColorSweep(
                source = original,
                targetColor = targetColor,
                tolerance = magicWandTolerance
            )
            processedBitmap = result
            syncLog = "Fondo removido. Ajuste la tolerancia libremente."
        }
    }

    /**
     * Re-runs the color sweep when the user adjusts the tolerance slider
     */
    fun onToleranceSliderChange(newTolerance: Float) {
        magicWandTolerance = newTolerance
        val touchedColor = lastTouchedColor
        if (touchedColor != null) {
            applyMagicWand(touchedColor)
        }
    }

    /**
     * Manual swipe finger eraser action
     */
    fun performBrushErase(startX: Float, startY: Float, endX: Float, endY: Float) {
        val currentProcessed = processedBitmap ?: return
        viewModelScope.launch {
            val edited = BitmapEditorUtils.eraseWithBrush(
                source = currentProcessed,
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                brushSize = brushSize
            )
            processedBitmap = edited
        }
    }

    /**
     * Resets visual modifications to raw capture
     */
    fun resetImageEdits() {
        processedBitmap = rawCapturedBitmap?.copy(Bitmap.Config.ARGB_8888, true)
        lastTouchedColor = null
        syncLog = "Fondo restaurado al estado original."
    }

    /**
     * Asks Gemini to enrich product specifications using multimodal input and parsing
     */
    fun enrichWithGemini() {
        val bitmap = rawCapturedBitmap ?: run {
            syncLog = "Capture o seleccione una fotografía primero"
            return
        }

        viewModelScope.launch {
            isAnalyzingWithGemini = true
            syncLog = "Gemini AI analizando adorno visualmente..."
            val suggestion = geminiService.analyzeProduct(bitmap)
            if (suggestion != null) {
                productName = suggestion.nombreSugerido
                material = suggestion.materialSugerido
                presentation = suggestion.presentacionSugerida
                syncLog = "¡Formulario enriquecido por Gemini AI con éxito!"
            } else {
                syncLog = "Sugerencia de red: La IA no pudo devolver una propuesta. Ingrese datos manualmente o revise su GEMINI_API_KEY en los secretos."
            }
            isAnalyzingWithGemini = false
        }
    }

    /**
     * Generates technical card drawing and saves it to local device, database, and optional Drive & Sheets.
     */
    fun saveAndSyncCatalogItem() {
        if (productName.isEmpty() || productCode.isEmpty()) {
            syncLog = "Error: Ingrese Nombre y Código de producto para indexar."
            return
        }

        viewModelScope.launch {
            isSyncingWithWorkspace = true
            syncLog = "Compilando lienzo técnico vectorial de 1080x1080..."

            // Load logo company bitmap if specified
            var companyLogo: Bitmap? = null
            val logoUriStr = companyBranding.value.logoUri
            if (!logoUriStr.isNullOrEmpty()) {
                try {
                    val logoUri = Uri.parse(logoUriStr)
                    val stream = getApplication<Application>().contentResolver.openInputStream(logoUri)
                    companyLogo = BitmapFactory.decodeStream(stream)
                } catch (e: Exception) {
                    Log.e("CatalogVM", "Corporate logo load failure", e)
                }
            }

            // Create Canvas Composite bitmap
            val finalCanvasBitmap = CatalogCanvasRenderer.createCatalogBitmap(
                productBitmap = processedBitmap ?: rawCapturedBitmap,
                logoBitmap = companyLogo,
                companyConfig = companyBranding.value,
                productName = productName,
                productCode = productCode,
                dimHeight = dimHeight,
                dimWidth = dimWidth,
                dimDepth = dimDepth,
                material = material,
                finish = finish,
                presentation = presentation
            )

            syncLog = "Iniciando cargamento a la nube de Google Workspace..."
            val syncResult = workspaceService.syncCatalogItem(
                bitmap = finalCanvasBitmap,
                name = productName,
                code = productCode,
                height = dimHeight,
                width = dimWidth,
                depth = dimDepth,
                material = material,
                finish = finish,
                presentation = presentation,
                accessToken = workspaceAccessToken,
                spreadsheetId = workspaceSpreadsheetId,
                folderId = workspaceFolderId,
                simulateWorkspace = simulateWorkspaceFlow
            )

            if (syncResult.success) {
                // Save record in Local Room DB
                val newItem = CatalogItem(
                    name = productName,
                    code = productCode,
                    height = dimHeight,
                    width = dimWidth,
                    depth = dimDepth,
                    material = material,
                    finish = finish,
                    presentation = presentation,
                    imagePath = syncResult.localPath,
                    driveUrl = syncResult.driveUrl,
                    sheetsUrl = if (syncResult.sheetsRowIndex != null) "Inyectado" else null
                )
                catalogRepository.insert(newItem)

                syncLog = syncResult.logMessage
                // Reset Form
                clearForm()
            } else {
                syncLog = "Falla de persistencia: ${syncResult.logMessage}"
            }
            isSyncingWithWorkspace = false
        }
    }

    /**
     * Erases the entire Room Database
     */
    fun clearDatabaseItems() {
        viewModelScope.launch {
            catalogRepository.clear()
            syncLog = "Historial local de fichas eliminado."
        }
    }

    /**
     * Erases specific SQLite item
     */
    fun deleteItem(item: CatalogItem) {
        viewModelScope.launch {
            catalogRepository.delete(item)
            syncLog = "Ficha ${item.code} eliminada del registro local."
        }
    }

    fun saveWorkspaceCredentials(accessToken: String, sheetId: String, folderId: String, simulate: Boolean) {
        workspaceAccessToken = accessToken
        workspaceSpreadsheetId = sheetId
        workspaceFolderId = folderId
        simulateWorkspaceFlow = simulate

        val sharedPrefs = getApplication<Application>().getSharedPreferences("workspace_credentials_pref", Application.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("access_token", accessToken)
            putString("spreadsheet_id", sheetId)
            putString("folder_id", folderId)
            putBoolean("simulate_workspace_v2", simulate)
            apply()
        }
        syncLog = "Configuración de Google Workspace guardada."
    }

    fun updateCorporateColors(name: String, logoUri: String?, primary: String, secondary: String, cta: String, text: String) {
        val updated = CompanyConfig(name, logoUri, primary, secondary, cta, text)
        configRepository.saveConfig(updated)
        syncLog = "Identidad visual de ${name} actualizada."
    }

    private fun clearForm() {
        productName = ""
        productCode = ""
        dimHeight = "15"
        dimWidth = "15"
        dimDepth = "5"
        material = ""
        finish = "Mate"
        presentation = "Unidad"
        rawCapturedBitmap = null
        processedBitmap = null
        lastTouchedColor = null
    }
}

class CatalogFormViewModelFactory(
    private val application: Application,
    private val catalogRepository: CatalogRepository,
    private val configRepository: CompanyConfigRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogFormViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CatalogFormViewModel(application, catalogRepository, configRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
