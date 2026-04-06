@startuml

actor User
participant UI
participant ViewModel
participant Repository
participant Worker
participant DB

Worker -> Repository : recordBatteryLevel()
Repository -> DB : insert()

UI -> ViewModel : observe
ViewModel -> Repository : getRecords()
Repository -> DB : query
DB --> Repository
Repository --> ViewModel
ViewModel --> UI

@enduml
