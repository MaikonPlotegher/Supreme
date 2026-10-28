package com.github.relativobr.supreme.util;

import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItemStack;
import lombok.experimental.UtilityClass;
import org.bukkit.inventory.ItemStack;

/**
 * Utilitário para montar arrays de receita ({@code ItemStack[]}) a partir de uma lista que mistura
 * {@link SlimefunItemStack}, {@link ItemStack} e {@code null}.
 * <p>
 * No Slimefun5, {@link SlimefunItemStack} não extende mais {@link ItemStack} (usa composição via
 * {@code .item()}), então não é possível colocar um {@link SlimefunItemStack} diretamente num
 * array {@code ItemStack[]}. Este helper evita ter que chamar {@code .item()} manualmente em cada
 * elemento de cada receita do addon.
 */
@UtilityClass
public class RecipeUtil {

    public static ItemStack[] recipe(Object... items) {
        ItemStack[] result = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            Object o = items[i];
            if (o == null) {
                result[i] = null;
            } else if (o instanceof SlimefunItemStack) {
                result[i] = ((SlimefunItemStack) o).item();
            } else if (o instanceof ItemStack) {
                result[i] = (ItemStack) o;
            } else {
                throw new IllegalArgumentException("Item de receita inválido: " + o);
            }
        }
        return result;
    }

}