/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.ui

import com.google.common.collect.ArrayTable
import com.google.common.collect.Table
import java.awt.Color

/**
 * Class offers material colors.
 *
 * The colors are generated by http://go/material-colors-sheet
 */
@Suppress("HasPlatformType")
object MaterialColors {

  enum class Color {
    RED,
    PINK,
    PURPLE,
    DEEP_PURPLE,
    INDIGO,
    BLUE,
    LIGHT_BLUE,
    CYAN,
    TEAL,
    GREEN,
    LIGHT_GREEN,
    LIME,
    YELLOW,
    AMBER,
    ORANGE,
    DEEP_ORANGE,
    BROWN,
    GRAY,
    BLUE_GRAY,
  }

  enum class Category(val displayName: String) {
    MATERIAL_50("Material 50"),
    MATERIAL_100("Material 100"),
    MATERIAL_200("Material 200"),
    MATERIAL_300("Material 300"),
    MATERIAL_400("Material 400"),
    MATERIAL_500("Material 500"),
    MATERIAL_600("Material 600"),
    MATERIAL_700("Material 700"),
    MATERIAL_800("Material 800"),
    MATERIAL_900("Material 900"),
    MATERIAL_ACCENT_100("Material A100"),
    MATERIAL_ACCENT_200("Material A200"),
    MATERIAL_ACCENT_400("Material A400"),
    MATERIAL_ACCENT_700("Material A700");

    override fun toString() = displayName
  }

  private val table: ArrayTable<Color, Category, java.awt.Color>
    = ArrayTable.create(Color.values().asIterable(), Category.values().asIterable())

  // Helper extension to allow using assignment to put value to the table
  operator fun <R, C, V> Table<R, C, V>.set(r: R, c: C, v: V) = put(r, c, v)

