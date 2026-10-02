plugins { java }

group = "com.rxspicy"
version = "0.6.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.citizensnpcs.co/repo")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.141-beta")
    compileOnly("net.citizensnpcs:citizens-main:2.0.44-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.6-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.18")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.testImplementation { extendsFrom(configurations.compileOnly.get()) }

// Explicit opt-in: sends synthetic designs only to the local Ollama instance.
tasks.register<JavaExec>("testBuildDesign") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.rxspicy.bigosciegf.BuildDesignProbe")
    args(providers.gradleProperty("designModel").getOrElse("qwen3-coder:30b"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.processResources {
    filesMatching("plugin.yml") { expand("version" to project.version) }
}

tasks.test {
    useJUnitPlatform()
}
