// Assume that the daemon on CI is started and will have access to the CI environment variables.
fun isEnvPresent(name: String) = System.getenv(name) != null

fun isOnCi(): Boolean = isEnvPresent("CI")

buildCache {
    local<DirectoryBuildCache> {
        directory = File(rootDir, ".gradle-build-cache")
        isEnabled = !isOnCi()
        removeUnusedEntriesAfterDays = 30
    }
}
