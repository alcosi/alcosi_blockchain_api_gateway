import com.alcosi.gradle.dependency.group.JsonGroupedGenerator
import com.alcosi.gradle.dependency.group.MDGroupedGenerator
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.github.jk1.license.LicenseReportExtension
import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.os.Os
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.extendsFrom
import org.springframework.boot.gradle.tasks.bundling.BootJar

buildscript {
    dependencies {
        classpath("com.alcosi:dependency-license-page-generator:1.0.0")
    }
}


plugins {
    id("idea")
    id("java-library")
    id("maven-publish")
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.spring") version "2.0.0"
    id("com.github.jk1.dependency-license-report") version "2.8"
    id("net.thebugmc.gradle.sonatype-central-portal-publisher") version "1.2.3"
    id("org.jetbrains.dokka") version "1.9.20"

    id("com.bmuschko.docker-remote-api") version "9.4.0"
    id("org.jetbrains.kotlin.kapt") version "2.0.0"

}



val web3jVersion = "4.12.0"
val jjwtVersion = "0.12.5"
val openApiJoinedFile = "openapi.yaml"
val openApiFile = "MarketGatewayAPI.yaml"


val gitUsername = "${System.getenv()["GIHUB_PACKAGE_USERNAME"] ?: System.getenv()["GITHUB_PACKAGE_USERNAME"]}"
val gitToken = "${System.getenv()["GIHUB_PACKAGE_TOKEN"] ?: System.getenv()["GITHUB_PACKAGE_TOKEN"]}"


val dockerUsername = System.getenv()["DOCKER_XRT_USERNAME"] ?: System.getenv()["CI_REGISTRY_USER"]
val dockerPass = System.getenv()["DOCKER_XRT_PASSWORD"] ?: System.getenv()["CI_JOB_TOKEN"]
val dockerRegistry = (System.getenv()["DOCKER_XRT_REGISTRY"] ?: System.getenv()["CI_REGISTRY"]) ?: "harbor.alcosi.com"
val dockerProjectNamespace = (System.getenv()["DOCKER_XRT_PROJECT_NAMESPACE"] ?: System.getenv()["CI_PROJECT_NAMESPACE"]) ?: "nft"
val dockerProjectName = (System.getenv()["DOCKER_XRT_PROJECT_NAME"] ?: System.getenv()["CI_PROJECT_NAME"]) ?: "nft-api-gateway"
val dockerHubProject = System.getenv()["DOCKER_XRT_PROJECT"] ?: "$dockerProjectNamespace/$dockerProjectName/"
val javaVersion = JavaVersion.VERSION_21



val env = "RELEASE"

group = "com.alcosi.nft"
version = "15.0-$env"
java.sourceCompatibility = javaVersion


val imageVersion = project.version
val dockerBuildDir = "build/docker/"
val appName = project.name
val uniqueContainerName = "$dockerRegistry/$dockerHubProject$appName:$imageVersion"
val uniqueContainerNameArm = "${uniqueContainerName}_arm"
val uniqueContainerNameX86 = "${uniqueContainerName}_x86"


//val dockerUsername = System.getenv()["DOCKER_USERNAME"]
//val dockerPass = System.getenv()["DOCKER_PASSWORD"]
//val dockerRegistry = System.getenv()["DOCKER_HUB_URL"]
//
//val dockerHubProject = System.getenv()["DOCKER_HUB_PROJECT"] ?: "nft/"
//
//val imageVersion = project.version
//val dockerBuildDir = "build/docker/"
//val appName = "nft-api-gateway"


