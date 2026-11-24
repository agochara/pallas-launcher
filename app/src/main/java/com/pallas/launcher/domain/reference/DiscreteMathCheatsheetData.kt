package com.pallas.launcher.domain.reference

import com.pallas.launcher.domain.math.CheatsheetFormula
import com.pallas.launcher.domain.math.CheatsheetSubsection
import com.pallas.launcher.domain.math.CheatsheetTopic

object DiscreteMathCheatsheetData {

    val DISCRETE_MATH_TOPIC: CheatsheetTopic = CheatsheetTopic(
        id = "discrete_math",
        title = "Discrete Math & Combinatorics",
        shortTitle = "Discrete",
        symbol = "∑",
        description = "Permutations, combinations, recurrence relations, modular arithmetic, and graph theory.",
        domain = CheatsheetDomain.MATHEMATICS,
        kind = CheatsheetKind.FORMULAS,
        subsections = listOf(
            CheatsheetSubsection(
                title = "Permutations & Combinations",
                shortTitle = "Combinatorics",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "k-Permutations & Combinations",
                        latex = """P(n,k) = \frac{n!}{(n-k)!}, \quad \binom{n}{k} = \frac{n!}{k!(n-k)!}"""
                    ),
                    CheatsheetFormula(
                        title = "Pascal & Hockey-Stick Identities",
                        latex = """\binom{n}{k} = \binom{n-1}{k-1} + \binom{n-1}{k}, \quad \sum_{i=k}^{n}\binom{i}{k} = \binom{n+1}{k+1}"""
                    ),
                    CheatsheetFormula(
                        title = "Vandermonde's Convolution",
                        latex = """\sum_{k=0}^{r} \binom{m}{k}\binom{n}{r-k} = \binom{m+n}{r}"""
                    ),
                    CheatsheetFormula(
                        title = "Stars and Bars (Multisets / Weak Compositions)",
                        latex = """\left(\!\binom{n}{k}\!\right) = \binom{n+k-1}{k}""",
                        note = "Number of non-negative integer solutions to x₁ + ... + xₙ = k; for positive xᵢ ≥ 1, use C(k − 1, n − 1)."
                    ),
                    CheatsheetFormula(
                        title = "Multinomial Theorem",
                        latex = """(x_1 + \cdots + x_m)^n = \sum_{k_1+\cdots+k_m=n} \frac{n!}{k_1!\cdots k_m!} \prod_{j=1}^{m} x_j^{k_j}"""
                    ),
                    CheatsheetFormula(
                        title = "Principle of Inclusion-Exclusion",
                        latex = """\left|\bigcup_{i=1}^{n} A_i\right| = \sum_{\emptyset \neq S \subseteq [n]} (-1)^{|S|-1} \left|\bigcap_{i \in S} A_i\right|"""
                    ),
                    CheatsheetFormula(
                        title = "Derangements (No Fixed Points)",
                        latex = """D_n = n! \sum_{k=0}^{n} \frac{(-1)^k}{k!} = \left\lfloor \frac{n!}{e} + \frac{1}{2} \right\rfloor \quad (n \ge 1)"""
                    ),
                    CheatsheetFormula(
                        title = "Catalan Numbers",
                        latex = """C_n = \frac{1}{n+1}\binom{2n}{n}, \quad C_{n+1} = \sum_{i=0}^{n} C_i C_{n-i}""",
                        note = "Counts valid n-pair parentheses, full binary trees with n+1 leaves, and monotonic Dyck paths."
                    ),
                    CheatsheetFormula(
                        title = "Stirling Numbers of the Second Kind",
                        latex = """\genfrac\{\}{0pt}{}{n}{k} = \frac{1}{k!} \sum_{j=0}^{k} (-1)^{k-j}\binom{k}{j} j^n""",
                        note = "Number of ways to partition an n-element set into k non-empty unlabeled subsets."
                    ),
                    CheatsheetFormula(
                        title = "Generalized Pigeonhole Principle",
                        latex = """N \text{ objects in } k \text{ boxes} \implies \text{some box has } \ge \left\lceil \frac{N}{k} \right\rceil"""
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Recurrence Relations & Generating Functions",
                shortTitle = "Recurrences",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Linear Homogeneous Characteristic Solution",
                        latex = """a_n = c_1 a_{n-1} + c_2 a_{n-2} \implies r^2 - c_1 r - c_2 = 0, \quad a_n = \alpha r_1^n + \beta r_2^n""",
                        note = "If root r₁ = r₂ has multiplicity m, multiply by polynomial factors 1, n, ..., n^(m−1)."
                    ),
                    CheatsheetFormula(
                        title = "Fibonacci Binet Closed Form",
                        latex = """F_n = \frac{\varphi^n - \psi^n}{\sqrt{5}}, \quad \varphi = \frac{1+\sqrt{5}}{2},\; \psi = \frac{1-\sqrt{5}}{2}"""
                    ),
                    CheatsheetFormula(
                        title = "Master Theorem (Divide & Conquer)",
                        latex = """T(n) = a\,T\!\left(\frac{n}{b}\right) + \Theta(n^d) \implies \begin{cases} \Theta(n^{\log_b a}) & d < \log_b a \\ \Theta(n^d \log n) & d = \log_b a \\ \Theta(n^d) & d > \log_b a \end{cases}""",
                        note = "Requires a ≥ 1, b > 1."
                    ),
                    CheatsheetFormula(
                        title = "Ordinary Generating Function Identities",
                        latex = """\sum_{n=0}^{\infty} x^n = \frac{1}{1-x}, \quad \sum_{n=0}^{\infty}\binom{n+k-1}{k-1} x^n = \frac{1}{(1-x)^k}"""
                    ),
                    CheatsheetFormula(
                        title = "Exponential Generating Function (Labeled)",
                        latex = """\hat{A}(x) = \sum_{n=0}^{\infty} a_n \frac{x^n}{n!}, \quad \hat{A}(x)\hat{B}(x) \leftrightarrow \sum_{k=0}^{n}\binom{n}{k} a_k b_{n-k}"""
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Modular Arithmetic & Number Theory",
                shortTitle = "Modular",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Bézout's Identity & Extended GCD",
                        latex = """ax + by = \gcd(a,b), \quad a^{-1} \bmod m \text{ exists} \iff \gcd(a,m) = 1"""
                    ),
                    CheatsheetFormula(
                        title = "Fermat's Little Theorem",
                        latex = """p \text{ prime},\; p \nmid a \implies a^{p-1} \equiv 1 \pmod p, \quad a^{-1} \equiv a^{p-2} \pmod p"""
                    ),
                    CheatsheetFormula(
                        title = "Euler's Totient Function & Theorem",
                        latex = """\varphi(n) = n \prod_{p \mid n} \left(1 - \frac{1}{p}\right), \quad \gcd(a,n)=1 \implies a^{\varphi(n)} \equiv 1 \pmod n"""
                    ),
                    CheatsheetFormula(
                        title = "Chinese Remainder Theorem (CRT)",
                        latex = """x \equiv a_i \pmod{m_i},\; \gcd(m_i,m_j)=1 \implies x \equiv \sum_{i} a_i M_i (M_i^{-1} \bmod m_i) \pmod{M}""",
                        note = "Where M = Π mᵢ and Mᵢ = M / mᵢ."
                    ),
                    CheatsheetFormula(
                        title = "Wilson's Theorem",
                        latex = """(p - 1)! \equiv -1 \pmod p \iff p \text{ is prime}"""
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Graph Theory",
                shortTitle = "Graphs",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Handshaking Lemma",
                        latex = """\sum_{v \in V} \deg(v) = 2|E|, \quad \text{\# odd-degree vertices is even}"""
                    ),
                    CheatsheetFormula(
                        title = "Trees & Cayley's Labeled Tree Formula",
                        latex = """|E| = |V| - 1 \text{ (acyclic connected)}, \quad \tau(K_n) = n^{n-2}""",
                        note = "Complete graph Kₙ has n^(n−2) distinct labeled spanning trees."
                    ),
                    CheatsheetFormula(
                        title = "Euler's Planar Formula & Edge Bounds",
                        latex = """V - E + F = 2, \quad E \le 3V - 6 \text{ (planar)},\; E \le 2V - 4 \text{ (triangle-free)}""",
                        note = "Applies to connected simple planar graphs with V ≥ 3."
                    ),
                    CheatsheetFormula(
                        title = "Eulerian & Dirac Hamiltonian Criteria",
                        latex = """\text{Euler circuit} \iff \forall v,\; 2 \mid \deg(v); \quad \delta(G) \ge \frac{n}{2}\;(n \ge 3) \implies \text{Hamiltonian}"""
                    ),
                    CheatsheetFormula(
                        title = "Hall's Marriage Theorem (Bipartite Matching)",
                        latex = """\text{Complete matching for } X \iff \forall S \subseteq X,\; |N(S)| \ge |S|"""
                    ),
                    CheatsheetFormula(
                        title = "Chromatic Bounds & Max-Flow Min-Cut",
                        latex = """\omega(G) \le \chi(G) \le \Delta(G) + 1, \quad \max_{\text{flow}} |f| = \min_{(S,T)} c(S,T)"""
                    )
                )
            )
        )
    )
}
