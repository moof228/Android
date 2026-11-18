# QEMU + Clang: отладка из VS Code / Cursor

Нужны расширения/пакеты:

- VS Code: **C/C++** (`ms-vscode.cpptools`)
- OS (Debian/Ubuntu):

```bash
sudo apt install clang gdb gdb-multiarch qemu-user
```

Откройте **корень репозитория** как workspace: конфиги лежат в `.vscode/` в корне, пути вида `${workspaceFolder}/qemu-debug/...`.

## Локально (одна машина)

1. `Run and Debug` → **QEMU + Clang (local :1234)** → Start.
2. Task сам соберёт `app` через Clang и поднимет:

```bash
qemu-x86_64 -g 1234 ./app
```

3. GDB из VS Code подключится к `127.0.0.1:1234`.

Поставьте breakpoint на `main` / `add` и жмите Continue.

## Дистанционно (QEMU на другой машине)

**На удалённой машине:**

```bash
cd qemu-debug
clang -g -O0 -fno-omit-frame-pointer -o app app.c
./scripts/run-qemu-gdb.sh ./app
# слушает :1234
```

**На вашей машине** — туннель к порту GDB:

```bash
ssh -L 1234:127.0.0.1:1234 user@remote-host
```

Скопируйте тот же `app` (с символами) локально в `qemu-debug/app`, затем в VS Code запустите:

- **QEMU + Clang (remote host)** — использует `gdb-multiarch`
- или **Attach only**, если QEMU уже запущен

В `launch.json` при необходимости поменяйте:

```json
"miDebuggerServerAddress": "127.0.0.1:1234"
```

(через SSH-туннель обычно оставляют `127.0.0.1`).

## Ручной запуск без VS Code

```bash
clang -g -O0 -o app app.c
qemu-x86_64 -g 1234 ./app
# в другом терминале:
gdb ./app
(gdb) target remote 127.0.0.1:1234
(gdb) break main
(gdb) continue
```

## Кросс-архитектура (например aarch64)

```bash
clang --target=aarch64-linux-gnu -g -O0 -static -o app app.c
qemu-aarch64 -g 1234 ./app
```

В `launch.json` для этой схемы:

- `"miDebuggerPath": "gdb-multiarch"`
- при необходимости добавьте в `setupCommands`: `"text": "set architecture aarch64"`
