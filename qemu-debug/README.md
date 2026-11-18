# QEMU + Clang: отладка из VS Code / Cursor

Stub QEMU (`-g порт`) говорит по **GDB Remote Serial Protocol**. Клиент может быть **GDB** или **LLDB** (`gdb-remote`).

## Что поставить

Расширения:

- **CodeLLDB** (`vadimcn.vscode-lldb`) — профили LLDB
- **C/C++** (`ms-vscode.cpptools`) — профили GDB

Пакеты (Debian/Ubuntu):

```bash
sudo apt install clang lldb gdb gdb-multiarch qemu-user
```

Откройте **корень репозитория** как workspace (конфиги в `.vscode/`).

## GDB + qemu-user (надёжный вариант)

1. Run and Debug → **QEMU + Clang + GDB (local :1234)**.
2. Собирается `app`, стартует `qemu-*-user -g 1234 ./app`, GDB цепляется к `:1234`.
3. Breakpoint на `main` / `add` → Continue.

```bash
clang -g -O0 -o app app.c
qemu-x86_64 -g 1234 ./app
# другой терминал:
gdb ./app
(gdb) target remote 127.0.0.1:1234
(gdb) break main
(gdb) continue
```

## LLDB — да, можно

В VS Code: **QEMU + Clang + LLDB (local :1234)** или **remote / attach** (нужен CodeLLDB).

В терминале:

```bash
lldb ./app
(lldb) gdb-remote 127.0.0.1:1234
```

**Важно:** у `qemu-user -g` stub’а LLDB часто видит host-процесс `qemu-*`, а не guest-ELF: breakpoints по `main` могут остаться *pending*. Для user-mode надёжнее **GDB**.

LLDB хорошо подходит, если:

- отлаживаете через **`gdbserver` / `lldb-server` внутри** полноценной VM (`qemu-system-…`), или
- подключаетесь не к stub QEMU, а к обычному remote debug server.

## Дистанционно

**Remote:**

```bash
cd qemu-debug
clang -g -O0 -fno-omit-frame-pointer -o app app.c
./scripts/run-qemu-gdb.sh ./app
```

**Локально:**

```bash
ssh -L 1234:127.0.0.1:1234 user@remote-host
```

Скопируйте тот же `app` с символами → профиль **LLDB (remote / attach)** или **GDB (remote host)**.

## Кросс (aarch64)

```bash
clang --target=aarch64-linux-gnu -g -O0 -static -o app app.c
qemu-aarch64 -g 1234 ./app
```

GDB: `gdb-multiarch` + при необходимости `set architecture aarch64`.
