plugins {
    id("org.cqfn.diktat.diktat-gradle-plugin") version "0.1.7"
}

repositories {
    mavenCentral()
}

diktat {
    inputs = files("src/**/*.kt")
}
