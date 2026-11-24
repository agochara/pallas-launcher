package com.pallas.launcher.domain.search

import com.pallas.launcher.platform.CurrencyRateService
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class ConversionOutcome(
    val title: String,
    val primaryValue: String,
    val badge: String,
    val subtitle: String,
    val copyText: String,
    val isCurrency: Boolean = false,
    val fromCurrency: String? = null,
    val toCurrency: String? = null
)

object UnitConverterEngine {

    enum class UnitCategory(val badgeLabel: String) {
        TEMPERATURE("Temperature"),
        LENGTH("Length"),
        MASS("Weight & Mass"),
        VOLUME("Volume"),
        AREA("Area"),
        SPEED("Speed"),
        TIME("Time"),
        DATA_SIZE("Data Size"),
        DATA_RATE("Bandwidth"),
        PRESSURE("Pressure"),
        ENERGY("Energy"),
        POWER("Power"),
        ANGLE("Angle"),
        FORCE("Force"),
        FREQUENCY("Frequency"),
        FUEL_ECONOMY("Fuel Economy"),
        NUMBER_BASE("Base & Numeral"),
        CURRENCY("Currency FX")
    }

    data class UnitDef(
        val id: String,
        val category: UnitCategory,
        val name: String,
        val symbol: String,
        val aliases: Set<String>,
        val toBase: (Double) -> Double,
        val fromBase: (Double) -> Double
    )

    private fun linearUnit(
        id: String,
        category: UnitCategory,
        name: String,
        symbol: String,
        factorToBase: Double,
        vararg aliases: String
    ): UnitDef = UnitDef(
        id = id,
        category = category,
        name = name,
        symbol = symbol,
        aliases = (aliases.map { it.lowercase(Locale.ROOT) } + id.lowercase(Locale.ROOT) + symbol.lowercase(Locale.ROOT) + name.lowercase(Locale.ROOT)).toSet(),
        toBase = { it * factorToBase },
        fromBase = { it / factorToBase }
    )

