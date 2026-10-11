# Charge Trail 設計

## 1. アーキテクチャ

本アプリは、Androidのモダンアーキテクチャに基づき、以下の構成を採用する。

- UI: Jetpack Compose（グラフは MPAndroidChart を AndroidView で埋め込む）
- 状態管理: ViewModel + StateFlow
- データ層: Repositoryパターン
- 永続化: Room（DB version 2。スキーマは app/schemas/ に保存）
- バックグラウンド処理: WorkManager（15分ごとの記録と古い記録の削除）
- 高精度記録（任意）: フォアグラウンドサービス（type=specialUse）

レイヤ構造:

UI (Compose)
↓
ViewModel
↓
Repository
↓
DataSource (Room / BatteryManager)

依存オブジェクト（Repository・設定・バッテリー状態の取得元）は `BatteryGraphApplication` でまとめて生成し、
Activity・Service・Worker から共通で使う。

## 2. 記録の流れ

| 経路 | タイミング | 記録の条件 |
|---|---|---|
| `BatteryWorker`（WorkManager） | 15分ごと（端末の省電力状態で遅れることがある） | 毎回記録し、90日より古い記録を削除する |
| `BatteryMonitorService`（高精度記録 ON 時） | `ACTION_BATTERY_CHANGED` を受け取るたび | 残量か電源接続の有無が変わったときだけ記録する |
| `MainActivity`（画面表示中） | `ACTION_BATTERY_CHANGED` を受け取るたび | 同上 |

- 書き込みは `BatteryRepository` の Mutex で直列化し、同時に受け取っても重複しないようにする
- 「最新の記録」との比較は追加順（id）で行い、端末の時刻が戻っても重複を防ぐ
- 端末の再起動後・アプリの更新後は `BootReceiver` が高精度記録を再開する

## 3. 画面

- 状態カード: 現在の残量・充電状態（充電中／接続中（充電停止）／満充電／放電中）・温度・電圧・最終記録時刻・減少ペース
- 期間の切り替え: 6時間／24時間／7日／30日
- グラフ: 充電中（電源接続中）の区間を緑で描く。7日・30日表示では点を間引く（`domain/Downsample.kt`）。
  目盛りは時計の時刻の区切りに合わせる（`ui/graph/TimeTicks.kt`）
- 横長の画面では左に操作、右にグラフを並べる。高さが足りない画面では全体をスクロールする

## ディレクトリ構成

```
data/
  ├ entity/
  ├ dao/
  └ database/
repository/
domain/        減少ペースの推定・グラフの点の間引き
service/       高精度記録のサービス・起動時の再開
ui/
  ├ graph/
  ├ screen/
  ├ theme/
  └ viewmodel/
util/          バッテリー状態の取得・設定
worker/
```
