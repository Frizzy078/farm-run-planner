# Bank to Patch

A RuneLite plugin that looks at your bank and plans your farm run for you.

## Features

- **Run planner** for herb runs, tree runs and fruit tree runs.
- Reads your **bank, inventory and equipment** and picks:
  - the best seeds/saplings you can plant with your Farming level,
  - compost (Ultracompost > Supercompost > Compost, or a Bottomless compost bucket),
  - the best **teleport you own** for every patch (Ardougne cloak, Explorer's ring, Ectophial,
    Xeric's talisman, Skills necklace, tablets, or spells if you have the runes),
  - the tools you need and the **coins** you need (tree removal and the Brimhaven boat),
  - the **protection payment** for the gardener for every tree (e.g. 10 cactus spines for a yew).
- **XP per run**: XP you get now (planting + compost) and later (check-health / harvest).
- **Seed overview**: how many runs your seeds and saplings last, and which tree seeds still need to become saplings.
- **Making saplings**: the best tree for your Farming level, how many saplings you can make and what you need.
- **Route tracker**: finished patches turn green, the current patch is yellow; click a step to tick it off.
- **Tree protection step**: each tree step ends with paying the gardener (by name). After planting, the hint arrow points to the gardener and the *Pay* option is highlighted; the route continues once you have paid.
- **Take list turns green** for items you already carry; the bank only highlights what you still need.
- **English or Dutch**: choose the panel language in the settings (Language / Taal).
- **Step-by-step guidance**:
  - highlights the correct right-click option on your teleport item (e.g. *Rub* on a Skills necklace),
  - highlights the correct choice in the teleport menu (e.g. *Farming Guild*),
  - highlights the farming patches in the world and shows a hint arrow to the nearest patch,
  - moves to the next patch automatically after you plant your seed.

## Usage

1. Open the **Bank to Patch** panel in the sidebar.
2. Open your bank once so the plugin can see your items.
3. Pick a run type and click **Make plan**.
4. Withdraw the highlighted items and follow the route.

## Settings

- Language: English or Nederlands
- Bank highlight on/off and colour
- Highlight teleport choice, patches and hint arrow on/off
- Coins per tree (default 200)
- Use teleport spells
- House in Hosidius (use *Teleport to house* for the Hosidius patch)
- Include the Trollheim herb patch / Lletya fruit tree patch

## Known limitations

- Seeds in the Farming Guild seed vault are not counted.
- Runes in a rune pouch and elemental staves are not counted.
- The plugin interface is in Dutch.
