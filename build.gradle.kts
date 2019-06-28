import buildsrc.ProjectInfo
import com.jfrog.bintray.gradle.BintrayExtension
import java.io.ByteArrayOutputStream
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
  id("com.gradle.build-scan") version "2.3"

  `java-library`
  `maven-publish`

  kotlin("jvm") version "1.3.40"

  id("nebula.release") version "10.1.1"
  id("com.github.ben-manes.versions") version "0.21.0"
  id("com.jfrog.bintray") version "1.8.4"
}

version = "0.1.0"
group = "com.mkobit.junit.jupiter.aws"
description = "Dynamo DB Local test extensions for JUnit"

val gitCommitSha: String by lazy {
  ByteArrayOutputStream().use {
    project.exec {
      commandLine("git", "rev-parse", "HEAD")
      standardOutput = it
    }
    it.toString(Charsets.UTF_8.name()).trim()
  }
}

val SourceSet.kotlin: SourceDirectorySet
  get() = withConvention(KotlinSourceSet::class) { kotlin }

buildScan {
  fun env(key: String): String? = System.getenv(key)

  termsOfServiceAgree = "yes"
  termsOfServiceUrl = "https://gradle.com/terms-of-service"

  // Env variables from https://circleci.com/docs/2.0/env-vars/
  if (env("CI") != null) {
    logger.lifecycle("Running in CI environment, setting build scan attributes.")
    tag("CI")
    env("CIRCLE_BRANCH")?.let { tag(it) }
    env("CIRCLE_BUILD_NUM")?.let { value("Circle CI Build Number", it) }
    env("CIRCLE_BUILD_URL")?.let { link("Build URL", it) }
    env("CIRCLE_SHA1")?.let { value("Revision", it) }
//    Issue with Circle CI/Gradle with caret (^) in URLs
//    see: https://discuss.gradle.org/t/build-scan-plugin-1-10-3-issue-when-using-a-url-with-a-caret/24965
//    see: https://discuss.circleci.com/t/circle-compare-url-does-not-url-escape-caret/18464
//    env("CIRCLE_COMPARE_URL")?.let { link("Diff", it) }
    env("CIRCLE_REPOSITORY_URL")?.let { value("Repository", it) }
    env("CIRCLE_PR_NUMBER")?.let { value("Pull Request Number", it) }
    link("Repository", ProjectInfo.projectUrl)
  }
}

repositories {
  jcenter()
  mavenCentral()
  maven {
    name = "dynamodb-local-oregon"
    url = uri("https://s3-us-west-2.amazonaws.com/dynamodb-local/release")
  }
}

configurations.all {
  resolutionStrategy.eachDependency {
    when (requested.group) {
      "dev.minutest" -> useVersion("1.7.0")
      "org.junit.jupiter" -> useVersion("5.4.2")
      "org.junit.platform" -> useVersion("1.4.2")
      "io.strikt" -> useVersion("0.21.1")
      "org.apache.logging.log4j" -> useVersion("2.11.2")
    }
  }
}

dependencies {
  api("software.amazon.awssdk:dynamodb:2.6.2")
  api("com.amazonaws:DynamoDBLocal:1.11.477")
  api("org.junit.jupiter:junit-jupiter-api")

  testImplementation(kotlin("stdlib-jdk8"))
  testImplementation(kotlin("reflect"))

  testImplementation("io.mockk:mockk:1.9.3")
  testImplementation("io.strikt:strikt-core")
  testImplementation("dev.minutest:minutest")
  testImplementation("org.junit.platform:junit-platform-testkit:")
  testImplementation("org.junit.platform:junit-platform-runner")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testRuntimeOnly("org.apache.logging.log4j:log4j-core")
  testRuntimeOnly("org.apache.logging.log4j:log4j-jul")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
  wrapper {
    gradleVersion = "5.5"
  }

  withType<Jar> {
    from(project.projectDir) {
      include("LICENSE.txt")
      into("META-INF")
    }
    manifest {
      attributes(mapOf(
          "Build-Revision" to gitCommitSha,
          "Automatic-Module-Name" to ProjectInfo.automaticModuleName,
          "Implementation-Version" to project.version
      ))
    }
  }

  withType<Test>().configureEach {
    systemProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
    testLogging {
      events("skipped", "failed")
    }
  }

  withType<Javadoc>().configureEach {
    options {
      header = project.name
      encoding = "UTF-8"
    }
  }

  withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
  }

  val sourcesJar by creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.map { it.allSource })
    description = "Assembles a JAR of the source code"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
  }

  val javadocJar by creating(Jar::class) {
    dependsOn(javadoc)
    from(javadoc.map { it.destinationDir })
    archiveClassifier.set("javadoc")
    description = "Assembles a JAR of the generated Javadoc"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
  }

  assemble {
    dependsOn(sourcesJar, javadocJar)
  }

  prepare {
    // disable Git upstream checks
    enabled = false
  }

  (release) {
    dependsOn(bintrayUpload)
    // disabled to not push git tag
    enabled = false
  }
}

val publicationName = "jupiterExtensions"
publishing {
  publications {
    val sourcesJar by tasks.getting
    val javadocJar by tasks.getting
    register(publicationName, MavenPublication::class) {
      from(components["java"])
      artifact(sourcesJar)
      artifact(javadocJar)
      pom {
        description.set(project.description)
        url.set(ProjectInfo.projectUrl)
        licenses {
          license {
            name.set("The MIT License")
            url.set("https://opensource.org/licenses/MIT")
            distribution.set("repo")
          }
        }
      }
    }
  }
}

bintray {
  user = findProperty("bintray.user") as String?
  key = findProperty("bintray.key") as String?
  publish = true
  setPublications(publicationName)
  pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
    repo = "junit"
    name = project.name
    userOrg = "mkobit"
    setLabels("junit", "jupiter", "junit5", "dynamodb", "aws")
    setLicenses("MIT")
    desc = project.description
    websiteUrl = ProjectInfo.projectUrl
    issueTrackerUrl = ProjectInfo.issuesUrl
    vcsUrl = ProjectInfo.scmUrl
  })
}
