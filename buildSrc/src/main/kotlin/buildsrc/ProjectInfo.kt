package buildsrc

object ProjectInfo {
  const val projectUrl = "https://github.com/mkobit/junit5-dynamodb-local-extension"
  const val issuesUrl = "https://github.com/mkobit/junit5-dynamodb-local-extension/issues"
  const val scmUrl = "https://github.com/mkobit/junit5-dynamodb-local-extension.git"

  // TODO: this should probably be a part of some buildSrc plugin that automatically configures the correct things
  const val automaticModuleName = "com.mkobit.aws.junit.jupiter.dynamodb"
}
