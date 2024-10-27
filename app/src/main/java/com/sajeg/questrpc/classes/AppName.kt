package com.sajeg.questrpc.classes

data class AppName(val packageName: String, var name: String) {
    override fun toString(): String {
        return "$packageName,$name"
    }
}
