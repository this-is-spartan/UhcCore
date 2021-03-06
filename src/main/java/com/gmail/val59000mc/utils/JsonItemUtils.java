package com.gmail.val59000mc.utils;

import com.gmail.val59000mc.exceptions.ParseException;
import com.google.gson.*;
import com.google.gson.JsonArray;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.*;

public class JsonItemUtils{

    public static String getItemJson(ItemStack item){
        JsonObject json = new JsonObject();
        json.addProperty("type", item.getType().toString());
        if (item.getAmount() != 1){
            json.addProperty("amount", item.getAmount());
        }
        if (item instanceof JsonItemStack){
            JsonItemStack jsonItem = (JsonItemStack) item;

            if (jsonItem.getMaximum() != 1){
                json.addProperty("minimum", jsonItem.getMinimum());
                json.addProperty("maximum", jsonItem.getMaximum());
                // Amount is random so not needed.
                json.remove("amount");
            }
        }
        if (item.getDurability() != 0){
            json.addProperty("durability", item.getDurability());
        }
        if (item.hasItemMeta()){
            ItemMeta meta = item.getItemMeta();

            if (meta.hasDisplayName()){
                json.addProperty("display-name", meta.getDisplayName().replace('\u00a7', '&'));
            }
            if (meta.hasLore()){
                JsonArray lore = new JsonArray();
                meta.getLore().forEach(line -> lore.add(new JsonPrimitive(line.replace('\u00a7', '&'))));
                json.add("lore", lore);
            }
            if (meta.hasEnchants()){
                Map<Enchantment, Integer> enchantments = meta.getEnchants();
                JsonArray jsonEnchants = new JsonArray();
                for (Enchantment enchantment : enchantments.keySet()){
                    JsonObject jsonEnchant = new JsonObject();
                    jsonEnchant.addProperty("type", VersionUtils.getVersionUtils().getEnchantmentKey(enchantment));
                    jsonEnchant.addProperty("level", enchantments.get(enchantment));
                    jsonEnchants.add(jsonEnchant);
                }
                json.add("enchantments", jsonEnchants);
            }

            JsonObject attributes = VersionUtils.getVersionUtils().getItemAttributes(meta);
            if (attributes != null){
                json.add("attributes", attributes);
            }

            if (meta instanceof PotionMeta){
                PotionMeta potionMeta = (PotionMeta) meta;

                JsonObject baseEffect = VersionUtils.getVersionUtils().getBasePotionEffect(potionMeta);
                if (baseEffect != null) {
                    json.add("base-effect", baseEffect);
                }

                if (!potionMeta.getCustomEffects().isEmpty()) {
                    JsonArray customEffects = new JsonArray();

                    for (PotionEffect effect : potionMeta.getCustomEffects()) {
                        JsonObject jsonEffect = new JsonObject();
                        jsonEffect.addProperty("type", effect.getType().getName());
                        jsonEffect.addProperty("duration", effect.getDuration());
                        jsonEffect.addProperty("amplifier", effect.getAmplifier());
                        customEffects.add(jsonEffect);
                    }
                    json.add("custom-effects", customEffects);
                }
            }
            else if (meta instanceof EnchantmentStorageMeta){
                EnchantmentStorageMeta enchantmentMeta = (EnchantmentStorageMeta) meta;
                Map<Enchantment, Integer> enchantments = enchantmentMeta.getStoredEnchants();
                JsonArray jsonEnchants = new JsonArray();
                for (Enchantment enchantment : enchantments.keySet()){
                    JsonObject jsonEnchant = new JsonObject();
                    jsonEnchant.addProperty("type", VersionUtils.getVersionUtils().getEnchantmentKey(enchantment));
                    jsonEnchant.addProperty("level", enchantments.get(enchantment));
                    jsonEnchants.add(jsonEnchant);
                }
                json.add("enchantments", jsonEnchants);
            }
        }
        return json.toString();
    }

