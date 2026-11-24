package com.pallas.launcher.domain.search

data class QrMatrix(
    val size: Int,
    val modules: BooleanArray
) {
    fun get(x: Int, y: Int): Boolean {
        if (x !in 0 until size || y !in 0 until size) return false
        return modules[y * size + x]
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QrMatrix) return false
        return size == other.size && modules.contentEquals(other.modules)
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + modules.contentHashCode()
        return result
    }
}

/**
 * Pure-Kotlin ISO/IEC 18004 QR Code encoder supporting Versions 1–14 (21×21 up to 73×73)
 * in Byte mode (Error Correction Level M up to 213 UTF-8 bytes, stepping to Level L up to 458 bytes).
 */
object QrCodeGenerator {

    private enum class EcLevel(val formatBits: Int) {
        M(0), // 0b00
        L(1)  // 0b01
    }

    private data class VersionSpec(
        val version: Int,
        val ecLevel: EcLevel,
        val totalDataBytes: Int,
        val ecPerBlock: Int,
        val g1Blocks: Int,
        val g1DataBytes: Int,
        val g2Blocks: Int,
        val g2DataBytes: Int
    ) {
        val totalBlocks: Int get() = g1Blocks + g2Blocks
        val countBits: Int get() = if (version <= 9) 8 else 16
        val maxBytePayload: Int get() = (totalDataBytes * 8 - 4 - countBits) / 8
    }

    private val LEVEL_M_SPECS = listOf(
        VersionSpec(1, EcLevel.M, 16, 10, 1, 16, 0, 0),
        VersionSpec(2, EcLevel.M, 28, 16, 1, 28, 0, 0),
        VersionSpec(3, EcLevel.M, 44, 26, 1, 44, 0, 0),
        VersionSpec(4, EcLevel.M, 64, 18, 2, 32, 0, 0),
        VersionSpec(5, EcLevel.M, 86, 24, 2, 43, 0, 0),
        VersionSpec(6, EcLevel.M, 108, 16, 4, 27, 0, 0),
        VersionSpec(7, EcLevel.M, 124, 18, 4, 31, 0, 0),
        VersionSpec(8, EcLevel.M, 154, 22, 2, 38, 2, 39),
        VersionSpec(9, EcLevel.M, 182, 22, 3, 36, 2, 37),
        VersionSpec(10, EcLevel.M, 216, 26, 4, 43, 1, 44),
        VersionSpec(11, EcLevel.M, 254, 30, 1, 50, 4, 51),
        VersionSpec(12, EcLevel.M, 290, 22, 6, 36, 2, 37),
        VersionSpec(13, EcLevel.M, 334, 22, 8, 37, 1, 38),
        VersionSpec(14, EcLevel.M, 365, 24, 4, 40, 5, 41)
    )

    private val LEVEL_L_SPECS = listOf(
        VersionSpec(1, EcLevel.L, 19, 7, 1, 19, 0, 0),
        VersionSpec(2, EcLevel.L, 34, 10, 1, 34, 0, 0),
        VersionSpec(3, EcLevel.L, 55, 15, 1, 55, 0, 0),
        VersionSpec(4, EcLevel.L, 80, 20, 1, 80, 0, 0),
        VersionSpec(5, EcLevel.L, 108, 26, 1, 108, 0, 0),
        VersionSpec(6, EcLevel.L, 136, 18, 2, 68, 0, 0),
        VersionSpec(7, EcLevel.L, 156, 20, 2, 78, 0, 0),
        VersionSpec(8, EcLevel.L, 194, 24, 2, 97, 0, 0),
        VersionSpec(9, EcLevel.L, 232, 30, 2, 116, 0, 0),
        VersionSpec(10, EcLevel.L, 274, 18, 2, 68, 2, 69),
        VersionSpec(11, EcLevel.L, 324, 20, 4, 81, 0, 0),
        VersionSpec(12, EcLevel.L, 370, 24, 2, 92, 2, 93),
        VersionSpec(13, EcLevel.L, 428, 26, 4, 107, 0, 0),
        VersionSpec(14, EcLevel.L, 461, 30, 3, 115, 1, 116)
    )

    private val ALIGNMENT_POSITIONS = mapOf(
        1 to intArrayOf(),
        2 to intArrayOf(6, 18),
        3 to intArrayOf(6, 22),
        4 to intArrayOf(6, 26),
        5 to intArrayOf(6, 30),
        6 to intArrayOf(6, 34),
        7 to intArrayOf(6, 22, 38),
        8 to intArrayOf(6, 24, 42),
        9 to intArrayOf(6, 26, 46),
        10 to intArrayOf(6, 28, 50),
        11 to intArrayOf(6, 30, 54),
        12 to intArrayOf(6, 32, 58),
        13 to intArrayOf(6, 34, 62),
        14 to intArrayOf(6, 26, 46, 66)
    )