  init {
    table[Color.RED, Category.MATERIAL_50] = Color(0xFFEBEE)
    table[Color.RED, Category.MATERIAL_100] = Color(0xFFCDD2)
    table[Color.RED, Category.MATERIAL_200] = Color(0xEF9A9A)
    table[Color.RED, Category.MATERIAL_300] = Color(0xE57373)
    table[Color.RED, Category.MATERIAL_400] = Color(0xEF5350)
    table[Color.RED, Category.MATERIAL_500] = Color(0xF44336)
    table[Color.RED, Category.MATERIAL_600] = Color(0xE53935)
    table[Color.RED, Category.MATERIAL_700] = Color(0xD32F2F)
    table[Color.RED, Category.MATERIAL_800] = Color(0xC62828)
    table[Color.RED, Category.MATERIAL_900] = Color(0xB71C1C)
    table[Color.RED, Category.MATERIAL_ACCENT_100] = Color(0xFF8A80)
    table[Color.RED, Category.MATERIAL_ACCENT_200] = Color(0xFF5252)
    table[Color.RED, Category.MATERIAL_ACCENT_400] = Color(0xFF1744)
    table[Color.RED, Category.MATERIAL_ACCENT_700] = Color(0xD50000)

    table[Color.PINK, Category.MATERIAL_50] = Color(0xFCE4EC)
    table[Color.PINK, Category.MATERIAL_100] = Color(0xF8BBD0)
    table[Color.PINK, Category.MATERIAL_200] = Color(0xF48FB1)
    table[Color.PINK, Category.MATERIAL_300] = Color(0xF06292)
    table[Color.PINK, Category.MATERIAL_400] = Color(0xEC407A)
    table[Color.PINK, Category.MATERIAL_500] = Color(0xE91E63)
    table[Color.PINK, Category.MATERIAL_600] = Color(0xD81B60)
    table[Color.PINK, Category.MATERIAL_700] = Color(0xC2185B)
    table[Color.PINK, Category.MATERIAL_800] = Color(0xAD1457)
    table[Color.PINK, Category.MATERIAL_900] = Color(0x880E4F)
    table[Color.PINK, Category.MATERIAL_ACCENT_100] = Color(0xFF80AB)
    table[Color.PINK, Category.MATERIAL_ACCENT_200] = Color(0xFF4081)
    table[Color.PINK, Category.MATERIAL_ACCENT_400] = Color(0xF50057)
    table[Color.PINK, Category.MATERIAL_ACCENT_700] = Color(0xC51162)

    table[Color.PURPLE, Category.MATERIAL_50] = Color(0xF3E5F5)
    table[Color.PURPLE, Category.MATERIAL_100] = Color(0xE1BEE7)
    table[Color.PURPLE, Category.MATERIAL_200] = Color(0xCE93D8)
    table[Color.PURPLE, Category.MATERIAL_300] = Color(0xBA68C8)
    table[Color.PURPLE, Category.MATERIAL_400] = Color(0xAB47BC)
    table[Color.PURPLE, Category.MATERIAL_500] = Color(0x9C27B0)
    table[Color.PURPLE, Category.MATERIAL_600] = Color(0x8E24AA)
    table[Color.PURPLE, Category.MATERIAL_700] = Color(0x7B1FA2)
    table[Color.PURPLE, Category.MATERIAL_800] = Color(0x6A1B9A)
    table[Color.PURPLE, Category.MATERIAL_900] = Color(0x4A148C)
    table[Color.PURPLE, Category.MATERIAL_ACCENT_100] = Color(0xEA80FC)
    table[Color.PURPLE, Category.MATERIAL_ACCENT_200] = Color(0xE040FB)
    table[Color.PURPLE, Category.MATERIAL_ACCENT_400] = Color(0xD500F9)
    table[Color.PURPLE, Category.MATERIAL_ACCENT_700] = Color(0xAA00FF)

    table[Color.DEEP_PURPLE, Category.MATERIAL_50] = Color(0xEDE7F6)
    table[Color.DEEP_PURPLE, Category.MATERIAL_100] = Color(0xD1C4E9)
    table[Color.DEEP_PURPLE, Category.MATERIAL_200] = Color(0xB39DDB)
    table[Color.DEEP_PURPLE, Category.MATERIAL_300] = Color(0x9575CD)
    table[Color.DEEP_PURPLE, Category.MATERIAL_400] = Color(0x7E57C2)
    table[Color.DEEP_PURPLE, Category.MATERIAL_500] = Color(0x673AB7)
    table[Color.DEEP_PURPLE, Category.MATERIAL_600] = Color(0x5E35B1)
    table[Color.DEEP_PURPLE, Category.MATERIAL_700] = Color(0x512DA8)
    table[Color.DEEP_PURPLE, Category.MATERIAL_800] = Color(0x4527A0)
    table[Color.DEEP_PURPLE, Category.MATERIAL_900] = Color(0x311B92)
    table[Color.DEEP_PURPLE, Category.MATERIAL_ACCENT_100] = Color(0xB388FF)
    table[Color.DEEP_PURPLE, Category.MATERIAL_ACCENT_200] = Color(0x7C4DFF)
    table[Color.DEEP_PURPLE, Category.MATERIAL_ACCENT_400] = Color(0x651FFF)
    table[Color.DEEP_PURPLE, Category.MATERIAL_ACCENT_700] = Color(0x6200EA)

    table[Color.INDIGO, Category.MATERIAL_50] = Color(0xE8EAF6)
    table[Color.INDIGO, Category.MATERIAL_100] = Color(0xC5CAE9)
    table[Color.INDIGO, Category.MATERIAL_200] = Color(0x9FA8DA)
    table[Color.INDIGO, Category.MATERIAL_300] = Color(0x7986CB)
    table[Color.INDIGO, Category.MATERIAL_400] = Color(0x5C6BC0)
    table[Color.INDIGO, Category.MATERIAL_500] = Color(0x3F51B5)
    table[Color.INDIGO, Category.MATERIAL_600] = Color(0x3949AB)
    table[Color.INDIGO, Category.MATERIAL_700] = Color(0x303F9F)
    table[Color.INDIGO, Category.MATERIAL_800] = Color(0x283593)
    table[Color.INDIGO, Category.MATERIAL_900] = Color(0x1A237E)
    table[Color.INDIGO, Category.MATERIAL_ACCENT_100] = Color(0x8C9EFF)
    table[Color.INDIGO, Category.MATERIAL_ACCENT_200] = Color(0x536DFE)
    table[Color.INDIGO, Category.MATERIAL_ACCENT_400] = Color(0x3D5AFE)
    table[Color.INDIGO, Category.MATERIAL_ACCENT_700] = Color(0x304FFE)

    table[Color.BLUE, Category.MATERIAL_50] = Color(0xE3F2FD)
    table[Color.BLUE, Category.MATERIAL_100] = Color(0xBBDEFB)
    table[Color.BLUE, Category.MATERIAL_200] = Color(0x90CAF9)
    table[Color.BLUE, Category.MATERIAL_300] = Color(0x64B5F6)
    table[Color.BLUE, Category.MATERIAL_400] = Color(0x42A5F5)
    table[Color.BLUE, Category.MATERIAL_500] = Color(0x2196F3)
    table[Color.BLUE, Category.MATERIAL_600] = Color(0x1E88E5)
    table[Color.BLUE, Category.MATERIAL_700] = Color(0x1976D2)
    table[Color.BLUE, Category.MATERIAL_800] = Color(0x1565C0)
    table[Color.BLUE, Category.MATERIAL_900] = Color(0x0D47A1)
    table[Color.BLUE, Category.MATERIAL_ACCENT_100] = Color(0x82B1FF)
    table[Color.BLUE, Category.MATERIAL_ACCENT_200] = Color(0x448AFF)
    table[Color.BLUE, Category.MATERIAL_ACCENT_400] = Color(0x2979FF)
    table[Color.BLUE, Category.MATERIAL_ACCENT_700] = Color(0x2962FF)

    table[Color.LIGHT_BLUE, Category.MATERIAL_50] = Color(0xE1F5FE)
    table[Color.LIGHT_BLUE, Category.MATERIAL_100] = Color(0xB3E5FC)
    table[Color.LIGHT_BLUE, Category.MATERIAL_200] = Color(0x81D4FA)
    table[Color.LIGHT_BLUE, Category.MATERIAL_300] = Color(0x4FC3F7)
    table[Color.LIGHT_BLUE, Category.MATERIAL_400] = Color(0x29B6F6)
    table[Color.LIGHT_BLUE, Category.MATERIAL_500] = Color(0x03A9F4)
    table[Color.LIGHT_BLUE, Category.MATERIAL_600] = Color(0x039BE5)
    table[Color.LIGHT_BLUE, Category.MATERIAL_700] = Color(0x0288D1)
    table[Color.LIGHT_BLUE, Category.MATERIAL_800] = Color(0x0277BD)
    table[Color.LIGHT_BLUE, Category.MATERIAL_900] = Color(0x01579B)
    table[Color.LIGHT_BLUE, Category.MATERIAL_ACCENT_100] = Color(0x80D8FF)
    table[Color.LIGHT_BLUE, Category.MATERIAL_ACCENT_200] = Color(0x40C4FF)
    table[Color.LIGHT_BLUE, Category.MATERIAL_ACCENT_400] = Color(0x00B0FF)
    table[Color.LIGHT_BLUE, Category.MATERIAL_ACCENT_700] = Color(0x0091EA)

    table[Color.CYAN, Category.MATERIAL_50] = Color(0xE0F7FA)
    table[Color.CYAN, Category.MATERIAL_100] = Color(0xB2EBF2)
    table[Color.CYAN, Category.MATERIAL_200] = Color(0x80DEEA)
    table[Color.CYAN, Category.MATERIAL_300] = Color(0x4DD0E1)
    table[Color.CYAN, Category.MATERIAL_400] = Color(0x26C6DA)
    table[Color.CYAN, Category.MATERIAL_500] = Color(0x00BCD4)
    table[Color.CYAN, Category.MATERIAL_600] = Color(0x00ACC1)
    table[Color.CYAN, Category.MATERIAL_700] = Color(0x0097A7)
    table[Color.CYAN, Category.MATERIAL_800] = Color(0x00838F)
    table[Color.CYAN, Category.MATERIAL_900] = Color(0x006064)
    table[Color.CYAN, Category.MATERIAL_ACCENT_100] = Color(0x84FFFF)
    table[Color.CYAN, Category.MATERIAL_ACCENT_200] = Color(0x18FFFF)
    table[Color.CYAN, Category.MATERIAL_ACCENT_400] = Color(0x00E5FF)
    table[Color.CYAN, Category.MATERIAL_ACCENT_700] = Color(0x00B8D4)

    table[Color.TEAL, Category.MATERIAL_50] = Color(0xE0F2F1)
    table[Color.TEAL, Category.MATERIAL_100] = Color(0xB2DFDB)
    table[Color.TEAL, Category.MATERIAL_200] = Color(0x80CBC4)
    table[Color.TEAL, Category.MATERIAL_300] = Color(0x4DB6AC)
    table[Color.TEAL, Category.MATERIAL_400] = Color(0x26A69A)
    table[Color.TEAL, Category.MATERIAL_500] = Color(0x009688)
    table[Color.TEAL, Category.MATERIAL_600] = Color(0x00897B)
    table[Color.TEAL, Category.MATERIAL_700] = Color(0x00796B)
    table[Color.TEAL, Category.MATERIAL_800] = Color(0x00695C)
    table[Color.TEAL, Category.MATERIAL_900] = Color(0x004D40)
    table[Color.TEAL, Category.MATERIAL_ACCENT_100] = Color(0xA7FFEB)
    table[Color.TEAL, Category.MATERIAL_ACCENT_200] = Color(0x64FFDA)
    table[Color.TEAL, Category.MATERIAL_ACCENT_400] = Color(0x1DE9B6)
    table[Color.TEAL, Category.MATERIAL_ACCENT_700] = Color(0x00BFA5)

    table[Color.GREEN, Category.MATERIAL_50] = Color(0xE8F5E9)
    table[Color.GREEN, Category.MATERIAL_100] = Color(0xC8E6C9)
    table[Color.GREEN, Category.MATERIAL_200] = Color(0xA5D6A7)
    table[Color.GREEN, Category.MATERIAL_300] = Color(0x81C784)
    table[Color.GREEN, Category.MATERIAL_400] = Color(0x66BB6A)
    table[Color.GREEN, Category.MATERIAL_500] = Color(0x4CAF50)
    table[Color.GREEN, Category.MATERIAL_600] = Color(0x43A047)
    table[Color.GREEN, Category.MATERIAL_700] = Color(0x388E3C)
    table[Color.GREEN, Category.MATERIAL_800] = Color(0x2E7D32)
    table[Color.GREEN, Category.MATERIAL_900] = Color(0x1B5E20)
    table[Color.GREEN, Category.MATERIAL_ACCENT_100] = Color(0xB9F6CA)
    table[Color.GREEN, Category.MATERIAL_ACCENT_200] = Color(0x69F0AE)
    table[Color.GREEN, Category.MATERIAL_ACCENT_400] = Color(0x00E676)
    table[Color.GREEN, Category.MATERIAL_ACCENT_700] = Color(0x00C853)

    table[Color.LIGHT_GREEN, Category.MATERIAL_50] = Color(0xF1F8E9)
    table[Color.LIGHT_GREEN, Category.MATERIAL_100] = Color(0xDCEDC8)
    table[Color.LIGHT_GREEN, Category.MATERIAL_200] = Color(0xC5E1A5)
    table[Color.LIGHT_GREEN, Category.MATERIAL_300] = Color(0xAED581)
    table[Color.LIGHT_GREEN, Category.MATERIAL_400] = Color(0x9CCC65)
    table[Color.LIGHT_GREEN, Category.MATERIAL_500] = Color(0x8BC34A)
    table[Color.LIGHT_GREEN, Category.MATERIAL_600] = Color(0x7CB342)
    table[Color.LIGHT_GREEN, Category.MATERIAL_700] = Color(0x689F38)
    table[Color.LIGHT_GREEN, Category.MATERIAL_800] = Color(0x558B2F)
    table[Color.LIGHT_GREEN, Category.MATERIAL_900] = Color(0x33691E)
    table[Color.LIGHT_GREEN, Category.MATERIAL_ACCENT_100] = Color(0xCCFF90)
    table[Color.LIGHT_GREEN, Category.MATERIAL_ACCENT_200] = Color(0xB2FF59)
    table[Color.LIGHT_GREEN, Category.MATERIAL_ACCENT_400] = Color(0x76FF03)
    table[Color.LIGHT_GREEN, Category.MATERIAL_ACCENT_700] = Color(0x64DD17)

    table[Color.LIME, Category.MATERIAL_50] = Color(0xF9FBE7)
    table[Color.LIME, Category.MATERIAL_100] = Color(0xF0F4C3)
    table[Color.LIME, Category.MATERIAL_200] = Color(0xE6EE9C)
    table[Color.LIME, Category.MATERIAL_300] = Color(0xDCE775)
    table[Color.LIME, Category.MATERIAL_400] = Color(0xD4E157)
    table[Color.LIME, Category.MATERIAL_500] = Color(0xCDDC39)
    table[Color.LIME, Category.MATERIAL_600] = Color(0xC0CA33)
    table[Color.LIME, Category.MATERIAL_700] = Color(0xAFB42B)
    table[Color.LIME, Category.MATERIAL_800] = Color(0x9E9D24)
    table[Color.LIME, Category.MATERIAL_900] = Color(0x827717)
    table[Color.LIME, Category.MATERIAL_ACCENT_100] = Color(0xF4FF81)
    table[Color.LIME, Category.MATERIAL_ACCENT_200] = Color(0xEEFF41)
    table[Color.LIME, Category.MATERIAL_ACCENT_400] = Color(0xC6FF00)
    table[Color.LIME, Category.MATERIAL_ACCENT_700] = Color(0xAEEA00)

    table[Color.YELLOW, Category.MATERIAL_50] = Color(0xFFFDE7)
    table[Color.YELLOW, Category.MATERIAL_100] = Color(0xFFF9C4)
    table[Color.YELLOW, Category.MATERIAL_200] = Color(0xFFF59D)
    table[Color.YELLOW, Category.MATERIAL_300] = Color(0xFFF176)
    table[Color.YELLOW, Category.MATERIAL_400] = Color(0xFFEE58)
    table[Color.YELLOW, Category.MATERIAL_500] = Color(0xFFEB3B)
    table[Color.YELLOW, Category.MATERIAL_600] = Color(0xFDD835)
    table[Color.YELLOW, Category.MATERIAL_700] = Color(0xFBC02D)
    table[Color.YELLOW, Category.MATERIAL_800] = Color(0xF9A825)
    table[Color.YELLOW, Category.MATERIAL_900] = Color(0xF57F17)
    table[Color.YELLOW, Category.MATERIAL_ACCENT_100] = Color(0xFFFF8D)
    table[Color.YELLOW, Category.MATERIAL_ACCENT_200] = Color(0xFFFF00)
    table[Color.YELLOW, Category.MATERIAL_ACCENT_400] = Color(0xFFEA00)
    table[Color.YELLOW, Category.MATERIAL_ACCENT_700] = Color(0xFFD600)

    table[Color.AMBER, Category.MATERIAL_50] = Color(0xFFF8E1)
    table[Color.AMBER, Category.MATERIAL_100] = Color(0xFFECB3)
    table[Color.AMBER, Category.MATERIAL_200] = Color(0xFFE082)
    table[Color.AMBER, Category.MATERIAL_300] = Color(0xFFD54F)
    table[Color.AMBER, Category.MATERIAL_400] = Color(0xFFCA28)
    table[Color.AMBER, Category.MATERIAL_500] = Color(0xFFC107)
    table[Color.AMBER, Category.MATERIAL_600] = Color(0xFFB300)
    table[Color.AMBER, Category.MATERIAL_700] = Color(0xFFA000)
    table[Color.AMBER, Category.MATERIAL_800] = Color(0xFF8F00)
    table[Color.AMBER, Category.MATERIAL_900] = Color(0xFF6F00)
    table[Color.AMBER, Category.MATERIAL_ACCENT_100] = Color(0xFFE57F)
    table[Color.AMBER, Category.MATERIAL_ACCENT_200] = Color(0xFFD740)
    table[Color.AMBER, Category.MATERIAL_ACCENT_400] = Color(0xFFC400)
    table[Color.AMBER, Category.MATERIAL_ACCENT_700] = Color(0xFFAB00)

    table[Color.ORANGE, Category.MATERIAL_50] = Color(0xFFF3E0)
    table[Color.ORANGE, Category.MATERIAL_100] = Color(0xFFE0B2)
    table[Color.ORANGE, Category.MATERIAL_200] = Color(0xFFCC80)
    table[Color.ORANGE, Category.MATERIAL_300] = Color(0xFFB74D)
    table[Color.ORANGE, Category.MATERIAL_400] = Color(0xFFA726)
    table[Color.ORANGE, Category.MATERIAL_500] = Color(0xFF9800)
    table[Color.ORANGE, Category.MATERIAL_600] = Color(0xFB8C00)
    table[Color.ORANGE, Category.MATERIAL_700] = Color(0xF57C00)
    table[Color.ORANGE, Category.MATERIAL_800] = Color(0xEF6C00)
    table[Color.ORANGE, Category.MATERIAL_900] = Color(0xE65100)
    table[Color.ORANGE, Category.MATERIAL_ACCENT_100] = Color(0xFFD180)
    table[Color.ORANGE, Category.MATERIAL_ACCENT_200] = Color(0xFFAB40)
    table[Color.ORANGE, Category.MATERIAL_ACCENT_400] = Color(0xFF9100)
    table[Color.ORANGE, Category.MATERIAL_ACCENT_700] = Color(0xFF6D00)

    table[Color.DEEP_ORANGE, Category.MATERIAL_50] = Color(0xFBE9E7)
    table[Color.DEEP_ORANGE, Category.MATERIAL_100] = Color(0xFFCCBC)
    table[Color.DEEP_ORANGE, Category.MATERIAL_200] = Color(0xFFAB91)
    table[Color.DEEP_ORANGE, Category.MATERIAL_300] = Color(0xFF8A65)
    table[Color.DEEP_ORANGE, Category.MATERIAL_400] = Color(0xFF7043)
    table[Color.DEEP_ORANGE, Category.MATERIAL_500] = Color(0xFF5722)
    table[Color.DEEP_ORANGE, Category.MATERIAL_600] = Color(0xF4511E)
    table[Color.DEEP_ORANGE, Category.MATERIAL_700] = Color(0xE64A19)
    table[Color.DEEP_ORANGE, Category.MATERIAL_800] = Color(0xD84315)
    table[Color.DEEP_ORANGE, Category.MATERIAL_900] = Color(0xBF360C)
    table[Color.DEEP_ORANGE, Category.MATERIAL_ACCENT_100] = Color(0xFF9E80)
    table[Color.DEEP_ORANGE, Category.MATERIAL_ACCENT_200] = Color(0xFF6E40)
    table[Color.DEEP_ORANGE, Category.MATERIAL_ACCENT_400] = Color(0xFF3D00)
    table[Color.DEEP_ORANGE, Category.MATERIAL_ACCENT_700] = Color(0xDD2C00)

    table[Color.BROWN, Category.MATERIAL_50] = Color(0xEFEBE9)
    table[Color.BROWN, Category.MATERIAL_100] = Color(0xD7CCC8)
    table[Color.BROWN, Category.MATERIAL_200] = Color(0xBCAAA4)
    table[Color.BROWN, Category.MATERIAL_300] = Color(0xA1887F)
    table[Color.BROWN, Category.MATERIAL_400] = Color(0x8D6E63)
    table[Color.BROWN, Category.MATERIAL_500] = Color(0x795548)
    table[Color.BROWN, Category.MATERIAL_600] = Color(0x6D4C41)
    table[Color.BROWN, Category.MATERIAL_700] = Color(0x5D4037)
    table[Color.BROWN, Category.MATERIAL_800] = Color(0x4E342E)
    table[Color.BROWN, Category.MATERIAL_900] = Color(0x3E2723)

    table[Color.GRAY, Category.MATERIAL_50] = Color(0xFAFAFA)
    table[Color.GRAY, Category.MATERIAL_100] = Color(0xF5F5F5)
    table[Color.GRAY, Category.MATERIAL_200] = Color(0xEEEEEE)
    table[Color.GRAY, Category.MATERIAL_300] = Color(0xE0E0E0)
    table[Color.GRAY, Category.MATERIAL_400] = Color(0xBDBDBD)
    table[Color.GRAY, Category.MATERIAL_500] = Color(0x9E9E9E)
    table[Color.GRAY, Category.MATERIAL_600] = Color(0x757575)
    table[Color.GRAY, Category.MATERIAL_700] = Color(0x616161)
    table[Color.GRAY, Category.MATERIAL_800] = Color(0x424242)
    table[Color.GRAY, Category.MATERIAL_900] = Color(0x212121)

    table[Color.BLUE_GRAY, Category.MATERIAL_50] = Color(0xECEFF1)
    table[Color.BLUE_GRAY, Category.MATERIAL_100] = Color(0xCFD8DC)
    table[Color.BLUE_GRAY, Category.MATERIAL_200] = Color(0xB0BEC5)
    table[Color.BLUE_GRAY, Category.MATERIAL_300] = Color(0x90A4AE)
    table[Color.BLUE_GRAY, Category.MATERIAL_400] = Color(0x78909C)
    table[Color.BLUE_GRAY, Category.MATERIAL_500] = Color(0x607D8B)
    table[Color.BLUE_GRAY, Category.MATERIAL_600] = Color(0x546E7A)
    table[Color.BLUE_GRAY, Category.MATERIAL_700] = Color(0x455A64)
    table[Color.BLUE_GRAY, Category.MATERIAL_800] = Color(0x37474F)
    table[Color.BLUE_GRAY, Category.MATERIAL_900] = Color(0x263238)
  }

