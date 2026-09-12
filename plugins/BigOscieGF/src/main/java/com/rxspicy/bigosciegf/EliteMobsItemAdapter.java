package com.rxspicy.bigosciegf;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Optional reflective bridge to the exact EliteMobs version installed on the server. */
final class EliteMobsItemAdapter {
    private final NyxPlugin plugin;

    EliteMobsItemAdapter(NyxPlugin plugin) {
        this.plugin = plugin;
    }

    ItemStack applyCustomEnchantments(ItemStack source, Map<String, Integer> enchantments) throws Exception {
        if (enchantments.isEmpty()) return source;
        Plugin eliteMobs = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (eliteMobs == null || !eliteMobs.isEnabled()) {
            throw new IllegalStateException("EliteMobs is not enabled");
        }

        ClassLoader loader = eliteMobs.getClass().getClassLoader();
        Class<?> definitions = loader.loadClass(
                "com.magmaguy.elitemobs.magmacore.enchantments.EnchantmentDefinitions");
        Class<?> items = loader.loadClass(
                "com.magmaguy.elitemobs.magmacore.enchantments.EnchantmentItems");
        Method resolve = definitions.getMethod("resolve", String.class);
        Method classify = items.getMethod("classify", ItemStack.class);

        Function<String, Object> resolver = id -> invoke(resolve, null, id);
        Function<ItemStack, Object> classifier = item -> invoke(classify, null, item);
        Constructor<?> constructor = items.getConstructor(Function.class, Function.class);
        Object service = constructor.newInstance(resolver, classifier);
        Method preview = items.getMethod("previewAuthoredCustom", ItemStack.class, Map.class);
        Object draft = preview.invoke(service, source, new LinkedHashMap<>(enchantments));
        Method apply = draft.getClass().getMethod("apply", ItemStack.class);
        return (ItemStack) apply.invoke(draft, source);
    }

    private static Object invoke(Method method, Object target, Object argument) {
        try {
            return method.invoke(target, argument);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
