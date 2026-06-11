package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EditorialBackground
import com.example.ui.theme.EditorialBorder
import com.example.ui.theme.EditorialMutedText
import com.example.ui.theme.EditorialPrimaryDark
import com.example.ui.theme.EditorialPurpleCard
import com.example.ui.theme.EditorialTextDark
import com.example.ui.viewmodel.AuditViewModel

@Composable
fun AuthScreen(viewModel: AuditViewModel, modifier: Modifier = Modifier) {
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    val error by viewModel.authError.collectAsState()
    val isLoading by viewModel.isProcessingAuth.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorialBackground)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header
        Text(
            text = "The Audit.",
            fontFamily = FontFamily.Serif,
            fontSize = 42.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Light,
            color = EditorialPrimaryDark,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "UI/UX REVIEWS & ACCESSIBILITY AUDITS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = EditorialMutedText,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Tab selection (Toggle Auth Mode)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF3EDF7))
                .border(1.dp, EditorialBorder.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (!isSignUp) EditorialPurpleCard else Color.Transparent)
                    .clickable { isSignUp = false }
                    .testTag("tab_login"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (!isSignUp) EditorialPrimaryDark else EditorialMutedText
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSignUp) EditorialPurpleCard else Color.Transparent)
                    .clickable { isSignUp = true }
                    .testTag("tab_register"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Register",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isSignUp) EditorialPrimaryDark else EditorialMutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Card form
        val signUpOtpRequired by viewModel.signUpOtpRequired.collectAsState()
        val generatedOtp by viewModel.generatedOtp.collectAsState()
        var enteredOtp by remember { mutableStateOf("") }

        if (signUpOtpRequired) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .border(1.dp, EditorialBorder, RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Confirm Gmail Address",
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    color = EditorialPrimaryDark,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "We sent a 4-digit verification code to $email. Please enter it to verify. Since this is a local sandbox environment:",
                    fontSize = 13.sp,
                    color = EditorialMutedText,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EditorialPurpleCard)
                        .padding(12.dp)
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sandbox Activation Code: $generatedOtp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = EditorialPrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = enteredOtp,
                    onValueChange = { if (it.length <= 4) enteredOtp = it },
                    label = { Text("4-Digit OTP Code") },
                    placeholder = { Text("e.g. $generatedOtp") },
                    leadingIcon = { Icon(Icons.Default.Lock, "OTP Security Icon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_otp_code"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = EditorialTextDark,
                        unfocusedTextColor = EditorialTextDark,
                        focusedBorderColor = EditorialPrimaryDark,
                        unfocusedBorderColor = EditorialBorder,
                        focusedLabelColor = EditorialPrimaryDark,
                        unfocusedLabelColor = EditorialMutedText,
                        focusedLeadingIconColor = EditorialPrimaryDark,
                        unfocusedLeadingIconColor = EditorialMutedText,
                        cursorColor = EditorialPrimaryDark
                    )
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error ?: "",
                        color = Color(0xFFB3261E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.verifyAndRegister(email, password, displayName, enteredOtp) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_otp_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EditorialPrimaryDark,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.height(24.dp).width(24.dp))
                    } else {
                        Text(
                            text = "Verify & Access Trial",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Go Back & Edit Info",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EditorialPrimaryDark,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { viewModel.cancelOtpVerification() }
                        .padding(8.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .border(1.dp, EditorialBorder, RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = if (isSignUp) "Create Account" else "Welcome Back",
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    color = EditorialPrimaryDark,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isSignUp) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        leadingIcon = { Icon(Icons.Default.Person, "Name Icon") },
                        modifier = Modifier.fillMaxWidth().testTag("input_display_name"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = EditorialTextDark,
                            unfocusedTextColor = EditorialTextDark,
                            focusedBorderColor = EditorialPrimaryDark,
                            unfocusedBorderColor = EditorialBorder,
                            focusedLabelColor = EditorialPrimaryDark,
                            unfocusedLabelColor = EditorialMutedText,
                            focusedLeadingIconColor = EditorialPrimaryDark,
                            unfocusedLeadingIconColor = EditorialMutedText,
                            cursorColor = EditorialPrimaryDark
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, "Email Icon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("input_email"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = EditorialTextDark,
                        unfocusedTextColor = EditorialTextDark,
                        focusedBorderColor = EditorialPrimaryDark,
                        unfocusedBorderColor = EditorialBorder,
                        focusedLabelColor = EditorialPrimaryDark,
                        unfocusedLabelColor = EditorialMutedText,
                        focusedLeadingIconColor = EditorialPrimaryDark,
                        unfocusedLeadingIconColor = EditorialMutedText,
                        cursorColor = EditorialPrimaryDark
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, "Lock Icon") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().testTag("input_password"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = EditorialTextDark,
                        unfocusedTextColor = EditorialTextDark,
                        focusedBorderColor = EditorialPrimaryDark,
                        unfocusedBorderColor = EditorialBorder,
                        focusedLabelColor = EditorialPrimaryDark,
                        unfocusedLabelColor = EditorialMutedText,
                        focusedLeadingIconColor = EditorialPrimaryDark,
                        unfocusedLeadingIconColor = EditorialMutedText,
                        cursorColor = EditorialPrimaryDark
                    )
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error ?: "",
                        color = Color(0xFFB3261E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.handleAuthentication(isSignUp, email, password, displayName) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_auth_button"),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EditorialPrimaryDark,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.height(24.dp).width(24.dp))
                    } else {
                        Text(
                            text = if (isSignUp) "Get Subscription Access" else "Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSignUp) "Already have an account?" else "New to The Audit?",
                fontSize = 13.sp,
                color = EditorialMutedText
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isSignUp) "Sign In" else "Register Now",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = EditorialPrimaryDark,
                modifier = Modifier.clickable { isSignUp = !isSignUp }
            )
        }
    }
}
