package enchantEvolution;

import java.io.File;
import java.util.List;
import java.util.Random;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class main extends JavaPlugin{
	public static int failChance = 0;
	public static boolean consumeRune = false;
	public static boolean limitToWeapons = false;
	public static Configuration cfg = null;
	public static Economy econ = null;
	public static final String defaultHelpMessage = "/ee <player> <enchant name>";
	public static final String defaultPlayerErrorMessage = "The first parameter is not a player's IGN";
	public static int evolutionPrice = 0;
	public static boolean geometric = false;
	public static int geoFactor = 0;
	public static boolean replaceRune = false;
	public static Random rndGen = new Random();
	public static String socketLore = "";
	public static boolean sendMoneymsg = false;
	public static String runeTxt = "";
	@Override
	public void onEnable(){
		if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
		File f = getDataFolder();
		if (!f.exists()){
			f.mkdir();
			saveResource("config.yml", false);
		}
		cfg = this.getConfig();
		failChance = cfg.getInt("failChance");
		consumeRune = cfg.getBoolean("alwaysConsumeRune");
		limitToWeapons = cfg.getBoolean("limitToWeapons");
		evolutionPrice = cfg.getInt("price");
		geometric = cfg.getBoolean("geometricEvolution");
		geoFactor = cfg.getInt("geometricFactor");
		replaceRune = cfg.getBoolean("replaceRuneWithSocket");
		socketLore = cfg.getString("socketSpaceLore");
		sendMoneymsg = cfg.getBoolean("sendMoneyMessage");
		runeTxt = cfg.getString("runeText");
		getLogger().info("Plugin enabled");
	}
	@Override
	public void onDisable(){
		getLogger().info("Plugin disabled");
	}
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if (cmd.getName().equalsIgnoreCase("enchantevolve")){
			if (args.length != 2){
				sender.sendMessage(defaultHelpMessage);
				return true;
			}else{
				Player pl =Bukkit.getPlayer(args[0]);
				if (pl == null){
					sender.sendMessage(defaultPlayerErrorMessage);
					return true;
				}else{
					ItemStack item = pl.getItemInHand();
					if (limitToWeapons){
						//code to check if it's a weapon
					}
					Enchantment ench = Enchantment.getByName(args[1]);
					if (ench != null){
						int level = item.getEnchantmentLevel(ench);
						if (level < 10){
							EconomyResponse res = null;
							double price = 0.;
							if (geometric){
								price = Math.pow(geoFactor, level+1) * evolutionPrice;
							}else{
								price= (level+1) * evolutionPrice;
							}
							res = econ.withdrawPlayer((Player) sender, price);
							if (res.transactionSuccess()){
								if (sendMoneymsg){
									sender.sendMessage(price + " have been taken from your account.");
								}
								boolean hasRune = false;
								ItemMeta meta = null;
								List<String> lore = null;
								boolean success = (rndGen.nextInt(100) + 1 <= failChance);
								if (item.hasItemMeta()){
									meta = item.getItemMeta();
									lore = meta.getLore();
									if (lore.contains(runeTxt)){
										hasRune = true;
									}
								}
								if (hasRune){
									if (consumeRune || !success){
										lore.remove(runeTxt);
										lore.add(socketLore);
										meta.setLore(lore);
										item.setItemMeta(meta);
									}
								}
								if (success){
									item.addUnsafeEnchantment(ench, level+1);
									sender.sendMessage("Enchantment Completed with success!");
								}else{
									if (!hasRune){
										item.removeEnchantment(ench);
										sender.sendMessage("Enchantment Failed, your enchantment was destroyed");
									}else{
										sender.sendMessage("Enchantment Failed, The Rune of Protection protected your enchantment");
									}
								}
								pl.setItemInHand(item);
								item = null;
								meta = null;
								lore = null;
							}else{
								sender.sendMessage("You don't have enough money to do that!");
							}
							return true;
						}else{
							sender.sendMessage("This enchantment is capped");
						}
					}else{
						sender.sendMessage("Enchantment name Not Valid");
					}
					return true;
				}
			}
		}
		return false;
	}
}
