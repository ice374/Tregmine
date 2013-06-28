package info.tregmine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.GameMode;

import info.tregmine.quadtree.IntersectionException;

import info.tregmine.api.TregminePlayer;
import info.tregmine.database.ConnectionPool;
import info.tregmine.database.DBChestBlessDAO;
import info.tregmine.database.DBZonesDAO;
import info.tregmine.zones.Lot;
import info.tregmine.zones.Zone;
import info.tregmine.zones.ZoneWorld;

import info.tregmine.listeners.*;
import info.tregmine.commands.*;

/**
 * @author Ein Andersson
 * @author Emil Hernvall
 */
public class Tregmine extends JavaPlugin
{
    public final static int VERSION = 0;
    public final static int AMOUNT = 0;

    public final static Logger LOGGER = Logger.getLogger("Minecraft");

    private Server server;

    private Map<String, TregminePlayer> players;
    private Map<Integer, String> blessedBlocks;

    private Map<String, ZoneWorld> worlds;
    private Map<Integer, Zone> zones;

    @Override
    public void onEnable()
    {
        this.server = getServer();

        // Set up all data structures
        players = new HashMap<String, TregminePlayer>();

        worlds =
            new TreeMap<String, ZoneWorld>(
                new Comparator<String>() {
                    @Override
                    public int compare(String a, String b) {
                        return a.compareToIgnoreCase(b);
                    }
                });

        zones = new HashMap<Integer, Zone>();

        // Set up all worlds
        WorldCreator citadelCreator = new WorldCreator("citadel");
        citadelCreator.environment(Environment.NORMAL);
        citadelCreator.createWorld();

        WorldCreator world = new WorldCreator("world");
        world.environment(Environment.NORMAL);
        world.createWorld();

        WorldCreator treton = new WorldCreator("treton");
        treton.environment(Environment.NORMAL);
        treton.createWorld();

        WorldCreator vanilla = new WorldCreator("elva");
        vanilla.environment(Environment.NORMAL);
        vanilla.createWorld();

        WorldCreator einhome = new WorldCreator("einhome");
        einhome.environment(Environment.NORMAL);
        einhome.createWorld();

        WorldCreator alpha = new WorldCreator("alpha");
        alpha.environment(Environment.NORMAL);
        alpha.createWorld();

        WorldCreator nether = new WorldCreator("world_nether");
        nether.environment(Environment.NETHER);
        nether.createWorld();

        // Register all listeners
        PluginManager pluginMgm = server.getPluginManager();
        pluginMgm.registerEvents(new BlessedBlockListener(this), this);
        pluginMgm.registerEvents(new BoxFillBlockListener(this), this);
        pluginMgm.registerEvents(new ChatListener(this), this);
        pluginMgm.registerEvents(new CompassListener(this), this);
        pluginMgm.registerEvents(new PlayerLookupListener(this), this);
        pluginMgm.registerEvents(new SignColorListener(), this);
        pluginMgm.registerEvents(new TauntListener(this), this);
        pluginMgm.registerEvents(new TregmineBlockListener(this), this);
        pluginMgm.registerEvents(new TregmineEntityListener(this), this);
        pluginMgm.registerEvents(new TregminePlayerListener(this), this);
        pluginMgm.registerEvents(new TregmineWeatherListener(this), this);
        pluginMgm.registerEvents(new ZoneBlockListener(this), this);
        pluginMgm.registerEvents(new ZoneEntityListener(this), this);
        pluginMgm.registerEvents(new ZonePlayerListener(this), this);

        // Declaration of all commands
        getCommand("a").setExecutor(
            new NotifyCommand(this, "a") {
                @Override
                public boolean isTarget(TregminePlayer player) {
                    return player.isAdmin();
                }
            });

        getCommand("g").setExecutor(
            new NotifyCommand(this, "g") {
                @Override
                public boolean isTarget(TregminePlayer player) {
                    return player.isGuardian();
                }
            });

        getCommand("action").setExecutor(new ActionCommand(this));
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("bless").setExecutor(new BlessCommand(this));
        getCommand("blockhere").setExecutor(new BlockHereCommand(this));
        getCommand("channel").setExecutor(new ChannelCommand(this));
        getCommand("clean").setExecutor(new CleanInventoryCommand(this));
        getCommand("cname").setExecutor(new ChangeNameCommand(this));
        getCommand("createmob").setExecutor(new CreateMobCommand(this));
        getCommand("createwarp").setExecutor(new CreateWarpCommand(this));
        getCommand("creative").setExecutor( new GameModeCommand(this, "creative", GameMode.CREATIVE));
        getCommand("fill").setExecutor(new FillCommand(this, "fill"));
        getCommand("force").setExecutor(new ForceCommand(this));
        getCommand("give").setExecutor(new GiveCommand(this));
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("item").setExecutor(new ItemCommand(this));
        getCommand("keyword").setExecutor(new KeywordCommand(this));
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("lot").setExecutor(new LotCommand(this));
        getCommand("msg").setExecutor(new MsgCommand(this));
        getCommand("newspawn").setExecutor(new NewSpawnCommand(this));
        getCommand("normal").setExecutor(new NormalCommand(this));
        getCommand("nuke").setExecutor(new NukeCommand(this));
        getCommand("password").setExecutor(new KeywordCommand(this));
        getCommand("pos").setExecutor(new PositionCommand(this));
        getCommand("regeneratechunk").setExecutor(new RegenerateChunkCommand(this));
        getCommand("say").setExecutor(new SayCommand(this));
        getCommand("sendto").setExecutor(new SendToCommand(this));
        getCommand("setspawner").setExecutor(new SetSpawnerCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("summon").setExecutor(new SummonCommand(this));
        getCommand("survival").setExecutor( new GameModeCommand(this, "survival", GameMode.SURVIVAL));
        getCommand("testfill").setExecutor(new FillCommand(this, "testfill"));
        getCommand("time").setExecutor(new TimeCommand(this));
        getCommand("tp").setExecutor(new TeleportCommand(this));
        getCommand("tpshield").setExecutor(new TeleportShieldCommand(this));
        getCommand("tpto").setExecutor(new TeleportToCommand(this));
        getCommand("user").setExecutor(new UserCommand(this));
        getCommand("warp").setExecutor(new WarpCommand(this));
        getCommand("zone").setExecutor(new ZoneCommand(this));
    }

    //run when plugin is disabled
    @Override
    public void onDisable()
    {
        server.getScheduler().cancelTasks(this);

        Player[] players = server.getOnlinePlayers();
        for (Player player : players) {
            player.sendMessage(ChatColor.AQUA + "Tregmine successfully unloaded " +
                    "build: " + getDescription().getVersion());
        }
    }

    @Override
    public void onLoad()
    {
        Player[] players = server.getOnlinePlayers();
        for (Player player : players) {
            TregminePlayer tregPlayer = new TregminePlayer(player);
            tregPlayer.load();
            addPlayer(tregPlayer);

            player.sendMessage(ChatColor.GRAY + "Tregmine successfully loaded " +
                    "to build: " + this.getDescription().getVersion());
        }

        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            DBChestBlessDAO chestBlessDAO = new DBChestBlessDAO(conn);
            this.blessedBlocks = chestBlessDAO.loadBlessedChests();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) {}
            }
        }
    }

    public ZoneWorld getWorld(World world)
    {
        ZoneWorld zoneWorld = worlds.get(world.getName());

        // lazy load zone worlds as required
        if (zoneWorld == null) {
            Connection conn = null;
            try {
                conn = ConnectionPool.getConnection();
                DBZonesDAO dao = new DBZonesDAO(conn);

                zoneWorld = new ZoneWorld(world);
                List<Zone> zones = dao.getZones(world.getName());
                for (Zone zone : zones) {
                    try {
                        zoneWorld.addZone(zone);
                        this.zones.put(zone.getId(), zone);
                    } catch (IntersectionException e) {
                        LOGGER.warning("Failed to load zone " + zone.getName() + " with id " + zone.getId() + ".");
                    }
                }

                List<Lot> lots = dao.getLots(world.getName());
                for (Lot lot : lots) {
                    try {
                        zoneWorld.addLot(lot);
                    } catch (IntersectionException e) {
                        LOGGER.warning("Failed to load lot " + lot.getName() + " with id " + lot.getId() + ".");
                    }
                }

                worlds.put(world.getName(), zoneWorld);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                if (conn != null) {
                    try { conn.close(); } catch (SQLException e) {}
                }
            }
        }

        return zoneWorld;
    }

    public Zone getZone(int zoneId)
    {
        return zones.get(zoneId);
    }

    public void addPlayer(TregminePlayer player)
    {
        players.put(player.getName(), player);
    }

    public void removePlayer(TregminePlayer player)
    {
        players.remove(player.getName());
    }

    public TregminePlayer getPlayer(String name)
    {
        return players.get(name);
    }

    public TregminePlayer getPlayer(Player player)
    {
        return players.get(player.getName());
    }

    public List<TregminePlayer> matchPlayer(String pattern)
    {
        List<Player> matches = server.matchPlayer(pattern);
        if (matches.size() == 0) {
            return new ArrayList<TregminePlayer>();
        }

        List<TregminePlayer> decoratedMatches = new ArrayList<TregminePlayer>();
        for (Player match : matches) {
            TregminePlayer decoratedMatch = getPlayer(match);
            if (decoratedMatch == null) {
                continue;
            }
            decoratedMatches.add(decoratedMatch);
        }

        return decoratedMatches;
    }

    public Map<Integer, String> getBlessedBlocks()
    {
        return blessedBlocks;
    }
}