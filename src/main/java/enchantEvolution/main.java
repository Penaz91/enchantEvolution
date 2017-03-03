package enchantEvolution;

import java.io.File;
//import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
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
	public static boolean geometric = false;
	public static int geoFactor = 0;
	public static boolean replaceRune = false;
	public static Random rndGen = new Random();
	public static String socketLore = "";
	public static boolean sendMoneymsg = false;
	public static boolean lowerLevel = false;
	public static String runeTxt = "";
	public static String NFString = "";
	public static ConfigurationSection limits = null;
	public static ConfigurationSection prices = null;
	public static final Material[] weaponarr = {Material.WOOD_AXE, Material.WOOD_HOE, Material.WOOD_PICKAXE, Material.WOOD_SPADE, Material.WOOD_SWORD, Material.GOLD_AXE, Material.GOLD_BOOTS, Material.GOLD_CHESTPLATE, Material.GOLD_HELMET, Material.GOLD_HOE, Material.GOLD_LEGGINGS, Material.GOLD_PICKAXE, Material.GOLD_SPADE, Material.GOLD_SWORD, Material.IRON_AXE, Material.IRON_BOOTS, Material.IRON_CHESTPLATE, Material.IRON_HELMET, Material.IRON_HOE, Material.IRON_LEGGINGS, Material.IRON_PICKAXE, Material.IRON_SPADE, Material.IRON_SWORD,Material.DIAMOND_AXE, Material.DIAMOND_BOOTS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET, Material.DIAMOND_HOE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_PICKAXE, Material.DIAMOND_SPADE, Material.DIAMOND_SWORD, Material.LEATHER_BOOTS, Material.LEATHER_CHESTPLATE, Material.LEATHER_HELMET,Material.LEATHER_LEGGINGS, Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_HELMET, Material.CHAINMAIL_LEGGINGS, Material.BOW, Material.FISHING_ROD, Material.SHIELD, Material.ELYTRA};
	public static final String pluginName = ChatColor.DARK_RED + "[" + ChatColor.DARK_PURPLE + "EnchantEvolution" + ChatColor.DARK_RED + "]" + ChatColor.RESET + " ";
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
		prices = cfg.getConfigurationSection("prices");
		geometric = cfg.getBoolean("geometricEvolution");
		geoFactor = cfg.getInt("geometricFactor");
		replaceRune = cfg.getBoolean("replaceRuneWithSocket");
		socketLore = cfg.getString("socketSpaceLore");
		sendMoneymsg = cfg.getBoolean("sendMoneyMessage");
		runeTxt = cfg.getString("runeText");
		NFString = cfg.getString("nonForgeText");
		limits = cfg.getConfigurationSection("levelCaps");
		lowerLevel=cfg.getBoolean("lowerLevel");
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
				getLogger().info(defaultHelpMessage);
				return true;
			}else{
				@SuppressWarnings("deprecation")
				Player pl =Bukkit.getPlayerExact(args[0]);
				if (pl == null){
					getLogger().info(pluginName + defaultPlayerErrorMessage + ": " + args[0]);
					return true;
				}else{
					ItemStack item = pl.getInventory().getItemInMainHand();
					if (item.hasItemMeta()){
						if (item.getItemMeta().hasLore()){
							for (String l: item.getItemMeta().getLore()){
								String cleanLore = ChatColor.stripColor(l);
								if (cleanLore.equalsIgnoreCase(NFString)){
									pl.sendMessage(pluginName + "This item's enchantments are not forgeable");
									return true;
								}
							}
						}
					}
					if (limitToWeapons){
						Material type = item.getData().getItemType();
						boolean found = false;
						for (Material it: weaponarr){
							if (type.compareTo(it) == 0){
								found = true;
							}
						}
						if (!found){
							pl.sendMessage(pluginName + "This item's enchantments are not forgeable");
							return true;
						}
					}
					Enchantment ench = Enchantment.getByName(args[1]);
					if (ench != null){
						int level = item.getEnchantmentLevel(ench);
						int cap = limits.getInt(ench.getName());
						System.out.println("Enchantment " + ench.toString() + " level cap: " + cap);
						if (level < cap){
							EconomyResponse res = null;
							double price = 0.;
							double evolutionPrice = prices.getDouble(ench.getName());
							if (geometric){
								price = Math.pow(geoFactor, level+1) * evolutionPrice;
							}else{
								price= (level+1) * evolutionPrice;
							}
							res = econ.withdrawPlayer(pl, price);
							if (res.transactionSuccess()){
								if (sendMoneymsg){
									pl.sendMessage(pluginName +price + " have been taken from your account.");
								}
								boolean hasRune = false;
								ItemMeta meta = null;
								List<String> lore = null;
								boolean success = (rndGen.nextInt(100) + 1 >= failChance);
								int toRemove = 0;
								if (item.hasItemMeta()){
									meta = item.getItemMeta();
									if (meta.hasLore()){
										lore = meta.getLore();
										for (String l: lore){
											String cleanLore = ChatColor.stripColor(l);
											if (cleanLore.equalsIgnoreCase(runeTxt)){
												toRemove = lore.indexOf(l);
												hasRune = true;
											}
										}
									}
								}
								if (hasRune){
									if (consumeRune || !success){
										lore.remove(lore.get(toRemove));
										if (replaceRune){
											lore.add(ChatColor.translateAlternateColorCodes('&', socketLore));
										}
										meta.setLore(lore);
										item.setItemMeta(meta);
									}
								}
								if (success){
									item.addUnsafeEnchantment(ench, level+1);
									pl.sendMessage(pluginName + "Enchantment Forging Completed with success!");
									return true;
								}else{
									if (!hasRune){
										item.removeEnchantment(ench);
										if(lowerLevel && level>0){
											item.addUnsafeEnchantment(ench, Math.max(level-1,0));
											pl.sendMessage(pluginName + "Enchantment Forging Failed, your enchantment was damaged");
										}else{
											pl.sendMessage(pluginName + "Enchantment Forging Failed, your enchantment was destroyed");
										}
									}else{
										pl.sendMessage(pluginName + "Enchantment Forging Failed, '"+runeTxt+"' protected your enchantment");
									}
								}
								pl.getInventory().setItemInMainHand(item);
								item = null;
								meta = null;
								lore = null;
								return true;
							}else{
								pl.sendMessage(pluginName+"You don't have enough money to do that!");
								return true;
							}
						}else{
							pl.sendMessage(pluginName+"This enchantment is capped");
							return true;
						}
					}else{
						pl.sendMessage(pluginName+"Enchantment name Not Valid");
						return true;
					}
				}
			}
		}
		return false;
	}
}
