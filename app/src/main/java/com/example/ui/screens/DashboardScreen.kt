package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.AuditReport
import com.example.ui.theme.EditorialBackground
import com.example.ui.theme.EditorialBorder
import com.example.ui.theme.EditorialMutedText
import com.example.ui.theme.EditorialNavBg
import com.example.ui.theme.EditorialPrimaryDark
import com.example.ui.theme.EditorialPurpleCard
import com.example.ui.theme.EditorialPurpleLight
import com.example.ui.theme.EditorialTextDark
import com.example.ui.viewmodel.AuditViewModel
import com.example.ui.viewmodel.DashboardTab

@Composable
fun DashboardScreen(viewModel: AuditViewModel, modifier: Modifier = Modifier) {
    val currentTab by viewModel.currentTab.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(EditorialBackground),
        bottomBar = {
            BottomNavigationBar(
                selectedTab = currentTab,
                onTabSelected = { viewModel.selectNavigationTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EditorialBackground)
                .padding(innerPadding)
        ) {
            when (currentTab) {
                DashboardTab.Overview -> OverviewTabContent(viewModel = viewModel)
                DashboardTab.History -> HistoryTabContent(viewModel = viewModel)
                DashboardTab.Settings -> SettingsTabContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun OverviewTabContent(viewModel: AuditViewModel) {
    val reports by viewModel.reports.collectAsState()
    val isAuditRunning by viewModel.isAuditRunning.collectAsState()
    val auditError by viewModel.auditError.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var appName by remember { mutableStateOf("") }
    var screenDescription by remember { mutableStateOf("") }

    val recentReport = reports.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        // Header
        HeaderComponent(userInitials = getInitials(currentUser?.displayName ?: "JD"))

        // Title
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "The Audit.",
                fontFamily = FontFamily.Serif,
                fontSize = 48.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                color = EditorialPrimaryDark,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                text = "Visual cohesion isn't just about color; it's about the rhythm of your white space.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = EditorialMutedText,
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audits Dashboard Highlights
        if (recentReport != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LATEST RESULT: ${recentReport.appName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = EditorialMutedText
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(EditorialPrimaryDark)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Score: ${recentReport.overallScore}/10",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 1. Hierarchy Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(EditorialPurpleCard)
                        .padding(24.dp)
                        .height(148.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(EditorialPrimaryDark)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = recentReport.hierarchyStatus.uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        // Custom trend up arrow
                        Canvas(modifier = Modifier.size(24.dp)) {
                            drawLine(
                                color = EditorialPrimaryDark,
                                start = Offset(4.dp.toPx(), 20.dp.toPx()),
                                end = Offset(20.dp.toPx(), 4.dp.toPx()),
                                strokeWidth = 3.dp.toPx()
                            )
                            drawLine(
                                color = EditorialPrimaryDark,
                                start = Offset(10.dp.toPx(), 4.dp.toPx()),
                                end = Offset(20.dp.toPx(), 4.dp.toPx()),
                                strokeWidth = 3.dp.toPx()
                            )
                            drawLine(
                                color = EditorialPrimaryDark,
                                start = Offset(20.dp.toPx(), 10.dp.toPx()),
                                end = Offset(20.dp.toPx(), 4.dp.toPx()),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Hierarchy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = EditorialPrimaryDark,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = recentReport.hierarchyText,
                            fontSize = 13.sp,
                            color = EditorialMutedText
                        )
                    }
                }

                // 2 & 3. Grid Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Contrast Card (White, border)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White)
                            .border(1.dp, EditorialBorder, RoundedCornerShape(28.dp))
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Canvas(modifier = Modifier.size(28.dp)) {
                            drawCircle(color = EditorialPrimaryDark, radius = 8.dp.toPx(), center = Offset(10.dp.toPx(), 10.dp.toPx()))
                            drawCircle(color = EditorialPurpleLight, radius = 6.dp.toPx(), center = Offset(20.dp.toPx(), 16.dp.toPx()))
                        }

                        Column {
                            Text(
                                text = "Contrast",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = EditorialPrimaryDark,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = recentReport.contrastStatus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EditorialPrimaryDark.copy(alpha = 0.8f)
                            )
                            Text(
                                text = recentReport.contrastText,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                color = EditorialMutedText
                            )
                        }
                    }

                    // Targets Card (Solid light purple d0bcff)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(EditorialPurpleLight)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Canvas(modifier = Modifier.size(28.dp)) {
                            drawCircle(
                                color = EditorialPrimaryDark,
                                radius = 6.dp.toPx(),
                                center = Offset(14.dp.toPx(), 14.dp.toPx())
                            )
                            drawCircle(
                                color = EditorialPrimaryDark,
                                radius = 12.dp.toPx(),
                                center = Offset(14.dp.toPx(), 14.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }

                        Column {
                            Text(
                                text = "Targets",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = EditorialPrimaryDark,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = recentReport.targetsStatus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EditorialPrimaryDark.copy(alpha = 0.8f)
                            )
                            Text(
                                text = recentReport.targetsText,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                color = EditorialPrimaryDark.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, EditorialBorder, RoundedCornerShape(28.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to your Workspace",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = EditorialPrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You do not have any visual design audits yet. Describe a layout below to generate your first audit.",
                    fontSize = 13.sp,
                    color = EditorialMutedText,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // INPUT PROMPT ZONE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(1.dp, EditorialBorder, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "Request AI Audit",
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                color = EditorialPrimaryDark,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Describe an interface layout, color codes, custom margins, or interactive components for a detailed audit.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = EditorialMutedText,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = appName,
                onValueChange = { appName = it },
                label = { Text("App Name") },
                placeholder = { Text("e.g. My Portfolio Homepage") },
                modifier = Modifier.fillMaxWidth().testTag("input_audit_app_name"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EditorialPrimaryDark,
                    focusedLabelColor = EditorialPrimaryDark
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = screenDescription,
                onValueChange = { screenDescription = it },
                label = { Text("Layout Details") },
                placeholder = { Text("e.g. Lavender button over yellow backgrounds. Touch sizes are tightly bundled.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .testTag("input_audit_desc"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EditorialPrimaryDark,
                    focusedLabelColor = EditorialPrimaryDark
                )
            )

            if (auditError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = auditError ?: "",
                    color = Color(0xFFB3261E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            val hasKeys = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasKeys) "✓ Gemini 3.5 Active" else "Offline Engine enabled",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hasKeys) Color(0xFF388E3C) else EditorialMutedText
                )

                Button(
                    onClick = {
                        viewModel.runAppAudit(appName, screenDescription)
                        appName = ""
                        screenDescription = ""
                    },
                    enabled = !isAuditRunning,
                    modifier = Modifier.testTag("run_audit_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EditorialPrimaryDark,
                        contentColor = Color.White
                    )
                ) {
                    if (isAuditRunning) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text(text = "Run AI Audit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun HistoryTabContent(viewModel: AuditViewModel) {
    val reports by viewModel.reports.collectAsState()
    val selectedReport by viewModel.selectedReport.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        HeaderComponent(userInitials = "YE", titleText = "Report Archives")

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedReport != null) {
            val report = selectedReport!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "← Back to History",
                        color = EditorialPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { viewModel.selectReport(null) }
                            .padding(vertical = 8.dp)
                    )

                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Report",
                        tint = Color(0xFFB3261E),
                        modifier = Modifier
                            .clickable { viewModel.deleteReport(report.id) }
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = EditorialPurpleCard)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = report.appName.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EditorialPrimaryDark,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EditorialPrimaryDark)
                                    .size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${report.overallScore}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Text(
                            text = "Design Review Summary",
                            fontFamily = FontFamily.Serif,
                            fontSize = 28.sp,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Medium,
                            color = EditorialPrimaryDark,
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )

                        Text(
                            text = "Context given: ${report.screenDescription}",
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = EditorialMutedText,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Text(
                            text = "Hierarchy Feedback: [${report.hierarchyStatus}] ${report.hierarchyText}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EditorialPrimaryDark,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Text(
                            text = "Contrast Feedback: [${report.contrastStatus}] ${report.contrastText}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EditorialPrimaryDark,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Text(
                            text = "Targets Feedback: [${report.targetsStatus}] ${report.targetsText}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EditorialPrimaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "AI Specific Actions & Guidelines",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = EditorialPrimaryDark,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val lines = report.recommendations.split("\n")
                        lines.forEach { line ->
                            if (line.isNotBlank()) {
                                val cleanLine = line.trim().removePrefix("*").removePrefix("-").trim()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(text = "•", color = EditorialPrimaryDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                                    Text(text = cleanLine, fontSize = 13.sp, color = EditorialTextDark)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        } else {
            if (reports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.List, contentDescription = "", modifier = Modifier.size(48.dp), tint = EditorialMutedText)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No saved reviews.", fontWeight = FontWeight.Bold, color = EditorialPrimaryDark)
                        Text(text = "Reports created will appear here.", fontSize = 13.sp, color = EditorialMutedText)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reports) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectReport(item) }
                                .testTag("report_item_${item.id}"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.appName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = EditorialPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (item.screenDescription.length > 60) "${item.screenDescription.take(57)}..." else item.screenDescription,
                                        fontSize = 12.sp,
                                        color = EditorialMutedText
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EditorialPurpleCard)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Score: ${item.overallScore}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EditorialPrimaryDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "→", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EditorialPrimaryDark)
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
fun SettingsTabContent(viewModel: AuditViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        HeaderComponent(userInitials = "YE", titleText = "Workspace Settings")

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Subscription Model",
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    color = EditorialPrimaryDark,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "You are currently subscribed to the Premium Audit Club.",
                    fontSize = 13.sp,
                    color = EditorialTextDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EditorialPurpleCard)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Monthly Subscription Plan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EditorialPrimaryDark)
                            Text(text = "Active", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Price: $1.00 USD / Month", fontSize = 12.sp, color = EditorialMutedText)
                        if (currentUser?.subscriptionCardNumberSuffix != null) {
                            Text(text = "Card: Visa ending in ${currentUser?.subscriptionCardNumberSuffix}", fontSize = 12.sp, color = EditorialMutedText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.cancelSubscription() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cancel_sub_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF3EDF7),
                        contentColor = Color(0xFFB3261E)
                    )
                ) {
                    Text(text = "Cancel Subscription", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Account Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Account Credentials",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = EditorialPrimaryDark,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(text = "Display Name: ${currentUser?.displayName ?: "Guest"}", fontSize = 13.sp, color = EditorialTextDark)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Email Address: ${currentUser?.email ?: ""}", fontSize = 13.sp, color = EditorialTextDark)

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("logout_button"),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EditorialPrimaryDark,
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Sign Out Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun HeaderComponent(userInitials: String, titleText: String = "App Review") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { /* No-op standard header */ }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = EditorialPrimaryDark)
        }

        Text(
            text = titleText.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = EditorialMutedText,
            letterSpacing = 1.8.sp
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(EditorialPurpleCard),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userInitials,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = EditorialPrimaryDark
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(EditorialNavBg)
            .border(1.dp, EditorialBorder.copy(alpha = 0.2f))
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val overviewActive = selectedTab == DashboardTab.Overview
        val historyActive = selectedTab == DashboardTab.History
        val settingsActive = selectedTab == DashboardTab.Settings

        // Tab 1: Overview
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { onTabSelected(DashboardTab.Overview) }
                .testTag("nav_tab_overview"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (overviewActive) EditorialPurpleCard else Color.Transparent)
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(18.dp)) {
                    drawRect(color = if (overviewActive) EditorialPrimaryDark else EditorialMutedText)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Overview",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (overviewActive) EditorialPrimaryDark else EditorialMutedText
            )
        }

        // Tab 2: History
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { onTabSelected(DashboardTab.History) }
                .testTag("nav_tab_history"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (historyActive) EditorialPurpleCard else Color.Transparent)
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "History Icon",
                    modifier = Modifier.size(18.dp),
                    tint = if (historyActive) EditorialPrimaryDark else EditorialMutedText
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "History",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (historyActive) EditorialPrimaryDark else EditorialMutedText
            )
        }

        // Tab 3: Settings
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable { onTabSelected(DashboardTab.Settings) }
                .testTag("nav_tab_settings"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (settingsActive) EditorialPurpleCard else Color.Transparent)
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings Icon",
                    modifier = Modifier.size(18.dp),
                    tint = if (settingsActive) EditorialPrimaryDark else EditorialMutedText
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Settings",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (settingsActive) EditorialPrimaryDark else EditorialMutedText
            )
        }
    }
}

private fun getInitials(name: String): String {
    val uppercase = name.trim().uppercase()
    if (uppercase.isBlank()) return "JD"
    val split = uppercase.split(" ")
    return if (split.size >= 2) {
        "${split[0].firstOrNull() ?: 'J'}${split[1].firstOrNull() ?: 'D'}"
    } else {
        "${uppercase.firstOrNull() ?: 'J'}"
    }
}
