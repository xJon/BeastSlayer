package com.unoriginal.beastslayer.entity.Entities;

import com.unoriginal.beastslayer.init.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

public class AbstractTribesmen extends EntityMob {
    private ResourceLocation sellingTable;
    private long sellingTableSeed;
    public int fieryTicks;
    protected static final DataParameter<Boolean> FIERY = EntityDataManager.createKey(AbstractTribesmen.class, DataSerializers.BOOLEAN);
    protected static final DataParameter<Boolean> TRADING = EntityDataManager.createKey(AbstractTribesmen.class, DataSerializers.BOOLEAN);
    private EntityAITempt aiTempt;
    public int tradeTicks;

    public AbstractTribesmen(World worldIn) {
        super(worldIn);
        this.setFiery(false);
        this.isImmuneToFire = this.isFiery();
    }

    protected void entityInit()
    {
        super.entityInit();
        this.dataManager.register(FIERY, false);
        this.dataManager.register(TRADING, false);
    }

    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAISwimming(this));
        aiTempt = new EntityAITempt(this, 0.6D, ModItems.CURSED_WOOD, true);
        this.tasks.addTask(3, this.aiTempt);
        this.tasks.addTask(6, new EntityAIWanderAvoidWater(this, 0.7D));
        this.tasks.addTask(7, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(2, new EntityAIAvoidEntity<>(this, AbstractTribesmen.class, p_apply_1_ -> p_apply_1_.isFiery() && p_apply_1_ != this && !this.isFiery(),this.avoidDistance(20F), 1.0F, 1.5F));
        this.targetTasks.addTask(0, new EntityAINearestAttackableTarget<>(this, AbstractTribesmen.class, 10, true, false, p_apply_1_ -> !p_apply_1_.isFiery() && this.isFiery()));
        this.targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this,EntityPlayer.class, 10, true, false, p_apply_1_ -> this.isFiery()));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityAnimal.class, 10, true, false, p_apply_1_ -> this.isFiery()));
    }

    public void onUpdate() {
        super.onUpdate();
        if(this.isBurning() && !this.isFiery())
        {
            this.setFiery(true);
            this.fieryTicks = 20;
            this.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 30, 10, false, false));
            this.world.setEntityState(this, (byte)5);
            if (this.world.isRemote)
            {
                for (int i = 0; i < 30; ++i)
                {
                    this.world.spawnParticle(EnumParticleTypes.FLAME, this.posX + (this.rand.nextDouble() - 0.5D) * (double)this.width, this.posY + this.rand.nextDouble() * (double)this.height + 0.25D, this.posZ + (this.rand.nextDouble() - 0.5D) * (double)this.width, (this.rand.nextDouble() - 0.5D) * 0.5D, -this.rand.nextDouble(), (this.rand.nextDouble() - 0.5D) * 0.5D);
                }
            }
            this.setRevengeTarget(null);
            this.getHeldItemMainhand().setCount(0);
            this.getHeldItemOffhand().setCount(0);
            this.extinguish();
        }
        if(this.fieryTicks > 0){
            --this.fieryTicks;
        }
        if(this.isFiery()){
            if(this.isWet()) {
                this.attackEntityFrom(DamageSource.DROWN, 1.0F);
            }
            if(this.getAttackTarget() instanceof AbstractTribesmen && !this.world.isRemote)
            {
                AbstractTribesmen tribesmen = (AbstractTribesmen) this.getAttackTarget();
                if(tribesmen.isFiery()){
                    this.setAttackTarget(null);
                    this.setRevengeTarget(null);
                }
            }
            if(this.isBurning()){
                this.heal(1.0F);
            }
            if(this.world.isRemote){
                for (int i = 0; i < 1; ++i)
                {
                    double d0 = this.posX + rand.nextDouble() - 0.5D;
                    double d1 = this.posY + rand.nextDouble() * 0.5D + 0.5D;
                    double d2 = this.posZ + rand.nextDouble() - 0.5D;
                    this.world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, d0, d1, d2, 0.0D, 0.0D, 0.0D);
                    this.world.spawnParticle(EnumParticleTypes.FLAME, d0, d1, d2, 0D, 0D, 0D);
                }
            }
        }
        if(this.isTrading() && !this.isFiery()){
            if(--this.tradeTicks <= 0 && !this.world.isRemote){
                this.dropBartering();
                this.swingArm(EnumHand.MAIN_HAND);
                this.setTrading(false);
                this.getHeldItemMainhand().setCount(0);
                Item item = this.getHeldItemOffhand().getItem();
                this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(item));
                this.getHeldItemOffhand().setCount(0);
            }
        }
        if(this.getAttackTarget() instanceof AbstractTribesmen && !this.isFiery())
        {
            AbstractTribesmen tribesmen = (AbstractTribesmen)this.getAttackTarget();
            if(!tribesmen.isFiery() && !this.world.isRemote){
                this.setAttackTarget(null);
                this.setRevengeTarget(null);
            }
        }
    }
    public void onCollideWithPlayer(EntityPlayer entityIn)
    {
        super.onCollideWithPlayer(entityIn);
        if(!this.world.isRemote && shouldTradeWithplayer(entityIn)){
            this.getHeldItemMainhand();
            Item item = this.getHeldItemMainhand().getItem();

            this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(item));
            entityIn.getHeldItemMainhand().shrink(1);
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(ModItems.CURSED_WOOD));
            this.tradeTicks = 100;
            this.setTrading(true);
        }
    }

    public boolean shouldTradeWithplayer(EntityPlayer player){
        return player.getHeldItemMainhand().getItem() == ModItems.CURSED_WOOD && !this.isFiery() && !this.isTrading();
    }

    public boolean isFiery()
    {

        return this.dataManager.get(FIERY);
    }

    protected void setFiery(boolean f)
    {
        this.dataManager.set(FIERY, f);
    }

    public boolean attackEntityAsMob(Entity entityIn)
    {
        boolean flag = super.attackEntityAsMob(entityIn);

        if (flag && this.getHeldItemMainhand().isEmpty() && entityIn instanceof EntityLivingBase)
        {
            if(this.isFiery()){
                entityIn.setFire(2);
            }
        }
        return flag;
    }

    public boolean isTrading()
    {

        return this.dataManager.get(TRADING);
    }

    protected void setTrading(boolean t)
    {
        this.dataManager.set(TRADING, t);
    }


    @Nullable
    protected ResourceLocation getBarteringTable()
    {
        return null;
    }

    protected void dropBartering()
    {
        ResourceLocation resourcelocation = this.sellingTable;

        if (resourcelocation == null)
        {
            resourcelocation = this.getBarteringTable();
        }

        if (resourcelocation != null) {
            LootTable loottable = this.world.getLootTableManager().getLootTableFromLocation(resourcelocation);
            this.sellingTable = null;
            LootContext.Builder lootcontext$builder = (new LootContext.Builder((WorldServer) this.world)).withLootedEntity(this);

            List<ItemStack> itemlist = loottable.generateLootForPools(this.sellingTableSeed == 0L ? this.rand : new Random(this.sellingTableSeed), lootcontext$builder.build());
            if (itemlist.size() > 0){
                ItemStack itemstack = itemlist.get(0);
                this.entityDropItem(itemstack, 0.0F);
            }


        }
    }
    @Override
    public boolean canDespawn(){
        return false;
    }


    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if (this.sellingTable != null)
        {
            compound.setString("SellingTable", this.sellingTable.toString());

            if (this.sellingTableSeed != 0L)
            {
                compound.setLong("SellingTableSeed", this.sellingTableSeed);
            }
        }
        compound.setBoolean("Fiery", this.isFiery());
        compound.setBoolean("Trading", this.isTrading());

        compound.setInteger("TradeTicks", this.tradeTicks);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("SellingTable", 8))
        {
            this.sellingTable = new ResourceLocation(compound.getString("SellingTable"));
            this.sellingTableSeed = compound.getLong("SellingTableSeed");
        }
        if(compound.hasKey("Fiery")){
            this.dataManager.set(FIERY, compound.getBoolean("Fiery"));
        }
        if(compound.hasKey("Trading")){
            this.dataManager.set(TRADING, compound.getBoolean("Trading"));
        }
        if(compound.hasKey("TradeTicks")){
            this.tradeTicks = compound.getInteger("TradeTicks");
        }
    }

    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender()
    {
        return this.isFiery() ? 15728880 : super.getBrightnessForRender();
    }

    public float getBrightness()
    {
        return this.isFiery() ? 1.0F : super.getBrightness();
    }

    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id) {
        if (id == 5) {
            this.fieryTicks = 30;

            for (int i = 0; i < 50; ++i)
            {
                this.world.spawnParticle(EnumParticleTypes.FLAME, this.posX + (this.rand.nextDouble() - 0.5D) * (double)this.width, this.posY + this.rand.nextDouble() * (double)this.height + 0.5D, this.posZ + (this.rand.nextDouble() - 0.5D) * (double)this.width, (this.rand.nextDouble() - 0.5D) * 0.5D, -this.rand.nextDouble(), (this.rand.nextDouble() - 0.5D) * 0.5D);
            }

        }
        else {
            super.handleStatusUpdate(id);
        }
    }

    public boolean attackEntityFrom(DamageSource source, float amount)
    {
        if (this.isEntityInvulnerable(source))
        {
            return false;
        }
        else
        {
            if(!this.isFiery()) {
                List<AbstractTribesmen> tribesmen = this.world.getEntitiesWithinAABB(AbstractTribesmen.class, this.getEntityBoundingBox().grow(10D));
                for(AbstractTribesmen t : tribesmen){
                    if(!t.isFiery() && !(source.getTrueSource() instanceof AbstractTribesmen)){
                        if (source.getTrueSource() instanceof EntityLivingBase) {
                            EntityLivingBase l = (EntityLivingBase) source.getTrueSource();
                            t.setAttackTarget(l);
                        }
                        if (source.getTrueSource() instanceof EntityPlayer){
                            EntityPlayer p = (EntityPlayer) source.getTrueSource();
                            if (!p.isCreative()){
                                t.setAttackTarget(p);
                            }
                        }
                    }
                }
            }
            if(this.isFiery()) {
                List<AbstractTribesmen> tribesmen = this.world.getEntitiesWithinAABB(AbstractTribesmen.class, this.getEntityBoundingBox().grow(10D));
                for(AbstractTribesmen t : tribesmen){
                    if(t.isFiery() && source.getTrueSource() instanceof EntityLivingBase){
                        EntityLivingBase l = (EntityLivingBase)source.getTrueSource();
                        t.setAttackTarget(l);
                    }
                }
            }
            return super.attackEntityFrom(source, amount);
        }
    }

    public float avoidDistance(float distance){
        return distance;
    }

    @SideOnly(Side.CLIENT)
    public int getFieryTicks(){
        return this.fieryTicks;
    }
}
