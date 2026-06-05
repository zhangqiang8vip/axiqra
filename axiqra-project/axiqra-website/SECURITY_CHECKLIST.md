# Axiqra Website Security Checklist

## 中文

上线前检查：

- 不提交 `.env`、日志、候补名单 CSV、Claude/Codex 本地权限文件。
- `docker compose` 只把 Nginx 暴露到本机或可信反代，API 容器不直接对公网暴露。
- 生产域名只使用 `https://axiqra.com` 和 `https://www.axiqra.com`。
- Cloudflare / 反代开启 HTTPS、自动跳转 HTTPS、基础 DDoS 防护。
- 定期备份 `data/waitlist.csv`，下载后先用文本编辑器检查，不直接双击可疑 CSV。
- 表单只收集早期访问需要的信息，不收集 secret、私有日志、源码或客户数据。
- 修改 CSP 前先确认没有放开 `*`、第三方脚本或不必要的 iframe 权限。

## English

Before deployment:

- Do not commit `.env`, logs, waitlist CSV files, or local Claude/Codex permission files.
- Expose only Nginx to localhost or a trusted reverse proxy. Do not expose the API container directly.
- Use only `https://axiqra.com` and `https://www.axiqra.com` in production.
- Enable HTTPS, HTTPS redirects, and basic DDoS protection at Cloudflare or the reverse proxy.
- Back up `data/waitlist.csv` regularly. Inspect it as text before opening it in spreadsheet software.
- The waitlist form should not collect secrets, private logs, source code, or customer data.
- When changing CSP, do not allow `*`, third-party scripts, or unnecessary iframe permissions.
