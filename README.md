# Inmis + ItemLocks Compatibility

Client-side Forge addon for Minecraft 1.20.1. Adds ItemLocks-style slot locking to Inmis backpack storage slots.

## Requirements
- Java 17
- Gradle 8.1+
- Minecraft Forge 1.20.1
- ItemLocks `1.20.1-1.3.8`
- Inmis `2.7.2-2.9.4-1.20.1`

If BMC4 Forge is installed v62hf is required which will bring Inmis & Minecraft in the correct version. ItemLocks mod has to be installed separately 

## Build
1. Run `gradle build` from the project root.
2. Find the JAR in `build/libs/`.

## Side Note
Since shift + mouseclick + dragging over slots moves the items from the hotbar to the backpack and vice-versa shift is not
supported as a button to unlock the slots

## Lock behavior (hotbar, inventory, backpack)
- Temporary lift key: hover a locked slot shows an open lock, locked slots can be clicked/shift-clicked/moved. Unlocked slots: nothing.
- Permanent lift key: click an unlocked slot (also empty ones) locks it, click a locked slot unlocks it. Hover on unlocked slots shows nothing.
- Empty locked slots still accept items placed by hand; shift-click/bulk moves skip them.

## Backpack buttons (ClientSort)
Needs ClientSort (`libs/clientsort-forge-3.82.2+1.20.1.jar`). The compat layer registers a ClientSort policy for the Inmis menu (sort, move all, move matching buttons for backpack and inventory, stack fill as key only) and makes ClientSort skip locked backpack slots. If you configure `draylar.inmis.ui.BackpackScreenHandler` in the ClientSort config yourself, yours wins.

## Tests
`gradle test` (pure logic: lock decisions, icons, ID mapping).
