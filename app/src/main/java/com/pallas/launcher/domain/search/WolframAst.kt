package com.pallas.launcher.domain.search

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.util.Locale
import kotlin.math.*

// =========================================================================
// Wolfram AST, Lexer & Parser
// =========================================================================

internal enum class TokenType {
    NUMBER, IDENT, STRING, LBRACKET, RBRACKET, LBRACE, RBRACE, LPAREN, RPAREN,
    COMMA, PLUS, MINUS, STAR, SLASH, CARET, PERCENT, EXCLAMATION,
    AT, // @
    POSTFIX, // //
    MAP, // /@
    APPLY, // @@
    APPLY_LEVEL, // @@@
    RIGHT_COMPOSITION, // /*
    COMPOSITION, // @*
    STRING_JOIN, // <>
    ARROW, // -> or →
    REPLACE_ALL, // /.
    REPLACE_REPEATED, // //.
    AMP, // &
    SLOT, // #, #1, #2...
    SEMICOLON, // ;
    SPAN, // ;;
    EQUAL, // =
    SET_DELAYED, // :=
    PLUS_EQUAL, // +=
    MINUS_EQUAL, // -=
    STAR_EQUAL, // *=
    SLASH_EQUAL, // /=
    PLUS_PLUS, // ++
    MINUS_MINUS, // --
    EQUAL_EQUAL, // ==
    NOT_EQUAL, // != or ≠
    LESS, // <
    LESS_EQUAL, // <= or ≤
    GREATER, // >
    GREATER_EQUAL, // >= or ≥
    AND, // &&
    OR, // ||
    EOF
}

internal data class Token(
    val type: TokenType,
    val text: String,
    val value: Double = 0.0,
    val intVal: Int = 0
)

internal class Tokenizer(private val input: String) {
    private var pos = 0

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        while (pos < input.length) {
            val c = input[pos]
            when {
                c.isWhitespace() -> pos++
                c == '(' && input.startsWith("(*", pos) -> {
                    pos += 2
                    var depth = 1
                    while (pos < input.length && depth > 0) {
                        when {
                            input.startsWith("(*", pos) -> { depth++; pos += 2 }
                            input.startsWith("*)", pos) -> { depth--; pos += 2 }
                            else -> pos++
                        }
                    }
                }
                c == '"' -> {
                    pos++
                    val sb = StringBuilder()
                    while (pos < input.length && input[pos] != '"') {
                        if (input[pos] == '\\' && pos + 1 < input.length) {
                            pos++
                            when (input[pos]) {
                                'n' -> sb.append('\n')
                                't' -> sb.append('\t')
                                'r' -> sb.append('\r')
                                '"' -> sb.append('"')
                                '\\' -> sb.append('\\')
                                else -> sb.append(input[pos])
                            }
                            pos++
                        } else {
                            sb.append(input[pos])
                            pos++
                        }
                    }
                    if (pos < input.length && input[pos] == '"') pos++
                    tokens.add(Token(TokenType.STRING, sb.toString()))
                }
                c.isDigit() || (c == '.' && pos + 1 < input.length && input[pos + 1].isDigit()) -> {
                    val start = pos
                    while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) pos++
                    if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
                        pos++
                        if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
                        while (pos < input.length && input[pos].isDigit()) pos++
                    }
                    val str = input.substring(start, pos)
                    val num = str.toDoubleOrNull() ?: 0.0
                    tokens.add(Token(TokenType.NUMBER, str, num))
                }
                c == '@' -> {
                    if (input.startsWith("@@@", pos)) {
                        tokens.add(Token(TokenType.APPLY_LEVEL, "@@@"))
                        pos += 3
                    } else if (input.startsWith("@@", pos)) {
                        tokens.add(Token(TokenType.APPLY, "@@"))
                        pos += 2
                    } else if (input.startsWith("@*", pos)) {
                        tokens.add(Token(TokenType.COMPOSITION, "@*"))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.AT, "@"))
                        pos += 1
                    }
                }
                c == '/' || c == '÷' -> {
                    if (input.startsWith("//.", pos)) {
                        tokens.add(Token(TokenType.REPLACE_REPEATED, "//."))
                        pos += 3
                    } else if (input.startsWith("//", pos)) {
                        tokens.add(Token(TokenType.POSTFIX, "//"))
                        pos += 2
                    } else if (input.startsWith("/@", pos)) {
                        tokens.add(Token(TokenType.MAP, "/@"))
                        pos += 2
                    } else if (input.startsWith("/.", pos)) {
                        tokens.add(Token(TokenType.REPLACE_ALL, "/."))
                        pos += 2
                    } else if (input.startsWith("/*", pos)) {
                        tokens.add(Token(TokenType.RIGHT_COMPOSITION, "/*"))
                        pos += 2
                    } else if (input.startsWith("/=", pos)) {
                        tokens.add(Token(TokenType.SLASH_EQUAL, "/="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.SLASH, "/"))
                        pos += 1
                    }
                }
                c == '-' -> {
                    if (input.startsWith("->", pos)) {
                        tokens.add(Token(TokenType.ARROW, "->"))
                        pos += 2
                    } else if (input.startsWith("-=", pos)) {
                        tokens.add(Token(TokenType.MINUS_EQUAL, "-="))
                        pos += 2
                    } else if (input.startsWith("--", pos)) {
                        tokens.add(Token(TokenType.MINUS_MINUS, "--"))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.MINUS, "-"))
                        pos += 1
                    }
                }
                c == '→' -> {
                    tokens.add(Token(TokenType.ARROW, "→"))
                    pos += 1
                }
                c == '+' -> {
                    if (input.startsWith("+=", pos)) {
                        tokens.add(Token(TokenType.PLUS_EQUAL, "+="))
                        pos += 2
                    } else if (input.startsWith("++", pos)) {
                        tokens.add(Token(TokenType.PLUS_PLUS, "++"))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.PLUS, "+"))
                        pos += 1
                    }
                }
                c == '*' || c == '×' -> {
                    if (input.startsWith("*=", pos)) {
                        tokens.add(Token(TokenType.STAR_EQUAL, "*="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.STAR, "*"))
                        pos += 1
                    }
                }
                c == '&' -> {
                    if (input.startsWith("&&", pos)) {
                        tokens.add(Token(TokenType.AND, "&&"))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.AMP, "&"))
                        pos += 1
                    }
                }
                c == '|' -> {
                    if (input.startsWith("||", pos)) {
                        tokens.add(Token(TokenType.OR, "||"))
                        pos += 2
                    } else {
                        pos++
                    }
                }
                c == ':' -> {
                    if (input.startsWith(":=", pos)) {
                        tokens.add(Token(TokenType.SET_DELAYED, ":="))
                        pos += 2
                    } else {
                        pos++
                    }
                }
                c == '=' -> {
                    if (input.startsWith("==", pos)) {
                        tokens.add(Token(TokenType.EQUAL_EQUAL, "=="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.EQUAL, "="))
                        pos += 1
                    }
                }
                c == '!' -> {
                    if (input.startsWith("!=", pos)) {
                        tokens.add(Token(TokenType.NOT_EQUAL, "!="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.EXCLAMATION, "!"))
                        pos += 1
                    }
                }
                c == '≠' -> {
                    tokens.add(Token(TokenType.NOT_EQUAL, "≠"))
                    pos += 1
                }
                c == '<' -> {
                    if (input.startsWith("<>", pos)) {
                        tokens.add(Token(TokenType.STRING_JOIN, "<>"))
                        pos += 2
                    } else if (input.startsWith("<=", pos)) {
                        tokens.add(Token(TokenType.LESS_EQUAL, "<="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.LESS, "<"))
                        pos += 1
                    }
                }
                c == '≤' -> {
                    tokens.add(Token(TokenType.LESS_EQUAL, "≤"))
                    pos += 1
                }
                c == '>' -> {
                    if (input.startsWith(">=", pos)) {
                        tokens.add(Token(TokenType.GREATER_EQUAL, ">="))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.GREATER, ">"))
                        pos += 1
                    }
                }
                c == '≥' -> {
                    tokens.add(Token(TokenType.GREATER_EQUAL, "≥"))
                    pos += 1
                }
                c == ';' -> {
                    if (input.startsWith(";;", pos)) {
                        tokens.add(Token(TokenType.SPAN, ";;"))
                        pos += 2
                    } else {
                        tokens.add(Token(TokenType.SEMICOLON, ";"))
                        pos++
                    }
                }
                c == '[' -> { tokens.add(Token(TokenType.LBRACKET, "[")); pos++ }
                c == ']' -> { tokens.add(Token(TokenType.RBRACKET, "]")); pos++ }
                c == '{' -> { tokens.add(Token(TokenType.LBRACE, "{")); pos++ }
                c == '}' -> { tokens.add(Token(TokenType.RBRACE, "}")); pos++ }
                c == '(' -> { tokens.add(Token(TokenType.LPAREN, "(")); pos++ }
                c == ')' -> { tokens.add(Token(TokenType.RPAREN, ")")); pos++ }
                c == ',' -> { tokens.add(Token(TokenType.COMMA, ",")); pos++ }
                c == '^' -> { tokens.add(Token(TokenType.CARET, "^")); pos++ }
                c == '%' -> { tokens.add(Token(TokenType.PERCENT, "%")); pos++ }
                c == '°' -> { tokens.add(Token(TokenType.IDENT, "°")); pos++ }
                c == '#' -> {
                    pos++
                    val start = pos
                    while (pos < input.length && input[pos].isDigit()) pos++
                    val slotNum = if (pos > start) input.substring(start, pos).toIntOrNull() ?: 1 else 1
                    tokens.add(Token(TokenType.SLOT, input.substring(start - 1, pos), intVal = slotNum))
                }
                c.isLetter() || c == 'π' || c == '∞' || c == '_' -> {
                    val start = pos
                    while (pos < input.length && (input[pos].isLetter() || input[pos].isDigit() || input[pos] == '_' || input[pos] == 'π' || input[pos] == '∞')) {
                        pos++
                    }
                    val str = input.substring(start, pos)
                    tokens.add(Token(TokenType.IDENT, str))
                }
                else -> pos++
            }
        }
        tokens.add(Token(TokenType.EOF, ""))
        return tokens
    }
}

// Execution Exceptions for Control Flow and Safety
class ComputationLimitException(message: String) : RuntimeException(message)
internal class BreakException : RuntimeException()
internal class ContinueException : RuntimeException()
internal class ReturnException(val value: MathValue) : RuntimeException()

class ExecutionContext(
    val maxSteps: Int = 10_000,
    val timeoutMs: Long = 150L,
    val env: MutableMap<String, MathValue> = mutableMapOf(),
    val printedLines: MutableList<String> = mutableListOf(),
    var rng: java.util.Random = MathStatsEngine.rng
) {
    val startTime = System.currentTimeMillis()
    var steps = 0
    var callDepth = 0
    private var printedOverflow = false

    fun printLine(line: String) {
        if (printedLines.size < 50) {
            printedLines.add(line)
        } else if (!printedOverflow) {
            printedOverflow = true
            printedLines.add("…")
        }
    }

    inline fun <T> withCallFrame(block: () -> T): T {
        if (callDepth >= 64) {
            throw ComputationLimitException("Recursion depth exceeded (64)")
        }
        callDepth++
        try {
            return block()
        } finally {
            callDepth--
        }
    }

    fun tick() {
        steps++
        if (steps > maxSteps) {
            throw ComputationLimitException("Computation exceeded step limit ($maxSteps steps)")
        }
        if ((steps and 63) == 0) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw ComputationLimitException("Computation timed out (> ${timeoutMs}ms)")
            }
        }
    }
}

sealed interface MathNode {
    fun eval(ctx: ExecutionContext): MathValue

    fun eval(env: Map<String, MathValue> = emptyMap()): MathValue {
        val ctx = ExecutionContext(env = env.toMutableMap())
        return eval(ctx)
    }

