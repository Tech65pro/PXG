package com.tech65pro.vortexmc.client;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;

/**
 * The keybinds VortexMC registers.
 *
 * <p>The type is Minecraft's own {@code KeyBinding} (Yarn name for {@code class_304}); Fabric's
 * helper simply registers it. Its {@code Category} is created through the static factory rather
 * than by referencing a constant, because Yarn's mappings do not list enum-style constants.</p>
 *
 * <p><strong>The category must be created exactly once.</strong> {@code KeyBinding.Category} is a
 * record, so two instances holding the same {@link Identifier} are {@code equals}. The factory
 * keeps a {@code List<Category>} of everything it has handed out and, on finding an equal entry,
 * throws {@code IllegalArgumentException: Category '<id>' is already registered.} Calling
 * {@code create(CATEGORY_ID)} once per keybinding therefore crashes the client during
 * initialization. The single instance below is shared by both keybinds.</p>
 */
public final class VortexKeys {

    /**
     * GLFW key codes, written out rather than referenced from LWJGL so the values VortexMC
     * depends on are documented in one place: H is 72 and J is 74.
     *
     * <p>H and J are chosen deliberately. P is Minecraft's own "player list"/social screen
     * default and O is unbound by default but sits next to it; both are easy to hit by accident
     * while playing, and an accidental dashboard open is far more disruptive than an accidental
     * overlay toggle. H and J are adjacent on the home row, unbound by default in vanilla, and
     * far enough from the movement cluster that they are not pressed while walking.</p>
     */
    private static final int GLFW_KEY_H = 72;
    private static final int GLFW_KEY_J = 74;

    /** Category identifier. The label shown in Controls comes from the key below. */
    public static final Identifier CATEGORY_ID = Identifier.of("vortexmc", "main");

    /**
     * Translation key the game derives from {@link #CATEGORY_ID} via {@code Identifier.toTranslationKey}.
     * It is singular ({@code key.category.}, not {@code key.categories.}) because that is the
     * prefix {@code Category.getLabel()} uses.
     */
    public static final String CATEGORY_TRANSLATION_KEY = "key.category.vortexmc.main";

    /**
     * The one and only category instance. Created once, here, and reused by both keybinds.
     *
     * <p>Declared after {@link #CATEGORY_ID} on purpose: static initialisers run in declaration
     * order, so the identifier is non-null by the time the factory is called.</p>
     */
    public static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(CATEGORY_ID);

    public static KeyBinding openDashboard;
    public static KeyBinding toggleOverlay;

    private VortexKeys() {
    }

    public static void register() {
        openDashboard = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vortexmc.open_dashboard", GLFW_KEY_H, CATEGORY));
        toggleOverlay = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.vortexmc.toggle_overlay", GLFW_KEY_J, CATEGORY));
    }
}