    public static JsonItemStack getItemFromJson(String jsonString) throws ParseException{
        JsonObject json;
        try{
            json = new JsonParser().parse(jsonString).getAsJsonObject();
        }catch (JsonSyntaxException | IllegalStateException ex){
            throw new ParseException("There is an error in the json syntax of item: " + jsonString);
        }

       Material material;

       try{
           material = Material.valueOf(json.get("type").getAsString());
       }catch (IllegalArgumentException ex){
           throw new ParseException("Invalid item type: " + json.get("type").getAsString() + " does the item exist on this minecraft version?");
       }

       JsonItemStack item = new JsonItemStack(material);
       ItemMeta meta = item.getItemMeta();

       try{
            for (Map.Entry<String, JsonElement> entry : json.entrySet()){
                switch (entry.getKey()){
                    case "type":
                        continue;
                    case "amount":
                        item.setAmount(entry.getValue().getAsInt());
                        break;
                    case "maximum":
                        item.setMaximum(entry.getValue().getAsInt());
                        break;
                    case "minimum":
                        item.setMinimum(entry.getValue().getAsInt());
                        break;
                    case "durability":
                        item.setItemMeta(meta);
                        item.setDurability(entry.getValue().getAsShort());
                        meta = item.getItemMeta();
                        break;
                    case "display-name":
                        meta.setDisplayName(entry.getValue().getAsString());
                        break;
                    case "lore":
                        meta = parseLore(meta, entry.getValue().getAsJsonArray());
                        break;
                    case "enchantments":
                        meta = parseEnchantments(meta, entry.getValue().getAsJsonArray());
                        break;
                    case "base-effect":
                        meta = parseBasePotionEffect(meta, entry.getValue().getAsJsonObject());
                        break;
                    case "custom-effects":
                        meta = parseCustomPotionEffects(meta, entry.getValue().getAsJsonArray());
                        break;
                    case "attributes":
                        meta = VersionUtils.getVersionUtils().applyItemAttributes(meta, entry.getValue().getAsJsonObject());
                }
            }

            item.setItemMeta(meta);
            return item;
        }catch (Exception ex){
            if (ex instanceof ParseException){
                throw ex;
            }

            ParseException exception = new ParseException(ex.getMessage());
            ex.setStackTrace(ex.getStackTrace());
            throw exception;
        }
    }

    private static ItemMeta parseLore(ItemMeta meta, JsonArray jsonArray){
        Iterator<JsonElement> lines = jsonArray.iterator();
        List<String> lore = new ArrayList<>();
        while (lines.hasNext()){
            lore.add(ChatColor.translateAlternateColorCodes('&', lines.next().getAsString()));
        }
        meta.setLore(lore);
        return meta;
    }

    private static ItemMeta parseEnchantments(ItemMeta meta, JsonArray jsonArray){
        EnchantmentStorageMeta enchantmentMeta = null;

        if (meta instanceof EnchantmentStorageMeta){
            enchantmentMeta = (EnchantmentStorageMeta) meta;
        }

        for (JsonElement jsonElement : jsonArray) {
            JsonObject enchant = jsonElement.getAsJsonObject();

            Enchantment enchantment = VersionUtils.getVersionUtils().getEnchantmentFromKey(enchant.get("type").getAsString());
            Validate.notNull(enchantment, "Unknown enchantment type: " + enchant.get("type").getAsString());

            int level = enchant.get("level").getAsInt();
            if (enchantmentMeta == null){
                meta.addEnchant(enchantment, level, true);
            }else{
                enchantmentMeta.addStoredEnchant(enchantment, level, true);
            }
        }
        return enchantmentMeta == null ? meta : enchantmentMeta;
    }

    private static ItemMeta parseBasePotionEffect(ItemMeta meta, JsonObject jsonObject) throws ParseException{
        PotionMeta potionMeta = (PotionMeta) meta;

        PotionType type;

        try {
            type = PotionType.valueOf(jsonObject.get("type").getAsString());
        }catch (IllegalArgumentException ex){
            throw new ParseException(ex.getMessage());
        }

        JsonElement jsonElement;
        jsonElement = jsonObject.get("extended");
        boolean extended = jsonElement != null && jsonElement.getAsBoolean();
        jsonElement = jsonObject.get("upgraded");
        boolean upgraded = jsonElement != null && jsonElement.getAsBoolean();

        PotionData potionData = new PotionData(type, extended, upgraded);
        potionMeta = VersionUtils.getVersionUtils().setBasePotionEffect(potionMeta, potionData);

        return potionMeta;
    }

    private static ItemMeta parseCustomPotionEffects(ItemMeta meta, JsonArray jsonArray) throws ParseException{
        PotionMeta potionMeta = (PotionMeta) meta;
        Iterator<JsonElement> effects = jsonArray.iterator();

        while (effects.hasNext()){
            JsonObject effect = effects.next().getAsJsonObject();
            PotionEffectType type = PotionEffectType.getByName(effect.get("type").getAsString());

            if (type == null){
                throw new ParseException("Invalid potion type: " + effect.get("type").getAsString());
            }

            int duration;
            int amplifier;

            try {
                duration = effect.get("duration").getAsInt();
                amplifier = effect.get("amplifier").getAsInt();
            }catch (NullPointerException ex){
                throw new ParseException("Missing duration or amplifier tag for: " + effect.toString());
            }

            PotionEffect potionEffect = new PotionEffect(type, duration, amplifier);
            potionMeta.addCustomEffect(potionEffect, true);
        }

        return potionMeta;
    }

}