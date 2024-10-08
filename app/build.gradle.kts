plugins {
    id("application")
    id("java")
}

group = "com.barassolutions"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // OkHTTP Bill of Materials
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    // Any required OkHttp artifacts can then be declared without version
    implementation("com.squareup.okhttp3:okhttp")

    implementation("com.google.code.gson:gson:2.11.0")

//    testImplementation(platform("org.junit:junit-bom:5.10.0"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass = "com.barassolutions.Main"
}

tasks.named<Jar>("jar") {
    archiveClassifier = "" // Append "" (nothing) at the end of the generated jar file
    manifest {
        attributes["Main-Class"] = "com.barassolutions.Main"
    }
    from(configurations.runtimeClasspath.get().map(::zipTree))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
