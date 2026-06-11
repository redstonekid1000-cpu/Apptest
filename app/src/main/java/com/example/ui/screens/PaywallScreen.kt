package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
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
fun PaywallScreen(viewModel: AuditViewModel, modifier: Modifier = Modifier) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isProcessingPayment.collectAsState()
    val paymentError by viewModel.paymentError.collectAsState()

    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf(currentUser?.displayName ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditorialBackground)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        // Top navigation bar for logging out / changing accounts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { viewModel.logout() }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Log Out",
                    tint = EditorialPrimaryDark
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sign Out",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EditorialPrimaryDark
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Editorial Header
            Text(
                text = "Premium Access.",
                fontFamily = FontFamily.Serif,
                fontSize = 40.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                color = EditorialPrimaryDark,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            Text(
                text = "INVEST IN YOUR VISUAL IDENTITY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = EditorialMutedText,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Pricing details container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(EditorialPurpleCard)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "The Audit Club",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = EditorialPrimaryDark
                    )
                    Text(
                        text = "${'$'}1.00 / mo",
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = EditorialPrimaryDark
                    )
                }

                Text(
                    text = "Secure local membership. Cancel anytime. Unlocks comprehensive design evaluations, contrast analyzers, and touch target threshold audits powered by artificial intelligence.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = EditorialMutedText
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subscriptions Form (Visa/MC Inputs)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .border(1.dp, EditorialBorder, RoundedCornerShape(28.dp))
                    .padding(24.dp)
            ) {
                Text(
                    text = "Membership Payment",
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    color = EditorialPrimaryDark,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Cards Owner Name
                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it },
                    label = { Text("Cardholder Name") },
                    leadingIcon = { Icon(Icons.Default.Person, "Owner Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_cardholder_name"),
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

                Spacer(modifier = Modifier.height(12.dp))

                // Card Number (16 digits)
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 19) cardNumber = it }, // allow spaces
                    label = { Text("Card Number") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, "Card number") },
                    modifier = Modifier.fillMaxWidth().testTag("input_card_number"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Expiration MM/YY
                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { if (it.length <= 5) expiry = it },
                        label = { Text("Expiry (MM/YY)") },
                        leadingIcon = { Icon(Icons.Default.DateRange, "Expiry date") },
                        modifier = Modifier.weight(1.2f).testTag("input_expiry"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                    // CVV (3 digits)
                    OutlinedTextField(
                        value = cvv,
                        onValueChange = { if (it.length <= 3 && it.all { ch -> ch.isDigit() }) cvv = it },
                        label = { Text("CVV") },
                        leadingIcon = { Icon(Icons.Default.Lock, "CVV security code") },
                        modifier = Modifier.weight(0.8f).testTag("input_cvv"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                }

                if (paymentError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = paymentError ?: "",
                        color = Color(0xFFB3261E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.processSubscriptionPayment(cardNumber, expiry, cvv, cardHolder) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("subscribe_checkout_button"),
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
                            text = "Pay $1.00 USD securely",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
