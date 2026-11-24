package com.pallas.launcher.domain.reference

import com.pallas.launcher.domain.math.CheatsheetFormula
import com.pallas.launcher.domain.math.CheatsheetSubsection
import com.pallas.launcher.domain.math.CheatsheetTopic

object PhysicsCheatsheetData {

    val PHYSICS_TOPIC: CheatsheetTopic = CheatsheetTopic(
        id = "physics",
        title = "Physics",
        shortTitle = "Physics",
        symbol = "ℏ",
        description = "Classical mechanics, electromagnetism, thermodynamics, waves & optics, relativity, quantum mechanics, and physical constants.",
        domain = CheatsheetDomain.SCIENCES,
        kind = CheatsheetKind.PHYSICS,
        subsections = listOf(
            CheatsheetSubsection(
                title = "Mechanics & Gravitation",
                shortTitle = "Mechanics",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Newton's Second Law & Impulse",
                        latex = """\vec{F} = \frac{d\vec{p}}{dt} = m\vec{a} \quad (\text{constant } m), \quad \vec{J} = \int_{t_1}^{t_2} \vec{F}\,dt = \Delta\vec{p}""",
                        note = "Force equals the rate of change of linear momentum p = mv."
                    ),
                    CheatsheetFormula(
                        title = "Work-Energy Theorem & Mechanical Energy",
                        latex = """W = \int \vec{F} \cdot d\vec{r} = \Delta K, \quad K = \frac{1}{2}mv^2, \quad E = K + U""",
                        note = "For a conservative force F = −∇U, total mechanical energy E = K + U is conserved."
                    ),
                    CheatsheetFormula(
                        title = "Angular Momentum & Torque",
                        latex = """\vec{\tau} = \vec{r} \times \vec{F} = \frac{d\vec{L}}{dt}, \quad \vec{L} = \vec{r} \times \vec{p}, \quad L_z = I\omega""",
                        note = "Angular momentum is conserved when net external torque vanishes."
                    ),
                    CheatsheetFormula(
                        title = "Moment of Inertia & Parallel Axis Theorem",
                        latex = """I = \sum_i m_i r_i^2 = \int r^2\,dm, \quad I = I_{\text{cm}} + Md^2""",
                        note = "Shifts rotational inertia by Md² for an axis parallel to the center-of-mass axis at distance d."
                    ),
                    CheatsheetFormula(
                        title = "Centripetal Acceleration & Kepler's Third Law",
                        latex = """a_c = \frac{v^2}{r} = \omega^2 r, \quad T^2 = \frac{4\pi^2}{GM}a^3""",
                        note = "a is the semi-major axis of a bound orbit around central mass M (for M ≫ m)."
                    ),
                    CheatsheetFormula(
                        title = "Newton's Law of Gravitation & Escape Velocity",
                        latex = """\vec{F}_g = -\frac{GMm}{r^2}\hat{r}, \quad v_{\text{esc}} = \sqrt{\frac{2GM}{R}}""",
                        note = "Escape speed corresponds to zero total mechanical energy (K + U = 0) at infinity."
                    ),
                    CheatsheetFormula(
                        title = "Gravitational Potential Energy & Field",
                        latex = """U(r) = -\frac{GMm}{r}, \quad \Phi(r) = -\frac{GM}{r}, \quad \vec{g} = -\nabla\Phi""",
                        note = "Outside a spherically symmetric mass, gravity matches a point mass M at the center."
                    ),
                    CheatsheetFormula(
                        title = "Simple Harmonic Oscillator",
                        latex = """\ddot{x} + \omega_0^2 x = 0, \quad \omega_0 = \sqrt{\frac{k}{m}}, \quad T = 2\pi\sqrt{\frac{m}{k}}, \quad E = \frac{1}{2}kA^2""",
                        note = "For a simple pendulum of length L at small angles, ω₀ = √(g/L)."
                    ),
                    CheatsheetFormula(
                        title = "Damped Driven Resonance & Quality Factor",
                        latex = """\ddot{x} + \gamma\dot{x} + \omega_0^2 x = \frac{F_0}{m}\cos(\omega t), \quad A(\omega) = \frac{F_0 / m}{\sqrt{(\omega_0^2 - \omega^2)^2 + (\gamma\omega)^2}}, \quad Q = \frac{\omega_0}{\gamma}""",
                        note = "Resonant amplitude peaks near ω = ω₀; Q measures radians of oscillation per 1/e energy decay."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Electromagnetism",
                shortTitle = "E&M",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Maxwell's Equations (Differential Form)",
                        latex = """\begin{aligned} \nabla \cdot \mathbf{E} &= \frac{\rho}{\varepsilon_0} \\ \nabla \cdot \mathbf{B} &= 0 \\ \nabla \times \mathbf{E} &= -\frac{\partial \mathbf{B}}{\partial t} \\ \nabla \times \mathbf{B} &= \mu_0\mathbf{J} + \mu_0\varepsilon_0\frac{\partial \mathbf{E}}{\partial t} \end{aligned}""",
                        note = "Gauss's law, Gauss's law for magnetism, Faraday's law, and the Ampère-Maxwell law."
                    ),
                    CheatsheetFormula(
                        title = "Maxwell's Equations (Integral Form)",
                        latex = """\begin{aligned} \oint_S \mathbf{E} \cdot d\mathbf{A} &= \frac{Q_{\text{enc}}}{\varepsilon_0} \\ \oint_S \mathbf{B} \cdot d\mathbf{A} &= 0 \\ \oint_C \mathbf{E} \cdot d\mathbf{l} &= -\frac{d\Phi_B}{dt} \\ \oint_C \mathbf{B} \cdot d\mathbf{l} &= \mu_0 I_{\text{enc}} + \mu_0\varepsilon_0\frac{d\Phi_E}{dt} \end{aligned}""",
                        note = "Fluxes Φ_E = ∫ E·dA and Φ_B = ∫ B·dA span any surface bounded by closed contour C."
                    ),
                    CheatsheetFormula(
                        title = "Lorentz Force Law",
                        latex = """\mathbf{F} = q(\mathbf{E} + \mathbf{v} \times \mathbf{B}), \quad d\mathbf{F} = I\,d\mathbf{l} \times \mathbf{B}""",
                        note = "The magnetic force performs zero work on a free point charge because (v × B)·v = 0."
                    ),
                    CheatsheetFormula(
                        title = "Poynting Vector & Electromagnetic Energy Density",
                        latex = """\mathbf{S} = \frac{1}{\mu_0}(\mathbf{E} \times \mathbf{B}), \quad u = \frac{1}{2}\left(\varepsilon_0 E^2 + \frac{B^2}{\mu_0}\right), \quad \frac{\partial u}{\partial t} + \nabla \cdot \mathbf{S} = -\mathbf{J}\cdot\mathbf{E}""",
                        note = "S gives directional energy flux (W/m²); field energy loss balances Joule heating J·E."
                    ),
                    CheatsheetFormula(
                        title = "Charge Continuity Equation",
                        latex = """\nabla \cdot \mathbf{J} + \frac{\partial \rho}{\partial t} = 0""",
                        note = "Local conservation of electric charge; follows directly from the divergence of Ampère-Maxwell."
                    ),
                    CheatsheetFormula(
                        title = "Electromagnetic Wave Equation & Speed of Light",
                        latex = """\nabla^2 \mathbf{E} = \mu_0\varepsilon_0\frac{\partial^2 \mathbf{E}}{\partial t^2}, \quad \nabla^2 \mathbf{B} = \mu_0\varepsilon_0\frac{\partial^2 \mathbf{B}}{\partial t^2}, \quad c = \frac{1}{\sqrt{\varepsilon_0\mu_0}}""",
                        note = "In vacuum (ρ = 0, J = 0), E and B propagate as transverse waves at speed c."
                    ),
                    CheatsheetFormula(
                        title = "Coulomb's & Biot-Savart Laws",
                        latex = """\mathbf{E}(\mathbf{r}) = \frac{1}{4\pi\varepsilon_0}\int \frac{\rho(\mathbf{r}')\,\hat{\mathbf{R}}}{R^2}\,dV', \quad \mathbf{B}(\mathbf{r}) = \frac{\mu_0}{4\pi}\int \frac{I\,d\mathbf{l}' \times \hat{\mathbf{R}}}{R^2}""",
                        note = "Static field integrals where R = r − r' points from the source element to the field point."
                    ),
                    CheatsheetFormula(
                        title = "Capacitance, Inductance & Stored Energy",
                        latex = """C = \frac{Q}{V}, \quad U_C = \frac{1}{2}CV^2 = \frac{Q^2}{2C}, \quad \mathcal{E} = -L\frac{dI}{dt}, \quad U_L = \frac{1}{2}LI^2""",
                        note = "Parallel-plate capacitor: C = ε₀A/d; long solenoid: L = μ₀n²Al."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Thermodynamics & Statistical Mechanics",
                shortTitle = "Thermo",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "First Law of Thermodynamics",
                        latex = """dU = \delta Q - \delta W, \quad \delta W = P\,dV \quad (\text{quasistatic})""",
                        note = "Internal energy U is a state function; heat δQ and work δW are path-dependent."
                    ),
                    CheatsheetFormula(
                        title = "Second Law & Fundamental Thermodynamic Identity",
                        latex = """dS \ge \frac{\delta Q}{T}, \quad dU = T\,dS - P\,dV + \mu\,dN""",
                        note = "Total entropy of an isolated system never decreases (ΔS_total ≥ 0)."
                    ),
                    CheatsheetFormula(
                        title = "Boltzmann & Gibbs Entropy Formulas",
                        latex = """S = k_B \ln \Omega, \quad S = -k_B \sum_i p_i \ln p_i""",
                        note = "Ω is the number of accessible microstates; Gibbs form holds for general probability distributions."
                    ),
                    CheatsheetFormula(
                        title = "Canonical Partition Function & Boltzmann Factor",
                        latex = """P_i = \frac{e^{-\beta E_i}}{Z}, \quad Z = \sum_i e^{-\beta E_i}, \quad \beta = \frac{1}{k_B T}, \quad \langle E \rangle = -\frac{\partial \ln Z}{\partial \beta}""",
                        note = "Describes a system in thermal equilibrium with a reservoir at temperature T."
                    ),
                    CheatsheetFormula(
                        title = "Helmholtz & Gibbs Free Energies",
                        latex = """F = U - TS = -k_B T \ln Z, \quad G = H - TS = U + PV - TS""",
                        note = "F is minimized at constant (T, V); G is minimized at constant (T, P)."
                    ),
                    CheatsheetFormula(
                        title = "Ideal Gas Law & Equipartition Theorem",
                        latex = """PV = N k_B T = nRT, \quad \langle E_{\text{quad}} \rangle = \frac{1}{2}k_B T""",
                        note = "Each quadratic degree of freedom contributes ½k_BT on average; monatomic ideal gas has U = (3/2)Nk_BT."
                    ),
                    CheatsheetFormula(
                        title = "Carnot Efficiency & Refrigerator COP",
                        latex = """\eta_{\text{Carnot}} = 1 - \frac{T_C}{T_H}, \quad \text{COP}_{\text{fridge}} = \frac{T_C}{T_H - T_C}""",
                        note = "Maximum reversible efficiency between cold and hot reservoirs at absolute temperatures T_C, T_H (K)."
                    ),
                    CheatsheetFormula(
                        title = "Maxwell-Boltzmann Speed Distribution & RMS Speed",
                        latex = """f(v) = 4\pi \left(\frac{m}{2\pi k_B T}\right)^{3/2} v^2 e^{-mv^2/(2k_B T)}, \quad v_{\text{rms}} = \sqrt{\frac{3k_B T}{m}}, \quad \bar{v} = \sqrt{\frac{8k_B T}{\pi m}}""",
                        note = "Most probable speed is v_p = √(2k_B T / m)."
                    ),
                    CheatsheetFormula(
                        title = "Stefan-Boltzmann & Planck Radiation Laws",
                        latex = """\frac{P}{A} = \sigma T^4, \quad B_\nu(\nu, T) = \frac{2h\nu^3}{c^2}\frac{1}{e^{h\nu/(k_B T)} - 1}, \quad \lambda_{\max} T = b""",
                        note = "Total blackbody radiant exitance (σ ≈ 5.67×10⁻⁸ W/(m²·K⁴)) and Wien's constant b ≈ 2.898 mm·K."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Waves & Optics",
                shortTitle = "Optics",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Classical 1D Wave Equation & Harmonic Wave",
                        latex = """\frac{\partial^2 y}{\partial t^2} = v^2 \frac{\partial^2 y}{\partial x^2}, \quad y(x,t) = A\cos(kx - \omega t + \phi), \quad v = \lambda f = \frac{\omega}{k}""",
                        note = "On a string with tension T and linear mass density μ, wave speed is v = √(T/μ)."
                    ),
                    CheatsheetFormula(
                        title = "Dispersion, Phase & Group Velocity",
                        latex = """v_p = \frac{\omega(k)}{k}, \quad v_g = \frac{d\omega}{dk} = v_p - \lambda\frac{dv_p}{d\lambda}""",
                        note = "Wave packets and energy/information propagate at the group velocity v_g in dispersive media."
                    ),
                    CheatsheetFormula(
                        title = "Classical & Relativistic Doppler Shifts",
                        latex = """f_{\text{cl}} = f_0 \frac{v \pm v_o}{v \mp v_s}, \quad f_{\text{rel}} = f_0 \sqrt{\frac{1 + \beta}{1 - \beta}} \quad (\text{approaching},\; \beta = v/c)""",
                        note = "Upper signs for approach, lower for recession; relativistic formula is exact for light in vacuum."
                    ),
                    CheatsheetFormula(
                        title = "Snell's Law, Critical Angle & Brewster's Angle",
                        latex = """n_1 \sin\theta_1 = n_2 \sin\theta_2, \quad \sin\theta_c = \frac{n_2}{n_1}\;(n_1 > n_2), \quad \tan\theta_B = \frac{n_2}{n_1}""",
                        note = "Total internal reflection occurs for θ₁ > θ_c; at θ_B, reflected p-polarized light vanishes."
                    ),
                    CheatsheetFormula(
                        title = "Thin Lens & Spherical Mirror Equation",
                        latex = """\frac{1}{f} = \frac{1}{d_o} + \frac{1}{d_i} = (n - 1)\left(\frac{1}{R_1} - \frac{1}{R_2}\right), \quad M = -\frac{d_i}{d_o}""",
                        note = "Combines thin-lens/mirror imaging with the lensmaker's equation in air and magnification M."
                    ),
                    CheatsheetFormula(
                        title = "Double-Slit Interference & Diffraction Grating",
                        latex = """d\sin\theta = m\lambda \quad (\text{bright maxima}), \quad a\sin\theta = m\lambda \quad (\text{single-slit minima},\; m \neq 0)""",
                        note = "d is slit/grating period, a is single-slit width, and m is an integer order."
                    ),
                    CheatsheetFormula(
                        title = "Rayleigh Resolution Criterion",
                        latex = """\theta_{\min} \approx 1.22\frac{\lambda}{D}""",
                        note = "Minimum angular separation resolvable by a diffraction-limited circular aperture of diameter D."
                    ),
                    CheatsheetFormula(
                        title = "Standing Waves & Sound Intensity Level",
                        latex = """f_n = \frac{nv}{2L} \quad (\text{both ends fixed/open}), \quad L_{\text{dB}} = 10\log_{10}\!\left(\frac{I}{I_0}\right), \quad I_0 = 10^{-12}\text{ W/m}^2""",
                        note = "For a pipe closed at one end, only odd harmonics appear: f_n = nv/(4L) with n = 1, 3, 5, ..."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Special Relativity",
                shortTitle = "Relativity",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Lorentz Factor",
                        latex = """\gamma = \frac{1}{\sqrt{1 - \beta^2}}, \quad \beta = \frac{v}{c}, \quad \gamma \ge 1""",
                        note = "Governs relativistic time dilation, length contraction, and energy-momentum scaling."
                    ),
                    CheatsheetFormula(
                        title = "Lorentz Coordinate Transformations",
                        latex = """x' = \gamma(x - vt), \quad t' = \gamma\left(t - \frac{vx}{c^2}\right), \quad y' = y, \quad z' = z""",
                        note = "Standard boost along +x at speed v; the inverse boost replaces v with −v."
                    ),
                    CheatsheetFormula(
                        title = "Time Dilation & Length Contraction",
                        latex = """\Delta t = \gamma\,\Delta\tau, \quad L = \frac{L_0}{\gamma}""",
                        note = "Δτ is proper time in the rest frame of the clock; L₀ is proper rest length."
                    ),
                    CheatsheetFormula(
                        title = "Relativistic Velocity Addition",
                        latex = """u_x = \frac{u_x' + v}{1 + u_x' v / c^2}, \quad u_\perp = \frac{u_\perp'}{\gamma(1 + u_x' v / c^2)}""",
                        note = "Guarantees that composing any two subluminal speeds never exceeds c."
                    ),
                    CheatsheetFormula(
                        title = "Relativistic Energy-Momentum Relation",
                        latex = """E^2 = (pc)^2 + (m_0 c^2)^2, \quad E = \gamma m_0 c^2, \quad \vec{p} = \gamma m_0 \vec{v}""",
                        note = "For massless particles such as photons (m₀ = 0), E = pc."
                    ),
                    CheatsheetFormula(
                        title = "Spacetime Invariant Interval",
                        latex = """ds^2 = c^2 dt^2 - dx^2 - dy^2 - dz^2 = c^2 d\tau^2""",
                        note = "Lorentz invariant; ds² > 0 is timelike, ds² = 0 is lightlike (null), and ds² < 0 is spacelike."
                    ),
                    CheatsheetFormula(
                        title = "Four-Momentum & Relativistic Kinetic Energy",
                        latex = """P^\mu = \left(\frac{E}{c}, \vec{p}\right), \quad P_\mu P^\mu = m_0^2 c^2, \quad K = (\gamma - 1)m_0 c^2""",
                        note = "K reduces to ½m₀v² in the non-relativistic limit v ≪ c."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Quantum Mechanics",
                shortTitle = "Quantum",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Planck-Einstein & de Broglie Relations",
                        latex = """E = h\nu = \hbar\omega, \quad p = \frac{h}{\lambda} = \hbar k, \quad \lambda_{\text{dB}} = \frac{h}{p}""",
                        note = "Unifies particle energy-momentum (E, p) with wave frequency and wavevector (ω, k)."
                    ),
                    CheatsheetFormula(
                        title = "Heisenberg Uncertainty Principle",
                        latex = """\Delta x\,\Delta p_x \ge \frac{\hbar}{2}, \quad \Delta E\,\Delta t \ge \frac{\hbar}{2}""",
                        note = "Standard-deviation bound following from [x̂, p̂] = iℏ via the Robertson-Schrödinger inequality."
                    ),
                    CheatsheetFormula(
                        title = "Schrödinger Equations (Time-Dependent & Stationary)",
                        latex = """i\hbar\frac{\partial \Psi}{\partial t} = \hat{H}\Psi = \left(-\frac{\hbar^2}{2m}\nabla^2 + V(\mathbf{r}, t)\right)\Psi, \quad \hat{H}\psi_n = E_n\psi_n""",
                        note = "Born rule: |Ψ(r, t)|² dV gives the probability of detecting the particle in volume dV."
                    ),
                    CheatsheetFormula(
                        title = "Particle in a 1D Infinite Square Well",
                        latex = """E_n = \frac{n^2 \pi^2 \hbar^2}{2mL^2} = \frac{n^2 h^2}{8mL^2}, \quad \psi_n(x) = \sqrt{\frac{2}{L}}\sin\!\left(\frac{n\pi x}{L}\right) \quad (n = 1, 2, 3, \dots)""",
                        note = "Nonzero ground-state energy E₁ > 0 is a direct consequence of quantum confinement."
                    ),
                    CheatsheetFormula(
                        title = "Quantum Harmonic Oscillator",
                        latex = """E_n = \hbar\omega\left(n + \frac{1}{2}\right) \quad (n = 0, 1, 2, \dots), \quad \hat{H} = \hbar\omega\left(\hat{a}^\dagger \hat{a} + \frac{1}{2}\right)""",
                        note = "Ladder operators satisfy [â, â†] = 1 with zero-point energy ½ℏω."
                    ),
                    CheatsheetFormula(
                        title = "Hydrogen Energy Levels & Bohr Radius",
                        latex = """E_n = -\frac{13.6\text{ eV}}{n^2} = -\frac{m_e e^4}{2(4\pi\varepsilon_0)^2\hbar^2 n^2}, \quad a_0 = \frac{4\pi\varepsilon_0\hbar^2}{m_e e^2} \approx 5.29 \times 10^{-11}\text{ m}""",
                        note = "Bohr radius a₀ ≈ 0.529 Å sets the characteristic atomic length scale."
                    ),
                    CheatsheetFormula(
                        title = "Canonical Commutation & Ehrenfest's Theorem",
                        latex = """[\hat{x}_j, \hat{p}_k] = i\hbar\delta_{jk}, \quad [\hat{L}_j, \hat{L}_k] = i\hbar\varepsilon_{jkl}\hat{L}_l, \quad \frac{d\langle\hat{A}\rangle}{dt} = \frac{i}{\hbar}\langle[\hat{H},\hat{A}]\rangle + \left\langle\frac{\partial \hat{A}}{\partial t}\right\rangle""",
                        note = "Links quantum operator commutators directly to classical Hamiltonian evolution."
                    ),
                    CheatsheetFormula(
                        title = "Fermi's Golden Rule",
                        latex = """\Gamma_{i \to f} = \frac{2\pi}{\hbar}\left|\langle f | \hat{H}' | i \rangle\right|^2 \rho(E_f)""",
                        note = "First-order transition rate from state |i⟩ to final states |f⟩ with density of states ρ(E_f)."
                    )
                )
            )
        )
    )

    val PHYSICAL_CONSTANTS: List<PhysicalConstant> = listOf(
        PhysicalConstant(
            name = "Speed of light in vacuum",
            symbolLatex = "c",
            value = "299,792,458",
            unit = "m/s",
            note = "Exact by SI definition."
        ),
        PhysicalConstant(
            name = "Planck constant",
            symbolLatex = "h",
            value = "6.62607015 × 10⁻³⁴",
            unit = "J·s",
            note = "Exact by 2019 SI redefinition."
        ),
        PhysicalConstant(
            name = "Reduced Planck constant",
            symbolLatex = """\hbar""",
            value = "1.054571817... × 10⁻³⁴",
            unit = "J·s",
            note = "Exact: h / (2π)."
        ),
        PhysicalConstant(
            name = "Newtonian constant of gravitation",
            symbolLatex = "G",
            value = "6.67430(15) × 10⁻¹¹",
            unit = "m³/(kg·s²)",
            note = "CODATA 2022 recommended value."
        ),
        PhysicalConstant(
            name = "Boltzmann constant",
            symbolLatex = "k_B",
            value = "1.380649 × 10⁻²³",
            unit = "J/K",
            note = "Exact by 2019 SI redefinition."
        ),
        PhysicalConstant(
            name = "Avogadro constant",
            symbolLatex = "N_A",
            value = "6.02214076 × 10²³",
            unit = "mol⁻¹",
            note = "Exact by 2019 SI redefinition."
        ),
        PhysicalConstant(
            name = "Molar gas constant",
            symbolLatex = "R",
            value = "8.314462618...",
            unit = "J/(mol·K)",
            note = "Exact: N_A · k_B."
        ),
        PhysicalConstant(
            name = "Elementary charge",
            symbolLatex = "e",
            value = "1.602176634 × 10⁻¹⁹",
            unit = "C",
            note = "Exact by 2019 SI redefinition."
        ),
        PhysicalConstant(
            name = "Vacuum electric permittivity",
            symbolLatex = """\varepsilon_0""",
            value = "8.8541878188(14) × 10⁻¹²",
            unit = "F/m",
            note = "CODATA 2022; satisfies ε₀μ₀c² = 1."
        ),
        PhysicalConstant(
            name = "Vacuum magnetic permeability",
            symbolLatex = """\mu_0""",
            value = "1.25663706127(20) × 10⁻⁶",
            unit = "N/A²",
            note = "CODATA 2022; ≈ 4π × 10⁻⁷ N/A²."
        ),
        PhysicalConstant(
            name = "Coulomb constant",
            symbolLatex = "k_e",
            value = "8.9875517923(14) × 10⁹",
            unit = "N·m²/C²",
            note = "Electrostatic constant 1 / (4πε₀)."
        ),
        PhysicalConstant(
            name = "Electron mass",
            symbolLatex = "m_e",
            value = "9.1093837139(28) × 10⁻³¹",
            unit = "kg",
            note = "CODATA 2022; ≈ 0.51099895 MeV/c²."
        ),
        PhysicalConstant(
            name = "Proton mass",
            symbolLatex = "m_p",
            value = "1.67262192595(52) × 10⁻²⁷",
            unit = "kg",
            note = "CODATA 2022; ≈ 938.27209 MeV/c²."
        ),
        PhysicalConstant(
            name = "Neutron mass",
            symbolLatex = "m_n",
            value = "1.67492750056(85) × 10⁻²⁷",
            unit = "kg",
            note = "CODATA 2022; ≈ 939.56542 MeV/c²."
        ),
        PhysicalConstant(
            name = "Unified atomic mass unit",
            symbolLatex = "u",
            value = "1.66053906892(52) × 10⁻²⁷",
            unit = "kg",
            note = "1/12 of an unbound ¹²C atom mass; ≈ 931.4941 MeV/c²."
        ),
        PhysicalConstant(
            name = "Fine-structure constant",
            symbolLatex = """\alpha""",
            value = "7.2973525643(11) × 10⁻³",
            unit = "dimensionless",
            note = "e² / (4πε₀ℏc) ≈ 1 / 137.035999."
        ),
        PhysicalConstant(
            name = "Bohr radius",
            symbolLatex = "a_0",
            value = "5.29177210544(82) × 10⁻¹¹",
            unit = "m",
            note = "4πε₀ℏ² / (m_e e²) = ℏ / (α m_e c)."
        ),
        PhysicalConstant(
            name = "Rydberg constant",
            symbolLatex = """R_\infty""",
            value = "10,973,731.568157(12)",
            unit = "m⁻¹",
            note = "CODATA 2022; hcR_∞ ≈ 13.60569 eV."
        ),
        PhysicalConstant(
            name = "Stefan-Boltzmann constant",
            symbolLatex = """\sigma""",
            value = "5.670374419... × 10⁻⁸",
            unit = "W/(m²·K⁴)",
            note = "Exact: π² k_B⁴ / (60 ℏ³ c²)."
        ),
        PhysicalConstant(
            name = "Faraday constant",
            symbolLatex = "F",
            value = "96,485.33212...",
            unit = "C/mol",
            note = "Exact: N_A · e (charge per mole of electrons)."
        ),
        PhysicalConstant(
            name = "Standard acceleration of gravity",
            symbolLatex = "g_0",
            value = "9.80665",
            unit = "m/s²",
            note = "Exact conventional standard gravity at sea level."
        ),
        PhysicalConstant(
            name = "Standard atmosphere",
            symbolLatex = "P_0",
            value = "101,325",
            unit = "Pa",
            note = "Exact conventional atmospheric pressure (1 atm)."
        ),
        PhysicalConstant(
            name = "Astronomical unit",
            symbolLatex = """\text{AU}""",
            value = "1.495978707 × 10¹¹",
            unit = "m",
            note = "Exact IAU 2012 definition (mean Earth-Sun distance)."
        ),
        PhysicalConstant(
            name = "Solar mass",
            symbolLatex = """M_\odot""",
            value = "1.98847 × 10³⁰",
            unit = "kg",
            note = "Standard gravitational parameter GM_⊙ = 1.3271244 × 10²⁰ m³/s²."
        ),
        PhysicalConstant(
            name = "Earth mass",
            symbolLatex = """M_\oplus""",
            value = "5.9722 × 10²⁴",
            unit = "kg",
            note = "Standard gravitational parameter GM_⊕ = 3.986004418 × 10¹⁴ m³/s²."
        ),
        PhysicalConstant(
            name = "Mean Earth radius",
            symbolLatex = """R_\oplus""",
            value = "6.371 × 10⁶",
            unit = "m",
            note = "Volumetric mean radius (equatorial radius ≈ 6,378.1 km)."
        )
    )
}
