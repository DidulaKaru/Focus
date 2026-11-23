package com.example.currencyconverter.model

data class ModuleModel(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val colorHex: String = "#3B82F6",
    val createdAt: Long = System.currentTimeMillis()
)