    data class NumberNode(val value: Double) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.NumberVal(value)
        }
    }

    data class BooleanNode(val value: Boolean) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.BooleanVal(value)
        }
    }

    data class StringNode(val text: String) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.StringVal(text)
        }
    }

    data class ConstantValueNode(val value: MathValue) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return value
        }
    }

    object NullNode : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.NullVal
        }
    }

    object OutNode : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return ctx.env["%"] ?: ctx.env["out"] ?: MathStatsEngine.lastEvaluatedValue ?: MathValue.NumberVal(0.0)
        }
    }

    data class SlotNode(val index: Int = 1) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return ctx.env["#$index"] ?: if (index == 1) ctx.env["#"] ?: MathValue.NumberVal(0.0) else MathValue.NumberVal(0.0)
        }
    }

    data class IdentNode(val name: String) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val lower = name.lowercase(Locale.ROOT)
            ctx.env[lower]?.let { v ->
                return if (v is MathValue.DelayedVal) ctx.withCallFrame { v.expr.eval(ctx) } else v
            }
            return when (lower) {
                "pi", "π" -> MathValue.NumberVal(PI)
                "e" -> MathValue.NumberVal(E)
                "degree", "°" -> MathValue.NumberVal(PI / 180.0)
                "goldenratio" -> MathValue.NumberVal((1.0 + sqrt(5.0)) / 2.0)
                "eulergamma" -> MathValue.NumberVal(0.5772156649015329)
                "infinity", "inf", "∞" -> MathValue.NumberVal(Double.POSITIVE_INFINITY)
                "true" -> MathValue.BooleanVal(true)
                "false" -> MathValue.BooleanVal(false)
                "null" -> MathValue.NullVal
                "%", "out" -> ctx.env["%"] ?: ctx.env["out"] ?: MathStatsEngine.lastEvaluatedValue ?: MathValue.NumberVal(0.0)
                else -> MathValue.SymbolVal(name)
            }
        }
    }

    data class ListNode(val items: List<MathNode>) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            if (items.isEmpty()) return MathValue.ListVal(emptyList())
            val evaluated = items.map { it.eval(ctx) }
            return wrapSmartList(evaluated)
        }
    }

    data class SpanNode(
        val start: MathNode?,
        val end: MathNode?,
        val step: MathNode?
    ) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.ExprVal("Span")
        }
    }

    data class CompoundNode(val expressions: List<MathNode>) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            if (expressions.isEmpty()) return MathValue.NullVal
            var last: MathValue = MathValue.NullVal
            for (expr in expressions) {
                last = expr.eval(ctx)
            }
            return last
        }
    }

    data class AssignNode(val varName: String, val expr: MathNode, val op: Char? = null) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val lower = varName.lowercase(Locale.ROOT)
            val rhs = expr.eval(ctx)
            val finalVal: MathValue = when (op) {
                null -> rhs
                '+' -> {
                    val cur = ctx.env[lower]?.asDouble() ?: 0.0
                    MathValue.NumberVal(cur + rhs.asDouble())
                }
                '-' -> {
                    val cur = ctx.env[lower]?.asDouble() ?: 0.0
                    MathValue.NumberVal(cur - rhs.asDouble())
                }
                '*' -> {
                    val cur = ctx.env[lower]?.asDouble() ?: 1.0
                    MathValue.NumberVal(cur * rhs.asDouble())
                }
                '/' -> {
                    val cur = ctx.env[lower]?.asDouble() ?: 0.0
                    MathValue.NumberVal(cur / rhs.asDouble())
                }
                else -> rhs
            }
            ctx.env[lower] = finalVal
            return finalVal
        }
    }

    data class AssignDelayedNode(val varName: String, val expr: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            ctx.env[varName.lowercase(Locale.ROOT)] = MathValue.DelayedVal(expr)
            return MathValue.NullVal
        }
    }

    data class AssignFunctionNode(
        val funcName: String,
        val paramNames: List<String>,
        val body: MathNode
    ) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            ctx.env[funcName.lowercase(Locale.ROOT)] = MathValue.FunctionVal(body, paramNames)
            return MathValue.NullVal
        }
    }

    data class PrefixIncDecNode(val op: String, val varName: String) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val lower = varName.lowercase(Locale.ROOT)
            val cur = ctx.env[lower]?.asDouble() ?: 0.0
            val next = if (op == "++") cur + 1.0 else cur - 1.0
            val res = MathValue.NumberVal(next)
            ctx.env[lower] = res
            return res
        }
    }

    data class PostfixIncDecNode(val op: String, val varName: String) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val lower = varName.lowercase(Locale.ROOT)
            val cur = ctx.env[lower]?.asDouble() ?: 0.0
            val next = if (op == "++") cur + 1.0 else cur - 1.0
            ctx.env[lower] = MathValue.NumberVal(next)
            return MathValue.NumberVal(cur)
        }
    }

    data class RuleNode(val pattern: MathNode, val replacement: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val varName = when (pattern) {
                is IdentNode -> pattern.name
                else -> pattern.eval(ctx).toDisplayString()
            }
            val repVal = replacement.eval(ctx)
            return MathValue.RuleVal(varName, repVal)
        }
    }

    data class ReplaceAllNode(val expr: MathNode, val ruleNode: MathNode, val repeated: Boolean = false) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()

            if (repeated) {
                val delayedRules = linkedMapOf<String, MathNode>()
                fun collectDelayed(node: MathNode) {
                    when (node) {
                        is RuleNode -> {
                            val pat = node.pattern
                            if (pat is IdentNode) {
                                delayedRules[pat.name.lowercase(Locale.ROOT)] = node.replacement
                            }
                        }
                        is ListNode -> node.items.forEach(::collectDelayed)
                        else -> {}
                    }
                }
                collectDelayed(ruleNode)

                if (delayedRules.isNotEmpty()) {
                    val saved = mutableMapOf<String, MathValue?>()
                    return try {
                        for ((k, rhsNode) in delayedRules) {
                            saved[k] = ctx.env[k]
                            ctx.env[k] = MathValue.DelayedVal(rhsNode)
                        }
                        expr.eval(ctx)
                    } finally {
                        for ((k, v) in saved) {
                            if (v != null) ctx.env[k] = v else ctx.env.remove(k)
                        }
                    }
                }
            }

            val rules = mutableMapOf<String, MathValue>()
            fun collectRules(rVal: MathValue) {
                when (rVal) {
                    is MathValue.RuleVal -> rules[rVal.lhs.lowercase(Locale.ROOT)] = rVal.rhs
                    is MathValue.MixedListVal -> rVal.items.forEach(::collectRules)
                    else -> {}
                }
            }
            collectRules(ruleNode.eval(ctx))

            fun substituteInValue(v: MathValue): MathValue = when (v) {
                is MathValue.SymbolVal -> rules[v.name.lowercase(Locale.ROOT)] ?: v
                is MathValue.MixedListVal -> wrapSmartList(v.items.map(::substituteInValue))
                is MathValue.RuleVal -> MathValue.RuleVal(v.lhs, substituteInValue(v.rhs))
                else -> v
            }

            val saved = mutableMapOf<String, MathValue?>()
            var result: MathValue = try {
                for ((k, v) in rules) {
                    saved[k] = ctx.env[k]
                    ctx.env[k] = v
                }
                expr.eval(ctx)
            } finally {
                for ((k, v) in saved) {
                    if (v != null) ctx.env[k] = v else ctx.env.remove(k)
                }
            }
            if (repeated && rules.isNotEmpty()) {
                for (iter in 0 until 64) {
                    ctx.tick()
                    val next = substituteInValue(result)
                    if (next == result) break
                    result = next
                }
            }
            return result
        }
    }

    data class FunctionNode(val body: MathNode, val paramNames: List<String> = emptyList()) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.FunctionVal(body, paramNames)
        }
    }

    data class ComposeBodyNode(val first: MathNode, val second: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val argList = mutableListOf<MathValue>()
            var idx = 1
            while (true) {
                val v = ctx.env["#$idx"] ?: break
                argList.add(v)
                idx++
            }
            if (argList.isEmpty()) {
                argList.add(ctx.env["#"] ?: MathValue.NullVal)
            }
            val mid = invokeFunction(first, argList, ctx)
            return invokeFunction(second, listOf(mid), ctx)
        }
    }

    data class ComposeNode(val left: MathNode, val right: MathNode, val rightFirst: Boolean) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val first = if (rightFirst) right else left
            val second = if (rightFirst) left else right
            return MathValue.FunctionVal(ComposeBodyNode(first, second))
        }
    }

    data class ApplyCallNode(val callee: MathNode, val args: List<MathNode>) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val fnVal = callee.eval(ctx)
            val evaluatedArgs = args.map { it.eval(ctx) }
            return when (fnVal) {
                is MathValue.FunctionVal -> invokePureFunction(fnVal, evaluatedArgs, ctx)
                is MathValue.SymbolVal -> CallNode(fnVal.name, evaluatedArgs.map { ConstantValueNode(it) }).eval(ctx)
                else -> error("Cannot invoke non-function $fnVal")
            }
        }
    }

    data class StringJoinNode(val left: MathNode, val right: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.StringVal(left.eval(ctx).toDisplayString() + right.eval(ctx).toDisplayString())
        }
    }

    data class PrefixNode(val func: MathNode, val arg: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val argVal = arg.eval(ctx)
            return invokeFunction(func, listOf(argVal), ctx)
        }
    }

    data class PostfixNode(val arg: MathNode, val func: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val argVal = arg.eval(ctx)
            return invokeFunction(func, listOf(argVal), ctx)
        }
    }

    data class MapNode(val func: MathNode, val listNode: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val res = when (val lVal = listNode.eval(ctx)) {
                is MathValue.MixedListVal -> lVal.items.map { item ->
                    ctx.tick()
                    invokeFunction(func, listOf(item), ctx)
                }
                else -> lVal.asList().map { item ->
                    ctx.tick()
                    invokeFunction(func, listOf(MathValue.NumberVal(item)), ctx)
                }
            }
            return wrapSmartList(res)
        }
    }

    data class ApplyNode(val func: MathNode, val exprNode: MathNode, val level1: Boolean = false) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val eVal = exprNode.eval(ctx)
            if (level1 && eVal is MathValue.MixedListVal) {
                val res = eVal.items.map { item ->
                    ctx.tick()
                    val subArgs = when (item) {
                        is MathValue.MixedListVal -> item.items
                        is MathValue.ListVal -> item.elements.map { MathValue.NumberVal(it) }
                        else -> listOf(item)
                    }
                    invokeFunction(func, subArgs, ctx)
                }
                return wrapSmartList(res)
            }
            val args = when (eVal) {
                is MathValue.MixedListVal -> eVal.items
                else -> eVal.asList().map { MathValue.NumberVal(it) }
            }
            return invokeFunction(func, args, ctx)
        }
    }

    data class PartNode(val expr: MathNode, val indices: List<MathNode>) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val target = expr.eval(ctx)
            return resolvePart(target, indices, 0, ctx)
        }
    }

    data class ComparisonNode(val op: String, val left: MathNode, val right: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return MathValue.BooleanVal(compareMathValues(op, left.eval(ctx), right.eval(ctx)))
        }
    }

    data class ChainedComparisonNode(val operands: List<MathNode>, val ops: List<String>) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            if (operands.isEmpty()) return MathValue.BooleanVal(true)
            var prev = operands[0].eval(ctx)
            var allTrue = true
            for (i in ops.indices) {
                val next = operands[i + 1].eval(ctx)
                if (!compareMathValues(ops[i], prev, next)) {
                    allTrue = false
                }
                prev = next
            }
            return MathValue.BooleanVal(allTrue)
        }
    }

    data class LogicalOpNode(val op: String, val left: MathNode, val right: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val l = left.eval(ctx)
            return if (op == "&&") {
                if (!l.asBoolean()) MathValue.BooleanVal(false)
                else MathValue.BooleanVal(right.eval(ctx).asBoolean())
            } else {
                if (l.asBoolean()) MathValue.BooleanVal(true)
                else MathValue.BooleanVal(right.eval(ctx).asBoolean())
            }
        }
    }

    data class UnaryOpNode(val op: Char, val child: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val v = child.eval(ctx)
            return when (op) {
                '-' -> applyUnaryNeg(v)
                '+' -> v
                '~' -> MathValue.BooleanVal(!v.asBoolean()) // Logical Not
                '!' -> applyUnaryFactorial(v)
                else -> error("Unknown unary operator: $op")
            }
        }
    }

    data class BinaryOpNode(val op: Char, val left: MathNode, val right: MathNode) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            return applyBinaryOpValue(op, left.eval(ctx), right.eval(ctx))
        }
    }

    data class CallNode(val funcName: String, val args: List<MathNode>) : MathNode {
        override fun eval(ctx: ExecutionContext): MathValue {
            ctx.tick()
            val fn = funcName.lowercase(Locale.ROOT)

            // Custom function or delayed value defined in environment
            val custom = ctx.env[fn]
            if (custom is MathValue.FunctionVal) {
                val evaluatedArgs = args.map { it.eval(ctx) }
                return invokePureFunction(custom, evaluatedArgs, ctx)
            }

            // Plotting and Visualization functions (handled before eager arg evaluation to preserve AST & 2D lists)
            WolframPlotEvaluator.tryEvalPlotOrChart(fn, args, ctx)?.let { return it }

            // Special forms that delay or control argument evaluation
            when (fn) {
                "if" -> {
                    if (args.isEmpty()) return MathValue.NullVal
                    val cond = args[0].eval(ctx).asBoolean()
                    return if (cond) {
                        args.getOrNull(1)?.eval(ctx) ?: MathValue.NullVal
                    } else {
                        args.getOrNull(2)?.eval(ctx) ?: MathValue.NullVal
                    }
                }
                "which" -> {
                    var i = 0
                    while (i < args.size - 1) {
                        val c = args[i].eval(ctx).asBoolean()
                        if (c) return args[i + 1].eval(ctx)
                        i += 2
                    }
                    return MathValue.NullVal
                }
                "switch" -> {
                    if (args.size < 3) error("Switch requires at least 3 arguments")
                    val target = args[0].eval(ctx)
                    var i = 1
                    while (i + 1 < args.size) {
                        val patNode = args[i]
                        if (patNode is IdentNode && patNode.name == "_") {
                            return args[i + 1].eval(ctx)
                        }
                        if (valuesEqual(target, patNode.eval(ctx))) {
                            return args[i + 1].eval(ctx)
                        }
                        i += 2
                    }
                    return MathValue.NullVal
                }
                "print" -> {
                    val out = args.joinToString("") { it.eval(ctx).toDisplayString() }
                    ctx.printLine(out)
                    return MathValue.NullVal
                }
                "echo" -> {
                    val v = args.getOrNull(0)?.eval(ctx) ?: return MathValue.NullVal
                    val label = args.getOrNull(1)?.eval(ctx)?.toDisplayString()
                    ctx.printLine(if (!label.isNullOrEmpty()) "$label ${v.toDisplayString()}" else v.toDisplayString())
                    return v
                }
                "function" -> {
                    return when (args.size) {
                        0 -> MathValue.NullVal
                        1 -> MathValue.FunctionVal(args[0], emptyList())
                        else -> {
                            val params = when (val pNode = args[0]) {
                                is IdentNode -> listOf(pNode.name)
                                is ListNode -> pNode.items.mapNotNull { (it as? IdentNode)?.name }
                                else -> emptyList()
                            }
                            MathValue.FunctionVal(args[1], params)
                        }
                    }
                }
                "module", "block", "with" -> {
                    if (args.size < 2) error("$funcName requires bindings and body")
                    val bindingsNode = args[0] as? ListNode ?: error("$funcName requires a list of local bindings")
                    val bodyNode = args[1]
                    val saved = linkedMapOf<String, MathValue?>()
                    try {
                        for (item in bindingsNode.items) {
                            when (item) {
                                is IdentNode -> {
                                    val k = item.name.lowercase(Locale.ROOT)
                                    if (!saved.containsKey(k)) saved[k] = ctx.env[k]
                                    ctx.env.remove(k)
                                }
                                is AssignNode -> {
                                    val k = item.varName.lowercase(Locale.ROOT)
                                    val rhsVal = item.expr.eval(ctx)
                                    if (!saved.containsKey(k)) saved[k] = ctx.env[k]
                                    ctx.env[k] = rhsVal
                                }
                                else -> item.eval(ctx)
                            }
                        }
                        return try {
                            bodyNode.eval(ctx)
                        } catch (r: ReturnException) {
                            r.value
                        }
                    } finally {
                        for ((k, oldVal) in saved) {
                            if (oldVal != null) ctx.env[k] = oldVal else ctx.env.remove(k)
                        }
                    }
                }
                "clear" -> {
                    args.forEach { arg ->
                        when (arg) {
                            is IdentNode -> ctx.env.remove(arg.name.lowercase(Locale.ROOT))
                            is StringNode -> ctx.env.remove(arg.text.lowercase(Locale.ROOT))
                            else -> {}
                        }
                    }
                    return MathValue.NullVal
                }
                "seedrandom" -> {
                    val seed = args.getOrNull(0)?.eval(ctx)?.asDouble()
                    ctx.rng = if (seed != null && !seed.isNaN()) java.util.Random(seed.toLong()) else java.util.Random()
                    return MathValue.NullVal
                }
                "while" -> {
                    if (args.size < 2) error("While requires at least 2 arguments")
                    val testNode = args[0]
                    val bodyNode = args[1]
                    try {
                        while (testNode.eval(ctx).asBoolean()) {
                            ctx.tick()
                            try {
                                bodyNode.eval(ctx)
                            } catch (_: BreakException) {
                                break
                            } catch (_: ContinueException) {
                                continue
                            }
                        }
                    } catch (r: ReturnException) {
                        return r.value
                    }
                    return MathValue.NullVal
                }
                "for" -> {
                    if (args.size < 4) error("For requires 4 arguments")
                    val startNode = args[0]
                    val testNode = args[1]
                    val incrNode = args[2]
                    val bodyNode = args[3]
                    startNode.eval(ctx)
                    try {
                        while (testNode.eval(ctx).asBoolean()) {
                            ctx.tick()
                            try {
                                bodyNode.eval(ctx)
                            } catch (_: BreakException) {
                                break
                            } catch (_: ContinueException) {
                                // fall through to incr
                            }
                            incrNode.eval(ctx)
                        }
                    } catch (r: ReturnException) {
                        return r.value
                    }
                    return MathValue.NullVal
                }
                "do" -> {
                    if (args.size < 2) error("Do requires at least 2 arguments")
                    val bodyNode = args[0]
                    return try {
                        iterateMulti(args.drop(1), 0, ctx) {
                            bodyNode.eval(ctx)
                        }
                        MathValue.NullVal
                    } catch (_: BreakException) {
                        MathValue.NullVal
                    } catch (r: ReturnException) {
                        r.value
                    }
                }
                "break" -> throw BreakException()
                "continue" -> throw ContinueException()
                "return" -> throw ReturnException(args.getOrNull(0)?.eval(ctx) ?: MathValue.NullVal)
                "table" -> {
                    if (args.isEmpty()) error("Table requires arguments")
                    val exprNode = args[0]
                    if (args.size == 1) return exprNode.eval(ctx)
                    return evalTableRecursive(exprNode, args.drop(1), 0, ctx)
                }
                "sum" -> {
                    if (args.isEmpty()) error("Sum requires arguments")
                    if (args.size == 1) {
                        return MathValue.NumberVal(args[0].eval(ctx).asList().sum())
                    }
                    val exprNode = args[0]
                    var acc = 0.0
                    iterateMulti(args.drop(1), 0, ctx) {
                        acc += exprNode.eval(ctx).asDouble()
                    }
                    return MathValue.NumberVal(acc)
                }
                "product" -> {
                    if (args.isEmpty()) error("Product requires arguments")
                    if (args.size == 1) {
                        var prod = 1.0
                        args[0].eval(ctx).asList().forEach { prod *= it }
                        return MathValue.NumberVal(prod)
                    }
                    val exprNode = args[0]
                    var prod = 1.0
                    iterateMulti(args.drop(1), 0, ctx) {
                        prod *= exprNode.eval(ctx).asDouble()
                    }
                    return MathValue.NumberVal(prod)
                }
                "nintegrate" -> {
                    if (args.size < 2) error("NIntegrate requires expression and {x, a, b}")
                    return evalNIntegrate(args[0], args[1], ctx)
                }
                "nd" -> {
                    if (args.size < 3) error("ND requires expression, variable, and point")
                    return evalNumericalDerivative(args[0], args[1], args[2], ctx)
                }
                "findroot" -> {
                    if (args.size < 2) error("FindRoot requires expression and {x, x0}")
                    return evalFindRoot(args[0], args[1], ctx)
                }
                "nminimize" -> {
                    if (args.size < 2) error("NMinimize requires expression and {x, a, b}")
                    return evalNOpt(args[0], args[1], maximize = false, ctx = ctx)
                }
                "nmaximize" -> {
                    if (args.size < 2) error("NMaximize requires expression and {x, a, b}")
                    return evalNOpt(args[0], args[1], maximize = true, ctx = ctx)
                }
                "n" -> {
                    val v = args.getOrNull(0)?.eval(ctx) ?: return MathValue.NullVal
                    val prec = args.getOrNull(1)?.eval(ctx)?.asDouble()?.toInt()
                    return if (prec != null && prec in 1..15 && v is MathValue.NumberVal && v.value.isFinite()) {
                        MathValue.StringVal(formatSignificantDigits(v.value, prec))
                    } else v
                }
                "nest" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in Nest")
                    var curr = args.getOrNull(1)?.eval(ctx) ?: error("Expected expression in Nest")
                    val n = args.getOrNull(2)?.eval(ctx)?.asDouble()?.toInt()?.coerceIn(0, 1000) ?: 1
                    for (step in 0 until n) {
                        ctx.tick()
                        curr = invokeFunction(fnNode, listOf(curr), ctx)
                    }
                    return curr
                }
                "nestlist" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in NestList")
                    var curr = args.getOrNull(1)?.eval(ctx) ?: error("Expected expression in NestList")
                    val n = args.getOrNull(2)?.eval(ctx)?.asDouble()?.toInt()?.coerceIn(0, 1000) ?: 1
                    val list = mutableListOf(curr)
                    for (step in 0 until n) {
                        ctx.tick()
                        curr = invokeFunction(fnNode, listOf(curr), ctx)
                        list.add(curr)
                    }
                    return wrapSmartList(list)
                }
                "fold" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in Fold")
                    if (args.size >= 3) {
                        var acc = args[1].eval(ctx)
                        for (item in args[2].eval(ctx).asValueList()) {
                            ctx.tick()
                            acc = invokeFunction(fnNode, listOf(acc, item), ctx)
                        }
                        return acc
                    } else if (args.size == 2) {
                        val list = args[1].eval(ctx).asValueList()
                        if (list.isEmpty()) return MathValue.NumberVal(Double.NaN)
                        var acc = list[0]
                        for (i in 1 until list.size) {
                            ctx.tick()
                            acc = invokeFunction(fnNode, listOf(acc, list[i]), ctx)
                        }
                        return acc
                    } else {
                        error("Expected 2 or 3 arguments in Fold")
                    }
                }
                "foldlist" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in FoldList")
                    if (args.size >= 3) {
                        var acc = args[1].eval(ctx)
                        val res = mutableListOf(acc)
                        for (item in args[2].eval(ctx).asValueList()) {
                            ctx.tick()
                            acc = invokeFunction(fnNode, listOf(acc, item), ctx)
                            res.add(acc)
                        }
                        return wrapSmartList(res)
                    } else if (args.size == 2) {
                        val list = args[1].eval(ctx).asValueList()
                        if (list.isEmpty()) return MathValue.ListVal(emptyList())
                        var acc = list[0]
                        val res = mutableListOf(acc)
                        for (i in 1 until list.size) {
                            ctx.tick()
                            acc = invokeFunction(fnNode, listOf(acc, list[i]), ctx)
                            res.add(acc)
                        }
                        return wrapSmartList(res)
                    } else {
                        error("Expected 2 or 3 arguments in FoldList")
                    }
                }
                "select" -> {
                    val list = args.getOrNull(0)?.eval(ctx)?.asValueList() ?: emptyList()
                    val crit = args.getOrNull(1) ?: error("Expected criteria in Select")
                    val res = mutableListOf<MathValue>()
                    for (item in list) {
                        ctx.tick()
                        if (invokeFunction(crit, listOf(item), ctx).asBoolean()) res.add(item)
                    }
                    return wrapSmartList(res)
                }
                "count" -> {
                    val list = args.getOrNull(0)?.eval(ctx)?.asValueList() ?: emptyList()
                    val second = args.getOrNull(1) ?: return MathValue.NumberVal(list.size.toDouble())
                    val isCallable = isCallableNode(second, ctx)
                    var hits = 0
                    if (isCallable) {
                        for (item in list) {
                            ctx.tick()
                            if (invokeFunction(second, listOf(item), ctx).asBoolean()) hits++
                        }
                    } else {
                        val target = second.eval(ctx)
                        for (item in list) {
                            ctx.tick()
                            if (valuesEqual(item, target)) hits++
                        }
                    }
                    return MathValue.NumberVal(hits.toDouble())
                }
                "alltrue" -> {
                    val list = args.getOrNull(0)?.eval(ctx)?.asValueList() ?: emptyList()
                    val crit = args.getOrNull(1) ?: error("Expected criteria in AllTrue")
                    for (item in list) {
                        ctx.tick()
                        if (!invokeFunction(crit, listOf(item), ctx).asBoolean()) return MathValue.BooleanVal(false)
                    }
                    return MathValue.BooleanVal(true)
                }
                "anytrue" -> {
                    val list = args.getOrNull(0)?.eval(ctx)?.asValueList() ?: emptyList()
                    val crit = args.getOrNull(1) ?: error("Expected criteria in AnyTrue")
                    for (item in list) {
                        ctx.tick()
                        if (invokeFunction(crit, listOf(item), ctx).asBoolean()) return MathValue.BooleanVal(true)
                    }
                    return MathValue.BooleanVal(false)
                }
                "nonetrue" -> {
                    val list = args.getOrNull(0)?.eval(ctx)?.asValueList() ?: emptyList()
                    val crit = args.getOrNull(1) ?: error("Expected criteria in NoneTrue")
                    for (item in list) {
                        ctx.tick()
                        if (invokeFunction(crit, listOf(item), ctx).asBoolean()) return MathValue.BooleanVal(false)
                    }
                    return MathValue.BooleanVal(true)
                }
                "scan" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in Scan")
                    val list = args.getOrNull(1)?.eval(ctx)?.asValueList() ?: emptyList()
                    return try {
                        for (item in list) {
                            ctx.tick()
                            invokeFunction(fnNode, listOf(item), ctx)
                        }
                        MathValue.NullVal
                    } catch (r: ReturnException) {
                        r.value
                    }
                }
                "sortby" -> {
                    val list = args.getOrNull(0)?.eval(ctx)?.asValueList() ?: emptyList()
                    val fnNode = args.getOrNull(1) ?: error("Expected key function in SortBy")
                    val keyed = list.map { item ->
                        ctx.tick()
                        item to invokeFunction(fnNode, listOf(item), ctx).asDouble()
                    }
                    return wrapSmartList(keyed.sortedBy { it.second }.map { it.first })
                }
                "map" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in Map")
                    val list = args.getOrNull(1)?.eval(ctx)?.asValueList() ?: emptyList()
                    val res = list.map { item ->
                        ctx.tick()
                        invokeFunction(fnNode, listOf(item), ctx)
                    }
                    return wrapSmartList(res)
                }
                "apply" -> {
                    val fnNode = args.getOrNull(0) ?: error("Expected function in Apply")
                    val listVal = args.getOrNull(1)?.eval(ctx) ?: MathValue.ListVal(emptyList())
                    val level = args.getOrNull(2)?.eval(ctx)?.asDouble()?.toInt()
                    if (level == 1 && listVal is MathValue.MixedListVal) {
                        return wrapSmartList(listVal.items.map { item ->
                            ctx.tick()
                            invokeFunction(fnNode, item.asValueList(), ctx)
                        })
                    }
                    return invokeFunction(fnNode, listVal.asValueList(), ctx)
                }
            }

            val evaluatedArgs = args.map { it.eval(ctx) }

            // Check distribution constructors
            when (fn) {
                "normaldistribution" -> {
                    val mu = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val sigma = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(NormalDist(mu, sigma))
                }
                "uniformdistribution" -> {
                    var min = 0.0
                    var max = 1.0
                    if (evaluatedArgs.isNotEmpty()) {
                        val first = evaluatedArgs[0]
                        if (first is MathValue.ListVal && first.elements.size >= 2) {
                            min = first.elements[0]
                            max = first.elements[1]
                        } else {
                            min = first.asDouble()
                            max = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                        }
                    }
                    return MathValue.DistVal(UniformDist(min, max))
                }
                "exponentialdistribution" -> {
                    val lam = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    return MathValue.DistVal(ExponentialDist(lam))
                }
                "poissondistribution" -> {
                    val mu = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    return MathValue.DistVal(PoissonDist(mu))
                }
                "binomialdistribution" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble()?.toInt() ?: 1
                    val p = evaluatedArgs.getOrNull(1)?.asDouble() ?: 0.5
                    return MathValue.DistVal(BinomialDist(n, p))
                }
                "studenttdistribution" -> {
                    val nu = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    return MathValue.DistVal(StudentTDist(nu))
                }
                "cauchydistribution" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(CauchyDist(a, b))
                }
                "paretodistribution" -> {
                    val k = if (evaluatedArgs.size >= 2) evaluatedArgs[0].asDouble() else 1.0
                    val alpha = if (evaluatedArgs.size >= 2) evaluatedArgs[1].asDouble() else (evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0)
                    return MathValue.DistVal(ParetoDist(k, alpha))
                }
                "levydistribution" -> {
                    val mu = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val c = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(LevyDist(mu, c))
                }
                "frechetdistribution" -> {
                    val alpha = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    val s = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    val m = evaluatedArgs.getOrNull(2)?.asDouble() ?: 0.0
                    return MathValue.DistVal(FrechetDist(alpha, s, m))
                }
                "weibulldistribution" -> {
                    val k = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    val lam = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(WeibullDist(k, lam))
                }
                "lognormaldistribution" -> {
                    val mu = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val sigma = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(LogNormalDist(mu, sigma))
                }
                "gammadistribution" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(GammaDist(a, b))
                }
                "betadistribution" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.DistVal(BetaDist(a, b))
                }
                "geometricdistribution" -> {
                    val p = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.5
                    return MathValue.DistVal(GeometricDist(p))
                }
            }

            // Probability / Distribution operations
            when (fn) {
                "pdf" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in PDF")
                    val arg = evaluatedArgs.getOrNull(1) ?: error("Expected x in PDF")
                    return when (arg) {
                        is MathValue.SymbolVal -> MathValue.ExprVal(dist.pdfSymbolic(arg.name))
                        is MathValue.ExprVal -> MathValue.ExprVal(dist.pdfSymbolic(arg.text))
                        is MathValue.NumberVal -> MathValue.NumberVal(dist.pdf(arg.value))
                        is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { dist.pdf(it) })
                        else -> MathValue.NumberVal(dist.pdf(arg.asDouble()))
                    }
                }
                "cdf" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in CDF")
                    val arg = evaluatedArgs.getOrNull(1) ?: error("Expected x in CDF")
                    return when (arg) {
                        is MathValue.SymbolVal -> MathValue.ExprVal(dist.cdfSymbolic(arg.name))
                        is MathValue.ExprVal -> MathValue.ExprVal(dist.cdfSymbolic(arg.text))
                        is MathValue.NumberVal -> MathValue.NumberVal(dist.cdf(arg.value))
                        is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { dist.cdf(it) })
                        else -> MathValue.NumberVal(dist.cdf(arg.asDouble()))
                    }
                }
                "survivalfunction", "sf" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in SurvivalFunction")
                    val arg = evaluatedArgs.getOrNull(1) ?: error("Expected x in SurvivalFunction")
                    return when (arg) {
                        is MathValue.SymbolVal -> MathValue.ExprVal(dist.survivalSymbolic(arg.name))
                        is MathValue.ExprVal -> MathValue.ExprVal(dist.survivalSymbolic(arg.text))
                        is MathValue.NumberVal -> MathValue.NumberVal(dist.survival(arg.value))
                        is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { dist.survival(it) })
                        else -> MathValue.NumberVal(dist.survival(arg.asDouble()))
                    }
                }
                "hazardfunction", "hf" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in HazardFunction")
                    val arg = evaluatedArgs.getOrNull(1) ?: error("Expected x in HazardFunction")
                    return when (arg) {
                        is MathValue.SymbolVal -> MathValue.ExprVal(dist.hazardSymbolic(arg.name))
                        is MathValue.ExprVal -> MathValue.ExprVal(dist.hazardSymbolic(arg.text))
                        is MathValue.NumberVal -> MathValue.NumberVal(dist.hazard(arg.value))
                        is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { dist.hazard(it) })
                        else -> MathValue.NumberVal(dist.hazard(arg.asDouble()))
                    }
                }
                "inversecdf" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in InverseCDF")
                    val arg = evaluatedArgs.getOrNull(1) ?: error("Expected q in InverseCDF")
                    return when (arg) {
                        is MathValue.SymbolVal -> MathValue.ExprVal("InverseCDF[${dist.name}[${dist.paramsString}], ${arg.name}]")
                        is MathValue.ExprVal -> MathValue.ExprVal("InverseCDF[${dist.name}[${dist.paramsString}], ${arg.text}]")
                        is MathValue.NumberVal -> MathValue.NumberVal(dist.quantile(arg.value))
                        is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { dist.quantile(it) })
                        else -> MathValue.NumberVal(dist.quantile(arg.asDouble()))
                    }
                }
                "expectation" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in Expectation")
                    return MathValue.NumberVal(dist.mean())
                }
                "moment" -> {
                    val first = evaluatedArgs.getOrNull(0)
                    val r = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 1
                    if (first is MathValue.DistVal) {
                        val d = first.dist
                        return when (r) {
                            1 -> MathValue.NumberVal(d.mean())
                            2 -> MathValue.NumberVal(d.variance() + d.mean().pow(2))
                            else -> {
                                val samples = 5000
                                var sum = 0.0
                                for (i in 0 until samples) {
                                    sum += d.randomVariate(ctx.rng).pow(r)
                                }
                                MathValue.NumberVal(sum / samples)
                            }
                        }
                    } else {
                        val list = first?.asList() ?: emptyList()
                        if (list.isEmpty()) return MathValue.NumberVal(Double.NaN)
                        val m = list.map { it.pow(r) }.average()
                        return MathValue.NumberVal(m)
                    }
                }
                "centralmoment" -> {
                    val first = evaluatedArgs.getOrNull(0)
                    val r = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 1
                    if (first is MathValue.DistVal) {
                        val d = first.dist
                        return when (r) {
                            1 -> MathValue.NumberVal(0.0)
                            2 -> MathValue.NumberVal(d.variance())
                            else -> {
                                val mean = d.mean()
                                if (mean.isNaN()) MathValue.NumberVal(Double.NaN)
                                else {
                                    val samples = 5000
                                    var sum = 0.0
                                    for (i in 0 until samples) {
                                        sum += (d.randomVariate(ctx.rng) - mean).pow(r)
                                    }
                                    MathValue.NumberVal(sum / samples)
                                }
                            }
                        }
                    } else {
                        val list = first?.asList() ?: emptyList()
                        if (list.isEmpty()) return MathValue.NumberVal(Double.NaN)
                        val avg = list.average()
                        val cm = list.map { (it - avg).pow(r) }.average()
                        return MathValue.NumberVal(cm)
                    }
                }
                "randomvariate" -> {
                    val dist = (evaluatedArgs.getOrNull(0) as? MathValue.DistVal)?.dist ?: error("Expected distribution in RandomVariate")
                    val n = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                    return if (n == null) {
                        MathValue.NumberVal(dist.randomVariate(ctx.rng))
                    } else {
                        val count = n.coerceIn(0, 5000)
                        val list = (0 until count).map { dist.randomVariate(ctx.rng) }
                        MathValue.ListVal(list)
                    }
                }
                "randominteger" -> {
                    var min = 0L
                    var max = 1L
                    var count: Int? = null

                    if (evaluatedArgs.isEmpty()) {
                        min = 0L
                        max = 1L
                    } else {
                        val first = evaluatedArgs[0]
                        if (first is MathValue.ListVal && first.elements.isNotEmpty()) {
                            if (first.elements.size >= 2) {
                                min = first.elements[0].toLong()
                                max = first.elements[1].toLong()
                            } else {
                                min = 0L
                                max = first.elements[0].toLong()
                            }
                            count = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                        } else {
                            val v = first.asDouble().toLong()
                            if (evaluatedArgs.size >= 2) {
                                min = 0L
                                max = v
                                count = evaluatedArgs[1].asDouble().toInt()
                            } else {
                                min = 0L
                                max = v
                            }
                        }
                    }

                    fun randInt(a: Long, b: Long): Long {
                        val low = minOf(a, b)
                        val high = maxOf(a, b)
                        if (low == high) return low
                        val range = high - low + 1L
                        val r = if (range in 1..Int.MAX_VALUE) ctx.rng.nextInt(range.toInt()).toLong() else (ctx.rng.nextDouble() * range).toLong()
                        return low + r
                    }

                    return if (count == null) {
                        MathValue.NumberVal(randInt(min, max).toDouble())
                    } else {
                        val n = count.coerceIn(0, 5000)
                        MathValue.ListVal((0 until n).map { randInt(min, max).toDouble() })
                    }
                }
                "randomreal" -> {
                    var min = 0.0
                    var max = 1.0
                    var count: Int? = null

                    if (evaluatedArgs.isEmpty()) {
                        min = 0.0
                        max = 1.0
                    } else {
                        val first = evaluatedArgs[0]
                        if (first is MathValue.ListVal && first.elements.isNotEmpty()) {
                            if (first.elements.size >= 2) {
                                min = first.elements[0]
                                max = first.elements[1]
                            } else {
                                min = 0.0
                                max = first.elements[0]
                            }
                            count = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                        } else {
                            val v = first.asDouble()
                            if (evaluatedArgs.size >= 2) {
                                min = 0.0
                                max = v
                                count = evaluatedArgs[1].asDouble().toInt()
                            } else {
                                min = 0.0
                                max = v
                            }
                        }
                    }

                    fun randReal(a: Double, b: Double): Double {
                        val low = minOf(a, b)
                        val high = maxOf(a, b)
                        return low + ctx.rng.nextDouble() * (high - low)
                    }

                    return if (count == null) {
                        MathValue.NumberVal(randReal(min, max))
                    } else {
                        val n = count.coerceIn(0, 5000)
                        MathValue.ListVal((0 until n).map { randReal(min, max) })
                    }
                }
                "randomchoice" -> {
                    val first = evaluatedArgs.getOrNull(0) ?: error("Expected list in RandomChoice")
                    val list = first.asValueList()
                    if (list.isEmpty()) error("Empty list in RandomChoice")
                    val count = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                    return if (count == null) {
                        list[ctx.rng.nextInt(list.size)]
                    } else {
                        val n = count.coerceIn(0, 5000)
                        val res = (0 until n).map { list[ctx.rng.nextInt(list.size)] }
                        wrapSmartList(res)
                    }
                }
                "randomsample" -> {
                    val first = evaluatedArgs.getOrNull(0) ?: error("Expected list in RandomSample")
                    val list = first.asValueList()
                    val count = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: list.size
                    val k = count.coerceIn(0, list.size)
                    val shuffled = list.toMutableList()
                    for (i in shuffled.lastIndex downTo 1) {
                        val j = ctx.rng.nextInt(i + 1)
                        val tmp = shuffled[i]
                        shuffled[i] = shuffled[j]
                        shuffled[j] = tmp
                    }
                    return wrapSmartList(shuffled.take(k))
                }
            }

            // List creation, matrices, sets & string helpers
            when (fn) {
                "range" -> {
                    val n1 = evaluatedArgs.getOrNull(0)?.asDouble()?.toInt() ?: 1
                    val n2 = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                    val step = evaluatedArgs.getOrNull(2)?.asDouble()?.toInt() ?: 1
                    val list = mutableListOf<Double>()
                    if (n2 == null) {
                        val max = minOf(n1, 5000)
                        for (i in 1..max) list.add(i.toDouble())
                    } else {
                        if (step > 0 && n1 <= n2) {
                            var curr = n1
                            while (curr <= n2 && list.size < 5000) {
                                list.add(curr.toDouble())
                                curr += step
                            }
                        } else if (step < 0 && n1 >= n2) {
                            var curr = n1
                            while (curr >= n2 && list.size < 5000) {
                                list.add(curr.toDouble())
                                curr += step
                            }
                        }
                    }
                    return MathValue.ListVal(list)
                }
                "subdivide" -> {
                    val a: Double
                    val b: Double
                    val n: Int
                    when (evaluatedArgs.size) {
                        1 -> { a = 0.0; b = 1.0; n = evaluatedArgs[0].asDouble().toInt().coerceIn(1, 5000) }
                        2 -> { a = 0.0; b = evaluatedArgs[0].asDouble(); n = evaluatedArgs[1].asDouble().toInt().coerceIn(1, 5000) }
                        else -> {
                            a = evaluatedArgs[0].asDouble()
                            b = evaluatedArgs[1].asDouble()
                            n = evaluatedArgs[2].asDouble().toInt().coerceIn(1, 5000)
                        }
                    }
                    return MathValue.ListVal((0..n).map { i -> a + (b - a) * (i.toDouble() / n.toDouble()) })
                }
                "constantarray" -> {
                    val c = evaluatedArgs.getOrNull(0) ?: MathValue.NumberVal(0.0)
                    val n = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()?.coerceIn(0, 5000) ?: 0
                    return wrapSmartList(List(n) { c })
                }
                "length" -> {
                    val first = evaluatedArgs.getOrNull(0)
                    return when (first) {
                        is MathValue.ListVal -> MathValue.NumberVal(first.elements.size.toDouble())
                        is MathValue.MixedListVal -> MathValue.NumberVal(first.items.size.toDouble())
                        is MathValue.StringVal -> MathValue.NumberVal(0.0)
                        else -> MathValue.NumberVal(1.0)
                    }
                }
                "first" -> {
                    val items = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    return items.firstOrNull() ?: MathValue.NumberVal(Double.NaN)
                }
                "last" -> {
                    val items = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    return items.lastOrNull() ?: MathValue.NumberVal(Double.NaN)
                }
                "take" -> {
                    val items = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    val n = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: items.size
                    return wrapSmartList(if (n >= 0) items.take(n) else items.takeLast(-n))
                }
                "drop" -> {
                    val items = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    val n = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 0
                    return wrapSmartList(if (n >= 0) items.drop(n) else items.dropLast(-n))
                }
                "reverse" -> {
                    val items = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    return wrapSmartList(items.reversed())
                }
                "sort" -> {
                    val first = evaluatedArgs.getOrNull(0)
                    if (first is MathValue.MixedListVal) {
                        val sorted = first.items.sortedWith { a, b ->
                            val da = a.asDouble()
                            val db = b.asDouble()
                            if (!da.isNaN() && !db.isNaN()) da.compareTo(db)
                            else a.toDisplayString().compareTo(b.toDisplayString())
                        }
                        return wrapSmartList(sorted)
                    }
                    return MathValue.ListVal((first?.asList() ?: emptyList()).sorted())
                }
                "differences" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    if (list.size < 2) return MathValue.ListVal(emptyList())
                    val diffs = (0 until list.size - 1).map { list[it + 1] - list[it] }
                    return MathValue.ListVal(diffs)
                }
                "accumulate" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    var sum = 0.0
                    val acc = list.map { sum += it; sum }
                    return MathValue.ListVal(acc)
                }
                "deleteduplicates" -> {
                    val items = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    val unique = mutableListOf<MathValue>()
                    for (item in items) {
                        if (unique.none { valuesEqual(it, item) }) unique.add(item)
                    }
                    return wrapSmartList(unique)
                }
                "flatten" -> {
                    val first = evaluatedArgs.getOrNull(0) ?: return MathValue.ListVal(emptyList())
                    val level = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                    return flattenValue(first, level)
                }
                "join" -> {
                    val out = mutableListOf<MathValue>()
                    evaluatedArgs.forEach { arg ->
                        when (arg) {
                            is MathValue.ListVal -> out.addAll(arg.elements.map { MathValue.NumberVal(it) })
                            is MathValue.MixedListVal -> out.addAll(arg.items)
                            else -> out.add(arg)
                        }
                    }
                    return wrapSmartList(out)
                }
                "append" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    val item = evaluatedArgs.getOrNull(1) ?: return wrapSmartList(list)
                    return wrapSmartList(list + item)
                }
                "prepend" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    val item = evaluatedArgs.getOrNull(1) ?: return wrapSmartList(list)
                    return wrapSmartList(listOf(item) + list)
                }
                "partition" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    val n = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()?.coerceAtLeast(1) ?: 1
                    val d = evaluatedArgs.getOrNull(2)?.asDouble()?.toInt()?.coerceAtLeast(1) ?: n
                    val chunks = mutableListOf<MathValue>()
                    var i = 0
                    while (i + n <= list.size && chunks.size < 2000) {
                        chunks.add(wrapSmartList(list.subList(i, i + n)))
                        i += d
                    }
                    return wrapSmartList(chunks)
                }
                "riffle" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    val sep = evaluatedArgs.getOrNull(1) ?: return wrapSmartList(list)
                    if (list.size <= 1) return wrapSmartList(list)
                    val out = mutableListOf<MathValue>()
                    for (i in list.indices) {
                        if (i > 0) out.add(sep)
                        out.add(list[i])
                    }
                    return wrapSmartList(out)
                }
                "minmax" -> {
                    val nums = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    if (nums.isEmpty()) return MathValue.ListVal(emptyList())
                    return MathValue.ListVal(listOf(nums.min(), nums.max()))
                }
                "union" -> {
                    val out = mutableListOf<MathValue>()
                    evaluatedArgs.forEach { arg ->
                        arg.asValueList().forEach { item ->
                            if (out.none { valuesEqual(it, item) }) out.add(item)
                        }
                    }
                    out.sortWith { a, b ->
                        val da = a.asDouble()
                        val db = b.asDouble()
                        if (!da.isNaN() && !db.isNaN()) da.compareTo(db) else a.toDisplayString().compareTo(b.toDisplayString())
                    }
                    return wrapSmartList(out)
                }
                "intersection" -> {
                    if (evaluatedArgs.isEmpty()) return MathValue.ListVal(emptyList())
                    val firstItems = evaluatedArgs[0].asValueList()
                    val others = evaluatedArgs.drop(1).map { it.asValueList() }
                    val common = mutableListOf<MathValue>()
                    for (item in firstItems) {
                        if (common.none { valuesEqual(it, item) } &&
                            others.all { list -> list.any { valuesEqual(it, item) } }
                        ) {
                            common.add(item)
                        }
                    }
                    common.sortWith { a, b ->
                        val da = a.asDouble()
                        val db = b.asDouble()
                        if (!da.isNaN() && !db.isNaN()) da.compareTo(db) else a.toDisplayString().compareTo(b.toDisplayString())
                    }
                    return wrapSmartList(common)
                }
                "complement" -> {
                    val firstItems = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    val excl = evaluatedArgs.drop(1).flatMap { it.asValueList() }
                    val diff = mutableListOf<MathValue>()
                    for (item in firstItems) {
                        if (diff.none { valuesEqual(it, item) } && excl.none { valuesEqual(it, item) }) {
                            diff.add(item)
                        }
                    }
                    diff.sortWith { a, b ->
                        val da = a.asDouble()
                        val db = b.asDouble()
                        if (!da.isNaN() && !db.isNaN()) da.compareTo(db) else a.toDisplayString().compareTo(b.toDisplayString())
                    }
                    return wrapSmartList(diff)
                }
                "memberq" -> {
                    val items = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    val target = evaluatedArgs.getOrNull(1) ?: return MathValue.BooleanVal(false)
                    return MathValue.BooleanVal(items.any { valuesEqual(it, target) })
                }
                "position" -> {
                    val items = evaluatedArgs.getOrNull(0)?.asValueList() ?: emptyList()
                    val target = evaluatedArgs.getOrNull(1) ?: return MathValue.ListVal(emptyList())
                    val matches = mutableListOf<MathValue>()
                    items.forEachIndexed { idx, item ->
                        if (valuesEqual(item, target)) matches.add(MathValue.ListVal(listOf((idx + 1).toDouble())))
                    }
                    return wrapSmartList(matches)
                }
                "part" -> {
                    val target = evaluatedArgs.getOrNull(0) ?: return MathValue.NumberVal(Double.NaN)
                    val specNodes = args.drop(1)
                    return if (specNodes.isEmpty()) target else resolvePart(target, specNodes, 0, ctx)
                }
                "dot" -> {
                    val a = evaluatedArgs.getOrNull(0) ?: error("Dot requires 2 arguments")
                    val b = evaluatedArgs.getOrNull(1) ?: error("Dot requires 2 arguments")
                    val mA = a.toNumericMatrix()
                    val mB = b.toNumericMatrix()
                    return when {
                        mA == null && mB == null -> {
                            val vA = a.asList()
                            val vB = b.asList()
                            val len = minOf(vA.size, vB.size)
                            var acc = 0.0
                            for (i in 0 until len) acc += vA[i] * vB[i]
                            MathValue.NumberVal(acc)
                        }
                        mA != null && mB == null -> {
                            val vB = b.asList()
                            val res = mA.map { row ->
                                val len = minOf(row.size, vB.size)
                                var acc = 0.0
                                for (i in 0 until len) acc += row[i] * vB[i]
                                acc
                            }
                            MathValue.ListVal(res)
                        }
                        mA == null && mB != null -> {
                            val vA = a.asList()
                            val cols = mB[0].size
                            val rows = minOf(vA.size, mB.size)
                            val res = (0 until cols).map { c ->
                                var acc = 0.0
                                for (r in 0 until rows) acc += vA[r] * mB[r][c]
                                acc
                            }
                            MathValue.ListVal(res)
                        }
                        else -> {
                            val rowsA = mA!!
                            val rowsB = mB!!
                            val colsA = rowsA[0].size
                            val colsB = rowsB[0].size
                            if (colsA != rowsB.size) error("Incompatible matrix dimensions")
                            val out = rowsA.map { r ->
                                val rowOut = (0 until colsB).map { c ->
                                    var acc = 0.0
                                    for (k in 0 until colsA) acc += r[k] * rowsB[k][c]
                                    cleanZero(acc)
                                }
                                MathValue.ListVal(rowOut)
                            }
                            MathValue.MixedListVal(out)
                        }
                    }
                }
                "norm" -> {
                    val v = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    if (v.isEmpty()) return MathValue.NumberVal(0.0)
                    val pVal = evaluatedArgs.getOrNull(1)?.asDouble() ?: 2.0
                    val res = when {
                        pVal.isInfinite() -> v.maxOf { abs(it) }
                        abs(pVal - 2.0) < 1e-9 -> sqrt(v.sumOf { it * it })
                        abs(pVal - 1.0) < 1e-9 -> v.sumOf { abs(it) }
                        else -> v.sumOf { abs(it).pow(pVal) }.pow(1.0 / pVal)
                    }
                    return MathValue.NumberVal(res)
                }
                "cross" -> {
                    val u = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val v = evaluatedArgs.getOrNull(1)?.asList() ?: emptyList()
                    if (u.size < 3 || v.size < 3) error("Cross requires 3D vectors")
                    return MathValue.ListVal(
                        listOf(
                            u[1] * v[2] - u[2] * v[1],
                            u[2] * v[0] - u[0] * v[2],
                            u[0] * v[1] - u[1] * v[0]
                        )
                    )
                }
                "transpose" -> {
                    val m = evaluatedArgs.getOrNull(0)?.toNumericMatrix() ?: error("Expected rectangular matrix in Transpose")
                    val rows = m.size
                    val cols = m[0].size
                    val t = (0 until cols).map { c ->
                        MathValue.ListVal((0 until rows).map { r -> m[r][c] })
                    }
                    return MathValue.MixedListVal(t)
                }
                "tr" -> {
                    val arg = evaluatedArgs.getOrNull(0)
                    val m = arg?.toNumericMatrix()
                    return if (m != null) {
                        val n = minOf(m.size, m[0].size)
                        MathValue.NumberVal((0 until n).sumOf { i -> m[i][i] })
                    } else {
                        MathValue.NumberVal(arg?.asList()?.sum() ?: 0.0)
                    }
                }
                "dimensions" -> {
                    val arg = evaluatedArgs.getOrNull(0)
                    val m = arg?.toNumericMatrix()
                    return when {
                        m != null -> MathValue.ListVal(listOf(m.size.toDouble(), m[0].size.toDouble()))
                        arg is MathValue.ListVal -> MathValue.ListVal(listOf(arg.elements.size.toDouble()))
                        arg is MathValue.MixedListVal -> MathValue.ListVal(listOf(arg.items.size.toDouble()))
                        else -> MathValue.ListVal(emptyList())
                    }
                }
                "identitymatrix" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble()?.toInt()?.coerceIn(1, 20) ?: 1
                    return MathValue.MixedListVal((0 until n).map { r ->
                        MathValue.ListVal((0 until n).map { c -> if (r == c) 1.0 else 0.0 })
                    })
                }
                "diagonalmatrix" -> {
                    val diag = evaluatedArgs.getOrNull(0)?.asList()?.take(20) ?: emptyList()
                    val n = diag.size
                    return MathValue.MixedListVal((0 until n).map { r ->
                        MathValue.ListVal((0 until n).map { c -> if (r == c) diag[r] else 0.0 })
                    })
                }
                "det" -> {
                    val m = evaluatedArgs.getOrNull(0)?.toNumericMatrix() ?: error("Expected matrix in Det")
                    if (m.size != m[0].size || m.size > 20) error("Det requires square matrix (<= 20x20)")
                    return MathValue.NumberVal(matrixDet(m))
                }
                "inverse" -> {
                    val m = evaluatedArgs.getOrNull(0)?.toNumericMatrix() ?: error("Expected matrix in Inverse")
                    if (m.size != m[0].size || m.size > 20) error("Inverse requires square matrix (<= 20x20)")
                    val inv = matrixInverse(m)
                    return MathValue.MixedListVal(inv.map { MathValue.ListVal(it) })
                }
                "stringjoin" -> {
                    return MathValue.StringVal(evaluatedArgs.joinToString("") { it.toDisplayString() })
                }
                "tostring" -> {
                    return MathValue.StringVal(evaluatedArgs.getOrNull(0)?.toDisplayString() ?: "")
                }
                "stringlength" -> {
                    val s = evaluatedArgs.getOrNull(0)?.toDisplayString() ?: ""
                    return MathValue.NumberVal(s.length.toDouble())
                }
                "stringreverse" -> {
                    val s = evaluatedArgs.getOrNull(0)?.toDisplayString() ?: ""
                    return MathValue.StringVal(s.reversed())
                }
                "touppercase" -> {
                    val s = evaluatedArgs.getOrNull(0)?.toDisplayString() ?: ""
                    return MathValue.StringVal(s.uppercase(Locale.ROOT))
                }
                "tolowercase" -> {
                    val s = evaluatedArgs.getOrNull(0)?.toDisplayString() ?: ""
                    return MathValue.StringVal(s.lowercase(Locale.ROOT))
                }
            }

            // Statistics & Aggregations
            val firstArg = evaluatedArgs.getOrNull(0)
            if (firstArg is MathValue.DistVal) {
                val d = firstArg.dist
                when (fn) {
                    "mean" -> return MathValue.NumberVal(d.mean())
                    "median" -> return MathValue.NumberVal(d.median())
                    "variance", "var" -> return MathValue.NumberVal(d.variance())
                    "standarddeviation", "stddev", "sd" -> return MathValue.NumberVal(d.standardDeviation())
                    "quantile" -> {
                        val arg = evaluatedArgs.getOrNull(1) ?: MathValue.NumberVal(0.5)
                        return when (arg) {
                            is MathValue.NumberVal -> MathValue.NumberVal(d.quantile(arg.value))
                            is MathValue.ListVal -> MathValue.ListVal(arg.elements.map { d.quantile(it) })
                            else -> MathValue.NumberVal(d.quantile(arg.asDouble()))
                        }
                    }
                }
            }

            val numbers: List<Double> = when (firstArg) {
                is MathValue.ListVal -> firstArg.elements
                is MathValue.MixedListVal -> firstArg.asList()
                is MathValue.NumberVal -> evaluatedArgs.map { it.asDouble() }
                else -> emptyList()
            }

            when (fn) {
                "total" -> return MathValue.NumberVal(numbers.sum())
                "mean" -> return MathValue.NumberVal(if (numbers.isEmpty()) Double.NaN else numbers.average())
                "median" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(Double.NaN)
                    val s = numbers.sorted()
                    val m = if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
                    return MathValue.NumberVal(m)
                }
                "variance", "var" -> {
                    if (numbers.size < 2) return MathValue.NumberVal(Double.NaN)
                    val avg = numbers.average()
                    val sumSq = numbers.sumOf { (it - avg).pow(2) }
                    return MathValue.NumberVal(sumSq / (numbers.size - 1))
                }
                "standarddeviation", "stddev", "sd" -> {
                    if (numbers.size < 2) return MathValue.NumberVal(Double.NaN)
                    val avg = numbers.average()
                    val sumSq = numbers.sumOf { (it - avg).pow(2) }
                    return MathValue.NumberVal(sqrt(sumSq / (numbers.size - 1)))
                }
                "quantile" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(Double.NaN)
                    val q = evaluatedArgs.getOrNull(1)?.asDouble()?.coerceIn(0.0, 1.0) ?: 0.5
                    val s = numbers.sorted()
                    val pos = q * (s.size - 1)
                    val low = floor(pos).toInt()
                    val high = ceil(pos).toInt()
                    val weight = pos - low
                    val res = s[low] * (1.0 - weight) + s[high] * weight
                    return MathValue.NumberVal(res)
                }
                "percentile" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(Double.NaN)
                    val p = (evaluatedArgs.getOrNull(1)?.asDouble() ?: 50.0) / 100.0
                    val q = p.coerceIn(0.0, 1.0)
                    val s = numbers.sorted()
                    val pos = q * (s.size - 1)
                    val low = floor(pos).toInt()
                    val high = ceil(pos).toInt()
                    val weight = pos - low
                    val res = s[low] * (1.0 - weight) + s[high] * weight
                    return MathValue.NumberVal(res)
                }
                "interquartilerange", "iqr" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(Double.NaN)
                    val s = numbers.sorted()
                    fun qAt(q: Double): Double {
                        val pos = q * (s.size - 1)
                        val low = floor(pos).toInt()
                        val high = ceil(pos).toInt()
                        return s[low] * (1.0 - (pos - low)) + s[high] * (pos - low)
                    }
                    return MathValue.NumberVal(qAt(0.75) - qAt(0.25))
                }
                "geometricmean" -> {
                    if (numbers.isEmpty() || numbers.any { it <= 0.0 }) return MathValue.NumberVal(Double.NaN)
                    val avgLn = numbers.map { ln(it) }.average()
                    return MathValue.NumberVal(exp(avgLn))
                }
                "harmonicmean" -> {
                    if (numbers.isEmpty() || numbers.any { it <= 0.0 }) return MathValue.NumberVal(Double.NaN)
                    val sumInv = numbers.sumOf { 1.0 / it }
                    return MathValue.NumberVal(numbers.size / sumInv)
                }
                "rootmeansquare", "rms" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(Double.NaN)
                    val meanSq = numbers.map { it * it }.average()
                    return MathValue.NumberVal(sqrt(meanSq))
                }
                "skewness" -> {
                    if (numbers.size < 3) return MathValue.NumberVal(Double.NaN)
                    val avg = numbers.average()
                    val m2 = numbers.map { (it - avg).pow(2) }.average()
                    val m3 = numbers.map { (it - avg).pow(3) }.average()
                    val s = sqrt(m2)
                    return MathValue.NumberVal(if (s == 0.0) 0.0 else m3 / s.pow(3))
                }
                "kurtosis" -> {
                    if (numbers.size < 4) return MathValue.NumberVal(Double.NaN)
                    val avg = numbers.average()
                    val m2 = numbers.map { (it - avg).pow(2) }.average()
                    val m4 = numbers.map { (it - avg).pow(4) }.average()
                    return MathValue.NumberVal(if (m2 == 0.0) 0.0 else (m4 / (m2 * m2)) - 3.0)
                }
                "covariance" -> {
                    val l1 = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val l2 = evaluatedArgs.getOrNull(1)?.asList() ?: emptyList()
                    val n = minOf(l1.size, l2.size)
                    if (n < 2) return MathValue.NumberVal(Double.NaN)
                    val a1 = l1.take(n).average()
                    val a2 = l2.take(n).average()
                    var cov = 0.0
                    for (i in 0 until n) cov += (l1[i] - a1) * (l2[i] - a2)
                    return MathValue.NumberVal(cov / (n - 1))
                }
                "correlation" -> {
                    val l1 = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val l2 = evaluatedArgs.getOrNull(1)?.asList() ?: emptyList()
                    val n = minOf(l1.size, l2.size)
                    if (n < 2) return MathValue.NumberVal(Double.NaN)
                    val a1 = l1.take(n).average()
                    val a2 = l2.take(n).average()
                    var cov = 0.0
                    var v1 = 0.0
                    var v2 = 0.0
                    for (i in 0 until n) {
                        val d1 = l1[i] - a1
                        val d2 = l2[i] - a2
                        cov += d1 * d2
                        v1 += d1 * d1
                        v2 += d2 * d2
                    }
                    val denom = sqrt(v1 * v2)
                    return MathValue.NumberVal(if (denom == 0.0) 0.0 else cov / denom)
                }
                "standardize" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    if (list.size < 2) return MathValue.ListVal(list)
                    val avg = list.average()
                    val variance = list.sumOf { (it - avg).pow(2) } / (list.size - 1)
                    val sd = sqrt(variance)
                    if (sd == 0.0) return MathValue.ListVal(list.map { 0.0 })
                    return MathValue.ListVal(list.map { (it - avg) / sd })
                }
                "movingaverage" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val r = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 1
                    if (r < 1 || r > list.size) return MathValue.ListVal(emptyList())
                    val ma = mutableListOf<Double>()
                    var windowSum = (0 until r).sumOf { list[it] }
                    ma.add(windowSum / r)
                    for (i in r until list.size) {
                        windowSum += list[i] - list[i - r]
                        ma.add(windowSum / r)
                    }
                    return MathValue.ListVal(ma)
                }
                "maxdrawdown" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    if (list.isEmpty()) return MathValue.NumberVal(0.0)
                    var peak = list[0]
                    var maxDd = 0.0
                    for (p in list) {
                        if (p > peak) peak = p
                        val dd = if (peak != 0.0) (peak - p) / abs(peak) else 0.0
                        if (dd > maxDd) maxDd = dd
                    }
                    return MathValue.NumberVal(maxDd)
                }
                "hillestimator" -> {
                    val list = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val pos = list.filter { it > 0.0 }.sortedDescending()
                    if (pos.size < 3) return MathValue.NumberVal(Double.NaN)
                    val kArg = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt()
                    val k = (kArg ?: maxOf(2, minOf(pos.size / 4, 100))).coerceIn(1, pos.size - 1)
                    val thresh = pos[k]
                    val logThresh = ln(thresh)
                    var sumLog = 0.0
                    for (i in 0 until k) {
                        sumLog += (ln(pos[i]) - logThresh)
                    }
                    val gamma = sumLog / k
                    if (gamma <= 0.0) return MathValue.NumberVal(Double.NaN)
                    val alpha = 1.0 / gamma
                    return MathValue.NumberVal(alpha)
                }
                "expectedshortfall", "cvar" -> {
                    val first = evaluatedArgs.getOrNull(0)
                    val q = evaluatedArgs.getOrNull(1)?.asDouble() ?: 0.95
                    if (first is MathValue.DistVal) {
                        val d = first.dist
                        val qClamped = q.coerceIn(0.001, 0.999)
                        val steps = 100
                        if (qClamped >= 0.5) {
                            val stepSize = (1.0 - qClamped) / steps
                            var sum = 0.0
                            for (i in 0 until steps) {
                                val mid = qClamped + (i + 0.5) * stepSize
                                sum += d.quantile(mid)
                            }
                            return MathValue.NumberVal(sum / steps)
                        } else {
                            val stepSize = qClamped / steps
                            var sum = 0.0
                            for (i in 0 until steps) {
                                val mid = (i + 0.5) * stepSize
                                sum += d.quantile(mid)
                            }
                            return MathValue.NumberVal(sum / steps)
                        }
                    } else {
                        val list = first?.asList() ?: emptyList()
                        if (list.isEmpty()) return MathValue.NumberVal(Double.NaN)
                        val sorted = list.sorted()
                        val qClamped = q.coerceIn(0.0, 1.0)
                        val pos = qClamped * (sorted.size - 1)
                        val low = floor(pos).toInt().coerceIn(0, sorted.size - 1)
                        val high = ceil(pos).toInt().coerceIn(0, sorted.size - 1)
                        val weight = pos - low
                        val qVal = if (low == high) sorted[low] else sorted[low] * (1.0 - weight) + sorted[high] * weight

                        val tail = if (qClamped >= 0.5) {
                            sorted.filter { it >= qVal - 1e-9 }
                        } else {
                            sorted.filter { it <= qVal + 1e-9 }
                        }
                        val es = if (tail.isNotEmpty()) tail.average() else qVal
                        return MathValue.NumberVal(es)
                    }
                }
                "min" -> return MathValue.NumberVal(numbers.minOrNull() ?: Double.NaN)
                "max" -> return MathValue.NumberVal(numbers.maxOrNull() ?: Double.NaN)
            }

            // Element-wise unary functions (recursive over nested lists)
            fun applyUnaryVal(v: MathValue, func: (Double) -> Double): MathValue = when (v) {
                is MathValue.NumberVal -> MathValue.NumberVal(func(v.value))
                is MathValue.ListVal -> MathValue.ListVal(v.elements.map(func))
                is MathValue.MixedListVal -> wrapSmartList(v.items.map { applyUnaryVal(it, func) })
                else -> MathValue.NumberVal(Double.NaN)
            }
            fun applyUnary(func: (Double) -> Double): MathValue {
                val first = evaluatedArgs.getOrNull(0) ?: return MathValue.NumberVal(Double.NaN)
                return applyUnaryVal(first, func)
            }

            // Predicates
            when (fn) {
                "evenq" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble() ?: return MathValue.BooleanVal(false)
                    val r = kotlin.math.round(n)
                    return MathValue.BooleanVal(abs(n - r) < 1e-9 && (r.toLong() % 2L == 0L))
                }
                "oddq" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble() ?: return MathValue.BooleanVal(false)
                    val r = kotlin.math.round(n)
                    return MathValue.BooleanVal(abs(n - r) < 1e-9 && (r.toLong() % 2L != 0L))
                }
                "integerq" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble() ?: return MathValue.BooleanVal(false)
                    return MathValue.BooleanVal(!n.isNaN() && !n.isInfinite() && abs(n - kotlin.math.round(n)) < 1e-9)
                }
                "positive" -> return MathValue.BooleanVal((evaluatedArgs.getOrNull(0)?.asDouble() ?: Double.NaN) > 0.0)
                "negative" -> return MathValue.BooleanVal((evaluatedArgs.getOrNull(0)?.asDouble() ?: Double.NaN) < 0.0)
                "zeroq" -> return MathValue.BooleanVal(abs(evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0) < 1e-11)
                "boole" -> {
                    val b = evaluatedArgs.getOrNull(0)?.asBoolean() ?: false
                    return MathValue.NumberVal(if (b) 1.0 else 0.0)
                }
            }

            when (fn) {
                "sqrt" -> return applyUnary { sqrt(it) }
                "cuberoot" -> return applyUnary { if (it >= 0.0) it.pow(1.0 / 3.0) else -(-it).pow(1.0 / 3.0) }
                "abs" -> return applyUnary { abs(it) }
                "sign" -> return applyUnary { sign(it) }
                "round" -> {
                    val step = evaluatedArgs.getOrNull(1)?.asDouble()
                    return if (step != null && step > 0.0) applyUnary { kotlin.math.round(it / step) * step }
                    else applyUnary { kotlin.math.round(it) }
                }
                "floor" -> return applyUnary { floor(it) }
                "ceiling", "ceil" -> return applyUnary { ceil(it) }
                "sin" -> return applyUnary { sin(it) }
                "cos" -> return applyUnary { cos(it) }
                "tan" -> return applyUnary { tan(it) }
                "cot" -> return applyUnary { 1.0 / tan(it) }
                "sec" -> return applyUnary { 1.0 / cos(it) }
                "csc" -> return applyUnary { 1.0 / sin(it) }
                "arcsin", "asin" -> return applyUnary { asin(it) }
                "arccos", "acos" -> return applyUnary { acos(it) }
                "arctan", "atan" -> {
                    if (evaluatedArgs.size >= 2) {
                        val x = evaluatedArgs[0].asDouble()
                        val y = evaluatedArgs[1].asDouble()
                        return MathValue.NumberVal(atan2(y, x))
                    }
                    return applyUnary { atan(it) }
                }
                "arccot" -> return applyUnary { if (it >= 0.0) atan2(1.0, it) else atan2(-1.0, -it) - PI }
                "arcsec" -> return applyUnary { acos(1.0 / it) }
                "arccsc" -> return applyUnary { asin(1.0 / it) }
                "sinh" -> return applyUnary { sinh(it) }
                "cosh" -> return applyUnary { cosh(it) }
                "tanh" -> return applyUnary { tanh(it) }
                "coth" -> return applyUnary { 1.0 / tanh(it) }
                "sech" -> return applyUnary { 1.0 / cosh(it) }
                "csch" -> return applyUnary { 1.0 / sinh(it) }
                "arcsinh", "asinh" -> return applyUnary { asinh(it) }
                "arccosh", "acosh" -> return applyUnary { acosh(it) }
                "arctanh", "atanh" -> return applyUnary { atanh(it) }
                "sinc" -> return applyUnary { if (abs(it) < 1e-8) 1.0 - it * it / 6.0 else sin(it) / it }
                "logisticsigmoid" -> return applyUnary { 1.0 / (1.0 + exp(-it)) }
                "gamma" -> return applyUnary { x ->
                    if (x <= 0.0 && abs(x - kotlin.math.round(x)) < 1e-9) Double.NaN else gamma(x)
                }
                "loggamma" -> return applyUnary { x -> if (x <= 0.0) Double.NaN else lnGamma(x) }
                "erf" -> return applyUnary { erf(it) }
                "erfc" -> return applyUnary { erfc(it) }
                "beta" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 1.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.NumberVal(exp(lnGamma(a) + lnGamma(b) - lnGamma(a + b)))
                }
                "harmonicnumber" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble()?.toInt() ?: 0
                    if (n < 0 || n > 100_000) return MathValue.NumberVal(Double.NaN)
                    var h = 0.0
                    for (i in 1..n) {
                        if ((i and 255) == 0) ctx.tick()
                        h += 1.0 / i
                    }
                    return MathValue.NumberVal(h)
                }
                "deg" -> return applyUnary { it * 180.0 / PI }
                "rad" -> return applyUnary { it * PI / 180.0 }
                "exp" -> return applyUnary { exp(it) }
                "log", "ln" -> {
                    if (evaluatedArgs.size >= 2) {
                        val b = evaluatedArgs[0].asDouble()
                        val second = evaluatedArgs[1]
                        return applyUnaryVal(second) { ln(it) / ln(b) }
                    }
                    return applyUnary { ln(it) }
                }
                "log2" -> return applyUnary { ln(it) / ln(2.0) }
                "log10" -> return applyUnary { log10(it) }
                "mod" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    val d = evaluatedArgs.getOrNull(2)?.asDouble() ?: 0.0
                    return MathValue.NumberVal(if (b == 0.0) Double.NaN else a - b * floor((a - d) / b))
                }
                "quotient" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val b = evaluatedArgs.getOrNull(1)?.asDouble() ?: 1.0
                    return MathValue.NumberVal(if (b == 0.0) Double.NaN else floor(a / b))
                }
                "divisible" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble()?.toLong() ?: 0L
                    val b = evaluatedArgs.getOrNull(1)?.asDouble()?.toLong() ?: 1L
                    return MathValue.BooleanVal(b != 0L && a % b == 0L)
                }
                "gcd" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(0.0)
                    var acc = numbers[0].toLong()
                    for (i in 1 until numbers.size) {
                        acc = MathStatsEngine.gcd(acc, numbers[i].toLong())
                    }
                    return MathValue.NumberVal(abs(acc).toDouble())
                }
                "lcm" -> {
                    if (numbers.isEmpty()) return MathValue.NumberVal(0.0)
                    var acc = numbers[0].toLong()
                    for (i in 1 until numbers.size) {
                        acc = MathStatsEngine.lcm(acc, numbers[i].toLong())
                    }
                    return MathValue.NumberVal(abs(acc).toDouble())
                }
                "factorinteger" -> {
                    val nRaw = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val n = kotlin.math.round(nRaw).toLong()
                    if (abs(n.toDouble()) > 1e12) error("Number too large for FactorInteger")
                    return factorIntegerValue(n, ctx)
                }
                "divisors" -> {
                    val nRaw = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val n = abs(kotlin.math.round(nRaw).toLong())
                    if (n == 0L || n.toDouble() > 1e10) return MathValue.ListVal(emptyList())
                    val small = mutableListOf<Double>()
                    val large = mutableListOf<Double>()
                    var i = 1L
                    while (i * i <= n) {
                        if ((i and 255L) == 0L) ctx.tick()
                        if (n % i == 0L) {
                            small.add(i.toDouble())
                            if (i * i != n) large.add((n / i).toDouble())
                        }
                        i++
                    }
                    small.addAll(large.reversed())
                    return MathValue.ListVal(small)
                }
                "prime" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble()?.toInt() ?: 1
                    if (n !in 1..10_000) return MathValue.NumberVal(Double.NaN)
                    return MathValue.NumberVal(nthPrime(n, ctx).toDouble())
                }
                "primepi" -> {
                    val x = floor(evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0).toInt()
                    if (x < 2) return MathValue.NumberVal(0.0)
                    if (x > 100_000) error("PrimePi limit exceeded (<= 100000)")
                    return MathValue.NumberVal(countPrimesUpTo(x).toDouble())
                }
                "powermod" -> {
                    val a = evaluatedArgs.getOrNull(0)?.asDouble()?.toLong() ?: 0L
                    val b = evaluatedArgs.getOrNull(1)?.asDouble()?.toLong() ?: 0L
                    val m = evaluatedArgs.getOrNull(2)?.asDouble()?.toLong() ?: 1L
                    if (m <= 0L) return MathValue.NumberVal(Double.NaN)
                    val res = BigInteger.valueOf(a).modPow(BigInteger.valueOf(b), BigInteger.valueOf(m))
                    return MathValue.NumberVal(res.toDouble())
                }
                "integerdigits" -> {
                    var n = abs(evaluatedArgs.getOrNull(0)?.asDouble()?.toLong() ?: 0L)
                    val base = (evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 10).coerceIn(2, 36)
                    if (n == 0L) return MathValue.ListVal(listOf(0.0))
                    val digits = mutableListOf<Double>()
                    while (n > 0L) {
                        digits.add((n % base).toDouble())
                        n /= base
                    }
                    return MathValue.ListVal(digits.reversed())
                }
                "fromdigits" -> {
                    val digits = evaluatedArgs.getOrNull(0)?.asList() ?: emptyList()
                    val base = evaluatedArgs.getOrNull(1)?.asDouble() ?: 10.0
                    var acc = 0.0
                    for (d in digits) {
                        acc = acc * base + d
                    }
                    return MathValue.NumberVal(acc)
                }
                "factorial" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    val k = kotlin.math.round(n).toInt()
                    return MathValue.NumberVal(if (k < 0 || abs(n - k) > 1e-9) Double.NaN else MathStatsEngine.exactFactorial(k))
                }
                "binomial" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble()?.toInt() ?: 0
                    val k = evaluatedArgs.getOrNull(1)?.asDouble()?.toInt() ?: 0
                    return MathValue.NumberVal(MathStatsEngine.exactBinomial(n, k))
                }
                "primeq" -> {
                    val d = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    if (abs(d) > 1e14) return MathValue.BooleanVal(false)
                    val n = d.toLong()
                    return MathValue.BooleanVal(MathStatsEngine.isPrime(n))
                }
                "nextprime" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble()?.toLong() ?: 0L
                    if (n < 0 || n >= 1_000_000_000_000L) return MathValue.NumberVal(Double.NaN)
                    val np = MathStatsEngine.nextPrime(n)
                    return MathValue.NumberVal(if (np == -1L) Double.NaN else np.toDouble())
                }
                "fibonacci" -> {
                    val n = evaluatedArgs.getOrNull(0)?.asDouble()?.toInt() ?: 0
                    if (n < 0 || n > 92) return MathValue.NumberVal(Double.NaN)
                    val fib = MathStatsEngine.fibonacci(n)
                    return MathValue.NumberVal(if (fib < 0) Double.NaN else fib.toDouble())
                }
                "clip" -> {
                    val x = evaluatedArgs.getOrNull(0)?.asDouble() ?: 0.0
                    var min = 0.0
                    var max = 1.0
                    val second = evaluatedArgs.getOrNull(1)
                    if (second is MathValue.ListVal && second.elements.size >= 2) {
                        min = second.elements[0]
                        max = second.elements[1]
                    } else {
                        min = second?.asDouble() ?: 0.0
                        max = evaluatedArgs.getOrNull(2)?.asDouble() ?: 1.0
                    }
                    return MathValue.NumberVal(x.coerceIn(min, max))
                }
                "plus" -> {
                    if (evaluatedArgs.size == 1) {
                        return when (val v0 = evaluatedArgs[0]) {
                            is MathValue.ListVal -> MathValue.NumberVal(v0.elements.sum())
                            is MathValue.MixedListVal -> {
                                if (v0.items.isEmpty()) MathValue.NumberVal(0.0)
                                else {
                                    var acc = v0.items[0]
                                    for (i in 1 until v0.items.size) acc = applyBinaryOpValue('+', acc, v0.items[i])
                                    acc
                                }
                            }
                            else -> v0
                        }
                    }
                    var acc = evaluatedArgs.firstOrNull() ?: return MathValue.NumberVal(0.0)
                    for (i in 1 until evaluatedArgs.size) acc = applyBinaryOpValue('+', acc, evaluatedArgs[i])
                    return acc
                }
                "subtract" -> {
                    val l = evaluatedArgs.getOrNull(0) ?: MathValue.NumberVal(0.0)
                    val r = evaluatedArgs.getOrNull(1) ?: MathValue.NumberVal(0.0)
                    return applyBinaryOpValue('-', l, r)
                }
                "times" -> {
                    if (evaluatedArgs.size == 1) {
                        return when (val v0 = evaluatedArgs[0]) {
                            is MathValue.ListVal -> {
                                var prod = 1.0
                                v0.elements.forEach { prod *= it }
                                MathValue.NumberVal(prod)
                            }
                            is MathValue.MixedListVal -> {
                                if (v0.items.isEmpty()) MathValue.NumberVal(1.0)
                                else {
                                    var acc = v0.items[0]
                                    for (i in 1 until v0.items.size) acc = applyBinaryOpValue('*', acc, v0.items[i])
                                    acc
                                }
                            }
                            else -> v0
                        }
                    }
                    var acc = evaluatedArgs.firstOrNull() ?: return MathValue.NumberVal(1.0)
                    for (i in 1 until evaluatedArgs.size) acc = applyBinaryOpValue('*', acc, evaluatedArgs[i])
                    return acc
                }
                "divide" -> {
                    val l = evaluatedArgs.getOrNull(0) ?: MathValue.NumberVal(0.0)
                    val r = evaluatedArgs.getOrNull(1) ?: MathValue.NumberVal(1.0)
                    return applyBinaryOpValue('/', l, r)
                }
                "power" -> {
                    val l = evaluatedArgs.getOrNull(0) ?: MathValue.NumberVal(0.0)
                    val r = evaluatedArgs.getOrNull(1) ?: MathValue.NumberVal(1.0)
                    return applyBinaryOpValue('^', l, r)
                }
            }

            error("Unknown function call: $funcName")
        }
    }
}

