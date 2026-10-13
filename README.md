# Charge Trail: Battery Graph

An Android app that records your battery level over time and shows it as a graph.

バッテリー残量の推移をグラフで記録する Android アプリです。英語/日本語対応

- Battery history graph (6h / 24h / 7d / 30d), with charging periods highlighted
- Drain rate and estimated time left
- Optional high-precision recording (records every 1% change via a foreground service)
- Japanese and English

### Build

```
./gradlew assembleDebug
```

Release builds read the signing key from environment variables: `KEYSTORE_PATH` (default `app/release.keystore`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. The version is computed from git (see below).

```
./gradlew bundleRelease
```

### Version
- `versionName` = `appVersion` in `gradle.properties` (e.g. `1.1`) + the number of commits since that line was last changed, counting from 0 (e.g. `1.1.3`). Raise `appVersion` only for larger updates.
- `versionCode` = the total number of commits, so it increases with every commit.
- `./gradlew -q :app:printVersion` prints the current values.

### Release
- **Larger update**: raise `appVersion` in `gradle.properties` (e.g. `1.1` → `1.2`) and push to `main`. The **Release** workflow runs automatically.
- **Small fix**: push to `main`, then run the **Release** workflow from the Actions tab on GitHub (Run workflow, branch `main`).

The workflow tags the commit as `v` + versionName and attaches `BatteryGraph-v1.2.0-code90.aab` / `.apk` to a GitHub Release. Upload the AAB to Google Play.

### Tests

```
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

### Documents

- Requirements: [docs/requirements.md](docs/requirements.md)
- Architecture: [docs/architecture.md](docs/architecture.md)
- ER diagram: [docs/er.puml](docs/er.puml)
- Class diagram: [docs/class.puml](docs/class.puml)
- Sequence diagram: [docs/sequence.md](docs/sequence.md)
- Privacy policy: [docs/privacy-policy.md](docs/privacy-policy.md)
- Google Play listing: [docs/store/listing.md](docs/store/listing.md)
