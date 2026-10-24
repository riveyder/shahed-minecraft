# AUDIO LICENSES — current assets, v1.0.2

Current release uses six third-party recordings. Downloads are the publicly served HQ MP3
previews, not lossless WAV originals. Licenses were checked on the individual Freesound
pages and their Creative Commons license links on 2026-10-24. No account-only downloads,
YouTube extraction, commercial-game audio, or unlicensed recordings are included.

CC BY 4.0 permits redistribution and adaptation with credit, license link and modification
notice; those credits below must accompany redistribution of the derived piston assets.
CC0 permits redistribution/adaptation without required attribution; creators are credited anyway.
No endorsement by any recording author or aircraft manufacturer is implied.

| Recording / source | Author | License | Distributed derivatives |
|---|---|---|---|
| [Plane Cessna start stop.wav](https://freesound.org/people/Cheeseheadburger/sounds/141519/) | Cheeseheadburger | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) | engine.ogg, startup.ogg, shutdown.ogg |
| [Drone taking off and landing](https://freesound.org/people/hoshisato/sounds/822500/) | hoshisato | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | quad_engine.ogg, quad_startup.ogg, quad_shutdown.ogg, contact_quad_engine.ogg, contact_quad_startup.ogg, contact_quad_shutdown.ogg |
| [Jet Engine Start Cart.wav](https://freesound.org/people/C-V/sounds/704945/) | C-V | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | jet_engine.ogg, jet_startup.ogg, jet_shutdown.ogg |
| [Sharp Explosion 4 (of 5)](https://freesound.org/people/Rudmer_Rotteveel/sounds/336011/) | Rudmer_Rotteveel | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | blast.ogg, blast_distant.ogg, blast_echo.ogg |
| [thunder rumble 1.wav](https://freesound.org/people/FenrirFangs/sounds/234736/) | FenrirFangs | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | blast.ogg, blast_distant.ogg, blast_echo.ogg |
| [Metal Latch](https://freesound.org/people/mm4wood/sounds/760626/) | mm4wood | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | payload_release.ogg |

## Modifications and provenance

All derivatives: stereo-to-mono downmix, 48 kHz resampling, DC removal, band limiting,
conservative gain/headroom and Vorbis encoding. Piston, multirotor and turbine loops use
equal-power overlap; contact variant has a slight recorded-sample speed shift. Startup/
shutdown files are short excerpts of recorded transitions, not complete physical sequences.
Explosion: recorded firework attack + slowed/filtered recording for the body + recorded
low thunder for atmospheric decay. Distant and echo assets derive from that master.
Payload release: one cropped metal-latch action. No synthesized oscillator/noise replacements.

Exact trim times, filters and derived file hashes: [audio/edits.json](audio/edits.json).
Exact source and public CDN URLs, authors, licenses and downloaded hashes:
[audio/sources.json](audio/sources.json). Reproduction: `python docs/audio/fetch_sources.py`,
followed by `python docs/audio/produce.py` (existing FFmpeg, NumPy and SciPy). Source MP3/PCM
stay in ignored `build/`; finished derivatives and credits are distributed. Credits are also
inside every mod JAR at `assets/shahed/AUDIO_CREDITS.md`.

## Retained sounds / limitations

`wind.ogg` and `impact.ogg` remain the original SHAHED procedural CC0 assets, authored
for this project by КРИВЫЕ РУКИ AEROSPACE; generator: [model/generate_audio.py](model/generate_audio.py).
The attempted replacement metal impact (SamsterBirdies 581596) and alternative jet loop
(smidoid 128613) returned HTTP 403; neither was downloaded or included. The turbine replacement
uses the successfully retrieved C-V recording. A better recorded impact/wind remains future work.
Public previews are already lossy. Recording descriptions and numerical checks do not establish
absence of background speech, audible seams, or perceived quality. In-game listening and
multiplayer audio acceptance have NOT been performed.

---

## Historical provenance before v1.0.2 (superseded assets)

# AUDIO LICENSES — SHAHED.exe 0.4.2

Все восемь OGG созданы специально для проекта процедурным синтезом.
Аудиозаписи самолётов, YouTube, сторонние семплы и звуковые библиотеки не использовались.

| Файл | Автор / происхождение | Лицензия | Атрибуция / распространение |
| --- | --- | --- | --- |
| engine.ogg | SHAHED.exe / КРИВЫЕ РУКИ AEROSPACE; гармоники, периодическая модуляция | CC0 1.0 | Не требуется; включение в мод разрешено |
| startup.ogg | Тот же оригинальный генератор; разгон импульсного тембра | CC0 1.0 | Не требуется; включение в мод разрешено |
| shutdown.ogg | Тот же генератор; замедление и затухание | CC0 1.0 | Не требуется; включение в мод разрешено |
| wind.ogg | Периодическая сумма шумовых полос | CC0 1.0 | Не требуется; включение в мод разрешено |
| impact.ogg | Шумовой транзиент с огибающей | CC0 1.0 | Не требуется; включение в мод разрешено |
| blast.ogg | Оригинальные транзиент, басовый удар, шум и затухающий гул | CC0 1.0 | Не требуется; включение в мод разрешено |
| blast_distant.ogg | Оригинальный низкочастотный гул с длинной огибающей, 8 секунд | CC0 1.0 | Не требуется; включение в мод разрешено |
| blast_echo.ogg | Оригинальные разреженные отражения и фильтрованный шум | CC0 1.0 | Не требуется; включение в мод разрешено |

Исходный URL внешней записи, автор записи и условия сторонней библиотеки: **не применимо**.
Внешние записи не включены. Источник — [generate_audio.py](model/generate_audio.py).
Процедурно созданные аудиоактивы передаются под [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/).
Код генератора распространяется по лицензии репозитория.

Воспроизведение: `python docs/model/generate_audio.py` из проекта. Используется
уже установленный FFmpeg/libvorbis, системные программы не устанавливаются.
PCM остаётся в `build/audio`; в мод включаются только 48 kHz mono Vorbis.
Проверка декодирования, отсутствия клиппинга/пауз на стыке и манифеста:
`python docs/model/test_audio.py`. Mono выбран для позиционного воспроизведения.

Источники технической справки (не источники включённого аудио):

- [OpenAL 1.1](https://www.openal.org/documentation/openal-1.1-specification.pdf):
  положение источника/слушателя, затухание и изменение высоты тона при движении.
- [Audacity: loops](https://manual.audacityteam.org/man/tutorial_looping.html) и
  [crossfade](https://manual.audacityteam.org/man/creating_a_crossfade.html):
  непрерывность стыка. Здесь петля синтезирована как периодическая функция,
  а не обрезанная запись с тишиной на концах.

Ориентир для тембра — характерный выхлоп модельных поршневых двигателей,
описанный [SAITO](https://www.saito-mfg.com/en/welcome-to-saitos-web-site/)
и [O.S. Engines](https://www.os-engines.co.jp/english/2004autumn/2004spring/fl70/fl70main.htm).
Их описания использованы как общая справка; звук с этих сайтов не копировался.

Тембр — художественный игровой синтез, вдохновлённый общими признаками
поршневого двигателя и пропеллера. Аутентичность конкретному аппарату не заявляется.
Новые спрайты также оригинальные: [generate_explosion_sprites.py](model/generate_explosion_sprites.py),
без заимствованных изображений; распространяются по лицензии репозитория.

В 0.4.2 пересинтезированы двигатель (64 Hz, чередование гармоник, треск и пропеллерная модуляция), близкий взрыв (6,5 с), эхо (5,5 с), дальний гул (8 с). PCM нормализован до 0,78 для запаса громкости. Результат не прослушан в игре.

## v1.0.0 — флот

Девять новых оригинальных синтезированных файлов: `quad_{engine,startup,shutdown}.ogg`,
`contact_quad_{engine,startup,shutdown}.ogg`, `jet_{engine,startup,shutdown}.ogg`.
Автор: проект SHAHED.exe / КРИВЫЕ РУКИ AEROSPACE, создано процедурно для этого репозитория.
Источник: [generate_fleet_audio.py](model/generate_fleet_audio.py). Внешних записей,
YouTube-фрагментов и заимствованных сэмплов нет; атрибуция третьим лицам не требуется.
Новые аудиофайлы передаются под CC0 1.0, включая право перераспространения с модом.
Генератор — лицензия репозитория. Всего теперь 17 mono OGG; старые восемь сохранены.

Петли — 4 секунды, гармоники/модуляции периодичны, без огибающей тишины на стыке;
отдельные startup 1,2 с и shutdown 1,4 с имеют плавные огибающие. PCM имеет запас
до 0,7 пика. Кодек — Vorbis, 48 kHz mono. Проверены математический стык, запас
амплитуды и метаданные; звучание, громкость микса и отсутствие слышимых швов
в Minecraft ещё требуют ручного прослушивания.