    val UNITS: List<UnitDef> = listOf(
        // =====================================================================
        // 1. TEMPERATURE (Base: Celsius)
        // =====================================================================
        UnitDef(
            id = "celsius",
            category = UnitCategory.TEMPERATURE,
            name = "Celsius",
            symbol = "°C",
            aliases = setOf("c", "°c", "degc", "deg c", "celsius", "centigrade", "degrees c", "degree celsius", "degrees celsius"),
            toBase = { it },
            fromBase = { it }
        ),
        UnitDef(
            id = "fahrenheit",
            category = UnitCategory.TEMPERATURE,
            name = "Fahrenheit",
            symbol = "°F",
            aliases = setOf("f", "°f", "degf", "deg f", "fahrenheit", "degrees f", "degree fahrenheit", "degrees fahrenheit"),
            toBase = { (it - 32.0) * 5.0 / 9.0 },
            fromBase = { it * 9.0 / 5.0 + 32.0 }
        ),
        UnitDef(
            id = "kelvin",
            category = UnitCategory.TEMPERATURE,
            name = "Kelvin",
            symbol = "K",
            aliases = setOf("k", "°k", "degk", "kelvin", "kelvins"),
            toBase = { it - 273.15 },
            fromBase = { it + 273.15 }
        ),
        UnitDef(
            id = "rankine",
            category = UnitCategory.TEMPERATURE,
            name = "Rankine",
            symbol = "°R",
            aliases = setOf("r", "°r", "rankine", "degr"),
            toBase = { (it - 491.67) * 5.0 / 9.0 },
            fromBase = { (it + 273.15) * 9.0 / 5.0 }
        ),

        // =====================================================================
        // 2. LENGTH (Base: Meter)
        // =====================================================================
        linearUnit("nm", UnitCategory.LENGTH, "Nanometers", "nm", 1e-9, "nanometer", "nanometers", "nanometre", "nanometres"),
        linearUnit("um", UnitCategory.LENGTH, "Micrometers", "μm", 1e-6, "um", "μm", "µm", "micron", "microns", "micrometer", "micrometers", "micrometre", "micrometres"),
        linearUnit("mm", UnitCategory.LENGTH, "Millimeters", "mm", 1e-3, "millimeter", "millimeters", "millimetre", "millimetres"),
        linearUnit("cm", UnitCategory.LENGTH, "Centimeters", "cm", 1e-2, "centimeter", "centimeters", "centimetre", "centimetres", "cms"),
        linearUnit("dm", UnitCategory.LENGTH, "Decimeters", "dm", 1e-1, "decimeter", "decimeters", "decimetre", "decimetres"),
        linearUnit("m", UnitCategory.LENGTH, "Meters", "m", 1.0, "meter", "meters", "metre", "metres", "mtr", "mtrs"),
        linearUnit("km", UnitCategory.LENGTH, "Kilometers", "km", 1000.0, "kilometer", "kilometers", "kilometre", "kilometres", "kms", "klick", "klicks"),
        linearUnit("mil", UnitCategory.LENGTH, "Mils", "mil", 0.0000254, "mil", "mils", "thou"),
        linearUnit("in", UnitCategory.LENGTH, "Inches", "in", 0.0254, "in", "inch", "inches", "\"", "″"),
        linearUnit("ft", UnitCategory.LENGTH, "Feet", "ft", 0.3048, "ft", "foot", "feet", "'", "′", "ft in", "feet inches"),
        linearUnit("yd", UnitCategory.LENGTH, "Yards", "yd", 0.9144, "yd", "yds", "yard", "yards"),
        linearUnit("mi", UnitCategory.LENGTH, "Miles", "mi", 1609.344, "mi", "mile", "miles"),
        linearUnit("nmi", UnitCategory.LENGTH, "Nautical Miles", "nmi", 1852.0, "nmi", "nmile", "nmiles", "nautical mile", "nautical miles"),
        linearUnit("fathom", UnitCategory.LENGTH, "Fathoms", "ftm", 1.8288, "fathom", "fathoms", "ftm"),
        linearUnit("furlong", UnitCategory.LENGTH, "Furlongs", "fur", 201.168, "furlong", "furlongs"),
        linearUnit("au", UnitCategory.LENGTH, "Astronomical Units", "AU", 149_597_870_700.0, "au", "astronomical unit", "astronomical units"),
        linearUnit("ly", UnitCategory.LENGTH, "Light-Years", "ly", 9_460_730_472_580_800.0, "ly", "lightyear", "lightyears", "light year", "light years", "light-year", "light-years"),
        linearUnit("pc", UnitCategory.LENGTH, "Parsecs", "pc", 30_856_775_814_913_673.0, "pc", "parsec", "parsecs"),

        // =====================================================================
        // 3. MASS / WEIGHT (Base: Kilogram)
        // =====================================================================
        linearUnit("ug", UnitCategory.MASS, "Micrograms", "μg", 1e-9, "ug", "μg", "µg", "mcg", "microgram", "micrograms"),
        linearUnit("mg", UnitCategory.MASS, "Milligrams", "mg", 1e-6, "mg", "milligram", "milligrams"),
        linearUnit("g", UnitCategory.MASS, "Grams", "g", 1e-3, "g", "gm", "gms", "gram", "grams", "gramme", "grammes"),
        linearUnit("kg", UnitCategory.MASS, "Kilograms", "kg", 1.0, "kg", "kgs", "kilo", "kilos", "kilogram", "kilograms", "kilogramme", "kilogrammes"),
        linearUnit("tonne", UnitCategory.MASS, "Metric Tonnes", "t", 1000.0, "t", "mt", "tonne", "tonnes", "metric ton", "metric tons"),
        linearUnit("ct", UnitCategory.MASS, "Carats", "ct", 0.0002, "ct", "carat", "carats"),
        linearUnit("gr", UnitCategory.MASS, "Grains", "gr", 0.00006479891, "gr", "grain", "grains"),
        linearUnit("oz", UnitCategory.MASS, "Ounces", "oz", 0.028349523125, "oz", "ozs", "ounce", "ounces"),
        linearUnit("ozt", UnitCategory.MASS, "Troy Ounces", "oz t", 0.0311034768, "ozt", "troy oz", "troy ounce", "troy ounces"),
        linearUnit("lb", UnitCategory.MASS, "Pounds", "lb", 0.45359237, "lb", "lbs", "lbm", "pound", "pounds"),
        linearUnit("st", UnitCategory.MASS, "Stone", "st", 6.35029318, "st", "stone", "stones"),
        linearUnit("ton", UnitCategory.MASS, "US Short Tons", "ton", 907.18474, "ton", "tons", "short ton", "short tons", "us ton", "us tons"),
        linearUnit("long_ton", UnitCategory.MASS, "UK Long Tons", "long ton", 1016.0469088, "long ton", "long tons", "imperial ton", "imperial tons", "uk ton", "uk tons"),

        // =====================================================================
        // 4. VOLUME (Base: Liter)
        // =====================================================================
        linearUnit("ml", UnitCategory.VOLUME, "Milliliters", "mL", 1e-3, "ml", "mls", "milliliter", "milliliters", "millilitre", "millilitres", "cc", "cm3", "cm^3", "cm³"),
        linearUnit("cl", UnitCategory.VOLUME, "Centiliters", "cL", 1e-2, "cl", "centiliter", "centiliters", "centilitre", "centilitres"),
        linearUnit("dl", UnitCategory.VOLUME, "Deciliters", "dL", 1e-1, "dl", "deciliter", "deciliters", "decilitre", "decilitres"),
        linearUnit("l", UnitCategory.VOLUME, "Liters", "L", 1.0, "l", "ltr", "ltrs", "liter", "liters", "litre", "litres"),
        linearUnit("m3", UnitCategory.VOLUME, "Cubic Meters", "m³", 1000.0, "m3", "m^3", "m³", "cu m", "cubic meter", "cubic meters", "cubic metre", "cubic metres"),
        linearUnit("tsp", UnitCategory.VOLUME, "US Teaspoons", "tsp", 0.00492892159375, "tsp", "tsps", "teaspoon", "teaspoons"),
        linearUnit("tbsp", UnitCategory.VOLUME, "US Tablespoons", "tbsp", 0.01478676478125, "tbsp", "tbsps", "tbs", "tablespoon", "tablespoons"),
        linearUnit("floz", UnitCategory.VOLUME, "US Fluid Ounces", "fl oz", 0.0295735295625, "fl oz", "floz", "fl. oz", "fluid oz", "fluid ounce", "fluid ounces", "us fl oz"),
        linearUnit("uk_floz", UnitCategory.VOLUME, "Imperial Fluid Ounces", "imp fl oz", 0.0284130625, "uk fl oz", "imp fl oz", "imperial fl oz", "imperial fluid ounce", "imperial fluid ounces"),
        linearUnit("cup", UnitCategory.VOLUME, "US Cups", "cup", 0.2365882365, "cup", "cups"),
        linearUnit("pt", UnitCategory.VOLUME, "US Pints", "pt", 0.473176473, "pt", "pts", "pint", "pints", "us pt", "us pint", "us pints"),
        linearUnit("uk_pt", UnitCategory.VOLUME, "Imperial Pints", "imp pt", 0.56826125, "uk pt", "imp pt", "uk pint", "uk pints", "imperial pint", "imperial pints"),
        linearUnit("qt", UnitCategory.VOLUME, "US Quarts", "qt", 0.946352946, "qt", "qts", "quart", "quarts", "us qt", "us quart"),
        linearUnit("uk_qt", UnitCategory.VOLUME, "Imperial Quarts", "imp qt", 1.1365225, "uk qt", "imp qt", "uk quart", "imperial quart", "imperial quarts"),
        linearUnit("gal", UnitCategory.VOLUME, "US Gallons", "gal", 3.785411784, "gal", "gals", "gallon", "gallons", "us gal", "us gallon", "us gallons"),
        linearUnit("uk_gal", UnitCategory.VOLUME, "Imperial Gallons", "imp gal", 4.54609, "uk gal", "imp gal", "uk gallon", "uk gallons", "imperial gallon", "imperial gallons"),
        linearUnit("in3", UnitCategory.VOLUME, "Cubic Inches", "in³", 0.016387064, "in3", "in^3", "in³", "cu in", "cubic inch", "cubic inches"),
        linearUnit("ft3", UnitCategory.VOLUME, "Cubic Feet", "ft³", 28.316846592, "ft3", "ft^3", "ft³", "cu ft", "cubic foot", "cubic feet"),
        linearUnit("bbl", UnitCategory.VOLUME, "Oil Barrels", "bbl", 158.987294928, "bbl", "barrel", "barrels"),

        // =====================================================================
        // 5. AREA (Base: Square Meter)
        // =====================================================================
        linearUnit("mm2", UnitCategory.AREA, "Square Millimeters", "mm²", 1e-6, "mm2", "mm^2", "mm²", "sq mm", "sqmm", "square millimeter", "square millimeters"),
        linearUnit("cm2", UnitCategory.AREA, "Square Centimeters", "cm²", 1e-4, "cm2", "cm^2", "cm²", "sq cm", "sqcm", "square centimeter", "square centimeters"),
        linearUnit("m2", UnitCategory.AREA, "Square Meters", "m²", 1.0, "m2", "m^2", "m³".replace('³', '²'), "sq m", "sqm", "square meter", "square meters", "square metre", "square metres"),
        linearUnit("ha", UnitCategory.AREA, "Hectares", "ha", 10_000.0, "ha", "hectare", "hectares"),
        linearUnit("km2", UnitCategory.AREA, "Square Kilometers", "km²", 1_000_000.0, "km2", "km^2", "km²", "sq km", "sqkm", "square kilometer", "square kilometers", "square kilometre", "square kilometres"),
        linearUnit("in2", UnitCategory.AREA, "Square Inches", "in²", 0.00064516, "in2", "in^2", "in²", "sq in", "sqin", "square inch", "square inches"),
        linearUnit("ft2", UnitCategory.AREA, "Square Feet", "ft²", 0.09290304, "ft2", "ft^2", "ft²", "sq ft", "sqft", "square foot", "square feet"),
        linearUnit("yd2", UnitCategory.AREA, "Square Yards", "yd²", 0.83612736, "yd2", "yd^2", "yd²", "sq yd", "sqyd", "square yard", "square yards"),
        linearUnit("acre", UnitCategory.AREA, "Acres", "ac", 4046.8564224, "ac", "acre", "acres"),
        linearUnit("mi2", UnitCategory.AREA, "Square Miles", "mi²", 2_589_988.110336, "mi2", "mi^2", "mi²", "sq mi", "sqmi", "square mile", "square miles"),

        // =====================================================================
        // 6. SPEED (Base: Meters per second)
        // =====================================================================
        linearUnit("mps", UnitCategory.SPEED, "Meters per second", "m/s", 1.0, "m/s", "mps", "meter per second", "meters per second", "metre per second", "metres per second"),
        linearUnit("kmh", UnitCategory.SPEED, "Kilometers per hour", "km/h", 1.0 / 3.6, "km/h", "kmh", "kph", "kmph", "km/hr", "kilometer per hour", "kilometers per hour", "kilometre per hour", "kilometres per hour"),
        linearUnit("mph", UnitCategory.SPEED, "Miles per hour", "mph", 0.44704, "mph", "mi/h", "mi/hr", "mile per hour", "miles per hour"),
        linearUnit("fps", UnitCategory.SPEED, "Feet per second", "ft/s", 0.3048, "ft/s", "fps", "foot per second", "feet per second"),
        linearUnit("knot", UnitCategory.SPEED, "Knots", "kn", 1852.0 / 3600.0, "kn", "kt", "kts", "knot", "knots"),
        linearUnit("mach", UnitCategory.SPEED, "Mach", "Mach", 343.0, "mach"),
        linearUnit("c_light", UnitCategory.SPEED, "Speed of Light", "c", 299_792_458.0, "speed of light", "lightspeed"),

        // =====================================================================
        // 7. TIME (Base: Second)
        // =====================================================================
        linearUnit("ns", UnitCategory.TIME, "Nanoseconds", "ns", 1e-9, "ns", "nsec", "nanosecond", "nanoseconds"),
        linearUnit("us", UnitCategory.TIME, "Microseconds", "μs", 1e-6, "us", "μs", "µs", "usec", "microsecond", "microseconds"),
        linearUnit("ms", UnitCategory.TIME, "Milliseconds", "ms", 1e-3, "ms", "msec", "msecs", "millisecond", "milliseconds"),
        linearUnit("s", UnitCategory.TIME, "Seconds", "s", 1.0, "s", "sec", "secs", "second", "seconds"),
        linearUnit("min", UnitCategory.TIME, "Minutes", "min", 60.0, "min", "mins", "minute", "minutes"),
        linearUnit("hr", UnitCategory.TIME, "Hours", "hr", 3600.0, "h", "hr", "hrs", "hour", "hours"),
        linearUnit("day", UnitCategory.TIME, "Days", "d", 86_400.0, "d", "day", "days"),
        linearUnit("wk", UnitCategory.TIME, "Weeks", "wk", 604_800.0, "wk", "wks", "week", "weeks"),
        linearUnit("mo", UnitCategory.TIME, "Months", "mo", 2_629_746.0, "mo", "mos", "month", "months"),
        linearUnit("yr", UnitCategory.TIME, "Years", "yr", 31_556_952.0, "yr", "yrs", "year", "years"),
        linearUnit("decade", UnitCategory.TIME, "Decades", "decade", 315_569_520.0, "decade", "decades"),
        linearUnit("century", UnitCategory.TIME, "Centuries", "century", 3_155_695_200.0, "century", "centuries"),

        // =====================================================================
        // 8. DATA SIZE (Base: Byte)
        // =====================================================================
        linearUnit("bit", UnitCategory.DATA_SIZE, "Bits", "bit", 0.125, "b", "bit", "bits"),
        linearUnit("kbit", UnitCategory.DATA_SIZE, "Kilobits", "Kbit", 125.0, "kbit", "kbits", "kilobit", "kilobits"),
        linearUnit("mbit", UnitCategory.DATA_SIZE, "Megabits", "Mbit", 125_000.0, "mbit", "mbits", "megabit", "megabits"),
        linearUnit("gbit", UnitCategory.DATA_SIZE, "Gigabits", "Gbit", 125_000_000.0, "gbit", "gbits", "gigabit", "gigabits"),
        linearUnit("tbit", UnitCategory.DATA_SIZE, "Terabits", "Tbit", 125_000_000_000.0, "tbit", "tbits", "terabit", "terabits"),
        linearUnit("byte", UnitCategory.DATA_SIZE, "Bytes", "B", 1.0, "byte", "bytes"),
        linearUnit("kb", UnitCategory.DATA_SIZE, "Kilobytes", "KB", 1e3, "kb", "kbyte", "kbytes", "kilobyte", "kilobytes"),
        linearUnit("kib", UnitCategory.DATA_SIZE, "Kibibytes", "KiB", 1024.0, "kib", "kibibyte", "kibibytes"),
        linearUnit("mb", UnitCategory.DATA_SIZE, "Megabytes", "MB", 1e6, "mb", "mbyte", "mbytes", "megabyte", "megabytes"),
        linearUnit("mib", UnitCategory.DATA_SIZE, "Mebibytes", "MiB", 1024.0.pow(2), "mib", "mebibyte", "mebibytes"),
        linearUnit("gb", UnitCategory.DATA_SIZE, "Gigabytes", "GB", 1e9, "gb", "gbyte", "gbytes", "gigabyte", "gigabytes"),
        linearUnit("gib", UnitCategory.DATA_SIZE, "Gibibytes", "GiB", 1024.0.pow(3), "gib", "gibibyte", "gibibytes"),
        linearUnit("tb", UnitCategory.DATA_SIZE, "Terabytes", "TB", 1e12, "tb", "tbyte", "tbytes", "terabyte", "terabytes"),
        linearUnit("tib", UnitCategory.DATA_SIZE, "Tebibytes", "TiB", 1024.0.pow(4), "tib", "tebibyte", "tebibytes"),
        linearUnit("pb", UnitCategory.DATA_SIZE, "Petabytes", "PB", 1e15, "pb", "pbyte", "pbytes", "petabyte", "petabytes"),
        linearUnit("pib", UnitCategory.DATA_SIZE, "Pebibytes", "PiB", 1024.0.pow(5), "pib", "pebibyte", "pebibytes"),

        // =====================================================================
        // 9. DATA TRANSFER RATE / BANDWIDTH (Base: Bits per second)
        // =====================================================================
        linearUnit("bps", UnitCategory.DATA_RATE, "Bits per second", "bps", 1.0, "bps", "bit/s", "bits/s", "b/s"),
        linearUnit("kbps", UnitCategory.DATA_RATE, "Kilobits per second", "Kbps", 1e3, "kbps", "kbit/s", "kbits/s", "kb/s"),
        linearUnit("mbps", UnitCategory.DATA_RATE, "Megabits per second", "Mbps", 1e6, "mbps", "mbit/s", "mbits/s"),
        linearUnit("gbps", UnitCategory.DATA_RATE, "Gigabits per second", "Gbps", 1e9, "gbps", "gbit/s", "gbits/s"),
        linearUnit("tbps", UnitCategory.DATA_RATE, "Terabits per second", "Tbps", 1e12, "tbps", "tbit/s", "tbits/s"),
        linearUnit("kBps", UnitCategory.DATA_RATE, "Kilobytes per second", "KB/s", 8e3, "kbyte/s", "kbytes/s", "kilobytes/s", "kilobytes per second"),
        linearUnit("mBps", UnitCategory.DATA_RATE, "Megabytes per second", "MB/s", 8e6, "mb/s", "mbyte/s", "mbytes/s", "megabytes/s", "megabytes per second"),
        linearUnit("gBps", UnitCategory.DATA_RATE, "Gigabytes per second", "GB/s", 8e9, "gb/s", "gbyte/s", "gbytes/s", "gigabytes/s", "gigabytes per second"),

        // =====================================================================
        // 10. PRESSURE (Base: Pascal)
        // =====================================================================
        linearUnit("pa", UnitCategory.PRESSURE, "Pascals", "Pa", 1.0, "pa", "pascal", "pascals"),
        linearUnit("hpa", UnitCategory.PRESSURE, "Hectopascals", "hPa", 100.0, "hpa", "hectopascal", "hectopascals"),
        linearUnit("kpa", UnitCategory.PRESSURE, "Kilopascals", "kPa", 1000.0, "kpa", "kilopascal", "kilopascals"),
        linearUnit("mpa", UnitCategory.PRESSURE, "Megapascals", "MPa", 1_000_000.0, "mpa", "megapascal", "megapascals"),
        linearUnit("bar", UnitCategory.PRESSURE, "Bars", "bar", 100_000.0, "bar", "bars"),
        linearUnit("mbar", UnitCategory.PRESSURE, "Millibars", "mbar", 100.0, "mbar", "mbars", "millibar", "millibars"),
        linearUnit("atm", UnitCategory.PRESSURE, "Atmospheres", "atm", 101_325.0, "atm", "atmosphere", "atmospheres"),
        linearUnit("psi", UnitCategory.PRESSURE, "PSI", "psi", 6894.757293168, "psi", "lb/in2", "lbf/in2"),
        linearUnit("torr", UnitCategory.PRESSURE, "Torr (mmHg)", "mmHg", 133.322368421, "torr", "mmhg", "mm hg"),
        linearUnit("inhg", UnitCategory.PRESSURE, "Inches of Mercury", "inHg", 3386.389, "inhg", "in hg"),

        // =====================================================================
        // 11. ENERGY (Base: Joule)
        // =====================================================================
        linearUnit("j", UnitCategory.ENERGY, "Joules", "J", 1.0, "j", "joule", "joules"),
        linearUnit("kj", UnitCategory.ENERGY, "Kilojoules", "kJ", 1000.0, "kj", "kilojoule", "kilojoules"),
        linearUnit("mj", UnitCategory.ENERGY, "Megajoules", "MJ", 1_000_000.0, "mj", "megajoule", "megajoules"),
        linearUnit("cal", UnitCategory.ENERGY, "Calories (gram)", "cal", 4.184, "cal", "calorie", "calories", "small calorie"),
        linearUnit("kcal", UnitCategory.ENERGY, "Kilocalories (Food)", "kcal", 4184.0, "kcal", "kcals", "kilocalorie", "kilocalories", "food calorie", "food calories"),
        linearUnit("wh", UnitCategory.ENERGY, "Watt-hours", "Wh", 3600.0, "wh", "watt hour", "watt hours", "watt-hour", "watt-hours"),
        linearUnit("kwh", UnitCategory.ENERGY, "Kilowatt-hours", "kWh", 3_600_000.0, "kwh", "kilowatt hour", "kilowatt hours", "kilowatt-hour", "kilowatt-hours"),
        linearUnit("mwh", UnitCategory.ENERGY, "Megawatt-hours", "MWh", 3_600_000_000.0, "mwh", "megawatt hour", "megawatt-hour"),
        linearUnit("ev", UnitCategory.ENERGY, "Electronvolts", "eV", 1.602176634e-19, "ev", "electronvolt", "electronvolts"),
        linearUnit("btu", UnitCategory.ENERGY, "BTU", "BTU", 1055.05585262, "btu", "btus"),
        linearUnit("ftlb", UnitCategory.ENERGY, "Foot-pounds", "ft⋅lb", 1.35581794833, "ft lb", "ft-lb", "ftlb", "foot pound", "foot-pound", "foot pounds"),
        linearUnit("erg", UnitCategory.ENERGY, "Ergs", "erg", 1e-7, "erg", "ergs"),

        // =====================================================================
        // 12. POWER (Base: Watt)
        // =====================================================================
        linearUnit("mw_power", UnitCategory.POWER, "Milliwatts", "mW", 1e-3, "mw", "milliwatt", "milliwatts"),
        linearUnit("w", UnitCategory.POWER, "Watts", "W", 1.0, "w", "watt", "watts"),
        linearUnit("kw", UnitCategory.POWER, "Kilowatts", "kW", 1000.0, "kw", "kilowatt", "kilowatts"),
        linearUnit("megawatt", UnitCategory.POWER, "Megawatts", "MW", 1_000_000.0, "megawatt", "megawatts"),
        linearUnit("gw", UnitCategory.POWER, "Gigawatts", "GW", 1_000_000_000.0, "gw", "gigawatt", "gigawatts"),
        linearUnit("hp", UnitCategory.POWER, "Horsepower", "hp", 745.699871582, "hp", "bhp", "horsepower"),
        linearUnit("ps", UnitCategory.POWER, "Metric Horsepower", "PS", 735.49875, "ps", "metric hp", "metric horsepower"),
        linearUnit("btuh", UnitCategory.POWER, "BTU per hour", "BTU/h", 0.29307107, "btu/h", "btu/hr", "btuh"),
        UnitDef(
            id = "dbm",
            category = UnitCategory.POWER,
            name = "dBm",
            symbol = "dBm",
            aliases = setOf("dbm"),
            toBase = { 10.0.pow((it - 30.0) / 10.0) },
            fromBase = { if (it > 0.0) 10.0 * log10(it) + 30.0 else Double.NEGATIVE_INFINITY }
        ),

        // =====================================================================
        // 13. ANGLE (Base: Degree)
        // =====================================================================
        linearUnit("deg", UnitCategory.ANGLE, "Degrees", "°", 1.0, "deg", "degs", "°", "degree", "degrees"),
        linearUnit("rad", UnitCategory.ANGLE, "Radians", "rad", 180.0 / Math.PI, "rad", "rads", "radian", "radians"),
        linearUnit("grad", UnitCategory.ANGLE, "Gradians", "grad", 0.9, "grad", "grads", "gon", "gradian", "gradians"),
        linearUnit("arcmin", UnitCategory.ANGLE, "Arcminutes", "′", 1.0 / 60.0, "arcmin", "amin", "arcminute", "arcminutes"),
        linearUnit("arcsec", UnitCategory.ANGLE, "Arcseconds", "″", 1.0 / 3600.0, "arcsec", "asec", "arcsecond", "arcseconds"),
        linearUnit("turn", UnitCategory.ANGLE, "Revolutions", "rev", 360.0, "turn", "turns", "rev", "revs", "revolution", "revolutions"),

        // =====================================================================
        // 14. FORCE (Base: Newton)
        // =====================================================================
        linearUnit("n", UnitCategory.FORCE, "Newtons", "N", 1.0, "n", "newton", "newtons"),
        linearUnit("kn_force", UnitCategory.FORCE, "Kilonewtons", "kN", 1000.0, "kilonewton", "kilonewtons"),
        linearUnit("dyn", UnitCategory.FORCE, "Dynes", "dyn", 1e-5, "dyn", "dyne", "dynes"),
        linearUnit("lbf", UnitCategory.FORCE, "Pound-force", "lbf", 4.4482216152605, "lbf", "pound-force", "pound force"),
        linearUnit("kgf", UnitCategory.FORCE, "Kilogram-force", "kgf", 9.80665, "kgf", "kp", "kilogram-force", "kilopond"),

        // =====================================================================
        // 15. FREQUENCY (Base: Hertz)
        // =====================================================================
        linearUnit("hz", UnitCategory.FREQUENCY, "Hertz", "Hz", 1.0, "hz", "hertz"),
        linearUnit("khz", UnitCategory.FREQUENCY, "Kilohertz", "kHz", 1e3, "khz", "kilohertz"),
        linearUnit("mhz", UnitCategory.FREQUENCY, "Megahertz", "MHz", 1e6, "mhz", "megahertz"),
        linearUnit("ghz", UnitCategory.FREQUENCY, "Gigahertz", "GHz", 1e9, "ghz", "gigahertz"),
        linearUnit("thz", UnitCategory.FREQUENCY, "Terahertz", "THz", 1e12, "thz", "terahertz"),
        linearUnit("rpm", UnitCategory.FREQUENCY, "RPM", "rpm", 1.0 / 60.0, "rpm", "r/min", "rev/min"),

        // =====================================================================
        // 16. FUEL ECONOMY (Base: Kilometers per Liter)
        // =====================================================================
        linearUnit("kml", UnitCategory.FUEL_ECONOMY, "Kilometers per Liter", "km/L", 1.0, "km/l", "kml", "km/ltr", "kilometers per liter", "kilometres per litre"),
        linearUnit("mpg", UnitCategory.FUEL_ECONOMY, "Miles per Gallon (US)", "mpg", 1.609344 / 3.785411784, "mpg", "us mpg", "mi/gal", "miles per gallon"),
        linearUnit("uk_mpg", UnitCategory.FUEL_ECONOMY, "Miles per Gallon (UK)", "imp mpg", 1.609344 / 4.54609, "uk mpg", "imp mpg", "imperial mpg"),
        UnitDef(
            id = "l100km",
            category = UnitCategory.FUEL_ECONOMY,
            name = "Liters per 100 km",
            symbol = "L/100km",
            aliases = setOf("l/100km", "l/100 km", "lp100km", "liters per 100km", "litres per 100km"),
            toBase = { if (it != 0.0) 100.0 / it else Double.NaN },
            fromBase = { if (it != 0.0) 100.0 / it else Double.NaN }
        )
    )

