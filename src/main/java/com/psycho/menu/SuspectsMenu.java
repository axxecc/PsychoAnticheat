package com.psycho.menu;

import com.psycho.Psycho;
import com.psycho.checks.Check;
import com.psycho.checks.impl.combat.aim.ml.AimAssistML;
import com.psycho.player.PsychoPlayer;
import com.psycho.utils.Hex;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class SuspectsMenu implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45;
    private static final int HISTORY_DISPLAY_LIMIT = 10;

    private final Psycho plugin;
    private final Player admin;
    private final Inventory inventory;
    private final NamespacedKey suspectUuidKey;

    private List<SuspectEntry> cachedSuspects = Collections.emptyList();
    private boolean loading;
    private boolean closed;
    private int refreshSequence;
    private int page = 0;

    public SuspectsMenu(Psycho plugin, Player admin) {
        this.plugin = plugin;
        this.admin = admin;
        this.suspectUuidKey = new NamespacedKey(plugin, "suspect_uuid");
        this.inventory = Bukkit.createInventory(
                null,
                INVENTORY_SIZE,
                color(plugin.getConfigService().getString("menu.suspects.title", "#ff4500Psycho &8> &7Suspects"))
        );
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        renderLoadingState();
        admin.openInventory(inventory);
        refreshSuspectsAsync();
    }

    private void refreshSuspectsAsync() {
        if (closed) {
            return;
        }

        int requestId = ++refreshSequence;
        List<SuspectSnapshot> snapshots = collectSuspectSnapshots();
        loading = true;

        if (cachedSuspects.isEmpty()) {
            renderLoadingState();
        }

        plugin.getScheduler().runAsync(() -> {
            List<SuspectEntry> suspects = buildSuspectEntries(snapshots);

            plugin.getScheduler().runSync(() -> {
                if (closed || requestId != refreshSequence || !admin.isOnline()) {
                    return;
                }

                cachedSuspects = suspects;
                loading = false;
                renderCurrentPage();
            });
        });
    }

    private void renderCurrentPage() {
        inventory.clear();

        List<SuspectEntry> suspects = cachedSuspects;
        int totalPages = Math.max(1, (int) Math.ceil((double) suspects.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        if (suspects.isEmpty()) {
            inventory.setItem(22, createItem(
                    Material.BARRIER,
                    "&cNo suspects found",
                    "&7No players currently have ML history to display."
            ));
            renderFooter(1, 0);
            return;
        }

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, suspects.size());

        for (int slot = 0; slot < endIndex - startIndex; slot++) {
            inventory.setItem(slot, createSuspectHead(suspects.get(startIndex + slot)));
        }

        renderFooter(totalPages, suspects.size());
    }

    private void renderLoadingState() {
        inventory.clear();
        inventory.setItem(22, createItem(
                Material.CLOCK,
                "&eLoading suspects...",
                "&7Collecting and sorting ML history asynchronously."
        ));
        renderLoadingFooter();
    }

    private List<SuspectSnapshot> collectSuspectSnapshots() {
        List<SuspectSnapshot> snapshots = new ArrayList<>();

        for (Player online : Bukkit.getOnlinePlayers()) {
            PsychoPlayer psychoPlayer = plugin.getConnectionListener().getPlayer(online.getUniqueId());
            if (psychoPlayer == null) continue;

            AimAssistML aimAssistML = psychoPlayer.getCheck(AimAssistML.class);
            if (aimAssistML == null) continue;

            Deque<Double> avgHistory = aimAssistML.getAvgHistory();
            if (avgHistory.isEmpty()) continue;

            snapshots.add(new SuspectSnapshot(
                    online.getUniqueId(),
                    online.getName(),
                    new ArrayList<>(avgHistory),
                    psychoPlayer.getStats().getFailedChecks().size(),
                    psychoPlayer.getCps()
            ));
        }

        return snapshots;
    }

    private List<SuspectEntry> buildSuspectEntries(List<SuspectSnapshot> snapshots) {
        List<SuspectEntry> suspects = new ArrayList<>(snapshots.size());

        for (SuspectSnapshot snapshot : snapshots) {
            List<Double> history = snapshot.history();
            double latest = history.get(history.size() - 1);
            double total = 0.0;
            double peak = Double.NEGATIVE_INFINITY;

            for (double value : history) {
                total += value;
                if (value > peak) {
                    peak = value;
                }
            }

            suspects.add(new SuspectEntry(
                    snapshot.uuid(),
                    snapshot.name(),
                    total / history.size(),
                    peak == Double.NEGATIVE_INFINITY ? 0.0 : peak,
                    latest,
                    history,
                    snapshot.failedChecks(),
                    snapshot.cps()
            ));
        }

        suspects.sort(Comparator
                .comparingDouble(SuspectEntry::latest)
                .reversed()
                .thenComparing(Comparator.comparingDouble(SuspectEntry::average).reversed())
                .thenComparing(SuspectEntry::name, String.CASE_INSENSITIVE_ORDER));

        return suspects;
    }

    private void renderLoadingFooter() {
        inventory.setItem(49, createItem(
                Material.CLOCK,
                "&6Suspects",
                "&7Refreshing entries..."
        ));

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 45; slot < INVENTORY_SIZE; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void renderFooter(int totalPages, int totalSuspects) {
        if (page > 0) {
            inventory.setItem(45, createItem(Material.ARROW, "&ePrevious Page", "&7Open page &f" + page));
        }

        inventory.setItem(49, createItem(
                Material.PAPER,
                "&6Suspects",
                "&7Page: &f" + (page + 1) + "&7/&f" + totalPages,
                "&7Entries: &f" + totalSuspects
        ));

        if (page + 1 < totalPages) {
            inventory.setItem(53, createItem(Material.ARROW, "&eNext Page", "&7Open page &f" + (page + 2)));
        }

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 45; slot < INVENTORY_SIZE; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private ItemStack createSuspectHead(SuspectEntry entry) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) {
            return head;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.uuid());
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName(color("&c" + entry.name()));
        meta.getPersistentDataContainer().set(suspectUuidKey, PersistentDataType.STRING, entry.uuid().toString());

        List<String> lore = new ArrayList<>();
        lore.add(color("&8&m------------------------"));
        lore.add(color("&7Current: &f" + formatValue(entry.latest()) + " &8" + Check.buildBar(entry.latest())));
        lore.add(color("&7Average: &f" + formatValue(entry.average())));
        lore.add(color("&7Peak: &f" + formatValue(entry.peak())));
        lore.add(color("&7Last " + HISTORY_DISPLAY_LIMIT + ": " + buildHistoryLine(entry.history())));
        lore.add(color("&7Failed checks: &f" + entry.failedChecks()));
        lore.add(color("&7CPS: &f" + entry.cps()));
        lore.add(color("&8&m------------------------"));
        lore.add(color("&eLeft click &7- teleport"));
        lore.add(color("&eRight click &7- spectate"));
        lore.add(color("&eQ &7- punish"));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color(name));

        if (loreLines.length > 0) {
            List<String> lore = new ArrayList<>();
            for (String loreLine : loreLines) {
                lore.add(color(loreLine));
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private String buildHistoryLine(List<Double> history) {
        int startIndex = Math.max(0, history.size() - HISTORY_DISPLAY_LIMIT);
        List<Double> recentHistory = history.subList(startIndex, history.size());

        StringBuilder builder = new StringBuilder();
        for (Double value : recentHistory) {
            if (builder.length() > 0) {
                builder.append(" &8| ");
            }
            builder.append(valueColor(value)).append(String.format("%.2f", value)).append("&r");
        }
        return builder.toString();
    }

    private String formatValue(double value) {
        return valueColor(value) + String.format("%.2f", value) + "&r";
    }

    private String valueColor(double value) {
        if (value >= 0.9) return "&4&l";
        if (value >= 0.8) return "&c";
        if (value >= 0.6) return "&6";
        return "&a";
    }

    private String color(String text) {
        return Hex.translateHexColors(text);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory() != inventory) return;

        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= INVENTORY_SIZE) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (rawSlot == 45 && page > 0 && item.getType() == Material.ARROW) {
            page--;
            renderCurrentPage();
            return;
        }

        if (rawSlot == 53 && item.getType() == Material.ARROW) {
            page++;
            renderCurrentPage();
            return;
        }

        if (loading) return;
        if (rawSlot >= ITEMS_PER_PAGE) return;
        if (item.getType() != Material.PLAYER_HEAD) return;

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) return;
        String rawUuid = skullMeta.getPersistentDataContainer().get(suspectUuidKey, PersistentDataType.STRING);
        if (rawUuid == null) return;

        UUID targetUuid;
        try {
            targetUuid = UUID.fromString(rawUuid);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null || !target.isOnline()) {
            admin.sendMessage(color(plugin.getConfigService().getString(
                    "menu.suspects.suspects-player-offline",
                    "{prefix} §cThat player is no longer online."
            ).replace("{prefix}", plugin.getMessagesCfg().prefix())));
            refreshSuspectsAsync();
            return;
        }

        if (event.getClick() == ClickType.DROP) {
            banTarget(target);
            refreshSuspectsAsync();
            return;
        }

        if (event.isRightClick()) {
            admin.setGameMode(GameMode.SPECTATOR);
            admin.sendMessage(color(plugin.getConfigService().getString(
                    "menu.suspects.suspects-spectate",
                    "{prefix} #ffe1c9Now spectating #ff4500{player}"
            ).replace("{prefix}", plugin.getMessagesCfg().prefix()).replace("{player}", target.getName())));
            return;
        }

        admin.teleport(target);
        admin.sendMessage(color(plugin.getConfigService().getString(
                "menu.suspects.suspects-teleport",
                "{prefix} #ffe1c9Teleported to #ff4500{player}"
        ).replace("{prefix}", plugin.getMessagesCfg().prefix()).replace("{player}", target.getName())));
    }

    private void banTarget(Player target) {
        String punishCommand = plugin.getConfigService().getString(
                        "menu.suspects.suspects-punish-command",
                        "ban {player} Banned by Psycho"
                ).replace("{player}", target.getName())
                .replace("{admin}", admin.getName());

        String broadcast = color(plugin.getConfigService().getString(
                        "menu.suspects.suspects-ban-broadcast",
                        "{prefix} #ffe1c9{admin} #ff4500banned #ffe1c9{player} #ff4500via suspects menu."
                ).replace("{prefix}", plugin.getMessagesCfg().prefix())
                .replace("{admin}", admin.getName())
                .replace("{player}", target.getName()));

        Location soundLoc = target.getLocation();

        plugin.getScheduler().runSync(() -> {
            World world = soundLoc.getWorld();
            if (world != null) {
                world.playSound(soundLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
            }
        });

        plugin.getScheduler().runSync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), punishCommand));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("psycho.alerts")) {
                online.sendMessage(broadcast);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory() == inventory) {
            closed = true;
            HandlerList.unregisterAll(this);
        }
    }

    private record SuspectSnapshot(
            UUID uuid,
            String name,
            List<Double> history,
            int failedChecks,
            int cps
    ) {
    }

    private record SuspectEntry(
            UUID uuid,
            String name,
            double average,
            double peak,
            double latest,
            List<Double> history,
            int failedChecks,
            int cps
    ) {
    }
}
