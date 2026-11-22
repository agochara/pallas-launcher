package com.pallas.launcher.domain.math

data class CheatsheetFormula(
    val title: String,
    val latex: String,
    val note: String? = null
)

data class CheatsheetSubsection(
    val title: String,
    val formulas: List<CheatsheetFormula>,
    val shortTitle: String = title
)

data class CheatsheetTopic(
    val id: String,
    val title: String,
    val shortTitle: String,
    val symbol: String,
    val description: String,
    val subsections: List<CheatsheetSubsection>,
    val showLatexSource: Boolean = false
) {
    val formulaCount: Int
        get() = subsections.sumOf { it.formulas.size }
}

object MathCheatsheets {
    val TOPICS: List<CheatsheetTopic> = listOf(
        CheatsheetTopic(
            id = "calculus",
            title = "Calculus",
            shortTitle = "Calculus",
            symbol = "∫",
            description = "Limits, differentiation, integration, infinite series, vector calculus, ODEs, and transforms.",
            subsections = listOf(
                CheatsheetSubsection(
                    title = "Limits & Asymptotics",
                    shortTitle = "Limits",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Sinc & Trigonometric Limits",
                            latex = """\lim_{x \to 0} \frac{\sin x}{x} = 1, \quad \lim_{x \to 0} \frac{1 - \cos x}{x^2} = \frac{1}{2}"""
                        ),
                        CheatsheetFormula(
                            title = "Exponential & Logarithmic Limits",
                            latex = """\lim_{x \to \infty} \left(1 + \frac{a}{x}\right)^x = e^a, \quad \lim_{x \to 0} \frac{e^x - 1}{x} = 1, \quad \lim_{x \to 0} \frac{\ln(1+x)}{x} = 1"""
                        ),
                        CheatsheetFormula(
                            title = "L'Hôpital's Rule",
                            latex = """\lim_{x \to c} \frac{f(x)}{g(x)} = \lim_{x \to c} \frac{f'(x)}{g'(x)}""",
                            note = "Valid for 0/0 or ±∞/∞ indeterminate forms when g'(x) ≠ 0 near c."
                        ),
                        CheatsheetFormula(
                            title = "Stirling's Approximation",
                            latex = """n! \sim \sqrt{2\pi n}\left(\frac{n}{e}\right)^n \quad (n \to \infty)"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Differentiation",
                    shortTitle = "Derivatives",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Product & Quotient Rules",
                            latex = """(fg)' = f'g + fg', \quad \left(\frac{f}{g}\right)' = \frac{f'g - fg'}{g^2}"""
                        ),
                        CheatsheetFormula(
                            title = "Chain Rule",
                            latex = """\frac{d}{dx} f(g(x)) = f'(g(x))\,g'(x)"""
                        ),
                        CheatsheetFormula(
                            title = "General Leibniz Rule (n-th Derivative)",
                            latex = """(fg)^{(n)} = \sum_{k=0}^{n} \binom{n}{k} f^{(n-k)} g^{(k)}"""
                        ),
                        CheatsheetFormula(
                            title = "Inverse Function Theorem",
                            latex = """\left(f^{-1}\right)'(y) = \frac{1}{f'\left(f^{-1}(y)\right)}"""
                        ),
                        CheatsheetFormula(
                            title = "Logarithmic Differentiation",
                            latex = """\frac{d}{dx}\left(f(x)^{g(x)}\right) = f(x)^{g(x)}\left(g'(x)\ln f(x) + \frac{g(x)f'(x)}{f(x)}\right)"""
                        ),
                        CheatsheetFormula(
                            title = "Secant & Tangent",
                            latex = """\frac{d}{dx}\tan x = \sec^2 x, \quad \frac{d}{dx}\sec x = \sec x \tan x"""
                        ),
                        CheatsheetFormula(
                            title = "Inverse Trigonometric",
                            latex = """\frac{d}{dx}\arcsin x = \frac{1}{\sqrt{1 - x^2}}, \quad \frac{d}{dx}\arctan x = \frac{1}{1 + x^2}"""
                        ),
                        CheatsheetFormula(
                            title = "Inverse Secant",
                            latex = """\frac{d}{dx}\operatorname{arcsec} x = \frac{1}{|x|\sqrt{x^2 - 1}} \quad (|x| > 1)"""
                        ),
                        CheatsheetFormula(
                            title = "Inverse Hyperbolic",
                            latex = """\frac{d}{dx}\operatorname{arsinh} x = \frac{1}{\sqrt{x^2 + 1}}, \quad \frac{d}{dx}\operatorname{artanh} x = \frac{1}{1 - x^2}"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Integration & Theorems",
                    shortTitle = "Integrals",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Fundamental Theorem of Calculus",
                            latex = """\frac{d}{dx}\int_a^x f(t)\,dt = f(x), \quad \int_a^b f'(x)\,dx = f(b) - f(a)"""
                        ),
                        CheatsheetFormula(
                            title = "Leibniz Integral Rule",
                            latex = """\frac{d}{dx}\int_{a(x)}^{b(x)} f(x,t)\,dt = f(x,b(x))b'(x) - f(x,a(x))a'(x) + \int_{a(x)}^{b(x)} \frac{\partial f}{\partial x}(x,t)\,dt"""
                        ),
                        CheatsheetFormula(
                            title = "Integration by Parts",
                            latex = """\int u\,dv = uv - \int v\,du"""
                        ),
                        CheatsheetFormula(
                            title = "Weierstrass Substitution",
                            latex = """t = \tan\frac{x}{2} \implies \sin x = \frac{2t}{1+t^2},\; \cos x = \frac{1-t^2}{1+t^2},\; dx = \frac{2\,dt}{1+t^2}""",
                            note = "Converts any rational function of sin x and cos x into a rational function of t."
                        ),
                        CheatsheetFormula(
                            title = "Quadratic Denominators",
                            latex = """\int \frac{dx}{x^2 + a^2} = \frac{1}{a}\arctan\frac{x}{a}, \quad \int \frac{dx}{x^2 - a^2} = \frac{1}{2a}\ln\left|\frac{x - a}{x + a}\right|"""
                        ),
                        CheatsheetFormula(
                            title = "Radical Forms (Inverse Trig & Hyperbolic)",
                            latex = """\int \frac{dx}{\sqrt{a^2 - x^2}} = \arcsin\frac{x}{a}, \quad \int \frac{dx}{\sqrt{x^2 \pm a^2}} = \ln\left|x + \sqrt{x^2 \pm a^2}\right|"""
                        ),
                        CheatsheetFormula(
                            title = "Circular Segment Area Integral",
                            latex = """\int \sqrt{a^2 - x^2}\,dx = \frac{x}{2}\sqrt{a^2 - x^2} + \frac{a^2}{2}\arcsin\frac{x}{a}"""
                        ),
                        CheatsheetFormula(
                            title = "Secant & Secant Cubed",
                            latex = """\int \sec x\,dx = \ln|\sec x + \tan x|, \quad \int \sec^3 x\,dx = \frac{1}{2}\left(\sec x \tan x + \ln|\sec x + \tan x|\right)"""
                        ),
                        CheatsheetFormula(
                            title = "Damped Oscillatory Integrals",
                            latex = """\int e^{ax}\sin(bx)\,dx = \frac{e^{ax}(a\sin bx - b\cos bx)}{a^2 + b^2}, \quad \int e^{ax}\cos(bx)\,dx = \frac{e^{ax}(a\cos bx + b\sin bx)}{a^2 + b^2}"""
                        ),
                        CheatsheetFormula(
                            title = "Logarithmic Integration",
                            latex = """\int \ln x\,dx = x\ln x - x + C"""
                        ),
                        CheatsheetFormula(
                            title = "Gaussian Integral",
                            latex = """\int_{-\infty}^{\infty} e^{-ax^2}\,dx = \sqrt{\frac{\pi}{a}} \quad (a > 0)"""
                        ),
                        CheatsheetFormula(
                            title = "Euler Gamma & Beta Functions",
                            latex = """\Gamma(z) = \int_0^\infty t^{z-1}e^{-t}\,dt, \quad \mathrm{B}(x,y) = \int_0^1 t^{x-1}(1-t)^{y-1}\,dt = \frac{\Gamma(x)\Gamma(y)}{\Gamma(x+y)}"""
                        ),
                        CheatsheetFormula(
                            title = "Dirichlet Integral",
                            latex = """\int_0^\infty \frac{\sin x}{x}\,dx = \frac{\pi}{2}"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Infinite Series & Taylor Expansions",
                    shortTitle = "Series",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Taylor's Theorem (Lagrange Remainder)",
                            latex = """f(x) = \sum_{k=0}^{n} \frac{f^{(k)}(a)}{k!}(x-a)^k + \frac{f^{(n+1)}(\xi)}{(n+1)!}(x-a)^{n+1}""",
                            note = "For some ξ strictly between a and x."
                        ),
                        CheatsheetFormula(
                            title = "Exponential & Euler's Formula",
                            latex = """e^x = \sum_{n=0}^{\infty} \frac{x^n}{n!}, \quad e^{i\theta} = \cos\theta + i\sin\theta"""
                        ),
                        CheatsheetFormula(
                            title = "Sine & Cosine Series",
                            latex = """\sin x = \sum_{n=0}^{\infty} \frac{(-1)^n x^{2n+1}}{(2n+1)!}, \quad \cos x = \sum_{n=0}^{\infty} \frac{(-1)^n x^{2n}}{(2n)!}"""
                        ),
                        CheatsheetFormula(
                            title = "Logarithm & Arctangent Series",
                            latex = """\ln(1+x) = \sum_{n=1}^{\infty} \frac{(-1)^{n-1}x^n}{n}\;(|x|<1), \quad \arctan x = \sum_{n=0}^{\infty} \frac{(-1)^n x^{2n+1}}{2n+1}\;(|x|\le 1)"""
                        ),
                        CheatsheetFormula(
                            title = "Generalized Binomial Series",
                            latex = """(1+x)^\alpha = \sum_{n=0}^{\infty} \binom{\alpha}{n} x^n, \quad \binom{\alpha}{n} = \frac{\alpha(\alpha-1)\cdots(\alpha-n+1)}{n!} \quad (|x|<1)"""
                        ),
                        CheatsheetFormula(
                            title = "D'Alembert Ratio & Cauchy Root Tests",
                            latex = """L = \lim_{n\to\infty}\left|\frac{a_{n+1}}{a_n}\right| \;\text{or}\; \limsup_{n\to\infty} |a_n|^{1/n}: \quad L < 1 \implies \text{converges abs.}"""
                        ),
                        CheatsheetFormula(
                            title = "Cauchy-Hadamard Radius of Convergence",
                            latex = """\frac{1}{R} = \limsup_{n\to\infty} |c_n|^{1/n} \quad \text{for } \sum_{n=0}^{\infty} c_n (z - z_0)^n"""
                        ),
                        CheatsheetFormula(
                            title = "Riemann Zeta & Basel Problem",
                            latex = """\zeta(s) = \sum_{n=1}^{\infty} \frac{1}{n^s}\;(\operatorname{Re}(s)>1), \quad \zeta(2) = \sum_{n=1}^{\infty} \frac{1}{n^2} = \frac{\pi^2}{6}"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Multivariable & Vector Calculus",
                    shortTitle = "Vector",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Gradient, Divergence & Curl",
                            latex = """\nabla f = \left(\frac{\partial f}{\partial x}, \frac{\partial f}{\partial y}, \frac{\partial f}{\partial z}\right), \quad \nabla\cdot\mathbf{F} = \sum_i \frac{\partial F_i}{\partial x_i}, \quad \nabla\times\mathbf{F} = \det\begin{pmatrix}\mathbf{i}&\mathbf{j}&\mathbf{k}\\\partial_x&\partial_y&\partial_z\\F_x&F_y&F_z\end{pmatrix}"""
                        ),
                        CheatsheetFormula(
                            title = "Exactness & Solenoidal Identities",
                            latex = """\nabla \times (\nabla f) = \mathbf{0}, \quad \nabla \cdot (\nabla \times \mathbf{F}) = 0"""
                        ),
                        CheatsheetFormula(
                            title = "Vector Laplacian Identity",
                            latex = """\nabla \times (\nabla \times \mathbf{F}) = \nabla(\nabla \cdot \mathbf{F}) - \nabla^2 \mathbf{F}"""
                        ),
                        CheatsheetFormula(
                            title = "Divergence Product Rule",
                            latex = """\nabla \cdot (f\mathbf{F}) = (\nabla f)\cdot\mathbf{F} + f(\nabla \cdot \mathbf{F})"""
                        ),
                        CheatsheetFormula(
                            title = "Change of Variables & Spherical Jacobian",
                            latex = """\iint_R f(x,y)\,dx\,dy = \iint_D f(T(u,v))\left|\det J_T\right|\,du\,dv, \quad dV = r^2\sin\varphi\,dr\,d\varphi\,d\theta"""
                        ),
                        CheatsheetFormula(
                            title = "Green's Theorem",
                            latex = """\oint_{\partial D} (P\,dx + Q\,dy) = \iint_D \left(\frac{\partial Q}{\partial x} - \frac{\partial P}{\partial y}\right) dx\,dy"""
                        ),
                        CheatsheetFormula(
                            title = "Stokes' Theorem",
                            latex = """\oint_{\partial \Sigma} \mathbf{F} \cdot d\mathbf{r} = \iint_\Sigma (\nabla \times \mathbf{F}) \cdot d\mathbf{S}"""
                        ),
                        CheatsheetFormula(
                            title = "Divergence (Gauss) Theorem",
                            latex = """\iint_{\partial \Omega} \mathbf{F} \cdot d\mathbf{S} = \iiint_\Omega (\nabla \cdot \mathbf{F})\,dV"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Differential Equations & Transforms",
                    shortTitle = "ODEs",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "First-Order Linear Integrating Factor",
                            latex = """y' + P(x)y = Q(x) \implies \mu(x) = e^{\int P(x)\,dx}, \quad y = \frac{1}{\mu(x)}\int \mu(x)Q(x)\,dx"""
                        ),
                        CheatsheetFormula(
                            title = "Second-Order Characteristic Roots",
                            latex = """ay'' + by' + cy = 0 \implies ar^2 + br + c = 0"""
                        ),
                        CheatsheetFormula(
                            title = "Variation of Parameters (Wronskian)",
                            latex = """y_p = -y_1\int\frac{y_2 g}{W}\,dx + y_2\int\frac{y_1 g}{W}\,dx, \quad W(y_1,y_2) = y_1 y_2' - y_1' y_2"""
                        ),
                        CheatsheetFormula(
                            title = "Euler-Lagrange Equation",
                            latex = """\frac{\partial L}{\partial q} - \frac{d}{dt}\left(\frac{\partial L}{\partial \dot{q}}\right) = 0"""
                        ),
                        CheatsheetFormula(
                            title = "Laplace Transform & Derivative Property",
                            latex = """\mathcal{L}\{f\}(s) = \int_0^\infty e^{-st}f(t)\,dt, \quad \mathcal{L}\{f^{(n)}\}(s) = s^n F(s) - \sum_{k=0}^{n-1} s^{n-1-k}f^{(k)}(0)"""
                        ),
                        CheatsheetFormula(
                            title = "Laplace Convolution Theorem",
                            latex = """\mathcal{L}\{(f * g)(t)\} = F(s)G(s), \quad (f * g)(t) = \int_0^t f(\tau)g(t-\tau)\,d\tau"""
                        ),
                        CheatsheetFormula(
                            title = "Fourier Transform Pair",
                            latex = """\hat{f}(\xi) = \int_{-\infty}^{\infty} f(x)e^{-2\pi i x \xi}\,dx, \quad f(x) = \int_{-\infty}^{\infty} \hat{f}(\xi)e^{2\pi i x \xi}\,d\xi"""
                        ),
                        CheatsheetFormula(
                            title = "Parseval–Plancherel Identity",
                            latex = """\int_{-\infty}^{\infty} |f(x)|^2\,dx = \int_{-\infty}^{\infty} |\hat{f}(\xi)|^2\,d\xi"""
                        )
                    )
                )
            )
        ),
        CheatsheetTopic(
            id = "linear_algebra",
            title = "Linear Algebra",
            shortTitle = "Linear Algebra",
            symbol = "λ",
            description = "Eigenvalues, spectral & SVD decompositions, determinants, projections, and matrix calculus.",
            subsections = listOf(
                CheatsheetSubsection(
                    title = "Eigenvalues, Decompositions & Determinants",
                    shortTitle = "Spectral",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Characteristic Equation & Spectral Theorem",
                            latex = """\det(A - \lambda I) = 0, \quad A = A^T \implies A = Q \Lambda Q^T"""
                        ),
                        CheatsheetFormula(
                            title = "Singular Value Decomposition (SVD)",
                            latex = """A = U \Sigma V^T \quad (U^T U = I,\; V^T V = I,\; \Sigma = \operatorname{diag}(\sigma_1,\dots,\sigma_r))"""
                        ),
                        CheatsheetFormula(
                            title = "Cayley-Hamilton Theorem",
                            latex = """p(\lambda) = \det(\lambda I - A) \implies p(A) = 0"""
                        ),
                        CheatsheetFormula(
                            title = "Jacobi's Formula & Matrix Exponential Determinant",
                            latex = """\det(e^A) = e^{\operatorname{tr}(A)}, \quad \frac{d}{dt}\det A(t) = \operatorname{tr}\!\left(\operatorname{adj}(A)\frac{dA}{dt}\right)"""
                        ),
                        CheatsheetFormula(
                            title = "Matrix Determinant Lemma",
                            latex = """\det(A + uv^T) = (1 + v^T A^{-1} u)\det(A)"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Inverses, Projections & Matrix Calculus",
                    shortTitle = "Matrix Calculus",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Sherman-Morrison-Woodbury Identity",
                            latex = """(A + UCV)^{-1} = A^{-1} - A^{-1}U(C^{-1} + V A^{-1} U)^{-1} V A^{-1}"""
                        ),
                        CheatsheetFormula(
                            title = "Moore-Penrose Normal Equations",
                            latex = """A^+ = (A^T A)^{-1}A^T \quad (\text{full column rank}), \quad \hat{x} = \arg\min_x \|Ax - b\|_2"""
                        ),
                        CheatsheetFormula(
                            title = "Gram-Schmidt Orthogonalization",
                            latex = """u_k = v_k - \sum_{j=1}^{k-1} \frac{\langle v_k, u_j \rangle}{\langle u_j, u_j \rangle} u_j, \quad e_k = \frac{u_k}{\|u_k\|_2}"""
                        ),
                        CheatsheetFormula(
                            title = "Quadratic Form Gradient",
                            latex = """\nabla_x (x^T A x) = (A + A^T)x"""
                        ),
                        CheatsheetFormula(
                            title = "Log-Determinant & Inverse Derivatives",
                            latex = """\frac{\partial \ln\det X}{\partial X} = X^{-T}, \quad \frac{\partial X^{-1}}{\partial \alpha} = -X^{-1}\frac{\partial X}{\partial \alpha} X^{-1}"""
                        )
                    )
                )
            )
        ),
        CheatsheetTopic(
            id = "probability_statistics",
            title = "Probability & Statistics",
            shortTitle = "Probability & Stats",
            symbol = "ℙ",
            description = "Bayes' theorem, moments, concentration inequalities, limit laws, distributions, and tail estimators.",
            subsections = listOf(
                CheatsheetSubsection(
                    title = "Foundations, Moments & Inequalities",
                    shortTitle = "Foundations",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Bayes' Theorem & Law of Total Variance",
                            latex = """P(A\mid B) = \frac{P(B\mid A)P(A)}{P(B)}, \quad \operatorname{Var}(Y) = \mathbb{E}[\operatorname{Var}(Y\mid X)] + \operatorname{Var}(\mathbb{E}[Y\mid X])"""
                        ),
                        CheatsheetFormula(
                            title = "Moment Generating & Characteristic Functions",
                            latex = """M_X(t) = \mathbb{E}[e^{tX}], \quad \varphi_X(t) = \mathbb{E}[e^{itX}], \quad \mathbb{E}[X^n] = M_X^{(n)}(0)"""
                        ),
                        CheatsheetFormula(
                            title = "Markov, Chebyshev & Jensen Inequalities",
                            latex = """P(X \ge a) \le \frac{\mathbb{E}[X]}{a}, \quad P(|X - \mu| \ge k\sigma) \le \frac{1}{k^2}, \quad \varphi(\mathbb{E}[X]) \le \mathbb{E}[\varphi(X)]"""
                        ),
                        CheatsheetFormula(
                            title = "Central Limit Theorem",
                            latex = """\frac{\sqrt{n}(\bar{X}_n - \mu)}{\sigma} \xrightarrow{d} \mathcal{N}(0, 1) \quad (n \to \infty,\; \sigma^2 < \infty)"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Distributions & Tail Analysis",
                    shortTitle = "Distributions",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Gaussian & Cauchy-Lorentz PDFs",
                            latex = """f_{\mathcal{N}}(x) = \frac{1}{\sigma\sqrt{2\pi}}e^{-\frac{(x-\mu)^2}{2\sigma^2}}, \quad f_{\text{Cauchy}}(x) = \frac{1}{\pi\gamma\left[1 + \left(\frac{x-x_0}{\gamma}\right)^2\right]}"""
                        ),
                        CheatsheetFormula(
                            title = "Student-t Distribution PDF",
                            latex = """f(t) = \frac{\Gamma\left(\frac{\nu+1}{2}\right)}{\sqrt{\nu\pi}\,\Gamma\left(\frac{\nu}{2}\right)}\left(1 + \frac{t^2}{\nu}\right)^{-\frac{\nu+1}{2}}"""
                        ),
                        CheatsheetFormula(
                            title = "Pareto Power-Law Survival & Density",
                            latex = """S(x) = P(X > x) = \left(\frac{x_m}{x}\right)^\alpha, \quad f(x) = \frac{\alpha x_m^\alpha}{x^{\alpha+1}} \quad (x \ge x_m)"""
                        ),
                        CheatsheetFormula(
                            title = "Hill Tail-Index Estimator",
                            latex = """\hat{\alpha}_{\text{Hill}} = \left(\frac{1}{k}\sum_{i=1}^{k} \ln\frac{X_{(n-i+1)}}{X_{(n-k)}}\right)^{-1}"""
                        ),
                        CheatsheetFormula(
                            title = "Expected Shortfall (CVaR)",
                            latex = """\operatorname{ES}_q(X) = \frac{1}{1-q}\int_q^1 \operatorname{VaR}_u(X)\,du = \mathbb{E}[X \mid X \ge \operatorname{VaR}_q(X)]"""
                        )
                    )
                )
            )
        ),
        CheatsheetTopic(
            id = "latex",
            title = "LaTeX Syntax",
            shortTitle = "LaTeX",
            symbol = "LaTeX",
            description = "Fractions, roots, sums, Greek letters, relations, arrows, delimiters, matrices, and math fonts.",
            showLatexSource = true,
            subsections = listOf(
                CheatsheetSubsection(
                    title = "Core Constructs & Scripts",
                    shortTitle = "Basics",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Subscripts & Superscripts",
                            latex = """x_i^2, \quad a_{n+1}^{k-1}, \quad e^{-x^2/2}""",
                            note = "Group multi-character exponents or subscripts inside { ... }."
                        ),
                        CheatsheetFormula(
                            title = "Fractions (Standard, Display & Inline)",
                            latex = """\frac{a}{b}, \quad \dfrac{x+1}{x-1}, \quad \tfrac{1}{2}"""
                        ),
                        CheatsheetFormula(
                            title = "Roots & Radicals",
                            latex = """\sqrt{x}, \quad \sqrt[3]{27}, \quad \sqrt[n]{a^2 + b^2}"""
                        ),
                        CheatsheetFormula(
                            title = "Binomials & Stacking",
                            latex = """\binom{n}{k}, \quad \overset{?}{=}, \quad \underset{x \to 0}{\lim}"""
                        ),
                        CheatsheetFormula(
                            title = "Horizontal Braces & Lines",
                            latex = """\overline{z}, \quad \underline{x}, \quad \overbrace{a+b+c}^{3\text{ terms}}, \quad \underbrace{1+\cdots+n}_{S_n}"""
                        ),
                        CheatsheetFormula(
                            title = "Spacing in Math Mode",
                            latex = """a\!b \;\; a\,b \;\; a\:b \;\; a\;b \quad a\quad b \qquad c""",
                            note = "Increasing width: negative (\\!), thin (\\,), medium (\\:), thick (\\;), \\quad, and \\qquad."
                        ),
                        CheatsheetFormula(
                            title = "Boxed Result",
                            latex = """\boxed{E = mc^2}"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Big Operators & Calculus",
                    shortTitle = "Operators",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Sums, Products & Coproducts",
                            latex = """\sum_{i=1}^{n} i, \quad \prod_{k=1}^{n} k, \quad \coprod_{\alpha} X_\alpha"""
                        ),
                        CheatsheetFormula(
                            title = "Integrals & Contour Integrals",
                            latex = """\int_a^b f(x)\,dx, \quad \iint_D, \quad \iiint_\Omega, \quad \oint_C"""
                        ),
                        CheatsheetFormula(
                            title = "Limits & Extrema",
                            latex = """\lim_{x \to \infty}, \quad \limsup_{n \to \infty}, \quad \sup_{x \in S}, \quad \inf_{x \in S}"""
                        ),
                        CheatsheetFormula(
                            title = "Derivatives & Partials",
                            latex = """f'(x), \quad f''(x), \quad \frac{df}{dx}, \quad \frac{\partial f}{\partial x}, \quad \nabla f"""
                        ),
                        CheatsheetFormula(
                            title = "Set Unions & Intersections",
                            latex = """\bigcup_{i=1}^{n} A_i, \quad \bigcap_{i=1}^{n} A_i, \quad \bigoplus_k V_k, \quad \bigotimes_k W_k"""
                        ),
                        CheatsheetFormula(
                            title = "Custom Operators & Text",
                            latex = """\operatorname{Tr}(A), \quad \arg\min_x, \quad \text{if } x > 0"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Greek Letters",
                    shortTitle = "Greek",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Lowercase (α – ι)",
                            latex = """\alpha, \beta, \gamma, \delta, \epsilon, \varepsilon, \zeta, \eta, \theta, \vartheta, \iota"""
                        ),
                        CheatsheetFormula(
                            title = "Lowercase (κ – ρ)",
                            latex = """\kappa, \lambda, \mu, \nu, \xi, \pi, \varpi, \rho, \varrho"""
                        ),
                        CheatsheetFormula(
                            title = "Lowercase (σ – ω)",
                            latex = """\sigma, \varsigma, \tau, \upsilon, \phi, \varphi, \chi, \psi, \omega"""
                        ),
                        CheatsheetFormula(
                            title = "Uppercase Greek",
                            latex = """\Gamma, \Delta, \Theta, \Lambda, \Xi, \Pi, \Sigma, \Upsilon, \Phi, \Psi, \Omega""",
                            note = "Capitals identical to Latin (A, B, E, Z, H, I, K, M, N, O, P, T, X) are typed as regular letters."
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Relations, Logic & Set Theory",
                    shortTitle = "Relations",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Inequalities & Comparisons",
                            latex = """\neq, \le, \ge, \ll, \gg, \approx, \equiv, \cong, \sim, \simeq, \propto"""
                        ),
                        CheatsheetFormula(
                            title = "Binary & Arithmetic Symbols",
                            latex = """\pm, \mp, \times, \div, \cdot, \ast, \star, \circ, \bullet, \oplus, \otimes, \wedge, \vee"""
                        ),
                        CheatsheetFormula(
                            title = "Set Membership & Subsets",
                            latex = """\in, \notin, \ni, \subset, \subseteq, \supset, \supseteq, \cup, \cap, \setminus, \emptyset"""
                        ),
                        CheatsheetFormula(
                            title = "Logic & Quantifiers",
                            latex = """\forall, \exists, \nexists, \neg, \land, \lor, \top, \bot, \vdash, \models, \therefore, \because"""
                        ),
                        CheatsheetFormula(
                            title = "Geometry & Misc Symbols",
                            latex = """\infty, \angle, \perp, \parallel, \triangle, \nabla, \hbar, \ell, \aleph, \Re, \Im, \dagger"""
                        ),
                        CheatsheetFormula(
                            title = "Ellipses",
                            latex = """\dots, \quad \ldots, \quad \cdots, \quad \vdots, \quad \ddots"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Arrows",
                    shortTitle = "Arrows",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Standard Arrows",
                            latex = """\to, \leftarrow, \rightarrow, \leftrightarrow, \mapsto, \longmapsto, \uparrow, \downarrow"""
                        ),
                        CheatsheetFormula(
                            title = "Implication & Double Arrows",
                            latex = """\implies, \impliedby, \iff, \Rightarrow, \Leftarrow, \Leftrightarrow, \Uparrow, \Downarrow"""
                        ),
                        CheatsheetFormula(
                            title = "Labeled & Hook Arrows",
                            latex = """\xrightarrow{f}, \xleftarrow{g}, \hookrightarrow, \twoheadrightarrow, \rightharpoonup, \rightleftharpoons"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Delimiters & Brackets",
                    shortTitle = "Delimiters",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Auto-Sizing Delimiters",
                            latex = """\left( \frac{a}{b} \right), \quad \left[ \frac{x}{y} \right], \quad \left\{ x \mid x > 0 \right\}"""
                        ),
                        CheatsheetFormula(
                            title = "Manual Sizing",
                            latex = """\big( \Big( \bigg( \Bigg( x \Bigg) \bigg) \Big) \big)"""
                        ),
                        CheatsheetFormula(
                            title = "Bars, Norms & Inner Products",
                            latex = """|x|, \quad \|v\|_2, \quad \langle u, v \rangle, \quad \lfloor x \rfloor, \quad \lceil x \rceil"""
                        ),
                        CheatsheetFormula(
                            title = "Evaluation Bar",
                            latex = """\left. \frac{df}{dx} \right|_{x=0}, \quad \left. F(x) \right|_a^b""",
                            note = "Pair \\left. or \\right. as an invisible delimiter when only one vertical bar is needed."
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Matrices, Cases & Alignment",
                    shortTitle = "Matrices",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Parenthesized & Bracketed Matrices",
                            latex = """\begin{pmatrix} a & b \\ c & d \end{pmatrix}, \quad \begin{bmatrix} 1 & 0 \\ 0 & 1 \end{bmatrix}"""
                        ),
                        CheatsheetFormula(
                            title = "Determinant & Norm Matrices",
                            latex = """\begin{vmatrix} a & b \\ c & d \end{vmatrix}, \quad \begin{Vmatrix} u \\ v \end{Vmatrix}"""
                        ),
                        CheatsheetFormula(
                            title = "Piecewise Functions",
                            latex = """f(x) = \begin{cases} x^2 & x \ge 0 \\ -x & x < 0 \end{cases}"""
                        ),
                        CheatsheetFormula(
                            title = "Aligned Equations (align)",
                            latex = """\begin{align} (a+b)^3 &= a^3 + 3a^2b + 3ab^2 + b^3 \\ &= a^3 + b^3 + 3ab(a+b) \end{align}""",
                            note = "Place & before = to align rows and \\\\ for line breaks. Use align* to omit equation numbers, or aligned inside another equation."
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Fonts, Styles & Accents",
                    shortTitle = "Fonts",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Blackboard Bold",
                            latex = """\mathbb{N}, \mathbb{Z}, \mathbb{Q}, \mathbb{R}, \mathbb{C}, \mathbb{E}, \mathbb{P}"""
                        ),
                        CheatsheetFormula(
                            title = "Calligraphic & Fraktur",
                            latex = """\mathcal{F}, \mathcal{L}, \mathcal{N}, \mathcal{O}, \quad \mathfrak{g}, \mathfrak{h}, \mathfrak{sl}_2"""
                        ),
                        CheatsheetFormula(
                            title = "Roman, Bold, Sans & Monospace",
                            latex = """\mathrm{d}x, \quad \mathbf{v}, \quad \mathsf{T}, \quad \mathtt{id}, \quad \mathit{var}"""
                        ),
                        CheatsheetFormula(
                            title = "Math Accents",
                            latex = """\hat{x}, \quad \bar{x}, \quad \tilde{f}, \quad \vec{v}, \quad \dot{x}, \quad \ddot{x}, \quad \check{x}, \quad \acute{x}"""
                        ),
                        CheatsheetFormula(
                            title = "Wide Accents & Vectors",
                            latex = """\widehat{ABC}, \quad \widetilde{f+g}, \quad \overrightarrow{AB}, \quad \overleftarrow{BA}"""
                        )
                    )
                )
            )
        )
    )

    fun topicById(id: String): CheatsheetTopic? = TOPICS.firstOrNull { it.id == id }
}
