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