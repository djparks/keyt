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

### Open a file on startup

You can pass a keystore or certificate file path as the first argument. Supported extensions:
- Keystore: .jks, .pks, .p12 (PKCS12)
- Certificates: .cert, .crt, .der, .pem (X.509)

Examples:
```
# Open a JKS keystore (will prompt for keystore/key passwords)
java -jar target/keyt.jar /path/to/keystore.jks

# Open a PKCS12 keystore (.p12/.pks)
java -jar target/keyt.jar /path/to/keystore.p12

# Open a certificate (PEM/DER)
java -jar target/keyt.jar /path/to/cert.pem
```

Notes:
- If the file path contains spaces, wrap it in quotes, e.g.:
  `java -jar target/keyt.jar "/path/with spaces/keystore.p12"`
- For keystores, the app will prompt for the keystore password and (optionally) a key password.
- For certificates, contents are displayed without a password.

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

## Running with command file from any folder

To make the `.sh` file executable from any folder on your Mac, you need to place it in a directory included in your system's `PATH` environment variable and ensure it has the correct permissions. Here's how to do it:

1. **Choose a Directory**: A common location for user-added scripts is `~/bin`, as it’s typically included in the `PATH` and writable by the user.

2. **Move the Script**:
    - Copy or move your `.sh` file (e.g., `keyt.sh`) to `/usr/local/bin`. Open a terminal and run:
      ```bash
      mv keyt.sh /usr/local/bin/
      ```
      You’ll need to enter your password because `/usr/local/bin` requires elevated permissions.

3. **Set Execute Permissions**:
    - Ensure the script is executable by running:
      ```bash
      chmod +x ~/bin/keyt.sh
      ```

4. **Verify the PATH**:
    - Check if `~/bin` is in your `PATH` by running:
      ```bash
      echo $PATH
      ```
      If `~/bin` is listed, you’re set. If not, add it by editing your shell configuration file (e.g., `~/.zshrc` for zsh, which is the default shell on macOS):
      ```bash
      echo 'export PATH="~/bin:$PATH"' >> ~/.zshrc
      source ~/.zshrc
      ```

5. **Test the Script**:
    - From any directory, run:
      ```bash
      keyt.sh [parameters]
      ```
      The script should execute `java -jar keyt.jar` with any parameters you pass, as long as `keyt.jar` is in the directory where you run the command or you specify its full path in the script.

**Notes**:
- Ensure `keyt.jar` is either in the current working directory when you run the script or update the script to include the full path to `keyt.jar` (e.g., `java -jar /path/to/keyt.jar "$@"`).
- If you encounter permission issues, double-check ownership with `ls -l ~/bin/keyt.sh` and adjust using `sudo chown $USER ~/bin/keyt.sh` if needed.

Now you can run `keyt.sh` from any folder, and it will pass any arguments to `java -jar keyt.jar` while ensuring Java 17 or greater is used.

## License

This project is provided as-is for demonstration purposes.
