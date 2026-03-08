package de.drick.wtf_osd


fun CharArray.toLinesString(osdRecord: OsdRecord) = buildString {
    for (y in 0 until osdRecord.charHeight) {
        for (x in 0 until osdRecord.charWidth) {
            val index = y * osdRecord.charWidth + x
            val char = this@toLinesString[index]
            append(char)
        }
        append("\n")
    }
}


fun ShortArray.detectTrailingString(fontVariant: FontVariant, code: Int, number: Int): String? {
    val chars = detectTrailingChars(code, number)
    return chars?.toNullString()
}

private fun ShortArray.detectTrailingChars(code: Int, number: Int): CharArray? {
    val shortCode = code.toShort()
    val index = indexOfFirst { it == shortCode }
    return if (index >= 0) {
        CharArray(number) {
            val short = this[index + it + 1]
            Char(short.toInt())
        }
    } else {
        null
    }
}


private fun CharArray.toNullString(): String {
    val endIndex = indexOf('\u0000')
    return if (endIndex >= 0) {
        concatToString(startIndex = 0, endIndex)
    } else {
        concatToString()
    }
}

/*fun String.replaceSym(symbols: Symbols): String {

}*/

class Symbols(val osdRecord: OsdRecord) {

    fun toString(frame: ShortArray, replaceCharsWithSym: Boolean = true): String = frame.toCharArray()
            .toLinesList(osdRecord, replaceCharsWithSym = replaceCharsWithSym)
            .joinToString("\n")

    fun fromCode(code: Int) = entries().firstOrNull { it.code == code }

    fun replaceSymbols(symString: String): String {
        var newString = symString
        entries().forEach {
            newString.replace(it.name, Char(it.code).toString())
        }
        return newString
    }

    private fun entries(): List<Symbol> = when (osdRecord.fontVariant) {
        FontVariant.BETAFLIGHT -> BetaflightSymbols.entries
        FontVariant.INAV -> INavSymbols.entries
        else -> emptyList()
    }

}

fun ShortArray.toCharArray(): CharArray = map { Char(it.toInt()) }.toCharArray()

fun CharArray.toLinesList(osdRecord: OsdRecord, replaceCharsWithSym: Boolean = false) = buildList {
    val symbols = Symbols(osdRecord)
    for (y in 0 until osdRecord.charHeight) {
        val line = buildString {
            for (x in 0 until osdRecord.charWidth) {
                val index = y * osdRecord.charWidth + x
                val char = this@toLinesList[index]
                if (replaceCharsWithSym) {
                    val sym = symbols.fromCode(char.code)
                    if (sym != null) {6
                        append(sym.replaceWith ?: sym.name)
                    } else {
                        append(char)
                    }
                } else {
                    append(char)
                }
            }
        }
        add(line)
    }
}

sealed interface Symbol {
    val code: Int
    val name: String
    val replaceWith: String?
}

/**
 * Source:
 * https://www.betaflight.com/docs/development/OSD-Glyps
 */
