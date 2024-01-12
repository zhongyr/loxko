package tool

import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString


fun defineType(buffer: Buffer, baseName: String, className: String, fieldList: String) {
  val fields: List<Pair<String, String>> = fieldList.split(",").map { field ->
    val (type, name) = field.trim().split(" ").map { it.trim() }
    Pair(type, name)
  }
  buffer.writeString("  class $className (")
  fields.map { field ->
    buffer.writeString("val ${field.second} : ${field.first},")
  }
  buffer.writeString(") : $baseName() { \n")
  buffer.writeString("""
    override fun<R> accept(visitor: Visitor<R>) : R {
      return visitor.visit$className$baseName(this)
    }
  }
  """.trimIndent())
}

fun defineVisitor(buffer: Buffer, baseName: String, types: List<String>) {
  buffer.writeString("  interface Visitor<R> {\n")

  types.map {
    val typeName = it.split(":")[0].trim()
    buffer.writeString("    fun visit$typeName$baseName(${baseName.lowercase()} : $typeName) : R\n")
  }

  buffer.writeString("  }\n")
}

fun defineAst(outputDir: String, baseName: String, types: List<String>) {
  val path: String = "$outputDir/$baseName.kt"
  Buffer().apply {
    val content = """
package lox

abstract class $baseName {

companion object {

      """.trimIndent()
    this.writeString(content)

    types.map { type ->
      val (className, fields) = type.split(":").map { it.trim() }
      defineType(this, baseName, className, fields)
    }
    // companion object
    this.writeString(
      """
      }
      
    """.trimIndent())

    defineVisitor(this, baseName, types)
    this.writeString("\n")
    this.writeString("  abstract fun<R> accept(visitor: Visitor<R>) : R \n")
    // class
    this.writeString(
      """
      }
    """.trimIndent())

  }.let { buffer ->
    SystemFileSystem.sink(Path(path)).write(buffer, buffer.size)
  }
}
