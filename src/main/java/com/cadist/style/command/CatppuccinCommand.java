package com.cadist.style.command;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.gui.CatppuccinGui;
import com.cadist.style.util.CatppuccinTheme;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CatppuccinCommand implements CommandExecutor {

    private final CatppuccinStyler plugin;

    public CatppuccinCommand(CatppuccinStyler plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command must be run by a player.", CatppuccinTheme.RED));
            return true;
        }

        if (!player.hasPermission("catppuccinstyler.admin")) {
            player.sendMessage(Component.text("You don't have permission.", CatppuccinTheme.RED));
            return true;
        }

        new CatppuccinGui(plugin, player).open();
        return true;
    }
}