internal fun MathValue.asValueList(): List<MathValue> = when (this) {
    is MathValue.ListVal -> elements.map { MathValue.NumberVal(it) }
    is MathValue.MixedListVal -> items
    else -> emptyList()
}

internal fun wrapSmartList(evaluated: List<MathValue>): MathValue {
    if (evaluated.isEmpty()) return MathValue.ListVal(emptyList())
    if (evaluated.all { it is MathValue.NumberVal }) {
        return MathValue.ListVal(evaluated.map { (it as MathValue.NumberVal).value })
    }
    return MathValue.MixedListVal(evaluated)
}

internal fun cleanZero(v: Double): Double = if (v.isFinite() && abs(v) < 1e-12) 0.0 else v

internal fun valuesEqual(a: MathValue, b: MathValue): Boolean {
    if (a is MathValue.NumberVal && b is MathValue.NumberVal) {
        return abs(a.value - b.value) < 1e-9
    }
    if (a is MathValue.StringVal && b is MathValue.StringVal) {
        return a.text == b.text
    }
    if (a is MathValue.BooleanVal && b is MathValue.BooleanVal) {
        return a.value == b.value
    }
    val la = a.asValueList()
    val lb = b.asValueList()
    if ((a is MathValue.ListVal || a is MathValue.MixedListVal) &&
        (b is MathValue.ListVal || b is MathValue.MixedListVal)
    ) {
        return la.size == lb.size && la.indices.all { valuesEqual(la[it], lb[it]) }
    }
    return a == b
}