    // =========================================================================
    // Currency Definitions & Aliases
    // =========================================================================

    data class CurrencyMeta(
        val code: String,
        val name: String,
        val symbol: String,
        val zeroDecimal: Boolean = false
    )

    val CURRENCIES: Map<String, CurrencyMeta> = listOf(
        CurrencyMeta("USD", "US Dollar", "$"),
        CurrencyMeta("EUR", "Euro", "€"),
        CurrencyMeta("GBP", "British Pound", "£"),
        CurrencyMeta("INR", "Indian Rupee", "₹"),
        CurrencyMeta("JPY", "Japanese Yen", "¥", zeroDecimal = true),
        CurrencyMeta("CAD", "Canadian Dollar", "C$"),
        CurrencyMeta("AUD", "Australian Dollar", "A$"),
        CurrencyMeta("CHF", "Swiss Franc", "CHF "),
        CurrencyMeta("CNY", "Chinese Yuan", "¥"),
        CurrencyMeta("HKD", "Hong Kong Dollar", "HK$"),
        CurrencyMeta("SGD", "Singapore Dollar", "S$"),
        CurrencyMeta("NZD", "New Zealand Dollar", "NZ$"),
        CurrencyMeta("KRW", "South Korean Won", "₩", zeroDecimal = true),
        CurrencyMeta("SEK", "Swedish Krona", "kr "),
        CurrencyMeta("NOK", "Norwegian Krone", "kr "),
        CurrencyMeta("DKK", "Danish Krone", "kr "),
        CurrencyMeta("PLN", "Polish Złoty", "zł"),
        CurrencyMeta("CZK", "Czech Koruna", "Kč "),
        CurrencyMeta("HUF", "Hungarian Forint", "Ft ", zeroDecimal = true),
        CurrencyMeta("RON", "Romanian Leu", "lei "),
        CurrencyMeta("BGN", "Bulgarian Lev", "лв "),
        CurrencyMeta("ISK", "Icelandic Króna", "kr ", zeroDecimal = true),
        CurrencyMeta("TRY", "Turkish Lira", "₺"),
        CurrencyMeta("ILS", "Israeli Shekel", "₪"),
        CurrencyMeta("ZAR", "South African Rand", "R "),
        CurrencyMeta("BRL", "Brazilian Real", "R$"),
        CurrencyMeta("MXN", "Mexican Peso", "MX$"),
        CurrencyMeta("ARS", "Argentine Peso", "AR$"),
        CurrencyMeta("CLP", "Chilean Peso", "CL$", zeroDecimal = true),
        CurrencyMeta("COP", "Colombian Peso", "CO$"),
        CurrencyMeta("PEN", "Peruvian Sol", "S/ "),
        CurrencyMeta("UYU", "Uruguayan Peso", "\$U "),
        CurrencyMeta("AED", "UAE Dirham", "AED "),
        CurrencyMeta("SAR", "Saudi Riyal", "SAR "),
        CurrencyMeta("QAR", "Qatari Riyal", "QAR "),
        CurrencyMeta("KWD", "Kuwaiti Dinar", "KD "),
        CurrencyMeta("BHD", "Bahraini Dinar", "BD "),
        CurrencyMeta("OMR", "Omani Rial", "OMR "),
        CurrencyMeta("JOD", "Jordanian Dinar", "JD "),
        CurrencyMeta("EGP", "Egyptian Pound", "E£"),
        CurrencyMeta("MAD", "Moroccan Dirham", "MAD "),
        CurrencyMeta("NGN", "Nigerian Naira", "₦"),
        CurrencyMeta("KES", "Kenyan Shilling", "KSh "),
        CurrencyMeta("GHS", "Ghanaian Cedi", "GH₵"),
        CurrencyMeta("TZS", "Tanzanian Shilling", "TSh "),
        CurrencyMeta("UGX", "Ugandan Shilling", "USh ", zeroDecimal = true),
        CurrencyMeta("THB", "Thai Baht", "฿"),
        CurrencyMeta("MYR", "Malaysian Ringgit", "RM"),
        CurrencyMeta("IDR", "Indonesian Rupiah", "Rp ", zeroDecimal = true),
        CurrencyMeta("PHP", "Philippine Peso", "₱"),
        CurrencyMeta("VND", "Vietnamese Đồng", "₫", zeroDecimal = true),
        CurrencyMeta("TWD", "New Taiwan Dollar", "NT$"),
        CurrencyMeta("PKR", "Pakistani Rupee", "Rs "),
        CurrencyMeta("BDT", "Bangladeshi Taka", "৳"),
        CurrencyMeta("LKR", "Sri Lankan Rupee", "Rs "),
        CurrencyMeta("NPR", "Nepalese Rupee", "Rs "),
        CurrencyMeta("RUB", "Russian Ruble", "₽"),
        CurrencyMeta("UAH", "Ukrainian Hryvnia", "₴"),
        CurrencyMeta("KZT", "Kazakhstani Tenge", "₸"),
        CurrencyMeta("UZS", "Uzbekistani Som", "so'm "),
        CurrencyMeta("GEL", "Georgian Lari", "₾"),
        CurrencyMeta("AMD", "Armenian Dram", "֏"),
        CurrencyMeta("AZN", "Azerbaijani Manat", "₼"),
        CurrencyMeta("BYN", "Belarusian Ruble", "Br "),
        CurrencyMeta("MDL", "Moldovan Leu", "MDL "),
        CurrencyMeta("ALL", "Albanian Lek", "L "),
        CurrencyMeta("MKD", "Macedonian Denar", "ден "),
        CurrencyMeta("RSD", "Serbian Dinar", "дин "),
        CurrencyMeta("BAM", "Bosnia-Herzegovina Mark", "KM "),
        CurrencyMeta("CRC", "Costa Rican Colón", "₡"),
        CurrencyMeta("DOP", "Dominican Peso", "RD$"),
        CurrencyMeta("GTQ", "Guatemalan Quetzal", "Q"),
        CurrencyMeta("HNL", "Honduran Lempira", "L"),
        CurrencyMeta("NIO", "Nicaraguan Córdoba", "C$"),
        CurrencyMeta("PAB", "Panamanian Balboa", "B/."),
        CurrencyMeta("PYG", "Paraguayan Guarani", "₲", zeroDecimal = true),
        CurrencyMeta("BOB", "Bolivian Boliviano", "Bs."),
        CurrencyMeta("VES", "Venezuelan Bolívar", "Bs."),
        CurrencyMeta("JMD", "Jamaican Dollar", "J$"),
        CurrencyMeta("TTD", "Trinidad & Tobago Dollar", "TT$"),
        CurrencyMeta("BBD", "Barbadian Dollar", "Bds$"),
        CurrencyMeta("BSD", "Bahamian Dollar", "B$"),
        CurrencyMeta("BZD", "Belize Dollar", "BZ$"),
        CurrencyMeta("HTG", "Haitian Gourde", "G "),
        CurrencyMeta("XCD", "East Caribbean Dollar", "EC$"),
        CurrencyMeta("AWG", "Aruban Florin", "ƒ"),
        CurrencyMeta("ANG", "Netherlands Antillean Guilder", "ƒ"),
        CurrencyMeta("KYD", "Cayman Islands Dollar", "CI$"),
        CurrencyMeta("BMD", "Bermudan Dollar", "BD$"),
        CurrencyMeta("DZD", "Algerian Dinar", "DA "),
        CurrencyMeta("TND", "Tunisian Dinar", "DT "),
        CurrencyMeta("LYD", "Libyan Dinar", "LD "),
        CurrencyMeta("ETB", "Ethiopian Birr", "Br "),
        CurrencyMeta("ZMW", "Zambian Kwacha", "ZK "),
        CurrencyMeta("BWP", "Botswanan Pula", "P"),
        CurrencyMeta("MUR", "Mauritian Rupee", "Rs "),
        CurrencyMeta("NAD", "Namibian Dollar", "N$"),
        CurrencyMeta("SCR", "Seychellois Rupee", "SR "),
        CurrencyMeta("MZN", "Mozambican Metical", "MT "),
        CurrencyMeta("AOA", "Angolan Kwanza", "Kz "),
        CurrencyMeta("XOF", "West African CFA Franc", "CFA ", zeroDecimal = true),
        CurrencyMeta("XAF", "Central African CFA Franc", "FCFA ", zeroDecimal = true),
        CurrencyMeta("CDF", "Congolese Franc", "FC "),
        CurrencyMeta("RWF", "Rwandan Franc", "RF ", zeroDecimal = true),
        CurrencyMeta("MWK", "Malawian Kwacha", "MK "),
        CurrencyMeta("LBP", "Lebanese Pound", "L£"),
        CurrencyMeta("IQD", "Iraqi Dinar", "IQD "),
        CurrencyMeta("IRR", "Iranian Rial", "IRR ", zeroDecimal = true),
        CurrencyMeta("SYP", "Syrian Pound", "£S"),
        CurrencyMeta("YER", "Yemeni Rial", "﷼"),
        CurrencyMeta("AFN", "Afghan Afghani", "؋"),
        CurrencyMeta("MMK", "Myanmar Kyat", "K "),
        CurrencyMeta("KHR", "Cambodian Riel", "៛"),
        CurrencyMeta("LAK", "Laotian Kip", "₭", zeroDecimal = true),
        CurrencyMeta("MNT", "Mongolian Tögrög", "₮"),
        CurrencyMeta("BND", "Brunei Dollar", "B$"),
        CurrencyMeta("MOP", "Macanese Pataca", "MOP$"),
        CurrencyMeta("MVR", "Maldivian Rufiyaa", "Rf "),
        CurrencyMeta("FJD", "Fijian Dollar", "FJ$"),
        CurrencyMeta("PGK", "Papua New Guinean Kina", "K "),
        CurrencyMeta("XPF", "CFP Franc", "₣", zeroDecimal = true),
        CurrencyMeta("XAU", "Gold (Troy Ounce)", "XAU "),
        CurrencyMeta("XAG", "Silver (Troy Ounce)", "XAG "),
        CurrencyMeta("BTC", "Bitcoin", "₿"),
        CurrencyMeta("ETH", "Ethereum", "Ξ"),
        CurrencyMeta("SOL", "Solana", "SOL ")
    ).associateBy { it.code }

