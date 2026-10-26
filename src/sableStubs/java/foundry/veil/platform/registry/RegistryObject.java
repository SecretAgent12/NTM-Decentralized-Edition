// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only
// Additional terms under GPL-3.0 section 7 apply: see NOTICE.md

package foundry.veil.platform.registry;

import java.util.function.Supplier;

/**
 * Compile-time stand-in for Veil's interface of the same name (shipped inside Sable): only what NTM
 * calls. Not packaged; at run time the class comes from Sable's bundled Veil.
 */
public interface RegistryObject<T> extends Supplier<T> {
    @Override
    T get();
}
