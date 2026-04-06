# バッテリーグラフアプリ 設計

## 1. アーキテクチャ

本アプリは、Androidのモダンアーキテクチャに基づき、以下の構成を採用する。

- UI: Jetpack Compose
- 状態管理: ViewModel + StateFlow
- データ層: Repositoryパターン
- 永続化: Room
- バックグラウンド処理: WorkManager

レイヤ構造:

UI (Compose)
↓
ViewModel
↓
Repository
↓
DataSource (Room / BatteryManager)

## ディレクトリ構成

data/
  ├ entity/
  ├ dao/
  └ database/

repository/

domain/（必要なら）

ui/
  ├ screen/
  └ viewmodel/

worker/

util/
