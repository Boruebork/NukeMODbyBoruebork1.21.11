package com.boruebork.nukemod.gui;

import com.boruebork.nukemod.NukeModbyBoruebork;
import com.boruebork.nukemod.gui.menu.EnricherMenu;
import com.boruebork.nukemod.gui.menu.GuidedMissileLauncherMenu;
import com.boruebork.nukemod.gui.menu.IonizerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, NukeModbyBoruebork.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<EnricherMenu>> ENRICHER_MENU =
            registerMenuType("enricher_menu", EnricherMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<IonizerMenu>> IONIZER_MENU =
            registerMenuType("ionizer_menu", IonizerMenu::new);
    public static final DeferredHolder<MenuType<?>, MenuType<GuidedMissileLauncherMenu>> GUIDED_MISSILE_MENU =
            registerMenuType("guided_missile_launcher_menu", GuidedMissileLauncherMenu::new);


    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name,
                                                                                                               IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
