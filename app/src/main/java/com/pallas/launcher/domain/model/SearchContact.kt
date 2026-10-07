package com.pallas.launcher.domain.model

import java.util.UUID

data class SearchContact(
    val id: String = UUID.randomUUID().toString(),
    val nickname: String,
    val name: String,
    val phoneNumber: String
)
