plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "org.example.EconomistBot")
    }
}

tasks.register<Jar>("fatJar") {
    manifest {
        attributes["Main-Class"] = "org.example.EconomistBot"
    }

    // Handle duplicates
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}


dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.jsoup:jsoup:1.16.2")
    //implementation ("com.github.pengrad:java-telegram-bot-api:6.9.1")
    implementation ("org.telegram:telegrambots:6.8.0")
    // https://mvnrepository.com/artifact/com.vladsch.flexmark/flexmark
    implementation ("com.vladsch.flexmark:flexmark-all:0.62.2")

}

tasks.test {
    useJUnitPlatform()
}