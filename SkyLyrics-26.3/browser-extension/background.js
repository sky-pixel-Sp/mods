chrome.runtime.onMessage.addListener((payload, sender, sendResponse) => {
  if (!payload || !["youtube_music", "youtube", "spotify_web"].includes(payload.source)) return;

  const enrichedPayload = { ...payload, tabId: sender?.tab?.id ?? -1 };

  fetch("http://127.0.0.1:38173/v1/browser-session", {
    method: "POST",
    headers: { "Content-Type": "text/plain;charset=UTF-8" },
    body: JSON.stringify(enrichedPayload),
    cache: "no-store"
  }).then(response => {
    sendResponse({ ok: response.ok || response.status === 204 });
  }).catch(() => sendResponse({ ok: false }));

  return true;
});