internal fun compareMathValues(op: String, l: MathValue, r: MathValue): Boolean {
    val lNum = l.asDouble()
    val rNum = r.asDouble()
    return when (op) {
        "==", "===" -> if (!lNum.isNaN() && !rNum.isNaN()) abs(lNum - rNum) < 1e-9 else valuesEqual(l, r)
        "!=", "=!=" -> if (!lNum.isNaN() && !rNum.isNaN()) abs(lNum - rNum) >= 1e-9 else !valuesEqual(l, r)
        "<" -> lNum < rNum
        "<=" -> lNum <= rNum
        ">" -> lNum > rNum
        ">=" -> lNum >= rNum
        else -> false
    }
}

internal fun applyUnaryNeg(v: MathValue): MathValue = when (v) {
    is MathValue.NumberVal -> MathValue.NumberVal(-v.value)
    is MathValue.ListVal -> MathValue.ListVal(v.elements.map { -it })
    is MathValue.MixedListVal -> wrapSmartList(v.items.map { applyUnaryNeg(it) })
    else -> error("Cannot negate $v")
}

internal fun applyUnaryFactorial(v: MathValue): MathValue = when (v) {
    is MathValue.NumberVal -> {
        val n = kotlin.math.round(v.value).toInt()
        if (n < 0 || abs(v.value - n) > 1e-9) MathValue.NumberVal(Double.NaN)
        else MathValue.NumberVal(MathStatsEngine.exactFactorial(n))
    }
    is MathValue.ListVal -> MathValue.ListVal(v.elements.map {
        val n = kotlin.math.round(it).toInt()
        if (n < 0 || abs(it - n) > 1e-9) Double.NaN else MathStatsEngine.exactFactorial(n)
    })
    is MathValue.MixedListVal -> wrapSmartList(v.items.map { applyUnaryFactorial(it) })
    else -> error("Cannot compute factorial of $v")
}

