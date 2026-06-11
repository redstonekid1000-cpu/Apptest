package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.InkColor
import com.example.data.PaperType
import com.example.ui.viewmodel.HandwriteViewModel
import com.example.ui.viewmodel.AuditViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwriteScreen(viewModel: HandwriteViewModel, auditViewModel: AuditViewModel) {
    val context = LocalContext.current

    val currentUser by auditViewModel.currentUser.collectAsState()
    val isSubscribed = currentUser?.isSubscribed == true
    val trialUses = currentUser?.trialUses ?: 0
    val trialRemaining = (3 - trialUses).coerceAtLeast(0)

    val textInput by viewModel.textInput.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val imperfectionLevel by viewModel.imperfectionLevel.collectAsState()
    val paperType by viewModel.paperType.collectAsState()
    val inkColor by viewModel.inkColor.collectAsState()
    val applyLighting by viewModel.applyLighting.collectAsState()
    val applyPencilBlend by viewModel.applyPencilBlend.collectAsState()

    val isDownloadingFonts by viewModel.isDownloadingFonts.collectAsState()
    val fontDownloadError by viewModel.fontDownloadError.collectAsState()
    val fontsDownloaded by viewModel.fontsDownloaded.collectAsState()

    val isRendering by viewModel.isRendering.collectAsState()
    val generatedBitmap by viewModel.generatedBitmap.collectAsState()
    val renderError by viewModel.renderError.collectAsState()

    // Interactive zoom/pan states for the high-res canvas card
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset += offsetChange
    }

    // Reset zoom when bitmap changes
    LaunchedEffect(generatedBitmap) {
        scale = 1f
        offset = Offset.Zero
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✍️ Handwrite",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.setTextInput("")
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear text")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { auditViewModel.logout() }) {
                        Text("Sign Out", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("app_top_bar")
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSubscribed) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                    ),
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (!isSubscribed) {
                            auditViewModel.navigateToPaywall()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSubscribed) Color(0xFFBBF7D0) else Color(0xFFFDE68A)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Membership Status",
                                tint = if (isSubscribed) Color(0xFF16A34A) else Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSubscribed) "Premium Member • Unlimited page generations" 
                                       else "Free Pass Mode: $trialRemaining of 3 generations remaining",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSubscribed) Color(0xFF14532D) else Color(0xFF78350F)
                            )
                        }
                        if (!isSubscribed) {
                            Text(
                                text = "Upgrade",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F),
                                modifier = Modifier
                                    .background(Color(0xFFFDE68A), RoundedCornerShape(4.dp))
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Craft authentic handwritten letters from digital text and code instantly.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontStyle = FontStyle.Italic,
                    lineHeight = 18.sp
                )
            }

            // --- Section 1: Font Downloading Progress ---
            if (isDownloadingFonts) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Downloading flowing handwriting scripts...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            if (fontDownloadError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Warning icon",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Fonts download failed. Running offline standard fallbacks.",
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }
            }

            // --- Section 2: Input Field ---
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enter Code or Text",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    viewModel.setTextInput(
                                        "def calculate_factorial(n):\n" +
                                        "    # Recursive handwriting test!\n" +
                                        "    if n <= 1:\n" +
                                        "        return 1\n" +
                                        "    return n * calculate_factorial(n - 1)\n\n" +
                                        "print(f\"Factorial of 5: {calculate_factorial(5)}\")\n" +
                                        "המסמך נכתב באהבה מרובה ✍️"
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Code, contentDescription = "Code icon", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insert Sample Code", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { viewModel.setTextInput(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("text_input_field"),
                            shape = RoundedCornerShape(12.dp),
                            placeholder = { Text("Write your code or document paragraph here...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }

            // --- Section 3: Designer Control Panel ---
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Styling Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Font sizing slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Font Size Spacing", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${fontSize.toInt()} sp", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = fontSize,
                                onValueChange = { viewModel.setFontSize(it) },
                                valueRange = 14f..38f,
                                modifier = Modifier.testTag("font_size_slider")
                            )
                        }

                        // Jitter & Imperfection level slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Human Writing Jitter (Imperfection)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("${(imperfectionLevel * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = imperfectionLevel,
                                onValueChange = { viewModel.setImperfectionLevel(it) },
                                valueRange = 0.0f..1.0f,
                                modifier = Modifier.testTag("imperfection_slider")
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 1. Selector for Paper Type
                        Column {
                            Text("Notebook Paper Background", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PaperStyleChip(
                                    label = "Workbook",
                                    selected = paperType == PaperType.NOTEBOOK,
                                    onClick = { viewModel.setPaperType(PaperType.NOTEBOOK) }
                                )
                                PaperStyleChip(
                                    label = "Graph Grid",
                                    selected = paperType == PaperType.GRID,
                                    onClick = { viewModel.setPaperType(PaperType.GRID) }
                                )
                                PaperStyleChip(
                                    label = "Canary Pad",
                                    selected = paperType == PaperType.LEGAL_PAD,
                                    onClick = { viewModel.setPaperType(PaperType.LEGAL_PAD) }
                                )
                                PaperStyleChip(
                                    label = "Plain",
                                    selected = paperType == PaperType.BLANK_PARCHMENT,
                                    onClick = { viewModel.setPaperType(PaperType.BLANK_PARCHMENT) }
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 2. Ink color bubbles
                        Column {
                            Text("Ballpoint Ink Color", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                InkColorBubble(InkColor.BLUE, inkColor == InkColor.BLUE) { viewModel.setInkColor(InkColor.BLUE) }
                                InkColorBubble(InkColor.BLACK, inkColor == InkColor.BLACK) { viewModel.setInkColor(InkColor.BLACK) }
                                InkColorBubble(InkColor.RED, inkColor == InkColor.RED) { viewModel.setInkColor(InkColor.RED) }
                                InkColorBubble(InkColor.PENCIL, inkColor == InkColor.PENCIL) { viewModel.setInkColor(InkColor.PENCIL) }
                                
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (inkColor) {
                                        InkColor.BLUE -> "Classic Ballpoint Blue"
                                        InkColor.BLACK -> "Deep Black Gel Pen"
                                        InkColor.RED -> "Editing Red Ink"
                                        InkColor.PENCIL -> "Pencil Charcoal Slate"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        // 3. Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Uneven Overhead Light Shader", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("Adds camera look gradients", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = applyLighting,
                                onCheckedChange = { viewModel.setApplyLighting(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Pressure Ink Bleeding", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("Varies alpha & stroke of downstrokes", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = applyPencilBlend,
                                onCheckedChange = { viewModel.setApplyPencilBlend(it) }
                            )
                        }
                    }
                }
            }

            // --- Section 4: Trigger Render CTA ---
            item {
                Button(
                    onClick = {
                        auditViewModel.checkAndIncrementTrial(
                            onAllowed = {
                                viewModel.triggerRender()
                            },
                            onLimitReached = {
                                Toast.makeText(context, "Trial limit reached. Please purchase premium membership to continue rendering pages!", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("render_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isRendering
                ) {
                    if (isRendering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Rendering Organic Vectors...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Gesture, contentDescription = "Write icon")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Render Handwritten Page", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            // --- Section 5: Render Error ---
            if (renderError != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error detail",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = renderError ?: "Rendering failed. Check input structure.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // --- Section 6: Preview Canvas Card & Actions ---
            item {
                Text(
                    text = "Page Preview (Lossless Vector Export)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (generatedBitmap != null) {
                            // Zoomable viewer instructions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.ZoomIn,
                                    contentDescription = "Pinch to zoom",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pinch to zoom & inspect ink details",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // The actual zoomable box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(390.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .transformable(state = transformState),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = generatedBitmap!!.asImageBitmap(),
                                    contentDescription = "Generated handwritten page preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))

                            // Saving and sharing action row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        shareBitmap(context, generatedBitmap)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("share_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share page", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share PNG", fontSize = 13.sp)
                                }

                                Button(
                                    onClick = {
                                        saveBitmapToGallery(context, generatedBitmap) { filePath ->
                                            if (filePath != null) {
                                                Toast.makeText(context, "Saved page to Photos/Handwrite!", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Failed to save file.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("download_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = "Download page", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Page", fontSize = 13.sp)
                                }
                            }
                        } else {
                            // Empty placeholder state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.ImportContacts,
                                        contentDescription = "Empty review paper",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Your rendered page will display here.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun PaperStyleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RowScope.InkColorBubble(
    ink: InkColor,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(ink.colorVal))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Media saving utility supporting scoped and standard storage
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap?, onResult: (String?) -> Unit) {
    if (bitmap == null) {
        onResult(null)
        return
    }
    try {
        val resolver = context.contentResolver
        val filename = "handwriting_page_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Handwrite")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri != null) {
            resolver.openOutputStream(imageUri).use { out ->
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            onResult(imageUri.toString())
        } else {
            onResult(null)
        }
    } catch (e: Exception) {
        onResult(null)
    }
}

// Global cached temporary image sharing utility to pass to standard Chooser
private fun shareBitmap(context: Context, bitmap: Bitmap?) {
    if (bitmap == null) return
    try {
        val cacheFile = File(context.cacheDir, "shared_handwriting.png")
        FileOutputStream(cacheFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Handwritten Page"))
    } catch (e: Exception) {
        Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
