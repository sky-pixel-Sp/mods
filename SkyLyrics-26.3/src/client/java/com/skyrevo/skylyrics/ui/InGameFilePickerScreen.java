package com.skyrevo.skylyrics.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * An in-game file browser for importing local audio and LRC files.
 * It never opens an operating-system file dialog or an external media player.
 */
public final class InGameFilePickerScreen extends Screen {
    private static final int PAGE_SIZE = 8;
    private static final int MAX_ENTRIES = 2_000;
    private static Path lastDirectory;

    private final Screen parent;
    private final boolean lyricsOnly;
    private final Consumer<Path> callback;
    private Path directory;
    private List<Path> entries = List.of();
    private String error = "";
    private int page;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public InGameFilePickerScreen(Screen parent, boolean lyricsOnly, Consumer<Path> callback, Path initialDirectory) {
        this(parent, lyricsOnly, callback, initialDirectory, 0);
    }

    private InGameFilePickerScreen(Screen parent, boolean lyricsOnly, Consumer<Path> callback,
                                   Path initialDirectory, int initialPage) {
        super(Component.literal(lyricsOnly ? "Import Lyrics" : "Import Music"));
        this.parent = parent;
        this.lyricsOnly = lyricsOnly;
        this.callback = callback;
        Path candidate = initialDirectory == null ? defaultDirectory() : initialDirectory;
        this.directory = Files.isDirectory(candidate) ? candidate.toAbsolutePath().normalize() : defaultDirectory();
        loadEntries();
        this.page = Math.max(0, Math.min(initialPage, Math.max(0, (entries.size() - 1) / PAGE_SIZE)));
    }

    public static Path defaultDirectory() {
        if (lastDirectory != null && Files.isDirectory(lastDirectory)) return lastDirectory;
        String home = System.getProperty("user.home", ".");
        Path path = Path.of(home).toAbsolutePath().normalize();
        return Files.isDirectory(path) ? path : Path.of(".").toAbsolutePath().normalize();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(720, Math.max(300, width - 24));
        panelHeight = Math.min(470, Math.max(300, height - 24));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;

        int gap = 6;
        int shortcutWidth = Math.max(52, (panelWidth - 36 - gap * 4) / 5);
        int sx = left + 18;
        int sy = top + 62;

        addButton("HOME", sx, sy, shortcutWidth, 24, () -> navigate(defaultDirectory()));
        addButton("MUSIC", sx + shortcutWidth + gap, sy, shortcutWidth, 24,
                () -> navigate(homeFolder("Music")));
        addButton("DOWNLOADS", sx + (shortcutWidth + gap) * 2, sy, shortcutWidth, 24,
                () -> navigate(homeFolder("Downloads")));
        addButton("DESKTOP", sx + (shortcutWidth + gap) * 3, sy, shortcutWidth, 24,
                () -> navigate(homeFolder("Desktop")));
        addButton("UP  ↑", sx + (shortcutWidth + gap) * 4, sy, shortcutWidth, 24,
                () -> navigate(directory.getParent()));

        int rowY = top + 112;
        int rowWidth = panelWidth - 36;
        int start = page * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        if (entries.isEmpty()) {
            Button empty = Button.builder(Component.literal(error.isBlank()
                    ? (lyricsOnly ? "No .LRC files in this folder" : "No supported audio files in this folder")
                    : error), b -> { })
                    .bounds(left + 18, rowY, rowWidth, 30).build();
            empty.active = false;
            addRenderableWidget(empty);
        } else {
            for (int i = start; i < end; i++) {
                Path entry = entries.get(i);
                boolean folder = Files.isDirectory(entry);
                String name = entry.getFileName() == null ? entry.toString() : entry.getFileName().toString();
                String prefix = folder ? "FOLDER   " : (lyricsOnly ? "LYRICS   " : "AUDIO   ");
                String label = prefix + clip(name, 56);
                int y = rowY + (i - start) * 34;
                addButton(label, left + 18, y, rowWidth, 29, () -> {
                    if (Files.isDirectory(entry)) navigate(entry);
                    else select(entry);
                });
            }
        }

        int footerY = top + panelHeight - 42;
        int half = (panelWidth - 42) / 2;
        Button previous = addButton("← PREVIOUS", left + 18, footerY, half, 24,
                () -> changePage(-1));
        previous.active = page > 0;
        Button next = addButton("NEXT →", left + 24 + half, footerY, half, 24,
                () -> changePage(1));
        next.active = (page + 1) * PAGE_SIZE < entries.size();
    }

