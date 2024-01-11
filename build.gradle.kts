import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  kotlin("multiplatform") version "1.9.22"
  application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
  mavenCentral()
}

kotlin {

  macosArm64() {
    binaries {
      executable("jlox") {
        baseName = "jlox"
        entryPoint = "lox.main"
      }
      executable("generate_ast") {
        baseName = "generate_ast"
        entryPoint = "tool.main"
      }
    }
  }
  jvm() {
    withJava()
  }

  sourceSets {
    val desktopMain by creating {
      dependsOn(commonMain.get())
    }
    commonMain {
      dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.3.0")
      }
    }
    jvmMain {
      dependsOn(commonMain.get())
    }
    macosMain.get().dependsOn(commonMain.get())

  }
}

application {
  mainClass = "MainKt"
  tasks.named<JavaExec>("run") {
    standardInput = System.`in`
  }
}