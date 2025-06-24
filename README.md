# PartyTS

PartyTS is a plugin for PaperMC servers that enhances the [TimingSystem](https://www.spigotmc.org/resources/timingsystem.96777/) plugin by adding social racing features. It allows players to create parties to race together or challenge other players to 1v1 duels on your server's tracks.

## Features

-   **Party System**: Create, invite, join, leave, and manage parties with your friends.
-   **Party Races**: Party leaders can initiate a private race for all party members on any available track.
-   **Player Duels**: Challenge any other player on the server to a 1v1 race to see who is fastest.
-   **Seamless Integration**: Built to work directly with the TimingSystem plugin for all race, track, and lap-time handling.
-   **Highly Configurable**: Customize race lengths, timeouts, and message colors to fit your server's style via a simple `config.yml`.
-   **Robust & Safe**: Automatically cleans up any active races or duels on server shutdown or reload to prevent data corruption.

## Dependencies

-   **Server**: Paper (or a fork like Purpur) for Minecraft 1.21.1 or newer.
-   **Plugin**: TimingSystem v2.3 or newer.

## Installation

1.  Ensure you have a supported PaperMC server and the **TimingSystem** plugin installed and configured.
2.  Download the latest `PartyTS.jar` from the releases page.
3.  Place the `PartyTS.jar` file into your server's `/plugins` directory.
4.  Start or restart your server. The configuration file (`config.yml`) will be generated in `/plugins/PartyTS/` on the first run.

## Usage

PartyTS provides two main commands: `/party` for managing groups and `/duel` for challenging players.

### Party Commands

| Command                                    | Description                                                  | Permission      |
| ------------------------------------------ | ------------------------------------------------------------ | --------------- |
| `/party create`                            | Creates a new party, making you the leader.                  | `partyts.user`  |
| `/party invite <player>`                   | Invites a player to your party.                              | `partyts.leader`|
| `/party accept <player>`                   | Accepts a pending party invitation from a player.            | `partyts.user`  |
| `/party decline <player>`                  | Declines a pending party invitation.                         | `partyts.user`  |
| `/party leave`                             | Leaves your current party.                                   | `partyts.user`  |
| `/party kick <player>`                     | Kicks a player from your party.                              | `partyts.leader`|
| `/party promote <player>`                  | Promotes a party member to be the new leader.                | `partyts.leader`|
| `/party disband`                           | Disbands the party, removing all members.                    | `partyts.leader`|
| `/party list`                              | Shows all members of your current party.                     | `partyts.user`  |
| `/party race <track> [laps] [pits]`        | Starts a race for the party on the specified track.          | `partyts.leader`|

### Duel Commands

| Command                                    | Description                                                  | Permission      |
| ------------------------------------------ | ------------------------------------------------------------ | --------------- |
| `/duel <player> <track> [laps] [pits]`     | Challenges a player to a 1v1 duel on a specific track.       | `partyts.user`  |
| `/duel accept`                             | Accepts a pending duel invitation.                           | `partyts.user`  |
| `/duel decline`                            | Declines a pending duel invitation.                          | `partyts.user`  |

## Configuration

You can customize the plugin's behavior in the `config.yml` file.