    private Button addButton(String label, int x, int y, int w, int h, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
                .bounds(x, y, w, h).build());
    }

    private void navigate(Path target) {
        if (target == null || !Files.isDirectory(target)) return;
        lastDirectory = target.toAbsolutePath().normalize();
        Minecraft.getInstance().setScreen(
                new InGameFilePickerScreen(parent, lyricsOnly, callback, lastDirectory));
    }

    private void changePage(int delta) {
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int nextPage = Math.max(0, Math.min(pages - 1, page + delta));
        Minecraft.getInstance().setScreen(
                new InGameFilePickerScreen(parent, lyricsOnly, callback, directory, nextPage));
    }

    private void loadEntries() {
        error = "";
        try (Stream<Path> stream = Files.list(directory)) {
            entries = stream
                    .filter(Files::isReadable)
                    .filter(path -> Files.isDirectory(path) || isSupported(path))
                    .sorted(Comparator
                            .comparing((Path path) -> !Files.isDirectory(path))
                            .thenComparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .limit(MAX_ENTRIES)
                    .toList();
        } catch (IOException | SecurityException exception) {
            entries = List.of();
            error = "Cannot read this folder";
        }
    }

    private boolean isSupported(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (lyricsOnly) return name.endsWith(".lrc");
        return name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav")
                || name.endsWith(".aif") || name.endsWith(".aiff") || name.endsWith(".au");
    }

    private static Path homeFolder(String child) {
        Path home = defaultDirectory();
        Path candidate = home.resolve(child);
        return Files.isDirectory(candidate) ? candidate : home;
    }

    private void select(Path file) {
        if (!Files.isRegularFile(file) || !Files.isReadable(file)) {
            error = "This file cannot be read";
            loadEntries();
            return;
        }
        lastDirectory = file.getParent();
        Minecraft.getInstance().setScreen(parent);
        if (callback != null) callback.accept(file.toAbsolutePath().normalize());
    }

    private static String clip(String value, int max) {
        if (value == null || value.length() <= max) return value == null ? "" : value;
        return value.substring(0, Math.max(1, max - 1)) + "…";
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, width, height, 0xB8090D12);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0151A21);
        graphics.outline(left, top, panelWidth, panelHeight, 0xFF46515E);
        graphics.fill(left, top, left + panelWidth, top + 3, 0xFF9DAFC2);
        graphics.fill(left + 12, top + 96, left + panelWidth - 12, top + 97, 0xFF303A46);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.text(font, lyricsOnly ? "IMPORT LYRICS" : "IMPORT MUSIC", left + 18, top + 18,
                0xFFF1F4F7, true);
        graphics.text(font, "IN-GAME FILE BROWSER  ·  NO EXTERNAL PLAYER",
                left + 18, top + 37, 0xFFAAB6C3, false);
        String path = directory.toString();
        graphics.text(font, clip(path, Math.max(24, panelWidth / 7)), left + 18, top + 98,
                0xFFD2DCE6, false);
        int pages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        graphics.text(font, (entries.size() + (entries.size() >= MAX_ENTRIES ? "+" : ""))
                        + " items  ·  " + (page + 1) + " / " + pages,
                left + 18, top + panelHeight - 58, 0xFF9EABB8, false);
        graphics.text(font, lyricsOnly
                        ? "Choose an .LRC file. It will be copied into the SkyLyrics library."
                        : "Choose a song. It will be copied into config/skylyrics/music and played in-game.",
                left + 18, top + panelHeight - 77, 0xFF9EABB8, false);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.isEscape()) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}