  @JvmStatic
  fun getColor(name: Color, category: Category) = table[name, category]

  /**
   * Get the series of [java.awt.Color] by the given [Color].
   */
  @JvmStatic
  fun getColorSeries(name: Color) = table.row(name)

  /**
   * Get the set of [java.awt.Color] by the given [Category].
   */
  @JvmStatic
  fun getColorSet(category: Category): Map<Color, java.awt.Color> = table.column(category)

  // Keep these constants for back compatibility

  @JvmField val RED_50 = table[Color.RED, Category.MATERIAL_50]!!
  @JvmField val RED_100 = table[Color.RED, Category.MATERIAL_100]!!
  @JvmField val RED_200 = table[Color.RED, Category.MATERIAL_200]!!
  @JvmField val RED_300 = table[Color.RED, Category.MATERIAL_300]!!
  @JvmField val RED_400 = table[Color.RED, Category.MATERIAL_400]!!
  @JvmField val RED_500 = table[Color.RED, Category.MATERIAL_500]!!
  @JvmField val RED_600 = table[Color.RED, Category.MATERIAL_600]!!
  @JvmField val RED_700 = table[Color.RED, Category.MATERIAL_700]!!
  @JvmField val RED_800 = table[Color.RED, Category.MATERIAL_800]!!
  @JvmField val RED_900 = table[Color.RED, Category.MATERIAL_900]!!
  @JvmField val PINK_50 = table[Color.PINK, Category.MATERIAL_50]!!
  @JvmField val PINK_100 = table[Color.PINK, Category.MATERIAL_100]!!
  @JvmField val PINK_200 = table[Color.PINK, Category.MATERIAL_200]!!
  @JvmField val PINK_300 = table[Color.PINK, Category.MATERIAL_300]!!
  @JvmField val PINK_400 = table[Color.PINK, Category.MATERIAL_400]!!
  @JvmField val PINK_500 = table[Color.PINK, Category.MATERIAL_500]!!
  @JvmField val PINK_600 = table[Color.PINK, Category.MATERIAL_600]!!
  @JvmField val PINK_700 = table[Color.PINK, Category.MATERIAL_700]!!
  @JvmField val PINK_800 = table[Color.PINK, Category.MATERIAL_800]!!
  @JvmField val PINK_900 = table[Color.PINK, Category.MATERIAL_900]!!
  @JvmField val PURPLE_50 =  table[Color.PURPLE, Category.MATERIAL_50]!!
  @JvmField val PURPLE_100 = table[Color.PURPLE, Category.MATERIAL_100]!!
  @JvmField val PURPLE_200 = table[Color.PURPLE, Category.MATERIAL_200]!!
  @JvmField val PURPLE_300 = table[Color.PURPLE, Category.MATERIAL_300]!!
  @JvmField val PURPLE_400 = table[Color.PURPLE, Category.MATERIAL_400]!!
  @JvmField val PURPLE_500 = table[Color.PURPLE, Category.MATERIAL_500]!!
  @JvmField val PURPLE_600 = table[Color.PURPLE, Category.MATERIAL_600]!!
  @JvmField val PURPLE_700 = table[Color.PURPLE, Category.MATERIAL_700]!!
  @JvmField val PURPLE_800 = table[Color.PURPLE, Category.MATERIAL_800]!!
  @JvmField val PURPLE_900 = table[Color.PURPLE, Category.MATERIAL_900]!!
  @JvmField val DEEP_PURPLE_50 =  table[Color.DEEP_PURPLE, Category.MATERIAL_50]!!
  @JvmField val DEEP_PURPLE_100 = table[Color.DEEP_PURPLE, Category.MATERIAL_100]!!
  @JvmField val DEEP_PURPLE_200 = table[Color.DEEP_PURPLE, Category.MATERIAL_200]!!
  @JvmField val DEEP_PURPLE_300 = table[Color.DEEP_PURPLE, Category.MATERIAL_300]!!
  @JvmField val DEEP_PURPLE_400 = table[Color.DEEP_PURPLE, Category.MATERIAL_400]!!
  @JvmField val DEEP_PURPLE_500 = table[Color.DEEP_PURPLE, Category.MATERIAL_500]!!
  @JvmField val DEEP_PURPLE_600 = table[Color.DEEP_PURPLE, Category.MATERIAL_600]!!
  @JvmField val DEEP_PURPLE_700 = table[Color.DEEP_PURPLE, Category.MATERIAL_700]!!
  @JvmField val DEEP_PURPLE_800 = table[Color.DEEP_PURPLE, Category.MATERIAL_800]!!
  @JvmField val DEEP_PURPLE_900 = table[Color.DEEP_PURPLE, Category.MATERIAL_900]!!
  @JvmField val INDIGO_50 =  table[Color.INDIGO, Category.MATERIAL_50]!!
  @JvmField val INDIGO_100 = table[Color.INDIGO, Category.MATERIAL_100]!!
  @JvmField val INDIGO_200 = table[Color.INDIGO, Category.MATERIAL_200]!!
  @JvmField val INDIGO_300 = table[Color.INDIGO, Category.MATERIAL_300]!!
  @JvmField val INDIGO_400 = table[Color.INDIGO, Category.MATERIAL_400]!!
  @JvmField val INDIGO_500 = table[Color.INDIGO, Category.MATERIAL_500]!!
  @JvmField val INDIGO_600 = table[Color.INDIGO, Category.MATERIAL_600]!!
  @JvmField val INDIGO_700 = table[Color.INDIGO, Category.MATERIAL_700]!!
  @JvmField val INDIGO_800 = table[Color.INDIGO, Category.MATERIAL_800]!!
  @JvmField val INDIGO_900 = table[Color.INDIGO, Category.MATERIAL_900]!!
  @JvmField val BLUE_50 =  table[Color.BLUE, Category.MATERIAL_50]!!
  @JvmField val BLUE_100 = table[Color.BLUE, Category.MATERIAL_100]!!
  @JvmField val BLUE_200 = table[Color.BLUE, Category.MATERIAL_200]!!
  @JvmField val BLUE_300 = table[Color.BLUE, Category.MATERIAL_300]!!
  @JvmField val BLUE_400 = table[Color.BLUE, Category.MATERIAL_400]!!
  @JvmField val BLUE_500 = table[Color.BLUE, Category.MATERIAL_500]!!
  @JvmField val BLUE_600 = table[Color.BLUE, Category.MATERIAL_600]!!
  @JvmField val BLUE_700 = table[Color.BLUE, Category.MATERIAL_700]!!
  @JvmField val BLUE_800 = table[Color.BLUE, Category.MATERIAL_800]!!
  @JvmField val BLUE_900 = table[Color.BLUE, Category.MATERIAL_900]!!
  @JvmField val LIGHT_BLUE_50 =  table[Color.LIGHT_BLUE, Category.MATERIAL_50]!!
  @JvmField val LIGHT_BLUE_100 = table[Color.LIGHT_BLUE, Category.MATERIAL_100]!!
  @JvmField val LIGHT_BLUE_200 = table[Color.LIGHT_BLUE, Category.MATERIAL_200]!!
  @JvmField val LIGHT_BLUE_300 = table[Color.LIGHT_BLUE, Category.MATERIAL_300]!!
  @JvmField val LIGHT_BLUE_400 = table[Color.LIGHT_BLUE, Category.MATERIAL_400]!!
  @JvmField val LIGHT_BLUE_500 = table[Color.LIGHT_BLUE, Category.MATERIAL_500]!!
  @JvmField val LIGHT_BLUE_600 = table[Color.LIGHT_BLUE, Category.MATERIAL_600]!!
  @JvmField val LIGHT_BLUE_700 = table[Color.LIGHT_BLUE, Category.MATERIAL_700]!!
  @JvmField val LIGHT_BLUE_800 = table[Color.LIGHT_BLUE, Category.MATERIAL_800]!!
  @JvmField val LIGHT_BLUE_900 = table[Color.LIGHT_BLUE, Category.MATERIAL_900]!!
  @JvmField val CYAN_50 =  table[Color.CYAN, Category.MATERIAL_50]!!
  @JvmField val CYAN_100 = table[Color.CYAN, Category.MATERIAL_100]!!
  @JvmField val CYAN_200 = table[Color.CYAN, Category.MATERIAL_200]!!
  @JvmField val CYAN_300 = table[Color.CYAN, Category.MATERIAL_300]!!
  @JvmField val CYAN_400 = table[Color.CYAN, Category.MATERIAL_400]!!
  @JvmField val CYAN_500 = table[Color.CYAN, Category.MATERIAL_500]!!
  @JvmField val CYAN_600 = table[Color.CYAN, Category.MATERIAL_600]!!
  @JvmField val CYAN_700 = table[Color.CYAN, Category.MATERIAL_700]!!
  @JvmField val CYAN_800 = table[Color.CYAN, Category.MATERIAL_800]!!
  @JvmField val CYAN_900 = table[Color.CYAN, Category.MATERIAL_900]!!
  @JvmField val TEAL_50 = table[Color.TEAL, Category.MATERIAL_50]!!
  @JvmField val TEAL_100 = table[Color.TEAL, Category.MATERIAL_100]!!
  @JvmField val TEAL_200 = table[Color.TEAL, Category.MATERIAL_200]!!
  @JvmField val TEAL_300 = table[Color.TEAL, Category.MATERIAL_300]!!
  @JvmField val TEAL_400 = table[Color.TEAL, Category.MATERIAL_400]!!
  @JvmField val TEAL_500 = table[Color.TEAL, Category.MATERIAL_500]!!
  @JvmField val TEAL_600 = table[Color.TEAL, Category.MATERIAL_600]!!
  @JvmField val TEAL_700 = table[Color.TEAL, Category.MATERIAL_700]!!
  @JvmField val TEAL_800 = table[Color.TEAL, Category.MATERIAL_800]!!
  @JvmField val TEAL_900 = table[Color.TEAL, Category.MATERIAL_900]!!
  @JvmField val GREEN_50 = table[Color.GREEN, Category.MATERIAL_50]!!
  @JvmField val GREEN_100 = table[Color.GREEN, Category.MATERIAL_100]!!
  @JvmField val GREEN_200 = table[Color.GREEN, Category.MATERIAL_200]!!
  @JvmField val GREEN_300 = table[Color.GREEN, Category.MATERIAL_300]!!
  @JvmField val GREEN_400 = table[Color.GREEN, Category.MATERIAL_400]!!
  @JvmField val GREEN_500 = table[Color.GREEN, Category.MATERIAL_500]!!
  @JvmField val GREEN_600 = table[Color.GREEN, Category.MATERIAL_600]!!
  @JvmField val GREEN_700 = table[Color.GREEN, Category.MATERIAL_700]!!
  @JvmField val GREEN_800 = table[Color.GREEN, Category.MATERIAL_800]!!
  @JvmField val GREEN_900 = table[Color.GREEN, Category.MATERIAL_900]!!
  @JvmField val LIGHT_GREEN_50 = table[Color.LIGHT_GREEN, Category.MATERIAL_50]!!
  @JvmField val LIGHT_GREEN_100 = table[Color.LIGHT_GREEN, Category.MATERIAL_100]!!
  @JvmField val LIGHT_GREEN_200 = table[Color.LIGHT_GREEN, Category.MATERIAL_200]!!
  @JvmField val LIGHT_GREEN_300 = table[Color.LIGHT_GREEN, Category.MATERIAL_300]!!
  @JvmField val LIGHT_GREEN_400 = table[Color.LIGHT_GREEN, Category.MATERIAL_400]!!
  @JvmField val LIGHT_GREEN_500 = table[Color.LIGHT_GREEN, Category.MATERIAL_500]!!
  @JvmField val LIGHT_GREEN_600 = table[Color.LIGHT_GREEN, Category.MATERIAL_600]!!
  @JvmField val LIGHT_GREEN_700 = table[Color.LIGHT_GREEN, Category.MATERIAL_700]!!
  @JvmField val LIGHT_GREEN_800 = table[Color.LIGHT_GREEN, Category.MATERIAL_800]!!
  @JvmField val LIGHT_GREEN_900 = table[Color.LIGHT_GREEN, Category.MATERIAL_900]!!
  @JvmField val LIME_50 = table[Color.LIME, Category.MATERIAL_50]!!
  @JvmField val LIME_100 = table[Color.LIME, Category.MATERIAL_100]!!
  @JvmField val LIME_200 = table[Color.LIME, Category.MATERIAL_200]!!
  @JvmField val LIME_300 = table[Color.LIME, Category.MATERIAL_300]!!
  @JvmField val LIME_400 = table[Color.LIME, Category.MATERIAL_400]!!
  @JvmField val LIME_500 = table[Color.LIME, Category.MATERIAL_500]!!
  @JvmField val LIME_600 = table[Color.LIME, Category.MATERIAL_600]!!
  @JvmField val LIME_700 = table[Color.LIME, Category.MATERIAL_700]!!
  @JvmField val LIME_800 = table[Color.LIME, Category.MATERIAL_800]!!
  @JvmField val LIME_900 = table[Color.LIME, Category.MATERIAL_900]!!
  @JvmField val YELLOW_50 = table[Color.YELLOW, Category.MATERIAL_50]!!
  @JvmField val YELLOW_100 = table[Color.YELLOW, Category.MATERIAL_100]!!
  @JvmField val YELLOW_200 = table[Color.YELLOW, Category.MATERIAL_200]!!
  @JvmField val YELLOW_300 = table[Color.YELLOW, Category.MATERIAL_300]!!
  @JvmField val YELLOW_400 = table[Color.YELLOW, Category.MATERIAL_400]!!
  @JvmField val YELLOW_500 = table[Color.YELLOW, Category.MATERIAL_500]!!
  @JvmField val YELLOW_600 = table[Color.YELLOW, Category.MATERIAL_600]!!
  @JvmField val YELLOW_700 = table[Color.YELLOW, Category.MATERIAL_700]!!
  @JvmField val YELLOW_800 = table[Color.YELLOW, Category.MATERIAL_800]!!
  @JvmField val YELLOW_900 = table[Color.YELLOW, Category.MATERIAL_900]!!
  @JvmField val AMBER_50 = table[Color.AMBER, Category.MATERIAL_50]!!
  @JvmField val AMBER_100 = table[Color.AMBER, Category.MATERIAL_100]!!
  @JvmField val AMBER_200 = table[Color.AMBER, Category.MATERIAL_200]!!
  @JvmField val AMBER_300 = table[Color.AMBER, Category.MATERIAL_300]!!
  @JvmField val AMBER_400 = table[Color.AMBER, Category.MATERIAL_400]!!
  @JvmField val AMBER_500 = table[Color.AMBER, Category.MATERIAL_500]!!
  @JvmField val AMBER_600 = table[Color.AMBER, Category.MATERIAL_600]!!
  @JvmField val AMBER_700 = table[Color.AMBER, Category.MATERIAL_700]!!
  @JvmField val AMBER_800 = table[Color.AMBER, Category.MATERIAL_800]!!
  @JvmField val AMBER_900 = table[Color.AMBER, Category.MATERIAL_900]!!
  @JvmField val ORANGE_50 = table[Color.ORANGE, Category.MATERIAL_50]!!
  @JvmField val ORANGE_100 = table[Color.ORANGE, Category.MATERIAL_100]!!
  @JvmField val ORANGE_200 = table[Color.ORANGE, Category.MATERIAL_200]!!
  @JvmField val ORANGE_300 = table[Color.ORANGE, Category.MATERIAL_300]!!
  @JvmField val ORANGE_400 = table[Color.ORANGE, Category.MATERIAL_400]!!
  @JvmField val ORANGE_500 = table[Color.ORANGE, Category.MATERIAL_500]!!
  @JvmField val ORANGE_600 = table[Color.ORANGE, Category.MATERIAL_600]!!
  @JvmField val ORANGE_700 = table[Color.ORANGE, Category.MATERIAL_700]!!
  @JvmField val ORANGE_800 = table[Color.ORANGE, Category.MATERIAL_800]!!
  @JvmField val ORANGE_900 = table[Color.ORANGE, Category.MATERIAL_900]!!
  @JvmField val DEEP_ORANGE_50 = table[Color.DEEP_ORANGE, Category.MATERIAL_50]!!
  @JvmField val DEEP_ORANGE_100 = table[Color.DEEP_ORANGE, Category.MATERIAL_100]!!
  @JvmField val DEEP_ORANGE_200 = table[Color.DEEP_ORANGE, Category.MATERIAL_200]!!
  @JvmField val DEEP_ORANGE_300 = table[Color.DEEP_ORANGE, Category.MATERIAL_300]!!
  @JvmField val DEEP_ORANGE_400 = table[Color.DEEP_ORANGE, Category.MATERIAL_400]!!
  @JvmField val DEEP_ORANGE_500 = table[Color.DEEP_ORANGE, Category.MATERIAL_500]!!
  @JvmField val DEEP_ORANGE_600 = table[Color.DEEP_ORANGE, Category.MATERIAL_600]!!
  @JvmField val DEEP_ORANGE_700 = table[Color.DEEP_ORANGE, Category.MATERIAL_700]!!
  @JvmField val DEEP_ORANGE_800 = table[Color.DEEP_ORANGE, Category.MATERIAL_800]!!
  @JvmField val DEEP_ORANGE_900 = table[Color.DEEP_ORANGE, Category.MATERIAL_900]!!
  @JvmField val BROWN_50 = table[Color.BROWN, Category.MATERIAL_50]!!
  @JvmField val BROWN_100 = table[Color.BROWN, Category.MATERIAL_100]!!
  @JvmField val BROWN_200 = table[Color.BROWN, Category.MATERIAL_200]!!
  @JvmField val BROWN_300 = table[Color.BROWN, Category.MATERIAL_300]!!
  @JvmField val BROWN_400 = table[Color.BROWN, Category.MATERIAL_400]!!
  @JvmField val BROWN_500 = table[Color.BROWN, Category.MATERIAL_500]!!
  @JvmField val BROWN_600 = table[Color.BROWN, Category.MATERIAL_600]!!
  @JvmField val BROWN_700 = table[Color.BROWN, Category.MATERIAL_700]!!
  @JvmField val BROWN_800 = table[Color.BROWN, Category.MATERIAL_800]!!
  @JvmField val BROWN_900 = table[Color.BROWN, Category.MATERIAL_900]!!
  @JvmField val GRAY_50 = table[Color.GRAY, Category.MATERIAL_50]!!
  @JvmField val GRAY_100 = table[Color.GRAY, Category.MATERIAL_100]!!
  @JvmField val GRAY_200 = table[Color.GRAY, Category.MATERIAL_200]!!
  @JvmField val GRAY_300 = table[Color.GRAY, Category.MATERIAL_300]!!
  @JvmField val GRAY_400 = table[Color.GRAY, Category.MATERIAL_400]!!
  @JvmField val GRAY_500 = table[Color.GRAY, Category.MATERIAL_500]!!
  @JvmField val GRAY_600 = table[Color.GRAY, Category.MATERIAL_600]!!
  @JvmField val GRAY_700 = table[Color.GRAY, Category.MATERIAL_700]!!
  @JvmField val GRAY_800 = table[Color.GRAY, Category.MATERIAL_800]!!
  @JvmField val GRAY_900 = table[Color.GRAY, Category.MATERIAL_900]!!
  @JvmField val BLUE_GRAY_50 = table[Color.BLUE_GRAY, Category.MATERIAL_50]!!
  @JvmField val BLUE_GRAY_100 = table[Color.BLUE_GRAY, Category.MATERIAL_100]!!
  @JvmField val BLUE_GRAY_200 = table[Color.BLUE_GRAY, Category.MATERIAL_200]!!
  @JvmField val BLUE_GRAY_300 = table[Color.BLUE_GRAY, Category.MATERIAL_300]!!
  @JvmField val BLUE_GRAY_400 = table[Color.BLUE_GRAY, Category.MATERIAL_400]!!
  @JvmField val BLUE_GRAY_500 = table[Color.BLUE_GRAY, Category.MATERIAL_500]!!
  @JvmField val BLUE_GRAY_600 = table[Color.BLUE_GRAY, Category.MATERIAL_600]!!
  @JvmField val BLUE_GRAY_700 = table[Color.BLUE_GRAY, Category.MATERIAL_700]!!
  @JvmField val BLUE_GRAY_800 = table[Color.BLUE_GRAY, Category.MATERIAL_800]!!
  @JvmField val BLUE_GRAY_900 = table[Color.BLUE_GRAY, Category.MATERIAL_900]!!

