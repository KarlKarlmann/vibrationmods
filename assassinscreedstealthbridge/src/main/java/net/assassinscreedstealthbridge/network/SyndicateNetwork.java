package net.assassinscreedstealthbridge.network;

import net.assassinscreedstealthbridge.AssassinsCreedStealthBridge;
import net.assassinscreedstealthbridge.syndicate.SyndicateBoardManager;
import net.assassinscreedstealthbridge.syndicate.SyndicateDivision;
import net.assassinscreedstealthbridge.syndicate.SyndicateEvents;
import net.assassinscreedstealthbridge.syndicate.SyndicateMember;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

public class SyndicateNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AssassinsCreedStealthBridge.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, OpenBoardPacket.class, OpenBoardPacket::encode, OpenBoardPacket::new, OpenBoardPacket::handle);
        CHANNEL.registerMessage(id++, ProcessDecisionPacket.class, ProcessDecisionPacket::encode, ProcessDecisionPacket::new, ProcessDecisionPacket::handle);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static class OpenBoardPacket {
        public final CompoundTag boardData;

        public OpenBoardPacket(CompoundTag boardData) {
            this.boardData = boardData;
        }

        public OpenBoardPacket(FriendlyByteBuf buf) {
            this.boardData = buf.readNbt();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeNbt(boardData);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // WICHTIG: DistExecutor sorgt dafür, dass dieser Codeblock
                // vom Server beim Laden komplett ignoriert wird!
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handleOpenBoard(this.boardData));
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ProcessDecisionPacket {
        public final UUID memberId;
        public final String action;

        public ProcessDecisionPacket(UUID memberId, String action) {
            this.memberId = memberId;
            this.action = action;
        }

        public ProcessDecisionPacket(FriendlyByteBuf buf) {
            this.memberId = buf.readUUID();
            this.action = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUUID(memberId);
            buf.writeUtf(action);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                SyndicateBoardManager board = SyndicateBoardManager.get(player.serverLevel());
                
                // --- PUBLISH INTEL (Data Release) ---
                if ("publish_intel".equals(this.action)) {
                    SyndicateDivision division = board.getDivision(this.memberId.toString());
                    if (division == null) {
                        SyndicateMember m = board.getMember(this.memberId);
                        if (m != null) division = board.getDivision(m.divisionId);
                    }
                    
                    if (division != null && division.intel >= 100) {
                        // 1. Spawne den verzweifelten Angriff VOR DEM LÖSCHEN, damit die Daten übertragen werden können
                        // (Die Mobs speichern den Loot in ihrem NBT und droppen ihn beim physischen Tod)
                        SyndicateEvents.triggerDesperateAssault(player, player.serverLevel(), division);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§4[Syndicate] §fData published! The " + division.name + " are sending an elimination squad. A new faction takes their place..."));
                        
                        // 2. FRAKTION ROTIEREN (Wirft die alte Fraktion vom Board und bringt eine ungenutzte)
                        board.replaceDivision(division.id);
                        
                        player.closeContainer(); 
                        return;
                    }
                }

                // --- EXECUTE & INTERROGATE ---
                SyndicateMember member = board.getMember(this.memberId);
                if (member != null && player.getUUID().equals(member.capturedBy)) {
                    SyndicateDivision division = board.getDivision(member.divisionId);
                    String boxId = division != null ? division.rewardFlavor : "reward_box:default";
                    int oldRank = member.rank; 

                    if ("execute".equals(this.action)) {
                        board.executeMember(member);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[Syndicate] §fYou executed " + member.name + ". They grow stronger..."));
                        
                    } else if ("interrogate".equals(this.action)) {
                        board.interrogateMember(member);
                        
                        if (division != null) {
                            division.intel = Math.min(100, division.intel + (oldRank * 10));
                        }
                        
                        // Bei Verhören gibt es die Box sofort ins Inventar!
                        giveRewardBox(player, boxId, oldRank);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[Syndicate] §fYou interrogated " + member.name + " and stole their Reward Box."));
                    }

                    // GUI aktualisieren
                    sendToPlayer(new OpenBoardPacket(board.save(new CompoundTag())), player);
                }
            });
            ctx.get().setPacketHandled(true);
        }

        private void giveRewardBox(ServerPlayer player, String boxId, int tier) {
            Item boxItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("reward_box", "reward_chest"));
            if (boxItem == null || boxItem == Items.AIR) {
                AssassinsCreedStealthBridge.LOGGER.error("RewardBox mod is missing or item ID is wrong!");
                boxItem = Items.CHEST; // Fallback
            }
            
            ItemStack reward = new ItemStack(boxItem, 1);
            CompoundTag tag = reward.getOrCreateTag();
            tag.putString("BoxId", boxId);
            tag.putInt("RewardTier", tier);
            
            // Dem Item einen schönen Namen geben
            String readableName = boxId.replace("reward_box:", ""); 
            readableName = readableName.substring(0, 1).toUpperCase() + readableName.substring(1).replace("_", " "); 
            
            net.minecraft.network.chat.MutableComponent displayName = net.minecraft.network.chat.Component.literal("Tier " + tier + " " + readableName + " Box")
                .withStyle(net.minecraft.ChatFormatting.GOLD)
                .withStyle(net.minecraft.ChatFormatting.BOLD);
                
            reward.setHoverName(displayName);
            
            if (!player.getInventory().add(reward)) {
                player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), reward));
            }
        }
    }

    /**
     * Diese statische Hilfsklasse wird nur aufgerufen, wenn wir uns
     * wirklich auf dem physischen Client (Spieler-PC) befinden.
     */
    public static class ClientPacketHandler {
        public static void handleOpenBoard(CompoundTag boardData) {
            SyndicateBoardManager clientBoard = SyndicateBoardManager.load(boardData);
            // Direkte Verweise auf net.minecraft.client.* sind hier erlaubt!
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new net.assassinscreedstealthbridge.client.SyndicateBoardScreen(clientBoard)
            );
        }
    }
}