internal fun applyBinaryOpValue(op: Char, l: MathValue, r: MathValue): MathValue {
    fun scalarOp(a: Double, b: Double): Double = when (op) {
        '+' -> a + b
        '-' -> a - b
        '*' -> a * b
        '/' -> a / b
        '%' -> a % b
        '^' -> a.pow(b)
        else -> error("Unknown operator $op")
    }
    return when {
        l is MathValue.NumberVal && r is MathValue.NumberVal -> MathValue.NumberVal(scalarOp(l.value, r.value))
        l is MathValue.NumberVal && r is MathValue.ListVal -> MathValue.ListVal(r.elements.map { scalarOp(l.value, it) })
        l is MathValue.ListVal && r is MathValue.NumberVal -> MathValue.ListVal(l.elements.map { scalarOp(it, r.value) })
        l is MathValue.ListVal && r is MathValue.ListVal -> {
            val size = minOf(l.elements.size, r.elements.size)
            MathValue.ListVal((0 until size).map { scalarOp(l.elements[it], r.elements[it]) })
        }
        l is MathValue.MixedListVal && r is MathValue.MixedListVal -> {
            val size = minOf(l.items.size, r.items.size)
            wrapSmartList((0 until size).map { applyBinaryOpValue(op, l.items[it], r.items[it]) })
        }
        l is MathValue.MixedListVal -> wrapSmartList(l.items.map { applyBinaryOpValue(op, it, r) })
        r is MathValue.MixedListVal -> wrapSmartList(r.items.map { applyBinaryOpValue(op, l, it) })
        else -> error("Cannot apply binary operator $op to $l and $r")
    }
}

