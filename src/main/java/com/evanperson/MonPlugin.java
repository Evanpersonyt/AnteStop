package com.evanperson;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class MonPlugin extends JavaPlugin implements Listener {

    private int countdownTime;
    private int restartCountdownTime;
    private int reloadCountdownTime;
    private String countdownMessage;
    private String stopMessage;
    private String restartCountdownMessage;
    private String restartMessage;
    private String reloadCountdownMessage;
    private String reloadMessage;
    private String maintenanceKickMessage;
    private String opMaintenanceActionBarMessage;
    private boolean isMaintenance = false; // Variable pour gérer l'état de maintenance
    
    private void loadConfigValues() {
        countdownTime = getConfig().getInt("countdown-seconds", 3);
        restartCountdownTime = getConfig().getInt("restart-countdown-seconds", 5);
        reloadCountdownTime = getConfig().getInt("reload-countdown-seconds", 4);
        countdownMessage = getConfig().getString("messages.countdown", "Arrêt dans {time} secondes...");
        stopMessage = getConfig().getString("messages.stop", "Le serveur s'arrête maintenant !");
        restartCountdownMessage = getConfig().getString("messages.restart-countdown", "Redémarrage dans {time} secondes...");
        restartMessage = getConfig().getString("messages.restart", "Le serveur redémarre maintenant !");
        reloadCountdownMessage = getConfig().getString("messages.reload-countdown", "Rechargement dans {time} secondes...");
        reloadMessage = getConfig().getString("messages.reload", "Le serveur se recharge maintenant !");
        maintenanceKickMessage = getConfig().getString("messages.maintenance-kick", "Le serveur est en maintenance. Revenez plus tard.");
        opMaintenanceActionBarMessage = getConfig().getString("messages.op-maintenance-actionbar", "Le serveur est en maintenance.");
    }
    
    @Override
    public void onEnable() {
        // Charger la configuration
        saveDefaultConfig();
        loadConfigValues();

        // Vérifier l'état de maintenance enregistré dans le fichier de configuration
        isMaintenance = getConfig().getBoolean("is-maintenance", false);

        getLogger().info("\u00A74Démmarrage Du Plugin AnteStop");

        // Enregistrer le listener
        Bukkit.getPluginManager().registerEvents(this, this);

        // Lancer la tâche qui envoie le message aux OPs si le mode maintenance est activé
        startActionBarTask();
    }

    @Override
    public void onDisable() {
        getLogger().info("\u00A74bye :/ !");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("tempmaintenance")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("tempstop.use")) {
                    startShutdownCountdown();
                    return true;
                } else {
                    player.sendMessage("Vous n'avez pas la permission d'utiliser cette commande.");
                    return true;
                }
            } else {
                startShutdownCountdown();
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("temprestart")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("temprestart.use")) {
                    startRestartCountdown();
                    return true;
                } else {
                    player.sendMessage("Vous n'avez pas la permission d'utiliser cette commande.");
                    return true;
                }
            } else {
                startRestartCountdown();
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("tempreload")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("tempreload.use")) {
                    startReloadCountdown();
                    return true;
                } else {
                    player.sendMessage("Vous n'avez pas la permission d'utiliser cette commande.");
                    return true;
                }
            } else {
                startReloadCountdown();
                return true;
            }
        }

        // Commande pour arrêter le mode maintenance
        if (command.getName().equalsIgnoreCase("stopmaintenance")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("stopmaintenance.use")) {
                    stopMaintenance();
                    return true;
                } else {
                    player.sendMessage("Vous n'avez pas la permission d'utiliser cette commande.");
                    return true;
                }
            } else {
                stopMaintenance();
                return true;
            }
        }

        return false;
    }

    private void startShutdownCountdown() {
        new BukkitRunnable() {
            int countdown = countdownTime;

            @Override
            public void run() {
                if (countdown > 0) {
                    String message = countdownMessage.replace("{time}", String.valueOf(countdown));
                    Bukkit.broadcastMessage(message);
                    countdown--;
                } else {
                    isMaintenance = true; // Activer l'état de maintenance
                    getConfig().set("is-maintenance", true); // Mémoriser l'état de maintenance
                    saveConfig(); // Sauvegarder la configuration
                    // Expulser tous les joueurs non-OP en ligne après un délai
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!player.isOp()) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.kickPlayer(maintenanceKickMessage);
                                }
                            }.runTaskLater(MonPlugin.this, 20L); // 20 ticks = 1 seconde
                        }
                    }
                    Bukkit.broadcastMessage(stopMessage);
                    Bukkit.broadcastMessage("Le serveur va être mis en maintenance. Merci de revenir plus tard.");
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 20L); // 20 ticks = 1 seconde
    }

    private void stopMaintenance() {
        isMaintenance = false; // Désactiver l'état de maintenance
        getConfig().set("is-maintenance", false); // Mémoriser l'état de maintenance
        saveConfig(); // Sauvegarder la configuration
        Bukkit.broadcastMessage("Le mode maintenance est désactivé. Les joueurs peuvent maintenant rejoindre !");
    }

    private void startRestartCountdown() {
        new BukkitRunnable() {
            int countdown = restartCountdownTime;

            @Override
            public void run() {
                if (countdown > 0) {
                    String message = restartCountdownMessage.replace("{time}", String.valueOf(countdown));
                    Bukkit.broadcastMessage(message);
                    countdown--;
                } else {
                    Bukkit.broadcastMessage(restartMessage);
                    // Exécuter la commande /restart dans la console
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "restart");
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 20L); // 20 ticks = 1 seconde
    }

    private void startReloadCountdown() {
        new BukkitRunnable() {
            int countdown = reloadCountdownTime;

            @Override
            public void run() {
                if (countdown > 0) {
                    String message = reloadCountdownMessage.replace("{time}", String.valueOf(countdown));
                    Bukkit.broadcastMessage(message);
                    countdown--;
                } else {
                    Bukkit.broadcastMessage(reloadMessage);
                    // Exécuter la commande /reload dans la console
                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "reload");
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 20L); // 20 ticks = 1 seconde
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isMaintenance && !event.getPlayer().isOp()) {
            // Délai d'une seconde avant de kicker le joueur
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Kick le joueur avec un message personnalisé
                    event.getPlayer().kickPlayer(maintenanceKickMessage);
                }
            }.runTaskLater(this, 20L); // 20 ticks = 1 seconde
        }
    }

   private void startActionBarTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isMaintenance) {
                    // Envoyer un message ActionBar aux OPs seulement si la maintenance est activée
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.isOp()) {
                            sendActionBar(player, opMaintenanceActionBarMessage);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 200L); // 40 ticks = 2 secondes
    }

    private void sendActionBar(Player player, String message) {
        // Envoie un message d'ActionBar avec la commande native Minecraft
        player.sendMessage("\u00A7a" + message);
    }


}
