import com.alcosi.lib.license_report.GroupedJsonReport
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.github.jk1.license.LicenseReportExtension
import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.os.Os
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("com.bmuschko.docker-remote-api") version "9.4.0"
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    id("maven-publish")
    id("com.github.jk1.dependency-license-report") version "2.5"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
}

buildscript {
    repositories {
        maven {
            name = "GitHub"
            url = uri("https://maven.pkg.github.com/alcosi/alcosi_commons_library")
            credentials {
                username = "${System.getenv()["GIHUB_PACKAGE_USERNAME"]}"
                password = "${System.getenv()["GIHUB_PACKAGE_TOKEN"]}"
            }
        }
    }
    dependencies {
        classpath("com.alcosi:report-group-plugin:1.4")
    }
}

group = "com.alcosi.nft"
version = "12.13"
java.sourceCompatibility = JavaVersion.VERSION_21
val web3jVersion = "4.10.3"
val jjwtVersion = "0.12.3"
val openApiJoinedFile = "openapi.yaml"
val openApiFile = "MarketGatewayAPI.yaml"

val gitUsername = System.getenv()["GIHUB_PACKAGE_USERNAME"]
val gitToken = System.getenv()["GIHUB_PACKAGE_TOKEN"]

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        url = uri("https://repo1.maven.org/maven2")
    }
    maven {
        name = "GitHub"
        url = uri("https://maven.pkg.github.com/alcosi/alcosi_commons_library")
        credentials {
            username = gitUsername
            password = gitToken
        }
    }
    maven { url = uri("https://jitpack.io") }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/alcosi/alcosi_blockchain_api_gateway")
            credentials {
                username = gitUsername
                password = gitToken
            }
        }
    }
    publications {
        val groupId = group.toString()
        val component = components["java"]
        val name = appName
        create<MavenPublication>("maven") {
            setGroupId(groupId)
            artifactId = name
            setVersion(project.version)
            from(component)
        }
    }
}

val dockerUsername = System.getenv()["DOCKER_USERNAME"]
val dockerPass = System.getenv()["DOCKER_PASSWORD"]
val dockerRegistry = System.getenv()["DOCKER_HUB_URL"]

val dockerHubProject = System.getenv()["DOCKER_HUB_PROJECT"] ?: "nft/"

val imageVersion = project.version
val dockerBuildDir = "build/docker/"
val appName = "nft-api-gateway"
val uniqueContainerName = "$dockerRegistry$dockerHubProject$appName:$imageVersion"

docker {
    registryCredentials {
        url.set("https://$dockerRegistry")
        username.set(dockerUsername)
        password.set(dockerPass)
    }
}
tasks.create("createDockerfile", com.bmuschko.gradle.docker.tasks.image.Dockerfile::class) {
    dependsOn("bootJar")
    destFile.set(project.file("$dockerBuildDir/Dockerfile"))
    from("amazoncorretto:21.0.2-alpine3.19")
//    runCommand("apk add --update npm")
//    runCommand("npm i -g @redocly/cli@1.2.0")
    runCommand("mkdir /opt/app && mkdir /opt/app/logs")
    addFile("api-gateway-$imageVersion.jar", "/opt/app/app.jar")
    entryPoint("java")
    defaultCommand(
        "-jar",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "-Dapp.home=/opt/app/",
        "/opt/app/app.jar",
    )
}

tasks.create("buildDockerImage", com.bmuschko.gradle.docker.tasks.image.DockerBuildImage::class) {
    dependsOn("createDockerfile")
    doFirst {
        println("copy File begin")
        copy {
            from("build/libs")
            into(dockerBuildDir)
            include("*.jar")
        }
        println("copy File end")
    }
    platform.set("linux/amd64")
    inputDir.set(project.file(dockerBuildDir))
    images.add(uniqueContainerName)
}

tasks.create<DockerPushImage>("pushDockerImage") {
    dependsOn("buildDockerImage")
    images.add(uniqueContainerName)
    doLast {
        println("Image pushed: $uniqueContainerName")
    }
}

configurations {
    configureEach {
        exclude("com.zaxxer", "HikariCP")
        exclude("org.springframework.boot", "spring-boot-starter-web")
        exclude("org.springframework.boot", "spring-boot-starter-jetty")
    }
}
tasks.compileJava {
    val dependsOn = dependsOn
    dependsOn.add(tasks.processResources)
}
tasks.compileKotlin {
    dependsOn.add(tasks.processResources)
}

dependencies {
    api("com.alcosi:commons-library-basic-dependency:3.2.2.3.3.14")
    api("org.springframework.data:spring-data-r2dbc:3.2.1")
    api("io.grpc:grpc-netty:1.61.1")
    api("org.postgresql:r2dbc-postgresql:1.0.3.RELEASE")
    api("io.r2dbc:r2dbc-pool:1.0.1.RELEASE")
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("io.github.breninsul:webflux-logging:1.1.0.6")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions:+")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:+")
    api("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    api("org.springframework.cloud:spring-cloud-starter-gateway:4.1.0")
    api("org.apache.commons:commons-pool2:2.12.0")
    api("com.google.api-client:google-api-client:2.3.0")
    api("jakarta.servlet:jakarta.servlet-api:5.0.0")
    api("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    api("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    api("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
    api("org.web3j:crypto:$web3jVersion")
    api("commons-codec:commons-codec:1.16.0")
    api("org.apache.commons:commons-text:1.11.0")
    annotationProcessor("org.apache.logging.log4j:log4j-core")
    annotationProcessor("org.springframework.boot:spring-boot-autoconfigure-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

configure<SourceSetContainer> {
    named("main") {
        java.srcDir("src/main/kotlin")
    }
}

tasks.withType<JavaCompile> {
    options.isIncremental = true
}

tasks.withType<BootJar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val copySpecApi =
        copySpec {
            from("$buildDir/resources/api")
            into("BOOT-INF/classes")
        }
    with(copySpecApi)
}

/**
 * This task runs redocly app. Have to install it using "npm i -g @redocly/cli@latest" (tested 1.0.0-beta.125)
 */
tasks.register<Exec>("joinApi") {
    val redoclyCommand =
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            "redocly.cmd"
        } else {
            "redocly"
        }

    commandLine =
        listOf(
            redoclyCommand,
            "bundle",
            "./API_OPENAPI/$openApiFile",
            "-d",
            "false",
            "--remove-unused-components",
            "false",
            "-o",
            "$buildDir/openapi/$openApiJoinedFile",
        )
    println("Executed redocly bundle!")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}
java {
    withSourcesJar()
    withJavadocJar()
}

licenseReport {
    unionParentPomLicenses = true
    outputDir = "$projectDir/reports/license"
    configurations = LicenseReportExtension.ALL
    excludeGroups = arrayOf("do.not.want")
    excludes = arrayOf("moduleGroup:moduleName")
    excludeOwnGroup = true
    excludeBoms = false
    renderers = arrayOf(GroupedJsonReport("group-report.json", false, true))
    allowedLicensesFile = File("$projectDir/config/allowed-licenses.json")
}