private fun flattenValue(value: MathValue, maxDepth: Int?): MathValue {
    val out = mutableListOf<MathValue>()
    fun walk(v: MathValue, depth: Int) {
        when (v) {
            is MathValue.ListVal -> if (maxDepth != null && depth >= maxDepth) out.add(v) else v.elements.forEach { out.add(MathValue.NumberVal(it)) }
            is MathValue.MixedListVal -> if (maxDepth != null && depth >= maxDepth) out.add(v) else v.items.forEach { walk(it, depth + 1) }
            else -> out.add(v)
        }
    }
    walk(value, 0)
    return wrapSmartList(out)
}

private fun resolveSpanIndices(len: Int, startRaw: Int?, endRaw: Int?, step: Int): List<Int> {
    if (len <= 0 || step == 0) return emptyList()
    fun norm(idx: Int): Int = if (idx > 0) idx - 1 else if (idx < 0) len + idx else -1
    val sIdx = if (startRaw == null) {
        if (step > 0) 0 else len - 1
    } else norm(startRaw).coerceIn(0, len - 1)
    val eIdx = if (endRaw == null) {
        if (step > 0) len - 1 else 0
    } else norm(endRaw).coerceIn(0, len - 1)

    val out = mutableListOf<Int>()
    if (step > 0) {
        var i = sIdx
        while (i <= eIdx && out.size < 5000) {
            out.add(i)
            i += step
        }
    } else {
        var i = sIdx
        while (i >= eIdx && out.size < 5000) {
            out.add(i)
            i += step
        }
    }
    return out
}

internal fun resolvePart(
    target: MathValue,
    specs: List<MathNode>,
    specIdx: Int,
    ctx: ExecutionContext
): MathValue {
    if (specIdx >= specs.size) return target
    val items = target.asValueList()
    if (items.isEmpty()) return MathValue.NumberVal(Double.NaN)
    val spec = specs[specIdx]

    if (spec is MathNode.SpanNode) {
        fun evalBound(n: MathNode?): Int? {
            if (n == null) return null
            if (n is MathNode.IdentNode && n.name.equals("all", ignoreCase = true)) return null
            val d = n.eval(ctx).asDouble()
            return if (d.isNaN()) null else d.toInt()
        }
        val s = evalBound(spec.start)
        val e = evalBound(spec.end)
        val step = evalBound(spec.step) ?: 1
        val positions = resolveSpanIndices(items.size, s, e, step)
        return wrapSmartList(positions.map { p -> resolvePart(items[p], specs, specIdx + 1, ctx) })
    }

    val specVal = spec.eval(ctx)
    if (specVal is MathValue.ListVal) {
        val chosen = specVal.elements.map { raw ->
            val i = raw.toInt()
            val pos = if (i > 0) i - 1 else if (i < 0) items.size + i else -1
            if (pos in items.indices) resolvePart(items[pos], specs, specIdx + 1, ctx) else MathValue.NumberVal(Double.NaN)
        }
        return wrapSmartList(chosen)
    }

    val idxVal = specVal.asDouble().toInt()
    val pos = if (idxVal > 0) idxVal - 1 else if (idxVal < 0) items.size + idxVal else -1
    if (pos !in items.indices) return MathValue.NumberVal(Double.NaN)
    return resolvePart(items[pos], specs, specIdx + 1, ctx)
}

private fun isCallableNode(node: MathNode, ctx: ExecutionContext): Boolean = when (node) {
    is MathNode.FunctionNode, is MathNode.ComposeNode -> true
    is MathNode.IdentNode -> {
        val lower = node.name.lowercase(Locale.ROOT)
        ctx.env[lower] is MathValue.FunctionVal || lower in setOf(
            "evenq", "oddq", "integerq", "primeq", "positive", "negative", "zeroq"
        )
    }
    else -> false
}

private fun iterateSpec(specNode: MathNode, ctx: ExecutionContext, onStep: () -> Unit) {
    if (specNode is MathNode.ListNode) {
        if (specNode.items.isEmpty()) return
        if (specNode.items.size == 1) {
            val n = specNode.items[0].eval(ctx).asDouble().toInt().coerceIn(0, 5000)
            for (i in 0 until n) {
                ctx.tick()
                try {
                    onStep()
                } catch (_: BreakException) {
                    break
                } catch (_: ContinueException) {
                    continue
                }
            }
            return
        }
        val varNode = specNode.items[0] as? MathNode.IdentNode
        val varName = varNode?.name?.lowercase(Locale.ROOT) ?: "i"
        val saved = ctx.env[varName]
        try {
            if (specNode.items.size == 2) {
                val boundVal = specNode.items[1].eval(ctx)
                if (boundVal is MathValue.ListVal || boundVal is MathValue.MixedListVal) {
                    var count = 0
                    for (item in boundVal.asValueList()) {
                        if (count >= 5000) break
                        ctx.tick()
                        ctx.env[varName] = item
                        try {
                            onStep()
                        } catch (_: BreakException) {
                            break
                        } catch (_: ContinueException) {
                            // next
                        }
                        count++
                    }
                    return
                }
                val max = boundVal.asDouble()
                var curr = 1.0
                var count = 0
                while (curr <= max + 1e-9 && count < 5000) {
                    ctx.tick()
                    ctx.env[varName] = MathValue.NumberVal(curr)
                    try {
                        onStep()
                    } catch (_: BreakException) {
                        break
                    } catch (_: ContinueException) {
                        // next
                    }
                    curr += 1.0
                    count++
                }
                return
            }

            val min = specNode.items[1].eval(ctx).asDouble()
            val max = specNode.items[2].eval(ctx).asDouble()
            val step = if (specNode.items.size >= 4) specNode.items[3].eval(ctx).asDouble() else 1.0
            var curr = min
            var count = 0
            if (step > 0.0) {
                while (curr <= max + 1e-9 && count < 5000) {
                    ctx.tick()
                    ctx.env[varName] = MathValue.NumberVal(curr)
                    try {
                        onStep()
                    } catch (_: BreakException) {
                        break
                    } catch (_: ContinueException) {
                        // next
                    }
                    curr += step
                    count++
                }
            } else if (step < 0.0) {
                while (curr >= max - 1e-9 && count < 5000) {
                    ctx.tick()
                    ctx.env[varName] = MathValue.NumberVal(curr)
                    try {
                        onStep()
                    } catch (_: BreakException) {
                        break
                    } catch (_: ContinueException) {
                        // next
                    }
                    curr += step
                    count++
                }
            }
        } finally {
            if (saved != null) ctx.env[varName] = saved else ctx.env.remove(varName)
        }
    } else {
        val n = specNode.eval(ctx).asDouble().toInt().coerceIn(0, 5000)
        for (i in 0 until n) {
            ctx.tick()
            try {
                onStep()
            } catch (_: BreakException) {
                break
            } catch (_: ContinueException) {
                continue
            }
        }
    }
}

