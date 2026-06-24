package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentEntity
import com.example.ui.theme.*
import com.example.viewmodel.DocumentViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDocScreen(
    viewModel: DocumentViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // ViewModel states
    val allDocs by viewModel.allDocuments.collectAsState()
    val selectedDoc by viewModel.selectedDocument.collectAsState()
    val isParsingPptx by viewModel.isParsingPptx.collectAsState()
    val isAnalyzingGrammar by viewModel.isAnalyzingGrammar.collectAsState()
    val isAnalyzingSpelling by viewModel.isAnalyzingSpelling.collectAsState()
    val isSummarizing by viewModel.isSummarizing.collectAsState()
    val isAnalyzingPlagiarism by viewModel.isAnalyzingPlagiarism.collectAsState()
    val apiErrorMessage by viewModel.apiErrorMessage.collectAsState()
    val userApiKey by viewModel.userApiKey.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    // Screen navigation state
    var showMobileSidebar by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it)
            viewModel.importPptx(it, fileName)
        }
    }

    // PDF creator launcher
    val pdfSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    viewModel.exportToPdf(
                        outputStream = outputStream,
                        onSuccess = {
                            Toast.makeText(context, "PDF exported successfully!", Toast.LENGTH_LONG).show()
                        },
                        onFailure = { error ->
                            Toast.makeText(context, "Export failed: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Display Toast on API Error
    LaunchedEffect(apiErrorMessage) {
        apiErrorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearErrorMessage()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(SleekBackground)) {
        val isWideScreen = maxWidth >= 800.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // SIDEBAR FOR DESKTOP / WIDE SCREENS
            if (isWideScreen) {
                SidebarContent(
                    allDocs = allDocs,
                    selectedDoc = selectedDoc,
                    userApiKey = userApiKey,
                    onSelectDoc = { viewModel.selectDocument(it) },
                    onDeleteDoc = { viewModel.deleteDocument(it) },
                    onNewDocClick = { viewModel.clearActiveDocument() },
                    onSettingsClick = { showSettingsDialog = true },
                    modifier = Modifier.width(300.dp).fillMaxHeight()
                )
                
                // Vertical divider
                Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0xFFE0E0E0)))
            }

            // MAIN INTERFACE WORKSPACE
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Header bar
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "DeckDoc AI",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp,
                                    fontSize = 20.sp
                                ),
                                color = SleekPrimary
                            )
                            Badge(
                                containerColor = SleekSecondaryContainer,
                                contentColor = SleekOnSecondaryContainer
                            ) {
                                Text("v1.0 Pro", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    },
                    navigationIcon = {
                        if (!isWideScreen) {
                            IconButton(
                                onClick = { showMobileSidebar = true },
                                modifier = Modifier.testTag("menu_button")
                            ) {
                                Text("≡", fontSize = 28.sp, color = SleekPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Text("⚙️", fontSize = 20.sp)
                        }
                        
                        // User Avatar
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SleekSecondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("JD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekOnSecondaryContainer)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SleekBackground)
                )

                // Divider
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0x1A000000)))

                // Active Workspace Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isParsingPptx) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = SleekPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Unzipping & extracting slides text...", color = SleekSecondaryText, fontSize = 14.sp)
                        }
                    } else if (selectedDoc == null) {
                        // Empty / Upload Zone
                        UploadZone(
                            onUploadClick = {
                                filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.presentationml.presentation")
                            }
                        )
                    } else {
                        // Document details workspace
                        ActiveDocumentWorkspace(
                            document = selectedDoc!!,
                            selectedLanguage = selectedLanguage,
                            isGrammarLoading = isAnalyzingGrammar,
                            isSpellingLoading = isAnalyzingSpelling,
                            isSummaryLoading = isSummarizing,
                            isPlagiarismLoading = isAnalyzingPlagiarism,
                            onLanguageChange = { viewModel.setLanguage(it) },
                            onGrammarClick = { viewModel.runGrammarCheck() },
                            onSpellingClick = { viewModel.runSpellingCheck() },
                            onSummarizeClick = { viewModel.runSummarize() },
                            onPlagiarismClick = { viewModel.runPlagiarismCheck() },
                            onExportPdfClick = {
                                val cleanFileName = selectedDoc!!.title.replace(".pptx", "").replace(".ppt", "") + "_Report"
                                pdfSaveLauncher.launch(cleanFileName)
                            }
                        )
                    }
                }
            }
        }

        // MOBILE DRAWER/SIDEBAR (Bottom Sheet or Dialog wrapper)
        if (!isWideScreen && showMobileSidebar) {
            ModalBottomSheet(
                onDismissRequest = { showMobileSidebar = false },
                dragHandle = { BottomSheetDefaults.DragHandle() },
                containerColor = SleekBackground
            ) {
                SidebarContent(
                    allDocs = allDocs,
                    selectedDoc = selectedDoc,
                    userApiKey = userApiKey,
                    onSelectDoc = {
                        viewModel.selectDocument(it)
                        showMobileSidebar = false
                    },
                    onDeleteDoc = { viewModel.deleteDocument(it) },
                    onNewDocClick = {
                        viewModel.clearActiveDocument()
                        showMobileSidebar = false
                    },
                    onSettingsClick = {
                        showSettingsDialog = true
                        showMobileSidebar = false
                    },
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(bottom = 16.dp)
                )
            }
        }

        // SETTINGS DIALOG (For Custom API Key configuration)
        if (showSettingsDialog) {
            var tempKey by remember { mutableStateOf(userApiKey) }
            
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = SleekPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "To analyze PowerPoint decks, the app calls the Google Gemini 3.5 API.",
                            fontSize = 13.sp,
                            color = SleekSecondaryText
                        )
                        Text(
                            text = "By default, the built-in system API key is used. If you have your own personal API key, you can enter it below as an override.",
                            fontSize = 13.sp,
                            color = SleekSecondaryText
                        )
                        
                        OutlinedTextField(
                            value = tempKey,
                            onValueChange = { tempKey = it },
                            label = { Text("Gemini API Key Override") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                        )
                        
                        if (tempKey.isNotBlank()) {
                            Text(
                                "✔ Custom API key will take precedence",
                                color = Color(0xFF2E7D32),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                "ℹ Using platform default API Key",
                                color = SleekSecondaryText,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.saveApiKey(tempKey)
                            showSettingsDialog = false
                            Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Cancel", color = SleekSecondaryText)
                    }
                }
            )
        }
    }
}

