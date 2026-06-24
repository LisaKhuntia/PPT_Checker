package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.DocumentEntity
import com.example.network.GeminiRepository
import com.example.util.PdfExporter
import com.example.util.PptxParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.OutputStream

class DocumentViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "DocumentViewModel"
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.documentDao()

    // UI States
    private val _allDocuments = MutableStateFlow<List<DocumentEntity>>(emptyList())
    val allDocuments: StateFlow<List<DocumentEntity>> = _allDocuments.asStateFlow()

    private val _selectedDocument = MutableStateFlow<DocumentEntity?>(null)
    val selectedDocument: StateFlow<DocumentEntity?> = _selectedDocument.asStateFlow()

    private val _isParsingPptx = MutableStateFlow(false)
    val isParsingPptx: StateFlow<Boolean> = _isParsingPptx.asStateFlow()

    private val _isAnalyzingGrammar = MutableStateFlow(false)
    val isAnalyzingGrammar: StateFlow<Boolean> = _isAnalyzingGrammar.asStateFlow()

    private val _isAnalyzingSpelling = MutableStateFlow(false)
    val isAnalyzingSpelling: StateFlow<Boolean> = _isAnalyzingSpelling.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing: StateFlow<Boolean> = _isSummarizing.asStateFlow()

    private val _isAnalyzingPlagiarism = MutableStateFlow(false)
    val isAnalyzingPlagiarism: StateFlow<Boolean> = _isAnalyzingPlagiarism.asStateFlow()

    private val _apiErrorMessage = MutableStateFlow<String?>(null)
    val apiErrorMessage: StateFlow<String?> = _apiErrorMessage.asStateFlow()

    // Settings
    private val _userApiKey = MutableStateFlow("")
    val userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    init {
        // Load custom api key from shared preferences if available
        val prefs = application.getSharedPreferences("deckdoc_prefs", Context.MODE_PRIVATE)
        _userApiKey.value = prefs.getString("gemini_api_key", "") ?: ""
        
        // Collect documents from database
        viewModelScope.launch {
            dao.getAllDocuments().collectLatest { docs ->
                _allDocuments.value = docs
                // Auto-select first document if nothing is selected yet
                if (_selectedDocument.value == null && docs.isNotEmpty()) {
                    _selectedDocument.value = docs.first()
                }
            }
        }
    }

    fun selectDocument(doc: DocumentEntity) {
        _selectedDocument.value = doc
    }

    fun setLanguage(lang: String) {
        _selectedLanguage.value = lang
    }

    fun saveApiKey(key: String) {
        _userApiKey.value = key
        val prefs = getApplication<Application>().getSharedPreferences("deckdoc_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    /**
     * Imports a PowerPoint file from its URI, parses slides, extracts text, and saves to database.
     */
    fun importPptx(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isParsingPptx.value = true
            _apiErrorMessage.value = null
            try {
                val context = getApplication<Application>()
                val slides = PptxParser.extractTextFromPptx(context, uri)
                
                if (slides.isEmpty()) {
                    _apiErrorMessage.value = "Could not extract any text. Please verify if this is a valid PowerPoint file with text contents."
                    _isParsingPptx.value = false
                    return@launch
                }
                
                // Combine extracted text into a slides block representation
                val originalTextBuilder = StringBuilder()
                for (slide in slides) {
                    originalTextBuilder.append("--- Slide ${slide.slideNumber} ---\n")
                    originalTextBuilder.append(slide.text).append("\n\n")
                }
                
                val doc = DocumentEntity(
                    title = fileName,
                    originalText = originalTextBuilder.toString().trim(),
                    slideCount = slides.size,
                    language = _selectedLanguage.value
                )
                
                val newId = dao.insertDocument(doc)
                val insertedDoc = doc.copy(id = newId)
                _selectedDocument.value = insertedDoc
                
            } catch (e: Exception) {
                Log.e(TAG, "Error importing PPTX", e)
                _apiErrorMessage.value = "Failed to parse PowerPoint: ${e.localizedMessage}"
            } finally {
                _isParsingPptx.value = false
            }
        }
    }

    fun runGrammarCheck() {
        val doc = _selectedDocument.value ?: return
        viewModelScope.launch {
            _isAnalyzingGrammar.value = true
            _apiErrorMessage.value = null
            try {
                val result = GeminiRepository.checkGrammar(
                    text = doc.originalText,
                    language = _selectedLanguage.value,
                    userApiKey = _userApiKey.value.ifBlank { null }
                )
                val updatedDoc = doc.copy(grammarAnalysis = result)
                dao.updateDocument(updatedDoc)
                _selectedDocument.value = updatedDoc
            } catch (e: Exception) {
                _apiErrorMessage.value = "Grammar check failed: ${e.localizedMessage}"
            } finally {
                _isAnalyzingGrammar.value = false
            }
        }
    }

    fun runSpellingCheck() {
        val doc = _selectedDocument.value ?: return
        viewModelScope.launch {
            _isAnalyzingSpelling.value = true
            _apiErrorMessage.value = null
            try {
                val result = GeminiRepository.checkSpelling(
                    text = doc.originalText,
                    language = _selectedLanguage.value,
                    userApiKey = _userApiKey.value.ifBlank { null }
                )
                val updatedDoc = doc.copy(spellingAnalysis = result)
                dao.updateDocument(updatedDoc)
                _selectedDocument.value = updatedDoc
            } catch (e: Exception) {
                _apiErrorMessage.value = "Spelling check failed: ${e.localizedMessage}"
            } finally {
                _isAnalyzingSpelling.value = false
            }
        }
    }

    fun runSummarize() {
        val doc = _selectedDocument.value ?: return
        viewModelScope.launch {
            _isSummarizing.value = true
            _apiErrorMessage.value = null
            try {
                val result = GeminiRepository.summarize(
                    text = doc.originalText,
                    language = _selectedLanguage.value,
                    userApiKey = _userApiKey.value.ifBlank { null }
                )
                val updatedDoc = doc.copy(summary = result)
                dao.updateDocument(updatedDoc)
                _selectedDocument.value = updatedDoc
            } catch (e: Exception) {
                _apiErrorMessage.value = "Summarization failed: ${e.localizedMessage}"
            } finally {
                _isSummarizing.value = false
            }
        }
    }

    fun runPlagiarismCheck() {
        val doc = _selectedDocument.value ?: return
        viewModelScope.launch {
            _isAnalyzingPlagiarism.value = true
            _apiErrorMessage.value = null
            try {
                val result = GeminiRepository.checkPlagiarism(
                    text = doc.originalText,
                    userApiKey = _userApiKey.value.ifBlank { null }
                )
                val updatedDoc = doc.copy(
                    aiPlagiarismScore = result.percentage,
                    aiAnalysis = result.analysis
                )
                dao.updateDocument(updatedDoc)
                _selectedDocument.value = updatedDoc
            } catch (e: Exception) {
                _apiErrorMessage.value = "Integrity check failed: ${e.localizedMessage}"
            } finally {
                _isAnalyzingPlagiarism.value = false
            }
        }
    }

    fun deleteDocument(doc: DocumentEntity) {
        viewModelScope.launch {
            dao.deleteDocument(doc)
            if (_selectedDocument.value?.id == doc.id) {
                _selectedDocument.value = _allDocuments.value.firstOrNull { it.id != doc.id }
            }
        }
    }

    fun clearActiveDocument() {
        _selectedDocument.value = null
    }

    fun exportToPdf(outputStream: OutputStream, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val doc = _selectedDocument.value
        if (doc == null) {
            onFailure("No document selected")
            return
        }
        viewModelScope.launch {
            try {
                val success = PdfExporter.exportDocumentToPdf(getApplication(), doc, outputStream)
                if (success) {
                    onSuccess()
                } else {
                    onFailure("PDF creation failed inside standard painter")
                }
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Unknown error exporting to PDF")
            }
        }
    }

    fun clearErrorMessage() {
        _apiErrorMessage.value = null
    }
}
