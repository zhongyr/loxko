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
  buffer.writeString(") : $baseName() {} \n")
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

    this.writeString(
      """
      }
      }
    """.trimIndent()
    )
  }.let { buffer ->
    SystemFileSystem.sink(Path(path)).write(buffer, buffer.size)
  }
}
