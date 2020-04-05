// SPDX-License-Identifier: BSD-3-Clause
package org.islandoftex.arara.ruleset

import org.islandoftex.arara.rules.Directive
import org.islandoftex.arara.rules.DirectiveConditional

/**
 * Implements the directive model.
 *
 * @author Island of TeX
 * @version 5.0
 * @since 4.0
 */
data class DirectiveImpl(
    override val identifier: String,
    override val parameters: Map<String, Any>,
    override val conditional: DirectiveConditional,
    override val lineNumbers: List<Int>
) : Directive