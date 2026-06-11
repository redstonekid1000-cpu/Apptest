package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppDatabase
import com.example.data.AuditReport
import com.example.data.AuditReportRepository
import com.example.data.User
import com.example.data.UserRepository
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen {
    Auth,
    Paywall,
    Dashboard
}

enum class DashboardTab {
    Overview,
    History,
    Settings
}

class AuditViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val userRepository = UserRepository(db.userDao())
    private val auditReportRepository = AuditReportRepository(db.auditReportDao())

    // UI States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow(Screen.Auth)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow(DashboardTab.Overview)
    val currentTab: StateFlow<DashboardTab> = _currentTab.asStateFlow()

    private val _reports = MutableStateFlow<List<AuditReport>>(emptyList())
    val reports: StateFlow<List<AuditReport>> = _reports.asStateFlow()

    private val _selectedReport = MutableStateFlow<AuditReport?>(null)
    val selectedReport: StateFlow<AuditReport?> = _selectedReport.asStateFlow()

    // Loading & Error States
    private val _isProcessingAuth = MutableStateFlow(false)
    val isProcessingAuth: StateFlow<Boolean> = _isProcessingAuth.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isProcessingPayment = MutableStateFlow(false)
    val isProcessingPayment: StateFlow<Boolean> = _isProcessingPayment.asStateFlow()

    private val _paymentError = MutableStateFlow<String?>(null)
    val paymentError: StateFlow<String?> = _paymentError.asStateFlow()

    private val _isAuditRunning = MutableStateFlow(false)
    val isAuditRunning: StateFlow<Boolean> = _isAuditRunning.asStateFlow()

    private val _auditError = MutableStateFlow<String?>(null)
    val auditError: StateFlow<String?> = _auditError.asStateFlow()

    init {
        // Collect reports reactively when user changes
        viewModelScope.launch {
            _currentUser.collectLatest { user ->
                if (user != null) {
                    auditReportRepository.getReportsForUser(user.id).collect { list ->
                        _reports.value = list
                    }
                } else {
                    _reports.value = emptyList()
                    _selectedReport.value = null
                }
            }
        }
    }

    // AUTH ACTIONS
    fun handleAuthentication(isSignUp: Boolean, email: String, passwordHash: String, displayName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessingAuth.value = true
            _authError.value = null
            try {
                if (email.isBlank() || passwordHash.isBlank() || (isSignUp && displayName.isBlank())) {
                    _authError.value = "All fields are required."
                    _isProcessingAuth.value = false
                    return@launch
                }

                if (isSignUp) {
                    val existing = userRepository.getUserByEmail(email)
                    if (existing != null) {
                        _authError.value = "An account with this email already exists."
                    } else {
                        val newUser = User(
                            email = email,
                            passwordHash = passwordHash,
                            displayName = displayName,
                            isSubscribed = false
                        )
                        val id = userRepository.registerUser(newUser)
                        val createdUser = newUser.copy(id = id)
                        _currentUser.value = createdUser
                        _currentScreen.value = Screen.Paywall
                    }
                } else {
                    val user = userRepository.getUserByEmail(email)
                    if (user == null || user.passwordHash != passwordHash) {
                        _authError.value = "Invalid email or password."
                    } else {
                        _currentUser.value = user
                        if (user.isSubscribed) {
                            _currentScreen.value = Screen.Dashboard
                        } else {
                            _currentScreen.value = Screen.Paywall
                        }
                    }
                }
            } catch (e: Exception) {
                _authError.value = "Authentication error: ${e.localizedMessage}"
            } finally {
                _isProcessingAuth.value = false
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = Screen.Auth
        _currentTab.value = DashboardTab.Overview
        _selectedReport.value = null
    }

    // BILLING & SUBSCRIPTION ($1/MONTH)
    fun processSubscriptionPayment(cardNumber: String, expiry: String, cvv: String, cardHolder: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessingPayment.value = true
            _paymentError.value = null
            try {
                val cleanCard = cardNumber.replace(" ", "")
                if (cleanCard.length < 15 || cleanCard.length > 16 || !cleanCard.all { it.isDigit() }) {
                    _paymentError.value = "Please enter a valid 15 or 16 digit card number."
                    _isProcessingPayment.value = false
                    return@launch
                }
                if (!expiry.contains("/") || expiry.length != 5) {
                    _paymentError.value = "Please enter format MM/YY."
                    _isProcessingPayment.value = false
                    return@launch
                }
                if (cvv.length != 3 || !cvv.all { it.isDigit() }) {
                    _paymentError.value = "Please enter a 3 digit CVV."
                    _isProcessingPayment.value = false
                    return@launch
                }
                if (cardHolder.isBlank()) {
                    _paymentError.value = "Please enter the cardholder's name."
                    _isProcessingPayment.value = false
                    return@launch
                }

                // Simulate processing delay
                kotlinx.coroutines.delay(1500)

                val user = _currentUser.value
                if (user != null) {
                    val lastFour = cleanCard.takeLast(4)
                    val updatedUser = user.copy(
                        isSubscribed = true,
                        subscriptionCardNumberSuffix = lastFour,
                        subscriptionDate = System.currentTimeMillis()
                    )
                    userRepository.updateUser(updatedUser)
                    _currentUser.value = updatedUser

                    // Preload default editorial audit if they have zero audits
                    val existingAudits = _reports.value
                    if (existingAudits.isEmpty()) {
                        preloadDefaultAudits(user.id)
                    }

                    _currentScreen.value = Screen.Dashboard
                } else {
                    _paymentError.value = "Session expired. Please log in again."
                }
            } catch (e: Exception) {
                _paymentError.value = "Payment failed: ${e.localizedMessage}"
            } finally {
                _isProcessingPayment.value = false
            }
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = _currentUser.value
            if (user != null) {
                val updatedUser = user.copy(
                    isSubscribed = false,
                    subscriptionCardNumberSuffix = null,
                    subscriptionDate = null
                )
                userRepository.updateUser(updatedUser)
                _currentUser.value = updatedUser
                _currentScreen.value = Screen.Paywall
            }
        }
    }

    // AUDIT PIPELINE
    fun runAppAudit(appName: String, screenDesc: String) {
        val user = _currentUser.value
        if (user == null) {
            _auditError.value = "User session not found."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isAuditRunning.value = true
            _auditError.value = null
            try {
                if (appName.isBlank() || screenDesc.isBlank()) {
                    _auditError.value = "App Name and Description must not be empty."
                    _isAuditRunning.value = false
                    return@launch
                }

                val apiKey = BuildConfig.GEMINI_API_KEY
                var auditReport: AuditReport? = null

                if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                    try {
                        val prompt = getAuditPrompt(appName, screenDesc)
                        val requestObj = GenerateContentRequest(
                            contents = listOf(
                                Content(parts = listOf(Part(text = prompt)))
                            ),
                            generationConfig = GenerationConfig(temperature = 0.7f)
                        )
                        val response = RetrofitClient.service.generateContent(apiKey, requestObj)
                        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        if (responseText != null) {
                            auditReport = parseGeminiResponse(responseText, user.id, appName, screenDesc)
                        }
                    } catch (netEx: Exception) {
                        Log.e("AuditViewModel", "Gemini API failed, falling back to offline audit engine", netEx)
                    }
                }

                if (auditReport == null) {
                    // Fallback to high-quality procedural review generator (offline preview engine)
                    auditReport = generateProceduralAudit(user.id, appName, screenDesc)
                }

                auditReportRepository.insertReport(auditReport)
                _selectedReport.value = auditReport
                _currentTab.value = DashboardTab.History
            } catch (e: Exception) {
                _auditError.value = "Failed to run audit: ${e.localizedMessage}"
            } finally {
                _isAuditRunning.value = false
            }
        }
    }

    fun selectNavigationTab(tab: DashboardTab) {
        _currentTab.value = tab
    }

    fun selectReport(report: AuditReport?) {
        _selectedReport.value = report
    }

    fun deleteReport(reportId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            auditReportRepository.deleteReport(reportId)
            if (_selectedReport.value?.id == reportId) {
                _selectedReport.value = null
            }
        }
    }

    // INTERNAL HEURISTIC ENGINE & PRELOADER
    private suspend fun preloadDefaultAudits(userId: Long) {
        val defaultMock = AuditReport(
            userId = userId,
            appName = "Demo Portfolio App",
            screenDescription = "Modern corporate home page with full landing graphics",
            overallScore = 8,
            hierarchyStatus = "Critical",
            hierarchyText = "Your primary CTA is competing with background noise.",
            contrastStatus = "AA standard missed",
            contrastText = "The thin gray lettering on the pristine slate background misses the targets.",
            targetsStatus = "44px threshold",
            targetsText = "Two bottom horizontal action elements utilize insufficient spacing (<40dp).",
            recommendations = """
                ### Architectural Design Critique
                
                The interface excels in elegant modern styling but misses several basic utility targets. Consider these priority enhancements:
                
                *   **Spacing and Breathability:** Visual cohesion isn't just about color; it's about the rhythm of your white space. Elevate vertical margins.
                *   **Aesthetic Contrast Tuning:** Darken secondary text color `#49454f` to `#1d1b20` on light canvases to preserve AA accessibility standards.
                *   **Sizing Integrity:** Expand touch targets on action cards to at least **48dp** to facilitate accurate mobile tactile feedback controls.
            """.trimIndent()
        )
        auditReportRepository.insertReport(defaultMock)
    }

    private fun generateProceduralAudit(userId: Long, appName: String, desc: String): AuditReport {
        // Generate high-quality procedural reviewer text if offline
        val hasButtons = desc.contains("button", ignoreCase = true) || desc.contains("cta", ignoreCase = true) || desc.contains("click", ignoreCase = true)
        val hasImages = desc.contains("image", ignoreCase = true) || desc.contains("photo", ignoreCase = true) || desc.contains("banner", ignoreCase = true)
        val hasList = desc.contains("list", ignoreCase = true) || desc.contains("grid", ignoreCase = true) || desc.contains("scroll", ignoreCase = true)

        val score = if (desc.length > 50) 8 else 6
        val hierStat = if (hasButtons) "Warning" else "Critical"
        val hierText = if (hasButtons) "Your primary action is somewhat layout-bound and competes for focal attention." else "Critical action flow is absent; primary focus remains unstructured."

        val contStat = if (hasImages) "AA standard missed" else "Excellent"
        val contText = if (hasImages) "Overlay texts over hero backgrounds drop below the mandatory 4.5:1 ratio." else "Text contrasts are visually sharp and safe across standard screens."

        val targStat = if (hasList) "44px threshold" else "Excellent"
        val targText = if (hasList) "A series of sequential rows underpresses navigation targets below 48dp." else "Tappable actions satisfy Material-3 tactile size classes."

        val recs = """
            ### Procedural Design Audit Report for: **$appName**
            
            This report was prepared by the offline expert system engine because an external API connection was not established.
            
            #### Critical Guidelines Evaluated
            *   **Visual Dominance**: The primary visual flow shows that visual elements are crowded. Consider employing generous negative space instead of hard dividers.
            *   **Accent Color Contrast**: Elevate colors to match proper AA accessibility contrast thresholds.
            *   **Interaction Padding**: Expand elements to provide Material 3 ripples together with tactile target sizes of at least **48dp x 48dp**.
            
            *Tip: Enter your Google AI Studio Secrets Key to engage the supercharged Gemini 3.5 AI reasoning engine.*
        """.trimIndent()

        return AuditReport(
            userId = userId,
            appName = appName,
            screenDescription = desc,
            overallScore = score,
            hierarchyStatus = hierStat,
            hierarchyText = hierText,
            contrastStatus = contStat,
            contrastText = contText,
            targetsStatus = targStat,
            targetsText = targText,
            recommendations = recs
        )
    }

    private fun getAuditPrompt(appName: String, desc: String): String {
        return """
            You are a highly detailed and strict visual/UX and UI auditor. Analyze the following app interface request and write a professional, elegant UX audit report.
            
            APP NAME: $appName
            SCREEN DESCRIPTION/CRITIQUE DETAILS: $desc
            
            Your analysis must be structured exactly with the following tag delimiters so that it can be parsed programmatically:
            
            [OVERALL_SCORE]
            <An integer from 1 to 10 evaluating the overall usability and visual alignment>
            
            [HIERARCHY_STATUS]
            <"Critical", "Warning", or "Excellent">
            
            [HIERARCHY_TEXT]
            <A single clear, concise sentence with visual design feedback on the hierarchy, e.g. "Your primary CTA is competing with background noise.">
            
            [CONTRAST_STATUS]
            <"AA standard missed", "Warning", or "Excellent">
            
            [CONTRAST_TEXT]
            <A single clear, concise sentence on accessibility and contrast safety.>
            
            [TARGETS_STATUS]
            <"44px threshold", "Warning", or "Excellent">
            
            [TARGETS_TEXT]
            <A single clear, concise sentence on touch targets and interactive component spacing.>
            
            [RECOMMENDATIONS]
            <Detailed, bulleted, editorial style suggestions and visual rhythm feedback in Markdown format.>
            
            Be professional, clear, and match the 'Editorial Acoustic' aesthetic. Do not output anything outside of these tags.
        """.trimIndent()
    }

    private fun parseGeminiResponse(responseText: String, userId: Long, appName: String, screenDescription: String): AuditReport {
        fun extractTag(tag: String): String {
            val startTag = "[$tag]"
            if (!responseText.contains(startTag)) return ""
            val startIdx = responseText.indexOf(startTag) + startTag.length
            val sub = responseText.substring(startIdx)
            val endTagIdx = sub.indexOf('[')
            val content = if (endTagIdx != -1) sub.substring(0, endTagIdx) else sub
            return content.trim()
        }

        val scoreStr = extractTag("OVERALL_SCORE")
        val overallScore = scoreStr.filter { it.isDigit() }.toIntOrNull() ?: 7
        val hierarchyStatus = extractTag("HIERARCHY_STATUS").ifEmpty { "Warning" }
        val hierarchyText = extractTag("HIERARCHY_TEXT").ifEmpty { "Audit in progress." }
        val contrastStatus = extractTag("CONTRAST_STATUS").ifEmpty { "AA standard missed" }
        val contrastText = extractTag("CONTRAST_TEXT").ifEmpty { "Contrast evaluation in progress." }
        val targetsStatus = extractTag("TARGETS_STATUS").ifEmpty { "44px threshold" }
        val targetsText = extractTag("TARGETS_TEXT").ifEmpty { "Design target checklist." }
        val recommendations = extractTag("RECOMMENDATIONS").ifEmpty { "Recommendations for this layout will be generated shortly." }

        return AuditReport(
            userId = userId,
            appName = appName,
            screenDescription = screenDescription,
            overallScore = overallScore,
            hierarchyStatus = hierarchyStatus,
            hierarchyText = hierarchyText,
            contrastStatus = contrastStatus,
            contrastText = contrastText,
            targetsStatus = targetsStatus,
            targetsText = targetsText,
            recommendations = recommendations
        )
    }
}
