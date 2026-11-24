package com.pallas.launcher.domain.math

import com.pallas.launcher.domain.reference.CheatsheetDomain
import com.pallas.launcher.domain.reference.CheatsheetKind
import com.pallas.launcher.domain.reference.DiscreteMathCheatsheetData
import com.pallas.launcher.domain.reference.EngineeringCheatsheetData
import com.pallas.launcher.domain.reference.GeometryFinanceCsBioData
import com.pallas.launcher.domain.reference.PhysicsCheatsheetData

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
    val showLatexSource: Boolean = false,
    val domain: CheatsheetDomain = CheatsheetDomain.MATHEMATICS,
    val kind: CheatsheetKind = CheatsheetKind.FORMULAS,
    val countLabel: String? = null,
    val aliases: List<String> = emptyList()
) {
    val formulaCount: Int
        get() = subsections.sumOf { it.formulas.size }
}

data class CheatsheetFormulaHit(
    val topic: CheatsheetTopic,
    val subsection: CheatsheetSubsection,
    val formula: CheatsheetFormula,
    val score: Int
)

private data class IndexedCheatsheetFormula(
    val topic: CheatsheetTopic,
    val subsection: CheatsheetSubsection,
    val formula: CheatsheetFormula,
    val topicLower: String,
    val shortTopicLower: String,
    val subLower: String,
    val titleLower: String,
    val noteLower: String,
    val latexLower: String
)

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
            description = "Fundamental subspaces, LU/QR/Cholesky/Spectral/SVD decompositions, projections, identities, and matrix calculus.",
            subsections = listOf(
                CheatsheetSubsection(
                    title = "The Four Fundamental Subspaces",
                    shortTitle = "Subspaces",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Rank–Nullity Theorem",
                            latex = """\dim\mathcal{C}(A) + \dim\mathcal{N}(A) = n, \quad \operatorname{rank}(A) + \operatorname{nullity}(A) = n""",
                            note = "For A ∈ ℝ^{m×n}; column space C(A) and null space N(A)."
                        ),
                        CheatsheetFormula(
                            title = "Dimensions of the Four Subspaces",
                            latex = """\dim\mathcal{C}(A) = \dim\mathcal{R}(A^T) = r, \quad \dim\mathcal{N}(A) = n - r, \quad \dim\mathcal{N}(A^T) = m - r"""
                        ),
                        CheatsheetFormula(
                            title = "Orthogonality of Fundamental Subspaces",
                            latex = """\mathcal{N}(A) \perp \mathcal{R}(A^T)\;\text{in }\mathbb{R}^n, \quad \mathcal{N}(A^T) \perp \mathcal{C}(A)\;\text{in }\mathbb{R}^m""",
                            note = "Every x ∈ ℝ^n decomposes uniquely as x = x_row + x_null with x_row ⊥ x_null."
                        ),
                        CheatsheetFormula(
                            title = "Rank & Sylvester Inequalities",
                            latex = """\operatorname{rank}(AB) \le \min(\operatorname{rank} A, \operatorname{rank} B), \quad \operatorname{rank}(A)+\operatorname{rank}(B)-n \le \operatorname{rank}(AB)"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Matrix Decompositions",
                    shortTitle = "Decompositions",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "LU Decomposition",
                            latex = """PA = LU \quad (L\text{ unit lower triangular},\; U\text{ upper triangular})""",
                            note = "Exists for any square matrix with permutation P; used for O(n^3/3) linear solves."
                        ),
                        CheatsheetFormula(
                            title = "QR Decomposition",
                            latex = """A = QR \quad (Q^T Q = I,\; R\text{ upper triangular})""",
                            note = "Exists for any m×n matrix (m ≥ n); unique when A has full column rank and R_ii > 0."
                        ),
                        CheatsheetFormula(
                            title = "Cholesky Decomposition",
                            latex = """A = L L^T \quad (A = A^T \succ 0,\; L\text{ lower triangular},\; L_{ii} > 0)""",
                            note = "Unique iff A is symmetric positive-definite; ~2× faster than LU."
                        ),
                        CheatsheetFormula(
                            title = "Spectral Decomposition",
                            latex = """A = Q \Lambda Q^T = \sum_{i=1}^n \lambda_i q_i q_i^T \quad (A = A^T,\; Q^T Q = I)""",
                            note = "Every real symmetric matrix has real eigenvalues and an orthonormal eigenbasis."
                        ),
                        CheatsheetFormula(
                            title = "Singular Value Decomposition (SVD)",
                            latex = """A = U \Sigma V^T \quad (U^T U = I,\; V^T V = I,\; \Sigma = \operatorname{diag}(\sigma_1,\dots,\sigma_r))""",
                            note = "Exists for every real or complex m×n matrix; σ_i = √λ_i(A^T A) ≥ 0."
                        ),
                        CheatsheetFormula(
                            title = "Schur Decomposition",
                            latex = """A = Q T Q^* \quad (Q^* Q = I,\; T\text{ upper triangular})""",
                            note = "Exists for every square complex matrix; diagonal of T holds the eigenvalues of A."
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Least Squares & Projections",
                    shortTitle = "Projections",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Normal Equations",
                            latex = """A^T A \hat{x} = A^T b, \quad \hat{x} = \arg\min_x \|Ax - b\|_2"""
                        ),
                        CheatsheetFormula(
                            title = "Orthogonal Projection Matrix",
                            latex = """P = A(A^T A)^{-1}A^T, \quad P^2 = P = P^T, \quad (I - P)b \perp \mathcal{C}(A)""",
                            note = "Projects b onto C(A) when A has full column rank."
                        ),
                        CheatsheetFormula(
                            title = "Moore–Penrose Pseudoinverse",
                            latex = """A^+ = V \Sigma^+ U^T, \quad A^+ = (A^T A)^{-1}A^T\;\text{(full column rank)}"""
                        ),
                        CheatsheetFormula(
                            title = "Gram–Schmidt Orthogonalization",
                            latex = """u_k = v_k - \sum_{j=1}^{k-1} \frac{\langle v_k, u_j \rangle}{\langle u_j, u_j \rangle} u_j, \quad e_k = \frac{u_k}{\|u_k\|_2}"""
                        ),
                        CheatsheetFormula(
                            title = "Tikhonov (Ridge) Regularization",
                            latex = """\hat{x}_\lambda = (A^T A + \lambda I)^{-1}A^T b = \arg\min_x \|Ax - b\|_2^2 + \lambda\|x\|_2^2"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Eigenvalues, Determinants & Identities",
                    shortTitle = "Identities",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Characteristic Equation, Trace & Determinant",
                            latex = """\det(A - \lambda I) = 0, \quad \operatorname{tr}(A) = \sum_{i=1}^n \lambda_i, \quad \det(A) = \prod_{i=1}^n \lambda_i"""
                        ),
                        CheatsheetFormula(
                            title = "Positive-Definiteness Criteria",
                            latex = """x^T A x > 0\;(\forall x \neq 0) \iff \lambda_i(A) > 0 \iff \Delta_k > 0\;(\forall k)""",
                            note = "Sylvester's criterion: symmetric A ≻ 0 iff all leading principal minors Δ_k > 0."
                        ),
                        CheatsheetFormula(
                            title = "Trace Cyclic Property & Frobenius Norm",
                            latex = """\operatorname{tr}(ABC) = \operatorname{tr}(CAB) = \operatorname{tr}(BCA), \quad \|A\|_F^2 = \operatorname{tr}(A^T A)"""
                        ),
                        CheatsheetFormula(
                            title = "Sherman–Morrison–Woodbury Inverse",
                            latex = """(A + UCV)^{-1} = A^{-1} - A^{-1}U(C^{-1} + V A^{-1} U)^{-1} V A^{-1}"""
                        ),
                        CheatsheetFormula(
                            title = "Cayley–Hamilton & Matrix Determinant Lemma",
                            latex = """p(\lambda)=\det(\lambda I - A) \implies p(A)=0, \quad \det(A + uv^T) = (1 + v^T A^{-1} u)\det(A)"""
                        ),
                        CheatsheetFormula(
                            title = "Jacobi's Formula & Matrix Exponential Determinant",
                            latex = """\det(e^A) = e^{\operatorname{tr}(A)}, \quad \frac{d}{dt}\det A(t) = \operatorname{tr}\!\left(\operatorname{adj}(A)\frac{dA}{dt}\right)"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Matrix Calculus",
                    shortTitle = "Calculus",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Gradients of Linear & Bilinear Forms",
                            latex = """\nabla_x (c^T x) = c, \quad \nabla_x (x^T A y) = Ay, \quad \nabla_y (x^T A y) = A^T x"""
                        ),
                        CheatsheetFormula(
                            title = "Gradient of Quadratic Form",
                            latex = """\nabla_x (x^T A x) = (A + A^T)x \quad (\text{and } 2Ax\text{ when } A = A^T)"""
                        ),
                        CheatsheetFormula(
                            title = "Log-Determinant & Inverse Derivatives",
                            latex = """\frac{\partial \ln\det X}{\partial X} = X^{-T}, \quad \frac{\partial X^{-1}}{\partial \alpha} = -X^{-1}\frac{\partial X}{\partial \alpha} X^{-1}"""
                        ),
                        CheatsheetFormula(
                            title = "Trace Derivatives",
                            latex = """\frac{\partial \operatorname{tr}(AX)}{\partial X} = A^T, \quad \frac{\partial \operatorname{tr}(X^T A X)}{\partial X} = (A + A^T)X"""
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
            description = "Distributions, concentration inequalities, extreme-value tails, limit laws, and transforms.",
            domain = CheatsheetDomain.MATHEMATICS,
            subsections = listOf(
                CheatsheetSubsection(
                    title = "Distributions Reference",
                    shortTitle = "Distributions",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Normal (Gaussian) Distribution",
                            latex = """f(x) = \frac{1}{\sigma\sqrt{2\pi}}e^{-\frac{(x-\mu)^2}{2\sigma^2}}, \quad \mathbb{E}[X]=\mu, \quad \operatorname{Var}(X)=\sigma^2"""
                        ),
                        CheatsheetFormula(
                            title = "Exponential Distribution",
                            latex = """f(x) = \lambda e^{-\lambda x}\;(x \ge 0), \quad \mathbb{E}[X] = \frac{1}{\lambda}, \quad \operatorname{Var}(X) = \frac{1}{\lambda^2}""",
                            note = "Memoryless continuous distribution: P(X > s + t | X > s) = P(X > t)."
                        ),
                        CheatsheetFormula(
                            title = "Poisson Distribution",
                            latex = """P(X=k) = \frac{\lambda^k e^{-\lambda}}{k!}\;(k \in \mathbb{N}_0), \quad \mathbb{E}[X] = \lambda, \quad \operatorname{Var}(X) = \lambda"""
                        ),
                        CheatsheetFormula(
                            title = "Binomial Distribution",
                            latex = """P(X=k) = \binom{n}{k}p^k(1-p)^{n-k}, \quad \mathbb{E}[X]=np, \quad \operatorname{Var}(X)=np(1-p)"""
                        ),
                        CheatsheetFormula(
                            title = "Pareto / Power-Law Distribution",
                            latex = """f(x) = \frac{\alpha x_m^\alpha}{x^{\alpha+1}}\;(x \ge x_m), \quad \mathbb{E}[X]=\frac{\alpha x_m}{\alpha-1}\;(\alpha>1), \quad \operatorname{Var}(X)=\frac{\alpha x_m^2}{(\alpha-1)^2(\alpha-2)}\;(\alpha>2)""",
                            note = "Moments diverge in the tail: mean is ∞ for α ≤ 1; variance is ∞ for α ≤ 2."
                        ),
                        CheatsheetFormula(
                            title = "Cauchy–Lorentz Distribution",
                            latex = """f(x) = \frac{1}{\pi\gamma\left[1 + \left(\frac{x-x_0}{\gamma}\right)^2\right]}, \quad \mathbb{E}[X]\text{ undefined}, \quad \operatorname{Var}(X)=\infty""",
                            note = "All moments of order ≥ 1 diverge; the sample mean has the exact same Cauchy distribution."
                        ),
                        CheatsheetFormula(
                            title = "Student-t Distribution",
                            latex = """f(t) = \frac{\Gamma\left(\frac{\nu+1}{2}\right)}{\sqrt{\nu\pi}\,\Gamma\left(\frac{\nu}{2}\right)}\left(1 + \frac{t^2}{\nu}\right)^{-\frac{\nu+1}{2}}, \quad \mathbb{E}[T]=0\;(\nu>1), \quad \operatorname{Var}(T)=\frac{\nu}{\nu-2}\;(\nu>2)""",
                            note = "Moments diverge for k ≥ ν: mean diverges for ν ≤ 1 (Cauchy); variance diverges for ν ≤ 2."
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Inequalities & Bounds",
                    shortTitle = "Inequalities",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Markov's Inequality",
                            latex = """P(X \ge a) \le \frac{\mathbb{E}[X]}{a} \quad (X \ge 0,\; a > 0)"""
                        ),
                        CheatsheetFormula(
                            title = "Chebyshev's Inequality",
                            latex = """P(|X - \mu| \ge k\sigma) \le \frac{1}{k^2} \quad (\sigma^2 < \infty)"""
                        ),
                        CheatsheetFormula(
                            title = "Chernoff Bound",
                            latex = """P(X \ge a) \le \inf_{t > 0} e^{-ta}\,\mathbb{E}[e^{tX}] = \inf_{t > 0} e^{-ta} M_X(t)"""
                        ),
                        CheatsheetFormula(
                            title = "Jensen's Inequality",
                            latex = """\varphi(\mathbb{E}[X]) \le \mathbb{E}[\varphi(X)] \quad (\varphi\text{ convex; reverses if concave})""",
                            note = "Core of convexity & risk: volatility boosts convex payoffs and penalizes concave utility."
                        ),
                        CheatsheetFormula(
                            title = "Cauchy–Schwarz Inequality",
                            latex = """\left(\mathbb{E}[XY]\right)^2 \le \mathbb{E}[X^2]\,\mathbb{E}[Y^2], \quad |\operatorname{Cov}(X,Y)| \le \sigma_X \sigma_Y"""
                        ),
                        CheatsheetFormula(
                            title = "Hoeffding's Inequality",
                            latex = """P(\bar{X}_n - \mathbb{E}[\bar{X}_n] \ge t) \le \exp\!\left(-\frac{2n^2 t^2}{\sum_{i=1}^n (b_i - a_i)^2}\right) \quad (X_i \in [a_i, b_i])"""
                        )
                    )
                ),
                CheatsheetSubsection(
                    title = "Extreme Value & Tail Behavior",
                    shortTitle = "Tails & Limits",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Subexponential vs. Power-Law Tail Decay",
                            latex = """\lim_{x\to\infty}\frac{P(X_1+X_2>x)}{P(X_1>x)} = 2\;\text{(subexp.)}, \quad P(X>x) \sim L(x)x^{-\alpha}\;\text{(power-law)}""",
                            note = "In subexponential/heavy tails, a sum's extreme event is dominated by a single maximum jump."
                        ),
                        CheatsheetFormula(
                            title = "Law of Large Numbers (WLLN & SLLN)",
                            latex = """\bar{X}_n \xrightarrow{P} \mu\;\text{(Weak)}, \quad P\!\left(\lim_{n\to\infty}\bar{X}_n = \mu\right)=1\;\text{(Strong, }\mathbb{E}|X|<\infty\text{)}"""
                        ),
                        CheatsheetFormula(
                            title = "Classical Central Limit Theorem",
                            latex = """\frac{\sqrt{n}(\bar{X}_n - \mu)}{\sigma} \xrightarrow{d} \mathcal{N}(0, 1) \quad (n \to \infty,\; \sigma^2 < \infty)"""
                        ),
                        CheatsheetFormula(
                            title = "Generalized CLT & Stable Distributions",
                            latex = """\frac{\sum_{i=1}^n X_i - b_n}{a_n} \xrightarrow{d} S_\alpha(\beta, \gamma, \delta) \quad (\alpha \in (0, 2])""",
                            note = "When variance is infinite (α < 2), normalized i.i.d. sums converge to α-stable laws instead of Gaussian."
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
                ),
                CheatsheetSubsection(
                    title = "Foundations & Transforms",
                    shortTitle = "Transforms",
                    formulas = listOf(
                        CheatsheetFormula(
                            title = "Bayes' Theorem",
                            latex = """P(A\mid B) = \frac{P(B\mid A)P(A)}{P(B)} = \frac{P(B\mid A)P(A)}{\sum_i P(B\mid A_i)P(A_i)}"""
                        ),
                        CheatsheetFormula(
                            title = "Law of Total Expectation & Variance",
                            latex = """\mathbb{E}[Y] = \mathbb{E}[\mathbb{E}[Y\mid X]], \quad \operatorname{Var}(Y) = \mathbb{E}[\operatorname{Var}(Y\mid X)] + \operatorname{Var}(\mathbb{E}[Y\mid X])"""
                        ),
                        CheatsheetFormula(
                            title = "Moment Generating Function (MGF)",
                            latex = """M_X(t) = \mathbb{E}[e^{tX}], \quad \mathbb{E}[X^n] = M_X^{(n)}(0), \quad M_{X+Y}(t) = M_X(t)M_Y(t)""",
                            note = "Requires exponential tail decay; diverges for heavy-tailed laws like Pareto, Cauchy, and Student-t."
                        ),
                        CheatsheetFormula(
                            title = "Characteristic Function",
                            latex = """\varphi_X(t) = \mathbb{E}[e^{itX}], \quad \varphi_{aX+b}(t) = e^{itb}\varphi_X(at)""",
                            note = "Always exists and uniquely determines the distribution, even when all moments and MGF diverge."
                        ),
                        CheatsheetFormula(
                            title = "Cumulant Generating Function",
                            latex = """K_X(t) = \ln M_X(t) = \sum_{n=1}^{\infty} \kappa_n \frac{t^n}{n!}, \quad \kappa_1 = \mu,\; \kappa_2 = \sigma^2"""
                        )
                    )
                )
            )
        ),
        GeometryFinanceCsBioData.GEOMETRY_TRIG_TOPIC,
        DiscreteMathCheatsheetData.DISCRETE_MATH_TOPIC,
        PhysicsCheatsheetData.PHYSICS_TOPIC,
        CheatsheetTopic(
            id = "periodic_table",
            title = "Periodic Table of Elements",
            shortTitle = "Periodic Table",
            symbol = "Z",
            description = "Interactive 18-column periodic table of all 118 elements with search and full atomic property cards.",
            subsections = emptyList(),
            domain = CheatsheetDomain.SCIENCES,
            kind = CheatsheetKind.PERIODIC_TABLE,
            countLabel = "118"
        ),
        GeometryFinanceCsBioData.BIOLOGY_PHYSIOLOGY_TOPIC,
        EngineeringCheatsheetData.ENGINEERING_TOPIC,
        GeometryFinanceCsBioData.FINANCE_RISK_TOPIC,
        GeometryFinanceCsBioData.INFORMATION_THEORY_TOPIC,
        CheatsheetTopic(
            id = "latex",
            title = "LaTeX Syntax",
            shortTitle = "LaTeX",
            symbol = "LaTeX",
            description = "Fractions, roots, sums, Greek letters, relations, arrows, delimiters, matrices, and math fonts.",
            showLatexSource = true,
            domain = CheatsheetDomain.ENGINEERING_QUANT,
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

    val TOPIC_ALIASES: Map<String, List<String>> = mapOf(
        "calculus" to listOf("calc", "integrals", "integral", "derivatives", "odes", "ode", "series", "taylor", "fourier", "laplace", "vector calculus"),
        "linear_algebra" to listOf("la", "linalg", "linear", "matrix", "matrices", "svd", "eigen", "eigenvalues", "subspaces"),
        "probability_statistics" to listOf("stats", "stat", "prob", "probability", "distributions", "tails", "inequalities", "clt"),
        "geometry_trig" to listOf("geo", "geom", "geometry", "trig", "trigonometry", "conics"),
        "discrete_math" to listOf("discrete", "combinatorics", "graphs", "graph", "number theory", "logic", "sets"),
        "physics" to listOf("phys", "mechanics", "em", "electromagnetism", "thermo", "thermodynamics", "quantum", "relativity", "constants"),
        "periodic_table" to listOf("periodic", "elements", "chem", "chemistry", "ptable"),
        "biology_physiology" to listOf("bio", "biology", "physiology", "anatomy"),
        "engineering" to listOf("eng", "mech", "fluids", "beams", "materials", "circuits", "signals"),
        "finance_economics" to listOf("fin", "finance", "econ", "economics", "risk", "kelly", "tvm"),
        "information_theory" to listOf("info", "info theory", "information", "shannon", "entropy", "coding"),
        "latex" to listOf("tex", "latex syntax", "symbols")
    )

    fun aliasesFor(topic: CheatsheetTopic): List<String> =
        (topic.aliases + TOPIC_ALIASES[topic.id].orEmpty()).distinct()

    fun topicById(id: String): CheatsheetTopic? = TOPICS.firstOrNull { it.id == id }

    private val searchIndex: List<IndexedCheatsheetFormula> by lazy {
        buildList {
            TOPICS.forEach { topic ->
                val topicLower = topic.title.lowercase()
                val shortLower = topic.shortTitle.lowercase()
                topic.subsections.forEach { sub ->
                    val subLower = sub.title.lowercase()
                    sub.formulas.forEach { formula ->
                        add(
                            IndexedCheatsheetFormula(
                                topic = topic,
                                subsection = sub,
                                formula = formula,
                                topicLower = topicLower,
                                shortTopicLower = shortLower,
                                subLower = subLower,
                                titleLower = formula.title.lowercase(),
                                noteLower = formula.note.orEmpty().lowercase(),
                                latexLower = formula.latex.lowercase()
                            )
                        )
                    }
                }
            }
        }
    }

    fun searchFormulas(rawQuery: String): List<CheatsheetFormulaHit> {
        val tokens = rawQuery.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()

        return searchIndex.mapNotNull { item ->
            var totalScore = 0
            for (tok in tokens) {
                val tokScore = when {
                    item.titleLower == tok -> 100
                    item.titleLower.startsWith(tok) -> 75
                    item.titleLower.contains(tok) -> 55
                    item.noteLower.contains(tok) -> 40
                    item.subLower.contains(tok) -> 25
                    item.topicLower.contains(tok) || item.shortTopicLower.contains(tok) -> 15
                    item.latexLower.contains(tok) -> 10
                    else -> return@mapNotNull null
                }
                totalScore += tokScore
            }
            CheatsheetFormulaHit(item.topic, item.subsection, item.formula, totalScore)
        }.sortedByDescending { it.score }
    }
}
