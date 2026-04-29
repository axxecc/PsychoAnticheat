package com.psycho.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.psycho.Psycho;
import com.psycho.checks.Check;
import com.psycho.checks.impl.combat.aim.ml.AimAssistML;
import com.psycho.player.PsychoPlayer;
import com.psycho.scheduler.task.WrappedTask;
import com.psycho.utils.Hex;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Holograms extends PacketListenerAbstract implements Listener {

    private static final String PERMISSION = "psycho.alerts";
    private static final double DEFAULT_Y_OFFSET = 2.5;
    private static final int CLEANUP_INTERVAL = 100;

    private final Psycho plugin;

    private final Map<UUID, Integer> armorStandIds = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastSentText = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> viewersMap = new ConcurrentHashMap<>();

    private WrappedTask task;
    private int cleanupCounter = 0;

    public Holograms(Psycho plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
    }

    public void start() {
        PacketEvents.getAPI().getEventManager().registerListener(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        task = plugin.getScheduler().runSyncTimer(this::globalTick, 1L, 1L);
    }

    public void stop() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID targetId : new HashSet<>(armorStandIds.keySet())) {
            despawnForAll(targetId);
        }
        armorStandIds.clear();
        lastSentText.clear();
        viewersMap.clear();
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        try {
            if (event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION &&
                    event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION)
                return;

            Player player = event.getPlayer();
            if (player == null) return;

            Integer entityId = armorStandIds.get(player.getUniqueId());
            if (entityId == null) return;

            WrapperPlayClientPlayerFlying flying = new WrapperPlayClientPlayerFlying(event);
            if (!flying.hasPositionChanged()) return;

            Vector3d pos = flying.getLocation().getPosition();

            WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport(
                    entityId,
                    new Vector3d(pos.getX(), pos.getY() + DEFAULT_Y_OFFSET, pos.getZ()),
                    0f, 0f, false);

            Set<UUID> viewers = viewersMap.get(player.getUniqueId());
            if (viewers == null) return;

            for (UUID viewerId : viewers) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null && viewer.isOnline()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, teleport);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void globalTick() {
        List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        List<Player> admins = new ArrayList<>();
        for (Player p : allPlayers) {
            if (p.hasPermission(PERMISSION)) {
                admins.add(p);
            }
        }

        if (admins.isEmpty()) return;

        for (Player target : allPlayers) {
            updateNametag(target, admins);
        }

        if (++cleanupCounter > CLEANUP_INTERVAL) {
            cleanupCounter = 0;
            cleanupOfflinePlayers();
        }
    }

    private void cleanupOfflinePlayers() {
        armorStandIds.keySet().removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        for (Set<UUID> viewers : viewersMap.values()) {
            viewers.removeIf(uuid -> Bukkit.getPlayer(uuid) == null);
        }
    }

    private void updateNametag(Player target, List<Player> admins) {
        PsychoPlayer psychoPlayer = plugin.getConnectionListener().getPlayer(target.getUniqueId());
        if (psychoPlayer == null) return;

        AimAssistML aimAssistML = psychoPlayer.getCheck(AimAssistML.class);
        if (aimAssistML == null) return;

        String rawText = buildDisplayText(aimAssistML.getAvgHistory());

        int entityId = armorStandIds.computeIfAbsent(
                target.getUniqueId(),
                k -> ThreadLocalRandom.current().nextInt(1_000_000, 2_000_000));

        Location loc = target.getLocation().add(0, DEFAULT_Y_OFFSET, 0);

        String lastText = lastSentText.get(target.getUniqueId());
        boolean textChanged = !rawText.equals(lastText);

        for (Player viewer : admins) {
            if (viewer.getUniqueId().equals(target.getUniqueId())) continue;

            if (!viewer.getWorld().equals(target.getWorld()) ||
                    viewer.getLocation().distanceSquared(target.getLocation()) > 10_000) {
                removeViewer(target.getUniqueId(), viewer);
                continue;
            }

            updateFor(target, viewer, entityId, loc, rawText, textChanged);
        }

        if (textChanged) {
            lastSentText.put(target.getUniqueId(), rawText);
        }
    }

    private String buildDisplayText(Deque<Double> avgHistory) {
        Double latestAvg = avgHistory.peekLast();
        if (latestAvg == null) {
            return Check.buildBar(0.0);
        }
        return Check.buildBar(latestAvg);
    }

    private void updateFor(Player target, Player viewer, int entityId,
                           Location loc, String text, boolean textChanged) {
        Set<UUID> viewers = viewersMap.computeIfAbsent(
                target.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
        boolean isNew = viewers.add(viewer.getUniqueId());

        if (isNew) {
            WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                    entityId, Optional.of(UUID.randomUUID()), EntityTypes.ARMOR_STAND,
                    new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                    0f, 0f, 0f, 0, Optional.empty());
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, spawn);
        }

        if (isNew || textChanged) {
            List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> metadata =
                    buildMetadata(viewer, text);
            WrapperPlayServerEntityMetadata metaPacket =
                    new WrapperPlayServerEntityMetadata(entityId, metadata);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, metaPacket);
        }

        if (isNew) {
            WrapperPlayServerEntityTeleport teleport = new WrapperPlayServerEntityTeleport(
                    entityId, new Vector3d(loc.getX(), loc.getY(), loc.getZ()), 0f, 0f, false);
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, teleport);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        for (UUID targetId : viewersMap.keySet()) {
            removeViewer(targetId, event.getPlayer());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getWorld() != event.getTo().getWorld()) return;

        if (event.getFrom().distanceSquared(event.getTo()) > 2_500) {
            for (UUID targetId : viewersMap.keySet()) {
                removeViewer(targetId, event.getPlayer());
            }
        }
    }

    public void handlePlayerQuit(Player player) {
        despawnForAll(player.getUniqueId());

        for (UUID targetId : viewersMap.keySet()) {
            removeViewer(targetId, player);
        }
    }

    private void removeViewer(UUID targetId, Player viewer) {
        Set<UUID> viewers = viewersMap.get(targetId);
        if (viewers != null && viewers.remove(viewer.getUniqueId())) {
            Integer entityId = armorStandIds.get(targetId);
            if (entityId != null) {
                WrapperPlayServerDestroyEntities destroy =
                        new WrapperPlayServerDestroyEntities(new int[]{entityId});
                PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, destroy);
            }
        }
    }

    private void despawnForAll(UUID targetId) {
        Integer id = armorStandIds.remove(targetId);
        lastSentText.remove(targetId);
        Set<UUID> viewers = viewersMap.remove(targetId);

        if (id == null || viewers == null) return;

        WrapperPlayServerDestroyEntities destroy =
                new WrapperPlayServerDestroyEntities(new int[]{id});
        for (UUID viewerId : viewers) {
            Player p = Bukkit.getPlayer(viewerId);
            if (p != null && p.isOnline()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(p, destroy);
            }
        }
    }

    private List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> buildMetadata(
            Player viewer, String text) {

        List<com.github.retrooper.packetevents.protocol.entity.data.EntityData<?>> metadata = new ArrayList<>();

        com.github.retrooper.packetevents.protocol.player.ClientVersion clientVersion =
                PacketEvents.getAPI().getPlayerManager().getClientVersion(viewer);
        int version = clientVersion != null ? clientVersion.getProtocolVersion() : 770;

        metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Byte>(
                0,
                com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE,
                (byte) 0x20));

        String colorized = Hex.translateHexColors(text);

        if (version >= 766) {
            net.kyori.adventure.text.Component component =
                    net.kyori.adventure.text.Component.text(colorized);
            metadata.add(
                    new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Optional<net.kyori.adventure.text.Component>>(
                            2,
                            com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.OPTIONAL_ADV_COMPONENT,
                            Optional.of(component)));
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Boolean>(
                    3,
                    com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BOOLEAN,
                    true));
        } else if (version >= 393) {
            net.kyori.adventure.text.Component component =
                    net.kyori.adventure.text.Component.text(colorized);
            String json = com.github.retrooper.packetevents.util.adventure.AdventureSerializer
                    .getGsonSerializer().serialize(component);
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Optional<String>>(
                    2,
                    com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.OPTIONAL_COMPONENT,
                    Optional.of(json)));
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Boolean>(
                    3,
                    com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BOOLEAN,
                    true));
        } else {
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<String>(
                    2,
                    com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.STRING,
                    colorized));
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Boolean>(
                    3,
                    com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BOOLEAN,
                    true));
        }

        if (version < 766) {
            int markerIndex;
            if (version >= 755) {
                markerIndex = 15; // 1.17+
            } else if (version >= 448) {
                markerIndex = 14; // 1.14+
            } else if (version >= 385) {
                markerIndex = 12; // 1.13
            } else if (version >= 107) {
                markerIndex = 11; // 1.9-1.12
            } else {
                markerIndex = 10; // 1.8
            }
            metadata.add(new com.github.retrooper.packetevents.protocol.entity.data.EntityData<Byte>(
                    markerIndex,
                    com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE,
                    (byte) 0x10));
        }

        return metadata;
    }

}
