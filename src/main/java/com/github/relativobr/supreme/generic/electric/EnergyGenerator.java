package com.github.relativobr.supreme.generic.electric;

import com.github.relativobr.supreme.Supreme;
import com.github.relativobr.supreme.util.UtilEnergy;
import io.github.thebusybiscuit.slimefun5.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun5.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun5.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun5.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun5.implementation.items.electric.AbstractEnergyProvider;
import io.github.thebusybiscuit.slimefun5.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun5.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun5.utils.ChestMenuUtils;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Migrado de {@code extends MenuBlock} (net.guizhanss.guizhanlib.slimefun.machines.MenuBlock,
 * que ainda importa pacotes do Slimefun4 e por isso não compila com o Supreme/Slimefun5)
 * para {@code extends AbstractEnergyProvider}, a superclasse nativa do Slimefun5 usada pelo
 * próprio {@code AGenerator}. Diferente do {@code AGenerator}, este gerador não consome
 * combustível (itens) — a energia é calculada a partir de condições do mundo via
 * {@link GenerationType#generate(org.bukkit.World, Block, int)} — então não há slots de
 * input/output de item, e o {@link BlockMenuPreset} é montado manualmente no construtor
 * (mesmo padrão usado por {@code GenericMachine} e por {@code AGenerator}).
 */
public final class EnergyGenerator extends AbstractEnergyProvider {

  private int energy;
  private int buffer;
  /** Estado por bloco (generate, currentDelay). Antes era compartilhado entre todos os geradores do mesmo tipo. */
  private final Map<String, int[]> states = new ConcurrentHashMap<>();
  private GenerationType type;

  public EnergyGenerator(ItemGroup categories, SlimefunItemStack item, ItemStack[] recipe) {
    super(categories, item, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);

    new BlockMenuPreset(getId(), getInventoryTitle()) {

      @Override
      public void init() {
        optOutOfHeaderItem();
        drawBackground(new int[] {
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 10, 11, 12, 13, 14, 15, 16, 17,
                18, 19, 20, 21, 22, 23, 24, 25, 26
        });
      }

      @Override
      public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
        // Desenha o status imediatamente ao abrir, sem esperar o próximo tick.
        int[] state = states.get(key(b.getLocation()));
        updateMenu(menu, b.getLocation(), state == null ? 0 : state[0]);
      }

      @Override
      public boolean canOpen(Block b, Player p) {
        return p.hasPermission("slimefun.inventory.bypass") || Slimefun.getProtectionManager()
                .hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
      }

      @Override
      public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
        // Este gerador não tem slots de item (nem input nem output).
        return new int[0];
      }
    };

    registerDefaultFuelTypes();
  }

  public GenerationType getType() {
    return type;
  }

  public EnergyGenerator setType(GenerationType value) {
    this.type = value;
    return this;
  }

  public EnergyGenerator setEnergy(int value) {
    this.energy = value;
    return this;
  }

  public EnergyGenerator setBuffer(int value) {
    this.buffer = value;
    return this;
  }
  @Nonnull
  @Override
  public ItemStack getProgressBar() {
    return new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
  }

  @Override
  public int getEnergyProduction() {
    return this.energy;
  }

  @Override
  protected void registerDefaultFuelTypes() {
    // Gerador não consome combustível (itens); nada a registrar aqui.
  }

  @Override
  public int[] getInputSlots() {
    return new int[0];
  }

  @Override
  public int[] getOutputSlots() {
    return new int[0];
  }

  @Override
  public int getGeneratedOutput(Location l, Config data) {

    int[] state = states.computeIfAbsent(key(l), k -> new int[2]); // [0]=generate, [1]=currentDelay

    if (state[0] > 0 && (state[1] < Supreme.getSupremeOptions().getDelayTimeValidGenerators())) {
      state[1]++;
    } else {
      // check block
      state[0] = this.type.generate(l.getWorld(), l.getBlock(), this.energy);
      state[1] = 0;
    }

    BlockMenu inv = BlockStorage.getInventory(l);
    if (inv != null && inv.hasViewer()) {
      updateMenu(inv, l, state[0]);
    }

    return state[0];
  }

  private void updateMenu(@Nonnull BlockMenu inv, @Nonnull Location l, int generate) {
    if (generate == 0) {
      inv.replaceExistingItem(13, CustomItemStack.create(
              Material.RED_STAINED_GLASS_PANE,
              "&cNot generating",
              "&7Type: &6" + this.type,
              "&7Stored: &6" + UtilEnergy.format(getCharge(l)) + " J",
              "&7Capacity: &6" + UtilEnergy.format(this.buffer) + " J"
      ));
    } else {
      inv.replaceExistingItem(13, CustomItemStack.create(
              Material.GREEN_STAINED_GLASS_PANE,
              "&aGeneration",
              "&7Type: &6" + this.type,
              "&7Generating: &6" + UtilEnergy.format(generate) + " J/tick ",
              "&7Stored: &6" + UtilEnergy.format(getCharge(l)) + " J",
              "&7Capacity: &6" + UtilEnergy.format(this.buffer) + " J"
      ));
    }
  }

  private static String key(@Nonnull Location l) {
    return l.getWorld().getName() + ';' + l.getBlockX() + ';' + l.getBlockY() + ';' + l.getBlockZ();
  }

  public int getCapacity() {
    return this.buffer;
  }

}