    private val CURRENCY_ALIASES: Map<String, String> = buildMap {
        for (c in CURRENCIES.values) {
            put(c.code.lowercase(Locale.ROOT), c.code)
            put(c.name.lowercase(Locale.ROOT), c.code)
            put("${c.name.lowercase(Locale.ROOT)}s", c.code)
        }
        put("$", "USD"); put("us$", "USD"); put("usd", "USD"); put("dollar", "USD"); put("dollars", "USD")
        put("us dollar", "USD"); put("us dollars", "USD"); put("buck", "USD"); put("bucks", "USD")
        put("€", "EUR"); put("eur", "EUR"); put("euro", "EUR"); put("euros", "EUR")
        put("£", "GBP"); put("gbp", "GBP"); put("quid", "GBP"); put("sterling", "GBP")
        put("british pound", "GBP"); put("british pounds", "GBP"); put("pound sterling", "GBP")
        put("uk pound", "GBP"); put("uk pounds", "GBP")
        put("₹", "INR"); put("inr", "INR"); put("rs", "INR"); put("rs.", "INR"); put("rupee", "INR"); put("rupees", "INR")
        put("rupiya", "INR"); put("indian rupee", "INR"); put("indian rupees", "INR")
        put("¥", "JPY"); put("jpy", "JPY"); put("yen", "JPY"); put("yens", "JPY"); put("japanese yen", "JPY")
        put("cny", "CNY"); put("rmb", "CNY"); put("yuan", "CNY"); put("yuans", "CNY"); put("renminbi", "CNY"); put("chinese yuan", "CNY")
        put("c$", "CAD"); put("ca$", "CAD"); put("cad", "CAD"); put("canadian dollar", "CAD"); put("canadian dollars", "CAD")
        put("a$", "AUD"); put("au$", "AUD"); put("aud", "AUD"); put("australian dollar", "AUD"); put("australian dollars", "AUD")
        put("chf", "CHF"); put("sfr", "CHF"); put("franc", "CHF"); put("francs", "CHF"); put("swiss franc", "CHF"); put("swiss francs", "CHF")
        put("hk$", "HKD"); put("hkd", "HKD")
        put("s$", "SGD"); put("sgd", "SGD")
        put("nz$", "NZD"); put("nzd", "NZD")
        put("₩", "KRW"); put("krw", "KRW"); put("won", "KRW"); put("wons", "KRW"); put("korean won", "KRW"); put("south korean won", "KRW")
        put("₺", "TRY"); put("tl", "TRY"); put("lira", "TRY"); put("turkish lira", "TRY")
        put("₪", "ILS"); put("ils", "ILS"); put("nis", "ILS"); put("shekel", "ILS"); put("shekels", "ILS")
        put("r$", "BRL"); put("brl", "BRL"); put("real", "BRL"); put("reais", "BRL")
        put("mx$", "MXN"); put("mxn", "MXN"); put("peso", "MXN"); put("pesos", "MXN")
        put("aed", "AED"); put("dhs", "AED"); put("dirham", "AED"); put("dirhams", "AED"); put("uae dirham", "AED"); put("uae dirhams", "AED")
        put("sar", "SAR"); put("riyal", "SAR"); put("riyals", "SAR")
        put("₦", "NGN"); put("ngn", "NGN"); put("naira", "NGN")
        put("฿", "THB"); put("thb", "THB"); put("baht", "THB"); put("bhat", "THB"); put("bahts", "THB"); put("bhats", "THB")
        put("thai baht", "THB"); put("thai bhat", "THB")
        put("rm", "MYR"); put("myr", "MYR"); put("ringgit", "MYR"); put("ringgits", "MYR")
        put("rp", "IDR"); put("idr", "IDR"); put("rupiah", "IDR"); put("rupiahs", "IDR")
        put("₱", "PHP"); put("php", "PHP")
        put("₫", "VND"); put("vnd", "VND"); put("dong", "VND")
        put("bdt", "BDT"); put("taka", "BDT")
        put("nt$", "TWD"); put("twd", "TWD")
        put("₽", "RUB"); put("rub", "RUB"); put("ruble", "RUB"); put("rubles", "RUB"); put("rouble", "RUB"); put("roubles", "RUB")
        put("₴", "UAH"); put("uah", "UAH"); put("hryvnia", "UAH")
        put("zł", "PLN"); put("zl", "PLN"); put("pln", "PLN"); put("zloty", "PLN")
        put("kč", "CZK"); put("czk", "CZK"); put("koruna", "CZK")
        put("ft_huf", "HUF"); put("huf", "HUF"); put("forint", "HUF")
        put("rand", "ZAR"); put("zar", "ZAR")
        put("xau", "XAU"); put("gold", "XAU")
        put("xag", "XAG"); put("silver", "XAG")
        put("₿", "BTC"); put("btc", "BTC"); put("bitcoin", "BTC"); put("bitcoins", "BTC")
        put("Ξ", "ETH"); put("eth", "ETH"); put("ether", "ETH"); put("ethereum", "ETH")
        put("sol", "SOL"); put("solana", "SOL")
    }

    fun resolveCurrencyCode(rawToken: String): String? {
        val clean = rawToken.trim().lowercase(Locale.ROOT)
        if (clean.isEmpty()) return null
        CURRENCY_ALIASES[clean]?.let { return it }
        if (clean.length == 3 && clean.all { it in 'a'..'z' }) {
            val upper = clean.uppercase(Locale.ROOT)
            if (CURRENCIES.containsKey(upper) || CurrencyRateService.hasCurrency(upper)) {
                return upper
            }
        }
        return null
    }

    fun currencySymbol(code: String): String {
        val upper = code.uppercase(Locale.ROOT)
        return CURRENCIES[upper]?.symbol ?: "$upper "
    }

    fun currencyName(code: String): String {
        val upper = code.uppercase(Locale.ROOT)
        return CURRENCIES[upper]?.name ?: upper
    }

    fun allSupportedCurrencies(): List<CurrencyMeta> {
        val liveKeys = CurrencyRateService.getAllRatesSnapshot().keys
        val extra = liveKeys.filterNot { CURRENCIES.containsKey(it) }
            .map { code -> CurrencyMeta(code = code, name = code, symbol = "$code ") }
        return (CURRENCIES.values + extra).sortedBy { it.code }
    }

    // =========================================================================
    // Main Entry Point
    // =========================================================================

