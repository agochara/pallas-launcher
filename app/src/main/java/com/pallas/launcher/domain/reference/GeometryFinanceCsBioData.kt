package com.pallas.launcher.domain.reference

import com.pallas.launcher.domain.math.CheatsheetFormula
import com.pallas.launcher.domain.math.CheatsheetSubsection
import com.pallas.launcher.domain.math.CheatsheetTopic

object GeometryFinanceCsBioData {

    val GEOMETRY_TRIG_TOPIC: CheatsheetTopic = CheatsheetTopic(
        id = "geometry_trig",
        title = "Geometry & Trigonometry",
        shortTitle = "Geometry & Trig",
        symbol = "△",
        description = "2D/3D mensuration, conic sections, analytic geometry, trigonometric identities, and spherical geometry.",
        domain = CheatsheetDomain.MATHEMATICS,
        kind = CheatsheetKind.FORMULAS,
        subsections = listOf(
            CheatsheetSubsection(
                title = "2D & 3D Mensuration",
                shortTitle = "Mensuration",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Heron's Formula",
                        latex = """A = \sqrt{s(s - a)(s - b)(s - c)}, \quad s = \frac{a + b + c}{2}"""
                    ),
                    CheatsheetFormula(
                        title = "Sector & Segment Area",
                        latex = """A_{\text{sector}} = \frac{1}{2}r^2\theta, \quad s = r\theta, \quad A_{\text{segment}} = \frac{1}{2}r^2(\theta - \sin\theta)""",
                        note = "Central angle θ in radians."
                    ),
                    CheatsheetFormula(
                        title = "Regular Polygon Area",
                        latex = """A = \frac{n s^2}{4\tan(\pi/n)} = \frac{1}{2}n R^2 \sin\left(\frac{2\pi}{n}\right) = \frac{1}{2} P a_{\text{ap}}""",
                        note = "Side length s, circumradius R, perimeter P = ns, apothem a_ap."
                    ),
                    CheatsheetFormula(
                        title = "Sphere Volume & Surface Area",
                        latex = """V = \frac{4}{3}\pi r^3, \quad S = 4\pi r^2, \quad V_{\text{cap}} = \frac{\pi h^2}{3}(3r - h)"""
                    ),
                    CheatsheetFormula(
                        title = "Cone & Frustum",
                        latex = """V_{\text{cone}} = \frac{1}{3}\pi r^2 h, \quad S_{\text{lat}} = \pi r \ell, \quad V_{\text{frustum}} = \frac{\pi h}{3}\left(R^2 + Rr + r^2\right)""",
                        note = "Slant height ℓ = √(r² + h²) for cone, or √((R − r)² + h²) for frustum."
                    ),
                    CheatsheetFormula(
                        title = "Cylinder & Torus",
                        latex = """V_{\text{cyl}} = \pi r^2 h, \quad S_{\text{cyl}} = 2\pi r(r + h), \quad V_{\text{torus}} = 2\pi^2 R r^2, \quad S_{\text{torus}} = 4\pi^2 R r""",
                        note = "For torus: major radius R (center to tube center), minor tube radius r (R > r)."
                    ),
                    CheatsheetFormula(
                        title = "Euler's Polyhedron Formula",
                        latex = """V - E + F = 2""",
                        note = "For any convex polyhedron (or connected planar graph): vertices V, edges E, faces F."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Conic Sections",
                shortTitle = "Conics",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Circle & Distance/Midpoint",
                        latex = """(x - h)^2 + (y - k)^2 = r^2, \quad d = \sqrt{(x_2 - x_1)^2 + (y_2 - y_1)^2}, \quad M = \left(\frac{x_1 + x_2}{2}, \frac{y_1 + y_2}{2}\right)"""
                    ),
                    CheatsheetFormula(
                        title = "Ellipse (Standard, Eccentricity, Foci)",
                        latex = """\frac{(x - h)^2}{a^2} + \frac{(y - k)^2}{b^2} = 1, \quad c^2 = a^2 - b^2, \quad e = \frac{c}{a} \in [0, 1), \quad A = \pi a b""",
                        note = "Horizontal major semi-axis a > b > 0; foci at (h ± c, k)."
                    ),
                    CheatsheetFormula(
                        title = "Hyperbola & Asymptotes",
                        latex = """\frac{(x - h)^2}{a^2} - \frac{(y - k)^2}{b^2} = 1, \quad c^2 = a^2 + b^2, \quad e = \frac{c}{a} > 1, \quad y - k = \pm\frac{b}{a}(x - h)"""
                    ),
                    CheatsheetFormula(
                        title = "Parabola (Focus & Directrix)",
                        latex = """(x - h)^2 = 4p(y - k), \quad \text{Focus: } (h, k + p), \quad \text{Directrix: } y = k - p""",
                        note = "Focal width (latus rectum) = 4|p|; eccentricity e = 1."
                    ),
                    CheatsheetFormula(
                        title = "Polar Conic Equation",
                        latex = """r(\theta) = \frac{e d}{1 + e\cos\theta}""",
                        note = "Focus at origin, directrix x = d. Circle e = 0, ellipse 0 < e < 1, parabola e = 1, hyperbola e > 1."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Analytic & Coordinate Geometry",
                shortTitle = "Analytic",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Point-to-Line & Point-to-Plane Distance",
                        latex = """d_{\text{line}} = \frac{|ax_0 + by_0 + c|}{\sqrt{a^2 + b^2}}, \quad d_{\text{plane}} = \frac{|ax_0 + by_0 + cz_0 + d|}{\sqrt{a^2 + b^2 + c^2}}"""
                    ),
                    CheatsheetFormula(
                        title = "Shoelace Polygon Area",
                        latex = """A = \frac{1}{2}\left|\sum_{i=1}^{n}(x_i y_{i+1} - x_{i+1} y_i)\right| \quad (x_{n+1}=x_1,\; y_{n+1}=y_1)"""
                    ),
                    CheatsheetFormula(
                        title = "Rotation of Axes",
                        latex = """\begin{pmatrix} x' \\ y' \end{pmatrix} = \begin{pmatrix} \cos\theta & -\sin\theta \\ \sin\theta & \cos\theta \end{pmatrix} \begin{pmatrix} x \\ y \end{pmatrix}""",
                        note = "Counterclockwise rotation of a point by angle θ about the origin."
                    ),
                    CheatsheetFormula(
                        title = "Vector Dot/Cross Projection & Angle",
                        latex = """\mathbf{u} \cdot \mathbf{v} = \|\mathbf{u}\|\|\mathbf{v}\|\cos\theta, \quad \|\mathbf{u} \times \mathbf{v}\| = \|\mathbf{u}\|\|\mathbf{v}\|\sin\theta, \quad \operatorname{proj}_{\mathbf{v}}\mathbf{u} = \frac{\mathbf{u} \cdot \mathbf{v}}{\|\mathbf{v}\|^2}\mathbf{v}"""
                    ),
                    CheatsheetFormula(
                        title = "3D Line/Plane & Tetrahedron Volume",
                        latex = """\mathbf{n} \cdot (\mathbf{r} - \mathbf{r}_0) = 0, \quad \mathbf{r}(t) = \mathbf{r}_0 + t\mathbf{d}, \quad V_{\text{tet}} = \frac{1}{6}\left|\mathbf{a} \cdot (\mathbf{b} \times \mathbf{c})\right|"""
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Trigonometric Identities & Laws",
                shortTitle = "Trig",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Pythagorean Identities",
                        latex = """\sin^2\theta + \cos^2\theta = 1, \quad 1 + \tan^2\theta = \sec^2\theta, \quad 1 + \cot^2\theta = \csc^2\theta"""
                    ),
                    CheatsheetFormula(
                        title = "Law of Sines & Extended Sine Law",
                        latex = """\frac{a}{\sin A} = \frac{b}{\sin B} = \frac{c}{\sin C} = 2R""",
                        note = "R is the circumradius of triangle ABC."
                    ),
                    CheatsheetFormula(
                        title = "Law of Cosines",
                        latex = """c^2 = a^2 + b^2 - 2ab\cos C"""
                    ),
                    CheatsheetFormula(
                        title = "Angle Sum & Difference",
                        latex = """\sin(\alpha \pm \beta) = \sin\alpha\cos\beta \pm \cos\alpha\sin\beta, \quad \cos(\alpha \pm \beta) = \cos\alpha\cos\beta \mp \sin\alpha\sin\beta"""
                    ),
                    CheatsheetFormula(
                        title = "Double-Angle & Half-Angle Formulas",
                        latex = """\sin 2\theta = 2\sin\theta\cos\theta, \quad \cos 2\theta = \cos^2\theta - \sin^2\theta, \quad \sin^2\frac{\theta}{2} = \frac{1 - \cos\theta}{2}, \quad \cos^2\frac{\theta}{2} = \frac{1 + \cos\theta}{2}"""
                    ),
                    CheatsheetFormula(
                        title = "Product-to-Sum & Sum-to-Product",
                        latex = """\sin\alpha\cos\beta = \frac{1}{2}\left(\sin(\alpha+\beta) + \sin(\alpha-\beta)\right), \quad \sin\alpha + \sin\beta = 2\sin\left(\frac{\alpha+\beta}{2}\right)\cos\left(\frac{\alpha-\beta}{2}\right)"""
                    ),
                    CheatsheetFormula(
                        title = "De Moivre's Theorem",
                        latex = """(\cos\theta + i\sin\theta)^n = \cos(n\theta) + i\sin(n\theta)"""
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Spherical Geometry",
                shortTitle = "Spherical",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Spherical Law of Cosines",
                        latex = """\cos c = \cos a \cos b + \sin a \sin b \cos C""",
                        note = "For a spherical triangle with angular sides a, b, c (radians) and opposite angle C."
                    ),
                    CheatsheetFormula(
                        title = "Spherical Law of Sines",
                        latex = """\frac{\sin A}{\sin a} = \frac{\sin B}{\sin b} = \frac{\sin C}{\sin c}"""
                    ),
                    CheatsheetFormula(
                        title = "Haversine Great-Circle Distance",
                        latex = """d = 2R \arcsin\sqrt{\sin^2\left(\frac{\Delta\phi}{2}\right) + \cos\phi_1\cos\phi_2\sin^2\left(\frac{\Delta\lambda}{2}\right)}""",
                        note = "Latitude φ, longitude λ (radians), sphere radius R ≈ 6,371 km for Earth."
                    ),
                    CheatsheetFormula(
                        title = "Girard's Spherical Excess Area",
                        latex = """\mathcal{A} = R^2 E = R^2(A + B + C - \pi)""",
                        note = "Spherical excess E = A + B + C − π > 0 on a sphere of radius R."
                    )
                )
            )
        )
    )

    val FINANCE_RISK_TOPIC: CheatsheetTopic = CheatsheetTopic(
        id = "finance_economics",
        title = "Finance & Risk",
        shortTitle = "Finance & Risk",
        symbol = "f*",
        description = "Time value of money, Kelly criterion, ergodicity & compounding drag, and model-free tail bounds (Thorp / Taleb).",
        domain = CheatsheetDomain.ENGINEERING_QUANT,
        kind = CheatsheetKind.FORMULAS,
        subsections = listOf(
            CheatsheetSubsection(
                title = "Time Value of Money & Compounding",
                shortTitle = "TVM",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Discrete & Continuous Compounding",
                        latex = """FV = PV\left(1 + \frac{r}{m}\right)^{mt}, \quad FV_{\text{cont}} = PV\,e^{rt}"""
                    ),
                    CheatsheetFormula(
                        title = "Effective Annual Rate (EAR)",
                        latex = """\mathrm{EAR} = \left(1 + \frac{r}{m}\right)^m - 1, \quad \mathrm{EAR}_{\text{cont}} = e^r - 1"""
                    ),
                    CheatsheetFormula(
                        title = "Present Value of Annuity",
                        latex = """PV = C \cdot \frac{1 - (1 + r)^{-n}}{r}, \quad FV = C \cdot \frac{(1 + r)^n - 1}{r}""",
                        note = "Ordinary annuity paying cash flow C at the end of each of n periods at rate r."
                    ),
                    CheatsheetFormula(
                        title = "Perpetuity & Growing Perpetuity",
                        latex = """PV_{\text{perp}} = \frac{C}{r}, \quad PV_{\text{grow}} = \frac{C_1}{r - g} \quad (r > g)"""
                    ),
                    CheatsheetFormula(
                        title = "Loan Amortization Payment",
                        latex = """P = L \cdot \frac{r(1 + r)^n}{(1 + r)^n - 1}""",
                        note = "Periodic payment P for principal L, per-period rate r, and n periods."
                    ),
                    CheatsheetFormula(
                        title = "Doubling Time & Rule of 72",
                        latex = """t_2 = \frac{\ln 2}{\ln(1 + r)} \approx \frac{0.72}{r}""",
                        note = "Continuous doubling time is ln(2)/r ≈ 0.693/r; 72 is convenient for mental division."
                    ),
                    CheatsheetFormula(
                        title = "Fisher Real Interest Equation",
                        latex = """1 + i_{\text{nom}} = (1 + r_{\text{real}})(1 + \pi) \Rightarrow r_{\text{real}} \approx i_{\text{nom}} - \pi"""
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Kelly Criterion & Optimal Growth",
                shortTitle = "Kelly",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Binary Kelly Fraction",
                        latex = """f^* = p - \frac{q}{b} = \frac{bp - q}{b}""",
                        note = "Win probability p, loss probability q = 1 − p, net decimal odds b to 1."
                    ),
                    CheatsheetFormula(
                        title = "General Expected Log-Growth",
                        latex = """g(f) = \mathbb{E}\left[\ln(1 + fX)\right], \quad f^* = \arg\max_{f}\mathbb{E}\left[\ln(1 + fX)\right]""",
                        note = "Maximizes asymptotic compound wealth and minimizes expected time to any wealth target."
                    ),
                    CheatsheetFormula(
                        title = "Continuous-Time Kelly",
                        latex = """f^* = \frac{\mu - r}{\sigma^2}""",
                        note = "Ed Thorp: in practice use fractional Kelly (e.g., f*/2) because estimation error and heavy tails make over-betting catastrophic—half-Kelly gives 75% of max growth at 25% of variance."
                    ),
                    CheatsheetFormula(
                        title = "Geometric vs Arithmetic Mean Growth",
                        latex = """G = \left(\prod_{i=1}^n (1 + R_i)\right)^{1/n} - 1 \le \bar{R} = \frac{1}{n}\sum_{i=1}^n R_i"""
                    ),
                    CheatsheetFormula(
                        title = "Portfolio Growth Rate Penalty",
                        latex = """g(f) \approx r + f(\mu - r) - \frac{1}{2}f^2\sigma^2""",
                        note = "Quadratic expansion near optimum: betting past 2f* yields negative expected compounding growth."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Ergodicity, Volatility Drag & Ruin",
                shortTitle = "Ergodicity",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Volatility Drag",
                        latex = """g \approx \mu - \frac{\sigma^2}{2}""",
                        note = "For multiplicative returns, compound growth equals arithmetic drift minus half the variance."
                    ),
                    CheatsheetFormula(
                        title = "Asymmetric Drawdown Recovery",
                        latex = """r_{\text{rec}} = \frac{L}{1 - L}""",
                        note = "A 50% loss (L = 0.50) requires +100% to recover; an 80% loss requires +400%."
                    ),
                    CheatsheetFormula(
                        title = "Gambler's Ruin Probability",
                        latex = """P_{\text{ruin}} = \begin{cases} \frac{(q/p)^a - (q/p)^N}{1 - (q/p)^N} & p \neq q \\ 1 - \frac{a}{N} & p = q = \frac{1}{2} \end{cases}""",
                        note = "Starting capital a, target N, per-bet win probability p and loss probability q = 1 − p."
                    ),
                    CheatsheetFormula(
                        title = "Time-Average vs Ensemble-Average Growth",
                        latex = """g_{\text{time}} = \lim_{T\to\infty}\frac{1}{T}\ln\frac{W(T)}{W(0)} \neq \lim_{N\to\infty}\frac{1}{T}\ln\left(\frac{1}{N}\sum_{i=1}^N \frac{W_i(T)}{W(0)}\right) = g_{\text{ens}}""",
                        note = "Multiplicative wealth is non-ergodic: ensemble mean can grow while almost every individual path collapses."
                    ),
                    CheatsheetFormula(
                        title = "Maximum Drawdown Definition",
                        latex = """\mathrm{MDD}(T) = \sup_{t \in [0, T]} \left(\frac{\max_{s \in [0, t]} W(s) - W(t)}{\max_{s \in [0, t]} W(s)}\right)"""
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Convexity, Fat Tails & Model-Free Bounds",
                shortTitle = "Tail Risk",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Jensen's Inequality (Fragility vs Antifragility)",
                        latex = """\text{Convex } f: \;\mathbb{E}[f(X)] \ge f(\mathbb{E}[X]), \quad \text{Concave } f: \;\mathbb{E}[f(X)] \le f(\mathbb{E}[X])""",
                        note = "Taleb fragility heuristic: concave payoffs suffer from variance (fragile), convex payoffs gain from disorder (antifragile)."
                    ),
                    CheatsheetFormula(
                        title = "Model-Free Put-Call Parity",
                        latex = """C - P = S - K e^{-rT}""",
                        note = "Holds for European options on non-dividend underlying via static replication, with zero distributional assumptions."
                    ),
                    CheatsheetFormula(
                        title = "No-Arbitrage Option Bounds",
                        latex = """\max\left(0,\; S - K e^{-rT}\right) \le C \le S, \quad \max\left(0,\; K e^{-rT} - S\right) \le P \le K e^{-rT}""",
                        note = "Model-free European call (C) and put (P) price boundaries independent of return distribution."
                    ),
                    CheatsheetFormula(
                        title = "Pareto Conditional Tail Expectation",
                        latex = """\mathbb{E}[X \mid X > K] = \frac{\alpha}{\alpha - 1}K \quad (\alpha > 1)""",
                        note = "For power-law tail P(X > x) ~ x^(−α): conditional excess above threshold K scales linearly with K."
                    ),
                    CheatsheetFormula(
                        title = "Power-Law Tail Aggregation (Max-Sum Equivalence)",
                        latex = """\lim_{x \to \infty} \frac{\mathbb{P}\left(\sum_{i=1}^n X_i > x\right)}{\mathbb{P}\left(\max_{1 \le i \le n} X_i > x\right)} = 1 \quad (\alpha < 2)""",
                        note = "Subexponential catastrophe principle: extreme sums are dominated by a single outlier rather than many small shocks."
                    )
                )
            )
        )
    )

    val INFORMATION_THEORY_TOPIC: CheatsheetTopic = CheatsheetTopic(
        id = "information_theory",
        title = "Information Theory",
        shortTitle = "Info Theory",
        symbol = "H",
        description = "Entropy, divergence, mutual information, Shannon's coding theorems, channel capacity, and core inequalities.",
        domain = CheatsheetDomain.ENGINEERING_QUANT,
        kind = CheatsheetKind.FORMULAS,
        subsections = listOf(
            CheatsheetSubsection(
                title = "Entropy, Divergence & Mutual Information",
                shortTitle = "Measures",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Self-Information & Hartley Entropy",
                        latex = """I(x) = -\log_2 p(x), \quad H_0 = \log_2 |\mathcal{X}|""",
                        note = "Surprise of event x in bits; maximum uncertainty across alphabet X under uniform belief."
                    ),
                    CheatsheetFormula(
                        title = "Shannon Entropy",
                        latex = """H(X) = -\sum_{x \in \mathcal{X}} p(x)\log_2 p(x)""",
                        note = "Expected description length and uncertainty of discrete random variable X (0 log 0 = 0)."
                    ),
                    CheatsheetFormula(
                        title = "Binary Entropy Function",
                        latex = """H_b(p) = -p\log_2 p - (1 - p)\log_2(1 - p)""",
                        note = "Entropy of Bernoulli(p) in bits; symmetric around maximum H_b(1/2) = 1 bit."
                    ),
                    CheatsheetFormula(
                        title = "Joint, Conditional Entropy & Chain Rule",
                        latex = """H(X, Y) = H(X) + H(Y \mid X) = H(Y) + H(X \mid Y)""",
                        note = "Generalizes to H(X₁, …, Xₙ) = Σᵢ H(Xᵢ | X₁, …, Xᵢ₋₁)."
                    ),
                    CheatsheetFormula(
                        title = "Kullback–Leibler Divergence",
                        latex = """D_{\mathrm{KL}}(P \parallel Q) = \sum_{x \in \mathcal{X}} P(x)\log_2\frac{P(x)}{Q(x)}""",
                        note = "Asymmetric relative entropy measuring excess bits wasted when encoding distribution P with model Q."
                    ),
                    CheatsheetFormula(
                        title = "Cross-Entropy",
                        latex = """H(P, Q) = -\sum_{x \in \mathcal{X}} P(x)\log_2 Q(x) = H(P) + D_{\mathrm{KL}}(P \parallel Q)""",
                        note = "Average code length under wrong distribution Q; equals true entropy plus KL penalty."
                    ),
                    CheatsheetFormula(
                        title = "Mutual Information & Chain Rule",
                        latex = """I(X; Y) = H(X) - H(X \mid Y) = D_{\mathrm{KL}}(P_{XY} \parallel P_X P_Y)""",
                        note = "Symmetric reduction in uncertainty about X from observing Y; satisfies I(X₁,…,Xₙ; Y) = Σᵢ I(Xᵢ; Y | X₁^(i−1))."
                    ),
                    CheatsheetFormula(
                        title = "Conditional Mutual Information",
                        latex = """I(X; Y \mid Z) = H(X \mid Z) - H(X \mid Y, Z) = \mathbb{E}_Z\left[D_{\mathrm{KL}}(P_{XY|Z} \parallel P_{X|Z}P_{Y|Z})\right]""",
                        note = "Equals zero if and only if X and Y are conditionally independent given Z."
                    ),
                    CheatsheetFormula(
                        title = "Differential Entropy & Gaussian Limit",
                        latex = """h(X) = -\int_{-\infty}^{\infty} f(x)\ln f(x)\,dx, \quad h\left(\mathcal{N}(\mu, \sigma^2)\right) = \frac{1}{2}\ln(2\pi e \sigma^2)""",
                        note = "Gaussian distribution maximizes differential entropy among all continuous distributions with fixed variance σ²."
                    ),
                    CheatsheetFormula(
                        title = "Jensen–Shannon Divergence",
                        latex = """\mathrm{JSD}(P \parallel Q) = \frac{1}{2}D_{\mathrm{KL}}(P \parallel M) + \frac{1}{2}D_{\mathrm{KL}}(Q \parallel M), \quad M = \frac{P + Q}{2}""",
                        note = "Symmetric, smoothed divergence bounded in [0, 1] bit whose square root is a true metric."
                    ),
                    CheatsheetFormula(
                        title = "Rényi Entropy",
                        latex = """H_\alpha(X) = \frac{1}{1 - \alpha}\log_2\sum_{x \in \mathcal{X}} p(x)^\alpha \quad (\alpha \ge 0,\; \alpha \neq 1)""",
                        note = "Recovers Hartley (α=0), Shannon (α→1), collision entropy H₂, and min-entropy H_∞ = −log₂(max p)."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Fundamental Theorems",
                shortTitle = "Theorems",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Shannon's Source Coding Theorem",
                        latex = """H(X) \le L < H(X) + 1""",
                        note = "Expected codeword length L per symbol in lossless compression (approached arbitrarily closely via block coding)."
                    ),
                    CheatsheetFormula(
                        title = "Shannon's Noisy-Channel Coding Theorem",
                        latex = """R < C = \max_{p(x)} I(X; Y)""",
                        note = "Arbitrarily low error probability is achievable iff transmission rate R is below channel capacity C."
                    ),
                    CheatsheetFormula(
                        title = "Shannon–Hartley Theorem (AWGN)",
                        latex = """C = \frac{1}{2}\log_2(1 + \mathrm{SNR}), \quad C_B = B\log_2\left(1 + \frac{S}{N}\right)""",
                        note = "Capacity in bits per real channel use (left) or bps over bandwidth B Hz with signal-to-noise ratio S/N (right)."
                    ),
                    CheatsheetFormula(
                        title = "Asymptotic Equipartition Property (AEP)",
                        latex = """-\frac{1}{n}\log_2 p(X_1, \dots, X_n) \to H(X)""",
                        note = "Converges in probability for i.i.d. sources; defines the typical set A_ε^(n) of size ≈ 2^(n H(X))."
                    ),
                    CheatsheetFormula(
                        title = "Kraft–McMillan Inequality",
                        latex = """\sum_{i=1}^{n} D^{-\ell_i} \le 1""",
                        note = "Necessary and sufficient condition for a uniquely decodable (and prefix-free) D-ary code of lengths ℓᵢ."
                    ),
                    CheatsheetFormula(
                        title = "Rate–Distortion Theorem",
                        latex = """R(D) = \min_{p(\hat{x}|x):\;\mathbb{E}[d(X,\hat{X})] \le D} I(X; \hat{X})""",
                        note = "Minimum achievable rate (bits/symbol) for lossy compression at average distortion at most D."
                    ),
                    CheatsheetFormula(
                        title = "Slepian–Wolf Distributed Coding Theorem",
                        latex = """R_1 \ge H(X \mid Y), \quad R_2 \ge H(Y \mid X), \quad R_1 + R_2 \ge H(X, Y)""",
                        note = "Separately encoded correlated sources X, Y can be jointly decoded losslessly with no rate penalty vs joint encoding."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Core Inequalities & Bounds",
                shortTitle = "Inequalities",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Gibbs' Inequality",
                        latex = """D_{\mathrm{KL}}(P \parallel Q) \ge 0""",
                        note = "Non-negativity of KL divergence, with equality if and only if P = Q almost everywhere."
                    ),
                    CheatsheetFormula(
                        title = "Data Processing Inequality (DPI)",
                        latex = """X \to Y \to Z \implies I(X; Z) \le I(X; Y)""",
                        note = "For any Markov chain, deterministic or noisy post-processing cannot increase mutual information."
                    ),
                    CheatsheetFormula(
                        title = "Fano's Inequality",
                        latex = """H(X \mid \hat{X}) \le H_b(P_e) + P_e\log_2\left(|\mathcal{X}| - 1\right)""",
                        note = "Lower-bounds error probability P_e = P(X̂ ≠ X) of any estimator in terms of conditional entropy."
                    ),
                    CheatsheetFormula(
                        title = "Conditioning Reduces Entropy & Subadditivity",
                        latex = """H(X \mid Y) \le H(X), \quad H(X_1, \dots, X_n) \le \sum_{i=1}^{n} H(X_i)""",
                        note = "Information never hurts on average; equality holds iff variables are statistically independent."
                    ),
                    CheatsheetFormula(
                        title = "Pinsker's Inequality",
                        latex = """\mathrm{TV}(P, Q) = \frac{1}{2}\sum_{x}|P(x) - Q(x)| \le \sqrt{\frac{\ln 2}{2} D_{\mathrm{KL}}(P \parallel Q)}""",
                        note = "Upper-bounds L1 / total variation distance between distributions via square root of KL divergence (in bits)."
                    ),
                    CheatsheetFormula(
                        title = "Entropy Power Inequality (EPI)",
                        latex = """e^{2h(X + Y)} \ge e^{2h(X)} + e^{2h(Y)}""",
                        note = "Holds for independent continuous n-vectors (exponent 2/n in ℝⁿ), with equality for proportional Gaussians."
                    ),
                    CheatsheetFormula(
                        title = "Maximum Entropy Principle",
                        latex = """p^*(x) = \frac{1}{Z(\lambda)}\exp\left(-\sum_{k} \lambda_k f_k(x)\right)""",
                        note = "Least-committal distribution maximizing H(X) subject to known moment constraints E[fₖ(X)] = αₖ."
                    ),
                    CheatsheetFormula(
                        title = "Fisher Information & Cramér–Rao Bound",
                        latex = """J(\theta) = \mathbb{E}\left[\left(\frac{\partial \ln p_\theta(X)}{\partial \theta}\right)^2\right], \quad \operatorname{Var}(\hat{\theta}) \ge \frac{1}{J(\theta)}""",
                        note = "Lower-bounds unbiased estimator variance via local curvature of log-likelihood (second derivative of KL)."
                    )
                )
            )
        )
    )

    val BIOLOGY_PHYSIOLOGY_TOPIC: CheatsheetTopic = CheatsheetTopic(
        id = "biology_physiology",
        title = "Biology & Physiology",
        shortTitle = "Biology & Phys.",
        symbol = "ΔG",
        description = "Population genetics, enzyme kinetics, electrophysiology, fluid transport, metabolic scaling, and clinical baselines.",
        domain = CheatsheetDomain.SCIENCES,
        kind = CheatsheetKind.BIOLOGY,
        subsections = listOf(
            CheatsheetSubsection(
                title = "Population Genetics & Kinetics",
                shortTitle = "Kinetics",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Hardy-Weinberg Equilibrium",
                        latex = """p + q = 1, \quad p^2 + 2pq + q^2 = 1""",
                        note = "Assumes infinite population, random mating, and no mutation, migration, or selection."
                    ),
                    CheatsheetFormula(
                        title = "Michaelis-Menten Kinetics",
                        latex = """v = \frac{V_{\max}[S]}{K_m + [S]}, \quad V_{\max} = k_{\text{cat}}[E]_T"""
                    ),
                    CheatsheetFormula(
                        title = "Lineweaver-Burk & Catalytic Efficiency",
                        latex = """\frac{1}{v} = \frac{K_m}{V_{\max}}\cdot\frac{1}{[S]} + \frac{1}{V_{\max}}, \quad \text{Efficiency} = \frac{k_{\text{cat}}}{K_m}"""
                    ),
                    CheatsheetFormula(
                        title = "Hill Cooperativity Equation",
                        latex = """\theta = \frac{[L]^n}{K_d + [L]^n} = \frac{[L]^n}{K_{0.5}^n + [L]^n}""",
                        note = "Hill coefficient n > 1 indicates positive cooperativity (e.g., hemoglobin O₂ binding, n ≈ 2.8)."
                    ),
                    CheatsheetFormula(
                        title = "Exponential & Logistic Population Growth",
                        latex = """\frac{dN}{dt} = rN, \quad \frac{dN}{dt} = rN\left(1 - \frac{N}{K}\right)""",
                        note = "Intrinsic growth rate r and carrying capacity K."
                    ),
                    CheatsheetFormula(
                        title = "Lotka-Volterra Predator-Prey Equations",
                        latex = """\frac{dx}{dt} = \alpha x - \beta xy, \quad \frac{dy}{dt} = \delta xy - \gamma y""",
                        note = "Prey density x and predator density y with positive interaction constants α, β, γ, δ."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Electrophysiology, Fluid & Energetics",
                shortTitle = "Biophysics",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Nernst Equilibrium Potential",
                        latex = """E_{\text{ion}} = \frac{RT}{zF}\ln\frac{[\text{ion}]_o}{[\text{ion}]_i} \approx \frac{61.5\text{ mV}}{z}\log_{10}\frac{[\text{ion}]_o}{[\text{ion}]_i}""",
                        note = "61.5 mV factor applies at physiological 37 °C; z is the ion valence."
                    ),
                    CheatsheetFormula(
                        title = "Goldman-Hodgkin-Katz Voltage Equation",
                        latex = """V_m = \frac{RT}{F}\ln\left(\frac{P_K[K^+]_o + P_{Na}[Na^+]_o + P_{Cl}[Cl^-]_i}{P_K[K^+]_i + P_{Na}[Na^+]_i + P_{Cl}[Cl^-]_o}\right)"""
                    ),
                    CheatsheetFormula(
                        title = "Henderson-Hasselbalch Equation",
                        latex = """\mathrm{pH} = \mathrm{p}K_a + \log_{10}\frac{[\mathrm{A}^-]}{[\mathrm{HA}]}, \quad \mathrm{pH} = 6.1 + \log_{10}\frac{[\mathrm{HCO}_3^-]}{0.03\,P_{\mathrm{CO}_2}}"""
                    ),
                    CheatsheetFormula(
                        title = "Fick's First Law of Diffusion",
                        latex = """J = -D\frac{\partial C}{\partial x}"""
                    ),
                    CheatsheetFormula(
                        title = "Hagen-Poiseuille Vascular Flow",
                        latex = """Q = \frac{\pi r^4 \Delta P}{8\eta L}, \quad R = \frac{8\eta L}{\pi r^4}""",
                        note = "Halving vessel radius r increases vascular resistance R by 16×."
                    ),
                    CheatsheetFormula(
                        title = "Kleiber's Metabolic Scaling",
                        latex = """P \propto M^{3/4}""",
                        note = "Basal metabolic rate P scales to the 3/4 power of body mass M across mammals."
                    ),
                    CheatsheetFormula(
                        title = "Aerobic Respiration & ATP Hydrolysis ΔG",
                        latex = """\Delta G = \Delta G^{\circ} + RT\ln\frac{[\mathrm{ADP}][\mathrm{P}_i]}{[\mathrm{ATP}]} \approx -50\text{ kJ/mol}""",
                        note = "Overall aerobic glucose oxidation: C₆H₁₂O₆ + 6 O₂ → 6 CO₂ + 6 H₂O (ΔG°' ≈ −2,880 kJ/mol)."
                    )
                )
            )
        )
    )

    val HUMAN_PHYSIOLOGY_BASELINES: List<PhysiologyBaseline> = listOf(
        // Vitals
        PhysiologyBaseline("Vitals", "Core Body Temperature", "36.5 – 37.5", "°C"),
        PhysiologyBaseline("Vitals", "Resting Heart Rate", "60 – 100", "bpm"),
        PhysiologyBaseline("Vitals", "Respiratory Rate", "12 – 20", "breaths/min"),
        PhysiologyBaseline("Vitals", "Blood Pressure (SBP / DBP)", "< 120 / < 80", "mmHg"),
        // Hemodynamics
        PhysiologyBaseline("Hemodynamics", "Mean Arterial Pressure (MAP)", "70 – 100", "mmHg"),
        PhysiologyBaseline("Hemodynamics", "Cardiac Output (CO)", "4.0 – 8.0", "L/min"),
        PhysiologyBaseline("Hemodynamics", "Ejection Fraction (LVEF)", "55 – 70", "%"),
        PhysiologyBaseline("Hemodynamics", "Central Venous Pressure (CVP)", "2 – 8", "mmHg"),
        // Blood Gas & Electrolytes
        PhysiologyBaseline("Blood Gas & Electrolytes", "Arterial pH", "7.35 – 7.45", "pH units"),
        PhysiologyBaseline("Blood Gas & Electrolytes", "Arterial PaCO₂", "35 – 45", "mmHg"),
        PhysiologyBaseline("Blood Gas & Electrolytes", "Arterial PaO₂", "80 – 100", "mmHg"),
        PhysiologyBaseline("Blood Gas & Electrolytes", "Serum Bicarbonate (HCO₃⁻)", "22 – 26", "mEq/L"),
        PhysiologyBaseline("Blood Gas & Electrolytes", "Serum Sodium (Na⁺)", "135 – 145", "mEq/L"),
        PhysiologyBaseline("Blood Gas & Electrolytes", "Serum Potassium (K⁺)", "3.5 – 5.0", "mEq/L"),
        // Hematology (CBC)
        PhysiologyBaseline("Hematology (CBC)", "Hemoglobin (Hb)", "13.5–17.5 (M) / 12.0–15.5 (F)", "g/dL"),
        PhysiologyBaseline("Hematology (CBC)", "Hematocrit (Hct)", "41–50 (M) / 36–44 (F)", "%"),
        PhysiologyBaseline("Hematology (CBC)", "White Blood Cells (WBC)", "4.5 – 11.0", "×10³/µL"),
        PhysiologyBaseline("Hematology (CBC)", "Platelets", "150 – 400", "×10³/µL"),
        // Metabolic & Renal
        PhysiologyBaseline("Metabolic & Renal", "Fasting Blood Glucose", "70 – 99", "mg/dL"),
        PhysiologyBaseline("Metabolic & Renal", "Serum Creatinine", "0.7–1.3 (M) / 0.6–1.1 (F)", "mg/dL"),
        PhysiologyBaseline("Metabolic & Renal", "Blood Urea Nitrogen (BUN)", "7 – 20", "mg/dL"),
        PhysiologyBaseline("Metabolic & Renal", "Estimated GFR (eGFR)", "≥ 90", "mL/min/1.73m²")
    )
}
