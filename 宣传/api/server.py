from __future__ import annotations

import csv
import json
import os
import re
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

try:
    import fcntl
except ImportError:  # pragma: no cover - Windows fallback for local debugging.
    fcntl = None


DATA_PATH = Path(os.environ.get("WAITLIST_DATA_PATH", "/data/waitlist.csv"))
MAX_BODY_BYTES = int(os.environ.get("WAITLIST_MAX_BODY_BYTES", "32768"))
EMAIL_RE = re.compile(r"^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$")
ALLOWED_ORIGINS = {
    "https://axiqra.com",
    "https://www.axiqra.com",
    "http://localhost:8080",
    "http://127.0.0.1:8080",
}
FIELDNAMES = [
    "created_at",
    "name",
    "email",
    "company",
    "role",
    "use_case",
    "source",
    "consent",
    "ip",
    "user_agent",
]


def clean(value: Any, limit: int) -> str:
    text = "" if value is None else str(value)
    text = " ".join(text.replace("\x00", "").split())
    return text[:limit]


def response_payload(ok: bool, message: str, **extra: Any) -> bytes:
    payload = {"ok": ok, "message": message, **extra}
    return json.dumps(payload, ensure_ascii=False).encode("utf-8")


def read_rows(file_handle: Any) -> list[dict[str, str]]:
    file_handle.seek(0)
    sample = file_handle.read(1024)
    if not sample.strip():
        return []
    file_handle.seek(0)
    return list(csv.DictReader(file_handle))


def append_waitlist_row(row: dict[str, str]) -> tuple[bool, int]:
    DATA_PATH.parent.mkdir(parents=True, exist_ok=True)

    with DATA_PATH.open("a+", newline="", encoding="utf-8") as file_handle:
        if fcntl is not None:
            fcntl.flock(file_handle.fileno(), fcntl.LOCK_EX)

        try:
            rows = read_rows(file_handle)
            duplicate = any(
                (existing.get("email") or "").strip().lower() == row["email"].lower()
                for existing in rows
            )

            if not duplicate:
                file_handle.seek(0, os.SEEK_END)
                writer = csv.DictWriter(file_handle, fieldnames=FIELDNAMES)
                if not rows and file_handle.tell() == 0:
                    writer.writeheader()
                writer.writerow(row)
                file_handle.flush()
                rows.append(row)

            return duplicate, len(rows)
        finally:
            if fcntl is not None:
                fcntl.flock(file_handle.fileno(), fcntl.LOCK_UN)


class WaitlistHandler(BaseHTTPRequestHandler):
    server_version = "Axiqra"

    def _send_json(self, status: int, body: bytes, cors: bool = True) -> None:
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        if cors:
            origin = self.headers.get("Origin", "")
            if origin in ALLOWED_ORIGINS:
                self.send_header("Access-Control-Allow-Origin", origin)
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self) -> None:
        origin = self.headers.get("Origin", "")
        self.send_response(204)
        self.send_header("Allow", "GET, POST, OPTIONS")
        if origin in ALLOWED_ORIGINS:
            self.send_header("Access-Control-Allow-Origin", origin)
            self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_GET(self) -> None:
        path = urlparse(self.path).path
        if path in {"/health", "/api/health"}:
            self._send_json(200, response_payload(True, "ok"), cors=False)
            return

        self._send_json(404, response_payload(False, "Not found."), cors=False)

    def do_POST(self) -> None:
        path = urlparse(self.path).path
        if path not in {"/waitlist", "/api/waitlist"}:
            self._send_json(404, response_payload(False, "Not found."))
            return

        # CSRF: validate Origin header
        origin = self.headers.get("Origin", "")
        if origin and origin not in ALLOWED_ORIGINS:
            self._send_json(403, response_payload(False, "非法来源。"))
            return

        content_length = int(self.headers.get("Content-Length") or "0")
        if content_length <= 0 or content_length > MAX_BODY_BYTES:
            self._send_json(413, response_payload(False, "提交内容过大。"))
            return

        try:
            raw_body = self.rfile.read(content_length)
            payload = json.loads(raw_body.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            self._send_json(400, response_payload(False, "请求格式无效。"))
            return

        if clean(payload.get("website"), 200):
            self._send_json(200, response_payload(True, "ok", duplicate=False))
            return

        name = clean(payload.get("name"), 80)
        email = clean(payload.get("email"), 160).lower()
        company = clean(payload.get("company"), 120)
        role = clean(payload.get("role"), 80)
        use_case = clean(payload.get("use_case"), 400)
        source = clean(payload.get("source") or "axiqra-promo-page", 80)
        consent = bool(payload.get("consent"))

        if not name:
            self._send_json(400, response_payload(False, "请填写姓名。"))
            return

        if not EMAIL_RE.match(email):
            self._send_json(400, response_payload(False, "请填写有效邮箱。"))
            return

        if not role:
            self._send_json(400, response_payload(False, "请选择身份。"))
            return

        if not consent:
            self._send_json(400, response_payload(False, "请确认候补邀请授权。"))
            return

        ip = clean(self.headers.get("X-Real-IP", self.client_address[0]), 80)
        user_agent = clean(self.headers.get("User-Agent"), 240)

        row = {
            "created_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "name": name,
            "email": email,
            "company": company,
            "role": role,
            "use_case": use_case,
            "source": source,
            "consent": "yes",
            "ip": ip,
            "user_agent": user_agent,
        }

        try:
            duplicate, _count = append_waitlist_row(row)
        except OSError:
            self._send_json(500, response_payload(False, "候补名单暂时无法写入。"))
            return

        status = 200 if duplicate else 201
        message = "你已经在候补名单里了。" if duplicate else "已加入候补名单。"
        self._send_json(status, response_payload(True, message, duplicate=duplicate))

    def log_message(self, format: str, *args: Any) -> None:
        print("%s - %s" % (self.address_string(), format % args), flush=True)


def main() -> None:
    host = os.environ.get("WAITLIST_HOST", "0.0.0.0")
    port = int(os.environ.get("WAITLIST_PORT", "8080"))
    server = ThreadingHTTPServer((host, port), WaitlistHandler)
    print(f"Axiqra waitlist API listening on {host}:{port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