    private val COMPOUND_FEET_INCHES = Regex(
        """^\s*([+-]?\d+(?:\.\d+)?)\s*(?:ft|foot|feet|'|′)\s*(\d+(?:\.\d+)?)\s*(?:in|inch|inches|"|″)?\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val COMPOUND_STONE_POUNDS = Regex(
        """^\s*([+-]?\d+(?:\.\d+)?)\s*(?:st|stone|stones)\s*(\d+(?:\.\d+)?)\s*(?:lb|lbs|pound|pounds)?\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val COMPOUND_POUNDS_OUNCES = Regex(
        """^\s*([+-]?\d+(?:\.\d+)?)\s*(?:lb|lbs|pound|pounds)\s*(\d+(?:\.\d+)?)\s*(?:oz|ounce|ounces)\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val CONNECTOR_REGEX = Regex(
        """^(.+?)\s+(?:to|in|into|as|->|→)\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )

    private val ARROW_NO_SPACE_REGEX = Regex(
        """^(.+?)(?:->|→)(.+)$"""
    )

    /**
     * Evaluates a conversion query.
     *
     * @param rawQuery The user's input (either the argument after the `convert` prefix, or the full search query).
     * @param hasExplicitPrefix True if the user typed the `convert` (or `conv`) command prefix.
     *                          When true, single-side queries like `convert 100 c` or `convert 100 usd` also work.
     *                          When false, an explicit connector (`to`, `in`, `into`, `as`, `->`) is required.
     */
    fun evaluate(rawQuery: String, hasExplicitPrefix: Boolean): ConversionOutcome? {
        var text = rawQuery.trim()
        if (text.isEmpty()) return null
        if (!hasExplicitPrefix && text.startsWith("=")) {
            text = text.removePrefix("=").trim()
            if (text.isEmpty()) return null
        }

        // 1. Try splitting on explicit connector (`to`, `in`, `into`, `as`, `->`, `→`)
        val splitPair = splitOnConnector(text)
        if (splitPair != null) {
            val (leftSide, rightSide) = splitPair
            evaluateExplicitTarget(leftSide.trim(), rightSide.trim())?.let { return it }
        }

        // 1b. Currency pairs (`EUR/USD`, `100 EUR/USD`; plus concatenated `EURUSD` when prefixed)
        evaluateCurrencyPair(text, allowConcatenated = hasExplicitPrefix)?.let { return it }

        // 2. If user typed the explicit `convert` prefix, also support:
        //    a) `convert rates` / `convert fx` benchmark FX table
        //    b) Space-separated `<amount> <fromUnit> <toUnit>` (e.g., `convert 100 usd inr`, `convert 100 c f`)
        //    c) Single-unit/currency smart overview (e.g., `convert 100 c`, `convert 100 usd`, `convert usd`)
        if (hasExplicitPrefix) {
            val normalizedKeyword = text.lowercase(Locale.ROOT).replace("\\s+".toRegex(), " ")
            if (normalizedKeyword in setOf("rates", "fx", "fx rates", "exchange rates")) {
                buildMajorFxRatesOverview()?.let { return it }
            }
            evaluateSpaceSeparatedTarget(text)?.let { return it }
            evaluateSingleSourceOverview(text)?.let { return it }
        }

        return null
    }

    private fun splitOnConnector(text: String): Pair<String, String>? {
        // Handle special case where target unit is "in" (inches): e.g. "100 cm in in" or "100 cm to in"
        // Match the LAST connector so "10 in in cm" splits into ("10 in", "cm") and "100 cm in in" splits into ("100 cm", "in")
        val tokens = text.split("\\s+".toRegex())
        if (tokens.size >= 3) {
            for (i in tokens.size - 2 downTo 1) {
                val tok = tokens[i].lowercase(Locale.ROOT)
                if (tok in setOf("to", "in", "into", "as", "->", "→")) {
                    // Notice: if i == 1 and tokens[0] is just a bare number (e.g. "10 in to cm"),
                    // then "in" at index 1 is the source unit (inches) and "to" at index 2 is the connector!
                    if (tok == "in" && i == 1 && tokens[0].toDoubleOrNull() != null && tokens.size >= 4) {
                        val nextTok = tokens[2].lowercase(Locale.ROOT)
                        if (nextTok in setOf("to", "in", "into", "as", "->", "→")) {
                            continue
                        }
                    }
                    val left = tokens.subList(0, i).joinToString(" ")
                    val right = tokens.subList(i + 1, tokens.size).joinToString(" ")
                    if (left.isNotBlank() && right.isNotBlank()) {
                        return left to right
                    }
                }
            }
        }
        val m = CONNECTOR_REGEX.matchEntire(text)
        if (m != null) {
            return m.groupValues[1] to m.groupValues[2]
        }
        val arrow = ARROW_NO_SPACE_REGEX.matchEntire(text)
        if (arrow != null) {
            return arrow.groupValues[1] to arrow.groupValues[2]
        }
        return null
    }

    private fun evaluateExplicitTarget(leftRaw: String, rightRaw: String): ConversionOutcome? {
        val targetClean = rightRaw.trim().trimEnd('?')
        if (targetClean.isEmpty()) return null

        // A. Number Base & Roman Numeral conversions (e.g. "255 to hex", "0xff to dec", "2026 to roman", "MCMLXXXIV to dec")
        evaluateBaseConversion(leftRaw, targetClean)?.let { return it }

        // B. Compound source units (e.g., "5ft 10in to cm", "11st 4lb to kg", "5lb 8oz to g")
        evaluateCompoundToTarget(leftRaw, targetClean)?.let { return it }

        // C. Parse `<value> <fromUnit>` from leftRaw; if absent, check bare currency-to-currency (`thb to inr`, `baht to rs`)
        val parsedSource = parseAmountAndUnitToken(leftRaw)
        if (parsedSource == null) {
            val fromCur = resolveCurrencyWithContext(leftRaw, targetClean)
            val toCur = resolveCurrencyWithContext(targetClean, leftRaw)
            if (fromCur != null && toCur != null && fromCur != toCur) {
                return buildPairResultOrAmount(null, fromCur, toCur)
            }
            return null
        }
        val (amount, fromToken, caseSensitiveFrom) = parsedSource

        // D. Check Currency conversion first (or disambiguate `pound` / `pounds`)
        val fromCur = resolveCurrencyWithContext(fromToken, targetClean)
        val toCur = resolveCurrencyWithContext(targetClean, fromToken)
        if (fromCur != null && toCur != null) {
            return buildCurrencyConversion(amount, fromCur, toCur)
        }

        // E. Physical / Digital Unit conversion
        val (fromUnit, toUnit) = resolveCompatibleUnits(fromToken, caseSensitiveFrom, targetClean) ?: return null
        val preferCompoundFtIn = targetClean.lowercase(Locale.ROOT).replace("\\s+".toRegex(), " ") in setOf("ft in", "feet inches")
        return buildUnitConversion(amount, fromUnit, toUnit, preferCompoundFeetInches = preferCompoundFtIn)
    }

    private fun evaluateSpaceSeparatedTarget(text: String): ConversionOutcome? {
        // e.g. "100 usd inr", "100 c f", "70 kg lb", "$100 inr"
        val tokens = text.trim().split("\\s+".toRegex())
        if (tokens.size < 2) return null
        val lastToken = tokens.last()
        val leftPart = tokens.dropLast(1).joinToString(" ")
        return evaluateExplicitTarget(leftPart, lastToken)
    }

    private fun evaluateSingleSourceOverview(text: String): ConversionOutcome? {
        // 1. Compound feet + inches (e.g. "5ft 10in", "5'10\"")
        COMPOUND_FEET_INCHES.matchEntire(text)?.let { m ->
            val ft = m.groupValues[1].toDoubleOrNull() ?: return null
            val inch = m.groupValues[2].toDoubleOrNull() ?: return null
            val totalInches = if (ft < 0) ft * 12.0 - inch else ft * 12.0 + inch
            val meters = totalInches * 0.0254
            val cm = meters * 100.0
            return ConversionOutcome(
                title = "${fmtNum(ft)} ft ${fmtNum(inch)} in (${fmtNum(totalInches)} in)",
                primaryValue = "${fmtNum(cm)} cm",
                badge = UnitCategory.LENGTH.badgeLabel,
                subtitle = "${fmtNum(meters)} m  •  ${fmtNum(totalInches / 12.0)} ft",
                copyText = fmtPlain(cm)
            )
        }

        // 2. Compound stone + pounds (e.g. "11st 4lb")
        COMPOUND_STONE_POUNDS.matchEntire(text)?.let { m ->
            val st = m.groupValues[1].toDoubleOrNull() ?: return null
            val lb = m.groupValues[2].toDoubleOrNull() ?: return null
            val totalLb = st * 14.0 + lb
            val kg = totalLb * 0.45359237
            return ConversionOutcome(
                title = "${fmtNum(st)} st ${fmtNum(lb)} lb",
                primaryValue = "${fmtNum(kg)} kg",
                badge = UnitCategory.MASS.badgeLabel,
                subtitle = "${fmtNum(totalLb)} lb  •  ${fmtNum(kg * 1000.0)} g",
                copyText = fmtPlain(kg)
            )
        }

        // 3. Compound pounds + ounces (e.g. "5lb 8oz")
        COMPOUND_POUNDS_OUNCES.matchEntire(text)?.let { m ->
            val lb = m.groupValues[1].toDoubleOrNull() ?: return null
            val oz = m.groupValues[2].toDoubleOrNull() ?: return null
            val totalLb = lb + oz / 16.0
            val kg = totalLb * 0.45359237
            val g = kg * 1000.0
            return ConversionOutcome(
                title = "${fmtNum(lb)} lb ${fmtNum(oz)} oz",
                primaryValue = if (kg >= 1.0) "${fmtNum(kg)} kg" else "${fmtNum(g)} g",
                badge = UnitCategory.MASS.badgeLabel,
                subtitle = "${fmtNum(g)} g  •  ${fmtNum(totalLb)} lb",
                copyText = fmtPlain(if (kg >= 1.0) kg else g)
            )
        }

        // 4. Hex or Binary literal without target (e.g. "convert 0xff", "convert 0b101010")
        val cleanLower = text.trim().lowercase(Locale.ROOT)
        if (cleanLower.startsWith("0x") && cleanLower.length > 2) {
            val v = cleanLower.removePrefix("0x").toLongOrNull(16)
            if (v != null) {
                return ConversionOutcome(
                    title = "${text.trim()} (Hexadecimal)",
                    primaryValue = "$v (Decimal)",
                    badge = UnitCategory.NUMBER_BASE.badgeLabel,
                    subtitle = "Bin: 0b${v.toString(2)}  •  Oct: 0o${v.toString(8)}",
                    copyText = v.toString()
                )
            }
        }
        if (cleanLower.startsWith("0b") && cleanLower.length > 2) {
            val v = cleanLower.removePrefix("0b").toLongOrNull(2)
            if (v != null) {
                return ConversionOutcome(
                    title = "${text.trim()} (Binary)",
                    primaryValue = "$v (Decimal)",
                    badge = UnitCategory.NUMBER_BASE.badgeLabel,
                    subtitle = "Hex: 0x${v.toString(16).uppercase(Locale.ROOT)}  •  Oct: 0o${v.toString(8)}",
                    copyText = v.toString()
                )
            }
        }

        // 5. Standard `<amount> <unitOrCurrency>` or bare currency rate overview (`convert usd`)
        val parsed = parseAmountAndUnitToken(text)
        if (parsed == null) {
            val trimmed = text.trim()
            val curCode = resolveCurrencyCode(trimmed)
            if (curCode != null) {
                val lower = trimmed.lowercase(Locale.ROOT)
                val unitCollision = resolveSingleUnit(lower, trimmed) != null && trimmed.none { it.isUpperCase() }
                if (!unitCollision) {
                    return buildCurrencyRateOverview(curCode)
                }
            }
            return null
        }
        val (amount, unitToken, caseSensitiveToken) = parsed

        // Check if it's an unambiguous currency symbol or currency code
        // (Note: bare "c", "f", "k", "m", "g", "s", "l", "w", "n", "b" prefer physical units unless currency-specific)
        val unitMatch = resolveSingleUnit(unitToken, caseSensitiveToken)
        val currencyMatch = resolveCurrencyCode(unitToken)

        val preferCurrency = unitMatch == null || unitToken.lowercase(Locale.ROOT) in setOf(
            "$", "€", "£", "₹", "¥", "₩", "₺", "₽", "₦", "฿", "₱", "₫", "₪", "₿",
            "usd", "eur", "inr", "gbp", "jpy", "cad", "aud", "chf", "cny", "krw", "aed", "sar", "sgd",
            "hkd", "nzd", "brl", "mxn", "zar", "try", "rub", "thb", "btc", "eth", "sol",
            "dollar", "dollars", "euro", "euros", "rupee", "rupees", "rs", "yen",
            "baht", "bhat", "yuan", "rmb", "franc", "francs", "won", "dirham", "dirhams",
            "ringgit", "rupiah", "dong", "lira", "shekel", "ruble"
        )

        if (currencyMatch != null && preferCurrency) {
            return buildMultiCurrencyOverview(amount, currencyMatch)
        }

        if (unitMatch != null) {
            val defaultTargets = defaultTargetUnitsFor(unitMatch)
            if (defaultTargets.isNotEmpty()) {
                val primaryTarget = defaultTargets.first()
                val primaryOut = buildUnitConversion(amount, unitMatch, primaryTarget) ?: return null
                if (defaultTargets.size == 1) return primaryOut

                val baseVal = unitMatch.toBase(amount)
                val extras = defaultTargets.drop(1).mapNotNull { alt ->
                    val v = alt.fromBase(baseVal)
                    if (v.isFinite()) formatUnitValue(v, alt) else null
                }
                val extraSubtitle = if (extras.isNotEmpty()) {
                    extras.joinToString("  •  ")
                } else primaryOut.subtitle
                return primaryOut.copy(subtitle = extraSubtitle)
            }
        }

        if (currencyMatch != null) {
            return buildMultiCurrencyOverview(amount, currencyMatch)
        }

        return null
    }

    // =========================================================================
    // Amount + Unit Tokenizer (handles $100, 100$, 100k usd, 10 lakh inr, -40c)
    // =========================================================================

    private val LEADING_SYMBOL_NUMBER = Regex(
        """^\s*(US\$|CA\$|AU\$|NZ\$|HK\$|NT\$|MX\$|AR\$|CL\$|CO\$|C\$|A\$|S\$|R\$|E£|[\$€£₹¥₩₺₽₦฿₱₫₪₴₸₼₾֏₿Ξ])\s*([+-]?\d[\d,]*(?:\.\d+)?)\s*(k|m|b|t|lakh|lakhs|lac|lacs|crore|crores|cr|million|billion)?\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val TRAILING_SYMBOL_NUMBER = Regex(
        """^\s*([+-]?\d[\d,]*(?:\.\d+)?)\s*(k|m|b|t|lakh|lakhs|lac|lacs|crore|crores|cr|million|billion)?\s*(US\$|CA\$|AU\$|NZ\$|HK\$|NT\$|MX\$|AR\$|CL\$|CO\$|C\$|A\$|S\$|R\$|E£|[\$€£₹¥₩₺₽₦฿₱₫₪₴₸₼₾֏₿Ξ])\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val NUMBER_WITH_MULTIPLIER_AND_UNIT = Regex(
        """^\s*([+-]?\d[\d,]*(?:\.\d+)?(?:[eE][+-]?\d+)?)\s*(?:(k|m|b|t|lakh|lakhs|lac|lacs|crore|crores|cr|million|billion)\s+)?(.+?)\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val LEADING_CURRENCY_CODE_NUMBER = Regex(
        """^\s*([a-zA-Z.]{2,10})\s+([+-]?\d[\d,]*(?:\.\d+)?)\s*(k|m|b|t|lakh|lakhs|lac|lacs|crore|crores|cr|million|billion)?\s*$""",
        RegexOption.IGNORE_CASE
    )

    private fun multiplierValue(mult: String): Double? = when (mult.lowercase(Locale.ROOT)) {
        "k", "thousand" -> 1e3
        "m", "million", "mn" -> 1e6
        "b", "billion", "bn" -> 1e9
        "t", "trillion", "tn" -> 1e12
        "lakh", "lakhs", "lac", "lacs" -> 1e5
        "crore", "crores", "cr" -> 1e7
        else -> null
    }

    /**
     * Parses `leftRaw` into `(numericAmount, normalizedUnitToken, caseSensitiveUnitToken)`.
     */
    private fun parseAmountAndUnitToken(leftRaw: String): Triple<Double, String, String>? {
        val trimmed = leftRaw.trim()
        if (trimmed.isEmpty()) return null

        // Case 1: Leading currency symbol e.g. "$100", "₹5,000", "€50k", "C$250"
        LEADING_SYMBOL_NUMBER.matchEntire(trimmed)?.let { m ->
            val sym = m.groupValues[1].trim()
            val num = m.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
            val multStr = m.groupValues[3]
            val mult = if (multStr.isNotEmpty()) multiplierValue(multStr) ?: 1.0 else 1.0
            return Triple(num * mult, sym.lowercase(Locale.ROOT), sym)
        }

        // Case 1b: Trailing currency symbol e.g. "100$", "50k€", "5000₹"
        TRAILING_SYMBOL_NUMBER.matchEntire(trimmed)?.let { m ->
            val num = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val multStr = m.groupValues[2]
            val mult = if (multStr.isNotEmpty()) multiplierValue(multStr) ?: 1.0 else 1.0
            val sym = m.groupValues[3].trim()
            return Triple(num * mult, sym.lowercase(Locale.ROOT), sym)
        }

        // Case 2: Leading currency symbol PLUS trailing code e.g. "$100 CAD" or "$100k USD"
        val leadingSymWithCode = Regex(
            """^\s*([\$€£₹¥₩₺₽₦฿₱₫₪₿])\s*([+-]?\d[\d,]*(?:\.\d+)?)\s*(k|m|b|lakh|lakhs|lac|lacs|crore|crores|cr|million|billion)?\s+([a-zA-Z]{3})\s*$""",
            RegexOption.IGNORE_CASE
        ).matchEntire(trimmed)
        if (leadingSymWithCode != null) {
            val num = leadingSymWithCode.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
            val multStr = leadingSymWithCode.groupValues[3]
            val mult = if (multStr.isNotEmpty()) multiplierValue(multStr) ?: 1.0 else 1.0
            val code = leadingSymWithCode.groupValues[4]
            return Triple(num * mult, code.lowercase(Locale.ROOT), code)
        }

        // Case 3: Number + optional multiplier + unit (e.g. "100k usd", "100 km", "72°F", "10 lakh inr")
        val m = NUMBER_WITH_MULTIPLIER_AND_UNIT.matchEntire(trimmed)
        if (m != null) {
            val baseNum = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val maybeMult = m.groupValues[2]
            val restUnit = m.groupValues[3].trim()

            if (maybeMult.isNotEmpty()) {
                val multFactor = multiplierValue(maybeMult)
                if (multFactor != null && (resolveSingleUnit(restUnit.lowercase(Locale.ROOT), restUnit) != null || resolveCurrencyCode(restUnit) != null)) {
                    return Triple(baseNum * multFactor, restUnit.lowercase(Locale.ROOT), restUnit)
                }
                val fullUnit = "$maybeMult $restUnit".trim()
                return Triple(baseNum, fullUnit.lowercase(Locale.ROOT), fullUnit)
            }

            return Triple(baseNum, restUnit.lowercase(Locale.ROOT), restUnit)
        }

        // Case 4: Leading currency word/code + number (e.g. "rs 500", "usd 100", "thb 1000")
        LEADING_CURRENCY_CODE_NUMBER.matchEntire(trimmed)?.let { lc ->
            val token = lc.groupValues[1].trim()
            if (resolveCurrencyCode(token) != null) {
                val num = lc.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null
                val multStr = lc.groupValues[3]
                val mult = if (multStr.isNotEmpty()) multiplierValue(multStr) ?: 1.0 else 1.0
                return Triple(num * mult, token.lowercase(Locale.ROOT), token)
            }
        }

        return null
    }

    // =========================================================================
    // Unit & Currency Resolution with Smart Context Disambiguation
    // =========================================================================

    private fun resolveCurrencyWithContext(tokenRaw: String, otherSideRaw: String): String? {
        val clean = tokenRaw.trim().lowercase(Locale.ROOT)
        val otherClean = otherSideRaw.trim().lowercase(Locale.ROOT)
        if (clean in setOf("pound", "pounds", "lb", "lbs", "uk pound", "uk pounds")) {
            // "pound" / "pounds" is GBP only if the other side is a known currency (not a mass unit)
            if (clean in setOf("pound", "pounds", "uk pound", "uk pounds")) {
                val otherCur = resolveCurrencyCode(otherClean)
                if (otherCur != null) return "GBP"
            }
            return null
        }
        return resolveCurrencyCode(clean)
    }

    private fun resolveSingleUnit(lowerToken: String, caseSensitiveToken: String): UnitDef? {
        val cleanLower = lowerToken.trim()
        val cleanCase = caseSensitiveToken.trim()

        // Case-sensitive disambiguations first:
        when (cleanCase) {
            "B" -> return UNITS.first { it.id == "byte" }
            "b" -> return UNITS.first { it.id == "bit" }
            "KB", "kB" -> return UNITS.first { it.id == "kb" }
            "Kb", "kb" -> return UNITS.first { it.id == "kb" } // in everyday search, "kb/mb/gb/tb" means Kilobytes/Megabytes/Gigabytes/Terabytes
            "MB" -> return UNITS.first { it.id == "mb" }
            "Mb" -> return UNITS.first { it.id == "mbit" }
            "GB" -> return UNITS.first { it.id == "gb" }
            "Gb" -> return UNITS.first { it.id == "gbit" }
            "TB" -> return UNITS.first { it.id == "tb" }
            "Tb" -> return UNITS.first { it.id == "tbit" }
            "MB/s", "MBps" -> return UNITS.first { it.id == "mBps" }
            "Mbps", "mbps" -> return UNITS.first { it.id == "mbps" }
            "KB/s", "KBps", "kB/s" -> return UNITS.first { it.id == "kBps" }
            "Kbps", "kbps" -> return UNITS.first { it.id == "kbps" }
            "GB/s", "GBps" -> return UNITS.first { it.id == "gBps" }
            "Gbps", "gbps" -> return UNITS.first { it.id == "gbps" }
            "MW" -> return UNITS.first { it.id == "megawatt" }
            "mW" -> return UNITS.first { it.id == "mw_power" }
            "Cal" -> return UNITS.first { it.id == "kcal" }
            "cal" -> return UNITS.first { it.id == "cal" }
            "m" -> return UNITS.first { it.id == "m" }
            "c", "C" -> return UNITS.first { it.id == "celsius" }
            "f", "F" -> return UNITS.first { it.id == "fahrenheit" }
            "k", "K" -> return UNITS.first { it.id == "kelvin" }
        }

        return UNITS.firstOrNull { cleanLower in it.aliases }
    }

    private fun resolveAllCandidateUnits(lowerToken: String, caseSensitiveToken: String): List<UnitDef> {
        val cleanLower = lowerToken.trim()
        val cleanCase = caseSensitiveToken.trim()
        val candidates = mutableListOf<UnitDef>()

        // Add primary case-sensitive match first if any
        resolveSingleUnit(cleanLower, cleanCase)?.let { candidates.add(it) }

        // Add all alias matches across categories (e.g., "oz" -> mass oz & volume floz; "c" -> celsius & cup; "m" -> meter & minute)
        for (u in UNITS) {
            if (cleanLower in u.aliases && u !in candidates) {
                candidates.add(u)
            }
        }
        // Contextual multi-category tokens
        when (cleanLower) {
            "c" -> {
                UNITS.firstOrNull { it.id == "cup" }?.let { if (it !in candidates) candidates.add(it) }
                UNITS.firstOrNull { it.id == "c_light" }?.let { if (it !in candidates) candidates.add(it) }
            }
            "m" -> {
                UNITS.firstOrNull { it.id == "min" }?.let { if (it !in candidates) candidates.add(it) }
            }
            "oz", "ounce", "ounces" -> {
                UNITS.firstOrNull { it.id == "floz" }?.let { if (it !in candidates) candidates.add(it) }
            }
            "mb" -> {
                UNITS.firstOrNull { it.id == "mbar" }?.let { if (it !in candidates) candidates.add(it) }
                UNITS.firstOrNull { it.id == "mbit" }?.let { if (it !in candidates) candidates.add(it) }
            }
            "kn" -> {
                UNITS.firstOrNull { it.id == "kn_force" }?.let { if (it !in candidates) candidates.add(it) }
            }
        }
        return candidates
    }

    private fun resolveCompatibleUnits(
        fromLower: String,
        fromCase: String,
        toRaw: String
    ): Pair<UnitDef, UnitDef>? {
        val toLower = toRaw.trim().lowercase(Locale.ROOT)
        val toCase = toRaw.trim()

        val fromCandidates = resolveAllCandidateUnits(fromLower, fromCase)
        val toCandidates = resolveAllCandidateUnits(toLower, toCase)

        for (f in fromCandidates) {
            for (t in toCandidates) {
                if (f.category == t.category) {
                    return f to t
                }
            }
        }
        return null
    }

    private fun evaluateCompoundToTarget(leftRaw: String, targetRaw: String): ConversionOutcome? {
        val toCandidates = resolveAllCandidateUnits(targetRaw.lowercase(Locale.ROOT), targetRaw)
        if (toCandidates.isEmpty()) return null

        // 1. Feet + Inches -> target Length unit
        COMPOUND_FEET_INCHES.matchEntire(leftRaw)?.let { m ->
            val targetUnit = toCandidates.firstOrNull { it.category == UnitCategory.LENGTH } ?: return null
            val ft = m.groupValues[1].toDoubleOrNull() ?: return null
            val inch = m.groupValues[2].toDoubleOrNull() ?: return null
            val totalInches = if (ft < 0) ft * 12.0 - inch else ft * 12.0 + inch
            val inUnit = UNITS.first { it.id == "in" }
            val out = buildUnitConversion(totalInches, inUnit, targetUnit) ?: return null
            return out.copy(title = "${fmtNum(ft)} ft ${fmtNum(inch)} in → ${targetUnit.name}")
        }

        // 2. Stone + Pounds -> target Mass unit
        COMPOUND_STONE_POUNDS.matchEntire(leftRaw)?.let { m ->
            val targetUnit = toCandidates.firstOrNull { it.category == UnitCategory.MASS } ?: return null
            val st = m.groupValues[1].toDoubleOrNull() ?: return null
            val lb = m.groupValues[2].toDoubleOrNull() ?: return null
            val totalLb = st * 14.0 + lb
            val lbUnit = UNITS.first { it.id == "lb" }
            val out = buildUnitConversion(totalLb, lbUnit, targetUnit) ?: return null
            return out.copy(title = "${fmtNum(st)} st ${fmtNum(lb)} lb → ${targetUnit.name}")
        }

        // 3. Pounds + Ounces -> target Mass unit
        COMPOUND_POUNDS_OUNCES.matchEntire(leftRaw)?.let { m ->
            val targetUnit = toCandidates.firstOrNull { it.category == UnitCategory.MASS } ?: return null
            val lb = m.groupValues[1].toDoubleOrNull() ?: return null
            val oz = m.groupValues[2].toDoubleOrNull() ?: return null
            val totalLb = lb + oz / 16.0
            val lbUnit = UNITS.first { it.id == "lb" }
            val out = buildUnitConversion(totalLb, lbUnit, targetUnit) ?: return null
            return out.copy(title = "${fmtNum(lb)} lb ${fmtNum(oz)} oz → ${targetUnit.name}")
        }

        return null
    }

    // =========================================================================
    // Conversion Result Builders
    // =========================================================================

    private fun buildUnitConversion(
        amount: Double,
        fromUnit: UnitDef,
        toUnit: UnitDef,
        preferCompoundFeetInches: Boolean = false
    ): ConversionOutcome? {
        val baseValue = fromUnit.toBase(amount)
        val converted = toUnit.fromBase(baseValue)
        if (!converted.isFinite()) return null

        val decimalFormatted = formatUnitValue(converted, toUnit)
        var formattedPrimary = decimalFormatted
        var copyStr = fmtPlain(converted)
        val sourceFormatted = formatUnitValue(amount, fromUnit)

        var compoundFtInDisplay: String? = null
        var compoundFtInParen: String? = null
        if (toUnit.id == "ft" && abs(converted) < 10_000.0 && (preferCompoundFeetInches || abs(converted) >= 1.0)) {
            val sign = if (converted < 0) "-" else ""
            val totalIn = abs(converted) * 12.0
            var wholeFt = floor(totalIn / 12.0).toLong()
            var remIn = totalIn - wholeFt * 12.0
            if (remIn >= 11.995) {
                wholeFt += 1L
                remIn = 0.0
            }
            compoundFtInDisplay = "$sign$wholeFt ft ${fmtNum(remIn)} in"
            compoundFtInParen = "$sign$wholeFt ft ${fmtNum(remIn)} in ($sign$wholeFt′ ${fmtNum(remIn)}″)"
        }

        if (preferCompoundFeetInches && compoundFtInDisplay != null) {
            formattedPrimary = compoundFtInDisplay
            copyStr = compoundFtInDisplay
        }

        // Add helpful secondary breakdown in subtitle (e.g. feet+inches for ft, stone+lb for st, binary GiB/MiB for GB/MB)
        val extraHint = when {
            preferCompoundFeetInches && compoundFtInDisplay != null -> decimalFormatted
            compoundFtInParen != null -> compoundFtInParen
            toUnit.id == "st" && abs(converted) >= 1.0 && abs(converted) < 1000.0 -> {
                val totalLb = abs(converted) * 14.0
                val wholeSt = floor(totalLb / 14.0).toLong()
                val remLb = totalLb - wholeSt * 14.0
                "$wholeSt st ${fmtNum(remLb)} lb"
            }
            toUnit.id == "hr" && abs(converted) >= 1.0 && abs(converted) < 10_000.0 -> {
                val totalMin = (abs(converted) * 60.0).roundToLong()
                val h = totalMin / 60L
                val m = totalMin % 60L
                if (m > 0L) "${h}h ${m}m" else null
            }
            toUnit.id == "min" && abs(converted) >= 60.0 && abs(converted) < 600_000.0 -> {
                val totalSec = (abs(converted) * 60.0).roundToLong()
                val h = totalSec / 3600L
                val m = (totalSec % 3600L) / 60L
                if (h > 0L) "${h}h ${m}m" else null
            }
            fromUnit.category == UnitCategory.DATA_SIZE && toUnit.id in setOf("kb", "mb", "gb", "tb") -> {
                val binUnitId = when (toUnit.id) {
                    "kb" -> "kib"
                    "mb" -> "mib"
                    "gb" -> "gib"
                    "tb" -> "tib"
                    else -> null
                }
                val binUnit = UNITS.firstOrNull { it.id == binUnitId }
                if (binUnit != null) {
                    "Binary: ${formatUnitValue(binUnit.fromBase(baseValue), binUnit)}"
                } else null
            }
            else -> null
        }

        val unitRatio = if (fromUnit.category != UnitCategory.TEMPERATURE &&
            fromUnit.id != "l100km" && toUnit.id != "l100km" &&
            fromUnit.id != "dbm" && toUnit.id != "dbm"
        ) {
            val oneConverted = toUnit.fromBase(fromUnit.toBase(1.0))
            "1 ${fromUnit.symbol} = ${fmtNum(oneConverted)} ${toUnit.symbol}"
        } else null

        val subtitleParts = listOfNotNull(extraHint, unitRatio)

        return ConversionOutcome(
            title = "$sourceFormatted (${fromUnit.name}) → ${toUnit.name}",
            primaryValue = formattedPrimary,
            badge = fromUnit.category.badgeLabel,
            subtitle = subtitleParts.joinToString("  •  "),
            copyText = copyStr
        )
    }

    private fun buildCurrencyConversion(amount: Double, fromCode: String, toCode: String): ConversionOutcome? {
        val converted = CurrencyRateService.convert(amount, fromCode, toCode) ?: return null
        val unitRate = CurrencyRateService.convert(1.0, fromCode, toCode) ?: return null
        val inverseRate = CurrencyRateService.convert(1.0, toCode, fromCode) ?: return null

        val fromName = currencyName(fromCode)
        val toName = currencyName(toCode)
        val formattedSource = formatCurrencyAmount(amount, fromCode)
        val formattedTarget = formatCurrencyAmount(converted, toCode)
        val copyStr = formatCurrencyCopyText(converted, toCode)

        val ageNote = formatRateFreshness(CurrencyRateService.lastUpdatedMs)

        return ConversionOutcome(
            title = "$formattedSource ($fromName) → $toName ($toCode)",
            primaryValue = "$formattedTarget $toCode",
            badge = "1 $fromCode = ${fmtRate(unitRate)} $toCode",
            subtitle = "1 $toCode = ${fmtRate(inverseRate)} $fromCode  •  $ageNote",
            copyText = copyStr,
            isCurrency = true,
            fromCurrency = fromCode,
            toCurrency = toCode
        )
    }

    private fun buildMultiCurrencyOverview(amount: Double, fromCode: String): ConversionOutcome? {
        val localCode = runCatching {
            java.util.Currency.getInstance(Locale.getDefault())?.currencyCode?.uppercase(Locale.ROOT)
        }.getOrNull()

        val priorityTargets = buildList {
            if (localCode != null && localCode != fromCode && CurrencyRateService.hasCurrency(localCode)) {
                add(localCode)
            }
            for (c in listOf("USD", "EUR", "INR", "GBP", "JPY", "CAD", "AUD", "CHF")) {
                if (c != fromCode && c !in this) add(c)
            }
        }

        val conversions = priorityTargets.mapNotNull { target ->
            val v = CurrencyRateService.convert(amount, fromCode, target) ?: return@mapNotNull null
            target to v
        }
        if (conversions.isEmpty()) return null

        val topPrimary = conversions.take(2).joinToString("  •  ") { (code, v) ->
            "${formatCurrencyAmount(v, code)} $code"
        }
        val secondary = conversions.drop(2).take(4).joinToString("  •  ") { (code, v) ->
            "${formatCurrencyAmount(v, code)} $code"
        }
        val firstVal = conversions.first()
        val copyStr = formatCurrencyCopyText(firstVal.second, firstVal.first)
        val ageNote = formatRateFreshness(CurrencyRateService.lastUpdatedMs)
        val subtitleText = if (secondary.isNotEmpty()) "$secondary  •  $ageNote" else ageNote

        return ConversionOutcome(
            title = "${formatCurrencyAmount(amount, fromCode)} (${currencyName(fromCode)})",
            primaryValue = topPrimary,
            badge = UnitCategory.CURRENCY.badgeLabel,
            subtitle = subtitleText,
            copyText = copyStr,
            isCurrency = true,
            fromCurrency = fromCode,
            toCurrency = firstVal.first
        )
    }

    private val SLASH_CURRENCY_PAIR_REGEX = Regex(
        """^\s*(?:([+-]?\d[\d,]*(?:\.\d+)?)\s*(k|m|b|t|lakh|lakhs|lac|lacs|crore|crores|cr|million|billion)?\s+)?([a-zA-Z]{3})\s*/\s*([a-zA-Z]{3})\s*$""",
        RegexOption.IGNORE_CASE
    )

    private val COMPACT_CURRENCY_PAIR_REGEX = Regex(
        """^\s*(?:([+-]?\d[\d,]*(?:\.\d+)?)\s*(k|m|b|t|lakh|lakhs|lac|lacs|crore|crores|cr|million|billion)?\s+)?([a-zA-Z]{3})([a-zA-Z]{3})\s*$""",
        RegexOption.IGNORE_CASE
    )

    private fun resolveStrictCurrencyCode(rawThreeLetters: String): String? {
        val lower = rawThreeLetters.lowercase(Locale.ROOT)
        if (lower.length != 3 || !lower.all { it in 'a'..'z' }) return null
        return resolveCurrencyCode(lower)
    }

    private fun evaluateCurrencyPair(text: String, allowConcatenated: Boolean): ConversionOutcome? {
        val slashMatch = SLASH_CURRENCY_PAIR_REGEX.matchEntire(text)
        if (slashMatch != null) {
            val fromCode = resolveStrictCurrencyCode(slashMatch.groupValues[3])
            val toCode = resolveStrictCurrencyCode(slashMatch.groupValues[4])
            if (fromCode != null && toCode != null && fromCode != toCode) {
                val amount = parseOptionalAmount(slashMatch.groupValues[1], slashMatch.groupValues[2])
                return buildPairResultOrAmount(amount, fromCode, toCode)
            }
        }

        if (allowConcatenated) {
            val compactMatch = COMPACT_CURRENCY_PAIR_REGEX.matchEntire(text)
            if (compactMatch != null) {
                val combined = (compactMatch.groupValues[3] + compactMatch.groupValues[4]).lowercase(Locale.ROOT)
                val isKnownWordOrUnit = CURRENCY_ALIASES.containsKey(combined) ||
                    UNITS.any { combined in it.aliases }
                if (!isKnownWordOrUnit) {
                    val fromCode = resolveStrictCurrencyCode(compactMatch.groupValues[3])
                    val toCode = resolveStrictCurrencyCode(compactMatch.groupValues[4])
                    if (fromCode != null && toCode != null && fromCode != toCode) {
                        val amount = parseOptionalAmount(compactMatch.groupValues[1], compactMatch.groupValues[2])
                        return buildPairResultOrAmount(amount, fromCode, toCode)
                    }
                }
            }
        }
        return null
    }

    private fun parseOptionalAmount(numRaw: String, multRaw: String): Double? {
        if (numRaw.isEmpty()) return null
        val base = numRaw.replace(",", "").toDoubleOrNull() ?: return null
        val mult = if (multRaw.isNotEmpty()) multiplierValue(multRaw) ?: 1.0 else 1.0
        return base * mult
    }

    private fun buildPairResultOrAmount(amount: Double?, fromCode: String, toCode: String): ConversionOutcome? {
        if (amount != null) {
            return buildCurrencyConversion(amount, fromCode, toCode)
        }
        val unitRate = CurrencyRateService.convert(1.0, fromCode, toCode) ?: return null
        val inverseRate = CurrencyRateService.convert(1.0, toCode, fromCode) ?: return null
        val ageNote = formatRateFreshness(CurrencyRateService.lastUpdatedMs)
        return ConversionOutcome(
            title = "$fromCode/$toCode • ${currencyName(fromCode)} → ${currencyName(toCode)}",
            primaryValue = "1 $fromCode = ${fmtRate(unitRate)} $toCode",
            badge = "$fromCode/$toCode",
            subtitle = "1 $toCode = ${fmtRate(inverseRate)} $fromCode  •  $ageNote",
            copyText = fmtRate(unitRate),
            isCurrency = true,
            fromCurrency = fromCode,
            toCurrency = toCode
        )
    }

    private fun buildCurrencyRateOverview(fromCode: String): ConversionOutcome? {
        val localCode = runCatching {
            java.util.Currency.getInstance(Locale.getDefault())?.currencyCode?.uppercase(Locale.ROOT)
        }.getOrNull()

        val priorityTargets = buildList {
            if (localCode != null && localCode != fromCode && CurrencyRateService.hasCurrency(localCode)) {
                add(localCode)
            }
            for (c in listOf("USD", "EUR", "GBP", "JPY", "CHF", "CNY", "INR", "AUD", "CAD")) {
                if (c != fromCode && c !in this && CurrencyRateService.hasCurrency(c)) add(c)
            }
        }.take(6)

        val rows = priorityTargets.mapNotNull { target ->
            val r = CurrencyRateService.convert(1.0, fromCode, target) ?: return@mapNotNull null
            target to r
        }
        if (rows.isEmpty()) return null

        val gridLines = rows.chunked(2).joinToString("\n") { pair ->
            val left = "${pair[0].first} ${fmtRate(pair[0].second)}"
            if (pair.size == 2) {
                val right = "${pair[1].first} ${fmtRate(pair[1].second)}"
                "${left.padEnd(14)}  $right"
            } else {
                left
            }
        }
        val copyLines = rows.joinToString("\n") { (target, rate) ->
            "1 $fromCode = ${fmtRate(rate)} $target"
        }

        return ConversionOutcome(
            title = "1 $fromCode (${currencyName(fromCode)})",
            primaryValue = gridLines,
            badge = "$fromCode Rates",
            subtitle = formatRateFreshness(CurrencyRateService.lastUpdatedMs),
            copyText = copyLines,
            isCurrency = true,
            fromCurrency = fromCode,
            toCurrency = rows.first().first
        )
    }

    private fun buildMajorFxRatesOverview(): ConversionOutcome? {
        val majorPairs = listOf(
            "EUR" to "USD",
            "USD" to "JPY",
            "GBP" to "USD",
            "USD" to "CHF",
            "USD" to "CNY",
            "USD" to "INR",
            "AUD" to "USD",
            "USD" to "CAD"
        )
        val entries = majorPairs.mapNotNull { (base, quote) ->
            val rate = CurrencyRateService.convert(1.0, base, quote) ?: return@mapNotNull null
            "$base/$quote" to rate
        }
        if (entries.isEmpty()) return null

        val gridLines = entries.chunked(2).joinToString("\n") { pair ->
            val left = "${pair[0].first} ${fmtRate(pair[0].second)}"
            if (pair.size == 2) {
                val right = "${pair[1].first} ${fmtRate(pair[1].second)}"
                "${left.padEnd(15)}  $right"
            } else {
                left
            }
        }
        val copyLines = entries.joinToString("\n") { (pair, rate) -> "$pair = ${fmtRate(rate)}" }

        return ConversionOutcome(
            title = "Major FX Exchange Rates",
            primaryValue = gridLines,
            badge = "FX Rates",
            subtitle = formatRateFreshness(CurrencyRateService.lastUpdatedMs),
            copyText = copyLines,
            isCurrency = true
        )
    }

    private fun defaultTargetUnitsFor(from: UnitDef): List<UnitDef> {
        fun u(id: String) = UNITS.first { it.id == id }
        return when (from.id) {
            // Temperature
            "celsius" -> listOf(u("fahrenheit"), u("kelvin"))
            "fahrenheit" -> listOf(u("celsius"), u("kelvin"))
            "kelvin" -> listOf(u("celsius"), u("fahrenheit"))
            "rankine" -> listOf(u("fahrenheit"), u("celsius"))
            // Length
            "cm" -> listOf(u("in"), u("ft"), u("m"))
            "mm" -> listOf(u("in"), u("cm"))
            "m" -> listOf(u("ft"), u("in"), u("yd"))
            "km" -> listOf(u("mi"), u("m"), u("nmi"))
            "in" -> listOf(u("cm"), u("mm"), u("ft"))
            "ft" -> listOf(u("cm"), u("m"), u("in"))
            "yd" -> listOf(u("m"), u("ft"))
            "mi" -> listOf(u("km"), u("m"), u("ft"))
            "nmi" -> listOf(u("km"), u("mi"))
            // Mass
            "kg" -> listOf(u("lb"), u("st"), u("g"))
            "g" -> listOf(u("oz"), u("lb"), u("kg"))
            "mg" -> listOf(u("g"), u("gr"))
            "lb" -> listOf(u("kg"), u("g"), u("st"))
            "oz" -> listOf(u("g"), u("lb"))
            "st" -> listOf(u("kg"), u("lb"))
            "tonne" -> listOf(u("kg"), u("ton"), u("lb"))
            "ton" -> listOf(u("kg"), u("tonne"), u("lb"))
            // Volume
            "ml" -> listOf(u("floz"), u("tbsp"), u("cup"))
            "l" -> listOf(u("gal"), u("pt"), u("floz"))
            "gal" -> listOf(u("l"), u("qt"), u("pt"))
            "uk_gal" -> listOf(u("l"), u("gal"))
            "floz" -> listOf(u("ml"), u("tbsp"), u("cup"))
            "cup" -> listOf(u("ml"), u("floz"), u("tbsp"))
            "tbsp" -> listOf(u("ml"), u("tsp"), u("floz"))
            "tsp" -> listOf(u("ml"), u("tbsp"))
            "pt" -> listOf(u("ml"), u("l"), u("cup"))
            // Area
            "m2" -> listOf(u("ft2"), u("yd2"))
            "ft2" -> listOf(u("m2"), u("yd2"))
            "km2" -> listOf(u("mi2"), u("ha"), u("acre"))
            "mi2" -> listOf(u("km2"), u("acre"), u("ha"))
            "acre" -> listOf(u("m2"), u("ha"), u("ft2"))
            "ha" -> listOf(u("acre"), u("m2"), u("km2"))
            // Speed
            "kmh" -> listOf(u("mph"), u("mps"), u("knot"))
            "mph" -> listOf(u("kmh"), u("mps"), u("knot"))
            "mps" -> listOf(u("kmh"), u("mph"))
            "knot" -> listOf(u("kmh"), u("mph"))
            "mach" -> listOf(u("kmh"), u("mph"), u("mps"))
            // Data & Bandwidth
            "gb" -> listOf(u("mb"), u("gib"), u("tb"))
            "mb" -> listOf(u("gb"), u("mib"), u("kb"))
            "tb" -> listOf(u("gb"), u("tib"))
            "mbps" -> listOf(u("mBps"), u("kbps"), u("gbps"))
            "gbps" -> listOf(u("gBps"), u("mBps"), u("mbps"))
            "mBps" -> listOf(u("mbps"), u("gBps"))
            // Fuel Economy
            "mpg" -> listOf(u("l100km"), u("kml"), u("uk_mpg"))
            "l100km" -> listOf(u("mpg"), u("kml"), u("uk_mpg"))
            "kml" -> listOf(u("l100km"), u("mpg"))
            // Pressure
            "psi" -> listOf(u("bar"), u("kpa"), u("atm"))
            "bar" -> listOf(u("psi"), u("kpa"), u("atm"))
            "atm" -> listOf(u("kpa"), u("psi"), u("bar"))
            "kpa" -> listOf(u("psi"), u("bar"), u("atm"))
            // Energy & Power
            "kcal" -> listOf(u("kj"), u("j"), u("wh"))
            "cal" -> listOf(u("j"), u("kcal"))
            "kj" -> listOf(u("kcal"), u("btu"), u("wh"))
            "kwh" -> listOf(u("mj"), u("btu"), u("kcal"))
            "hp" -> listOf(u("kw"), u("w"), u("ps"))
            "kw" -> listOf(u("hp"), u("w"), u("ps"))
            "dbm" -> listOf(u("mw_power"), u("w"))
            // Angle
            "deg" -> listOf(u("rad"), u("turn"))
            "rad" -> listOf(u("deg"), u("turn"))
            else -> UNITS.filter { it.category == from.category && it.id != from.id }.take(2)
        }
    }

    // =========================================================================
    // Number Base & Roman Numeral Conversions
    // =========================================================================

    private val BASE_TARGETS = mapOf(
        "hex" to 16, "hexadecimal" to 16, "base16" to 16, "base 16" to 16,
        "bin" to 2, "binary" to 2, "base2" to 2, "base 2" to 2,
        "oct" to 8, "octal" to 8, "base8" to 8, "base 8" to 8,
        "dec" to 10, "decimal" to 10, "base10" to 10, "base 10" to 10,
        "roman" to -1, "roman numeral" to -1, "roman numerals" to -1
    )

    private fun evaluateBaseConversion(leftRaw: String, targetRaw: String): ConversionOutcome? {
        val targetKey = targetRaw.trim().lowercase(Locale.ROOT)
        val targetBase = BASE_TARGETS[targetKey] ?: return null

        var sourceStr = leftRaw.trim()
        var sourceBase = 10
        val lower = sourceStr.lowercase(Locale.ROOT)

        val baseSuffixes = listOf(
            " base 16" to 16, " base16" to 16, " hexadecimal" to 16, " hex" to 16,
            " base 2" to 2, " base2" to 2, " binary" to 2, " bin" to 2,
            " base 8" to 8, " base8" to 8, " octal" to 8, " oct" to 8,
            " base 10" to 10, " base10" to 10, " decimal" to 10, " dec" to 10
        )

        when {
            lower.startsWith("0x") -> {
                sourceBase = 16
                sourceStr = sourceStr.substring(2)
            }
            lower.startsWith("0b") -> {
                sourceBase = 2
                sourceStr = sourceStr.substring(2)
            }
            lower.startsWith("0o") -> {
                sourceBase = 8
                sourceStr = sourceStr.substring(2)
            }
            else -> {
                val matched = baseSuffixes.firstOrNull { (suffix, _) -> lower.endsWith(suffix) }
                if (matched != null) {
                    sourceBase = matched.second
                    sourceStr = sourceStr.dropLast(matched.first.length).trim()
                    if (sourceBase == 16 && sourceStr.startsWith("0x", ignoreCase = true)) {
                        sourceStr = sourceStr.substring(2)
                    } else if (sourceBase == 2 && sourceStr.startsWith("0b", ignoreCase = true)) {
                        sourceStr = sourceStr.substring(2)
                    } else if (sourceBase == 8 && sourceStr.startsWith("0o", ignoreCase = true)) {
                        sourceStr = sourceStr.substring(2)
                    }
                }
            }
        }

        val cleanSource = sourceStr.trim().replace(",", "").replace("_", "")
        val decimalValue: Long = if (sourceBase == 10 && cleanSource.all { it.uppercaseChar() in "IVXLCDM" } && cleanSource.isNotEmpty()) {
            fromRoman(cleanSource.uppercase(Locale.ROOT)) ?: return null
        } else {
            cleanSource.toLongOrNull(sourceBase) ?: return null
        }

        if (targetBase == -1) {
            val roman = toRoman(decimalValue.toInt()) ?: return null
            return ConversionOutcome(
                title = "$decimalValue → Roman Numeral",
                primaryValue = roman,
                badge = UnitCategory.NUMBER_BASE.badgeLabel,
                subtitle = "Decimal: $decimalValue  •  Hex: 0x${decimalValue.toString(16).uppercase(Locale.ROOT)}",
                copyText = roman
            )
        }

        val converted = when (targetBase) {
            16 -> "0x${decimalValue.toString(16).uppercase(Locale.ROOT)}"
            2 -> "0b${decimalValue.toString(2)}"
            8 -> "0o${decimalValue.toString(8)}"
            else -> decimalValue.toString()
        }
        val baseLabel = when (targetBase) {
            16 -> "Hexadecimal"
            2 -> "Binary"
            8 -> "Octal"
            else -> "Decimal"
        }

        return ConversionOutcome(
            title = "${leftRaw.trim()} → $baseLabel",
            primaryValue = converted,
            badge = UnitCategory.NUMBER_BASE.badgeLabel,
            subtitle = "Dec: $decimalValue  •  Hex: 0x${decimalValue.toString(16).uppercase(Locale.ROOT)}  •  Bin: 0b${decimalValue.toString(2)}",
            copyText = converted
        )
    }

    private fun toRoman(num: Int): String? {
        if (num !in 1..3999) return null
        val values = intArrayOf(1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1)
        val numerals = arrayOf("M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I")
        val sb = StringBuilder()
        var rem = num
        for (i in values.indices) {
            while (rem >= values[i]) {
                sb.append(numerals[i])
                rem -= values[i]
            }
        }
        return sb.toString()
    }

    private fun fromRoman(roman: String): Long? {
        if (roman.isEmpty()) return null
        val map = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
        var total = 0L
        var prev = 0
        for (i in roman.lastIndex downTo 0) {
            val cur = map[roman[i]] ?: return null
            if (cur < prev) total -= cur else total += cur
            prev = cur
        }
        return total.takeIf { it > 0 }
    }

    // =========================================================================
    // Number & Currency Formatting Helpers
    // =========================================================================

    private fun formatUnitValue(v: Double, unit: UnitDef): String {
        val numStr = fmtNum(v)
        return when (unit.symbol) {
            "°", "′", "″" -> "$numStr${unit.symbol}"
            else -> "$numStr ${unit.symbol}"
        }
    }

    fun fmtNum(v: Double): String {
        if (!v.isFinite()) return "—"
        val a = abs(v)
        if (a == 0.0) return "0"
        if (a >= 1e12 || (a > 0.0 && a < 1e-5)) {
            return String.format(Locale.US, "%.4e", v)
        }
        val rounded = v.roundToLong()
        if (abs(v - rounded) < 1e-7) {
            return String.format(Locale.US, "%,d", rounded)
        }
        val decimals = when {
            a >= 1000.0 -> 2
            a >= 10.0 -> 3
            a >= 1.0 -> 4
            else -> 5
        }
        val raw = String.format(Locale.US, "%,.${decimals}f", v)
        return if (raw.contains('.')) raw.trimEnd('0').trimEnd('.') else raw
    }

    fun fmtPlain(v: Double): String {
        if (!v.isFinite()) return ""
        val a = abs(v)
        if (a == 0.0) return "0"
        if (a >= 1e12 || (a > 0.0 && a < 1e-5)) {
            return String.format(Locale.US, "%.6e", v)
        }
        val rounded = v.roundToLong()
        if (abs(v - rounded) < 1e-7) {
            return rounded.toString()
        }
        val decimals = when {
            a >= 1000.0 -> 2
            a >= 10.0 -> 3
            a >= 1.0 -> 4
            else -> 6
        }
        return String.format(Locale.US, "%.${decimals}f", v).trimEnd('0').trimEnd('.')
    }

    private fun formatCurrencyAmount(v: Double, code: String): String {
        val meta = CURRENCIES[code.uppercase(Locale.ROOT)]
        val sym = meta?.symbol ?: "${code.uppercase(Locale.ROOT)} "
        val a = abs(v)
        val numPart = when {
            a == 0.0 -> "0.00"
            meta?.zeroDecimal == true && a >= 10.0 && abs(v - v.roundToLong()) < 0.05 ->
                String.format(Locale.US, "%,d", v.roundToLong())
            a >= 1.0 -> String.format(Locale.US, "%,.2f", v)
            a >= 0.01 -> String.format(Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
            else -> String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
        }
        return if (v < 0) "-$sym${numPart.removePrefix("-")}" else "$sym$numPart"
    }

    private fun formatCurrencyCopyText(v: Double, code: String): String {
        val meta = CURRENCIES[code.uppercase(Locale.ROOT)]
        val a = abs(v)
        return when {
            a == 0.0 -> "0"
            meta?.zeroDecimal == true && a >= 10.0 && abs(v - v.roundToLong()) < 0.05 ->
                v.roundToLong().toString()
            a >= 1.0 -> String.format(Locale.US, "%.2f", v)
            else -> String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
        }
    }

    fun formatRate(r: Double): String = fmtRate(r)

    private fun fmtRate(r: Double): String {
        val a = abs(r)
        return when {
            a >= 100.0 -> String.format(Locale.US, "%,.2f", r)
            a >= 1.0 -> String.format(Locale.US, "%.4f", r).trimEnd('0').trimEnd('.')
            else -> String.format(Locale.US, "%.6f", r).trimEnd('0').trimEnd('.')
        }
    }

    fun formatRateFreshness(updatedMs: Long): String {
        if (updatedMs <= 0L) return "Cached FX rates"
        val elapsedMin = ((System.currentTimeMillis() - updatedMs).coerceAtLeast(0L)) / 60_000L
        return when {
            elapsedMin < 2L -> "Live FX rates"
            elapsedMin < 60L -> "FX updated ${elapsedMin}m ago"
            elapsedMin < 1440L -> "FX updated ${elapsedMin / 60L}h ago"
            else -> "FX updated ${elapsedMin / 1440L}d ago"
        }
    }
}