repositories {
    mavenCentral()
    gradlePluginPortal()
    maven {
        url = uri("https://repo1.maven.org/maven2")
    }
    maven { url = uri("https://jitpack.io") }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

val repo = "github.com/alcosi/alcosi_blockchain_api_gateway"

centralPortal {
    pom {
        packaging = "jar"
        name.set(project.name)
        description.set("""
The application is a ready API Gateway with implemented features, including:
- Logging
- Authorization
- OpenAPI and Swagger distribution
- Authentication and authorization with Ethereum wallets, including refresh tokens.           
        """)
        val repository = "https://$repo"
        url.set(repository)
        licenses {
            license {
                name.set("Apache 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        scm {
            connection.set("scm:$repository.git")
            developerConnection.set("scm:git@$repo.git")
            url.set(repository)
        }
        developers {
            developer {
                id.set("Alcosi")
                name.set("Alcosi Group")
                email.set("info@alcosi.com")
                url.set("alcosi.com")
            }
        }
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
    api("com.alcosi:commons-library-basic-dependency:3.3.0.4.0.5")
    api("org.springframework.boot:spring-boot-starter-jdbc:3.3.0")
    api("org.springframework.boot:spring-boot-starter-data-redis-reactive:3.3.0")
    api("org.springframework.data:spring-data-r2dbc:3.3.0")
    api("org.springframework.cloud:spring-cloud-starter-gateway:4.1.4")
    api("io.grpc:grpc-netty:1.64.0")
    api("org.postgresql:r2dbc-postgresql:1.0.5.RELEASE")
    api("io.r2dbc:r2dbc-pool:1.0.1.RELEASE")
    api("io.github.breninsul:webflux-logging:1.1.0.6")
    api("io.projectreactor.kotlin:reactor-kotlin-extensions:1.2.2")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")
    api("org.apache.commons:commons-pool2:2.12.0")
    api("com.google.api-client:google-api-client:2.3.0")
    api("jakarta.servlet:jakarta.servlet-api:5.0.0")
    api("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    api("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    api("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
    api("org.web3j:crypto:$web3jVersion")
    annotationProcessor("org.apache.logging.log4j:log4j-core")
    kapt("org.springframework.boot:spring-boot-autoconfigure-processor")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
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
 * This task runs redocly app. Have to install it using "npm i
 * -g @redocly/cli@latest" (tested 1.0.0-beta.125)
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

configurations {
    testImplementation.extendsFrom(compileOnly)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = javaVersion.majorVersion
    }
}

tasks.withType<Test> {
    jvmArgs("-Xmx1024m", "--add-exports", "java.base/sun.security.rsa=ALL-UNNAMED")
    useJUnitPlatform()
}
val javadocJar =
    tasks.named<Jar>("javadocJar") {
        from(tasks.named("dokkaJavadoc"))
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

tasks.jar {
    enabled = true
    archiveClassifier.set("")
}
signing {
    useGpgCmd()
}

java {
    withSourcesJar()
    withJavadocJar()
}
tasks.named("generateLicenseReport") {
    outputs.upToDateWhen { false }
}
licenseReport {
    unionParentPomLicenses = false
    outputDir = "$projectDir/reports/license"
    configurations = LicenseReportExtension.ALL
    excludeOwnGroup = false
    excludeBoms = false
    renderers = arrayOf(
        JsonGroupedGenerator("group-report.json", onlyOneLicensePerModule = false),
        MDGroupedGenerator("../../DEPENDENCIES.md", onlyOneLicensePerModule = false)
    )
}

docker {
    registryCredentials {
        url.set("https://$dockerRegistry/")
        username.set(dockerUsername)
        password.set(dockerPass)
    }
}

tasks.create("createDockerfile", com.bmuschko.gradle.docker.tasks.image.Dockerfile::class) {
    dependsOn("bootJar")
    destFile.set(project.file("$dockerBuildDir/Dockerfile"))
    from("amazoncorretto:21.0.3-alpine3.19")
    runCommand("mkdir /opt/app && mkdir /opt/app/logs")
    addFile("${project.name}-$version.jar", "/opt/app/app.jar")
    entryPoint("java")
    defaultCommand(
        "-jar",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "-Dapp.home=/opt/app/",
        "/opt/app/app.jar",
    )
}

tasks.create<Copy>("copyJarToDockerBuildDir") {
    dependsOn("createDockerfile")
    from("build/libs")
    into(dockerBuildDir)
    include("*.jar")
    doLast {
        println("copy File end")
    }
}
tasks.create<DockerBuildImage>("buildDockerImageX86") {
    val buildPlatform = "linux/amd64"
    val imageName = uniqueContainerNameX86
    configBuildTask(this, buildPlatform, imageName)
}

tasks.create<DockerBuildImage>("buildDockerImageArm") {
    val buildPlatform = "linux/arm64/v8"
    val imageName = uniqueContainerNameArm
    configBuildTask(this, buildPlatform, imageName)
}
tasks.create<Task>("buildDockerImages") {
    dependsOn("buildDockerImageArm", "buildDockerImageX86")
}

tasks.create<DockerPushImage>("pushDockerImageArm") {
    dependsOn("buildDockerImageArm")
    images.addAll(listOf(uniqueContainerNameArm))
    doLast {
        println("Image pushed: $uniqueContainerNameArm")
    }
}

tasks.create<DockerPushImage>("pushDockerImageX86") {
    dependsOn("buildDockerImageX86")
    images.addAll(listOf(uniqueContainerNameX86))
    doLast {
        println("Image pushed: $uniqueContainerNameX86")
    }
}

tasks.create<Task>("pushDockerImage") {
    dependsOn("pushDockerImageArm", "pushDockerImageX86")
}

fun configBuildTask(
    dockerBuildImage: DockerBuildImage,
    buildPlatform: String,
    imageName: String,
) {
    dockerBuildImage.dependsOn("copyJarToDockerBuildDir")
    dockerBuildImage.platform.set(buildPlatform)
    dockerBuildImage.inputDir.set(project.file(dockerBuildDir))
    dockerBuildImage.images.add(imageName)
}


idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}
