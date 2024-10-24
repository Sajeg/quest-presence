package com.sajeg.questrpc

data class AppName(val packageName: String, val name: String) {
    override fun toString(): String {
        return "$packageName,$name"
    }
}