private fun iterateMulti(
    specs: List<MathNode>,
    idx: Int,
    ctx: ExecutionContext,
    leaf: () -> Unit
) {
    if (idx >= specs.size) {
        leaf()
        return
    }
    iterateSpec(specs[idx], ctx) {
        iterateMulti(specs, idx + 1, ctx, leaf)
    }
}

private fun evalTableRecursive(
    exprNode: MathNode,
    specs: List<MathNode>,
    idx: Int,
    ctx: ExecutionContext
): MathValue {
    if (idx >= specs.size) return exprNode.eval(ctx)
    val out = mutableListOf<MathValue>()
    iterateSpec(specs[idx], ctx) {
        out.add(evalTableRecursive(exprNode, specs, idx + 1, ctx))
    }
    return wrapSmartList(out)
}

private fun evalNIntegrate(exprNode: MathNode, specNode: MathNode, ctx: ExecutionContext): MathValue {
    val listSpec = specNode as? MathNode.ListNode ?: error("NIntegrate expects {x, a, b}")
    if (listSpec.items.size < 3) error("NIntegrate expects {x, a, b}")
    val varIdent = listSpec.items[0] as? MathNode.IdentNode ?: error("NIntegrate expects variable symbol")
    val varName = varIdent.name.lowercase(Locale.ROOT)
    val aRaw = listSpec.items[1].eval(ctx).asDouble()
    val bRaw = listSpec.items[2].eval(ctx).asDouble()
    if (!aRaw.isFinite() || !bRaw.isFinite()) error("NIntegrate requires finite bounds")
    if (aRaw == bRaw) return MathValue.NumberVal(0.0)

    val sign = if (bRaw < aRaw) -1.0 else 1.0
    val lo = minOf(aRaw, bRaw)
    val hi = maxOf(aRaw, bRaw)
    val saved = ctx.env[varName]

    fun f(x: Double): Double {
        ctx.tick()
        ctx.env[varName] = MathValue.NumberVal(x)
        return exprNode.eval(ctx).asDouble()
    }

    fun simpson(a: Double, b: Double, fa: Double, fm: Double, fb: Double): Double =
        (b - a) / 6.0 * (fa + 4.0 * fm + fb)

    fun adaptive(a: Double, b: Double, fa: Double, fm: Double, fb: Double, sWhole: Double, tol: Double, depth: Int): Double {
        val m = 0.5 * (a + b)
        val lm = 0.5 * (a + m)
        val rm = 0.5 * (m + b)
        val flm = f(lm)
        val frm = f(rm)
        val sLeft = simpson(a, m, fa, flm, fm)
        val sRight = simpson(m, b, fm, frm, fb)
        val sBoth = sLeft + sRight
        val err = abs(sBoth - sWhole)
        return if (depth <= 0 || (err <= 15.0 * tol && err.isFinite())) {
            sBoth + (sBoth - sWhole) / 15.0
        } else {
            adaptive(a, m, fa, flm, fm, sLeft, tol * 0.5, depth - 1) +
                adaptive(m, b, fm, frm, fb, sRight, tol * 0.5, depth - 1)
        }
    }

    return try {
        val panels = 8
        val h = (hi - lo) / panels
        var total = 0.0
        for (i in 0 until panels) {
            val a = lo + i * h
            val b = a + h
            val fa = f(a)
            val fm = f(0.5 * (a + b))
            val fb = f(b)
            total += adaptive(a, b, fa, fm, fb, simpson(a, b, fa, fm, fb), 1e-8 / panels, 10)
        }
        MathValue.NumberVal(cleanZero(sign * total))
    } finally {
        if (saved != null) ctx.env[varName] = saved else ctx.env.remove(varName)
    }
}

private fun evalNumericalDerivative(
    exprNode: MathNode,
    varNode: MathNode,
    pointNode: MathNode,
    ctx: ExecutionContext
): MathValue {
    var order = 1
    val varIdent = when (varNode) {
        is MathNode.IdentNode -> varNode
        is MathNode.ListNode -> {
            order = varNode.items.getOrNull(1)?.eval(ctx)?.asDouble()?.toInt() ?: 1
            varNode.items.getOrNull(0) as? MathNode.IdentNode ?: error("Expected variable in ND")
        }
        else -> error("Expected variable in ND")
    }
    val varName = varIdent.name.lowercase(Locale.ROOT)
    val x0 = pointNode.eval(ctx).asDouble()
    if (!x0.isFinite()) return MathValue.NumberVal(Double.NaN)
    val saved = ctx.env[varName]

    fun f(x: Double): Double {
        ctx.tick()
        ctx.env[varName] = MathValue.NumberVal(x)
        return exprNode.eval(ctx).asDouble()
    }

    return try {
        val res = if (order >= 2) {
            val h = maxOf(1e-4, 1e-4 * abs(x0))
            (-f(x0 + 2.0 * h) + 16.0 * f(x0 + h) - 30.0 * f(x0) + 16.0 * f(x0 - h) - f(x0 - 2.0 * h)) / (12.0 * h * h)
        } else {
            val h = maxOf(1e-5, 1e-5 * abs(x0))
            (-f(x0 + 2.0 * h) + 8.0 * f(x0 + h) - 8.0 * f(x0 - h) + f(x0 - 2.0 * h)) / (12.0 * h)
        }
        MathValue.NumberVal(cleanZero(res))
    } finally {
        if (saved != null) ctx.env[varName] = saved else ctx.env.remove(varName)
    }
}

private fun evalFindRoot(exprOrEq: MathNode, specNode: MathNode, ctx: ExecutionContext): MathValue {
    val listSpec = specNode as? MathNode.ListNode ?: error("FindRoot expects {x, x0}")
    val varIdent = listSpec.items.getOrNull(0) as? MathNode.IdentNode ?: error("FindRoot expects variable")
    val rawName = varIdent.name
    val varName = rawName.lowercase(Locale.ROOT)
    val saved = ctx.env[varName]

    fun f(x: Double): Double {
        ctx.tick()
        ctx.env[varName] = MathValue.NumberVal(x)
        return if (exprOrEq is MathNode.ComparisonNode && exprOrEq.op in setOf("==", "===")) {
            exprOrEq.left.eval(ctx).asDouble() - exprOrEq.right.eval(ctx).asDouble()
        } else {
            exprOrEq.eval(ctx).asDouble()
        }
    }

    return try {
        val root: Double = if (listSpec.items.size >= 3) {
            val a = listSpec.items[1].eval(ctx).asDouble()
            val b = listSpec.items[2].eval(ctx).asDouble()
            var lo = minOf(a, b)
            var hi = maxOf(a, b)
            var flo = f(lo)
            var fhi = f(hi)
            if (flo.isFinite() && fhi.isFinite() && flo * fhi > 0.0) {
                val nSub = 32
                val dx = (hi - lo) / nSub
                for (i in 0 until nSub) {
                    val x1 = lo + i * dx
                    val x2 = x1 + dx
                    val f1 = f(x1)
                    val f2 = f(x2)
                    if (f1.isFinite() && f2.isFinite() && f1 * f2 <= 0.0) {
                        lo = x1; hi = x2; flo = f1; fhi = f2
                        break
                    }
                }
            }
            if (flo.isFinite() && fhi.isFinite() && flo * fhi <= 0.0) {
                var best = lo
                for (iter in 0 until 80) {
                    val mid = 0.5 * (lo + hi)
                    val fm = f(mid)
                    best = mid
                    if (!fm.isFinite() || abs(fm) < 1e-11 || abs(hi - lo) < 1e-11) break
                    if (flo * fm <= 0.0) {
                        hi = mid; fhi = fm
                    } else {
                        lo = mid; flo = fm
                    }
                }
                best
            } else {
                dampedNewtonRoot(::f, 0.5 * (lo + hi))
            }
        } else {
            val x0 = listSpec.items.getOrNull(1)?.eval(ctx)?.asDouble() ?: 0.0
            dampedNewtonRoot(::f, x0)
        }
        MathValue.RuleVal(rawName, MathValue.NumberVal(cleanZero(root)))
    } finally {
        if (saved != null) ctx.env[varName] = saved else ctx.env.remove(varName)
    }
}

private fun dampedNewtonRoot(f: (Double) -> Double, start: Double): Double {
    var x = start
    var fx = f(x)
    for (iter in 0 until 80) {
        if (!fx.isFinite() || abs(fx) < 1e-11) break
        val h = maxOf(1e-6, 1e-6 * abs(x))
        val df = (f(x + h) - f(x - h)) / (2.0 * h)
        if (!df.isFinite() || abs(df) < 1e-13) break
        var step = -fx / df
        val maxStep = maxOf(10.0, 2.0 * abs(x) + 1.0)
        if (abs(step) > maxStep) step = sign(step) * maxStep
        var alpha = 1.0
        var accepted = false
        for (ls in 0 until 8) {
            val nx = x + alpha * step
            val nfx = f(nx)
            if (nfx.isFinite() && abs(nfx) < abs(fx)) {
                x = nx
                fx = nfx
                accepted = true
                break
            }
            alpha *= 0.5
        }
        if (!accepted) {
            x += step
            fx = f(x)
        }
        if (abs(alpha * step) < 1e-11) break
    }
    return x
}

private fun evalNOpt(exprNode: MathNode, specNode: MathNode, maximize: Boolean, ctx: ExecutionContext): MathValue {
    val (rawName, lo, hi) = when (specNode) {
        is MathNode.IdentNode -> Triple(specNode.name, -50.0, 50.0)
        is MathNode.ListNode -> {
            val varIdent = specNode.items.getOrNull(0) as? MathNode.IdentNode ?: error("Expected variable")
            val (a, b) = if (specNode.items.size >= 3) {
                val v1 = specNode.items[1].eval(ctx).asDouble()
                val v2 = specNode.items[2].eval(ctx).asDouble()
                minOf(v1, v2) to maxOf(v1, v2)
            } else {
                val center = specNode.items.getOrNull(1)?.eval(ctx)?.asDouble() ?: 0.0
                (center - 10.0) to (center + 10.0)
            }
            Triple(varIdent.name, a, b)
        }
        else -> error("Expected variable x or {x, a, b}")
    }
    val varName = rawName.lowercase(Locale.ROOT)
    if (!lo.isFinite() || !hi.isFinite()) error("Expected finite search domain")
    val saved = ctx.env[varName]

    fun obj(x: Double): Double {
        ctx.tick()
        ctx.env[varName] = MathValue.NumberVal(x)
        val v = exprNode.eval(ctx).asDouble()
        return if (maximize) -v else v
    }

    return try {
        val samples = 128
        val step = (hi - lo) / samples
        var bestIdx = 0
        var bestVal = Double.POSITIVE_INFINITY
        for (i in 0..samples) {
            val x = lo + i * step
            val v = obj(x)
            if (v.isFinite() && v < bestVal) {
                bestVal = v
                bestIdx = i
            }
        }
        var a = lo + maxOf(0, bestIdx - 1) * step
        var b = lo + minOf(samples, bestIdx + 1) * step
        val gr = (sqrt(5.0) - 1.0) / 2.0
        var c = b - gr * (b - a)
        var d = a + gr * (b - a)
        var fc = obj(c)
        var fd = obj(d)
        for (iter in 0 until 55) {
            if (abs(b - a) < 1e-10) break
            if (fc < fd) {
                b = d; d = c; fd = fc
                c = b - gr * (b - a); fc = obj(c)
            } else {
                a = c; c = d; fc = fd
                d = a + gr * (b - a); fd = obj(d)
            }
        }
        val optX = cleanZero(0.5 * (a + b))
        val optVal = cleanZero(if (maximize) -obj(optX) else obj(optX))
        MathValue.MixedListVal(
            listOf(
                MathValue.NumberVal(optVal),
                MathValue.MixedListVal(listOf(MathValue.RuleVal(rawName, MathValue.NumberVal(optX))))
            )
        )
    } finally {
        if (saved != null) ctx.env[varName] = saved else ctx.env.remove(varName)
    }
}

