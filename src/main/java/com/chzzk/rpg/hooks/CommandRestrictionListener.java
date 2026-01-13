package com.chzzk.rpg.hooks;

import com.chzzk.rpg.ChzzkRPG;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandRestrictionListener implements Listener {

    private final ChzzkRPG plugin;

    public CommandRestrictionListener(ChzzkRPG plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage().trim();
        if (!message.startsWith("/")) {
            return;
        }
        String command = message.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        CompatibilityManager compatibilityManager = plugin.getCompatibilityManager();
        if (compatibilityManager == null || !compatibilityManager.isCommandRestricted(command)) {
            return;
        }
        if (!compatibilityManager.isCommandAllowed(player)) {
            player.sendMessage("§c이 지역에서는 명령어를 사용할 수 없습니다.");
            event.setCancelled(true);
        }
    }
}
