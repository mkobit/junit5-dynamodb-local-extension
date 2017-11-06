package buildsrc

object ProjectInfo {
  val projectUrl = "https://github.com/mkobit/junit5-dynamodb-local-extension"
  val issuesUrl = "https://github.com/mkobit/junit5-dynamodb-local-extension/issues"
  val scmUrl = "https://github.com/mkobit/junit5-dynamodb-local-extension.git"

  // TODO: this should probably be a part of some buildSrc plugin that automatically configures the correct things
  val automaticModuleName = "com.mkobit.aws.junit.jupiter.dynamodb"
}
