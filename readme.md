# VisualClaim

VisualClaim is a small paper/spigot plugin which lets players claim chunks on your server. 
Claimed chunks are visually but non intrusive marked in the user interface and can be displayed on the Pl3xMap.
The functionality is only visual and does not prevent players from building in unclaimed chunks or other players' claimed chunks. 

## Features
- Claim chnunks with a simple command
- Show claims on the Pl3xMap
- manage multiple claims and give them names


## About Claims and Chunks
VisualClaim thinks in claims. A claim is a collection of chunks which belong to a player and can be freely placed on the server (even on different worlds). Claims can have a name which must be unique within a player's claims. Claims without a name are possible as well. Theses are called "default claim". The creation of a claim requires at least one chunk to be claimed but claims can exist without any chunks (empty claims). The claimed chunks dont need to be adjacent.

## Claim Names
Claim names can have spaces but must be wrapped in quotes when used in commands. Claim names are unique per player. Two different players can have claims with the same name.

## Commands
- `/claim <name>` - Claim the chunk the player is currently in with the given name. (the name is optional). If a claim with the same name already exists, the chunk will be added to that claim.

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