package com.pallas.launcher.domain.model

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val isPinned: Boolean = false
)
