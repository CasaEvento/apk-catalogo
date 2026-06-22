package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.BitmapFactory
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CatalogItem
import com.example.data.model.CompanyConfig
import com.example.presentation.viewmodel.CatalogFormViewModel
import com.example.ui.canvas.CatalogCanvasRenderer
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCatalogScreen(viewModel: CatalogFormViewModel) {
    val savedItems by viewModel.savedItems.collectAsState()
    val companyBranding by viewModel.companyBranding.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Synchronize snackbar notifications from model sync logs
    LaunchedEffect(viewModel.syncLog) {
        if (viewModel.syncLog.isNotEmpty() && viewModel.syncLog != "Formulario en blanco listo.") {
            snackbarHostState.showSnackbar(viewModel.syncLog)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Catálogo Móvil AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { activeTab = 1 }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurar Marca"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.AddPhotoAlternate, "Nuevo") },
                    label = { Text("Ficha") },
                    modifier = Modifier.testTag("tab_new_product")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Palette, "Marca") },
                    label = { Text("Identidad") },
                    modifier = Modifier.testTag("tab_branding")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                0 -> CatalogFormView(viewModel, companyBranding)
                1 -> BrandingConfigView(viewModel, companyBranding)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogFormView(viewModel: CatalogFormViewModel, branding: CompanyConfig) {
    val context = LocalContext.current
    var containerWidth by remember { mutableStateOf(1) }
    var containerHeight by remember { mutableStateOf(1) }

    // Launcher endpoints for images
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.setCapturedImageUri(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        // Direct handling or loading of camera caches
    }

    // Temporary camera caching location
    val tempPhotoFile = remember { File(context.cacheDir, "camera_capture.jpg") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // IMAGE CAPTURE AREA
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "1. Captura de Objeto",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (viewModel.processedBitmap == null) {
                        // Empty stage
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    imagePickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Picker",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Cargar fotografía del producto",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        // Interactive image stage
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(androidx.compose.ui.graphics.Color.LightGray)
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .onGloballyPositioned { coordinates ->
                                    containerWidth = coordinates.size.width
                                    containerHeight = coordinates.size.height
                                }
                                .pointerInput(viewModel.isErasingMode) {
                                    if (viewModel.isErasingMode) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            val endX = change.position.x
                                            val endY = change.position.y
                                            val startX = endX - dragAmount.x
                                            val startY = endY - dragAmount.y

                                            // Map display container bounds to Bitmap coordinate spacing
                                            viewModel.processedBitmap?.let { bmp ->
                                                val sX = bmp.width.toFloat() / containerWidth.toFloat()
                                                val sY = bmp.height.toFloat() / containerHeight.toFloat()
                                                viewModel.performBrushErase(
                                                    startX = startX * sX,
                                                    startY = startY * sY,
                                                    endX = endX * sX,
                                                    endY = endY * sY
                                                )
                                            }
                                        }
                                    } else {
                                        detectTapGestures { offset ->
                                            viewModel.processedBitmap?.let { bmp ->
                                                // Convert containers click coordinates to absolute pixel space
                                                val sX = bmp.width.toFloat() / containerWidth.toFloat()
                                                val sY = bmp.height.toFloat() / containerHeight.toFloat()
                                                val px = (offset.x * sX)
                                                    .toInt()
                                                    .coerceIn(0, bmp.width - 1)
                                                val py = (offset.y * sY)
                                                    .toInt()
                                                    .coerceIn(0, bmp.height - 1)
                                                val clickedColor = bmp.getPixel(px, py)

                                                viewModel.applyMagicWand(clickedColor)
                                            }
                                        }
                                    }
                                }
                        ) {
                            Image(
                                bitmap = viewModel.processedBitmap!!.asImageBitmap(),
                                contentDescription = "Editor",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Tag indicating mode
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .align(Alignment.TopEnd)
                                    .background(
                                        if (viewModel.isErasingMode) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (viewModel.isErasingMode) "BORRADOR" else "VARITA ACTIVADA",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.isErasingMode = !viewModel.isErasingMode },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.isErasingMode) MaterialTheme.colorScheme.errorContainer
                                    else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (viewModel.isErasingMode) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (viewModel.isErasingMode) Icons.Default.Gesture else Icons.Default.AutoFixHigh,
                                    contentDescription = "Modo"
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (viewModel.isErasingMode) "Varita" else "Pincel")
                            }

                            Button(
                                onClick = { viewModel.resetImageEdits() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, "Restaurar")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reajustar")
                            }

                            Button(
                                onClick = {
                                    imagePickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1.dp.value)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, "Nueva")
                            }
                        }

                        // Tolerance / Brush settings sliders
                        Spacer(modifier = Modifier.height(10.dp))
                        if (!viewModel.isErasingMode) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Sensibilidad Varita: ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        String.format("%.1f%%", viewModel.magicWandTolerance * 100f),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = viewModel.magicWandTolerance,
                                    onValueChange = { viewModel.onToleranceSliderChange(it) },
                                    valueRange = 0.01f..0.8f
                                )
                            }
                        } else {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Espesor de Borrador: ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${viewModel.brushSize.toInt()} px",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Slider(
                                    value = viewModel.brushSize,
                                    onValueChange = { viewModel.brushSize = it },
                                    valueRange = 10f..150f
                                )
                            }
                        }
                    }
                }
            }
        }

        // TEXT FIELDS AND GEMINI AI BUTTON
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Especificaciones Técnicas",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Button(
                            onClick = { viewModel.enrichWithGemini() },
                            enabled = viewModel.rawCapturedBitmap != null && !viewModel.isAnalyzingWithGemini,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (viewModel.isAnalyzingWithGemini) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.AutoAwesome, "IA", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Llenar con IA", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = viewModel.productName,
                        onValueChange = { viewModel.productName = it },
                        label = { Text("Nombre del Producto") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_product_name"),
                        leadingIcon = { Icon(Icons.Default.ShoppingBag, "Producto") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viewModel.productCode,
                        onValueChange = { viewModel.productCode = it },
                        label = { Text("Código de Producto") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_product_code"),
                        leadingIcon = { Icon(Icons.Default.QrCode, "Código") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.dimHeight,
                            onValueChange = { viewModel.dimHeight = it },
                            label = { Text("Alto (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_height"),
                            leadingIcon = { Icon(Icons.Default.SwapVert, "Alto") }
                        )

                        OutlinedTextField(
                            value = viewModel.dimWidth,
                            onValueChange = { viewModel.dimWidth = it },
                            label = { Text("Ancho (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_width"),
                            leadingIcon = { Icon(Icons.Default.SwapHoriz, "Ancho") }
                        )

                        OutlinedTextField(
                            value = viewModel.dimDepth,
                            onValueChange = { viewModel.dimDepth = it },
                            label = { Text("Prof (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_depth"),
                            leadingIcon = { Icon(Icons.Default.UnfoldMore, "Prof") }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viewModel.material,
                        onValueChange = { viewModel.material = it },
                        label = { Text("Material Fabricación") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Extension, "Material") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viewModel.presentation,
                        onValueChange = { viewModel.presentation = it },
                        label = { Text("Presentación Comercial") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Inventory2, "Presentación") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dropdown for Finished Surface styling selection
                    var isFinishMenuExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = isFinishMenuExpanded,
                        onExpandedChange = { isFinishMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = viewModel.finish,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Acabado del Topper/Papel") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFinishMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            leadingIcon = { Icon(Icons.Default.Brush, "Acabado") }
                        )
                        ExposedDropdownMenu(
                            expanded = isFinishMenuExpanded,
                            onDismissRequest = { isFinishMenuExpanded = false }
                        ) {
                            listOf("Mate", "Satinado", "Metalizado").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        viewModel.finish = option
                                        isFinishMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // CANVAS PREVIEW BLOCK
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "3. Vista Previa Lienzo 1080x1080",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Shared Custom Canvas Drawing inside Jetpack Compose
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f) // Square 1:1 format
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width.toInt()
                            val h = size.height.toInt()

                            val nativeCanvas = drawContext.canvas.nativeCanvas
                            CatalogCanvasRenderer.drawCatalogOnCanvas(
                                canvas = nativeCanvas,
                                width = w,
                                height = h,
                                productBitmap = viewModel.processedBitmap ?: viewModel.rawCapturedBitmap,
                                logoBitmap = null, // simplified inside Compose canvas, gets loaded in final generation
                                companyConfig = branding,
                                productName = viewModel.productName,
                                productCode = viewModel.productCode,
                                dimHeight = viewModel.dimHeight,
                                dimWidth = viewModel.dimWidth,
                                dimDepth = viewModel.dimDepth,
                                material = viewModel.material,
                                finish = viewModel.finish,
                                presentation = viewModel.presentation
                            )
                        }
                    }
                }
            }
        }

         // ACTION FINALIZE BUTTON
         item {
             Button(
                 onClick = { viewModel.saveAndSyncCatalogItem() },
                 enabled = !viewModel.isSyncingWithWorkspace,
                 modifier = Modifier
                     .fillMaxWidth()
                     .wrapContentHeight()
                     .testTag("button_save_sync"),
                 colors = ButtonDefaults.buttonColors(
                     containerColor = MaterialTheme.colorScheme.primary
                 ),
                 contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp)
             ) {
                 if (viewModel.isSyncingWithWorkspace) {
                     CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                     Spacer(modifier = Modifier.width(12.dp))
                     Text("Guardando y Sincronizando en la Nube...", fontSize = 14.sp)
                 } else {
                     Icon(Icons.Default.CloudUpload, "Sync")
                     Spacer(modifier = Modifier.width(12.dp))
                     Text(
                         text = "Guardar Automáticamente en Google Drive (Carpeta Catálogo)\ny Rellenar Fila en Google Sheets",
                         fontWeight = FontWeight.Bold,
                         fontSize = 13.sp,
                         lineHeight = 18.sp,
                         textAlign = TextAlign.Center
                     )
                 }
             }
             Spacer(modifier = Modifier.height(16.dp))
         }
    }
}