enum class BetaflightSymbols(
    override val code: Int,
    override val replaceWith: String? = null
): Symbol {
    SYM_NONE(0),
    SYM_RSSI(1),
    SYM_AH_RIGHT(2),
    SYM_AH_LEFT(3),
    SYM_CURSOR(3),
    SYM_THR(4),
    SYM_OVER_HOME(5),
    SYM_VOLT(6),
    SYM_MAH(7),
    SYM_STICK_OVERLAY_SPRITE_HIGH(8),
    SYM_STICK_OVERLAY_SPRITE_MID(9),
    SYM_STICK_OVERLAY_SPRITE_LOW(10),
    SYM_STICK_OVERLAY_CENTER(11),
    SYM_M(12),
    SYM_F(13),
    SYM_C(14),
    SYM_FT(15),
    SYM_BBLOG(16),
    SYM_HOMEFLAG(17),
    SYM_RPM(18),
    SYM_AH_DECORATION(19),
    SYM_ROLL(20),
    SYM_PITCH(21),
    SYM_STICK_OVERLAY_VERTICAL(22),
    SYM_STICK_OVERLAY_HORIZONTAL(23),
    SYM_HEADING_N(24),
    SYM_HEADING_S(25),
    SYM_HEADING_E(26),
    SYM_HEADING_W(27),
    SYM_HEADING_DIVIDED_LINE(28),
    SYM_HEADING_LINE(29),
    SYM_SAT_L(30),
    SYM_SAT_R(31),
    // Ascii chars
    //SYM_BLANK(32),
    //SYM_HYPHEN(45),
    //SYM_WATT(87), // Also ASCII W
    // End of ascii section
    SYM_ARROW_SOUTH(96),
    SYM_ARROW_2(97),
    SYM_ARROW_3(98),
    SYM_ARROW_4(99),
    SYM_ARROW_EAST(100),
    SYM_ARROW_6(101),
    SYM_ARROW_7(102),
    SYM_ARROW_8(103),
    SYM_ARROW_NORTH(104),
    SYM_ARROW_10(105),
    SYM_ARROW_11(106),
    SYM_ARROW_12(107),
    SYM_ARROW_WEST(108),
    SYM_ARROW_14(109),
    SYM_ARROW_15(110),
    SYM_ARROW_16(111),
    SYM_SPEED(112),
    SYM_TOTAL_DISTANCE(113),
    SYM_AH_CENTER_LINE(114),
    SYM_AH_CENTER(115),
    SYM_AH_CENTER_LINE_RIGHT(116),
    SYM_TEMPERATURE(122),
    SYM_ALTITUDE(127),
    SYM_AH_BAR9_0(128),
    SYM_AH_BAR9_1(129),
    SYM_AH_BAR9_2(130),
    SYM_AH_BAR9_3(131),
    SYM_AH_BAR9_4(132),
    SYM_AH_BAR9_5(133),
    SYM_AH_BAR9_6(134),
    SYM_AH_BAR9_7(135),
    SYM_AH_BAR9_8(136),
    SYM_LAT(137),
    SYM_PB_START(138),
    SYM_PB_FULL(139),
    SYM_PB_HALF(140),
    SYM_PB_EMPTY(141),
    SYM_PB_END(142),
    SYM_PB_CLOSE(143),
    SYM_BATT_FULL(144),
    SYM_BATT_5(145),
    SYM_BATT_4(146),
    SYM_BATT_3(147),
    SYM_BATT_2(148),
    SYM_BATT_1(149),
    SYM_BATT_EMPTY(150),
    SYM_MAIN_BATT(151),
    SYM_LON(152),
    SYM_FTPS(153),
    SYM_AMP(154),
    SYM_ON_M(155),
    SYM_FLY_M(156),
    SYM_MPH(157),
    SYM_KPH(158),
    SYM_MPS(159),
    SYM_END_OF_FONT(255);
}

/**
 * Source:
 * https://github.com/iNavFlight/inav-configurator/blob/master/resources/osd/INAV%20Character%20Map.md
 */
