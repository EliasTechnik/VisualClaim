# VisualClaim

VisualClaim is a small paper/spigot plugin which lets players claim chunks on your server. 
Claimed chunks are visually but non intrusive marked in the user interface and can be displayed on the Pl3xMap.
The functionality is only visual and does not prevent players from building in unclaimed chunks or other players' claimed chunks. 

## Features
- Claim chunks with a simple command
- Show claims on the Pl3xMap
- manage multiple claims and give them names

## Map Visualization
If Pl3xMap is installed on the server, claimed chunks will be displayed on the map. The color of the claimed chunks can be configured in the config file. Each player will get a random color assigned for their claims.


## About Claims and Chunks
VisualClaim thinks in claims. A claim is a collection of chunks which belong to a player and can be freely placed on the server (even on different worlds). Claims must have a name which must be unique within a player's claims. The creation of a claim requires at least one chunk to be claimed but claims can exist without any chunks (empty claims). The claimed chunks don't need to be adjacent.

## Claim Names
Claim names can have spaces but must be wrapped in quotes when used in commands. Claim names are unique per player. Two different players can have claims with the same name.

## Commands
- `/claim <name>` - Claim the chunk the player is currently in with the given name. (the name is optional). If a claim with the same name already exists, the chunk will be added to that claim.
- ...todo

## Todo

- prevent claiming in protected areas (worldguard, spawn, etc)
- prevent claimnames that match confirm keywords
- prevent multiple claims of the same chunk by the same player
- complete commands
- add ARGB color support
- add color customization (Fixed values from config from which the player can choose)
- add help
- add "non intrusive" claim visualization to HUD
- add persistent storage
- add permissions and limits
- add character limit
- add forbidden words filter
- remove non-config error messages in claimCommand
- test