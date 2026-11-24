package com.pallas.launcher.domain.reference

enum class CheatsheetDomain(val title: String) {
    MATHEMATICS("Mathematics"),
    SCIENCES("Physical & Life Sciences"),
    ENGINEERING_QUANT("Engineering, Quantitative & Reference")
}

enum class CheatsheetKind {
    FORMULAS,
    PHYSICS,
    PERIODIC_TABLE,
    ENGINEERING,
    BIOLOGY
}

data class PhysicalConstant(
    val name: String,
    val symbolLatex: String,
    val value: String,
    val unit: String,
    val note: String = ""
)

enum class ElementCategory(val label: String) {
    ALKALI_METAL("Alkali Metal"),
    ALKALINE_EARTH("Alkaline Earth Metal"),
    TRANSITION_METAL("Transition Metal"),
    POST_TRANSITION("Post-Transition Metal"),
    METALLOID("Metalloid"),
    REACTIVE_NONMETAL("Reactive Nonmetal"),
    HALOGEN("Halogen"),
    NOBLE_GAS("Noble Gas"),
    LANTHANIDE("Lanthanide"),
    ACTINIDE("Actinide")
}

data class PeriodicElement(
    val atomicNumber: Int,
    val symbol: String,
    val name: String,
    val atomicMass: String,
    val period: Int,
    val group: Int,
    val gridRow: Int,
    val gridCol: Int,
    val category: ElementCategory,
    val electronConfig: String,
    val electronegativity: Double?,
    val oxidationStates: String,
    val meltingPointK: Double?,
    val boilingPointK: Double?,
    val densityGcm3: Double?,
    val phaseAtStp: String
)

data class EngineeringMaterial(
    val name: String,
    val densityKgM3: Double,
    val densityLabel: String,
    val youngsModulusGPa: Double,
    val modulusLabel: String,
    val yieldStrengthMPa: Double?,
    val yieldLabel: String,
    val ultimateStrengthMPa: Double,
    val ultimateLabel: String,
    val note: String = ""
)

data class PhysiologyBaseline(
    val category: String,
    val metric: String,
    val normalRange: String,
    val unit: String
)
