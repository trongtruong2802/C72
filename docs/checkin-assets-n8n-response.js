const input = $json;
const summary = input.summary ?? {};

function toInt(value) {
  const parsed = Number.parseInt(String(value ?? '').trim(), 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
}

function toText(value) {
  return String(value ?? '').trim();
}

function toRows(value) {
  return Array.isArray(value) ? value : [];
}

return [
  {
    json: {
      success: Boolean(input.success),
      message: toText(input.message),
      session_id: toText(input.session_id || summary.session_id),
      total_received: toInt(input.total_received ?? summary.total_received),
      total_scanned_valid: toInt(input.total_scanned_valid ?? summary.total_scanned_valid),
      total_skipped: toInt(input.total_skipped ?? summary.total_skipped),
      total_inserted: toInt(input.total_inserted ?? summary.total_inserted),
      inserted_rows: toRows(input.inserted_rows),
    }
  }
];
