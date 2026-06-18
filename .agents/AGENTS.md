# Custom Agent Rules

## Compilation & Building
- **Never Build Locally**: Under no circumstances should you run local build, compile, check, or validation tasks (such as `./gradlew compileDebugKotlin`, `./gradlew help`, `./gradlew detekt`, or any other gradle task that compiles or evaluates code).
- **GitHub Build Dependency**: Always push code changes directly to GitHub and monitor the build status using the GitHub CLI (`gh run watch` or `gh run view`) to verify correctness and get compilation logs.
