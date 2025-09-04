# Architecture and Code Review for KeyT (2025-09-04)

This document provides an updated architectural and code review for the KeyT application. It reflects the current codebase and recent enhancements (certificate fingerprints with copy-to-clipboard), and offers prioritized recommendations.

Scope reviewed:
- Source: src/main/java/org/openjfx/*.java, org/openjfx/model/*.java, org/openjfx/service/**/*.java
- Build: pom.xml
- Resources: src/main/resources/*
- Docs: README.md, docs/ARCHITECTURE_REVIEW.md (replaced by this document)

Summary of architecture:
- Application type: Desktop GUI built with JavaFX (non-modular classpath build).
- Layering: A pragmatic layered approach is present:
  - UI: App.java (JavaFX Application) orchestrates the UI, event handling, dialogs, and background tasks.
  - Services: KeystoreService, CertificateService, ExportService encapsulate IO/crypto operations and mapping to simple model objects.
  - Strategy: KeystoreProviderStrategy + SunJksPkcs12Strategy provide pluggable keystore loading based on file type.
  - Models: CertificateInfo, KeystoreInfo provide display-centric POJOs.
- Concurrency: Long-running operations (keystore/cert loading) are executed on background threads using JavaFX Task with UI updates via Platform.runLater and progress indicator.
- Logging: SLF4J API with slf4j-simple runtime backend.
- Packaging: Executable fat JAR using maven-assembly-plugin; JavaFX platform-specific artifacts are included via Maven profiles.

Key strengths observed:
- Separation of IO/crypto concerns into services reduces the size of the UI class compared to a monolith and improves testability potential.
- Background Task usage prevents UI freezes during keystore/certificate loading, with simple progress feedback.
- Clear user interactions: drag-and-drop support, certificate detail dialog, export actions.
- Security-conscious touches: char[] used for passwords, wiping arrays after use when possible.
- Cross-platform build setup for JavaFX with sensible defaults and profiles.

Notable risks and issues:
- UI class size: App.java remains large and multi-responsibility (UI, some orchestration state, logic for dialogs, formatting), which can hinder maintainability.
- Password lifecycle: Copies of passwords are still stored on the App instance for subsequent operations (currentKeystorePassword/currentKeyPassword). While arrays are wiped in places, long-lived storage increases exposure risk.
- Exception handling: Some catch-all blocks (e.g., catch Exception/Throwable and ignore) remain; most errors are surfaced to dialogs, but diagnostics may be lost if not logged.
- Limited automated tests: No unit tests are present; services are testable but currently untested in the repo.
- Packaging via assembly: Works, but shade plugin may offer better resource handling; native packaging not present.

Runtime view and data flow:
- Startup: Main launches App (JavaFX). App builds UI, binds table to observable list.
- File open:
  - Keystore: prompt for passwords, load via KeystoreService.load on background Task, map entries via KeystoreService.listEntries, update table. Type inferred from file extension, displayed on status bar.
  - Certificates: CertificateService.loadCertificates reads one or multiple X.509 certs (PEM/DER/PKCS7 handled by CertificateFactory.generateCertificates), mapped to CertificateInfo and displayed.
- Details: Double-click row to show certificate details including subject/issuer, validity, algorithms, extensions (SAN, KU, EKU, BasicConstraints) when available, and fingerprints (MD5/SHA-1/SHA-256) in keytool-style colon-separated hex with one-click clipboard copy.
- Export: ExportService writes selected certificate to PEM or DER. JKS→PKCS12 conversion is available via service methods.

Threading model:
- Potentially blocking IO runs in Task executed on a new Thread; UI updates are coordinated via Platform.runLater.
- ProgressIndicator visibility is toggled around Task lifecycle.

Error handling and logging:
- Services throw typed exceptions (ServiceExceptions.*) for load/export issues.
- UI shows alerts for user-visible errors and leverages SLF4J logging (slf4j-simple) for debug stack traces in services.

Security considerations:
- Passwords accepted via dialogs; char[] used.
- Arrays cleared after use in some flows; however, copies stored as fields may remain longer than necessary. Consider shortening lifetime and wiping on completion of each operation.
- No sensitive data is logged; continue to avoid logging passwords or certificate private material.

Build and dependencies:
- Java 17 target; JavaFX 22.0.1 with platform classifiers.
- SLF4J API + simple backend at runtime.
- Executable JAR assembled with maven-assembly-plugin (jar-with-dependencies) with Main set to org.openjfx.Main.

Recommendations (prioritized, low effort first):
1) Tighten password lifecycle management
   - Keep keystore/key passwords only as local variables for the minimal necessary scope; avoid storing them as fields on App when possible.
   - Introduce a PasswordScope helper (AutoCloseable) that wipes arrays in close() and limit scope with try-with-resources.
   - Wipe stored fields (currentKeystorePassword/currentKeyPassword) immediately after an operation completes if they must be stored temporarily.

2) Improve exception handling hygiene
   - Replace broad catch(Throwable) with specific exceptions; where blanket catches are needed for UI robustness, at least log at debug level before ignoring.
   - Ensure all failure paths in background Tasks route to onFailed with user-facing error and a debug log in the service layer.

3) [X] Factor UI helpers out of App
   - Extract small utilities (hex formatting, clipboard helper, dialog builders) into org.openjfx.util to reduce App class weight.
   - Consider a tiny Controller/View separation for the main table to isolate event wiring.

4) Add unit tests for services
   - KeystoreService: load a sample JKS and PKCS12 from test resources; list entries; conversion roundtrip to PKCS12 in a temp directory.
   - CertificateService: parse PEM, DER, PKCS7 bundle; map fields; cover error messages for invalid files.
   - ExportService: export selected certificate to PEM/DER and validate outputs.

5) Packaging refinements (optional)
   - Consider maven-shade-plugin instead of assembly to better merge resources and reduce size.
   - Later, add jlink/jpackage for native installers and a bundled runtime.

6) UX polish
   - Ensure all long operations show progress and are cancelable where feasible.
   - [X] Normalize keystore type detection (file extension vs. KeyStore.getType) to avoid edge-case mislabeling; fix any typos in labels.

7) Extensibility
   - Keep keystore provider strategy extensible; an optional BouncyCastle-based strategy can be added for uncommon keystores.
   - In details dialog, consider adding copy buttons for other fields (subject DN, issuer DN, serial number) to increase utility.

Known minor items to watch
- There are still a few places using Optional.get()+clone() for password arrays; prefer defensive copies with immediate wiping and clear ownership comments.
- Ensure progress indicator visibility is reset on all code paths (already handled via listeners around Task; maintain this invariant when adding new tasks).

Roadmap proposal
- 0.1.2: Improve exception handling, password lifecycle tightening, minor refactors to util, add copy buttons for serial/subject if desired.
- 0.2.0: Introduce tests for services and CI build, consider shade plugin migration.
- 0.3.0: Optional: add BouncyCastle strategy; jpackage-based native installers.

This review aims to keep the code incremental and pragmatic while improving robustness, security, and maintainability without over-engineering the UI layer.
