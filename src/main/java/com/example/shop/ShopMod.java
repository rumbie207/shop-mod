package com.example.shop;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ShopMod implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("shop")
                .executes(context -> {
                    if (context.getSource().isExecutedByPlayer()) {
                        openMainShopGUI(context.getSource().getPlayer());
                    }
                    return 1;
                })
            );
        });
    }

    // --- MAIN SHOP MENU (Matches your GEAR screenshot) ---
    public static void openMainShopGUI(ServerPlayerEntity player) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X3, player, false);
        gui.setTitle(Text.literal("SHOP ➔ GEAR").formatted(Formatting.BOLD));

        // Row 1
        addShopSlot(gui, player, 0, Items.GOLDEN_APPLE, "Golden Apple", 1);
        addShopSlot(gui, player, 1, Items.WIND_CHARGE, "Wind Charge", 2);
        addShopSlot(gui, player, 2, Items.ENDER_PEARL, "Ender Pearl", 1);
        addShopSlot(gui, player, 3, Items.ENDER_CHEST, "Ender Chest", 3);
        addShopSlot(gui, player, 4, Items.EXPERIENCE_BOTTLE, "Bottle o' Enchanting", 1);
        addShopSlot(gui, player, 5, Items.GOLDEN_CARROT, "Golden Carrot", 1);
        addShopSlot(gui, player, 6, Items.SHIELD, "Shield", 1);
        addShopSlot(gui, player, 7, Items.TRIDENT, "Trident / Spear", 3);
        addShopSlot(gui, player, 8, Items.DIAMOND_PICKAXE, "Diamond Pickaxe", 3);

        // Row 2
        addShopSlot(gui, player, 9, Items.SPLASH_POTION, "Splash Potion of Strength", 3);
        addShopSlot(gui, player, 10, Items.SPLASH_POTION, "Splash Potion of Speed", 3);
        addShopSlot(gui, player, 11, Items.SPLASH_POTION, "Splash Potion of Fire Resistance", 3);
        addShopSlot(gui, player, 12, Items.OAK_LOG, "Oak Log", 1);
        addShopSlot(gui, player, 13, Items.BOOKSHELF, "Bookshelf", 1);

        // Bottom Navigation (Compass & Close Barrier)
        gui.setSlot(18, new GuiElementBuilder(Items.COMPASS).setName(Text.literal("Main Menu").formatted(Formatting.AQUA)).build());
        gui.setSlot(26, new GuiElementBuilder(Items.BARRIER).setName(Text.literal("Close").formatted(Formatting.RED)).setCallback((index, type, action) -> gui.close()).build());

        gui.open();
    }

    private static void addShopSlot(SimpleGui gui, ServerPlayerEntity player, int slot, Item item, String name, int priceInDiamonds) {
        gui.setSlot(slot, new GuiElementBuilder(item)
                .setName(Text.literal(name).formatted(Formatting.YELLOW))
                .setLore(java.util.List.of(Text.literal("Price: " + priceInDiamonds + " Diamond(s)").formatted(Formatting.AQUA)))
                .setCallback((index, type, action) -> openBuyingGUI(player, item, name, priceInDiamonds))
                .build());
    }

    // --- BUYING SUB-MENU ---
    public static void openBuyingGUI(ServerPlayerEntity player, Item item, String name, int pricePerUnit) {
        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X4, player, false);
        gui.setTitle(Text.literal("BUYING"));

        // Selected Item Display
        gui.setSlot(13, new GuiElementBuilder(item)
                .setName(Text.literal(name).formatted(Formatting.GOLD))
                .setLore(java.util.List.of(Text.literal("Price: " + pricePerUnit + " Diamond(s) each").formatted(Formatting.GRAY)))
                .build());

        // Frame Accent
        gui.setSlot(14, new GuiElementBuilder(Items.GREEN_STAINED_GLASS_PANE).setName(Text.empty()).build());

        // Buy 10x Button
        gui.setSlot(15, new GuiElementBuilder(Items.LIME_STAINED_GLASS_PANE, 10)
                .setName(Text.literal("Buy 10x").formatted(Formatting.GREEN))
                .setLore(java.util.List.of(Text.literal("Cost: " + (pricePerUnit * 10) + " Diamonds").formatted(Formatting.YELLOW)))
                .setCallback((index, type, action) -> processPurchase(player, item, 10, pricePerUnit * 10))
                .build());

        // Buy 64x Button
        gui.setSlot(16, new GuiElementBuilder(Items.LIME_STAINED_GLASS_PANE, 64)
                .setName(Text.literal("Buy 64x").formatted(Formatting.GREEN))
                .setLore(java.util.List.of(Text.literal("Cost: " + (pricePerUnit * 64) + " Diamonds").formatted(Formatting.YELLOW)))
                .setCallback((index, type, action) -> processPurchase(player, item, 64, pricePerUnit * 64))
                .build());

        // Cancel / Back Button
        gui.setSlot(22, new GuiElementBuilder(Items.BARRIER)
                .setName(Text.literal("Back to Shop").formatted(Formatting.RED))
                .setCallback((index, type, action) -> openMainShopGUI(player))
                .build());

        // Confirm Single Purchase (1x)
        gui.setSlot(23, new GuiElementBuilder(Items.LIME_DYE)
                .setName(Text.literal("Buy 1x").formatted(Formatting.GREEN))
                .setLore(java.util.List.of(Text.literal("Cost: " + pricePerUnit + " Diamond(s)").formatted(Formatting.YELLOW)))
                .setCallback((index, type, action) -> processPurchase(player, item, 1, pricePerUnit))
                .build());

        gui.open();
    }

    // --- DIAMOND PAYMENT LOGIC ---
    private static void processPurchase(ServerPlayerEntity player, Item item, int count, int totalDiamondCost) {
        int playerDiamonds = countItems(player, Items.DIAMOND);

        if (playerDiamonds >= totalDiamondCost) {
            removeItems(player, Items.DIAMOND, totalDiamondCost);
            player.getInventory().insertStack(new ItemStack(item, count));
            player.sendMessage(Text.literal("Purchased " + count + "x " + item.getName().getString() + " for " + totalDiamondCost + " Diamond(s)!").formatted(Formatting.GREEN));
        } else {
            player.sendMessage(Text.literal("You need " + totalDiamondCost + " Diamonds, but only have " + playerDiamonds + "!").formatted(Formatting.RED));
        }
    }

    private static int countItems(ServerPlayerEntity player, Item targetItem) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(targetItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeItems(ServerPlayerEntity player, Item targetItem, int amount) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(targetItem)) {
                int take = Math.min(amount, stack.getCount());
                stack.decrement(take);
                amount -= take;
                if (amount <= 0) break;
            }
        }
    }
}