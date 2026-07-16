package com.example.ghostride

import java.util.UUID

data class Driver(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)