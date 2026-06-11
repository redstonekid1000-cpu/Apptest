package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val isSubscribed: Boolean = false,
    val subscriptionCardNumberSuffix: String? = null,
    val subscriptionDate: Long? = null
)
