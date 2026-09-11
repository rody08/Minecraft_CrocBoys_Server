"""Small SFTP helper for the CrocBoys PebbleHost server.

Credentials are read from the repository's Git-ignored .env file. The server's
host key fingerprint is checked before authentication.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import os
from pathlib import Path
import stat
import sys
import uuid

try:
    import paramiko
except ImportError:
    raise SystemExit(
        "Paramiko is not installed. Run scripts/Setup-SftpTools.ps1 first."
    )


REPOSITORY_ROOT = Path(__file__).resolve().parent.parent


def load_env(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not path.is_file():
        raise SystemExit(f"Missing local settings file: {path}")

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().strip('"').strip("'")
    return values


class FingerprintPolicy(paramiko.MissingHostKeyPolicy):
    def __init__(self, expected: str) -> None:
        self.expected = expected

    def missing_host_key(self, client, hostname, key) -> None:
        digest = base64.b64encode(hashlib.sha256(key.asbytes()).digest()).decode()
        actual = "SHA256:" + digest.rstrip("=")
        if actual != self.expected:
            raise paramiko.SSHException(
                f"Host key mismatch for {hostname}. Expected {self.expected}; got {actual}."
            )


def connect(settings: dict[str, str]) -> tuple[paramiko.SSHClient, paramiko.SFTPClient]:
    required = (
        "PEBBLEHOST_SFTP_HOST",
        "PEBBLEHOST_SFTP_PORT",
        "PEBBLEHOST_SFTP_USERNAME",
        "PEBBLEHOST_SFTP_PASSWORD",
        "PEBBLEHOST_HOST_KEY_SHA256",
    )
    missing = [name for name in required if not settings.get(name)]
    if missing:
        raise SystemExit("Fill in these values in .env: " + ", ".join(missing))

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(
        FingerprintPolicy(settings["PEBBLEHOST_HOST_KEY_SHA256"])
    )
    client.connect(
        hostname=settings["PEBBLEHOST_SFTP_HOST"],
        port=int(settings["PEBBLEHOST_SFTP_PORT"]),
        username=settings["PEBBLEHOST_SFTP_USERNAME"],
        password=settings["PEBBLEHOST_SFTP_PASSWORD"],
        look_for_keys=False,
        allow_agent=False,
        timeout=15,
        banner_timeout=15,
        auth_timeout=15,
    )
    return client, client.open_sftp()


def list_directory(sftp: paramiko.SFTPClient, remote_path: str) -> None:
    for item in sorted(sftp.listdir_attr(remote_path), key=lambda entry: entry.filename.lower()):
        kind = "directory" if stat.S_ISDIR(item.st_mode) else "file"
        print(f"{kind}\t{item.st_size}\t{item.filename}")


def remote_exists(sftp: paramiko.SFTPClient, remote_path: str) -> bool:
    try:
        sftp.stat(remote_path)
        return True
    except FileNotFoundError:
        return False


def replace_file(
    sftp: paramiko.SFTPClient,
    local_path: Path,
    current_remote_path: str,
    new_remote_path: str,
    backup_remote_path: str,
) -> None:
    local_path = local_path.resolve()
    if not local_path.is_file():
        raise SystemExit(f"Local file does not exist: {local_path}")
    if not remote_exists(sftp, current_remote_path):
        raise SystemExit(f"Current remote file does not exist: {current_remote_path}")
    if remote_exists(sftp, backup_remote_path):
        raise SystemExit(f"Remote backup already exists: {backup_remote_path}")
    if new_remote_path != current_remote_path and remote_exists(sftp, new_remote_path):
        raise SystemExit(f"New remote path already exists: {new_remote_path}")

    temporary_path = new_remote_path + ".uploading-" + uuid.uuid4().hex
    old_moved = False
    try:
        sftp.put(str(local_path), temporary_path)
        remote_size = sftp.stat(temporary_path).st_size
        local_size = local_path.stat().st_size
        if remote_size != local_size:
            raise IOError(
                f"Upload size mismatch: local={local_size}, remote={remote_size}"
            )
        sftp.rename(current_remote_path, backup_remote_path)
        old_moved = True
        sftp.rename(temporary_path, new_remote_path)
    except Exception:
        if remote_exists(sftp, temporary_path):
            sftp.remove(temporary_path)
        if old_moved and not remote_exists(sftp, current_remote_path):
            sftp.rename(backup_remote_path, current_remote_path)
        raise

    print(f"Deployed {local_path.name} to {new_remote_path}")
    print(f"Previous remote file retained at {backup_remote_path}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Access CrocBoys PebbleHost over SFTP")
    parser.add_argument(
        "--env-file", type=Path, default=REPOSITORY_ROOT / ".env"
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    list_parser = subparsers.add_parser("list", help="List one remote directory")
    list_parser.add_argument("remote_path", nargs="?")

    download_parser = subparsers.add_parser("download", help="Download one file")
    download_parser.add_argument("remote_path")
    download_parser.add_argument("local_path", type=Path)

    upload_parser = subparsers.add_parser("upload", help="Upload one file")
    upload_parser.add_argument("local_path", type=Path)
    upload_parser.add_argument("remote_path")

    probe_parser = subparsers.add_parser(
        "probe-write", help="Create and immediately remove a tiny remote test file"
    )
    probe_parser.add_argument("remote_directory", nargs="?")

    replace_parser = subparsers.add_parser(
        "replace", help="Upload, verify, and replace one remote file with rollback"
    )
    replace_parser.add_argument("local_path", type=Path)
    replace_parser.add_argument("current_remote_path")
    replace_parser.add_argument("new_remote_path")
    replace_parser.add_argument("backup_remote_path")

    args = parser.parse_args()
    settings = load_env(args.env_file.resolve())
    client, sftp = connect(settings)
    try:
        if args.command == "list":
            remote_path = args.remote_path or settings.get(
                "PEBBLEHOST_PLUGIN_DIRECTORY", "/plugins"
            )
            list_directory(sftp, remote_path)
        elif args.command == "download":
            local_path = args.local_path.resolve()
            local_path.parent.mkdir(parents=True, exist_ok=True)
            sftp.get(args.remote_path, os.fspath(local_path))
            print(f"Downloaded {args.remote_path} to {local_path}")
        elif args.command == "upload":
            local_path = args.local_path.resolve()
            if not local_path.is_file():
                raise SystemExit(f"Local file does not exist: {local_path}")
            sftp.put(os.fspath(local_path), args.remote_path)
            print(f"Uploaded {local_path} to {args.remote_path}")
        elif args.command == "probe-write":
            remote_directory = args.remote_directory or settings.get(
                "PEBBLEHOST_PLUGIN_DIRECTORY", "/plugins"
            )
            for filename in sftp.listdir(remote_directory):
                if filename.startswith(".codex-write-test-") and filename.endswith(
                    ".tmp"
                ):
                    sftp.remove(remote_directory.rstrip("/") + "/" + filename)
            remote_path = (
                remote_directory.rstrip("/")
                + "/.codex-write-test-"
                + uuid.uuid4().hex
                + ".tmp"
            )
            created = False
            try:
                test_file = sftp.file(remote_path, mode="wx")
                created = True
                with test_file:
                    test_file.write(b"CrocBoys SFTP write-access test\n")
                size = sftp.stat(remote_path).st_size
                if size < 1:
                    raise RuntimeError(f"Remote test file was empty: {remote_path}")
            finally:
                if created:
                    sftp.remove(remote_path)
            print(
                "Write and delete access verified; the temporary test file was removed."
            )
        elif args.command == "replace":
            replace_file(
                sftp,
                args.local_path,
                args.current_remote_path,
                args.new_remote_path,
                args.backup_remote_path,
            )
    finally:
        sftp.close()
        client.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
