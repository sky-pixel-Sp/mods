from __future__ import annotations
import tkinter as tk
from tkinter import ttk, messagebox, filedialog
from datetime import datetime
from pathlib import Path
from skydesk_core import DataStore, PRIORITIES

BG, PANEL, PANEL2 = "#0B1020", "#121C31", "#17243B"
TEXT, MUTED, ACCENT, BLUE = "#F1F5FB", "#93A4BE", "#72E0C0", "#82A9FF"
FONT = "Segoe UI"

class SkyDeskApp:
    def __init__(self, root: tk.Tk):
        self.root = root
        self.store = DataStore()
        self.selected_note = None
        self.note_job = None
        self.timer_job = None
        self.timer_running = False
        self.timer_kind = "Fokus"
        self.timer_total = 25 * 60
        self.timer_remaining = self.timer_total
        self.root.title("SkyDesk — Ruang Kerja Pribadi")
        self.root.geometry("1120x740")
        self.root.minsize(900, 620)
        self.root.configure(bg=BG)
        self.root.option_add("*Font", (FONT, 10))
        self._style()
        self._build()
        self.refresh_all()
        self.root.protocol("WM_DELETE_WINDOW", self.close)

    def _style(self):
        s = ttk.Style()
        try: s.theme_use("clam")
        except tk.TclError: pass
        s.configure("TFrame", background=BG)
        s.configure("Panel.TFrame", background=PANEL)
        s.configure("TLabel", background=BG, foreground=TEXT, font=(FONT, 10))
        s.configure("Muted.TLabel", background=BG, foreground=MUTED)
        s.configure("Title.TLabel", background=BG, foreground=TEXT, font=(FONT, 22, "bold"))
        s.configure("TButton", background=PANEL2, foreground=TEXT, borderwidth=0, padding=(12, 8))
        s.map("TButton", background=[("active", "#263957")], foreground=[("active", TEXT)])
        s.configure("Accent.TButton", background=ACCENT, foreground=BG, font=(FONT, 10, "bold"))
        s.map("Accent.TButton", background=[("active", "#95F1D8")])
        s.configure("TEntry", fieldbackground=PANEL2, foreground=TEXT, insertcolor=TEXT, padding=8)
        s.configure("TCombobox", fieldbackground=PANEL2, background=PANEL2, foreground=TEXT, padding=7)
        s.map("TCombobox", fieldbackground=[("readonly", PANEL2)], foreground=[("readonly", TEXT)])
        s.configure("TNotebook", background=BG, borderwidth=0, tabmargins=(0, 5, 0, 0))
        s.configure("TNotebook.Tab", background=PANEL, foreground=MUTED, padding=(18, 10), borderwidth=0)
        s.map("TNotebook.Tab", background=[("selected", ACCENT)], foreground=[("selected", BG)])
        s.configure("Treeview", background=PANEL, fieldbackground=PANEL, foreground=TEXT, rowheight=32, borderwidth=0)
        s.configure("Treeview.Heading", background=PANEL2, foreground=TEXT, font=(FONT, 9, "bold"), padding=8)
        s.map("Treeview", background=[("selected", "#244B49")], foreground=[("selected", TEXT)])

    def _build(self):
        header = ttk.Frame(self.root, padding=(26, 22, 26, 15))
        header.pack(fill="x")
        ttk.Label(header, text="SKYDESK", foreground=ACCENT, font=(FONT, 11, "bold")).pack(anchor="w")
        ttk.Label(header, text="Ruang kerja pribadi, lebih teratur.", style="Title.TLabel").pack(anchor="w", pady=(4, 2))
        self.date_label = ttk.Label(header, text="", style="Muted.TLabel")
        self.date_label.pack(anchor="w")
        self.tabs = ttk.Notebook(self.root)
        self.tabs.pack(fill="both", expand=True, padx=26, pady=(0, 22))
        self.home = ttk.Frame(self.tabs, padding=18)
        self.task_page = ttk.Frame(self.tabs, padding=18)
        self.note_page = ttk.Frame(self.tabs, padding=18)
        self.focus_page = ttk.Frame(self.tabs, padding=18)
        self.settings_page = ttk.Frame(self.tabs, padding=18)
        for frame, title in [(self.home,"Beranda"),(self.task_page,"Tugas"),(self.note_page,"Catatan"),(self.focus_page,"Fokus"),(self.settings_page,"Pengaturan & backup")]:
            self.tabs.add(frame, text=title)
        self._build_home()
        self._build_tasks()
        self._build_notes()
        self._build_focus()
        self._build_settings()

    def panel(self, parent, title, hint=""):
        outer = ttk.Frame(parent, style="Panel.TFrame", padding=16)
        ttk.Label(outer, text=title, background=PANEL, foreground=TEXT, font=(FONT, 11, "bold")).pack(anchor="w")
        if hint:
            ttk.Label(outer, text=hint, background=PANEL, foreground=MUTED).pack(anchor="w", pady=(4, 0))
        return outer

    def _build_home(self):
        ttk.Label(self.home, text="Ringkasan aktivitas", font=(FONT, 15, "bold")).pack(anchor="w", pady=(0, 14))
        row = ttk.Frame(self.home); row.pack(fill="x")
        self.stats = {}
        for key, title in [("open","Tugas aktif"),("done","Selesai"),("notes","Catatan"),("focus","Menit fokus")]:
            box = self.panel(row, title)
            box.pack(side="left", fill="both", expand=True, padx=(0, 10))
            self.stats[key] = ttk.Label(box, text="0", background=PANEL, foreground=ACCENT, font=(FONT, 27, "bold"))
            self.stats[key].pack(anchor="w", pady=(10, 2))
        welcome = self.panel(self.home, "Selamat datang di SkyDesk", "Data tersimpan lokal di laptop ini. Tidak perlu akun atau koneksi internet.")
        welcome.pack(fill="x", pady=16)
        ttk.Label(welcome, text="Atur tugas, simpan ide, lalu gunakan timer fokus untuk menjaga ritme kerja.", background=PANEL, foreground=MUTED, wraplength=800).pack(anchor="w", pady=(12, 0))
        self.recent_box = self.panel(self.home, "Tugas berikutnya")
        self.recent_box.pack(fill="both", expand=True)

    def _build_tasks(self):
        top = ttk.Frame(self.task_page); top.pack(fill="x", pady=(0, 12))
        self.task_input = ttk.Entry(top); self.task_input.pack(side="left", fill="x", expand=True, padx=(0, 8))
        self.task_input.insert(0, "")
        self.priority = tk.StringVar(value="Sedang")
        ttk.Combobox(top, textvariable=self.priority, values=PRIORITIES, state="readonly", width=10).pack(side="left", padx=(0, 8))
        ttk.Button(top, text="＋ Tambah tugas", style="Accent.TButton", command=self.add_task).pack(side="left")
        self.task_input.bind("<Return>", lambda _e: self.add_task())
        tools = ttk.Frame(self.task_page); tools.pack(fill="x", pady=(0, 8))
        ttk.Button(tools, text="Tandai selesai / aktif", command=self.toggle_task).pack(side="left", padx=(0, 8))
        ttk.Button(tools, text="Hapus tugas", command=self.delete_task).pack(side="left")
        self.task_tree = ttk.Treeview(self.task_page, columns=("status","priority","text"), show="headings", selectmode="browse")
        for col, title, width in [("status","Status",100),("priority","Prioritas",120),("text","Tugas",600)]:
            self.task_tree.heading(col, text=title); self.task_tree.column(col, width=width, anchor="w")
        self.task_tree.pack(fill="both", expand=True)

    def _build_notes(self):
        left = ttk.Frame(self.note_page); left.pack(side="left", fill="y", padx=(0, 12))
        ttk.Button(left, text="＋ Catatan baru", style="Accent.TButton", command=self.add_note).pack(fill="x", pady=(0, 10))
        ttk.Button(left, text="Hapus catatan", command=self.delete_note).pack(fill="x")
        self.note_list = tk.Listbox(left, width=25, bg=PANEL, fg=TEXT, selectbackground="#244B49", selectforeground=TEXT,
                                    highlightthickness=0, relief="flat", font=(FONT, 10), activestyle="none")
        self.note_list.pack(fill="both", expand=True, pady=(10, 0))
        self.note_list.bind("<<ListboxSelect>>", self.select_note)
        editor = ttk.Frame(self.note_page); editor.pack(side="left", fill="both", expand=True)
        ttk.Label(editor, text="Judul catatan", font=(FONT, 10, "bold")).pack(anchor="w")
        self.note_title = ttk.Entry(editor); self.note_title.pack(fill="x", pady=(5, 12))
        self.note_title.bind("<KeyRelease>", self.schedule_note_save)
        ttk.Label(editor, text="Isi catatan", font=(FONT, 10, "bold")).pack(anchor="w")
        self.note_body = tk.Text(editor, wrap="word", bg=PANEL, fg=TEXT, insertbackground=TEXT, selectbackground="#244B49",
                                 relief="flat", padx=12, pady=12, undo=True, font=(FONT, 10))
        self.note_body.pack(fill="both", expand=True, pady=(5, 0))
        self.note_body.bind("<KeyRelease>", self.schedule_note_save)
        self.note_status = ttk.Label(editor, text="Perubahan disimpan otomatis", style="Muted.TLabel")
        self.note_status.pack(anchor="w", pady=(7, 0))

    def _build_focus(self):
        box = self.panel(self.focus_page, "Ruang fokus", "Pilih durasi, mulai sesi, dan selesaikan tanpa berpindah aplikasi.")
        box.pack(fill="both", expand=True, padx=65, pady=25)
        self.timer_label = ttk.Label(box, text="25:00", background=PANEL, foreground=ACCENT, font=(FONT, 52, "bold"))
        self.timer_label.pack(pady=(28, 8))
        self.timer_mode_var = tk.StringVar(value="Fokus 25 menit")
        ttk.Combobox(box, textvariable=self.timer_mode_var, values=("Fokus 25 menit","Fokus 50 menit","Istirahat 5 menit","Istirahat 15 menit"),
                     state="readonly", width=25).pack(pady=8)
        self.timer_mode_var.trace_add("write", lambda *_: self.select_timer_mode())
        buttons = ttk.Frame(box, style="Panel.TFrame"); buttons.pack(pady=12)
        ttk.Button(buttons, text="Mulai / jeda", style="Accent.TButton", command=self.toggle_timer).pack(side="left", padx=5)
        ttk.Button(buttons, text="Atur ulang", command=self.reset_timer).pack(side="left", padx=5)
        self.timer_status = ttk.Label(box, text="Siap untuk fokus?", background=PANEL, foreground=MUTED)
        self.timer_status.pack(pady=8)

    def _build_settings(self):
        box = self.panel(self.settings_page, "Privasi & cadangan data", "File data utama tersimpan di %APPDATA%\\SkyDesk\\data.json pada Windows.")
        box.pack(fill="x", pady=(0, 14))
        ttk.Button(box, text="Ekspor backup JSON", style="Accent.TButton", command=self.export_backup).pack(anchor="w", pady=(14, 6))
        ttk.Button(box, text="Impor backup JSON", command=self.import_backup).pack(anchor="w")
        ttk.Label(self.settings_page, text="Timer berjalan selama aplikasi terbuka. Backup sebaiknya dibuat berkala.", style="Muted.TLabel").pack(anchor="w")

    def refresh_all(self):
        now = datetime.now()
        days = ["Senin","Selasa","Rabu","Kamis","Jumat","Sabtu","Minggu"]
        months = ["Januari","Februari","Maret","April","Mei","Juni","Juli","Agustus","September","Oktober","November","Desember"]
        self.date_label.configure(text=f"{days[now.weekday()]}, {now.day} {months[now.month-1]} {now.year}")
        self.refresh_tasks(); self.refresh_notes(); self.refresh_stats()

    def refresh_stats(self):
        tasks = self.store.data["tasks"]
        self.stats["open"].configure(text=str(sum(not t["done"] for t in tasks)))
        self.stats["done"].configure(text=str(sum(t["done"] for t in tasks)))
        self.stats["notes"].configure(text=str(len(self.store.data["notes"])))
        self.stats["focus"].configure(text=str(self.store.total_focus_minutes()))
        for child in self.recent_box.winfo_children()[1:]: child.destroy()
        pending = [t for t in tasks if not t["done"]][:5]
        if not pending:
            ttk.Label(self.recent_box, text="Belum ada tugas aktif. Tambahkan tugas pertamamu di tab Tugas.", background=PANEL, foreground=MUTED).pack(anchor="w", pady=(12, 0))
        else:
            for t in pending:
                ttk.Label(self.recent_box, text=f"•  [{t['priority']}]  {t['text']}", background=PANEL, foreground=TEXT).pack(anchor="w", pady=4)

    def refresh_tasks(self):
        self.task_tree.delete(*self.task_tree.get_children())
        for t in self.store.data["tasks"]:
            self.task_tree.insert("", "end", iid=t["id"], values=("✓ Selesai" if t["done"] else "○ Aktif", t["priority"], t["text"]))

    def add_task(self):
        try: self.store.add_task(self.task_input.get(), self.priority.get())
        except ValueError: messagebox.showwarning("Tugas kosong", "Tulis isi tugas terlebih dahulu.", parent=self.root); return
        self.task_input.delete(0, "end"); self.refresh_all()

    def toggle_task(self):
        selected = self.task_tree.selection()
        if not selected: return
        item = next((t for t in self.store.data["tasks"] if t["id"] == selected[0]), None)
        if item: self.store.toggle_task(item["id"], not item["done"]); self.refresh_all()

    def delete_task(self):
        selected = self.task_tree.selection()
        if selected and messagebox.askyesno("Hapus tugas", "Hapus tugas yang dipilih?", parent=self.root):
            self.store.delete_task(selected[0]); self.refresh_all()

    def refresh_notes(self):
        current = self.selected_note
        self.note_list.delete(0, "end")
        for item in self.store.data["notes"]: self.note_list.insert("end", item["title"][:27])
        if current:
            for i, n in enumerate(self.store.data["notes"]):
                if n["id"] == current: self.note_list.selection_set(i); break

    def _flush_note(self):
        if self.note_job:
            try: self.root.after_cancel(self.note_job)
            except tk.TclError: pass
            self.note_job = None
        if not self.selected_note: return
        title = self.note_title.get()
        body = self.note_body.get("1.0", "end-1c")
        self.store.update_note(self.selected_note, title, body)
        self.note_status.configure(text="Tersimpan secara lokal")

    def schedule_note_save(self, _event=None):
        self.note_status.configure(text="Menyimpan…")
        if self.note_job:
            try: self.root.after_cancel(self.note_job)
            except tk.TclError: pass
        self.note_job = self.root.after(400, self._flush_note)

    def select_note(self, _event=None):
        selected = self.note_list.curselection()
        if not selected: return
        self._flush_note()
        item = self.store.data["notes"][selected[0]]
        self.selected_note = item["id"]
        self.note_title.delete(0, "end"); self.note_title.insert(0, item["title"])
        self.note_body.delete("1.0", "end"); self.note_body.insert("1.0", item["body"])
        self.note_status.configure(text="Perubahan disimpan otomatis")

    def add_note(self):
        self._flush_note()
        item = self.store.add_note()
        self.selected_note = item["id"]; self.refresh_notes()
        index = next(i for i,n in enumerate(self.store.data["notes"]) if n["id"] == item["id"])
        self.note_list.selection_clear(0, "end"); self.note_list.selection_set(index)
        self.note_title.delete(0, "end"); self.note_title.insert(0, item["title"])
        self.note_body.delete("1.0", "end"); self.note_body.focus_set()

    def delete_note(self):
        selected = self.note_list.curselection()
        if not selected: return
        item = self.store.data["notes"][selected[0]]
        if messagebox.askyesno("Hapus catatan", f'Hapus "{item["title"]}"?', parent=self.root):
            self._flush_note(); self.store.delete_note(item["id"]); self.selected_note = None
            self.note_title.delete(0, "end"); self.note_body.delete("1.0", "end"); self.refresh_all()

    def select_timer_mode(self):
        if self.timer_running: return
        mapping = {"Fokus 25 menit":("Fokus",25),"Fokus 50 menit":("Fokus",50),
                   "Istirahat 5 menit":("Istirahat",5),"Istirahat 15 menit":("Istirahat",15)}
        self.timer_kind, minutes = mapping.get(self.timer_mode_var.get(), ("Fokus",25))
        self.timer_total = minutes*60; self.timer_remaining = self.timer_total; self.update_timer_label()

    def update_timer_label(self):
        self.timer_label.configure(text=f"{self.timer_remaining//60:02d}:{self.timer_remaining%60:02d}")

    def toggle_timer(self):
        if self.timer_running:
            self.timer_running = False
            if self.timer_job:
                self.root.after_cancel(self.timer_job); self.timer_job = None
            self.timer_status.configure(text="Timer dijeda"); return
        self.timer_running = True; self.timer_status.configure(text=f"{self.timer_kind} sedang berjalan")
        self.tick_timer()

    def tick_timer(self):
        if not self.timer_running: return
        if self.timer_remaining <= 0:
            self.timer_running = False; self.timer_job = None
            if self.timer_kind == "Fokus":
                self.store.record_focus_session(self.timer_total//60); self.refresh_stats()
            self.root.bell(); self.timer_status.configure(text=f"{self.timer_kind} selesai")
            messagebox.showinfo("SkyDesk Fokus", f"{self.timer_kind} selesai.", parent=self.root); return
        self.timer_remaining -= 1; self.update_timer_label()
        self.timer_job = self.root.after(1000, self.tick_timer)

    def reset_timer(self):
        self.timer_running = False
        if self.timer_job:
            try: self.root.after_cancel(self.timer_job)
            except tk.TclError: pass
        self.timer_job = None; self.select_timer_mode()
        self.timer_status.configure(text="Timer siap")

    def export_backup(self):
        path = filedialog.asksaveasfilename(parent=self.root, title="Simpan backup", defaultextension=".json",
                                            filetypes=[("JSON", "*.json")], initialfile="skydesk-backup.json")
        if not path: return
        self._flush_note()
        try: self.store.export_to(path); messagebox.showinfo("Backup", "Backup berhasil disimpan.", parent=self.root)
        except OSError as exc: messagebox.showerror("Backup gagal", str(exc), parent=self.root)

    def import_backup(self):
        path = filedialog.askopenfilename(parent=self.root, title="Pilih backup", filetypes=[("JSON", "*.json")])
        if not path: return
        if not messagebox.askyesno("Impor backup", "Data sekarang akan diganti oleh isi backup. Lanjutkan?", parent=self.root): return
        try:
            self._flush_note(); self.store.import_from(path); self.selected_note = None
            self.note_title.delete(0, "end"); self.note_body.delete("1.0", "end"); self.refresh_all()
            messagebox.showinfo("Backup", "Backup berhasil diimpor.", parent=self.root)
        except (OSError, ValueError, TypeError) as exc: messagebox.showerror("Backup tidak valid", str(exc), parent=self.root)

    def close(self):
        self._flush_note()
        if self.timer_job:
            try: self.root.after_cancel(self.timer_job)
            except tk.TclError: pass
        self.root.destroy()

if __name__ == "__main__":
    root = tk.Tk()
    SkyDeskApp(root)
    root.mainloop()
