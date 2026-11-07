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
})();
