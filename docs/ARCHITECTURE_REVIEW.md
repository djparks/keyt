# Architecture and Code Review for KeyT

This document reviews the current codebase and proposes targeted improvements, with emphasis on architectural separation, maintainability, testability, and security.

Scope reviewed:
- Source: src/main/java/org/openjfx/App.java, Main.java, SystemInfo.java
- Build: pom.xml
- Docs/Script: README.md, src/main/resources/keyt.sh

Summary:
- App.java is a God class combining UI (JavaFX), keystore/certificate IO, conversion/export, and state. This hinders testability and maintainability.
- Long-running work is done synchronously on the JavaFX Application Thread (e.g., file IO, keystore loading, conversion), which can freeze the UI.
- Limited logging and structured error handling.
- Passwords are retained in fields for later operations; while arrays are used and cleared in places, copies remain as instance fields.
- Packaging uses maven-assembly jar-with-dependencies. That works, but shade or jlink/jpackage could be a better fit depending on distribution goals.
- Minor script issue: keyt.sh was pointing to my.jar instead of keyt.jar.

Recommendations

1. Architecture: Separate concerns (MVVM or MVC)
- UI layer (JavaFX): Stages/Scenes/Controllers or View + ViewModel responsible only for presentation state, user interactions, and triggering operations.
- Domain/Service layer:
  - KeystoreService: load, list entries, export certs, convert to PKCS12.
  - CertificateService: load X.509 files (PEM/DER/bundle), parse metadata.
  - ExportService: write PEM/DER, name resolution.
- Model layer:
  - CertificateInfo (alias, entryType, validFrom, validUntil, sigAlg, serial, chain length, etc.).
  - KeystoreInfo (type, path, entries), PasswordCredentials (keystorePassword, keyPassword) with lifecycle management.
- Benefits: easier unit testing (services decoupled from JavaFX), smaller UI classes, increased reuse (CLI future?).

2. Threading and responsiveness
- Perform IO/CPU-bound tasks in background threads using JavaFX Task or CompletableFuture and update UI on success/failure via Platform.runLater.
  - Examples: keystore load, conversion, exporting certificates, loading certificate bundles.
- Provide progress indicator for long operations.

3. Password handling and security
- Avoid storing passwords in long-lived fields. Pass them into service calls when needed and clear immediately after use.
- Use char[] and explicitly zero arrays in finally blocks. Consider a small PasswordScope helper that ensures wiping on close.
- For keystore conversion where both keystore and key password are needed, prompt just-in-time or cache in-memory for the minimal duration with an expiry.
- Consider using KeyStore.Builder with ProtectionParameter for finer control.

4. Error handling and user feedback
- Centralize error handling in service layer with typed exceptions (e.g., KeystoreLoadException, ExportException) carrying user-friendly messages and causes.
- In the UI, map exceptions to dialogs with clear guidance (wrong password vs. corrupt file vs. unsupported type).
- Add validation for file extensions and existence before attempting load; already partially done but can be consolidated in services.

5. Logging
- Introduce SLF4J + a simple backend (e.g., logback-classic or java.util.logging bridge) for diagnostics instead of swallowing exceptions or only showing alerts.
- Log stack traces at debug level; show concise messages to users.

6. Extensibility for formats and features
- Isolate provider-specific code (e.g., SunPKCS12 vs. JKS). Optionally, support BouncyCastle as an alternative provider via a pluggable strategy (useful for older/edge keystores).
- CertificateService: support PEM bundles, multiple certs in one file (already handled via CertificateFactory.generateCertificates), and potentially PKCS7.
- Future: view subject/issuer DN, SANs, key usages, fingerprints (SHA-1/SHA-256).

7. Testing strategy
- Unit tests for services without JavaFX (e.g., load keystore given a test resource; parse cert; export PEM; convert JKS→PKCS12 roundtrip with temporary files).
- Property-based tests for PEM/DER parsing robustness can be added later.
- UI tests optional; keep UI thin to minimize need for UI-level tests.

8. Packaging and distribution
- Continue with shaded/assembled JAR for dev simplicity, but consider:
  - maven-shade-plugin instead of assembly for better resource merging and minimization.
  - jlink + jpackage to produce native installers and bundle a runtime, eliminating end-user JDK dependency.
- Ensure Reproducible Builds by pinning plugin versions (already done) and enabling build reproducibility features where applicable.

9. Modularity and structure
- Optionally add module-info.java if you plan to use Java modules strongly; otherwise ensure JavaFX on classpath works across platforms (current approach is OK).
- Package structure proposal:
  - org.openjfx.ui (controllers, views)
  - org.openjfx.model (POJOs: CertificateInfo, KeystoreInfo)
  - org.openjfx.service (KeystoreService, CertificateService, ExportService)
  - org.openjfx.util (PasswordScope, formatting)

10. Small UI/UX improvements
- Show file name and keystore type in a status bar.
- Add context menu on table rows for export.
- Allow double-click to show certificate details in a dialog.
- Improve drop hint text (remove PKS typo, clarify PKCS12).

Concrete refactor outline (incremental)
- Step 1: Extract model class CertificateInfo and map App.TableRowData to it; adapt TableView binding.
- Step 2: Extract KeystoreService with methods:
  - KeyStore load(File file, char[] ksPassword)
  - List<CertificateInfo> listEntries(KeyStore ks)
  - void convertToPkcs12(KeyStore source, char[] ksPwd, char[] keyPwd, Path target)
  - Optional<Certificate> getCertificate(KeyStore ks, String alias)
- Step 3: Extract CertificateService with methods:
  - List<CertificateInfo> loadCertificates(File file)
- Step 4: Move file IO and keystore parsing from App into the services. Keep App focused on wiring and UI.
- Step 5: Introduce background Task usage for load/convert/export; add a progress indicator.
- Step 6: Introduce SLF4J logging and replace broad catch(Throwable) with specific exceptions.

Build/pom suggestions
- Consider replacing maven-assembly-plugin with maven-shade-plugin for fat JAR builds to avoid potential service descriptor/resource duplication issues:
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.1</version>
    <executions>
      <execution>
        <phase>package</phase>
        <goals><goal>shade</goal></goals>
        <configuration>
          <createDependencyReducedPom>true</createDependencyReducedPom>
          <transformers>
            <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
              <mainClass>org.openjfx.Main</mainClass>
            </transformer>
          </transformers>
        </configuration>
      </execution>
    </executions>
  </plugin>
- For native packaging, add javafx-maven-plugin jlink/jpackage goals later.
- Add plugins for quality gates (optional):
  - maven-enforcer-plugin (enforce Maven/Java versions)
  - spotbugs-maven-plugin, checkstyle, maven-dependency-plugin analyze

Security notes
- Do not log or display passwords.
- Be careful when deriving alias from subject names—consider sanitizing for filenames.
- When exporting, set correct file permissions on sensitive outputs where possible.

Known minor issues spotted
- keyt.sh referenced my.jar. Should be keyt.jar.
- UI thread performs IO-heavy tasks; refactor to background tasks to avoid freezes.
- Multiple catch(Throwable) blocks swallow exceptions; log them for diagnostics.
- Menu label says "Convert to PKS" (typo). Consider renaming to "Convert to PKCS12".

Roadmap proposal
- 0.1.1: Fix labels, background task for loads, script fix, add logging skeleton.
- 0.2.0: Service extraction, models, unit tests.
- 0.3.0: UX improvements (details dialog), jpackage for native installers.

This plan keeps changes incremental and low-risk, while positioning the project for easier maintenance and growth.