@Composable
fun HistoryView(items: List<CatalogItem>, viewModel: CatalogFormViewModel) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Empty",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
                Text(
                    "Catálogo Vacío",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "No has creado ninguna ficha técnica aún. Diseña un producto en el generador y se sincronizará aquí.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fichas Registradas (${items.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                TextButton(
                    onClick = { viewModel.clearDatabaseItems() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, "Clear")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Borrar Todo")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("catalog_item_card_${item.code}"),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Local Render generated Bitmap thumbnail if exists
                            if (!item.imagePath.isNullOrEmpty()) {
                                val file = File(item.imagePath)
                                if (file.exists()) {
                                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Thumb",
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Image, "No Image", tint = MaterialTheme.colorScheme.outline)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Código: ${item.code}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "Tamaño: ${item.height}x${item.width} cm | ${item.finish}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.material,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = { viewModel.deleteItem(item) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }

                                if (!item.driveUrl.isNullOrEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Sincronizado",
                                        tint = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BrandingConfigView(viewModel: CatalogFormViewModel, config: CompanyConfig) {
    var name by remember { mutableStateOf(config.name) }
    var logoUriStr by remember { mutableStateOf(config.logoUri ?: "") }
    var pCol by remember { mutableStateOf(config.primaryColorHex) }
    var sCol by remember { mutableStateOf(config.secondaryColorHex) }
    var ctaCol by remember { mutableStateOf(config.ctaColorHex) }
    var tCol by remember { mutableStateOf(config.textColorHex) }

    // Google Workspace credentials state
    var token by remember { mutableStateOf(viewModel.workspaceAccessToken) }
    var spreadsheetId by remember { mutableStateOf(viewModel.workspaceSpreadsheetId) }
    var folderId by remember { mutableStateOf(viewModel.workspaceFolderId) }
    var simulate by remember { mutableStateOf(viewModel.simulateWorkspaceFlow) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            logoUriStr = uri.toString()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MODULE A: BRAND PROFILE CONFIG
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Identidad Visual Corporativa",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre de la Empresa") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_company_name"),
                        leadingIcon = { Icon(Icons.Default.Business, "Empresa") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Logo Select item
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                logoPicker.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.UploadFile, "Logo")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cargar Logotipo")
                        }

                        if (logoUriStr.isNotEmpty()) {
                            Text(
                                "Logo.png cargado ✔",
                                color = androidx.compose.ui.graphics.Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Text(
                                "Omitido",
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Paleta de Colores Corporativos (Código Hexadecimal)",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // 4 color text entries
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pCol,
                            onValueChange = { pCol = it },
                            label = { Text("Primario") },
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        OutlinedTextField(
                            value = sCol,
                            onValueChange = { sCol = it },
                            label = { Text("Secundario") },
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = ctaCol,
                            onValueChange = { ctaCol = it },
                            label = { Text("Contraste CTA") },
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        OutlinedTextField(
                            value = tCol,
                            onValueChange = { tCol = it },
                            label = { Text("Texto Base") },
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.updateCorporateColors(
                                name = name,
                                logoUri = if (logoUriStr.isEmpty()) null else logoUriStr,
                                primary = pCol,
                                secondary = sCol,
                                cta = ctaCol,
                                text = tCol
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Establecer Marca Visual")
                    }
                }
            }
        }

        // GOOGLE WORKSPACE CREDENTIALS PANEL
        item {
            Card(
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Sincronización Google Workspace CRM",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Configure su cuenta OAuth, documento de Google Sheets y Google Drive para transferencias en segundo plano de sus catálogos.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Modo Simulación (Sandbox)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Simula Drive y Sheets sin token activo.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = simulate,
                            onCheckedChange = { simulate = it },
                            modifier = Modifier.testTag("switch_simulation")
                        )
                    }

                    if (!simulate) {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            label = { Text("Google Access Token (OAuth2)") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            leadingIcon = { Icon(Icons.Default.VpnKey, "Token") }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = spreadsheetId,
                            onValueChange = { spreadsheetId = it },
                            label = { Text("Google Sheets ID") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            leadingIcon = { Icon(Icons.Default.GridOn, "Spreadsheet") }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = folderId,
                            onValueChange = { folderId = it },
                            label = { Text("ID Carpeta Google Drive") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            leadingIcon = { Icon(Icons.Default.Cloud, "Folder") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.saveWorkspaceCredentials(
                                accessToken = token,
                                sheetId = spreadsheetId,
                                folderId = folderId,
                                simulate = simulate
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Configuración Workspace")
                    }
                }
            }
        }
    }
}
