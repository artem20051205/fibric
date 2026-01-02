package com.example;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ExampleMod implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("getitem")
					.then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
							.executes(ctx -> {
								ServerPlayerEntity player = ctx.getSource().getPlayer();
								ItemStackArgument itemArg = ItemStackArgumentType.getItemStackArgument(ctx, "item");
								ItemStack stack = itemArg.createStack(1, false);
								player.getInventory().insertStack(stack);
								return Command.SINGLE_SUCCESS;
							})
					)
			);
		});
	}
}