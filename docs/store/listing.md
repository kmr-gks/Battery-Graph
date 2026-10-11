# Google Play 掲載情報・申告内容

Play Console に入力する内容をまとめたものです。日本語（ja-JP）と英語（en-US）の2言語で掲載します。

## 基本情報

| 項目 | 内容 |
|---|---|
| アプリ名（30文字以内） | Charge Trail: Battery Graph |
| ホーム画面の表示名 | Charge Trail |
| パッケージ名 | com.gukos.battery_graph |
| 既定の言語 | 英語（en-US）。日本語（ja-JP）を翻訳として追加 |
| カテゴリ | ツール |
| 料金 | 無料（広告なし・アプリ内購入なし） |
| プライバシーポリシー URL | https://github.com/kmr-gks/Battery-Graph/blob/main/docs/privacy-policy.md |
| 連絡先メールアドレス | （Play Console で設定。ストアに公開される） |

## ストアの掲載情報

### 簡単な説明（80文字以内）

- ja: 電池残量の推移をグラフで記録。充電した時間や減るペース、残り時間の目安もひと目で分かります。
- en: See your battery level history as a graph, with charging times and drain rate.

### 詳しい説明（4000文字以内）

#### ja

```
Charge Trail は、スマートフォンのバッテリー残量の推移をグラフで記録するアプリです。
「最近、電池の減りが早い気がする」というときに、いつ・どれくらい減ったのかを振り返ることができます。

■ 主な機能
・残量の履歴グラフ：6時間／24時間／7日／30日の期間で表示を切り替えられます
・充電の区間を色分け：充電していた時間がグラフ上でひと目で分かります
・減少ペースと残り時間の目安：直近の減り方から「約○%/時」「残り約○時間」を表示します
・現在の状態：残量、充電中かどうか、バッテリーの温度と電圧を表示します
・高精度記録（任意）：オンにすると、残量が1%変わるたびに記録し、通知に現在の残量を表示します
・ダークテーマに対応

■ 記録のしくみ
・通常は15分ごとに自動で記録します（端末の省電力状態によって間隔が空くことがあります）
・「高精度記録」をオンにすると、残量や充電状態が変わるたびに記録します
・90日より古い記録は自動で削除されます

■ プライバシー
・記録はすべて端末内にのみ保存され、外部に送信されることはありません
・インターネット接続、広告、アクセス解析は使用していません
```

#### en

```
Charge Trail records your phone's battery level over time and shows it as a graph.
When your battery seems to drain faster than usual, you can look back and see exactly when and how much it dropped.

Features
- Battery history graph: switch between 6 hours, 24 hours, 7 days, and 30 days
- Charging periods highlighted: see at a glance when your phone was charging
- Drain rate and time left: shows "about X%/h" and "about Xh left" based on recent usage
- Current status: battery level, charging state, temperature, and voltage
- High-precision recording (optional): records every 1% change and shows the current level in a notification
- Dark theme support

How recording works
- By default, the battery level is recorded automatically every 15 minutes (the interval may be longer when your device is saving power)
- With high-precision recording on, every change in level or charging state is recorded
- Records older than 90 days are deleted automatically

Privacy
- All records stay on your device and are never sent anywhere
- No internet access, no ads, no analytics
```

### 画像素材

| 素材 | ファイル | 備考 |
|---|---|---|
| アプリアイコン（512×512） | `app/src/main/ic_launcher-playstore.png` | |
| フィーチャーグラフィック（1024×500） | `docs/store/feature-graphic.png` | 両言語共通 |
| スクリーンショット（日本語） | `docs/store/screenshots/ja/*.png` | 1080×2160。24時間・7日・ダークテーマ |
| スクリーンショット（英語） | `docs/store/screenshots/en/*.png` | 同上 |

スクリーンショットのグラフは、表示確認用の模擬データです。

## アプリのコンテンツ（申告）

### フォアグラウンド サービス

- 種類：**特殊な用途（specialUse）**
- 用途：ユーザーが「高精度記録」をオンにした場合のみ動作する。初期設定はオフ

説明（en）:

