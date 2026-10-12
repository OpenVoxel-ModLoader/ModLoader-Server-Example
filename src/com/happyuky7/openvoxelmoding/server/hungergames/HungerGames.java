package com.happyuky7.openvoxelmoding.server.hungergames;

import net.openvoxel.server.plugin.*;
import net.openvoxel.server.plugin.event.EventHandler;
import net.openvoxel.server.plugin.event.Listener;
import net.openvoxel.server.plugin.event.player.PlayerDeathEvent;
import net.openvoxel.server.plugin.event.player.PlayerJoinEvent;
import net.openvoxel.server.plugin.event.player.PlayerQuitEvent;

import java.util.*;

/**
 * Minimal Hunger Games: /hg start spreads everyone in a circle, counts down, runs a grace
 * period, then the last player standing wins. Server-side only; works with vanilla clients.
 */
public final class HungerGames extends Plugin implements Listener {
    private enum State { IDLE, COUNTDOWN, GRACE, RUNNING }

    private State state = State.IDLE;
    private final Set<String> alive = new LinkedHashSet<>();
    private final List<Integer> tasks = new ArrayList<>();
    private Location center;
    private int radius = 30, grace = 30, countdown = 10;

    @Override public void onEnable() {
        VoxelServer.getPluginManager().registerEvents(this, this);
        VoxelServer.getCommandManager().register("hg", "<start [radius] | stop | status> - Hunger Games", (player, args) -> {
            String sub = args.length > 0 ? args[0].toLowerCase() : "status";
            switch (sub) {
                case "start": start(player, args.length > 1 ? parse(args[1], radius) : radius); break;
                case "stop": stop("&eThe game was stopped by " + player.getName()); break;
                default: player.sendMessage("&6[HG] &fState: &e" + state + " &f| alive: &a" + alive.size() + " &f| radius " + radius + ", grace " + grace + "s");
            }
        }, this);
        getLogger().info("Hunger Games ready: /hg start");
    }

    @Override public void onDisable() { cancelTasks(); }

    private static int parse(String s, int def) { try { return Math.max(5, Integer.parseInt(s)); } catch (NumberFormatException e) { return def; } }

    private void start(Player starter, int r) {
        if (state != State.IDLE) { starter.sendMessage("&c[HG] A game is already running. /hg stop first."); return; }
        List<Player> players = new ArrayList<>(VoxelServer.getOnlinePlayers());
        if (players.isEmpty()) { starter.sendMessage("&c[HG] No players online."); return; }
        if (players.size() < 2) starter.sendMessage("&e[HG] Only one player: starting a practice round.");
        radius = r;
        center = starter.getEntity() != null ? starter.getLocation() : players.get(0).getLocation();
        alive.clear();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            double angle = 2 * Math.PI * i / players.size();
            double x = center.getX() + Math.cos(angle) * radius, z = center.getZ() + Math.sin(angle) * radius;
            double y = groundAt((int) Math.floor(x), (int) Math.floor(center.getY()) + 24, (int) Math.floor(z));
            if (p.getEntity() != null) { p.getEntity().health = p.getEntity().maxHealth; p.getEntity().dead = false; }
            p.setLocation(new Location(x, y, z, (float) Math.toDegrees(angle) + 180f, 0f));
            alive.add(p.getName());
        }
        state = State.COUNTDOWN;
        VoxelServer.broadcast("&6&l[HG] &eHunger Games starting! &f" + alive.size() + " tributes, radius " + radius + ".");
        schedule(countdown, "&6[HG] &fStarts in &e%d", () -> {
            state = State.GRACE;
            VoxelServer.broadcast("&a&l[HG] GO! &fGrace period: " + grace + " s (no fighting).");
            schedule(grace, "&6[HG] &fGrace ends in &e%d", () -> {
                state = State.RUNNING;
                VoxelServer.broadcast("&c&l[HG] &fGrace over. May the odds be ever in your favor!");
                checkWinner();
            });
        });
    }

    /** Counts down with chat messages at 10/5/3/2/1 then runs the action. */
    private void schedule(int seconds, String format, Runnable done) {
        for (int s = seconds; s >= 1; s--) {
            final int left = s;
            if (s <= 5 || s == 10 || s % 30 == 0)
                tasks.add(VoxelServer.getScheduler().runLater(this, () -> VoxelServer.broadcast(String.format(format, left)), (seconds - s) * 20L));
        }
        tasks.add(VoxelServer.getScheduler().runLater(this, done, seconds * 20L));
    }

    private void cancelTasks() { for (int id : tasks) VoxelServer.getScheduler().cancel(id); tasks.clear(); }

    private void stop(String reason) {
        cancelTasks();
        state = State.IDLE;
        alive.clear();
        VoxelServer.broadcast(reason);
    }

    private void checkWinner() {
        if (state == State.IDLE) return;
        if (alive.size() == 1) stop("&6&l[HG] &e" + alive.iterator().next() + " &fwins the Hunger Games!");
        else if (alive.isEmpty()) stop("&6[HG] &fNobody survived.");
    }

    private void eliminate(String name, String how) {
        if (state == State.IDLE || !alive.remove(name)) return;
        VoxelServer.broadcast("&c[HG] &f" + name + " " + how + ". &7" + alive.size() + " left.");
        if (state == State.RUNNING || state == State.GRACE) checkWinner();
    }

    @EventHandler public void onDeath(PlayerDeathEvent event) { eliminate(event.getPlayer().getName(), "has fallen"); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { eliminate(event.getPlayer().getName(), "left the arena"); }
    @EventHandler public void onJoin(PlayerJoinEvent event) { if (state != State.IDLE) event.getPlayer().sendMessage("&6[HG] &fA game is in progress; wait for the next one."); }

    /** Highest non-air block top around y (scanning down), so tributes do not spawn inside terrain. */
    private double groundAt(int x, int top, int z) {
        for (int y = Math.min(top, 500); y > 1; y--) {
            long state = VoxelServer.getBlock(x, y, z);
            String name = VoxelServer.blockName(state);
            if (name != null && !name.equalsIgnoreCase("air") && !name.toLowerCase().contains("water") && !name.toLowerCase().contains("lava")) return y + 1;
        }
        return top;
    }
}
