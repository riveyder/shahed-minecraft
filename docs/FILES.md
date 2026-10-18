# Дополнения 0.9.0

- `race/RaceCourse`, `CourseRun` — маршруты, пересечение плоскостей и серверное время.
- `race/RaceStorage` — атомарное сохранение мира; `RaceAircraft` — граница адаптера аппаратов.
- `control/DroneRacing`, `command/RaceCommands` — заезды, редактор, рекорды и права.
- `network/Race*Payload` — ограниченные пакеты запроса, каталога и состояния.
- `client/RaceClient`, `RaceGateParticle`, `RaceScreen` — кольца, HUD и меню.
- `client/FpvHelpScreen` — RU/EN-справка с реальными назначениями клавиш.
- `docs/RACING.md`, `V0.9.0_VERIFICATION.md` — руководство и границы проверки.
- CI собирает JAR без запуска Minecraft/GameTests, согласно текущему заданию.

Ниже сохранены исторические перечни.

# Дополнения версии 0.3.0

- `assets-work/Shahed_Sheep.bbmodel` — неизменённый пользовательский источник.
- `assets-work/Shahed_Sheep_Overhaul.bbmodel` — масштаб 2,5×, 63 кубоида и встроенный атлас.
- `docs/model/overhaul.py`, `preview_overhaul.py`, `test_overhaul.py` — экспорт, предпросмотр и регрессии ресурсов.
- `docs/model/generate_audio.py` — воспроизводимые оригинальные звуки; OGG в `assets/shahed/sounds`.
- `control/DroneConfig`, `DroneRacing`, `RaceRun` — серверные настройки и гонки.
- `replay/` и `network/Replay*` — ограниченная запись, сетевой перенос, права просмотра и загрузка чанков.
- `client/DroneAudio`, `ReplayController`, `SignalLostScreen`, `FpvExtrasScreen` — клиентские подсистемы.
- `client/mixin/NightVisionMixin` — локальное ночное видение без эффектов зелья на сервере.
- `src/gametest/.../{Destruction,Replay,Extras}GameTests` — новые серверные регрессии.
- `src/clientSmoke/.../ClientSmokeTest` — опциональный графический интеграционный тест.
- `docs/AUTONOMOUS_PLAN.md`, `docs/verification/` — фактический протокол и визуальные свидетельства.

Ниже сохранён исторический перечень предыдущей версии.

---

# Файлы проекта

Репозиторий содержит основу этапа 1, реализацию этапа 2 и доработку 0.2.1. Сгенерированные `build/`, `.gradle/`, игровые миры и настройки облачной машины не входят в Git.

| Путь | Назначение |
| --- | --- |
| `build.gradle`, `gradle.properties`, `settings.gradle` | Сборка Fabric Loom, закреплённые версии, серверные GameTest |
| `gradlew`, `gradlew.bat`, `gradle/wrapper/` | Gradle Wrapper |
| `.github/workflows/build.yml` | CI-сборка и Artifacts JAR/отчёта |
| `src/main/java/io/github/riveyder/shahed/ShahedMod.java` | Общая регистрация мода |
| `src/main/java/io/github/riveyder/shahed/entity/DroneEntity.java` | Состояние дрона, сохранение владельца/импульса, тяга, гравитация, подъёмная сила, сопротивление, линейная/угловая инерция и коллизии |
| `src/main/java/io/github/riveyder/shahed/control/DroneInput.java` | Ограниченный формат команд управления |
| `src/main/java/io/github/riveyder/shahed/control/DroneSessions.java` | Серверные права, бессрочные сеансы без радиуса связи, оборудование, нейтрализация устаревшего ввода и телеметрия |
| `src/main/java/io/github/riveyder/shahed/control/DroneChunkTracking.java` | Временные билеты загрузки/симуляции вокруг камеры, совместное использование и освобождение |
| `src/main/java/io/github/riveyder/shahed/item/DroneItem.java` | Творческое размещение собственного дрона |
| `src/main/java/io/github/riveyder/shahed/item/ControllerItem.java` | Привязка UUID/владельца в предмете и запрос подключения |
| `src/main/java/io/github/riveyder/shahed/network/` | Регистрация и кодеки пакетов сеанса, ввода, выхода и телеметрии |
| `src/main/java/io/github/riveyder/shahed/mixin/` | Отдельный центр отправки чанков/сущностей для FPV, ограничение действий тела и временное подавление его гравитации без сохранённого флага |
| `src/main/java/io/github/riveyder/shahed/registry/` | Регистрация сущности, предметов и творческой вкладки |
| `src/main/java/io/github/riveyder/shahed/command/DroneCommands.java` | `/shahed kit` и `/shahed spawn` |
| `src/client/java/io/github/riveyder/shahed/client/ShahedClient.java` | Клиентская регистрация |
| `src/client/java/io/github/riveyder/shahed/client/FpvController.java` | FPV-камера, ввод, HUD и клиентская очистка сеанса |
| `src/client/java/io/github/riveyder/shahed/client/FpvKeys.java` | Настраиваемые назначения клавиш |
| `src/client/java/io/github/riveyder/shahed/client/FpvSettings.java` | Локальные предпочтения мыши и камеры |
| `src/client/java/io/github/riveyder/shahed/client/FpvSettingsScreen.java` | Меню настроек и выхода |
| `src/client/java/io/github/riveyder/shahed/client/mixin/` | Подключение FPV к камере, мыши и обычному вводу Minecraft |
| `src/client/java/io/github/riveyder/shahed/client/model/`, `renderer/` | Увеличенная детализированная модель дрона, камера и отображение с наклонами |
| `src/main/resources/fabric.mod.json`, `src/main/resources/shahed.mixins.json`, `src/client/resources/shahed.client.mixins.json` | Метаданные и конфигурация mixin |
| `src/main/resources/assets/shahed/` | Иконка, объёмные модели пульта/очков, модели и текстуры сущности, локализации `en_us`, `ru_ru`, `uk_ua` |
| `src/gametest/java/io/github/riveyder/shahed/test/StageOneGameTests.java` | Регистрация, размещение и сохранение основы |
| `src/gametest/java/io/github/riveyder/shahed/test/StageTwoGameTests.java` | Серверные проверки полёта, владения, ввода и жизненного цикла сеансов |
| `src/gametest/java/io/github/riveyder/shahed/test/PolishFlightGameTests.java` | Регрессии планирования, сопротивления, снижения, угловой инерции, тангажа и посадки |
| `src/gametest/java/io/github/riveyder/shahed/test/RemoteFpvGameTests.java` | Удалённые сеансы/отслеживание, настоящие временные билеты загрузки и очистка |
| `src/gametest/resources/fabric.mod.json` | Регистрация тестового мода |
| `README.md`, `docs/TESTING.md`, `docs/PROJECT_PLAN.md` | Использование, протокол проверок и дорожная карта |
| `docs/model/` | Исходники модели, Blockbench, предпросмотр и инструменты ресурсов |
| `.gitignore`, `.gitattributes`, `LICENSE` | Правила репозитория и лицензия |

Бинарный мод этапа 2: `build/libs/shahed-fpv-mayhem-0.2.1.jar`.
Отчёт автоматических серверных тестов: `build/gametest-results.xml`.
Локальные пользовательские настройки клиента: `config/shahed-fpv.json` в каталоге игрового профиля, не исходный файл проекта.
