package com.unoriginal.beastslayer.recipe;

import com.google.common.collect.Sets;
import com.unoriginal.beastslayer.BeastSlayer;
import com.unoriginal.beastslayer.init.ModItems;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.Set;

//TODO Lock quality result
public class WitchcraftRecipeHandler {
    private ItemStack stack;
    private int expectedQuality;
    public World world;
    private static final Set<Item> Q1 = Sets.newHashSet(
            Items.FEATHER,
            Items.STRING,
           Items.BONE);
    private static final Set<Item> Q2 = Sets.newHashSet(
            ModItems.CURSED_WOOD,
            ModItems.CLOTH,
            ModItems.BROKEN_TALISMAN
    );
    private static final Set<Item> Q3 = Sets.newHashSet(
            ModItems.DARK_GOOP,
            ModItems.DUST
    );
    private static final Set<Item> Q4 = Sets.newHashSet(

            ModItems.WISP_BOTTLE,
            ModItems.TABLET
    );
    public WitchcraftRecipeHandler(World world){
        this.world = world;
        this.expectedQuality = 0;
    }

    public ItemStack getResults(List<ItemStack> parItemStack)
    {
        double quality = 0D;
        int count = 0;
        if(parItemStack.size() != 5){
            return ItemStack.EMPTY;
        }

        for(ItemStack stack : parItemStack){
            if(Q1.contains(stack.getItem())){
                quality = quality + 1D;
                count = count + 1;
            }
            if (Q2.contains(stack.getItem())) {
                quality = quality + 2D;
                count = count + 1;
            }
            if (Q3.contains(stack.getItem())) {
                quality = quality + 3D;
                count = count + 1;
            }
            if (Q4.contains(stack.getItem())) {
                quality = quality + 4D;
                count = count + 1;
            }

        }
        quality = Math.round(quality / 5D);
        long seed = this.world.getSeed();
        if(quality <= 0.5 || count != 5){
            return ItemStack.EMPTY;
        }
        if(this.getLastCraftablebyQuality((int) quality) != null){
            return this.stack;
        }
        else {
            return pickLootTable(quality, seed, null);
        }

    }

    public ItemStack pickLootTable(double quality, long seed, @Nullable String tag)
    {
        ItemStack result = ItemStack.EMPTY;
        ResourceLocation location = new ResourceLocation(BeastSlayer.MODID, "magic/witchcraft_" + (int) quality + (tag == null ? "" : tag));
        if(!this.world.isRemote) {
            LootTable loottable = this.world.getLootTableManager().getLootTableFromLocation(location);
            LootContext.Builder lootcontext$builder = new LootContext.Builder((WorldServer) this.world);
            List<ItemStack> itemlist = loottable.generateLootForPools(new Random(seed), lootcontext$builder.build());
            if (!itemlist.isEmpty()) {
                result = itemlist.get(new Random().nextInt(itemlist.size()));
                this.setLastCraftablebyQuality((int)quality, result);
            }
       }
        return result; //source of desync
    }

    public void setLastCraftablebyQuality(int quality, ItemStack stack){
        this.stack = stack;
        this.expectedQuality = quality;
    }
    public ItemStack getLastCraftablebyQuality(int quality){
        if(quality == this.expectedQuality && this.expectedQuality != 0){
            return this.stack;
        } else {
            return null;
        }
    }
}
