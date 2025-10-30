### HeadGuess – Multiplayer Discovery: Successful Fixes and Patterns

#### NSD host visibility pattern (use this trigger to recall)
- Stop discovery on leave/back/join: call `NetworkDiscovery.stopAllDiscoveries()` from UI Back and system Back.
- Debounce NSD onServiceLost by ~5s and only remove a host if it hasn’t been seen again since scheduling the removal.
- Prune stale hosts with a rolling last-seen window (~6s), checked periodically (e.g., every 2s).

#### Key files
- `app/src/main/java/com/example/headguess/network/NetworkDiscovery.kt`: `discoverMultipleHosts(...)` NSD entry point.
- `app/src/main/java/com/example/headguess/ui/CustomGuessJoinScreen.kt`:
  - Debounced `onHostLost` (5s) with last-seen guard.
  - Pruning loop (6s window) to drop stale IPs.
  - Stop discovery on UI Back and system Back.
- Similar join screens (General/Custom for Guess, Charades, Impostor) call `discoverMultipleHosts(...)` and stop discovery on leave/join.

#### Host/Client lifecycle highlights
- Host starts advertising in lobby; hides IP on start or on leaving lobby via `vm.quickStopHosting()` and/or `hostServer.stopNSD()`.
- Clients rely on NSD only (no periodic TCP reachability checks) for presence.
- Client lobby `hostDisconnected` immediately navigates back and refreshes discovery.

#### Why this works
- NSD can flap; debouncing `onServiceLost` prevents premature disappearance.
- Periodic pruning removes truly stale hosts if a loss event is missed.
- Stopping discovery on navigation prevents stale listeners and stale IPs.

#### Quick test checklist
- Host in lobby: client sees IP and it remains visible.
- Host leaves or starts game: client IP disappears within ~5–6s.
- Navigating away from join screens stops scanning immediately.


### Guess the Word – General (Working Reference)

- Discovery entry points
  - `ui/JoinGameScreen.kt` starts two discoveries: `gameType = "guessword"` and `gameType = "custom"` (cross-connection enabled).
  - Uses `NetworkDiscovery.discoverMultipleHosts(...)` and updates a single `discoveredHosts` list.

- Visibility rules (client)
  - No periodic socket checks; rely solely on NSD callbacks.
  - No timestamp pruning; hosts are removed only on `onHostLost` from NSD.
  - Back/system back and Join: call `NetworkDiscovery.stopAllDiscoveries()` to avoid stale results.

- Stability tweaks
  - For custom cross-connection in Join, `onHostLost` is debounced by ~5s before removal to absorb NSD flaps.
  - General `guessword` removals are immediate on `onHostLost`.

- Host behavior
  - Host publishes NSD in lobby via `GameViewModel.publishNSD()` (calls into `HostServer.publishNSD()`).
  - Host hides immediately on Start/Leave via `hostServer.stopNSD()` from `GameViewModel` (e.g., `startGameForAll`, `quickStopHosting`).

- Navigation hygiene
  - Join screen Back/system back: stop discovery then navigate up.
  - Joining a host: stop discovery before `vm.joinHost(...)`.

- Expected UX
  - While host stays in lobby: client IP remains visible and stable (general and custom).
  - When host leaves/starts: client IP disappears promptly (immediate for general; within ~5s for custom cross-connection).
  - Returning from Join: scanning stops instantly; no lingering IPs.


### Guess the Word – Cross-Connection (Working)

- Discovery
  - General Join starts `guessword` + `custom`.
  - Custom Join starts `custom` + `guessword`.

- Removal policy (client)
  - Immediate removal on NSD `onHostLost` for BOTH directions (general↔custom).
  - No pruning loops, no socket reachability checks.

- Navigation hygiene
  - Back/system back and before Join: call `NetworkDiscovery.stopAllDiscoveries()`.

- Expected UX
  - Host in General, Client in Custom: IP visible while host stays; disappears immediately when host leaves/starts.
  - Host in Custom, Client in General: same behavior, immediate disappearance on host leave/start.