```
When the user turns on "High-precision recording" (off by default), the app runs a foreground service that listens for ACTION_BATTERY_CHANGED and records the battery level and charging state each time they change. This broadcast cannot be received in the background without a running process, and WorkManager runs at most every 15 minutes, so a foreground service is the only way to record every 1% change. The service does no polling or network access, and the persistent notification shows the current battery level. The user can turn it off at any time from the main screen.
```

説明（ja）:

```
ユーザーが「高精度記録」をオンにした場合（初期設定はオフ）のみ、フォアグラウンドサービスで ACTION_BATTERY_CHANGED を受信し、残量と充電状態が変わるたびに記録します。このブロードキャストはプロセスが動作していないと受信できず、WorkManager は最短でも15分間隔のため、1%ごとの変化を記録するにはフォアグラウンドサービスが必要です。サービスは定期的なポーリングやネットワーク通信を行わず、常駐通知には現在の残量を表示します。ユーザーはメイン画面からいつでもオフにできます。
```

システムによって処理が延期・中断された場合の影響（en）:

```
Battery level changes that happen while the service is stopped are not recorded, leaving gaps in the user's battery history, and the notification showing the current level disappears. Basic recording still continues every 15 minutes via WorkManager.
```

動画（提出用の撮影手順）:

1. アプリを起動し、メイン画面の「高精度記録」スイッチをオンにする
2. 通知の許可ダイアログで「許可」を選ぶ
3. ホーム画面に戻り、通知パネルを開いて残量の通知が表示されていることを見せる
4. （可能なら）充電ケーブルを抜き差しし、通知が「充電中」に変わることを見せる
5. アプリに戻り、スイッチをオフにすると通知が消えることを見せる

動画は YouTube などに限定公開でアップロードし、その URL を入力します。

### データ セーフティ

| 質問 | 回答 |
|---|---|
| 必須のユーザーデータを収集または共有しますか | いいえ |
| 理由 | 記録は端末内にのみ保存し、外部に送信しない。インターネット権限もない |

### その他の申告

| 項目 | 回答 |
|---|---|
| 広告 | 含まない |
| 広告 ID | 使用しない（AD_ID 権限なし） |
| 政府機関のアプリ | いいえ |
| アプリへのアクセス | すべての機能を制限なく利用できる（ログイン不要） |
| コンテンツのレーティング | カテゴリ「ユーティリティ、生産性、コミュニケーション、その他」。暴力・性的表現などの質問はすべて「いいえ」 |
| ターゲット年齢層 | 13歳以上を推奨（13歳未満を含めるとファミリー向けポリシーの審査対象になる） |
| ニュースアプリ | いいえ |
| 健康・金融などの特殊な機能 | なし |

## リリースノート（初回）

Play Console のリリース作成画面にそのまま貼り付けられる形式です（各言語500文字以内）。

```
<en-US>
First release: battery history graph, highlighted charging periods, drain rate and estimated time left, and optional high-precision recording.
</en-US>
<ja-JP>
初回リリース。バッテリー残量の履歴グラフ、充電区間の色分け、減少ペースと残り時間の目安、高精度記録に対応しました。
</ja-JP>
```

## 公開までの流れ

1. **アプリの作成**：Play Console で上記の名前・言語・無料アプリとして作成する
2. **Play アプリ署名**：初回アップロード時に有効にする。手元の `release.keystore` は「アップロード鍵」になり、配信用の鍵は Google が管理する。アップロード鍵をなくすと更新できなくなる（再発行の手続きは可能）ので、安全な場所に保管する
3. **AAB のアップロード**：`gradle.properties` の `appVersionCode` と `appVersionName` を上げて `main` にコミットし、`v` + `appVersionName` のタグ（例：`v1.1.0`）を push する。Release ワークフローが署名済みの AAB を作って GitHub Release に置くので、それを Play Console にアップロードする
4. **テスト**：まず「内部テスト」で配信し、実機で動作を確認する
5. **クローズドテスト**：個人の開発者アカウントは、本番公開の前にクローズドテストが必要な場合がある（12人以上のテスターが14日間参加、など）。条件はアカウントの種類と作成時期で変わるため、Play Console の表示を確認する
6. **本番公開の申請**：上記の掲載情報と申告をすべて入力し、審査に提出する
