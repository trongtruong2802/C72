# checkin-assets response cho n8n

Muc tieu:
- Luon tra ve JSON object
- Khong co nhanh nao duoc tra body rong
- Contract phang de app parse on dinh

Gan doan code trong [checkin-assets-n8n-response.js](/E:/design/uhf-truong/docs/checkin-assets-n8n-response.js:1) vao node `Code - Format Response` ngay truoc `Respond to Webhook`.

Response can tra ve cho app:

```json
{
  "success": true,
  "message": "Khong co tai san da quet hop le de luu DB",
  "session_id": "session-abc",
  "total_received": 12,
  "total_scanned_valid": 0,
  "total_skipped": 12,
  "total_inserted": 0,
  "inserted_rows": []
}
```

Luu y quan trong:
- Khong tra object kieu `{ "summary": { ... } }` nua
- Khong de lai expression text nhu `={{ $json.total_received }}`
- Neu khong co du lieu hop le, van tra:
  - `success: true`
  - `total_inserted: 0`
  - `inserted_rows: []`
- `Respond to Webhook` phai tra body tu output cua node format response nay, content-type `application/json`

Checklist verify:
- `POST /webhook/checkin-assets` khi khong co item hop le van tra JSON object day du
- `session_id`, `total_received`, `total_scanned_valid`, `total_skipped`, `total_inserted` deu nam o root
- Khong co lan nao `HTTP 200` ma body rong
