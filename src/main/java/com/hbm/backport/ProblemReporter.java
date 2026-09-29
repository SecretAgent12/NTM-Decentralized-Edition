// SPDX-FileCopyrightText: 2026 SecretAgent12
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.backport;

import org.slf4j.Logger;

/**
 * Backport of the parts of 26.x's ProblemReporter that ntm-next uses: only the
 * try-with-resources ScopedCollector wrapped around TagValueInput.create /
 * TagValueOutput.createWithContext. The storage shim reports its own problems to
 * the log, so the collector carries nothing.
 */
public interface ProblemReporter {
    ProblemReporter DISCARDING = new ProblemReporter() {};

    final class ScopedCollector implements ProblemReporter, AutoCloseable {
        public ScopedCollector(Object path, Logger logger) {}

        public ScopedCollector(Logger logger) {}

        @Override
        public void close() {}
    }
}
