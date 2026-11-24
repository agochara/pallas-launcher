package com.pallas.launcher.domain.reference

import com.pallas.launcher.domain.math.CheatsheetFormula
import com.pallas.launcher.domain.math.CheatsheetSubsection
import com.pallas.launcher.domain.math.CheatsheetTopic

object EngineeringCheatsheetData {

    val ENGINEERING_TOPIC: CheatsheetTopic = CheatsheetTopic(
        id = "engineering",
        title = "Structural Engineering",
        shortTitle = "Engineering",
        symbol = "σ",
        description = "Mechanics of materials, beam bending, torsion, buckling, fracture mechanics, and cross-section properties.",
        domain = CheatsheetDomain.ENGINEERING_QUANT,
        kind = CheatsheetKind.ENGINEERING,
        subsections = listOf(
            CheatsheetSubsection(
                title = "Stress, Strain & Elasticity",
                shortTitle = "Elasticity",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Axial Stress & Strain",
                        latex = """\sigma = \frac{F}{A}, \quad \varepsilon = \frac{\Delta L}{L_0}""",
                        note = "Engineering (nominal) normal stress and dimensionless axial strain."
                    ),
                    CheatsheetFormula(
                        title = "Hooke's Law & Axial Elongation",
                        latex = """\sigma = E\varepsilon, \quad \delta = \frac{PL}{AE}""",
                        note = "Linear elastic response where E is Young's modulus and AE is axial rigidity."
                    ),
                    CheatsheetFormula(
                        title = "Shear Stress & Modulus",
                        latex = """\tau = \frac{V}{A}, \quad \tau = G\gamma""",
                        note = "Average shear stress and Hooke's law in shear; G is the shear modulus."
                    ),
                    CheatsheetFormula(
                        title = "Poisson's Ratio & Bulk Modulus Relations",
                        latex = """\nu = -\frac{\varepsilon_{\text{trans}}}{\varepsilon_{\text{axial}}}, \quad E = 2G(1 + \nu) = 3K(1 - 2\nu)""",
                        note = "Isotropic linear-elastic constants linking E, shear modulus G, and bulk modulus K."
                    ),
                    CheatsheetFormula(
                        title = "Strain Energy Density",
                        latex = """U_0 = \frac{1}{2}\sigma\varepsilon = \frac{\sigma^2}{2E} = \frac{1}{2}E\varepsilon^2""",
                        note = "Elastic energy stored per unit volume; area under the linear stress-strain curve."
                    ),
                    CheatsheetFormula(
                        title = "Thermal Stress",
                        latex = """\varepsilon_{\text{th}} = \alpha\Delta T, \quad \sigma = E\alpha\Delta T""",
                        note = "Magnitude of axial stress induced when thermal expansion is fully constrained."
                    ),
                    CheatsheetFormula(
                        title = "Principal Stresses & Max Shear (2D Mohr's Circle)",
                        latex = """\sigma_{1,2} = \frac{\sigma_x + \sigma_y}{2} \pm \sqrt{\left(\frac{\sigma_x - \sigma_y}{2}\right)^2 + \tau_{xy}^2}, \quad \tau_{\max} = \frac{\sigma_1 - \sigma_2}{2}""",
                        note = "In-plane extreme normal stresses and maximum in-plane shear stress."
                    ),
                    CheatsheetFormula(
                        title = "Von Mises Yield Criterion",
                        latex = """\sigma_v = \sqrt{\frac{1}{2}\left[(\sigma_1 - \sigma_2)^2 + (\sigma_2 - \sigma_3)^2 + (\sigma_3 - \sigma_1)^2\right]} \le \sigma_y""",
                        note = "Distortion-energy yield criterion for ductile metals under multiaxial loading."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Beam Bending & Deflection",
                shortTitle = "Beams",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Flexure Formula",
                        latex = """\sigma = -\frac{My}{I}, \quad \sigma_{\max} = \frac{Mc}{I} = \frac{M}{S}""",
                        note = "Bending normal stress at distance y from the neutral axis; S = I/c is section modulus."
                    ),
                    CheatsheetFormula(
                        title = "Euler-Bernoulli Beam Curvature",
                        latex = """EI\,w'' = M, \quad \frac{1}{\rho} = \frac{M}{EI}""",
                        note = "Small-deflection beam equation relating bending moment M to flexural rigidity EI."
                    ),
                    CheatsheetFormula(
                        title = "Transverse Shear Stress",
                        latex = """\tau = \frac{VQ}{Ib}, \quad Q = A'\bar{y}'""",
                        note = "Jourawski shear formula; Q is first moment of area above/below the cut of width b."
                    ),
                    CheatsheetFormula(
                        title = "Cantilever End Load",
                        latex = """\delta_{\max} = \frac{PL^3}{3EI}, \quad \theta_{\max} = \frac{PL^2}{2EI}""",
                        note = "Tip deflection and slope for a cantilever of length L with point load P at free end."
                    ),
                    CheatsheetFormula(
                        title = "Simply Supported Center Load",
                        latex = """\delta_{\max} = \frac{PL^3}{48EI}, \quad M_{\max} = \frac{PL}{4}""",
                        note = "Midspan deflection and peak bending moment for point load P at span center."
                    ),
                    CheatsheetFormula(
                        title = "Simply Supported Uniform Load",
                        latex = """\delta_{\max} = \frac{5wL^4}{384EI}, \quad M_{\max} = \frac{wL^2}{8}""",
                        note = "Midspan deflection and peak moment under uniform distributed load w (force/length)."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Torsion & Pressure Vessels",
                shortTitle = "Torsion",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Torsional Shear Stress",
                        latex = """\tau = \frac{Tr}{J}, \quad \tau_{\max} = \frac{Tc}{J}""",
                        note = "Elastic shear stress in a circular shaft under torque T; J is polar moment of inertia."
                    ),
                    CheatsheetFormula(
                        title = "Angle of Twist",
                        latex = """\theta = \frac{TL}{GJ}""",
                        note = "Total twist angle in radians over length L; GJ is torsional rigidity."
                    ),
                    CheatsheetFormula(
                        title = "Thin-Walled Cylinder Hoop & Axial Stress",
                        latex = """\sigma_\theta = \frac{pr}{t}, \quad \sigma_z = \frac{pr}{2t}""",
                        note = "Hoop stress is twice axial stress for thin-walled cylinders (r/t ≥ 10), so pressurized cylinders fail longitudinally."
                    ),
                    CheatsheetFormula(
                        title = "Spherical Pressure Vessel",
                        latex = """\sigma = \frac{pr}{2t}, \quad \Delta r = \frac{pr^2}{2Et}(1 - \nu)""",
                        note = "Equal biaxial membrane stress in all tangent directions for internal gauge pressure p."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Buckling & Fracture",
                shortTitle = "Fracture",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Euler Critical Buckling Load",
                        latex = """P_{\text{cr}} = \frac{\pi^2 EI}{(KL)^2}""",
                        note = "Elastic column instability; effective length factor K = 1.0 (pinned-pinned), 0.5 (fixed-fixed), 2.0 (cantilever)."
                    ),
                    CheatsheetFormula(
                        title = "Euler Critical Stress & Slenderness Ratio",
                        latex = """\sigma_{\text{cr}} = \frac{\pi^2 E}{\lambda^2}, \quad \lambda = \frac{KL}{r_g}""",
                        note = "Valid for slender columns where critical stress remains below proportional limit."
                    ),
                    CheatsheetFormula(
                        title = "Griffith Fracture Criterion",
                        latex = """\sigma_f = \sqrt{\frac{2E\gamma}{\pi a}}""",
                        note = "Critical tensile stress for brittle crack propagation of half-length a with surface energy γ (plane stress)."
                    ),
                    CheatsheetFormula(
                        title = "Stress Intensity Factor",
                        latex = """K_I = Y\sigma\sqrt{\pi a} \ge K_{Ic}""",
                        note = "Mode-I crack-tip stress field intensity; fracture occurs when K_I reaches fracture toughness K_Ic."
                    ),
                    CheatsheetFormula(
                        title = "Inglis Elliptical Stress Concentration",
                        latex = """\sigma_{\max} = \sigma\left(1 + 2\sqrt{\frac{a}{\rho}}\right) = \sigma\left(1 + \frac{2a}{b}\right)""",
                        note = "Peak stress at the tip of an elliptical hole of semi-axis a and tip radius ρ = b²/a."
                    ),
                    CheatsheetFormula(
                        title = "Circular Hole Stress Concentration",
                        latex = """K_t = \frac{\sigma_{\max}}{\sigma} = 3""",
                        note = "Special case of Inglis (a = b = ρ) for a small circular hole in a wide plate under uniaxial tension."
                    )
                )
            ),
            CheatsheetSubsection(
                title = "Moments of Inertia (I & J)",
                shortTitle = "Inertia",
                formulas = listOf(
                    CheatsheetFormula(
                        title = "Solid Rectangle",
                        latex = """I_x = \frac{bh^3}{12}, \quad I_y = \frac{hb^3}{12}""",
                        note = "Second moments of area about centroidal axes for width b and height h."
                    ),
                    CheatsheetFormula(
                        title = "Hollow Rectangular Box",
                        latex = """I_x = \frac{BH^3 - bh^3}{12}""",
                        note = "Outer dimensions B × H minus inner cutout b × h with shared centroid."
                    ),
                    CheatsheetFormula(
                        title = "Solid & Hollow Circle",
                        latex = """I = \frac{\pi r^4}{4}, \quad J = \frac{\pi r^4}{2}, \quad I_{\text{ring}} = \frac{\pi}{4}\left(r_o^4 - r_i^4\right)""",
                        note = "Bending I and polar J = I_x + I_y; hollow shafts maximize J/mass."
                    ),
                    CheatsheetFormula(
                        title = "Triangle",
                        latex = """I_x = \frac{bh^3}{36}, \quad I_{\text{base}} = \frac{bh^3}{12}""",
                        note = "I_x is about the centroid (h/3 above base); I_base is about the base edge."
                    ),
                    CheatsheetFormula(
                        title = "Parallel Axis Theorem",
                        latex = """I = I_c + Ad^2, \quad J = J_c + Ad^2""",
                        note = "Transfers centroidal moment I_c across parallel offset distance d for area A."
                    ),
                    CheatsheetFormula(
                        title = "Radius of Gyration",
                        latex = """r_g = \sqrt{\frac{I}{A}}""",
                        note = "Effective distance from neutral axis governing column slenderness KL/r_g."
                    )
                )
            )
        )
    )

    val ENGINEERING_MATERIALS: List<EngineeringMaterial> = listOf(
        EngineeringMaterial(
            name = "Mild Steel (A36)",
            densityKgM3 = 7850.0,
            densityLabel = "7,850 kg/m³",
            youngsModulusGPa = 200.0,
            modulusLabel = "200 GPa",
            yieldStrengthMPa = 250.0,
            yieldLabel = "250 MPa",
            ultimateStrengthMPa = 400.0,
            ultimateLabel = "400–550 MPa",
            note = "Ductile structural baseline; high stiffness-to-cost ratio."
        ),
        EngineeringMaterial(
            name = "High-Tensile Steel (4340)",
            densityKgM3 = 7850.0,
            densityLabel = "7,850 kg/m³",
            youngsModulusGPa = 205.0,
            modulusLabel = "205 GPa",
            yieldStrengthMPa = 860.0,
            yieldLabel = "860 MPa",
            ultimateStrengthMPa = 1080.0,
            ultimateLabel = "1,080 MPa",
            note = "Quenched & tempered Ni-Cr-Mo alloy steel; aerospace shafts and landing gear."
        ),
        EngineeringMaterial(
            name = "Stainless Steel (304)",
            densityKgM3 = 8000.0,
            densityLabel = "8,000 kg/m³",
            youngsModulusGPa = 193.0,
            modulusLabel = "193 GPa",
            yieldStrengthMPa = 215.0,
            yieldLabel = "215 MPa",
            ultimateStrengthMPa = 505.0,
            ultimateLabel = "505 MPa",
            note = "Austenitic Cr-Ni steel; corrosion resistant with strong work-hardening."
        ),
        EngineeringMaterial(
            name = "Aluminum 6061-T6",
            densityKgM3 = 2700.0,
            densityLabel = "2,700 kg/m³",
            youngsModulusGPa = 69.0,
            modulusLabel = "69 GPa",
            yieldStrengthMPa = 276.0,
            yieldLabel = "276 MPa",
            ultimateStrengthMPa = 310.0,
            ultimateLabel = "310 MPa",
            note = "Precipitation-hardened alloy; ~1/3 steel density with comparable yield."
        ),
        EngineeringMaterial(
            name = "Titanium (Ti-6Al-4V)",
            densityKgM3 = 4430.0,
            densityLabel = "4,430 kg/m³",
            youngsModulusGPa = 114.0,
            modulusLabel = "114 GPa",
            yieldStrengthMPa = 880.0,
            yieldLabel = "880 MPa",
            ultimateStrengthMPa = 950.0,
            ultimateLabel = "950 MPa",
            note = "Grade 5 alpha-beta alloy; high specific strength and corrosion resistance."
        ),
        EngineeringMaterial(
            name = "Copper (Annealed)",
            densityKgM3 = 8960.0,
            densityLabel = "8,960 kg/m³",
            youngsModulusGPa = 117.0,
            modulusLabel = "117 GPa",
            yieldStrengthMPa = 70.0,
            yieldLabel = "70 MPa",
            ultimateStrengthMPa = 220.0,
            ultimateLabel = "220 MPa",
            note = "High electrical and thermal conductivity; very ductile when annealed."
        ),
        EngineeringMaterial(
            name = "Concrete (Compressive)",
            densityKgM3 = 2400.0,
            densityLabel = "2,400 kg/m³",
            youngsModulusGPa = 30.0,
            modulusLabel = "30 GPa",
            yieldStrengthMPa = null,
            yieldLabel = "—",
            ultimateStrengthMPa = 35.0,
            ultimateLabel = "35 MPa (comp.)",
            note = "Standard structural grade in compression; brittle failure without yield."
        ),
        EngineeringMaterial(
            name = "Concrete (Tensile)",
            densityKgM3 = 2400.0,
            densityLabel = "2,400 kg/m³",
            youngsModulusGPa = 30.0,
            modulusLabel = "30 GPa",
            yieldStrengthMPa = null,
            yieldLabel = "—",
            ultimateStrengthMPa = 3.5,
            ultimateLabel = "3.5 MPa (tens.)",
            note = "Roughly 10% of compressive strength; requires steel rebar in tension zones."
        ),
        EngineeringMaterial(
            name = "Timber (Spruce/Pine ∥ grain)",
            densityKgM3 = 450.0,
            densityLabel = "450 kg/m³",
            youngsModulusGPa = 11.0,
            modulusLabel = "11 GPa",
            yieldStrengthMPa = null,
            yieldLabel = "—",
            ultimateStrengthMPa = 70.0,
            ultimateLabel = "70 MPa",
            note = "Anisotropic cellular composite; high specific stiffness along the grain."
        ),
        EngineeringMaterial(
            name = "Carbon Fiber (CFRP unidirectional)",
            densityKgM3 = 1600.0,
            densityLabel = "1,600 kg/m³",
            youngsModulusGPa = 140.0,
            modulusLabel = "140 GPa",
            yieldStrengthMPa = null,
            yieldLabel = "—",
            ultimateStrengthMPa = 1500.0,
            ultimateLabel = "1,500 MPa",
            note = "Linear-elastic to brittle fracture along fiber axis; extreme specific strength."
        ),
        EngineeringMaterial(
            name = "Glass (Soda-Lime)",
            densityKgM3 = 2500.0,
            densityLabel = "2,500 kg/m³",
            youngsModulusGPa = 70.0,
            modulusLabel = "70 GPa",
            yieldStrengthMPa = null,
            yieldLabel = "—",
            ultimateStrengthMPa = 50.0,
            ultimateLabel = "50 MPa (bulk)",
            note = "Theoretical bond strength ~10 GPa; bulk fracture governed by surface flaws."
        ),
        EngineeringMaterial(
            name = "Natural Rubber",
            densityKgM3 = 930.0,
            densityLabel = "930 kg/m³",
            youngsModulusGPa = 0.005,
            modulusLabel = "0.005 GPa (5 MPa)",
            yieldStrengthMPa = null,
            yieldLabel = "—",
            ultimateStrengthMPa = 20.0,
            ultimateLabel = "20 MPa",
            note = "Entropic elastomer; sustains >500% elastic strain before rupture."
        ),
        EngineeringMaterial(
            name = "Cast Iron (Grey)",
            densityKgM3 = 7200.0,
            densityLabel = "7,200 kg/m³",
            youngsModulusGPa = 110.0,
            modulusLabel = "110 GPa",
            yieldStrengthMPa = null,
            yieldLabel = "—",
            ultimateStrengthMPa = 200.0,
            ultimateLabel = "200 MPa (tens.)",
            note = "Graphite flakes act as internal cracks in tension; ~3–4× stronger in compression."
        ),
        EngineeringMaterial(
            name = "Spider Silk (Dragline)",
            densityKgM3 = 1300.0,
            densityLabel = "1,300 kg/m³",
            youngsModulusGPa = 10.0,
            modulusLabel = "10 GPa",
            yieldStrengthMPa = 300.0,
            yieldLabel = "300 MPa",
            ultimateStrengthMPa = 1100.0,
            ultimateLabel = "1,100 MPa",
            note = "Biological protein fiber; high toughness (~160 MJ/m³) from ~25% strain-to-failure."
        )
    )
}
