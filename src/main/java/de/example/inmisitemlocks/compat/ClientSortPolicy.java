package de.example.inmisitemlocks.compat;

import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.config.Policy;
import dev.terminalmc.clientsort.client.util.PolicyManager;

import java.util.TreeSet;

/** Registers a ClientSort policy for the Inmis backpack menu (in memory, not saved). */
final class ClientSortPolicy {
    static final String MENU_CLASS = "draylar.inmis.ui.BackpackScreenHandler";

    private ClientSortPolicy() {}

    static void ensureRegistered() {
        var policies = Config.options().classPolicies;
        String key = ClassPolicy.getKey(MENU_CLASS, null);

        if (policies.containsKey(key)) return; // user configured it themselves

        policies.put(key, new ClassPolicy(
                MENU_CLASS, null, null, false,
                Policy.KEYBIND_BUTTON, // sort
                Policy.KEYBIND,        // stack fill (key only)
                Policy.KEYBIND_BUTTON, // match transfer
                Policy.KEYBIND_BUTTON, // transfer (move all)
                null, false, new TreeSet<>()));

        PolicyManager.reloadPolicyClasses(policies.keySet());
    }
}