enum class INavSymbols(
    override val code: Int,
    override val replaceWith: String? = null
): Symbol {
    SYM_NONE(0),
    SYM_RSSI(1),
    SYM_LQ(2),
    SYM_LAT(3),
    SYM_LON(4),
    SYM_AZIMUTH(5),
    SYM_TELEMETRY_0(6),
    SYM_TELEMETRY_1(7),
    SYM_SAT_L(8),
    SYM_SAT_R(9),
    SYM_HOME_NEAR(10),
    SYM_DEGREES(11),
    SYM_HEADING(12),
    SYM_SCALE(13),
    SYM_HDP_L(14),
    SYM_HDP_R(15),
    SYM_HOME(16),
    SYM_2RSS(17),
    SYM_DB(18),
    SYM_DBM(19),
    SYM_SNR(20),
    SYM_AH_DECORATION_UP(21),
    SYM_AH_DECORATION_DOWN(22),
    SYM_DECORATION(23),
    SYM_VOLT(31),
    //SYM_BLANK(32),
    SYM_AH_KM(34),
    SYM_AH_MI(36),
    SYM_VTX_POWER(39),
    SYM_AH_NM(63),
    // ASCII chars
    SYM_MAH_NM_0(96),
    SYM_MAH_NM_1(97),
    SYM_MILLIOHM(98),
    SYM_BATT_FULL(99),
    SYM_BATT_5(100),
    SYM_BATT_4(101),
    SYM_BATT_3(102),
    SYM_BATT_2(103),
    SYM_BATT_1(104),
    SYM_BATT_EMPTY(105),
    SYM_AMP(106),
    SYM_MAH_KM_0(107),
    SYM_MAH_KM_1(108),
    SYM_WH(109),
    SYM_WH_KM(110),
    SYM_WH_MI(111),
    SYM_WH_NM(112),
    SYM_WATT(113),
    SYM_MW(114),
    SYM_KILOWATT(115),
    SYM_FT(116),
    SYM_TRIP_DIST(117),
    SYM_ALT_M(118),
    SYM_ALT_KM(119),
    SYM_ALT_FT(120),
    SYM_ALT_KFT(121),
    SYM_DIST_M(122),
    SYM_DIST_KM(126),
    SYM_DIST_FT(127),
    SYM_DIST_MI(128),
    SYM_DIST_NM(129),
    SYM_M(130),
    SYM_KM(131),
    SYM_MI(132),
    SYM_NM(133),
    SYM_WIND_HORIZONTAL(134),
    SYM_WIND_VERTICAL(135),
    SYM_3D_KMH(136),
    SYM_3D_MPH(137),
    SYM_3D_KT(138),
    SYM_RPM(139),
    SYM_AIR(140),
    SYM_FTS(141),
    SYM_100FTM(142),
    SYM_MS(143),
    SYM_KMH(144),
    SYM_MPH(145),
    SYM_KT(146),
    SYM_MAH_MI_0(147),
    SYM_MAH_MI_1(148),
    SYM_THR(149),
    SYM_TEMP_F(150),
    SYM_TEMP_C(151),
    SYM_MAH(153),
    SYM_ON_H(154),
    SYM_FLY_H(155),
    SYM_GLIDESLOPE(156),
    SYM_WAYPOINT(157),
    SYM_ON_M(158),
    SYM_FLY_M(159),
    SYM_CLOCK(160),

    SYM_ZERO_HALF_TRAILING_DOT(161, "0"),
    SYM_1_HALF_TRAILING_DOT(162, "1"),
    SYM_2_HALF_TRAILING_DOT(163, "2"),
    SYM_3_HALF_TRAILING_DOT(164, "3"),
    SYM_4_HALF_TRAILING_DOT(165, "4"),
    SYM_5_HALF_TRAILING_DOT(166, "5"),
    SYM_6_HALF_TRAILING_DOT(167, "6"),
    SYM_7_HALF_TRAILING_DOT(168, "7"),
    SYM_8_HALF_TRAILING_DOT(169, "8"),
    SYM_9_HALF_TRAILING_DOT(170, "9"),
    SYM_AUTO_THR0(171),
    SYM_AUTO_THR1(172),
    SYM_ROLL_LEFT(173),
    SYM_ROLL_LEVEL(174),
    SYM_ROLL_RIGHT(175),
    SYM_PITCH_UP(176),
    SYM_ZERO_HALF_LEADING_DOT(177, ".0"),
    SYM_1_HALF_LEADING_DOT(178, ".1"),
    SYM_2_HALF_LEADING_DOT(179, ".2"),
    SYM_3_HALF_LEADING_DOT(180, ".3"),
    SYM_4_HALF_LEADING_DOT(181, ".4"),
    SYM_5_HALF_LEADING_DOT(182, ".5"),
    SYM_6_HALF_LEADING_DOT(183, ".6"),
    SYM_7_HALF_LEADING_DOT(184, ".7"),
    SYM_8_HALF_LEADING_DOT(185, ".8"),
    SYM_9_HALF_LEADING_DOT(186, ".9"),
    SYM_PITCH_DOWN(187),
    SYM_GFORCE(188),
    SYM_GFORCE_X(189),
    SYM_GFORCE_Y(190),
    SYM_GFORCE_Z(191),
    SYM_BARO_TEMP(192),
    SYM_IMU_TEMP(193),
    SYM_TEMP(194),
    SYM_ESC_TEMP(195),
    SYM_HEADING_N(200),
    SYM_HEADING_S(201),
    SYM_HEADING_E(202),
    SYM_HEADING_W(203),
    SYM_HEADING_DIVIDED_LINE(204),
    SYM_HEADING_LINE(205),
    SYM_MAX(206),
    SYM_PROFILE(207),
    SYM_SWITCH_INDICATOR_LOW(208),
    SYM_SWITCH_INDICATOR_HIGH(210),
    SYM_AH(211),
    SYM_GLIDE_DIST(212),
    SYM_GLIDE_MINS(213),
    SYM_AH_V_FT_0(214),
    SYM_AH_V_FT_1(215),
    SYM_AH_V_M_0(216),
    SYM_AH_V_M_1(217),
    SYM_FLIGHT_MINS_REMAINING(218),
    SYM_FLIGHT_HOURS_REMAINING(219),
    SYM_GROUND_COURSE(220),
    SYM_ALERT(221),
    SYM_TERRAIN_FOLLOWING(251),
    SYM_CROSS_TRACK_ERROR(252),
    SYM_ADSB(253),
    SYM_BLACKBOX(254),
    SYM_LOGO_START(257),
    SYM_AH_LEFT(300),
    SYM_AH_RIGHT(301),
    SYM_AH_DECORATION_MIN(302),
    SYM_AH_DECORATION(305),
    SYM_AH_DECORATION_MAX(307),
    SYM_AH_CH_LEFT(314),
    SYM_AH_CH_RIGHT(315),
    SYM_ARROW_UP(316),
    SYM_ARROW_2(317),
    SYM_ARROW_3(318),
    SYM_ARROW_4(319),
    SYM_ARROW_RIGHT(320),
    SYM_ARROW_6(321),
    SYM_ARROW_7(322),
    SYM_ARROW_8(323),
    SYM_ARROW_DOWN(324),
    SYM_ARROW_10(325),
    SYM_ARROW_11(326),
    SYM_ARROW_12(327),
    SYM_ARROW_LEFT(328),
    SYM_ARROW_14(329),
    SYM_ARROW_15(330),
    SYM_ARROW_16(331),
    SYM_AH_H_START(332),
    SYM_VARIO_UP_2A(341),
    SYM_VARIO_UP_1A(342),
    SYM_VARIO_DOWN_1A(343),
    SYM_VARIO_DOWN_2A(344),
    SYM_ALT(345),
    SYM_AH_V_START(346),
    SYM_HUD_SIGNAL_0(352),
    SYM_HUD_SIGNAL_1(353),
    SYM_HUD_SIGNAL_2(354),
    SYM_HUD_SIGNAL_3(355),
    SYM_HUD_SIGNAL_4(356),
    SYM_HOME_DIST(357),
    SYM_AH_CH_CENTER(358),
    SYM_FLIGHT_DIST_REMAINING(359),
    SYM_ODOMETER(360),
    SYM_RX_BAND(361),
    SYM_RX_MODE(362),
    SYM_AH_CH_TYPE3(400),
    SYM_AH_CH_TYPE4(403),
    SYM_AH_CH_TYPE5(406),
    SYM_AH_CH_TYPE6(409),
    SYM_AH_CH_TYPE7(412),
    SYM_AH_CH_TYPE8(415),
    SYM_AH_CH_AIRCRAFT0(418),
    SYM_AH_CH_AIRCRAFT1(419),
    SYM_AH_CH_AIRCRAFT2(420),
    SYM_AH_CH_AIRCRAFT3(421),
    SYM_AH_CH_AIRCRAFT4(422),
    SYM_HUD_ARROWS_L1(430),
    SYM_HUD_ARROWS_L2(431),
    SYM_HUD_ARROWS_L3(432),
    SYM_HUD_ARROWS_R1(433),
    SYM_HUD_ARROWS_R2(434),
    SYM_HUD_ARROWS_R3(435),
    SYM_HUD_ARROWS_U1(436),
    SYM_HUD_ARROWS_U2(437),
    SYM_HUD_ARROWS_U3(438),
    SYM_HUD_ARROWS_D1(439),
    SYM_HUD_ARROWS_D2(440),
    SYM_HUD_ARROWS_D3(441),
    SYM_HUD_CARDINAL(442),
    SYM_SERVO_PAN_IS_CENTRED(454),
    SYM_SERVO_PAN_IS_OFFSET_L(455),
    SYM_SERVO_PAN_IS_OFFSET_R(456),
    SYM_PILOT_LOGO_SML_L(469),
    SYM_PILOT_LOGO_LRG_START(472);
}