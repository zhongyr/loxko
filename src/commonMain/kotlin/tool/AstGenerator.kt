package tool

import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString

class AstGenerator {
  companion object {
    private fun defineType(buffer: Buffer, baseName: String, className: String, fieldList: String) {
      val fields: List<Pair<String, String>> = getFields(fieldList)
      buffer.writeString("  data class $className (")
      fields.map { field ->
        buffer.writeString("val ${field.second} : ${field.first},")
      }
      buffer.writeString(") : $baseName() \n")
    }

    private fun getFields(fieldList: String) = fieldList.split(",").map { field ->
      val (type, name) = field.trim().split(" ").map { it.trim() }
      Pair(type, name)
    }

    private fun defineVisitor(buffer: Buffer, baseName: String) {
      buffer.writeString("typealias ${baseName}Visitor<R> = ($baseName) -> R \n")
    }

    fun defineAst(outputDir: String, baseName: String, types: List<String>) {
      val path: String = "$outputDir/$baseName.kt"
      Buffer().apply {
        this.writeString("""
package lox

sealed class $baseName {

      """.trimIndent())

        types.map { type ->
          val (className, fields) = type.split(":").map { it.trim() }
          defineType(this, baseName, className, fields)
        }
        // companion object
        this.writeString(
          """
      }
      
    """.trimIndent())

        defineVisitor(this, baseName)
        this.writeString("\n")
        this.writeString("""
          fun<R> $baseName.accept(visitor: ${baseName}Visitor<R>) : R {
            return visitor(this)
          }
        """.trimIndent()
        )
      }.let { buffer ->
        SystemFileSystem.sink(Path(path)).write(buffer, buffer.size)
      }
    }
  }
}