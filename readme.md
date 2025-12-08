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

With the ```other```-Permissions almost every command can be executed for other players. All that is needed is to put ```-p <PlayerName>``` after the command and before the arguments.
**Note:** This is still work in progress and not all commands support this yet. Commands with support are:

- ```/renameclaim```
- ```/listclaims``` (sort of, the ```-p``` flag is not necessary here, just provide the player name as argument)
- ```/unclaim force``` (no flag necessary and name needed. It will stupidly unclaim the chunk from anything ;P )
- ```/deleteclaim```
- 

## Innerworkings

VisualClaim works mostly by commands issued by players. The call chain (should) look like this:

```
Player issues command 
    |
    V
CommandExecutor: Fetches player cache and command arguments, decides which command to execute, evaluates VCResults and sends messages to player
    |
    V
PrerequsiteService: Executes commands and does prerequisite checks beforehand
    |  /\
    V  |
ClaimService: Main service which talks to DB
    |
    V
ClaimRepository: Handles data storage and retrieval
```

VisualClaim relies heavily on its SQLite database. To take load off the main server thread, all database operations are executed asynchronously. 
This means that there might be a slight delay between issuing a command and seeing the result but in exchange the server performance will not be affected much. (Hopefully ;) )
To help with the speed and to enable smart tab completion, VisualClaim keeps an in-memory cache per player. The cache updates on important changes but might be slightly out of date in some edge cases.
The cache can be manually refreshed with every command execution. (even invalid ones.) Outdated caches will only affect tab completion and not the actual command execution.    


## Permissions

- ```VisualClaim.claim``` - Permission to claim, unlcaim and manage claims your own.
- ```VisualClaim.listOther``` - Permission to list other players' claims.
- ```VisualClaim.maxClaims.<number>``` - Set the maximum number of claims a player can have. Replace `<number>` with the desired limit. If multiple permissions are set, the highest number will be used. If no permission is set, the default limit from the config will be used. Replace `<number>` with `unlimited` for no limit.
- ```VisualClaim.maxChunks.<number>``` -  Set the maximum number of chunks a player can claim. Replace `<number>` with the desired limit. If multiple permissions are set, the highest number will be used. If no permission is set, the default limit from the config will be used. Replace `<number>` with `unlimited` for no limit.
- ```VisualClaim.maxclaimlength.<number>``` - Set the maximum length for claim names. Replace `<number>` with the desired limit. If multiple permissions are set, the lowest number will be used. If no permission is set, the default limit from the config will be used. Seting a high number here will break some ui elements like bossbars or markers on the map.
- ```VisualClaim.unclaimOther``` - Permission to unclaim chunks from other players' claims by using the `\unclaim force` command. TODO: check if this is implemented correctly.
- ```VisualClaim.deleteOther``` - Permission to delete other players' claims.
- ```VisualClaim.renameOther``` - Permission to rename other players' claims.
- ```VisualClaim.loadChunks``` - Permission to permanently load chunks. (the chunks don`t have to be claimed by the player)
- ```VisualClaim.listOtherChunkLoader``` - Permission to list other players' chunk loaders.
- ```VisualClaim.maxChunkLoaders.<number>``` - Set the maximum number of chunk loaders a player can have. Replace `<number>` with the desired limit. If multiple permissions are set, the highest number will be used. If no permission is set, the default limit from the config will be used. Replace `<number>` with `unlimited` for no limit. Use this with caution because it opens up the possibility of chunkloader abuse. (loading unlimited chunks can place artificial load on the server which cant be controlled by the server admin)`
- ```VisualClaim.unloadChunksOther``` - Permission to remove other players' chunk loaders.
- ```VisualClaim.maxclaimlorelength``` - Set the maximum length for claim lore. Replace `<number>` with the desired limit. If multiple permissions are set, the lowest number will be used. If no permission is set, the default limit from the config will be used.

## Todo

- add offline support for listclaims other: see TODO in ListClaimsCommand
- debug list other
- make claim visualize on each interaction
- fine tune claim visualisation
- make the debug option disable the debug log messages
- prevent claiming in protected areas (worldguard, spawn, etc)
- add ARGB color support to config
- add color customization (Fixed values from config from which the player can choose)
- add help
- add "non intrusive" claim visualization to HUD (togleable bossbar and particles on chunkborder)
- ```\showclaim <name>``` - shows border of claim with particles for a short time (or until toggled off) (always on when autoclaiming)
- ```/transferclaim <playername>:<claimname> <playername>``` - transfer claim to another player
- test
- Implement minimessage:
  - migrate all messages to minimessage

