package net.assassinscreedstealthbridge.client;

import net.assassinscreedstealthbridge.network.SyndicateNetwork;
import net.assassinscreedstealthbridge.syndicate.SyndicateBoardManager;
import net.assassinscreedstealthbridge.syndicate.SyndicateDivision;
import net.assassinscreedstealthbridge.syndicate.SyndicateMember;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SyndicateBoardScreen extends Screen {

    private final SyndicateBoardManager board;
    private final Map<UUID, NodePosition> nodePositions = new HashMap<>();

    public SyndicateBoardScreen(SyndicateBoardManager board) {
        super(Component.literal("Syndicate Investigation Board"));
        this.board = board;
    }

    private static class NodePosition {
        int x, y, width, height;
        NodePosition(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.nodePositions.clear();

        if (board == null) return;

        int marginX = 40;
        int startY = 30;
        
        // GANZ WICHTIG: Holt AUSSCHLIESSLICH die 4 aktiven Divisionen!
        List<SyndicateDivision> divisions = board.getActiveDivisions();
        if (divisions.isEmpty()) return;

        int colWidth = (this.width - (marginX * 2)) / divisions.size(); // Teilt den Platz durch 4
        UUID playerUUID = Minecraft.getInstance().player.getUUID();

        for (int i = 0; i < divisions.size(); i++) {
            SyndicateDivision div = divisions.get(i);
            int colCenterX = marginX + (i * colWidth) + (colWidth / 2);
            
            if (div.intel >= 100) {
                this.addRenderableWidget(Button.builder(Component.literal("§4RELEASE DATA"), b -> {
                    SyndicateNetwork.sendToServer(new SyndicateNetwork.ProcessDecisionPacket(div.leaderId != null ? div.leaderId : div.memberIds.get(0), "publish_intel"));
                })
                .bounds(colCenterX - 50, startY + 15, 100, 20)
                .tooltip(Tooltip.create(Component.literal("§cPublish their secrets!§r\nThe Division will launch a Desperate Assault.\n§aYields massive Loot and rotates the Faction!")))
                .build());
            }

            int currentY = startY + 45; 

            for (UUID memberId : div.memberIds) {
                SyndicateMember member = board.getMember(memberId);
                if (member == null) continue;

                int boxWidth = 100;
                int boxHeight = 45; 
                int boxX = colCenterX - (boxWidth / 2);
                int boxY = currentY;

                nodePositions.put(memberId, new NodePosition(boxX, boxY, boxWidth, boxHeight));

                if (playerUUID.equals(member.capturedBy)) {
                    this.addRenderableWidget(Button.builder(Component.literal("Execute"), b -> {
                        SyndicateNetwork.sendToServer(new SyndicateNetwork.ProcessDecisionPacket(memberId, "execute"));
                    })
                    .bounds(boxX, boxY + 24, boxWidth / 2, 20)
                    .tooltip(Tooltip.create(Component.literal("§cExecute (Grow)§r\nIncreases Rank by 1.\n§4NO LOOT.§r\nMakes future encounters harder but yields a better Reward Box later.")))
                    .build());

                    this.addRenderableWidget(Button.builder(Component.literal("Interrog"), b -> {
                        SyndicateNetwork.sendToServer(new SyndicateNetwork.ProcessDecisionPacket(memberId, "interrogate"));
                    })
                    .bounds(boxX + (boxWidth / 2), boxY + 24, boxWidth / 2, 20)
                    .tooltip(Tooltip.create(Component.literal("§eInterrogate (Harvest)§r\nDecreases Rank by 1.\n§b+" + (member.rank * 10) + "% Division Intel\n§aDrops a Reward Box based on Rank!")))
                    .build());
                }

                currentY += boxHeight + 15; 
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        if (board == null) return;

        graphics.drawCenteredString(this.font, "The Syndicate", this.width / 2, 10, 0xFF5555);

        for (SyndicateMember member : board.getAllMembers()) {
            NodePosition pos1 = nodePositions.get(member.id);
            if (pos1 == null) continue;
            int x1 = pos1.x + pos1.width / 2;
            int y1 = pos1.y + pos1.height / 2;

            for (UUID trustedId : member.trusted) {
                NodePosition pos2 = nodePositions.get(trustedId);
                if (pos2 != null && member.id.compareTo(trustedId) < 0) {
                    graphics.fill(x1, y1, pos2.x + pos2.width / 2, (pos2.y + pos2.height / 2) + 1, 0xFF00AA00);
                }
            }
            for (UUID rivalId : member.rivals) {
                NodePosition pos2 = nodePositions.get(rivalId);
                if (pos2 != null && member.id.compareTo(rivalId) < 0) {
                    graphics.fill(x1, y1, pos2.x + pos2.width / 2, (pos2.y + pos2.height / 2) + 1, 0xFFAA0000);
                }
            }
        }

        // GANZ WICHTIG: Holt AUSSCHLIESSLICH die 4 aktiven Divisionen!
        List<SyndicateDivision> divisions = board.getActiveDivisions();
        int marginX = 40;
        int colWidth = (this.width - (marginX * 2)) / Math.max(1, divisions.size());
        
        List<Component> activeTooltip = null;

        for (int i = 0; i < divisions.size(); i++) {
            SyndicateDivision div = divisions.get(i);
            int colCenterX = marginX + (i * colWidth) + (colWidth / 2);
            
            graphics.drawCenteredString(this.font, "§n" + div.name, colCenterX, 25, 0xFFFFFF);
            
            if (div.intel < 100) {
                int barWidth = 80;
                int barX = colCenterX - (barWidth / 2);
                graphics.fill(barX, 38, barX + barWidth, 42, 0xFF333333); 
                graphics.fill(barX, 38, barX + (int)(barWidth * (div.intel / 100f)), 42, 0xFFD4AF37); 
                graphics.drawCenteredString(this.font, div.intel + "% Intel", colCenterX, 44, 0xAAAAAA);
            }

            for (UUID memberId : div.memberIds) {
                SyndicateMember member = board.getMember(memberId);
                NodePosition pos = nodePositions.get(memberId);
                if (member == null || pos == null) continue;

                boolean isKnown = div.intel >= 10; 

                int bgColor = member.leader ? 0xAA550000 : 0xAA222222;
                if (member.capturedBy != null) bgColor = 0xAA440000; 

                graphics.fill(pos.x, pos.y, pos.x + pos.width, pos.y + pos.height, bgColor);
                graphics.renderOutline(pos.x, pos.y, pos.width, pos.height, 0xFFAAAAAA);

                if (!isKnown && member.capturedBy == null) {
                    graphics.drawCenteredString(this.font, "???", pos.x + pos.width / 2, pos.y + 10, 0xFF888888);
                    graphics.drawCenteredString(this.font, "Unknown Target", pos.x + pos.width / 2, pos.y + 25, 0xFF555555);
                } else {
                    graphics.drawCenteredString(this.font, member.name, pos.x + pos.width / 2, pos.y + 4, 0xFFFFFF);
                    graphics.drawCenteredString(this.font, member.getRankStars(), pos.x + pos.width / 2, pos.y + 14, 0xFFFF55);
                    
                    if (member.capturedBy == null) {
                        if (member.leader) {
                            graphics.drawCenteredString(this.font, "§6[Leader]", pos.x + pos.width / 2, pos.y + 28, 0xFFAA00);
                        } else if (div.intel >= 40) {
                            graphics.drawCenteredString(this.font, "§b(Loot Revealed)", pos.x + pos.width / 2, pos.y + 28, 0xFFFFFF);
                        }
                    }
                }

                if (mouseX >= pos.x && mouseX <= pos.x + pos.width && mouseY >= pos.y && mouseY <= pos.y + pos.height) {
                    activeTooltip = new ArrayList<>();
                    if (!isKnown) {
                        activeTooltip.add(Component.literal("§7Identity hidden..."));
                        activeTooltip.add(Component.literal("§8Interrogate other members"));
                        activeTooltip.add(Component.literal("§8to reveal their identity."));
                    } else {
                        activeTooltip.add(Component.literal("§f" + member.name));
                        activeTooltip.add(Component.literal("§eRank: " + member.getRankTitle()));
                        
                        if (div.intel >= 40) {
                            String buffName = (member.signatureEffect == null || member.signatureEffect.equals("none")) ? 
                                    "None" : Component.translatable(net.minecraft.Util.makeDescriptionId("effect", new ResourceLocation(member.signatureEffect))).getString();
                            
                            activeTooltip.add(Component.literal("§cSignature Buff: " + buffName));
                            String readableName = div.rewardFlavor.replace("reward_box:", "");
                            readableName = readableName.substring(0, 1).toUpperCase() + readableName.substring(1).replace("_", " ");
                            activeTooltip.add(Component.literal("§aDrops Box: " + readableName));
                        } else {
                            activeTooltip.add(Component.literal("§8Reach 40% Intel to reveal abilities."));
                        }
                    }
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTick);

        if (activeTooltip != null) {
            graphics.renderComponentTooltip(this.font, activeTooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}