# KeyT

KeyT is a simple desktop utility built with JavaFX for viewing and exporting information from Java KeyStores (JKS/PKCS12) and certificate files. It lets you:

- Drag and drop a keystore (.jks/.pks/.p12) or certificate file (.cert/.crt/.der/.pem) to inspect its contents
- View alias, entry type, validity period, signature algorithm, and serial number
- Export a selected certificate to PEM or DER
- Convert a JKS keystore to PKCS12 (.p12/.pks)

The app sets its window/Dock icon from `src/main/resources/icon.png` on macOS and other platforms.

## Requirements
- Java 17 or newer (JDK)
- Maven 3.8+

## Build

This project is configured to produce a single executable JAR that bundles the JavaFX libraries for your platform.

1) Clean and package:

```
mvn clean package
```

After a successful build, you will find the executable JAR:

- `target/keyt.jar`

## Run

Run the application with:

```
java -jar target/keyt.jar
```

No extra module parameters are required as the JAR includes the JavaFX dependencies (for your platform) on the classpath.

## Notes on platforms

JavaFX provides platform-specific artifacts that include native libraries. The Maven configuration auto-selects the classifier for common platforms via Maven profiles and defaults to Apple Silicon (mac-aarch64):

- macOS (Apple Silicon): `mac-aarch64`
- macOS (Intel): `mac`
- Windows (x64): `win-x86_64`
- Windows (ARM64): `win-aarch64`
- Linux (x64): `linux-x86_64`
- Linux (ARM64): `linux-aarch64`

Profiles are activated automatically by your OS/architecture. If detection fails or you need to override, you can explicitly activate a profile, for example:

```
mvn -P mac-x64 clean package
```

or build with a custom classifier:

```
mvn -Djavafx.platform=mac-aarch64 clean package
```

## Development

- Run directly via the JavaFX Maven plugin:

```
mvn clean javafx:run
```

- Main class: `org.openjfx.App`
- Module name: `org.openjfx`

## License

This project is provided as-is for demonstration purposes.
