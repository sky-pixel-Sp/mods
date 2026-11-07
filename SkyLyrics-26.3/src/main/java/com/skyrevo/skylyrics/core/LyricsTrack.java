package com.skyrevo.skylyrics.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses standard LRC, multiple line timestamps, offsets, and LRCLIB word-synced
 * tags such as [00:12.00]<00:12.00>Hello <00:12.42>world.
 */
public final class LyricsTrack {
    private static final Pattern TIMESTAMP = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]");
    private static final Pattern WORD_TIMESTAMP = Pattern.compile("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>");
    private static final Pattern OFFSET = Pattern.compile("\\[offset:([+-]?\\d+)]", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE = Pattern.compile("\\[ti:(.*?)]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ARTIST = Pattern.compile("\\[ar:(.*?)]", Pattern.CASE_INSENSITIVE);
    private static final Pattern META_TAG = Pattern.compile("\\[[a-zA-Z]+:.*?]");

    private final String title;
    private final String artist;
    private final Path source;
    private final long offsetMs;
    private final List<LyricsCue> cues;

    private LyricsTrack(String title, String artist, Path source, long offsetMs, List<LyricsCue> cues) {
        this.title = title;
        this.artist = artist;
        this.source = source;
        this.offsetMs = offsetMs;
        this.cues = List.copyOf(cues);
    }

    public static LyricsTrack load(Path file) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IOException("File lirik tidak ditemukan.");
        }
        if (!file.getFileName().toString().toLowerCase().endsWith(".lrc")) {
            throw new IOException("Format tidak didukung. Gunakan file .lrc.");
        }
        return parse(Files.readString(file, StandardCharsets.UTF_8), stripExtension(file.getFileName().toString()),
                "Unknown artist", file.toAbsolutePath().normalize());
    }

    /** Creates a track from LRC text returned by a lyrics provider or supplied by the user. */
    public static LyricsTrack fromLrcText(String text, String fallbackTitle, String fallbackArtist) throws IOException {
        return parse(text, fallbackTitle, fallbackArtist, null);
    }

    private static LyricsTrack parse(String text, String fallbackTitle, String fallbackArtist, Path source) throws IOException {
        if (text == null || text.isBlank()) {
            throw new IOException("Konten LRC kosong.");
        }
        String title = fallbackTitle == null || fallbackTitle.isBlank() ? "Unknown title" : fallbackTitle.strip();
        String artist = fallbackArtist == null || fallbackArtist.isBlank() ? "Unknown artist" : fallbackArtist.strip();
        long offsetMs = 0;
        List<LyricsCue> parsed = new ArrayList<>();

        for (String rawLine : text.split("\\R")) {
            String line = rawLine.replace("\uFEFF", "").strip();
            if (line.isEmpty()) continue;

            Matcher titleMatcher = TITLE.matcher(line);
            if (titleMatcher.find() && !titleMatcher.group(1).isBlank()) title = titleMatcher.group(1).strip();
            Matcher artistMatcher = ARTIST.matcher(line);
            if (artistMatcher.find() && !artistMatcher.group(1).isBlank()) artist = artistMatcher.group(1).strip();
            Matcher offsetMatcher = OFFSET.matcher(line);
            if (offsetMatcher.find()) {
                try { offsetMs = Long.parseLong(offsetMatcher.group(1)); }
                catch (NumberFormatException ignored) { offsetMs = 0; }
            }

            Matcher timestampMatcher = TIMESTAMP.matcher(line);
            List<Long> times = new ArrayList<>();
            int contentStart = 0;
            while (timestampMatcher.find()) {
                long time = parseTime(timestampMatcher.group(1), timestampMatcher.group(2), timestampMatcher.group(3));
                if (time < 0) continue;
                times.add(time);
                contentStart = timestampMatcher.end();
            }

            if (!times.isEmpty()) {
                String body = META_TAG.matcher(line.substring(contentStart)).replaceAll("");
                ParsedWords words = parseWordTimings(body);
                if (!words.text().isEmpty()) {
                    long primaryTime = times.getFirst();
                    for (long time : times) {
                        long shift = time - primaryTime;
                        List<LyricsCue.WordTiming> shiftedTimings = words.timings().stream()
                                .map(timing -> new LyricsCue.WordTiming(
                                        Math.max(0L, timing.timeMs() + shift), timing.wordIndex()))
                                .toList();
                        parsed.add(new LyricsCue(time, words.text(), shiftedTimings));
                    }
                }
            }
        }

        parsed.sort(Comparator.comparingLong(LyricsCue::timeMs));
        if (parsed.isEmpty()) throw new IOException("Tidak ada lirik bertimestamp yang valid di file LRC.");
        return new LyricsTrack(title, artist, source, offsetMs, parsed);
    }

    private static ParsedWords parseWordTimings(String source) {
        Matcher matcher = WORD_TIMESTAMP.matcher(source);
        StringBuilder clean = new StringBuilder(source.length());
        List<LyricsCue.WordTiming> timings = new ArrayList<>();
        int cursor = 0;
        while (matcher.find()) {
            clean.append(source, cursor, matcher.start());
            int wordIndex = countWords(clean);
            long time = parseTime(matcher.group(1), matcher.group(2), matcher.group(3));
            if (time >= 0) timings.add(new LyricsCue.WordTiming(time, wordIndex));
            cursor = matcher.end();
        }
        clean.append(source.substring(cursor));
        String text = clean.toString().replaceAll("\\s+", " ").strip();
        if (text.isEmpty() || timings.isEmpty()) return new ParsedWords(text, List.of());

        timings.sort(Comparator.comparingLong(LyricsCue.WordTiming::timeMs)
                .thenComparingInt(LyricsCue.WordTiming::wordIndex));
        int wordCount = text.split("\\s+").length;
        List<LyricsCue.WordTiming> bounded = timings.stream()
                .filter(timing -> timing.wordIndex() < wordCount)
                .toList();
        return new ParsedWords(text, bounded);
    }

    private static int countWords(StringBuilder text) {
        String value = text.toString().strip();
        return value.isEmpty() ? 0 : value.split("\\s+").length;
    }

    private static long parseTime(String minutesText, String secondsText, String fraction) {
        try {
            long minutes = Long.parseLong(minutesText);
            long seconds = Long.parseLong(secondsText);
            if (seconds >= 60) return -1L;
            long millis = 0L;
            if (fraction != null) {
                millis = switch (fraction.length()) {
                    case 1 -> Long.parseLong(fraction) * 100L;
                    case 2 -> Long.parseLong(fraction) * 10L;
                    default -> Long.parseLong(fraction.substring(0, 3));
                };
            }
            return minutes * 60_000L + seconds * 1_000L + millis;
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }

    private record ParsedWords(String text, List<LyricsCue.WordTiming> timings) { }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    public String title() { return title; }
    public String artist() { return artist; }
    public Path source() { return source; }
    public long offsetMs() { return offsetMs; }
    public List<LyricsCue> cues() { return cues; }
    public long durationMs() { return cues.get(cues.size() - 1).timeMs() + 5_000L; }
}