private fun formatSignificantDigits(v: Double, prec: Int): String {
    val bd = BigDecimal(v, MathContext(prec))
    val clean = if (bd.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else bd.stripTrailingZeros()
    return if (v != 0.0 && (abs(v) >= 1e9 || abs(v) < 1e-5)) {
        clean.toString()
    } else {
        clean.toPlainString()
    }
}

private fun factorIntegerValue(nRaw: Long, ctx: ExecutionContext): MathValue {
    if (nRaw == 0L) return MathValue.MixedListVal(listOf(MathValue.ListVal(listOf(0.0, 1.0))))
    val factors = mutableListOf<MathValue>()
    var n = nRaw
    if (n < 0L) {
        factors.add(MathValue.ListVal(listOf(-1.0, 1.0)))
        n = -n
    }
    if (n == 1L) {
        if (factors.isEmpty()) factors.add(MathValue.ListVal(listOf(1.0, 1.0)))
        return MathValue.MixedListVal(factors)
    }
    var count2 = 0
    while (n % 2L == 0L) {
        count2++
        n /= 2L
    }
    if (count2 > 0) factors.add(MathValue.ListVal(listOf(2.0, count2.toDouble())))
    var p = 3L
    while (p * p <= n) {
        if ((p and 255L) == 1L) ctx.tick()
        if (n % p == 0L) {
            var e = 0
            while (n % p == 0L) {
                e++
                n /= p
            }
            factors.add(MathValue.ListVal(listOf(p.toDouble(), e.toDouble())))
        }
        p += 2L
    }
    if (n > 1L) factors.add(MathValue.ListVal(listOf(n.toDouble(), 1.0)))
    return MathValue.MixedListVal(factors)
}

private fun nthPrime(n: Int, ctx: ExecutionContext): Long {
    if (n == 1) return 2L
    var count = 1
    var candidate = 1L
    while (count < n) {
        candidate += 2L
        if ((candidate and 127L) == 1L) ctx.tick()
        if (MathStatsEngine.isPrime(candidate)) count++
    }
    return candidate
}

private fun countPrimesUpTo(limit: Int): Int {
    val composite = BooleanArray(limit + 1)
    var count = 0
    for (p in 2..limit) {
        if (!composite[p]) {
            count++
            if (p.toLong() * p <= limit) {
                var m = p * p
                while (m <= limit) {
                    composite[m] = true
                    m += p
                }
            }
        }
    }
    return count
}

private fun matrixDet(m: List<List<Double>>): Double {
    val n = m.size
    val a = Array(n) { r -> m[r].toDoubleArray() }
    var det = 1.0
    for (col in 0 until n) {
        var pivot = col
        for (r in col + 1 until n) {
            if (abs(a[r][col]) > abs(a[pivot][col])) pivot = r
        }
        if (abs(a[pivot][col]) < 1e-12) return 0.0
        if (pivot != col) {
            val tmp = a[col]; a[col] = a[pivot]; a[pivot] = tmp
            det = -det
        }
        val pv = a[col][col]
        det *= pv
        for (r in col + 1 until n) {
            val factor = a[r][col] / pv
            for (c in col until n) a[r][c] -= factor * a[col][c]
        }
    }
    return cleanZero(det)
}

private fun matrixInverse(m: List<List<Double>>): List<List<Double>> {
    val n = m.size
    val aug = Array(n) { r ->
        DoubleArray(2 * n) { c -> if (c < n) m[r][c] else if (c - n == r) 1.0 else 0.0 }
    }
    for (col in 0 until n) {
        var pivot = col
        for (r in col + 1 until n) {
            if (abs(aug[r][col]) > abs(aug[pivot][col])) pivot = r
        }
        if (abs(aug[pivot][col]) < 1e-12) error("Singular matrix")
        if (pivot != col) {
            val tmp = aug[col]; aug[col] = aug[pivot]; aug[pivot] = tmp
        }
        val pv = aug[col][col]
        for (c in 0 until 2 * n) aug[col][c] /= pv
        for (r in 0 until n) {
            if (r == col) continue
            val factor = aug[r][col]
            if (factor != 0.0) {
                for (c in 0 until 2 * n) aug[r][c] -= factor * aug[col][c]
            }
        }
    }
    return (0 until n).map { r ->
        (n until 2 * n).map { c -> cleanZero(aug[r][c]) }
    }
}

internal fun invokeFunction(fnNode: MathNode, args: List<MathValue>, ctx: ExecutionContext): MathValue {
    ctx.tick()
    when (fnNode) {
        is MathNode.IdentNode -> {
            val fnLower = fnNode.name.lowercase(Locale.ROOT)
            val custom = ctx.env[fnLower]
            if (custom is MathValue.FunctionVal) {
                return invokePureFunction(custom, args, ctx)
            }
            return MathNode.CallNode(fnNode.name, args.map { MathNode.ConstantValueNode(it) }).eval(ctx)
        }
        is MathNode.FunctionNode -> {
            return invokePureFunction(MathValue.FunctionVal(fnNode.body, fnNode.paramNames), args, ctx)
        }
        is MathNode.ComposeNode -> {
            val first = if (fnNode.rightFirst) fnNode.right else fnNode.left
            val second = if (fnNode.rightFirst) fnNode.left else fnNode.right
            val mid = invokeFunction(first, args, ctx)
            return invokeFunction(second, listOf(mid), ctx)
        }
        else -> {
            val evaluated = fnNode.eval(ctx)
            if (evaluated is MathValue.FunctionVal) {
                return invokePureFunction(evaluated, args, ctx)
            }
            if (evaluated is MathValue.SymbolVal) {
                return MathNode.CallNode(evaluated.name, args.map { MathNode.ConstantValueNode(it) }).eval(ctx)
            }
            error("Cannot invoke non-function $evaluated")
        }
    }
}

internal fun invokePureFunction(fn: MathValue.FunctionVal, args: List<MathValue>, ctx: ExecutionContext): MathValue {
    return ctx.withCallFrame {
        val savedSlots = mutableMapOf<String, MathValue?>()
        val savedParams = mutableMapOf<String, MathValue?>()

        try {
            if (fn.paramNames.isNotEmpty()) {
                for (i in fn.paramNames.indices) {
                    val p = fn.paramNames[i].lowercase(Locale.ROOT)
                    savedParams[p] = ctx.env[p]
                    ctx.env[p] = args.getOrElse(i) { MathValue.NullVal }
                }
            }
            for (i in args.indices) {
                val key = "#${i + 1}"
                savedSlots[key] = ctx.env[key]
                ctx.env[key] = args[i]
            }
            savedSlots["#"] = ctx.env["#"]
            if (args.isNotEmpty()) ctx.env["#"] = args[0]

            try {
                fn.body.eval(ctx)
            } catch (r: ReturnException) {
                r.value
            }
        } finally {
            for ((k, v) in savedSlots) {
                if (v != null) ctx.env[k] = v else ctx.env.remove(k)
            }
            for ((k, v) in savedParams) {
                if (v != null) ctx.env[k] = v else ctx.env.remove(k)
            }
        }
    }
}

internal class WolframParser(private val tokens: List<Token>) {
    private var idx = 0
    private var depth = 0
    private val MAX_DEPTH = 40

    private inline fun <T> withDepth(block: () -> T): T {
        depth++
        if (depth > MAX_DEPTH) error("Recursion depth limit exceeded ($MAX_DEPTH)")
        try {
            return block()
        } finally {
            depth--
        }
    }

    private fun peek(): Token = if (idx < tokens.size) tokens[idx] else Token(TokenType.EOF, "")
    private fun peekNext(): Token = if (idx + 1 < tokens.size) tokens[idx + 1] else Token(TokenType.EOF, "")

    private fun match(type: TokenType): Boolean {
        if (peek().type == type) {
            idx++
            return true
        }
        return false
    }

    private fun consume(type: TokenType): Token {
        val t = peek()
        if (t.type == type) {
            idx++
            return t
        }
        error("Expected $type but got ${t.type}")
    }

    fun parse(): MathNode? {
        if (peek().type == TokenType.EOF) return null
        val expr = parseCompound()
        if (peek().type != TokenType.EOF) return null
        return expr
    }

    // CompoundExpression: expr1 ; expr2 ; ...
    private fun parseCompound(): MathNode {
        val expressions = mutableListOf<MathNode>()
        expressions.add(parseAssignment())
        while (match(TokenType.SEMICOLON)) {
            if (peek().type in setOf(
                    TokenType.EOF, TokenType.RBRACKET, TokenType.RPAREN, TokenType.RBRACE, TokenType.COMMA
                )
            ) {
                expressions.add(MathNode.NullNode)
                break
            }
            expressions.add(parseAssignment())
        }
        return if (expressions.size == 1) expressions[0] else MathNode.CompoundNode(expressions)
    }

    // Assignment: x = val, x := expr, f[x_] := body, x += val, ...
    private fun parseAssignment(): MathNode {
        val node = parseRule()
        val t = peek().type
        if (node is MathNode.IdentNode && t in setOf(
                TokenType.EQUAL, TokenType.SET_DELAYED,
                TokenType.PLUS_EQUAL, TokenType.MINUS_EQUAL,
                TokenType.STAR_EQUAL, TokenType.SLASH_EQUAL
            )
        ) {
            consume(t)
            val rhs = withDepth { parseAssignment() }
            if (t == TokenType.SET_DELAYED) {
                return MathNode.AssignDelayedNode(node.name, rhs)
            }
            val op = when (t) {
                TokenType.PLUS_EQUAL -> '+'
                TokenType.MINUS_EQUAL -> '-'
                TokenType.STAR_EQUAL -> '*'
                TokenType.SLASH_EQUAL -> '/'
                else -> null
            }
            return MathNode.AssignNode(node.name, rhs, op)
        }
        if (node is MathNode.CallNode && t in setOf(TokenType.EQUAL, TokenType.SET_DELAYED) &&
            node.args.all { it is MathNode.IdentNode }
        ) {
            consume(t)
            val paramNames = node.args.map { (it as MathNode.IdentNode).name.trimEnd('_') }
            val rhs = withDepth { parseAssignment() }
            return MathNode.AssignFunctionNode(node.funcName, paramNames, rhs)
        }
        return node
    }

    // Rule: lhs -> rhs
    private fun parseRule(): MathNode {
        var node = parseReplace()
        if (match(TokenType.ARROW)) {
            val rhs = withDepth { parseRule() }
            node = MathNode.RuleNode(node, rhs)
        }
        return node
    }

    // ReplaceAll: expr /. rule, ReplaceRepeated: expr //. rule
    private fun parseReplace(): MathNode {
        var node = parsePostfix()
        while (peek().type in setOf(TokenType.REPLACE_ALL, TokenType.REPLACE_REPEATED)) {
            val op = consume(peek().type)
            val rule = parseRule()
            node = MathNode.ReplaceAllNode(node, rule, repeated = (op.type == TokenType.REPLACE_REPEATED))
        }
        return node
    }

    // Postfix: expr // f
    private fun parsePostfix(): MathNode {
        var node = parseMapApply()
        while (match(TokenType.POSTFIX)) {
            val func = parseMapApply()
            node = MathNode.PostfixNode(node, func)
        }
        return node
    }

    // Map (/@), Apply (@@), Apply at level 1 (@@@)
    private fun parseMapApply(): MathNode {
        var node = parseFunction()
        while (peek().type in setOf(TokenType.MAP, TokenType.APPLY, TokenType.APPLY_LEVEL)) {
            val op = consume(peek().type)
            val right = parseFunction()
            node = when (op.type) {
                TokenType.MAP -> MathNode.MapNode(node, right)
                TokenType.APPLY -> MathNode.ApplyNode(node, right, level1 = false)
                TokenType.APPLY_LEVEL -> MathNode.ApplyNode(node, right, level1 = true)
                else -> node
            }
        }
        return node
    }

    // Pure function: expr &
    private fun parseFunction(): MathNode {
        var node = parseLogicalOr()
        if (match(TokenType.AMP)) {
            node = MathNode.FunctionNode(node)
        }
        return node
    }

    // Logical OR: expr || expr
    private fun parseLogicalOr(): MathNode {
        var node = parseLogicalAnd()
        while (match(TokenType.OR)) {
            val right = parseLogicalAnd()
            node = MathNode.LogicalOpNode("||", node, right)
        }
        return node
    }

    // Logical AND: expr && expr
    private fun parseLogicalAnd(): MathNode {
        var node = parseComparison()
        while (match(TokenType.AND)) {
            val right = parseComparison()
            node = MathNode.LogicalOpNode("&&", node, right)
        }
        return node
    }

    // Comparisons (supports chained comparisons like 1 < x < 5): ==, !=, <, <=, >, >=
    private fun parseComparison(): MathNode {
        val first = parseStringJoin()
        val operands = mutableListOf(first)
        val ops = mutableListOf<String>()
        while (peek().type in setOf(
                TokenType.EQUAL_EQUAL, TokenType.NOT_EQUAL,
                TokenType.LESS, TokenType.LESS_EQUAL,
                TokenType.GREATER, TokenType.GREATER_EQUAL
            )
        ) {
            ops.add(consume(peek().type).text)
            operands.add(parseStringJoin())
        }
        return when (ops.size) {
            0 -> first
            1 -> MathNode.ComparisonNode(ops[0], operands[0], operands[1])
            else -> MathNode.ChainedComparisonNode(operands, ops)
        }
    }

    // StringJoin: s1 <> s2
    private fun parseStringJoin(): MathNode {
        var node = parseExpression()
        while (match(TokenType.STRING_JOIN)) {
            val right = parseExpression()
            node = MathNode.StringJoinNode(node, right)
        }
        return node
    }

    // Add / Subtract: + , -
    private fun parseExpression(): MathNode {
        var node = parseTerm()
        while (peek().type in setOf(TokenType.PLUS, TokenType.MINUS)) {
            val op = consume(peek().type).text[0]
            val right = parseTerm()
            node = MathNode.BinaryOpNode(op, node, right)
        }
        return node
    }

    // Multiply / Divide / Modulo: * , / , % , and implicit multiplication
    private fun parseTerm(): MathNode {
        var node = parsePrefix()
        while (peek().type in setOf(TokenType.STAR, TokenType.SLASH) ||
            (peek().type == TokenType.PERCENT && isBinaryPercent()) ||
            canStartImplicitMult()
        ) {
            val op = if (peek().type in setOf(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
                consume(peek().type).text[0]
            } else {
                '*'
            }
            val right = parsePrefix()
            node = MathNode.BinaryOpNode(op, node, right)
        }
        return node
    }

    private fun isBinaryPercent(): Boolean {
        if (idx + 1 >= tokens.size) return false
        val nextType = tokens[idx + 1].type
        return nextType in setOf(
            TokenType.NUMBER, TokenType.IDENT, TokenType.LPAREN, TokenType.LBRACKET, TokenType.LBRACE, TokenType.SLOT
        )
    }

    private fun canStartImplicitMult(): Boolean {
        val t = peek().type
        return t in setOf(TokenType.IDENT, TokenType.LPAREN, TokenType.LBRACE, TokenType.SLOT)
    }

    // Prefix application: f @ x
    private fun parsePrefix(): MathNode {
        var node = parseComposition()
        if (match(TokenType.AT)) {
            val right = withDepth { parsePrefix() }
            node = MathNode.PrefixNode(node, right)
        }
        return node
    }

    // Composition: f /* g (left-to-right), f @* g (right-to-left)
    private fun parseComposition(): MathNode {
        var node = parseUnary()
        while (peek().type in setOf(TokenType.RIGHT_COMPOSITION, TokenType.COMPOSITION)) {
            val op = consume(peek().type)
            val right = parseUnary()
            node = MathNode.ComposeNode(node, right, rightFirst = (op.type == TokenType.COMPOSITION))
        }
        return node
    }

    // Unary prefix: +, -, !, ++x, --x
    private fun parseUnary(): MathNode {
        if (match(TokenType.PLUS)) return parseUnary()
        if (match(TokenType.MINUS)) return MathNode.UnaryOpNode('-', parseUnary())
        if (match(TokenType.EXCLAMATION)) return MathNode.UnaryOpNode('~', parseUnary())
        if (match(TokenType.PLUS_PLUS)) {
            val t = consume(TokenType.IDENT)
            return MathNode.PrefixIncDecNode("++", t.text)
        }
        if (match(TokenType.MINUS_MINUS)) {
            val t = consume(TokenType.IDENT)
            return MathNode.PrefixIncDecNode("--", t.text)
        }
        return parsePower()
    }

    // Power: ^ (right-associative)
    private fun parsePower(): MathNode {
        var node = parsePostfixUnary()
        if (match(TokenType.CARET)) {
            val right = withDepth { parseUnary() }
            node = MathNode.BinaryOpNode('^', node, right)
        }
        return node
    }

    // Postfix unary: x!, x++, x--, list[[parts]], expr[args]
    private fun parsePostfixUnary(): MathNode {
        var node = parsePrimary()
        while (true) {
            when {
                match(TokenType.EXCLAMATION) -> {
                    node = MathNode.UnaryOpNode('!', node)
                }
                match(TokenType.PLUS_PLUS) -> {
                    if (node is MathNode.IdentNode) node = MathNode.PostfixIncDecNode("++", node.name)
                }
                match(TokenType.MINUS_MINUS) -> {
                    if (node is MathNode.IdentNode) node = MathNode.PostfixIncDecNode("--", node.name)
                }
                peek().type == TokenType.LBRACKET && peekNext().type == TokenType.LBRACKET -> {
                    consume(TokenType.LBRACKET)
                    consume(TokenType.LBRACKET)
                    val specs = parsePartSpecList()
                    node = MathNode.PartNode(node, specs)
                }
                peek().type == TokenType.LBRACKET -> {
                    consume(TokenType.LBRACKET)
                    val args = parseArgList(TokenType.RBRACKET)
                    node = when (node) {
                        is MathNode.IdentNode -> MathNode.CallNode(node.name, args)
                        else -> MathNode.ApplyCallNode(node, args)
                    }
                }
                else -> break
            }
        }
        return node
    }

    private fun parsePartSpecList(): List<MathNode> = withDepth {
        val list = mutableListOf<MathNode>()
        while (true) {
            list.add(parsePartSpec())
            if (match(TokenType.COMMA)) continue
            consume(TokenType.RBRACKET)
            consume(TokenType.RBRACKET)
            break
        }
        list
    }

    private fun parsePartSpec(): MathNode {
        var start: MathNode? = null
        if (peek().type != TokenType.SPAN) {
            start = parseCompound()
            if (!match(TokenType.SPAN)) return start
        } else {
            consume(TokenType.SPAN)
        }
        var end: MathNode? = null
        if (peek().type !in setOf(TokenType.SPAN, TokenType.COMMA, TokenType.RBRACKET)) {
            end = parseCompound()
        }
        var step: MathNode? = null
        if (match(TokenType.SPAN)) {
            if (peek().type !in setOf(TokenType.COMMA, TokenType.RBRACKET)) {
                step = parseCompound()
            }
        }
        return MathNode.SpanNode(start, end, step)
    }

    private fun parsePrimary(): MathNode {
        val t = peek()
        return when (t.type) {
            TokenType.NUMBER -> {
                consume(TokenType.NUMBER)
                MathNode.NumberNode(t.value)
            }
            TokenType.STRING -> {
                consume(TokenType.STRING)
                MathNode.StringNode(t.text)
            }
            TokenType.PERCENT -> {
                consume(TokenType.PERCENT)
                MathNode.OutNode
            }
            TokenType.SLOT -> {
                consume(TokenType.SLOT)
                MathNode.SlotNode(t.intVal)
            }
            TokenType.IDENT -> {
                val name = consume(TokenType.IDENT).text
                if (match(TokenType.LBRACKET)) {
                    val args = parseArgList(TokenType.RBRACKET)
                    MathNode.CallNode(name, args)
                } else if (peek().type == TokenType.LPAREN &&
                    name.lowercase(Locale.ROOT) in MathStatsEngine.PAREN_CALL_FUNCS
                ) {
                    consume(TokenType.LPAREN)
                    val args = parseArgList(TokenType.RPAREN)
                    MathNode.CallNode(name, args)
                } else {
                    val lower = name.lowercase(Locale.ROOT)
                    if (lower == "true") MathNode.BooleanNode(true)
                    else if (lower == "false") MathNode.BooleanNode(false)
                    else if (lower == "null") MathNode.NullNode
                    else if (lower == "%" || lower == "out") MathNode.OutNode
                    else MathNode.IdentNode(name)
                }
            }
            TokenType.LPAREN -> {
                consume(TokenType.LPAREN)
                val expr = withDepth { parseCompound() }
                consume(TokenType.RPAREN)
                expr
            }
            TokenType.LBRACE -> {
                consume(TokenType.LBRACE)
                val items = withDepth { parseArgList(TokenType.RBRACE) }
                MathNode.ListNode(items)
            }
            else -> error("Unexpected token: ${t.text}")
        }
    }

    private fun parseArgList(closingType: TokenType): List<MathNode> = withDepth {
        val list = mutableListOf<MathNode>()
        if (match(closingType)) return@withDepth list
        while (true) {
            list.add(parseCompound())
            if (match(TokenType.COMMA)) continue
            consume(closingType)
            break
        }
        list
    }
}
