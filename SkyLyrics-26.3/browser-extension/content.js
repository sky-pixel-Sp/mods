(() => {
  const host = location.hostname;
  const source = host === "music.youtube.com" ? "youtube_music"
    : host === "www.youtube.com" ? "youtube" : "spotify_web";
  const isYouTube = source === "youtube_music" || source === "youtube";

  function readSession() {
    const metadata = navigator.mediaSession?.metadata;
    const media = document.querySelector("video, audio");
    const ytmusicTitle = document.querySelector("ytmusic-player-bar .title")?.textContent?.trim();
    const ytmusicArtist = document.querySelector("ytmusic-player-bar .subtitle")?.textContent?.trim();
    const youtubeTitle = document.querySelector("ytd-watch-metadata h1 yt-formatted-string")?.textContent?.trim();
    const youtubeArtist = document.querySelector("ytd-channel-name a, #owner-name a")?.textContent?.trim();
    const pageTitle = document.title
      .replace(/\s*[-–|]\s*YouTube Music\s*$/i, "")
      .replace(/\s*[-–|]\s*YouTube\s*$/i, "").trim();

    const title = (metadata?.title || (source === "youtube_music" ? ytmusicTitle : youtubeTitle) || pageTitle || "").trim();
    const artist = (metadata?.artist || (source === "youtube_music" ? ytmusicArtist : youtubeArtist) || "").trim();
    const album = (metadata?.album || "").trim();
    const artworkEntries = Array.isArray(metadata?.artwork)
      ? metadata.artwork.filter(item => typeof item?.src === "string" && item.src.startsWith("https://"))
      : [];
    const artworkUrl = artworkEntries
      .sort((a, b) => {
        const area = value => {
          const match = /^\s*(\d+)x(\d+)\s*$/.exec(value?.sizes || "");
          return match ? Number(match[1]) * Number(match[2]) : 0;
        };
        return area(b) - area(a);
      })[0]?.src || "";
    const positionState = navigator.mediaSession?.positionState;
    const durationSeconds = media && Number.isFinite(media.duration) && media.duration > 0
      ? media.duration : (Number.isFinite(positionState?.duration) ? positionState.duration : 0);
    const positionSeconds = media && Number.isFinite(media.currentTime)
      ? media.currentTime : (Number.isFinite(positionState?.position) ? positionState.position : 0);
    const durationMs = Math.max(0, Math.round(durationSeconds * 1000));
    const positionMs = Math.max(0, Math.round(positionSeconds * 1000));
    const playing = isYouTube
      ? !!media && !media.paused && !media.ended
      : navigator.mediaSession?.playbackState === "playing";

    if (!title || title === "YouTube Music" || title === "YouTube" || title === "Spotify") return;
    chrome.runtime.sendMessage({ source, title, artist, album, artworkUrl, playing, positionMs, durationMs });
  }

  readSession();
  window.setInterval(readSession, 1000);
  document.addEventListener("visibilitychange", () => { if (!document.hidden) readSession(); });

  function clickByLabel(labels) {
    const nodes = Array.from(document.querySelectorAll("button, [role='button']"));
    const found = nodes.find(node => {
      const label = ((node.getAttribute("aria-label") || "") + " " +
        (node.getAttribute("title") || "") + " " + (node.textContent || "")).toLowerCase();
      return labels.some(value => label.includes(value));
    });
    if (found) { found.click(); return true; }
    return false;
  }

  function executeSkyLyricsControl(command) {
    const media = document.querySelector("video, audio");
    switch (command) {
      case "play_pause":
        if (media) {
          if (media.paused || media.ended) media.play().catch(() => {});
          else media.pause();
        } else {
          clickByLabel(["play/pause", "pause", "play"]);
        }
        break;
      case "next":
        if (!clickByLabel(["skip forward", "next track", "next song", "next"])) {
          document.querySelector("ytmusic-player-bar .next-button, .ytp-next-button")?.click();
        }
        break;
      case "previous":
        if (!clickByLabel(["skip back", "previous track", "previous song", "previous"])) {
          document.querySelector("ytmusic-player-bar .previous-button")?.click();
        }
        break;
      case "seek_forward":
        if (media) media.currentTime = Math.min(media.duration || media.currentTime + 10, media.currentTime + 10);
        break;
      case "seek_back":
        if (media) media.currentTime = Math.max(0, media.currentTime - 10);
        break;
      case "volume_up":
        if (media) media.volume = Math.min(1, media.volume + 0.05);
        break;
      case "volume_down":
        if (media) media.volume = Math.max(0, media.volume - 0.05);
        break;
    }
  }

  chrome.runtime.onMessage.addListener(message => {
    if (message?.type === "skylyrics-control" && typeof message.command === "string") {
      executeSkyLyricsControl(message.command);
      readSession();
    }
  });

  // Active tabs poll their own targeted command queue; inactive browser tabs cannot steal commands.
  window.setInterval(() => {
    chrome.runtime.sendMessage({ type: "skylyrics-poll-controls" }, () => void chrome.runtime.lastError);
  }, 900);
})();
