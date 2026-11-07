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
    const durationMs = isYouTube && Number.isFinite(media?.duration)
      ? Math.max(0, Math.round(media.duration * 1000)) : 0;
    const positionMs = isYouTube && media && Number.isFinite(media.currentTime)
      ? Math.max(0, Math.round(media.currentTime * 1000)) : 0;
    const playing = isYouTube
      ? !!media && !media.paused && !media.ended
      : navigator.mediaSession?.playbackState === "playing";

    if (!title || title === "YouTube Music" || title === "YouTube" || title === "Spotify") return;
    chrome.runtime.sendMessage({ source, title, artist, album, playing, positionMs, durationMs });
  }

  readSession();
  window.setInterval(readSession, 1000);
  document.addEventListener("visibilitychange", () => { if (!document.hidden) readSession(); });
})();
