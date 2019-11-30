package com.gmail.val59000mc.commands;

import com.gmail.val59000mc.exceptions.ParseException;
import com.gmail.val59000mc.utils.JsonItemUtils;
import com.gmail.val59000mc.utils.SpigotUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ItemInfoCommandExecutor implements CommandExecutor{

    private static final boolean DEBUG = false;

    @Override @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player)){
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = ((Player) sender).getPlayer();
        ItemStack item = player.getItemInHand();

        if (DEBUG && args.length != 0){
            try {
                item = JsonItemUtils.getItemFromJson(args[0]);
                player.getInventory().addItem(item);
            }catch (ParseException ex){
                player.sendMessage(ex.getMessage());
            }
            return true;
        }

        if (item.getType() == Material.AIR){
            player.sendMessage(ChatColor.RED + "Please hold a item first!");
            return true;
        }

        player.sendMessage(ChatColor.DARK_GREEN + "Item Info:");
        player.sendMessage(ChatColor.DARK_GREEN + " Material: " + ChatColor.GREEN + item.getType());
        player.sendMessage(ChatColor.DARK_GREEN + " Data/Damage value: " + ChatColor.GREEN + item.getDurability());
        sendJsonItemMessage(player, item);

        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()){
            player.sendMessage(ChatColor.DARK_GREEN + " Enchantments:");
            Map<Enchantment, Integer> enchantments = item.getItemMeta().getEnchants();
            for (Enchantment enchantment : enchantments.keySet()){
                player.sendMessage("  " + ChatColor.DARK_GREEN + enchantment.getName() + ChatColor.GREEN + " (level " + enchantments.get(enchantment) + ")");
            }
        }
        return true;
    }

    private void sendJsonItemMessage(Player player, ItemStack item){
        String json = JsonItemUtils.getItemJson(item);
        String text = ChatColor.DARK_GREEN + " Json-Item: " + ChatColor.RESET + json;
        if (SpigotUtils.isSpigotServer()){
            SpigotUtils.sendMessage(player, text, ChatColor.GREEN + "Click to copy", json);
        }else{
            player.sendMessage(text);
        }
    }

}