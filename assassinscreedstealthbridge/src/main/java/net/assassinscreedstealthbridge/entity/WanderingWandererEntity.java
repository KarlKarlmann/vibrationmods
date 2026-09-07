package net.assassinscreedstealthbridge.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public class WanderingWandererEntity extends Monster {

    public WanderingWandererEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D) // Etwas langsamer, er schlendert ja
                .add(Attributes.FOLLOW_RANGE, 32.0D);  // Wichtig für das Scannen der Umgebung
    }

    @Override
    protected void registerGoals() {
        // Schwimmen, falls er ins Wasser fällt
        this.goalSelector.addGoal(0, new FloatGoal(this));
        
        // Er kann nicht kämpfen! Wenn ein Spieler auf 8 Blöcke herankommt, rennt er weg (Speed-Multiplikator 1.2)
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 8.0F, 1.0D, 1.2D));
        
        // Unser Custom-Goal: Zieht ihn magisch zu Spieler-Lagerfeuern und Fackeln an
        this.goalSelector.addGoal(2, new MoveToLightGoal(this, 1.0D));
        
        // Standard-Verhalten, wenn gerade kein Licht oder Spieler in der Nähe ist
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    /**
     * Ein benutzerdefiniertes AI-Goal.
     * Lässt den NPC die Umgebung nach Lichtquellen abscannen und gezielt dorthin laufen.
     */
    static class MoveToLightGoal extends Goal {
        private final Monster mob;
        private final double speedModifier;
        private BlockPos targetLightPos;
        private int searchCooldown;

        public MoveToLightGoal(Monster mob, double speedModifier) {
            this.mob = mob;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Um Lags zu vermeiden, scannen wir die Blöcke nur alle paar Sekunden
            if (this.searchCooldown > 0) {
                this.searchCooldown--;
                return false;
            }
            // Scanne alle 60 Ticks (3 Sekunden)
            this.searchCooldown = 60; 

            // Tagsüber ist überall Licht, da muss er nicht gezielt Fackeln suchen
            if (this.mob.level().isDay()) return false;

            this.targetLightPos = findNearestLightSource();
            return this.targetLightPos != null;
        }

        @Override
        public void start() {
            // Setzt den Pfad zur gefundenen Lichtquelle
            this.mob.getNavigation().moveTo(this.targetLightPos.getX(), this.targetLightPos.getY(), this.targetLightPos.getZ(), this.speedModifier);
        }

        @Override
        public boolean canContinueToUse() {
            // Läuft weiter, bis er angekommen ist oder ein Hindernis den Weg blockiert
            return !this.mob.getNavigation().isDone() && this.targetLightPos != null;
        }

        private BlockPos findNearestLightSource() {
            BlockPos mobPos = this.mob.blockPosition();
            Level level = this.mob.level();
            
            // Suchradius: 16 Blöcke in jede Richtung
            int radius = 16; 
            
            BlockPos bestPos = null;
            double bestDist = Double.MAX_VALUE;

            // Iteriert über alle Blöcke in der Bounding Box
            for (BlockPos pos : BlockPos.betweenClosed(mobPos.offset(-radius, -4, -radius), mobPos.offset(radius, 4, radius))) {
                BlockState state = level.getBlockState(pos);
                
                // Prüft, ob der Block selbst Licht abgibt (z.B. Fackel > 10, Lagerfeuer = 15)
                if (state.getLightEmission(level, pos) > 8) {
                    double dist = pos.distSqr(mobPos);
                    if (dist < bestDist) {
                        bestDist = dist;
                        bestPos = pos.immutable();
                    }
                }
            }
            return bestPos;
        }
    }
}