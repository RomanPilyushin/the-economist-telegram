plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.jsoup:jsoup:1.16.2")
    //implementation ("com.github.pengrad:java-telegram-bot-api:6.9.1")
    implementation ("org.telegram:telegrambots:6.8.0")

}

tasks.test {
    useJUnitPlatform()
}