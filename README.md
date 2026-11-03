# NPG418's Minecraft modding template

[English](./README.md) | [日本語](./README.ja.md)

[Gradle](https://gradle.org/) + [Kotlin](https://kotlinlang.org/) + [Version catalogs](https://docs.gradle.org/current/userguide/version_catalogs.html)
Multi loader supported modding template.

This project uses a composite build of a [build-logic](./build-logic) project to share build logic.

## Supported loaders

- Forge (via [ModDevGradle Legacy](https://github.com/neoforged/ModDevGradle))
- Fabric (via [Fabric Loom](https://github.com/FabricMC/fabric-loom))

Common code shared between loaders lives in the `common` subproject; each loader has its own subproject (`forge`,
`fabric`) that depends on it.

## Usage

1. Replace `rootProject.name` in [settings.gradle.kts](./settings.gradle.kts) to individual project name
2. Update the mod metadata in [gradle.properties](./gradle.properties) (`mod_id`, `mod_group_id`, `mod_name`, `mod_version`,
   `mod_description`, etc.)
3. Rename the base package (`com.npg418.examplemod`) across `common`, `forge`, and `fabric` to match your mod's
   package
4. Update each loader metadata template with your mod's metadata
5. Adjust dependency versions in [`gradle/`](./gradle) Version Catalogs as needed
6. (option) If you want to enable debug logging in the fabric project, update the log4j configuration file located in
   `fabric/log4j-dev.xml`.
7. Edit'n'Fun!

## Common Gradle tasks

| Task                                                              | Description                                       |
|-------------------------------------------------------------------|---------------------------------------------------|
| `./gradlew build`                                                 | Builds all subprojects                            |
| `./gradlew :forge:runClient`                                      | Launches the game client with the Forge loader    |
| `./gradlew :fabric:runClient`                                     | Launches the game client with the Fabric loader   |
| `./gradlew :forge:runServer` / `./gradlew :fabric:runServer`      | Launches a dedicated server                       |
| `./gradlew :forge:runDatagen` / `./gradlew :fabric:runDatagen`    | Runs data generation                              |

## Project structure

```
.
├── build-logic/      # Composite build providing shared convention plugins
├── common/           # Loader-agnostic mod code
├── forge/            # Forge-specific entrypoint and code
├── fabric/           # Fabric-specific entrypoint and code
├── gradle/
│   ├── libs.versions.toml         # Shared Version Catalog
│   ├── forgeLibs.versions.toml    # Forge Version Catalog
│   └── fabricLibs.versions.toml   # Fabric Version Catalog
├── gradle.properties # Mod metadata and build configuration
└── settings.gradle.kts
```

## Version Catalog

Dependency and plugin versions are organized into separated Version Catalogs under `gradle/` (`libs`, `forgeLibs`, `fabricLibs`). Convention plugins in `build-logic` apply shared configuration
(Java toolchain, repositories, common dependencies) to each loader subproject to keep per-loader `build.gradle.kts`
files minimal.

## License

See [LICENSE](./LICENSE.txt) for details.