plugins { java }

group = "com.rxspicy"
version = "0.5.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.citizensnpcs.co/repo")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.123-stable")
    compileOnly("net.citizensnpcs:citizens-main:2.0.43-SNAPSHOT") {
        exclude(group = "*", module = "*")
    }
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.5")
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
