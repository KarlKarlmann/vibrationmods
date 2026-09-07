package net.assassinscreedstealthbridge.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.assassinscreedstealthbridge.syndicate.SyndicateEvents;
import net.assassinscreedstealthbridge.syndicate.SyndicateManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;

public class SyndicateCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("syndicate")
                .requires(source -> source.hasPermission(2)) // Nur Ops / Admins duerfen das
                .then(Commands.literal("ambush")
                        // /syndicate ambush (loest bei dir selbst aus)
                        .executes(context -> triggerAmbush(context, Collections.singleton(context.getSource().getPlayerOrException())))
                        // /syndicate ambush <player> (loest bei Zielen aus)
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> triggerAmbush(context, EntityArgument.getPlayers(context, "targets")))
                        )
                )
        );
    }

	private static int triggerAmbush(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> targets) {
		CommandSourceStack source = context.getSource();
		SyndicateManager manager = SyndicateManager.get(source.getLevel());

		for (ServerPlayer player : targets) {
			// 1. Spiele die typische Warnmeldung ab
			player.displayClientMessage(Component.literal("§4The Syndicate has found you!").withStyle(net.minecraft.ChatFormatting.BOLD), true);
			
			// 2. Rufe den echten Ambush auf
			SyndicateEvents.triggerSyndicateAmbush(player, source.getLevel(), manager);
			
			// 3. Setze nur noch das Threat-Level zurueck, da das Wellen-System entfernt wurde
			manager.resetThreatLevel(player);

			source.sendSuccess(() -> Component.literal("Syndicate ambush triggered for: " + player.getName().getString()), true);
		}

		return targets.size(); 
	}
}