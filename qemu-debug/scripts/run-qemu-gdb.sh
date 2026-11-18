#!/usr/bin/env bash
# Starts the binary under QEMU user-mode and waits for GDB on :1234.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BIN="${1:-$ROOT/app}"
PORT="${QEMU_GDB_PORT:-1234}"
ARCH="$(uname -m)"

if [[ ! -x "$BIN" ]]; then
  echo "Binary not found or not executable: $BIN" >&2
  echo "Build first (VS Code task: build-clang)." >&2
  exit 1
fi

case "$ARCH" in
  x86_64|amd64) QEMU_BIN="${QEMU_BIN:-qemu-x86_64}" ;;
  aarch64|arm64) QEMU_BIN="${QEMU_BIN:-qemu-aarch64}" ;;
  *)
    echo "Unsupported host arch: $ARCH (set QEMU_BIN explicitly)" >&2
    exit 1
    ;;
esac

if ! command -v "$QEMU_BIN" >/dev/null 2>&1; then
  echo "Missing $QEMU_BIN. Install qemu-user (Debian/Ubuntu: sudo apt install qemu-user)." >&2
  exit 1
fi

echo "QEMU_GDB_READY port=${PORT} bin=${BIN} qemu=${QEMU_BIN}"
exec "$QEMU_BIN" -g "$PORT" "$BIN"
