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

Release builds read the signing key from environment variables: `KEYSTORE_PATH` (default `app/release.keystore`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. The version is set by `appVersionCode` / `appVersionName` in `gradle.properties`.

```
./gradlew bundleRelease
```

### Release
1. Raise `appVersionCode` and `appVersionName` in `gradle.properties` and commit to `main`
2. Push a tag named `v` + `appVersionName`:
   ```
   git tag v1.1.0
   git push origin v1.1.0
   ```
3. The Release workflow builds a signed APK and AAB and attaches them to a GitHub Release. Upload the AAB to Google Play.

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
