package com.pallas.launcher.domain.notebook

import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.domain.search.MathValue
import com.pallas.launcher.domain.search.SmartCommandEngine
import java.util.Random

/**
 * Isolated stateful computation kernel for a single [ComputationalNotebook].
 * Holds user-defined symbols, function definitions, `%`/`Out` history, and PRNG state
 * completely isolated from launcher search evaluation state.
 */
class NotebookKernel {

    private val env: MutableMap<String, MathValue> = linkedMapOf()
    private var executionCounter: Int = 0
    private var rng: Random = Random()

    @Synchronized
    fun nextExecutionNumber(): Int = ++executionCounter

    @Synchronized
    fun reset() {
        env.clear()
        executionCounter = 0
        rng = Random()
    }

    @Synchronized
    fun definedSymbols(): List<String> =
        env.keys
            .filter { key -> key != "%" && key != "out" && !key.startsWith("#") }
            .sorted()

    @Synchronized
    fun evaluateCell(rawSource: String): NotebookCellOutput {
        val trimmed = rawSource.trim()
        if (trimmed.isEmpty()) return NotebookCellOutput()

        val isExplicitTex = trimmed.startsWith("tex ", ignoreCase = true) ||
            trimmed.startsWith("tex\n", ignoreCase = true) ||
            trimmed.startsWith("$$") ||
            trimmed.startsWith("\\[")

        if (isExplicitTex) {
            val stripped = if (trimmed.startsWith("tex", ignoreCase = true)) {
                trimmed.substring(3).trim()
            } else {
                trimmed
            }
            val normalized = SmartCommandEngine.normalizeUserLatex(stripped)
            if (normalized.isEmpty()) {
                return NotebookCellOutput(
                    primaryValue = "Empty LaTeX expression",
                    badge = "Error",
                    isError = true
                )
            }
            return NotebookCellOutput(
                primaryValue = normalized,
                badge = "LaTeX",
                latexSource = normalized,
                copyText = normalized
            )
        }

        val res = MathStatsEngine.evaluateInKernel(
            rawInput = trimmed,
            env = env,
            rng = rng
        )
        rng = res.updatedRng

        if (res.errorMessage != null) {
            return NotebookCellOutput(
                primaryValue = res.errorMessage,
                badge = "Error",
                isError = true,
                copyText = res.errorMessage
            )
        }

        val outcome = res.outcome ?: return NotebookCellOutput(
            primaryValue = "No result",
            badge = "Error",
            isError = true
        )

        return NotebookCellOutput(
            primaryValue = outcome.primaryValue,
            badge = outcome.badge,
            subtitle = outcome.subtitle,
            consoleOutput = outcome.consoleOutput,
            latexSource = res.latexOutput,
            chart = outcome.chart,
            copyText = outcome.copyText,
            isError = false
        )
    }
}
