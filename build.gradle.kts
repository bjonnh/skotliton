plugins {
    kotlin("jvm")
//    kotlin("plugin.serialization")
    id("com.github.ben-manes.versions")
    id("maven-publish")
    id("com.jfrog.bintray")
    id("com.github.johnrengelman.shadow")
    id("org.jetbrains.dokka")
    id("fr.coppernic.versioning")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jmailen.kotlinter")
}

val publicationName: String by project
group = "net.nprod"
version = "0.0.1" + if (System.getProperty("snapshot")?.isEmpty() != false) {
    ""
} else {
    "-SNAPSHOT"
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
    maven("http://oss.jfrog.org/artifactory/oss-snapshot-local/")
}

dependencies {
    //val serializationRuntimeVersion: String by project
    val kotlinLoggingVersion: String by project
    val junitApiVersion: String by project
    val slf4jVersion: String by project

    implementation(kotlin("stdlib"))
    // implementation(kotlin("reflect"))
    // implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", serializationRuntimeVersion)

    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion") {
        exclude("org.slf4j")
    }

    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$slf4jVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitApiVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitApiVersion")
}

/**
 * The compatibility section (minimal version of Java compatible)
 */

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
    }

    dokkaHtml.configure {
        outputDirectory.set(buildDir.resolve("dokka"))
    }

    withType<com.jfrog.bintray.gradle.tasks.BintrayUploadTask> {
        dependsOn("build")
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

/*val javadocJar by tasks.registering(Jar::class) {
    dependsOn("dokkaH")
    archiveClassifier.set("javadoc")
    from("$buildDir/dokka")
}*/

publishing {
    publications {
        create<MavenPublication>(publicationName) {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
            artifact(sourcesJar.get())
            // artifact(javadocJar.get())
        }
    }
}

/**
 * Configuration of test framework
 */

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

/**
 * For publishing
 */

bintray {
    val bintrayRepo: String by project
    val bintrayName: String by project
    val bintrayVCSURL: String by project
    val bintrayLicense: String by project

    user = System.getenv("BINTRAY_USER") ?: System.getProperty("bintray.user")
    key = System.getenv("BINTRAY_KEY") ?: System.getProperty("bintray.key")
    dryRun = false
    publish = true
    setPublications(publicationName)
    pkg(
        delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.PackageConfig> {
            repo = bintrayRepo
            name = bintrayName
            setLicenses(*(bintrayLicense.split(",").toTypedArray()))
            vcsUrl = bintrayVCSURL
        }
    )
}

// Quality

ktlint {
    filter {
        exclude { element -> element.file.path.contains("generated/") }
        include("**/kotlin/**")
    }
}

kotlinter {
    ignoreFailures = project.hasProperty("lintContinueOnError")
    experimentalRules = project.hasProperty("lintKotlinExperimental")
}

detekt {
    val detektVersion: String by project
    toolVersion = detektVersion
    config = rootProject.files("qc/detekt.yml")
    buildUponDefaultConfig = true
    baseline = rootProject.file("qc/detekt-baseline.xml")
}
