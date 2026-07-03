package net.targetoutlines;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class TargetConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        final Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

    public static class Client {
        public final ForgeConfigSpec.BooleanValue seeThroughWalls;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.push("rendering");

            seeThroughWalls = builder
                    .comment("Soll die rote Umrandung der Gegner durch Wände hindurch (X-Ray) sichtbar sein?")
                    .define("seeThroughWalls", true);

            builder.pop();
        }
    }
}