@Composable
fun SidebarContent(
    allDocs: List<DocumentEntity>,
    selectedDoc: DocumentEntity?,
    userApiKey: String,
    onSelectDoc: (DocumentEntity) -> Unit,
    onDeleteDoc: (DocumentEntity) -> Unit,
    onNewDocClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SleekBackground)
            .padding(16.dp)
    ) {
        // Headline
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text(
                "Document Manager",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = SleekText
            )
            IconButton(onClick = onSettingsClick) {
                Text("⚙️", fontSize = 18.sp)
            }
        }

        // New Presentation Upload button
        Button(
            onClick = onNewDocClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("new_doc_button"),
            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("➕", fontSize = 14.sp)
                Text("Upload Presentation", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "HISTORIC REPORTS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = SleekSecondaryText,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (allDocs.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No parsed presentations found. Upload one to begin!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        color = SleekSecondaryText
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allDocs, key = { it.id }) { doc ->
                    val isSelected = selectedDoc?.id == doc.id
                    val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(doc.timestamp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) SleekSecondaryContainer else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) SleekPrimary else Color(0x1F000000),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectDoc(doc) }
                            .padding(12.dp)
                            .testTag("doc_item_${doc.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📄",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) SleekOnSecondaryContainer else SleekText,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${doc.slideCount} Slides • $formattedDate",
                                fontSize = 11.sp,
                                color = SleekSecondaryText,
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = { onDeleteDoc(doc) },
                            modifier = Modifier.size(28.dp).testTag("delete_doc_${doc.id}")
                        ) {
                            Text("🗑", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API Key Status Indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32))
                )
                Text(
                    text = if (userApiKey.isNotBlank()) "Using Custom API Key" else "Using System API Key",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SleekSecondaryText
                )
            }
        }
    }
}

