package com.aphoneus.fake

import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * In-memory simulated filesystem for unit testing sysfs parsers without requiring root access.
 */
class FakeSysfsFile(
    val name: String,
    val path: String,
    private val isDirectory: Boolean,
    private var content: String = "",
    private val children: MutableMap<String, FakeSysfsFile> = mutableMapOf()
) {
    fun isDirectory(): Boolean = isDirectory
    fun exists(): Boolean = true

    fun listFiles(): Array<FakeSysfsFile> = children.values.toTypedArray()

    fun getChild(name: String): FakeSysfsFile? = children[name]

    fun putChild(name: String, file: FakeSysfsFile) {
        children[name] = file
    }

    fun readText(): String = content

    fun writeText(newContent: String) {
        content = newContent
    }

    fun newInputStream(): InputStream = ByteArrayInputStream(content.toByteArray())
}

class FakeFileSystemManager {
    private val root = FakeSysfsFile("", "/", isDirectory = true)

    fun createDirectory(path: String): FakeSysfsFile {
        val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
        var current = root
        var currentPath = ""
        for (seg in segments) {
            currentPath += "/$seg"
            var next = current.getChild(seg)
            if (next == null) {
                next = FakeSysfsFile(seg, currentPath, isDirectory = true)
                current.putChild(seg, next)
            }
            current = next
        }
        return current
    }

    fun createFile(path: String, content: String): FakeSysfsFile {
        val parentPath = path.substringBeforeLast('/')
        val fileName = path.substringAfterLast('/')
        val parent = createDirectory(parentPath)
        val file = FakeSysfsFile(fileName, path, isDirectory = false, content = content)
        parent.putChild(fileName, file)
        return file
    }

    fun getFile(path: String): FakeSysfsFile? {
        val segments = path.trim('/').split('/').filter { it.isNotEmpty() }
        var current = root
        for (seg in segments) {
            current = current.getChild(seg) ?: return null
        }
        return current
    }
}
