from __future__ import annotations
import math
import os
import random
import sys
import tkinter as tk
from pathlib import Path
from tkinter import messagebox

from game_engine import clamp, collides, enemy_profile, load_high_score, save_high_score, wave_for_time

APP_NAME = "NEON RIFT"
BG = "#060914"
CYAN = "#5CF2E8"
PINK = "#FF4FA3"
PURPLE = "#9C78FF"
WHITE = "#E8F5FF"
MUTED = "#8293B4"
GOLD = "#FFD166"
GREEN = "#76FF9F"

def data_file() -> Path:
    root = Path(os.environ.get("APPDATA", Path.home()))
    return root / "NeonRift" / "profile.json"

class NeonRift:
    def __init__(self, root: tk.Tk):
        self.root = root
        self.root.title("NEON RIFT — Survival Protocol")
        self.root.geometry("1100x760")
        self.root.minsize(820, 600)
        self.root.configure(bg=BG)
        self.canvas = tk.Canvas(root, bg=BG, highlightthickness=0, cursor="crosshair")
        self.canvas.pack(fill="both", expand=True)
        self.profile_path = data_file()
        self.high_score = load_high_score(self.profile_path)
        self.keys: set[str] = set()
        self.mouse = [550.0, 380.0]
        self.mouse_fire = False
        self.state = "menu"
        self.last_tick = 0.0
        self.elapsed = 0.0
        self.score = 0
        self.kills = 0
        self.wave = 1
        self.spawn_clock = 0.0
        self.boss_waves: set[int] = set()
        self.enemies: list[dict] = []
        self.bullets: list[dict] = []
        self.enemy_bullets: list[dict] = []
        self.pickups: list[dict] = []
        self.particles: list[dict] = []
        self.stars = []
        self.player = {}
        self.toast = ""
        self.toast_time = 0.0
        self.canvas.bind("<Configure>", self.on_resize)
        self.root.bind("<KeyPress>", self.key_down)
        self.root.bind("<KeyRelease>", self.key_up)
        self.canvas.bind("<Motion>", self.mouse_move)
        self.canvas.bind("<ButtonPress-1>", self.mouse_down)
        self.canvas.bind("<ButtonRelease-1>", self.mouse_up)
        self.canvas.bind("<ButtonPress-3>", self.mouse_down)
        self.make_stars()
        self.show_menu()
        self.root.after(16, self.loop)

    def size(self):
        return max(820, self.canvas.winfo_width()), max(600, self.canvas.winfo_height())

    def on_resize(self, _event=None):
        if self.state in ("menu", "gameover", "paused"):
            self.draw_overlay()

    def make_stars(self):
        w, h = 1100, 760
        for _ in range(105):
            self.stars.append({"x": random.uniform(0,w), "y": random.uniform(0,h),
                               "r": random.choice((1,1,1,2)), "speed": random.uniform(12,50),
                               "phase": random.random() * math.tau})

    def key_down(self, event):
        key = event.keysym.lower()
        self.keys.add(key)
        if key in ("return", "kp_enter") and self.state in ("menu", "gameover"):
            self.start_game()
        elif key == "escape":
            if self.state == "playing":
                self.state = "paused"
                self.draw_overlay()
            elif self.state == "paused":
                self.state = "playing"
                self.last_tick = 0.0
        elif key == "r" and self.state == "gameover":
            self.start_game()
        elif key == "space" and self.state == "playing":
            self.keys.add("shoot")

    def key_up(self, event):
        key = event.keysym.lower()
        self.keys.discard(key)
        if key == "space":
            self.keys.discard("shoot")

    def mouse_move(self, event):
        self.mouse = [event.x, event.y]

    def mouse_down(self, event):
        self.mouse = [event.x, event.y]
        if self.state in ("menu", "gameover"):
            self.start_game()
        elif self.state == "paused":
            self.state = "playing"
            self.last_tick = 0.0
        else:
            self.mouse_fire = True

    def mouse_up(self, _event):
        self.mouse_fire = False

    def start_game(self):
        w, h = self.size()
        self.state = "playing"
        self.elapsed = 0.0
        self.score = 0
        self.kills = 0
        self.wave = 1
        self.spawn_clock = 0.0
        self.boss_waves = set()
        self.enemies.clear()
        self.bullets.clear()
        self.enemy_bullets.clear()
        self.pickups.clear()
        self.particles.clear()
        self.keys.clear()
        self.mouse_fire = False
        self.player = {"x": w * 0.5, "y": h * 0.68, "r": 14, "hp": 100, "max_hp": 100,
                       "speed": 300, "shot": 0.0, "rapid": 0.0, "shield": 0.0,
                       "invuln": 0.0, "aim": -math.pi/2}
        self.last_tick = 0.0
        self.toast = "SYSTEM ONLINE"
        self.toast_time = 1.4

    def show_menu(self):
        self.state = "menu"
        self.draw_overlay()

    def spawn_enemy(self, kind=None):
        w, h = self.size()
        if kind is None:
            roll = random.random()
            if self.wave >= 4 and roll < 0.12:
                kind = "brute"
            elif self.wave >= 2 and roll < 0.42:
                kind = "hunter"
            else:
                kind = "drone"
        side = random.randrange(4)
        margin = 45
        if side == 0: x, y = random.uniform(0,w), -margin
        elif side == 1: x, y = w+margin, random.uniform(0,h)
        elif side == 2: x, y = random.uniform(0,w), h+margin
        else: x, y = -margin, random.uniform(0,h)
        p = enemy_profile(kind, self.wave)
        self.enemies.append({"x":x,"y":y,"kind":kind,"hp":p["hp"],"max_hp":p["hp"],
                             "speed":p["speed"],"r":p["radius"],"score":p["score"],
                             "damage":p["damage"],"shot":random.uniform(1.1,2.8),
                             "phase":random.random()*math.tau,"boss":kind=="boss"})

    def emit(self, x, y, color, count=8, speed=160):
        for _ in range(count):
            a = random.random() * math.tau
            v = random.uniform(speed * 0.25, speed)
            self.particles.append({"x":x,"y":y,"vx":math.cos(a)*v,"vy":math.sin(a)*v,
                                   "life":random.uniform(.2,.65),"max":.65,"color":color,
                                   "r":random.uniform(1.5,3.8)})

    def toast_msg(self, text):
        self.toast = text
        self.toast_time = 1.65

    def update(self, dt):
        self.elapsed += dt
        new_wave = wave_for_time(self.elapsed)
        if new_wave > self.wave:
            self.wave = new_wave
            self.toast_msg(f"WAVE {self.wave} — THREAT LEVEL RISING")
            if self.wave % 5 == 0 and self.wave not in self.boss_waves:
                self.spawn_enemy("boss")
                self.boss_waves.add(self.wave)
        self.player["rapid"] = max(0.0, self.player["rapid"] - dt)
        self.player["shield"] = max(0.0, self.player["shield"] - dt)
        self.player["invuln"] = max(0.0, self.player["invuln"] - dt)
        self.spawn_clock += dt
        spawn_interval = max(.24, 1.0 - self.wave * .055)
        if self.spawn_clock >= spawn_interval:
            self.spawn_clock = 0.0
            self.spawn_enemy()
        self.update_player(dt)
        self.update_bullets(dt)
        self.update_enemies(dt)
        self.update_pickups(dt)
        self.update_particles(dt)
        if self.toast_time > 0:
            self.toast_time -= dt
        if self.player["hp"] <= 0:
            self.end_game()

    def update_player(self, dt):
        p = self.player
        w, h = self.size()
        dx = (1 if ("d" in self.keys or "right" in self.keys) else 0) - (1 if ("a" in self.keys or "left" in self.keys) else 0)
        dy = (1 if ("s" in self.keys or "down" in self.keys) else 0) - (1 if ("w" in self.keys or "up" in self.keys) else 0)
        length = math.hypot(dx, dy)
        if length:
            p["x"] += dx / length * p["speed"] * dt
            p["y"] += dy / length * p["speed"] * dt
        p["x"] = clamp(p["x"], p["r"]+8, w-p["r"]-8)
        p["y"] = clamp(p["y"], p["r"]+50, h-p["r"]-20)
        target_x, target_y = self.mouse
        if not (0 <= target_x <= w and 0 <= target_y <= h):
            target_x, target_y = p["x"], p["y"]-100
        p["aim"] = math.atan2(target_y-p["y"], target_x-p["x"])
        p["shot"] = max(0.0, p["shot"] - dt)
        fire = self.mouse_fire or "shoot" in self.keys or "space" in self.keys
        if fire and p["shot"] <= 0:
            # Mouse aiming is primary. Space uses nearest-enemy auto aim.
            if not self.mouse_fire and self.enemies:
                nearest = min(self.enemies, key=lambda e:(e["x"]-p["x"])**2+(e["y"]-p["y"])**2)
                p["aim"] = math.atan2(nearest["y"]-p["y"], nearest["x"]-p["x"])
            angle = p["aim"]
            speed = 670
            self.bullets.append({"x":p["x"]+math.cos(angle)*18,"y":p["y"]+math.sin(angle)*18,
                                 "vx":math.cos(angle)*speed,"vy":math.sin(angle)*speed,
                                 "r":4,"life":1.25,"damage":2 if p["rapid"] > 0 else 1,
                                 "color":GOLD if p["rapid"] > 0 else CYAN})
            p["shot"] = .09 if p["rapid"] > 0 else .19

    def update_bullets(self, dt):
        w, h = self.size()
        for b in self.bullets[:]:
            b["x"] += b["vx"]*dt; b["y"] += b["vy"]*dt; b["life"] -= dt
            if b["life"] <= 0 or b["x"] < -20 or b["x"] > w+20 or b["y"] < -20 or b["y"] > h+20:
                self.bullets.remove(b); continue
            for e in self.enemies[:]:
                if collides(b["x"],b["y"],b["r"],e["x"],e["y"],e["r"]):
                    e["hp"] -= b["damage"]
                    self.emit(b["x"],b["y"],b["color"],2,65)
                    if b in self.bullets: self.bullets.remove(b)
                    if e["hp"] <= 0:
                        self.destroy_enemy(e)
                    break
        for b in self.enemy_bullets[:]:
            b["x"] += b["vx"]*dt; b["y"] += b["vy"]*dt; b["life"] -= dt
            if b["life"] <= 0 or b["x"] < -20 or b["x"] > w+20 or b["y"] < -20 or b["y"] > h+20:
                self.enemy_bullets.remove(b); continue
            p = self.player
            if p["invuln"] <= 0 and collides(b["x"],b["y"],b["r"],p["x"],p["y"],p["r"]):
                self.damage_player(12)
                if b in self.enemy_bullets: self.enemy_bullets.remove(b)

    def update_enemies(self, dt):
        p = self.player
        for e in self.enemies[:]:
            angle = math.atan2(p["y"]-e["y"], p["x"]-e["x"])
            sway = math.sin(self.elapsed*2.5 + e["phase"]) * (0.22 if e["kind"]=="drone" else 0.08)
            e["x"] += math.cos(angle+sway)*e["speed"]*dt
            e["y"] += math.sin(angle+sway)*e["speed"]*dt
            if e["kind"] == "boss":
                e["shot"] -= dt
                if e["shot"] <= 0:
                    for offset in (-.45,-.22,0,.22,.45):
                        a = angle + offset
                        self.enemy_bullets.append({"x":e["x"],"y":e["y"],"vx":math.cos(a)*220,
                                                   "vy":math.sin(a)*220,"r":5,"life":3.0})
                    e["shot"] = 1.05
            if p["invuln"] <= 0 and collides(p["x"],p["y"],p["r"],e["x"],e["y"],e["r"]):
                self.damage_player(e["damage"])
                self.emit(e["x"],e["y"],PINK,10,130)
                e["x"] += math.cos(angle)*38
                e["y"] += math.sin(angle)*38
                if e["kind"] != "boss":
                    e["hp"] -= 1
                    if e["hp"] <= 0: self.destroy_enemy(e)

    def destroy_enemy(self, e):
        if e not in self.enemies: return
        self.enemies.remove(e)
        self.score += e["score"]
        self.kills += 1
        self.emit(e["x"],e["y"],PURPLE if e["kind"] in ("brute","boss") else PINK,18 if e["boss"] else 9,210)
        if e["boss"]:
            self.score += 250
            self.pickups.append({"x":e["x"],"y":e["y"],"kind":"heal","r":11,"life":12.0})
            self.toast_msg("BOSS ELIMINATED +250")
        elif random.random() < .14:
            kind = random.choice(("heal","rapid","shield"))
            self.pickups.append({"x":e["x"],"y":e["y"],"kind":kind,"r":10,"life":12.0})

    def damage_player(self, amount):
        p = self.player
        if p["shield"] > 0:
            p["shield"] = max(0, p["shield"]-1.5)
            self.emit(p["x"],p["y"],CYAN,7,100)
            return
        p["hp"] = max(0, p["hp"]-amount)
        p["invuln"] = .7
        self.emit(p["x"],p["y"],PINK,12,160)

    def update_pickups(self, dt):
        p = self.player
        for item in self.pickups[:]:
            item["life"] -= dt
            if item["life"] <= 0:
                self.pickups.remove(item); continue
            if collides(item["x"],item["y"],item["r"],p["x"],p["y"],p["r"]):
                if item["kind"] == "heal":
                    p["hp"] = min(p["max_hp"],p["hp"]+28); self.toast_msg("HULL REPAIRED +28")
                elif item["kind"] == "rapid":
                    p["rapid"] = 7.0; self.toast_msg("OVERDRIVE WEAPONS — 7s")
                else:
                    p["shield"] = 6.0; self.toast_msg("SHIELD ONLINE — 6s")
                self.emit(item["x"],item["y"],GREEN,10,135)
                self.pickups.remove(item)

    def update_particles(self, dt):
        for particle in self.particles[:]:
            particle["x"] += particle["vx"]*dt
            particle["y"] += particle["vy"]*dt
            particle["vx"] *= max(0,1-dt*2.8); particle["vy"] *= max(0,1-dt*2.8)
            particle["life"] -= dt
            if particle["life"] <= 0: self.particles.remove(particle)

    def end_game(self):
        self.state = "gameover"
        self.mouse_fire = False
        if self.score > self.high_score:
            self.high_score = self.score
            try: save_high_score(self.profile_path, self.high_score)
            except OSError: pass
        self.draw_overlay()

    def loop(self):
        try:
            import time
            now = time.perf_counter()
            dt = min(.035, now-self.last_tick) if self.last_tick else .016
            self.last_tick = now
            if self.state == "playing":
                self.update(dt)
                self.draw_game()
            else:
                self.animate_stars(dt)
                if self.state in ("menu","gameover","paused"):
                    self.draw_overlay()
        except Exception as exc:
            self.state = "menu"
            self.canvas.delete("all")
            self.canvas.create_text(40,40,anchor="nw",fill=PINK,text=f"Terjadi kesalahan: {exc}",font=("Segoe UI",14))
        self.root.after(16, self.loop)

    def animate_stars(self, dt):
        w,h = self.size()
        for s in self.stars:
            s["y"] += s["speed"] * dt * .45
            if s["y"] > h: s["y"] = 0; s["x"] = random.random()*w

    def draw_stars(self):
        self.canvas.delete("stars")
        w,h = self.size()
        for s in self.stars:
            x, y = s["x"] % w, s["y"] % h
            color = "#243652" if s["r"] == 1 else "#526A92"
            self.canvas.create_oval(x-s["r"],y-s["r"],x+s["r"],y+s["r"],fill=color,outline="",tags="stars")

    def draw_game(self):
        self.canvas.delete("frame")
        w,h = self.size()
        self.draw_stars()
        # restrained neon grid
        for x in range(0,w,60):
            self.canvas.create_line(x,0,x,h,fill="#0B1428",tags="frame")
        for y in range(0,h,60):
            self.canvas.create_line(0,y,w,y,fill="#0B1428",tags="frame")
        for particle in self.particles:
            r = max(1, particle["r"]*particle["life"]/particle["max"])
            self.canvas.create_oval(particle["x"]-r,particle["y"]-r,particle["x"]+r,particle["y"]+r,
                                    fill=particle["color"],outline="",tags="frame")
        for b in self.bullets:
            self.canvas.create_oval(b["x"]-b["r"]-2,b["y"]-b["r"]-2,b["x"]+b["r"]+2,b["y"]+b["r"]+2,
                                    outline=b["color"],width=2,tags="frame")
            self.canvas.create_oval(b["x"]-b["r"],b["y"]-b["r"],b["x"]+b["r"],b["y"]+b["r"],
                                    fill=b["color"],outline="",tags="frame")
        for b in self.enemy_bullets:
            self.canvas.create_oval(b["x"]-b["r"],b["y"]-b["r"],b["x"]+b["r"],b["y"]+b["r"],
                                    fill=PINK,outline=WHITE,width=1,tags="frame")
        for item in self.pickups:
            colors = {"heal":GREEN,"rapid":GOLD,"shield":CYAN}
            c = colors[item["kind"]]
            r = item["r"]+3*math.sin(self.elapsed*5)
            self.canvas.create_polygon(item["x"],item["y"]-r,item["x"]+r,item["y"],item["x"],item["y"]+r,item["x"]-r,item["y"],
                                       fill="#101A2F",outline=c,width=2,tags="frame")
            symbol = {"heal":"+","rapid":"⚡","shield":"◇"}[item["kind"]]
            self.canvas.create_text(item["x"],item["y"],text=symbol,fill=c,font=("Segoe UI",10,"bold"),tags="frame")
        for e in self.enemies:
            x,y,r = e["x"],e["y"],e["r"]
            color = {"drone":PINK,"hunter":PURPLE,"brute":GOLD,"boss":"#FF355D"}[e["kind"]]
            if e["boss"]:
                self.canvas.create_oval(x-r-7,y-r-7,x+r+7,y+r+7,outline="#552343",width=4,tags="frame")
            if e["kind"] == "drone":
                self.canvas.create_polygon(x,y-r-3,x+r+4,y,x,y+r+3,x-r-4,y,fill="#25132B",outline=color,width=2,tags="frame")
            elif e["kind"] == "hunter":
                self.canvas.create_polygon(x,y-r-3,x+r+3,y-r*.35,x+r,y+r*.7,x,y+r+3,x-r,y+r*.7,x-r-3,y-r*.35,
                                           fill="#19162F",outline=color,width=2,tags="frame")
            else:
                self.canvas.create_rectangle(x-r,y-r,x+r,y+r,fill="#28192A",outline=color,width=2,tags="frame")
                self.canvas.create_line(x-r*.6,y,x+r*.6,y,fill=color,width=2,tags="frame")
            if e["kind"] != "drone":
                bw = r*2
                self.canvas.create_rectangle(x-bw/2,y-r-10,x+bw/2,y-r-6,fill="#38213B",outline="",tags="frame")
                self.canvas.create_rectangle(x-bw/2,y-r-10,x-bw/2+bw*max(0,e["hp"])/e["max_hp"],y-r-6,fill=color,outline="",tags="frame")
        self.draw_player()
        self.draw_hud(w,h)

    def draw_player(self):
        p=self.player; x,y,r=p["x"],p["y"],p["r"]; a=p["aim"]
        if p["invuln"] > 0 and int(self.elapsed*18)%2: return
        tip=(x+math.cos(a)*r*1.7,y+math.sin(a)*r*1.7)
        left=(x+math.cos(a+2.45)*r,y+math.sin(a+2.45)*r)
        right=(x+math.cos(a-2.45)*r,y+math.sin(a-2.45)*r)
        if p["shield"] > 0:
            self.canvas.create_oval(x-r-9,y-r-9,x+r+9,y+r+9,outline=CYAN,width=3,tags="frame")
        self.canvas.create_polygon(tip[0],tip[1],left[0],left[1],x-math.cos(a)*r*.5,y-math.sin(a)*r*.5,right[0],right[1],
                                   fill="#102F3A",outline=CYAN,width=2,tags="frame")
        self.canvas.create_oval(x-4,y-4,x+4,y+4,fill=WHITE,outline="",tags="frame")
        if p["rapid"]>0:
            self.canvas.create_oval(x-r-5,y-r-5,x+r+5,y+r+5,outline=GOLD,width=2,tags="frame")

    def draw_hud(self,w,h):
        p=self.player
        self.canvas.create_rectangle(15,14,300,99,fill="#0B1224",outline="#23324D",width=1,tags="frame")
        self.canvas.create_text(30,27,anchor="nw",text="NEON RIFT  /  SURVIVAL PROTOCOL",fill=CYAN,font=("Segoe UI",9,"bold"),tags="frame")
        self.canvas.create_text(30,48,anchor="nw",text=f"SCORE  {self.score:07d}",fill=WHITE,font=("Consolas",17,"bold"),tags="frame")
        self.canvas.create_text(30,76,anchor="nw",text=f"WAVE {self.wave}     KILLS {self.kills}",fill=MUTED,font=("Consolas",10),tags="frame")
        self.canvas.create_rectangle(w-240,18,w-18,74,fill="#0B1224",outline="#23324D",tags="frame")
        self.canvas.create_text(w-226,27,anchor="nw",text="HULL INTEGRITY",fill=MUTED,font=("Segoe UI",9,"bold"),tags="frame")
        self.canvas.create_rectangle(w-226,48,w-34,61,fill="#291A31",outline="",tags="frame")
        hpw=192*max(0,p["hp"])/p["max_hp"]
        col=GREEN if p["hp"]>60 else GOLD if p["hp"]>30 else PINK
        self.canvas.create_rectangle(w-226,48,w-226+hpw,61,fill=col,outline="",tags="frame")
        self.canvas.create_text(w-226,64,anchor="nw",text=f"{int(p['hp'])}/100",fill=WHITE,font=("Consolas",9),tags="frame")
        if self.toast_time>0:
            self.canvas.create_text(w/2,34,text=self.toast,fill=GOLD,font=("Segoe UI",12,"bold"),tags="frame")
        effects=[]
        if p["shield"]>0: effects.append(f"SHIELD {p['shield']:.0f}s")
        if p["rapid"]>0: effects.append(f"OVERDRIVE {p['rapid']:.0f}s")
        if effects:
            self.canvas.create_text(w/2,h-30,text="   //   ".join(effects),fill=CYAN,font=("Consolas",11,"bold"),tags="frame")
        self.canvas.create_text(18,h-16,anchor="sw",text="WASD / ARROWS MOVE     •     CLICK OR HOLD SPACE TO FIRE     •     ESC PAUSE",
                                fill="#536785",font=("Segoe UI",9),tags="frame")

    def draw_overlay(self):
        self.canvas.delete("frame")
        w,h=self.size()
        self.draw_stars()
        # abstract orbital rings
        cx,cy=w*.5,h*.44
        for radius,color,width in [(110,"#12253C",2),(145,"#0C1B31",1),(190,"#0B172B",1)]:
            self.canvas.create_oval(cx-radius,cy-radius,cx+radius,cy+radius,outline=color,width=width,tags="frame")
        self.canvas.create_oval(cx-62,cy-62,cx+62,cy+62,outline="#143C4B",width=2,tags="frame")
        self.canvas.create_text(cx,cy-7,text="✦",fill=CYAN,font=("Segoe UI",42),tags="frame")
        if self.state=="menu":
            self.canvas.create_text(cx,cy-195,text="NEON",fill=WHITE,font=("Segoe UI",53,"bold"),tags="frame")
            self.canvas.create_text(cx,cy-140,text="R I F T",fill=CYAN,font=("Segoe UI",27,"bold"),tags="frame")
            self.canvas.create_text(cx,cy+105,text="SURVIVAL PROTOCOL",fill=PINK,font=("Consolas",12,"bold"),tags="frame")
            self.canvas.create_text(cx,cy+145,text="An endless neon arena. How long can you survive?",fill=MUTED,font=("Segoe UI",11),tags="frame")
            self.canvas.create_text(cx,cy+188,text="[ ENTER ]  OR  CLICK TO DEPLOY",fill=GOLD,font=("Consolas",12,"bold"),tags="frame")
            self.canvas.create_text(cx,cy+232,text="WASD / ARROWS   MOVE        •        CLICK / SPACE   FIRE        •        ESC   PAUSE",fill="#647A9C",font=("Segoe UI",9),tags="frame")
            self.canvas.create_text(cx,h-24,text=f"PERSONAL BEST  {self.high_score:07d}",fill="#647A9C",font=("Consolas",10),tags="frame")
        elif self.state=="paused":
            self.canvas.create_rectangle(cx-230,cy-100,cx+230,cy+120,fill="#0A1121",outline="#26415C",width=2,tags="frame")
            self.canvas.create_text(cx,cy-49,text="PAUSED",fill=WHITE,font=("Segoe UI",30,"bold"),tags="frame")
            self.canvas.create_text(cx,cy+3,text="Press ESC or click to resume",fill=CYAN,font=("Segoe UI",12),tags="frame")
            self.canvas.create_text(cx,cy+55,text=f"SCORE  {self.score:07d}     WAVE  {self.wave}",fill=MUTED,font=("Consolas",11),tags="frame")
        elif self.state=="gameover":
            self.canvas.create_rectangle(cx-260,cy-145,cx+260,cy+170,fill="#0A1121",outline="#4B2446",width=2,tags="frame")
            self.canvas.create_text(cx,cy-95,text="SHIP DESTROYED",fill=PINK,font=("Segoe UI",27,"bold"),tags="frame")
            self.canvas.create_text(cx,cy-38,text=f"{self.score:07d}",fill=WHITE,font=("Consolas",35,"bold"),tags="frame")
            self.canvas.create_text(cx,cy+5,text="FINAL SCORE",fill=MUTED,font=("Segoe UI",10,"bold"),tags="frame")
            self.canvas.create_text(cx,cy+45,text=f"WAVE {self.wave}      ELIMINATIONS {self.kills}",fill=CYAN,font=("Consolas",11),tags="frame")
            self.canvas.create_text(cx,cy+82,text=f"PERSONAL BEST  {self.high_score:07d}",fill=GOLD,font=("Consolas",11,"bold"),tags="frame")
            self.canvas.create_text(cx,cy+130,text="[ R ] RETRY     •     [ ENTER ] RETRY     •     CLICK TO RETRY",fill=WHITE,font=("Segoe UI",10,"bold"),tags="frame")

if __name__ == "__main__":
    root=tk.Tk()
    app=NeonRift(root)
    root.mainloop()