@Composable
fun UploadZone(onUploadClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .drawBehind {
                val stroke = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
                drawRoundRect(
                    color = Color(0xFF6750A4),
                    style = stroke,
                    cornerRadius = CornerRadius(24.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(24.dp))
            .background(SleekSecondaryContainer.copy(alpha = 0.3f))
            .clickable { onUploadClick() }
            .testTag("upload_zone"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text("📂", fontSize = 48.sp)
            Text(
                "Upload PPT Presentation",
                fontWeight = FontWeight.Bold,
                color = SleekOnSecondaryContainer,
                fontSize = 16.sp
            )
            Text(
                "Tap to browse files (.pptx)",
                fontSize = 12.sp,
                color = SleekSecondaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveDocumentWorkspace(
    document: DocumentEntity,
    selectedLanguage: String,
    isGrammarLoading: Boolean,
    isSpellingLoading: Boolean,
    isSummaryLoading: Boolean,
    isPlagiarismLoading: Boolean,
    onLanguageChange: (String) -> Unit,
    onGrammarClick: () -> Unit,
    onSpellingClick: () -> Unit,
    onSummarizeClick: () -> Unit,
    onPlagiarismClick: () -> Unit,
    onExportPdfClick: () -> Unit
) {
    val languages = listOf("English", "Spanish", "French", "German", "Chinese", "Hindi", "Japanese", "Arabic")
    var langMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Document Title Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = SleekText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val formattedDate = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(document.timestamp))
                Text(
                    text = "Imported $formattedDate • ${document.slideCount} slides found",
                    color = SleekSecondaryText,
                    fontSize = 12.sp
                )
            }
            
            // Slide Count badge
            Badge(
                containerColor = SleekSecondaryContainer,
                contentColor = SleekOnSecondaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("${document.slideCount} slides", modifier = Modifier.padding(6.dp), fontWeight = FontWeight.Bold)
            }
        }

        // ORIGINAL EXTRACTED TEXT WINDOW
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp, max = 220.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, SleekOutline),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Extracted Content Preview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SleekSecondaryText
                    )
                    Text(
                        "Original Text",
                        fontSize = 11.sp,
                        color = SleekPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(SleekSecondaryContainer, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = document.originalText,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = SleekSecondaryText,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }

        // LANGUAGE SELECTION AND CONTROLS GRID
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(1.dp, SleekOutline, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "AI Action Suite",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SleekText
                )
                
                // Language Dropdown Selector
                Box {
                    Button(
                        onClick = { langMenuExpanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SleekSecondaryContainer),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp).testTag("lang_picker_button")
                    ) {
                        Text(
                            text = "🌐 $selectedLanguage",
                            fontSize = 12.sp,
                            color = SleekOnSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    DropdownMenu(
                        expanded = langMenuExpanded,
                        onDismissRequest = { langMenuExpanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    onLanguageChange(lang)
                                    langMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Grid of buttons
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Grammar button
                ActionButton(
                    text = "✍️ Grammar",
                    isLoading = isGrammarLoading,
                    onClick = onGrammarClick,
                    modifier = Modifier.weight(1f).testTag("grammar_button")
                )
                
                // Spelling button
                ActionButton(
                    text = "🔍 Spelling",
                    isLoading = isSpellingLoading,
                    onClick = onSpellingClick,
                    modifier = Modifier.weight(1f).testTag("spelling_button")
                )

                // Summarize button
                ActionButton(
                    text = "📝 Summarize",
                    isLoading = isSummaryLoading,
                    onClick = onSummarizeClick,
                    modifier = Modifier.weight(1f).testTag("summarize_button")
                )

                // Plagiarism button
                ActionButton(
                    text = "🛡️ Integrity Check",
                    isLoading = isPlagiarismLoading,
                    onClick = onPlagiarismClick,
                    modifier = Modifier.weight(1f).testTag("plagiarism_button")
                )
            }
        }

        // AI PLAGIARISM / INTEGRITY REPORT CARD
        if (document.aiPlagiarismScore != null) {
            AIIntegrityReportCard(
                score = document.aiPlagiarismScore,
                analysis = document.aiAnalysis ?: ""
            )
        }

        // RESULT VIEW PANELS
        AnimatedVisibility(
            visible = !document.summary.isNullOrBlank() || !document.grammarAnalysis.isNullOrBlank() || !document.spellingAnalysis.isNullOrBlank(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Summary Panel
                if (!document.summary.isNullOrBlank()) {
                    ResultCard(
                        title = "📝 Executive Summary",
                        content = document.summary,
                        tag = "summary_card"
                    )
                }

                // Grammar Panel
                if (!document.grammarAnalysis.isNullOrBlank()) {
                    ResultCard(
                        title = "✍️ Grammar Suggestions",
                        content = document.grammarAnalysis,
                        tag = "grammar_card"
                    )
                }

                // Spelling Panel
                if (!document.spellingAnalysis.isNullOrBlank()) {
                    ResultCard(
                        title = "🔍 Spelling Audit",
                        content = document.spellingAnalysis,
                        tag = "spelling_card"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // PDF EXPORT BUTTON
        Button(
            onClick = onExportPdfClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("export_pdf_button"),
            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📥", fontSize = 18.sp)
                Text("EXPORT CLEAN PDF REPORT", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ActionButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = SleekSurfaceVariant,
            contentColor = SleekPrimary,
            disabledContainerColor = SleekSurfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SleekPrimary),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        modifier = modifier.height(48.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = SleekPrimary,
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AIIntegrityReportCard(score: Int, analysis: String) {
    val isHigh = score >= 50
    val cardAccentColor = if (isHigh) SleekError else Color(0xFF2E7D32)
    val probabilityText = if (isHigh) "High AI Probability" else "Low AI / Highly Original"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SleekSurfaceVariant, RoundedCornerShape(20.dp))
            .border(1.dp, SleekOutline, RoundedCornerShape(20.dp))
            .padding(16.dp)
            .testTag("plagiarism_report_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🛡️ AI Integrity Report",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = SleekText
            )
            Text(
                probabilityText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = cardAccentColor
            )
        }

        // Percentage gauge bar
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Scan score similarity", fontSize = 11.sp, color = SleekSecondaryText)
                Text("$score%", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = cardAccentColor)
            }
            
            // Linear Progress Indicator
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = cardAccentColor,
                trackColor = Color(0xFFE6E1E5)
            )
        }

        if (analysis.isNotBlank()) {
            Text(
                text = analysis,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                lineHeight = 18.sp,
                color = SleekSecondaryText
            )
        }
    }
}

@Composable
fun ResultCard(
    title: String,
    content: String,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, SleekOutline),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = SleekPrimary
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = content,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = SleekSecondaryText,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

// Utility function to parse the actual PowerPoint file name
private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "presentation.pptx"
}
