
description = "Kotlin Gradle Tooling support"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(kotlinStdlib())
    compile(intellijPluginDep("gradle"))
    compileOnly(intellijDep()) { includeJars("slf4j-api-1.7.25") }
    compile(files("/Users/Sergey.Rostov/Downloads/gradle-kotlin-dsl-tooling-models-6.0-20191002230139+0000.jar"))

    compileOnly(project(":kotlin-reflect-api"))
    runtime(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()
