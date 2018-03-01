package io.github.hsyyid.inspector.cmdexecutors;

import io.github.hsyyid.inspector.PluginInfo;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class InspectorExecutor implements CommandExecutor {
	public InspectorExecutor() {
	}

	public CommandResult execute(CommandSource src, CommandContext ctx) throws CommandException {
		src.sendMessage(Text.of(TextColors.BLUE, "[Inspector]: ", TextColors.GRAY, "Version: ", TextColors.GOLD, PluginInfo.VERSION));
		src.sendMessage(Text.of(TextColors.BLUE, "[Inspector]: ", TextColors.RED, "Warnning: ", TextColors.GOLD, PluginInfo.WARNING));
		return CommandResult.success();
	}
}