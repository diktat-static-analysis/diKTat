plugins {
    id("org.cqfn.diktat.diktat-gradle-plugin") version "1.0.0-rc.4"
}

repositories {
    mavenCentral()
}

diktat {
    inputs { include("src/**/*.kt") }
}