  @JvmField val RED_ACCENT_100 = table[Color.RED, Category.MATERIAL_ACCENT_100]!!
  @JvmField val RED_ACCENT_200 = table[Color.RED, Category.MATERIAL_ACCENT_200]!!
  @JvmField val RED_ACCENT_400 = table[Color.RED, Category.MATERIAL_ACCENT_400]!!
  @JvmField val RED_ACCENT_700 = table[Color.RED, Category.MATERIAL_ACCENT_700]!!
  @JvmField val PINK_ACCENT_100 = table[Color.PINK, Category.MATERIAL_ACCENT_100]!!
  @JvmField val PINK_ACCENT_200 = table[Color.PINK, Category.MATERIAL_ACCENT_200]!!
  @JvmField val PINK_ACCENT_400 = table[Color.PINK, Category.MATERIAL_ACCENT_400]!!
  @JvmField val PINK_ACCENT_700 = table[Color.PINK, Category.MATERIAL_ACCENT_700]!!
  @JvmField val PURPLE_ACCENT_100 = table[Color.PURPLE, Category.MATERIAL_ACCENT_100]!!
  @JvmField val PURPLE_ACCENT_200 = table[Color.PURPLE, Category.MATERIAL_ACCENT_200]!!
  @JvmField val PURPLE_ACCENT_400 = table[Color.PURPLE, Category.MATERIAL_ACCENT_400]!!
  @JvmField val PURPLE_ACCENT_700 = table[Color.PURPLE, Category.MATERIAL_ACCENT_700]!!
  @JvmField val DEEP_PURPLE_ACCENT_100 = table[Color.DEEP_PURPLE, Category.MATERIAL_ACCENT_100]!!
  @JvmField val DEEP_PURPLE_ACCENT_200 = table[Color.DEEP_PURPLE, Category.MATERIAL_ACCENT_200]!!
  @JvmField val DEEP_PURPLE_ACCENT_400 = table[Color.DEEP_PURPLE, Category.MATERIAL_ACCENT_400]!!
  @JvmField val DEEP_PURPLE_ACCENT_700 = table[Color.DEEP_PURPLE, Category.MATERIAL_ACCENT_700]!!
  @JvmField val INDIGO_ACCENT_100 = table[Color.INDIGO, Category.MATERIAL_ACCENT_100]!!
  @JvmField val INDIGO_ACCENT_200 = table[Color.INDIGO, Category.MATERIAL_ACCENT_200]!!
  @JvmField val INDIGO_ACCENT_400 = table[Color.INDIGO, Category.MATERIAL_ACCENT_400]!!
  @JvmField val INDIGO_ACCENT_700 = table[Color.INDIGO, Category.MATERIAL_ACCENT_700]!!
  @JvmField val BLUE_ACCENT_100 = table[Color.BLUE, Category.MATERIAL_ACCENT_100]!!
  @JvmField val BLUE_ACCENT_200 = table[Color.BLUE, Category.MATERIAL_ACCENT_200]!!
  @JvmField val BLUE_ACCENT_400 = table[Color.BLUE, Category.MATERIAL_ACCENT_400]!!
  @JvmField val BLUE_ACCENT_700 = table[Color.BLUE, Category.MATERIAL_ACCENT_700]!!
  @JvmField val LIGHT_BLUE_ACCENT_100 = table[Color.LIGHT_BLUE, Category.MATERIAL_ACCENT_100]!!
  @JvmField val LIGHT_BLUE_ACCENT_200 = table[Color.LIGHT_BLUE, Category.MATERIAL_ACCENT_200]!!
  @JvmField val LIGHT_BLUE_ACCENT_400 = table[Color.LIGHT_BLUE, Category.MATERIAL_ACCENT_400]!!
  @JvmField val LIGHT_BLUE_ACCENT_700 = table[Color.LIGHT_BLUE, Category.MATERIAL_ACCENT_700]!!
  @JvmField val CYAN_ACCENT_100 = table[Color.CYAN, Category.MATERIAL_ACCENT_100]!!
  @JvmField val CYAN_ACCENT_200 = table[Color.CYAN, Category.MATERIAL_ACCENT_200]!!
  @JvmField val CYAN_ACCENT_400 = table[Color.CYAN, Category.MATERIAL_ACCENT_400]!!
  @JvmField val CYAN_ACCENT_700 = table[Color.CYAN, Category.MATERIAL_ACCENT_700]!!
  @JvmField val TEAL_ACCENT_100 = table[Color.TEAL, Category.MATERIAL_ACCENT_100]!!
  @JvmField val TEAL_ACCENT_200 = table[Color.TEAL, Category.MATERIAL_ACCENT_200]!!
  @JvmField val TEAL_ACCENT_400 = table[Color.TEAL, Category.MATERIAL_ACCENT_400]!!
  @JvmField val TEAL_ACCENT_700 = table[Color.TEAL, Category.MATERIAL_ACCENT_700]!!
  @JvmField val GREEN_ACCENT_100 = table[Color.GREEN, Category.MATERIAL_ACCENT_100]!!
  @JvmField val GREEN_ACCENT_200 = table[Color.GREEN, Category.MATERIAL_ACCENT_200]!!
  @JvmField val GREEN_ACCENT_400 = table[Color.GREEN, Category.MATERIAL_ACCENT_400]!!
  @JvmField val GREEN_ACCENT_700 = table[Color.GREEN, Category.MATERIAL_ACCENT_700]!!
  @JvmField val LIGHT_GREEN_ACCENT_100 = table[Color.LIGHT_GREEN, Category.MATERIAL_ACCENT_100]!!
  @JvmField val LIGHT_GREEN_ACCENT_200 = table[Color.LIGHT_GREEN, Category.MATERIAL_ACCENT_200]!!
  @JvmField val LIGHT_GREEN_ACCENT_400 = table[Color.LIGHT_GREEN, Category.MATERIAL_ACCENT_400]!!
  @JvmField val LIGHT_GREEN_ACCENT_700 = table[Color.LIGHT_GREEN, Category.MATERIAL_ACCENT_700]!!
  @JvmField val LIME_ACCENT_100 = table[Color.LIME, Category.MATERIAL_ACCENT_100]!!
  @JvmField val LIME_ACCENT_200 = table[Color.LIME, Category.MATERIAL_ACCENT_200]!!
  @JvmField val LIME_ACCENT_400 = table[Color.LIME, Category.MATERIAL_ACCENT_400]!!
  @JvmField val LIME_ACCENT_700 = table[Color.LIME, Category.MATERIAL_ACCENT_700]!!
  @JvmField val YELLOW_ACCENT_100 = table[Color.YELLOW, Category.MATERIAL_ACCENT_100]!!
  @JvmField val YELLOW_ACCENT_200 = table[Color.YELLOW, Category.MATERIAL_ACCENT_200]!!
  @JvmField val YELLOW_ACCENT_400 = table[Color.YELLOW, Category.MATERIAL_ACCENT_400]!!
  @JvmField val YELLOW_ACCENT_700 = table[Color.YELLOW, Category.MATERIAL_ACCENT_700]!!
  @JvmField val AMBER_ACCENT_100 = table[Color.AMBER, Category.MATERIAL_ACCENT_100]!!
  @JvmField val AMBER_ACCENT_200 = table[Color.AMBER, Category.MATERIAL_ACCENT_200]!!
  @JvmField val AMBER_ACCENT_400 = table[Color.AMBER, Category.MATERIAL_ACCENT_400]!!
  @JvmField val AMBER_ACCENT_700 = table[Color.AMBER, Category.MATERIAL_ACCENT_700]!!
  @JvmField val ORANGE_ACCENT_100 = table[Color.ORANGE, Category.MATERIAL_ACCENT_100]!!
  @JvmField val ORANGE_ACCENT_200 = table[Color.ORANGE, Category.MATERIAL_ACCENT_200]!!
  @JvmField val ORANGE_ACCENT_400 = table[Color.ORANGE, Category.MATERIAL_ACCENT_400]!!
  @JvmField val ORANGE_ACCENT_700 = table[Color.ORANGE, Category.MATERIAL_ACCENT_700]!!
  @JvmField val DEEP_ORANGE_ACCENT_100 = table[Color.DEEP_ORANGE, Category.MATERIAL_ACCENT_100]!!
  @JvmField val DEEP_ORANGE_ACCENT_200 = table[Color.DEEP_ORANGE, Category.MATERIAL_ACCENT_200]!!
  @JvmField val DEEP_ORANGE_ACCENT_400 = table[Color.DEEP_ORANGE, Category.MATERIAL_ACCENT_400]!!
  @JvmField val DEEP_ORANGE_ACCENT_700 = table[Color.DEEP_ORANGE, Category.MATERIAL_ACCENT_700]!!

  const val PRIMARY_MATERIAL_ATTR = "colorPrimary"
  const val PRIMARY_DARK_MATERIAL_ATTR = "colorPrimaryDark"
  const val ACCENT_MATERIAL_ATTR = "colorAccent"
}
