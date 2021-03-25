/*
 * FirstAid
 * Copyright (C) 2017-2021
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ichttt.mods.firstaid.common.util;

import com.google.common.collect.Iterators;
import com.google.common.math.DoubleMath;
import ichttt.mods.firstaid.FirstAid;
import ichttt.mods.firstaid.FirstAidConfig;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.ISpecialArmor;

import javax.annotation.Nonnull;

public class ArmorUtils {

    // Use attributes instead of fields on ItemArmor, these are likely more correct
    public static double getArmor(ItemStack stack, EntityEquipmentSlot slot) {
        return getValueFromAttributes(SharedMonsterAttributes.ARMOR, slot, stack);
    }

    public static double getArmorThoughness(ItemStack stack, EntityEquipmentSlot slot) {
        return getValueFromAttributes(SharedMonsterAttributes.ARMOR_TOUGHNESS, slot, stack);
    }

    public static double applyArmorModifier(EntityEquipmentSlot slot, double rawArmor) {
        if (rawArmor <= 0D)
            return 0D;
        rawArmor = rawArmor * getArmorMultiplier(slot);
        rawArmor += getArmorOffset(slot);
        return rawArmor;
    }

    public static double applyToughnessModifier(EntityEquipmentSlot slot, double rawToughness) {
        if (rawToughness <= 0D) return 0D;
        rawToughness = rawToughness * getToughnessMultiplier(slot);
        rawToughness += getToughnessOffset(slot);
        return rawToughness;
    }

    private static double getArmorMultiplier(EntityEquipmentSlot slot) {
        FirstAidConfig.LocationalArmor.Armor config = FirstAidConfig.locationalArmor.armor;
        switch (slot) {
            case HEAD:
                return config.headArmorMultiplier;
            case CHEST:
                return config.chestArmorMultiplier;
            case LEGS:
                return config.legsArmorMultiplier;
            case FEET:
                return config.feetArmorMultiplier;
            default:
                throw new IllegalArgumentException("Invalid slot " + slot);
        }
    }

    private static double getArmorOffset(EntityEquipmentSlot slot) {
        FirstAidConfig.LocationalArmor.Armor config = FirstAidConfig.locationalArmor.armor;
        switch (slot) {
            case HEAD:
                return config.headArmorOffset;
            case CHEST:
                return config.chestArmorOffset;
            case LEGS:
                return config.legsArmorOffset;
            case FEET:
                return config.feetArmorOffset;
            default:
                throw new IllegalArgumentException("Invalid slot " + slot);
        }
    }

    private static double getToughnessMultiplier(EntityEquipmentSlot slot) {
        FirstAidConfig.LocationalArmor.Thoughness config = FirstAidConfig.locationalArmor.thoughness;
        switch (slot) {
            case HEAD:
                return config.headThoughnessMultiplier;
            case CHEST:
                return config.chestThoughnessMultiplier;
            case LEGS:
                return config.legsThoughnessMultiplier;
            case FEET:
                return config.feetThoughnessMultiplier;
            default:
                throw new IllegalArgumentException("Invalid slot " + slot);
        }
    }

    private static double getToughnessOffset(EntityEquipmentSlot slot) {
        FirstAidConfig.LocationalArmor.Thoughness config = FirstAidConfig.locationalArmor.thoughness;
        switch (slot) {
            case HEAD:
                return config.headThoughnessOffset;
            case CHEST:
                return config.chestThoughnessOffset;
            case LEGS:
                return config.legsThoughnessOffset;
            case FEET:
                return config.feetThoughnessOffset;
            default:
                throw new IllegalArgumentException("Invalid slot " + slot);
        }
    }

    private static double getValueFromAttributes(IAttribute attribute, EntityEquipmentSlot slot, ItemStack stack) {
        return stack.getItem().getAttributeModifiers(slot, stack).get(attribute.getName()).stream().mapToDouble(AttributeModifier::getAmount).sum();
    }

    private static double getGlobalRestAttribute(EntityPlayer player, IAttribute attribute) {
        double sumOfAllAttributes = 0.0D;
        for (EntityEquipmentSlot slot : CommonUtils.ARMOR_SLOTS) {
            ItemStack otherStack = player.getItemStackFromSlot(slot);
            sumOfAllAttributes += getValueFromAttributes(attribute, slot, otherStack);
        }
        double all = player.getEntityAttribute(attribute).getAttributeValue();
        if (!DoubleMath.fuzzyEquals(sumOfAllAttributes, all, 0.001D)) {
            double diff = all - sumOfAllAttributes;
            if (FirstAidConfig.debug) {
                FirstAid.LOGGER.info("Attribute value for {} does not match sum! Diff is {}, distributing to all!", attribute.getName(), diff);
            }
            return diff;
        }
        return 0.0D;
    }

    /**
     * Changed copy of ISpecialArmor{@link ISpecialArmor.ArmorProperties#applyArmor(EntityLivingBase, NonNullList, DamageSource, double)}
     */
    public static float applyArmor(@Nonnull EntityPlayer entity, @Nonnull ItemStack itemStack, @Nonnull DamageSource source, double damage, @Nonnull EntityEquipmentSlot slot) {
        NonNullList<ItemStack> inventory = entity.inventory.armorInventory;

        double totalArmor = 0;
        double totalToughness = 0;
        Item item = itemStack.getItem();

        ISpecialArmor.ArmorProperties prop = null;
        boolean unblockable = source.isUnblockable();
        if (unblockable && item instanceof ISpecialArmor)
            unblockable = ((ISpecialArmor) item).handleUnblockableDamage(entity, itemStack, source, damage, slot.getIndex());
        if (unblockable)
            return (float) damage;
        if (item instanceof ISpecialArmor) {
            ISpecialArmor armor = (ISpecialArmor) item;
            prop = armor.getProperties(entity, itemStack, source, damage, slot.getIndex()).copy();
            totalArmor += prop.Armor;
            totalToughness += prop.Toughness;
        } else if (item instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) item;
            prop = new ISpecialArmor.ArmorProperties(0, 0, Integer.MAX_VALUE);
            prop.Armor = armor.damageReduceAmount;
            prop.Toughness = armor.toughness;
        }
        if (item instanceof ItemArmor) { //Always add normal armor (even if the item is a special armor), as forge does this as well
            totalArmor += getArmor(itemStack, slot);
            totalToughness += getArmorThoughness(itemStack, slot);
        }

        if (prop != null) {
            totalArmor = applyArmorModifier(slot, totalArmor);
            totalToughness = applyToughnessModifier(slot, totalToughness);

            prop.Slot = slot.getIndex();

            double ratio = prop.AbsorbRatio * getArmorMultiplier(slot);

            double absorb = damage * ratio;
            if (absorb > 0) {
                ItemStack stack = inventory.get(prop.Slot);
                int itemDamage = (int) Math.max(1, absorb);
                if (stack.getItem() instanceof ISpecialArmor)
                    ((ISpecialArmor) stack.getItem()).damageArmor(entity, stack, source, itemDamage, prop.Slot);
                else stack.damageItem(itemDamage, entity);
            }
            damage -= (damage * ratio);
        }
        totalArmor += getGlobalRestAttribute(entity, SharedMonsterAttributes.ARMOR);
        totalToughness += getGlobalRestAttribute(entity, SharedMonsterAttributes.ARMOR_TOUGHNESS);

        if (damage > 0 && (totalArmor > 0 || totalToughness > 0)) {
            double armorDamage = Math.max(1.0F, damage);

            if (item instanceof ItemArmor) itemStack.damageItem((int) armorDamage, entity);
            damage = CombatRules.getDamageAfterAbsorb((float) damage, (float) totalArmor, (float) totalToughness);
        }

        return (float) damage;
    }

    /**
     * Changed copy of the first part from {@link EnchantmentHelper#applyEnchantmentModifier(EnchantmentHelper.IModifier, ItemStack)}
     */
    public static float applyGlobalPotionModifiers(EntityPlayer player, DamageSource source, float damage) {
        if (source.isDamageAbsolute()) return damage;
        if (player.isPotionActive(MobEffects.RESISTANCE) && source != DamageSource.OUT_OF_WORLD) {
            @SuppressWarnings("ConstantConditions") int i = (player.getActivePotionEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
            int j = 25 - i;
            float f = damage * (float) j;
            damage = f / 25.0F;
        }

        if (damage <= 0.0F) return 0.0F;

        return damage;
    }

    /**
     * Changed copy of the second part from {@link EnchantmentHelper#applyEnchantmentModifier(EnchantmentHelper.IModifier, ItemStack)}
     */
    public static float applyEnchantmentModifiers(EntityPlayer player, EntityEquipmentSlot slot, DamageSource source, float damage) {
        if (source.isDamageAbsolute()) return damage;
        int k;
        if (FirstAidConfig.armorEnchantmentMode == FirstAidConfig.ArmorEnchantmentMode.LOCAL_ENCHANTMENTS) {
            k = EnchantmentHelper.getEnchantmentModifierDamage(() -> Iterators.singletonIterator(player.getItemStackFromSlot(slot)), source);
            k *= 4;
        } else if (FirstAidConfig.armorEnchantmentMode == FirstAidConfig.ArmorEnchantmentMode.GLOBAL_ENCHANTMENTS){
            k = EnchantmentHelper.getEnchantmentModifierDamage(player.getArmorInventoryList(), source);
        } else {
            throw new RuntimeException("What dark magic is " + FirstAidConfig.armorEnchantmentMode);
        }

        if (k > 0) damage = CombatRules.getDamageAfterMagicAbsorb(damage, (float) k);
        return damage;
    }
}