    // GF(256) exp & log tables for polynomial 0x11D
    private val gfExp = IntArray(512)
    private val gfLog = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            gfExp[i] = x
            gfLog[x] = i
            x = x shl 1
            if (x >= 256) x = x xor 0x11D
        }
        for (i in 255 until 512) {
            gfExp[i] = gfExp[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return gfExp[gfLog[a] + gfLog[b]]
    }

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var poly = IntArray(degree)
        poly[degree - 1] = 1
        var root = 1
        for (i in 0 until degree) {
            for (j in poly.indices) {
                poly[j] = gfMul(poly[j], root)
                if (j + 1 < poly.size) {
                    poly[j] = poly[j] xor poly[j + 1]
                }
            }
            root = gfMul(root, 0x02)
        }
        return poly
    }

    private fun rsComputeRemainder(data: IntArray, divisor: IntArray): IntArray {
        val ecLen = divisor.size
        val rem = IntArray(ecLen)
        for (b in data) {
            val factor = b xor rem[0]
            System.arraycopy(rem, 1, rem, 0, ecLen - 1)
            rem[ecLen - 1] = 0
            for (i in 0 until ecLen) {
                rem[i] = rem[i] xor gfMul(divisor[i], factor)
            }
        }
        return rem
    }

    fun encode(text: String): QrMatrix? {
        if (text.isEmpty()) return null
        val payload = text.toByteArray(Charsets.UTF_8)
        if (payload.isEmpty()) return null

        // Prefer Error Correction M; fall back to Level L for long payloads (>213..458 bytes)
        val spec = LEVEL_M_SPECS.firstOrNull { payload.size <= it.maxBytePayload }
            ?: LEVEL_L_SPECS.firstOrNull { payload.size <= it.maxBytePayload }
            ?: return null

        val dataCodewords = encodeDataCodewords(payload, spec)
        val interleaved = addErrorCorrectionAndInterleave(dataCodewords, spec)
        return buildBestMaskedMatrix(interleaved, spec)
    }

    private fun encodeDataCodewords(payload: ByteArray, spec: VersionSpec): IntArray {
        val totalBits = spec.totalDataBytes * 8
        val bits = BitBuffer(totalBits)

        // 1. Byte mode indicator (0100) + character count indicator
        bits.appendBits(0b0100, 4)
        bits.appendBits(payload.size, spec.countBits)

        // 2. Payload bytes
        for (b in payload) {
            bits.appendBits(b.toInt() and 0xFF, 8)
        }

        // 3. Terminator (up to 4 zeros)
        val rem = totalBits - bits.bitLength
        if (rem > 0) {
            bits.appendBits(0, minOf(4, rem))
        }

        // 4. Pad to byte boundary
        while (bits.bitLength % 8 != 0) {
            bits.appendBits(0, 1)
        }

        // 5. Pad codewords alternating 0xEC and 0x11
        val out = IntArray(spec.totalDataBytes)
        val byteLen = bits.bitLength / 8
        for (i in 0 until byteLen) {
            out[i] = bits.bytes[i].toInt() and 0xFF
        }
        var padIndex = byteLen
        var flip = false
        while (padIndex < spec.totalDataBytes) {
            out[padIndex++] = if (flip) 0x11 else 0xEC
            flip = !flip
        }
        return out
    }

    private fun addErrorCorrectionAndInterleave(data: IntArray, spec: VersionSpec): IntArray {
        val blocksData = ArrayList<IntArray>(spec.totalBlocks)
        val blocksEc = ArrayList<IntArray>(spec.totalBlocks)
        val divisor = rsGeneratorPoly(spec.ecPerBlock)

        var offset = 0
        for (i in 0 until spec.totalBlocks) {
            val blockLen = if (i < spec.g1Blocks) spec.g1DataBytes else spec.g2DataBytes
            val block = data.copyOfRange(offset, offset + blockLen)
            offset += blockLen
            blocksData.add(block)
            blocksEc.add(rsComputeRemainder(block, divisor))
        }

        val maxDataLen = maxOf(spec.g1DataBytes, spec.g2DataBytes)
        val totalCodewords = spec.totalDataBytes + spec.totalBlocks * spec.ecPerBlock
        val result = IntArray(totalCodewords)
        var outIdx = 0

        for (i in 0 until maxDataLen) {
            for (b in blocksData) {
                if (i < b.size) result[outIdx++] = b[i]
            }
        }
        for (i in 0 until spec.ecPerBlock) {
            for (ec in blocksEc) {
                result[outIdx++] = ec[i]
            }
        }
        return result
    }

    private fun buildBestMaskedMatrix(codewords: IntArray, spec: VersionSpec): QrMatrix {
        val size = 17 + 4 * spec.version
        val modules = BooleanArray(size * size)
        val isFunction = BooleanArray(size * size)

        fun setFunc(r: Int, c: Int, v: Boolean) {
            if (r in 0 until size && c in 0 until size) {
                val idx = r * size + c
                modules[idx] = v
                isFunction[idx] = true
            }
        }

        // Draw finder patterns + 1-module separators
        fun drawFinder(top: Int, left: Int) {
            for (dy in -1..7) {
                for (dx in -1..7) {
                    val r = top + dy
                    val c = left + dx
                    if (r !in 0 until size || c !in 0 until size) continue
                    val dist = maxOf(kotlin.math.abs(dy - 3), kotlin.math.abs(dx - 3))
                    val dark = dy in 0..6 && dx in 0..6 && (dist == 0 || dist == 1 || dist == 3)
                    setFunc(r, c, dark)
                }
            }
        }

        drawFinder(0, 0)
        drawFinder(0, size - 7)
        drawFinder(size - 7, 0)

        // Timing patterns
        for (i in 8 until size - 8) {
            val dark = (i and 1) == 0
            setFunc(6, i, dark)
            setFunc(i, 6, dark)
        }

        // Alignment patterns (v >= 2)
        val alignPts = ALIGNMENT_POSITIONS[spec.version] ?: intArrayOf()
        for (cy in alignPts) {
            for (cx in alignPts) {
                if (isFunction[cy * size + cx] && !(cy == 6 || cx == 6)) continue
                val inTopLeft = cy == alignPts.first() && cx == alignPts.first()
                val inTopRight = cy == alignPts.first() && cx == alignPts.last()
                val inBottomLeft = cy == alignPts.last() && cx == alignPts.first()
                if (inTopLeft || inTopRight || inBottomLeft) continue
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val dist = maxOf(kotlin.math.abs(dy), kotlin.math.abs(dx))
                        setFunc(cy + dy, cx + dx, dist == 0 || dist == 2)
                    }
                }
            }
        }

        // Dark module
        setFunc(size - 8, 8, true)

        // Reserve format info bits (filled per mask later)
        for (i in 0..8) {
            if (i != 6) {
                setFunc(8, i, false)
                setFunc(i, 8, false)
            }
        }
        for (i in size - 8 until size) {
            setFunc(8, i, false)
        }
        for (i in size - 7 until size) {
            setFunc(i, 8, false)
        }

        // Version info for v >= 7
        if (spec.version >= 7) {
            val versionBits = computeVersionInfo(spec.version)
            for (i in 0 until 18) {
                val bit = ((versionBits ushr i) and 1) != 0
                val a = size - 11 + (i % 3)
                val b = i / 3
                setFunc(a, b, bit)
                setFunc(b, a, bit)
            }
        }

        // Zigzag data placement
        var bitIndex = 0
        val totalDataBits = codewords.size * 8
        var up = true
        var rightCol = size - 1
        while (rightCol >= 1) {
            if (rightCol == 6) {
                rightCol--
                continue
            }
            for (step in 0 until size) {
                val r = if (up) size - 1 - step else step
                for (dx in 0..1) {
                    val c = rightCol - dx
                    val idx = r * size + c
                    if (!isFunction[idx]) {
                        val bit = if (bitIndex < totalDataBits) {
                            val cw = codewords[bitIndex ushr 3]
                            ((cw ushr (7 - (bitIndex and 7))) and 1) != 0
                        } else {
                            false
                        }
                        modules[idx] = bit
                        bitIndex++
                    }
                }
            }
            up = !up
            rightCol -= 2
        }

        // Evaluate all 8 masks and pick lowest penalty score
        var bestPenalty = Int.MAX_VALUE
        var bestModules: BooleanArray? = null

        for (mask in 0 until 8) {
            val candidate = modules.copyOf()
            for (r in 0 until size) {
                for (c in 0 until size) {
                    val idx = r * size + c
                    if (!isFunction[idx] && maskBit(mask, r, c)) {
                        candidate[idx] = !candidate[idx]
                    }
                }
            }
            applyFormatInfo(candidate, size, spec.ecLevel.formatBits, mask)
            val penalty = computePenaltyScore(candidate, size)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestModules = candidate
            }
        }

        return QrMatrix(size = size, modules = bestModules ?: modules)
    }

    private fun maskBit(mask: Int, r: Int, c: Int): Boolean = when (mask) {
        0 -> ((r + c) and 1) == 0
        1 -> (r and 1) == 0
        2 -> c % 3 == 0
        3 -> (r + c) % 3 == 0
        4 -> (((r ushr 1) + c / 3) and 1) == 0
        5 -> (r * c) % 2 + (r * c) % 3 == 0
        6 -> (((r * c) % 2 + (r * c) % 3) and 1) == 0
        else -> (((r + c) % 2 + (r * c) % 3) and 1) == 0
    }

    private fun applyFormatInfo(modules: BooleanArray, size: Int, ecBits: Int, mask: Int) {
        val data = (ecBits shl 3) or mask
        var rem = data
        repeat(10) {
            rem = (rem shl 1) xor (((rem ushr 9) and 1) * 0x537)
        }
        val bits = ((data shl 10) or rem) xor 0x5412

        fun bit(i: Int): Boolean = ((bits ushr i) and 1) != 0

        // Copy 1: around top-left finder
        for (i in 0..5) modules[8 * size + i] = bit(14 - i)
        modules[8 * size + 7] = bit(8)
        modules[8 * size + 8] = bit(7)
        modules[7 * size + 8] = bit(6)
        for (i in 0..5) modules[(5 - i) * size + 8] = bit(5 - i)

        // Copy 2: bottom-left & top-right edges
        for (i in 0..6) modules[(size - 1 - i) * size + 8] = bit(14 - i)
        for (i in 0..7) modules[8 * size + (size - 8 + i)] = bit(7 - i)
    }

    private fun computeVersionInfo(version: Int): Int {
        var rem = version
        repeat(12) {
            rem = (rem shl 1) xor (((rem ushr 11) and 1) * 0x1F25)
        }
        return (version shl 12) or rem
    }

    private fun computePenaltyScore(m: BooleanArray, size: Int): Int {
        var penalty = 0

        // Rule N1 & Rule N3 across rows
        for (r in 0 until size) {
            var runColor = m[r * size]
            var runLen = 1
            for (c in 1 until size) {
                val v = m[r * size + c]
                if (v == runColor) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runColor = v
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)

            for (c in 0..size - 11) {
                val base = r * size + c
                if (matchesFinderLikePattern(m, base, 1)) penalty += 40
            }
        }

        // Rule N1 & Rule N3 across columns
        for (c in 0 until size) {
            var runColor = m[c]
            var runLen = 1
            for (r in 1 until size) {
                val v = m[r * size + c]
                if (v == runColor) {
                    runLen++
                } else {
                    if (runLen >= 5) penalty += 3 + (runLen - 5)
                    runColor = v
                    runLen = 1
                }
            }
            if (runLen >= 5) penalty += 3 + (runLen - 5)

            for (r in 0..size - 11) {
                if (matchesFinderLikePattern(m, r * size + c, size)) penalty += 40
            }
        }

        // Rule N2: 2x2 blocks of same color
        for (r in 0 until size - 1) {
            for (c in 0 until size - 1) {
                val color = m[r * size + c]
                if (m[r * size + c + 1] == color &&
                    m[(r + 1) * size + c] == color &&
                    m[(r + 1) * size + c + 1] == color
                ) {
                    penalty += 3
                }
            }
        }

        // Rule N4: dark module ratio vs 50%
        var darkCount = 0
        for (b in m) if (b) darkCount++
        val total = size * size
        val percent = (darkCount * 100) / total
        val prev5 = (percent / 5) * 5
        val next5 = prev5 + 5
        val k = minOf(kotlin.math.abs(prev5 - 50) / 5, kotlin.math.abs(next5 - 50) / 5)
        penalty += k * 10

        return penalty
    }

    private fun matchesFinderLikePattern(m: BooleanArray, start: Int, stride: Int): Boolean {
        // 10111010000 or 00001011101
        val b0 = m[start]
        val b1 = m[start + stride]
        val b2 = m[start + 2 * stride]
        val b3 = m[start + 3 * stride]
        val b4 = m[start + 4 * stride]
        val b5 = m[start + 5 * stride]
        val b6 = m[start + 6 * stride]
        val b7 = m[start + 7 * stride]
        val b8 = m[start + 8 * stride]
        val b9 = m[start + 9 * stride]
        val b10 = m[start + 10 * stride]
        val patA = b0 && !b1 && b2 && b3 && b4 && !b5 && b6 && !b7 && !b8 && !b9 && !b10
        val patB = !b0 && !b1 && !b2 && !b3 && b4 && !b5 && b6 && b7 && b8 && !b9 && b10
        return patA || patB
    }

    private class BitBuffer(capacityBits: Int) {
        val bytes = ByteArray((capacityBits + 7) / 8)
        var bitLength = 0
            private set

        fun appendBits(value: Int, numBits: Int) {
            for (i in numBits - 1 downTo 0) {
                if (((value ushr i) and 1) != 0) {
                    val byteIdx = bitLength ushr 3
                    val bitMask = 1 shl (7 - (bitLength and 7))
                    bytes[byteIdx] = (bytes[byteIdx].toInt() or bitMask).toByte()
                }
                bitLength++
            }
        }
    }
}
