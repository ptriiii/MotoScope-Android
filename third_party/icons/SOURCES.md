# Material Symbols Rounded provenance

The selected functional icons are derived from Google's official Material Design Icons repository:

- Repository: https://github.com/google/material-design-icons
- License: Apache License 2.0, https://github.com/google/material-design-icons/blob/master/LICENSE
- License text is preserved in `Apache-2.0-Material-Symbols.txt`.
- Original source family: Material Symbols Rounded, 24 px, weight 400/default fill.

The checked-in SVG files preserve the corresponding official path data and 960 x 960 viewport. The shared UI renders those same paths through a small common `ImageVector` builder because Compose Multiplatform 1.11.1's Android resource painter rejects SVG at runtime (`Android platform doesn't support SVG format`). This keeps the runtime icons vector-based, cross-platform, and free of an extended-icons dependency; the SVG files remain the auditable source-vector copies.

| Local resource | Official source |
| --- | --- |
| `msr_chevron_left.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/chevron_left/materialsymbolsrounded/chevron_left_24px.svg |
| `msr_chevron_right.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/chevron_right/materialsymbolsrounded/chevron_right_24px.svg |
| `msr_home.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/home/materialsymbolsrounded/home_24px.svg |
| `msr_home_filled.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/home/materialsymbolsrounded/home_fill1_24px.svg |
| `msr_bar_chart.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/bar_chart/materialsymbolsrounded/bar_chart_24px.svg |
| `msr_two_wheeler.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/two_wheeler/materialsymbolsrounded/two_wheeler_24px.svg |
| `msr_more_horiz.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/more_horiz/materialsymbolsrounded/more_horiz_24px.svg |
| `msr_build.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/build/materialsymbolsrounded/build_24px.svg |
| `msr_inventory_2.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/inventory_2/materialsymbolsrounded/inventory_2_24px.svg |
| `msr_description.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/description/materialsymbolsrounded/description_24px.svg |
| `msr_article.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/article/materialsymbolsrounded/article_24px.svg |
| `msr_balance.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/balance/materialsymbolsrounded/balance_24px.svg |
| `msr_handyman.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/handyman/materialsymbolsrounded/handyman_24px.svg |
| `msr_view_in_ar.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/view_in_ar/materialsymbolsrounded/view_in_ar_24px.svg |
| `msr_compare_arrows.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/compare_arrows/materialsymbolsrounded/compare_arrows_24px.svg |
| `msr_speed.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/speed/materialsymbolsrounded/speed_24px.svg |
| `msr_rotate_right.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/rotate_right/materialsymbolsrounded/rotate_right_24px.svg |
| `msr_weight.svg` | https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/weight/materialsymbolsrounded/weight_24px.svg |

No additional icon family or runtime dependency is used.
